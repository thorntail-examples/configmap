/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openshift.boosters.configmap;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.wildfly.swarm.spi.runtime.annotations.ConfigurationValue;

@Path("/")
@ApplicationScoped
public class GreetingController {

    private static final AtomicLong counter = new AtomicLong();

    @Inject
    @ConfigurationValue("greeting.message")
    Optional<String> message;

    @GET
    @Path("/greeting")
    @Produces("application/json")
    public Response greeting() {

        if(!message.isPresent()) {
            return Response.status(500).build();
        } else {
            return Response.ok()
                    .entity(new Greeting(counter.incrementAndGet(), message.get()))
                    .build();
        }
    }

    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "pong";
    }
}
