node("launchpad-maven") {
  checkout scm
  stage("Install ConfigMap") {
    sh "if ! oc get configmap app-config -o yaml | grep app-config.yml; then oc create configmap app-config --from-file=app-config.yml; fi"
  }
  stage("Build and Deploy") {
    sh "mvn fabric8:deploy -Popenshift -DskipTests"
  }
}