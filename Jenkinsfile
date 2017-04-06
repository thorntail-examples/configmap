node("maven") {
  checkout scm
  stage("Build") {
    sh "mvn clean -DskipTests install"
  }
  stage("Deploy") {
    sh "mvn fabric8:deploy -Popenshift -DskipTests"
  }
}
