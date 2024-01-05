#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests
CONTEXT_NAME=example

FRAMEWORK_LIBRARIES_VERSION=17.3.2
FRAMEWORK_VERSION=17.4.1
EVENT_STORE_VERSION=17.4.1

DOCKER_CONTAINER_REGISTRY_HOST_NAME=crmdvrepo01

LIQUIBASE_COMMAND=update
#LIQUIBASE_COMMAND=dropAll

#fail script on error
set -e

[ -z "$CPP_DOCKER_DIR" ] && echo "Please export CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo (https://github.com/hmcts/cpp-developers-docker) checked out locally" && exit 1
WILDFLY_DEPLOYMENT_DIR="$CPP_DOCKER_DIR/containers/wildfly/deployments"

source $CPP_DOCKER_DIR/docker-utility-functions.sh
source $CPP_DOCKER_DIR/build-scripts/integration-test-scipt-functions.sh

source ${CPP_DOCKER_DIR}/build-scripts/download-jars-for-liquibase-runner-functions.sh
source ${CPP_DOCKER_DIR}/build-scripts/download-liquibase-jar-functions.sh

runLiquibase() {
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runEventBufferLiquibase
  runViewStoreLiquibaseForExampleContext
  runSystemLiquibase
  runEventTrackingLiquibase
  runFileServiceLiquibase
  printf "${CYAN}All liquibase $LIQUIBASE_COMMAND scripts run${NO_COLOUR}\n\n"
}

buildDeployAndTest() {
  loginToDockerContainerRegistry
  #buildWars #ITs execution not skipped
  undeployWarsFromDocker
  buildAndStartContainers
  runLiquibase
  deployWiremock
  deployWarsForExampleContext
  #healthchecks
  integrationTests
}

buildDeployAndTest
