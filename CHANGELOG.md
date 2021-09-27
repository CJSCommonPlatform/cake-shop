# Change Log
All notable changes to this project will be documented in this file, which follows the guidelines
on [Keep a CHANGELOG](http://keepachangelog.com/). This project adheres to
[Semantic Versioning](http://semver.org/).

## [Unreleased]

## [7.0.0] - 2021-09-27
- Release of the Java 8 version of the framework
- Updated the version to 7.x.x to match the version of the framework  
- Moved to the new Cloudsmith.io maven repository
- Updated to maven-framework-parent-pom 2.0.0
- Updated to framework-libraries 7.2.2
- Updated to framework 7.2.23
- Updated to event-store 7.2.3
- Removed sonar from the release process as it's no longer compatible with java 8
- Added support for feature toggling with an integration test showing it working
- Reduced the maximum runtime for each iteration of the publishing beans in the IT tests to 450 milliseconds

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
- Use a single event-source.yaml in example-event-source module

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
- Extracted project from example app in Microservices Framework 5.0.0-M1: https://github.com/CJSCommonPlatform/microservice_framework


