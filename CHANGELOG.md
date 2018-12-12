# Change Log
All notable changes to this project will be documented in this file, which follows the guidelines
on [Keep a CHANGELOG](http://keepachangelog.com/). This project adheres to
[Semantic Versioning](http://semver.org/).

## [Unreleased]

### Changed
- Update framework-api to 3.1.0-M2
- Update framework to 5.1.0-M3
- Update framework-domain to 1.1.0-M3
- Update event-store to 1.1.0-M1
- Update framework-generators to 1.1.0-M1
- Update test-utils to 1.19.0

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


