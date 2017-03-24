# Introduction

## Problem Setting

A developer would like to configure an application or the application runtime deployed to Openshift.

## Description

This project demonstrates application and runtime configuration leveraging external configuration sources. Alongside the basic means to set up a configmap and use with a specific runtime, this quickstart also demonstrates how changes to the configuration can be applied to services already deployed to openshift.

## Concepts & Architectural Patterns

ConfigMap, Application Configuration, Rollout of changes

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x
[oc][3] | OpenShift Client | >1.4.1
[git][4] | Git version management | 2.x

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

# Build the Project

The project uses WildflySwarm to create and package the service.

Execute the following maven command:

```
mvn clean install
```

# Launch and test

1. Run the following command to start the maven goal of WildFlySwarm:

    ```
    mvn wildfly-swarm:run
    ```

1. If the application launched without error, use the following command to access the REST endpoint exposed using curl or httpie tool:

    ```
    http http://localhost:8080/api/greeting
    curl http://localhost:8080/api/greeting
    ```

  It should return the value `Hello, World!`, which uses the default greeting template due to the lack of a configmap.
  But in the next step, running on openshift, we'll supply a configmap with a `message` property.

# OpenShift Online

## Login and prepare your openshift account

1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used by the oc client for authentication and project access.

2. Using the `oc` client, execute the following command to replace MYTOKEN with the one from the Web Console:

    ```
    oc login https://api.dev-preview-int.openshift.com --token=MYTOKEN
    ```
3. To allow the WildFly Swarm application running as a pod to access the Kubernetes Api to retrieve the Config Map associated to the application name of the project `swarm-rest-configmap`,
   the view role must be assigned to the default service account in the current project:

    ```
    oc policy add-role-to-user view -n $(oc project -q) -z default
    ```      

## Working with a service that relies on an external configuration source

4. Deploy the configmap to openshift

	```
	oc create configmap app-config --from-file=app-config.yml		
	```

	One you've deployed the configmap, verify it's contents using:

	```
	oc get configmap app-config -o yaml
	```

	This will return something similar to:  

	```
  greeting:
    message: Hello World!
  swarm:
    logging: INFO    
	```

5. Next, use the Fabric8 Maven Plugin to launch the S2I process on the OpenShift Online machine & start the application.

    ```
    mvn clean fabric8:deploy -Popenshift
    ```

6. Get the route url.

    ```
    oc get route/wildfly-swarm-configmap
    NAME              HOST/PORT                                          PATH      SERVICE                TERMINATION   LABELS
    wildfly-swarm-configmap   <HOST_PORT_ADDRESS>             wildfly-swarm-configmap:8080
    ```

7. Use the Host or Port address to access the service.

    ```
    http http://<HOST_PORT_ADDRESS>/api/greeting    
    ```

    Here the response from the REST endpoint should use the greeting template defined in our configmap:

    ```
    {
    "id": 1,
    "message": "Hello World!"
	}
    ```

## Update the configuration and rollout the changes

1. Update the configuration source that's already deployed to Openshift:

	```
	oc edit configmap app-config
	```

	Change the value for the `greeting.message` key to `Bonjour!` and save the file. The changes will be propagated to Openshift.

2. Instruct Openshift to rollout a new version of your services to pick up the changes in the configmap:

	```
	oc rollout latest dc/wildfly-swarm-configmap

	```

	Wait for the pods to become ready:

	```
	oc get pods -w
	```

3. Verify that the changes have been picked up by the Wildfly Swarm service implementation:
	```
	http http://localhost:8080/api/greeting
    curl http://localhost:8080/api/greeting
	```
	This should return the value you've chosen for the key `greeting.message`, i.e.:

	```
	{
    "id": 1,
    "message": "Bonjour!"
	}
	```

Congratulations! You've just finished your first service configuration quickstart.
