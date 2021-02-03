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
package io.thorntail.example;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import io.thorntail.openshift.test.AdditionalResources;
import io.thorntail.openshift.test.AppMetadata;
import io.thorntail.openshift.test.OpenShiftTest;
import io.thorntail.openshift.test.injection.TestResource;
import io.thorntail.openshift.test.util.OpenShiftUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@OpenShiftTest
@AdditionalResources("classpath:test-config.yml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenshiftIT {
    private static final String CONFIGMAP_NAME = "app-config";

    @TestResource
    private OpenShiftClient oc;

    @TestResource
    private OpenShiftUtil openshift;

    @TestResource
    private AppMetadata appMetadata;

    @BeforeAll
    public static void setUp() {
        RestAssured.basePath = "/api/greeting";
    }

    @Test
    @Order(1)
    public void configMapExists() {
        Optional<ConfigMap> configMap = oc.configMaps()
                .list()
                .getItems()
                .stream()
                .filter(m -> CONFIGMAP_NAME.equals(m.getMetadata().getName()))
                .findAny();

        assertTrue(configMap.isPresent());
    }

    @Test
    @Order(2)
    public void defaultGreeting() {
        when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello World from a ConfigMap!"));
    }

    @Test
    @Order(3)
    public void customGreeting() {
        given()
                .queryParam("name", "Steve")
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello Steve from a ConfigMap!"));
    }

    @Test
    @Order(4)
    public void updateConfigGreeting() throws Exception {
        openshift.applyYaml(new File("target/test-classes/test-config-update.yml"));

        openshift.rolloutChanges(appMetadata.name, true);

        when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Good morning World from an updated ConfigMap!"));
    }

    @Test
    @Order(5)
    public void missingConfigurationSource() throws Exception {
        openshift.applyYaml(new File("target/test-classes/test-config-broken.yml"));

        openshift.rolloutChanges(appMetadata.name, false);

        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            when()
                    .get()
            .then()
                    .statusCode(500);
        });
    }
}
