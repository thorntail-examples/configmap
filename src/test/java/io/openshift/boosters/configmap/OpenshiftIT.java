package io.openshift.boosters.configmap;

import java.io.File;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static io.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
@RunAsClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenshiftIT {

    private static final String APPLICATION_NAME = System.getProperty("app.name");

    private static final OpenshiftTestAssistant openshift = new OpenshiftTestAssistant(APPLICATION_NAME);

    private static final String CONFIGMAP_NAME = "app-config";

    private static String API_ENDPOINT;

    @BeforeClass
    public static void setup() throws Exception {

        Assert.assertNotNull(APPLICATION_NAME);

        // pre-requisite for swarm
        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config.yml"));

        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();

        API_ENDPOINT = openshift.getBaseUrl() + "/api/greeting";
    }

    @AfterClass
    public static void teardown() throws Exception {
       openshift.cleanup();
    }

    @Test
    public void test_A_ConfigMapExists() throws Exception {

        Optional<ConfigMap> configMap = findConfigMap();
        Assert.assertTrue(configMap.isPresent());
    }

    @Test
    public void test_B_ConfigSourcePresent() {

        expect().
            statusCode(200).
            body(containsString("Hello World!")).
        when().
            get(API_ENDPOINT);
    }

    @Test

    public void test_B_MissingConfigurationSource() {

        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config-broken.yml"));
        openshift.rolloutChanges(APPLICATION_NAME);

        openshift.awaitApplicationReadinessOrFail();

        expect().
                statusCode(500).
                when().
                get(API_ENDPOINT);
    }

    private Optional<ConfigMap> findConfigMap() {

        OpenShiftClient client = openshift.getClient();

        List<ConfigMap> cfm = client.configMaps()
                .inNamespace(client.getNamespace())
                .list()
                .getItems();

        return cfm.stream()
                .filter(m -> CONFIGMAP_NAME.equals(m.getMetadata().getName()))
                .findAny();
    }

}
