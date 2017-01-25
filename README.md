# Introduction

This project demonstrates how to use an [Openshift configmap](https://docs.openshift.org/latest/dev_guide/configmaps.html) as means to configure a Wildfly Swarm service.
The two main attributes that we are going to configure are:

 * the log level for the service
 * a greeting value used to configure the application level.

The service log level can be verified using the Openshift management console (See Application > Pods > Logs).
The application level configuration on the other hand, will be returned from a REST endpoint.

NOTE: When running the service locally, you won't be able to access the greeting due to the lack of a configmap, and the service returns `n/a`. When running on Openshift however, with a configmap provided, the value of the configmap key `service.greeting` will be returned.

You can perform this task in three different ways:

1. Build and launch using WildflySwarm.
1. Build and deploy using OpenShift.
1. Build, deploy, and authenticate using OpenShift Online.

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x
[oc][3] | OpenShift Client | v3.3.x
[git][4] | Git version management | 2.x

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project, you must have an account on an OpenShift Online (OSO): https://console.dev-preview-int.openshift.com/ instance.

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
    http http://localhost:8080/greeting
    curl http://localhost:8080/greeting
    ```

  It should return the value `Hello n/a`, because no greeting value was supplied due to the lack of a configmap.
  But in the next step, running on openshift, we'll supply a configmap with a `service.greeting` property.

# OpenShift Online

1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used by the oc client for authentication and project access.

1. On the oc client, execute the following command to replace MYTOKEN with the one from the Web Console:

    ```
    oc login https://api.dev-preview-int.openshift.com --token=MYTOKEN
    ```
1. Use the Fabric8 Maven Plugin to launch the S2I process on the OpenShift Online machine & start the pod.

    ```
    mvn clean fabric8:deploy -Popenshift  -DskipTests
    ```

1. Get the route url.

    ```
    oc get route/wildfly-swarm-configmap
    NAME              HOST/PORT                                          PATH      SERVICE                TERMINATION   LABELS
    wildfly-swarm-configmap   <HOST_PORT_ADDRESS>             wildfly-swarm-configmap:8080
    ```

1. Use the Host or Port address to access the REST endpoint.
    ```
    http http://<HOST_PORT_ADDRESS>/greeting    
    ```

    Here the response from the Rest endpoint should
    contain the greeting defined in the `src/main/fabric8/configmap.yml`:

    ```
    {
      "id":1,
      "content":"Hello From ConfigMap"
    }
    ```

# Local Openshift Cluster

 Alternatively to the openshift online approach, you might run an [openshift cluster locally](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md)

 ```
 oc cluster up --version=v1.4.0-rc1
 [...]
 OpenShift server started.
   The server is accessible via web console at:
       https://<HOST_PORT_ADDRESS>:8443

   You are logged in as:
       User:     developer
       Password: developer

   To login as administrator:
       oc login -u system:admin
 ```
