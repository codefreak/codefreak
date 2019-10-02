#!/usr/bin/env bash
set -e

./gradlew clean assemble liquibaseUpdate
./gradlew liquibaseDiffChangelog -PchangeLogFile=build/db/changelog-diff.yaml
