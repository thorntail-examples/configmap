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

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.wildfly.swarm.spi.runtime.annotations.ConfigurationValue;

@Path("/")
@ApplicationScoped
public class GreetingEndpoint {

    @Inject
    @ConfigurationValue("greeting.message")
    private Optional<String> message;

    @GET
    @Path("/greeting")
    @Produces("application/json")
    public Response greeting(@QueryParam("name") String name) {
        String suffix = name != null ? name : "World";

        return message
                .map(s -> Response.ok().entity(new Greeting(String.format(s, suffix))).build())
                .orElseGet(() -> Response.status(500).entity("ConfigMap not present").build());

    }

    // Purely for OpenShift Readiness check
    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "pong";
    }
}
