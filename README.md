# Micronaut OpenTelemetry

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.opentelemetry/micronaut-opentelemetry.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.opentelemetry%22%20AND%20a:%22micronaut-opentelemetry%22)
[![Build Status](https://github.com/donbeave/micronaut-opentelemetry/workflows/Java%20CI/badge.svg)](https://github.com/donbeave/micronaut-opentelemetry/actions)

Micronaut OpenTelemetry integrations Micronaut with the OpenTelemetry observability framework.

## Documentation

See the [Documentation](https://donbeave.github.io/micronaut-opentelemetry/latest/guide/) for more information. 

See the [Snapshot Documentation](https://donbeave.github.io/micronaut-opentelemetry/snapshot/guide/) for the current development docs.

## Examples

Examples can be found in the [examples](https://github.com/donbeave/micronaut-opentelemetry/tree/master/examples) directory.

## Snapshots and Releases

Snaphots are automatically published to [Sonatype Snapshots](https://oss.sonatype.org/content/repositories/snapshots/) using [Github Actions](https://github.com/donbeave/micronaut-opentelemetry/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [Github Actions](https://github.com/donbeave/micronaut-opentelemetry/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/donbeave/micronaut-opentelemetry/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/donbeave/micronaut-opentelemetry/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/donbeave/micronaut-opentelemetry/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
