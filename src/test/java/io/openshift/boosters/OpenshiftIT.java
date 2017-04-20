/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.openshift.boosters;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.openshift.booster.test.OpenShiftTestAssistant;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Heiko Braun
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenshiftIT {

    private static final OpenShiftTestAssistant openshift = new OpenShiftTestAssistant();

    private static final String CONFIGMAP_NAME = "app-config";

    private static String originalBaseUri;

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

        originalBaseUri = RestAssured.baseURI;
        RestAssured.baseURI = RestAssured.baseURI + "/api/greeting";
    }

    @AfterClass
    public static void teardown() throws Exception {
        openshift.cleanup();
    }

    @Test
    public void testAConfigMapExists() throws Exception {
        Optional<ConfigMap> configMap = findConfigMap();
        assertTrue(configMap.isPresent());
    }

    @Test
    public void testBDefaultGreeting() {
        when()
                .get()
                .then()
                .assertThat().statusCode(200)
                .assertThat().body(containsString("Hello World from a ConfigMap!"));
    }

    @Test
    public void testCCustomGreeting() {
        given()
                .queryParam("name", "Steve")
                .when()
                .get()
                .then()
                .assertThat().statusCode(200)
                .assertThat().body(containsString("Hello Steve from a ConfigMap!"));
    }

    @Test
    public void testDUpdateConfigGreeting() throws Exception {
//        openshift.client().configMaps().withName(CONFIGMAP_NAME)
//                .edit()
//                .addToData("app-config.yml", "greeting: message: Good morning %s from an updated ConfigMap!");
//        openshift.rolloutChanges(APPLICATION_NAME);
        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config-update.yml"));

        rolloutChanges();

        when()
                .get()
                .then()
                .assertThat().statusCode(200)
                .assertThat().body(containsString("Good morning World from an updated ConfigMap!"));
    }

    @Test
    public void testEMissingConfigurationSource() throws Exception {
        openshift.deploy(CONFIGMAP_NAME, new File("target/test-classes/test-config-broken.yml"));

        rolloutChanges();

        await().atMost(5, TimeUnit.MINUTES).until(() -> get().then().assertThat().statusCode(500));
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

    private void rolloutChanges() throws InterruptedException {
        String name = openshift.applicationName();

        System.out.println("Rollout changes to " + name);

        openshift.client().deploymentConfigs()
                .inNamespace(openshift.client().getNamespace())
                .withName(name)
                .deployLatest();

        await().atMost(5, TimeUnit.MINUTES).until(() -> openshift.client().deploymentConfigs()
                .inNamespace(openshift.project())
                .withName(name)
                .get()
                .getStatus()
                .getAvailableReplicas() == 2
        );

        await().atMost(5, TimeUnit.MINUTES).until(() -> openshift.client().deploymentConfigs()
                .inNamespace(openshift.project())
                .withName(name)
                .get()
                .getStatus()
                .getAvailableReplicas() == 1
        );

        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                Response response = get(originalBaseUri + "/api/ping");
                return response.getStatusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
