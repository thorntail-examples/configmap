package org.obsidiantoaster.quickstart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obsidiantoaster.quickstart.service.GreetingController;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.arquillian.CreateSwarm;
import org.wildfly.swarm.jaxrs.JAXRSArchive;
import org.wildfly.swarm.jaxrs.JAXRSFraction;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
public class JAXRSArquillianTest {

    @Deployment(testable = false)
    public static Archive createDeployment() throws Exception {
        JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class, "rest-example.war");
        deployment.addPackage(GreetingController.class.getPackage());
        deployment.addAllDependencies();
        return deployment;
    }

    @CreateSwarm
    public static Swarm newContainer() throws Exception {
        return new Swarm().fraction(new JAXRSFraction());
    }

    @Test
    @RunAsClient
    public void testResource() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8080").path("greeting");

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.readEntity(String.class).contains("Hello, World!"));
    }

}
