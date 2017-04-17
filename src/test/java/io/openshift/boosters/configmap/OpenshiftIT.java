package io.openshift.boosters.configmap;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.openshift.booster.test.OpenShiftTestAssistant;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
@RunAsClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenshiftIT {

    private static final OpenShiftTestAssistant openshift = new OpenShiftTestAssistant();

    private static final String CONFIGMAP_NAME = "app-config";

    @BeforeClass
    public static void setup() throws Exception {
        // pre-requisite for swarm
        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config.yml"));

        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();

        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                Response response = get();
                return response.getStatusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });

        RestAssured.baseURI = RestAssured.baseURI + "/api/greeting";
    }

    @AfterClass
    public static void teardown() throws Exception {
       openshift.cleanup();
    }

    @Test
    public void test1ConfigMapExists() throws Exception {
        Optional<ConfigMap> configMap = findConfigMap();
        assertTrue(configMap.isPresent());
    }

    @Test
    public void test2ServiceInvocation() {
        when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello World from a ConfigMap!"));
    }

    @Test
    public void test3ServiceInvocationWithParam() {
        given()
                .queryParam("name", "Peter")
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello Peter from a ConfigMap!"));
    }

    @Test
    public void test4MissingEntryInConfigMap() throws IOException {
        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config-broken.yml"));
        // TODO rolloutChanges();

        when()
                .get()
        .then()
                .statusCode(500);
    }

    @Test
    public void test5MissingConfigMap() throws IOException {
        openshift.client().configMaps().withName(CONFIGMAP_NAME).delete();
        // TODO rolloutChanges();

        when()
                .get()
        .then()
                .statusCode(500);
    }

    private Optional<ConfigMap> findConfigMap() {
        OpenShiftClient client = openshift.client();

        List<ConfigMap> cfm = client.configMaps()
                .inNamespace(client.getNamespace())
                .list()
                .getItems();

        return cfm.stream()
                .filter(m -> CONFIGMAP_NAME.equals(m.getMetadata().getName()))
                .findAny();
    }

}
