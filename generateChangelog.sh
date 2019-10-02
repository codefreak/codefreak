#!/usr/bin/env bash
set -e

date=$(date '+%Y%m%d%H%M%S')
echo "Please enter a summary for this migration: "
read summary
filename=${date}-$(echo "$summary" | sed -e 's/[^A-Za-z0-9._]+/_/g' | tr '[:upper:]' '[:lower:]').yaml
./gradlew clean assemble liquibaseUpdate
./gradlew liquibaseDiffChangelog -PchangeLogFile=src/main/resources/db/changelogs/$filename
cat << EOL >> src/main/resources/db/changelog-master.yaml
- include:
    file: changelogs/$filename
    relativeToChangelogFile: true
EOL
