#!/usr/bin/env bash
set -e

./gradlew clean assemble liquibaseUpdate
./gradlew liquibaseGenerateChangelog -PchangeLogFile=build/db/changelog-diff.yaml
