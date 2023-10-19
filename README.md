<!-- Checklist: https://github.com/micronaut-projects/micronaut-core/wiki/New-Module-Checklist -->

# Micronaut test resources

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.testresources/micronaut-test-resources-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.testresources%22%20AND%20a:%22micronaut-test-resources-core%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-test-resources/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-test-resources/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=micronaut-projects_micronaut-test-resources&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=micronaut-projects_micronaut-test-resources)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

Micronaut Test Resources adds support for automatic test resources provisioning during development or test execution.

Test resources are a generic concept but in most cases, the [Testcontainers](https://www.testcontainers.org) library will be used under the hood.
For example, Micronaut Test Resources can automatically spawn a [MySQL test container for integration tests](https://www.testcontainers.org/modules/databases/mysql/).

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-test-resources/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-test-resources/snapshot/guide/) for the current development docs.

<!-- ## Examples

Examples can be found in the [examples](https://github.com/micronaut-projects/micronaut-test-resources/tree/master/examples) directory. -->

## Snapshots and Releases

Snapshots are automatically published to [Sonatype Snapshots](https://s01.oss.sonatype.org/content/repositories/snapshots/io/micronaut/) using [Github Actions](https://github.com/micronaut-projects/micronaut-test-resources/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-test-resources/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/micronaut-test-resources/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-test-resources/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/micronaut-test-resources/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!