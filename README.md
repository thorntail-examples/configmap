# Thorntail ConfigMap Example

## Purpose

This example demonstrates how to configure applications using Kubernetes ConfigMaps.

## Prerequisites

* Log into an OpenShift cluster of your choice: `oc login ...`.
* Select a project in which the services will be deployed: `oc project ...`.

## Deployment

Run the following commands to configure and deploy the applications.

### Deployment using S2I

```bash
oc apply -f ./.openshiftio/resource.configmap.yaml

oc apply -f ./.openshiftio/application.yaml
oc new-app --template=thorntail-configmap
```

### Deployment with the Fabric8 Maven Plugin

```bash
oc create configmap app-config --from-file=app-config.yml

mvn clean fabric8:deploy -Popenshift
```

## Test everything

This is completely self-contained and doesn't require the application to be deployed in advance.
Note that this may delete anything and everything in the OpenShift project.

```bash
mvn clean verify -Popenshift,openshift-it
```
