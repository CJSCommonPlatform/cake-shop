# Change Log
All notable changes to this project will be documented in this file, which follows the guidelines
on [Keep a CHANGELOG](http://keepachangelog.com/). This project adheres to
[Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Add jobstore usecase
- Add ITs for validating REPLAY_EVENT_TO_EVENT_LISTENER and REPLAY_EVENT_TO_EVENT_INDEXER command processing
### Changed
- Update framework libraries to 17.6.2 in order to:
  - Update jobstore to process tasks with higher priority first
  - Fix for Jackson single argument constructor issue inspired from  https://github.com/FasterXML/jackson-databind/issues/1498

## [17.0.1] - 2023-12-13
### Changed
- Updated to Junit 5
- Centralise all generic library dependencies and versions into maven-common-bom
- Update to Junit5 and surefire, failsafe plugin versions
- Add retry mechanism to jobstore via framework-libraries
### Fixed
- Fix Logging of missing event ranges to only log on debug
- Limit logging of MissingEventRanges logged to sensible maximum number.
### Added
- New JNDI value `catchup.max.number.of.missing.event.ranges.to.log`
- Add '-f' '--force' switch to the JmxCommandClient to bypass COMMAND_IN_PROGRESS check
### Removed
- Removed dependency on apache-drools as it's not used by any of the framework code
### Security
- Update common-bom to fix various security vulnerabilities in org.json, plexus-codehaus, apache-tika and google-guava


## [17.0.0] - 2023-02-07
### Changed
- Updated to Java 17
- Update common-bom to 17.0.0-M3 in order to:
  - Add byte-buddy 1.12.22 as a replacement for cglib
  - Downgrade h2 to 1.4.196 as 2.x.x is too strict for our tests
- Update framework-libraries to 17.0.0-M4 in order to:
  - Change 'additionalProperties' Map in generated pojos to HashMap to allow serialization
- Update framework-libraries to 17.0.0-M6 in order to:
  - Remove illegal-access argument from surefire plugin
  - Make pojo generator to perform null safe assignment of additionalProperties inside constructor

### Changed
- Update framework-libraries to 11.0.0 for:
    - A default name of `jms.queue.DLQ` rather than the original name of `DLQ`
    - A new constructor to pass the name in if you don't want the default name
    - New builder `MessageConsumerClientBuilder` that allows ActiveMQ connection parameters to be specified
- Updated to java 11 and OpenJdk
- Removed trigger from the event publishing process
- Updated wildfly to 20.0.1-Final  
- Reduced the maximum runtime for each iteration of the publishing beans in the IT tests to 450 milliseconds
- Update to maven-framework-parent-pom 11.0.0
- Update to framework 11.0.0
- Update to event-store 11.0.0
- Bumped the base version of the project to 11.0.0 to match the framework libraries and show java 11 change  
- Handled the move to the new Cloudsmith.io maven repository
- Updated slf4j/log4j bridge jar from slf4j-log4j12 to slf4j-reload4j
- Added Artemis healthcheck
- Downgraded maven minimum version to 3.3.9 until the pipeline maven version is updated
- Add cover all token to travis settings
- Update common bom in order to:
  - Update jboss-logging version to 3.5.0.Final
  - Update jackson libraries to 2.12.7
  - Update mockito version to 4.11.0
  - Update slf4j version to 2.0.6
  - Update hamcrest version to 2.2
   -Update slf4j version to 2.0.6

### Added
- Added support for feature toggling with an integration test showing it working
- Added healthcheck integration test

### Security
- Updates to various libraries to address security alerts:
  - wildfly to version 26.1.2.Final
  - artemis to version 2.20.0
  - resteasy-client to version 4.7.7.Final
  - Update hibernate version to 5.4.24.Final
  - Update jackson.databind version to 2.12.7.1

## [2.0.0] - 2019-08-19
### Added
- Update to framework 6.0.6
- Unified Search indexer module
- Integration test for event catchup
- Integration test for PublishedEvent rebuild.
- Update to event-store 2.0.6
- Update to framework-generators 2.0.4
- Update framework-api to 4.0.1
- Update file.service to 1.17.11
- Update common-bom to 2.4.1
- Update utilities to 1.20.2
- Update test-utils to 1.24.3
- Update json-schema-catalog to 1.7.4

### Changed
- Use a single event-source.yaml in cakeshop-event-source module

## [2.0.0-M3] - 2019-05-09

### Changed
- common-bom -> 2.0.2
- framework -> 6.0.0-M22
- event-store -> 2.0.0-M22
- framework-generators -> 2.0.0-M15
- file.service -> 1.17.7
- framework-api -> 4.0.0-M18
- generator-maven-plugin -> 2.7.0
- json-schema-catalog -> 1.7.0
- raml-maven-plugin -> 1.6.7
- test-utils -> 1.23.0
- utilities -> 1.18.0


### Added
- Integration Test for Event Catchup
### Changed
- Update Shuttering Integration Test
- Remove deprecated github_token entry from travis.yml


## [2.0.0-M2] - 2019-04-08

### Added
- Add Shuttering Integration Test
### Changed
- Update framework-api to 4.0.0-M5
- Update framework to 6.0.0-M10
- Update event-store to 2.0.0-M10
- Update framework-generators to 2.0.0-M8

## [2.0.0-M1] - 2019-03-25

### Changed
- Update framework-api to 4.0.0-M2
- Update framework to 6.0.0-M5
- Update event-store to 2.0.0-M7
- Update framework-generators to 2.0.0-M6
- Update plugins to use a single plugin declaration for each plugin rather that one large plugin with multiple configurations
- Removed framework-domain

## [1.1.0] - 2019-01-09

### Changed
- Update framework-api to 3.1.0
- Update framework to 5.1.0
- Update framework-domain to 1.1.0
- Update event-store to 1.1.0
- Update framework-generators to 1.1.0
- Update utilities to 1.16.2
- Update test-utils to 1.19.1
- Update file-service to 1.17.2
- Update json-schema-catalog to 1.4.3

### Added
- Liquibase script to add events into event_log before startup
- CakeShopReplayEvents IT to test the replaying of events on startup
- SubscriptionEventInterceptor into Event Listener to update Subscription event number

## [1.0.1] - 2018-12-11

### Changed
- Update framework-api to 3.0.1
- Update framework to 5.0.4
- Update framework-domain to 1.0.3
- Update event-store to 1.0.4
- Update framework-generators to 1.0.2
- Use new Enveloper in service components

## [1.0.0] - 2018-11-09

### Added
- Extracted project from cakeshop app in Microservices Framework 5.0.0-M1: https://github.com/CJSCommonPlatform/microservice_framework


