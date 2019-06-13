Code FREAK<br><small>╰ Code Feedback, Review & Evaluation Kit</small>
======

[![Build Status](https://travis-ci.com/code-freak/code-freak.svg?branch=master)](https://travis-ci.com/code-freak/code-freak)
[![Coverage Status](https://coveralls.io/repos/github/code-freak/code-freak/badge.svg?branch=master)](https://coveralls.io/github/code-freak/code-freak?branch=master)
[![Known Vulnerabilities](https://snyk.io/test/github/code-freak/code-freak/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/code-freak/code-freak?targetFile=build.gradle)
[![Docker Image](https://img.shields.io/docker/cloud/build/cfreak/code-freak.svg)](https://hub.docker.com/r/cfreak/code-freak)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-informational.svg)](https://www.gnu.org/licenses/agpl-3.0)

Code FREAK is an online IDE for educational purposes that allows teachers to create and enroll programming tasks
allows students to programm and execute their code without leaving the browser. Submissions can be evaluated
automatically by various testing-methods like unit tests or code smell detection.

## ⚠️ WORK IN PROGRESS ⚠️
Code FREAK is currently under heavy development. This means things can break without any further notice.
Please keep this in mind when testing the application.

## Development environment setup
Create a file `src/main/resources/application-dev.yml`. For documentation on how to configure the
server see [application.yml](https://github.com/code-freak/code-freak/blob/master/src/main/resources/application.yml)
in the same directory. Minimum configuration that uses the in-memory database:
```yaml
spring:
  jpa:
    database: HSQL
    hibernate:
      ddl-auto: create
```

### Database
You can either use the embedded HSQL storage or a PostgreSQL database. Data from the HSQL database will get lost when
the application shuts down. For Postgres create at least a dedicated database and adjust the configuration accordingly:
```yaml
spring:
  datasource:
    url: "jdbc:postgresql://[host]:[port]/database"
    username: user
    pasword: supersecure
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

### Docker
For many parts of the application we need connection to a (dedicated) Docker daemon. By default we use the default
socket for your platform. If you are on Linux please follow the installation guidelines for your distribution.
For Windows, MacOS and other OS that cannot run Docker natively you will either need the official Docker for Windows/MacOS
software stacks or use the Vagrant machine that is included in this repository. The Vagrant machine will make the Docker
daemon available at `127.0.0.1:2375` (the official Docker port). If you setup Docker for Windows/MacOS the Daemon should
be reachable on the same address. So if you are on Windows or MacOS adjust your `application-dev.properties` file so it points
at a Docker daemon.
```yaml
code-freak:
  docker:
    host: "tcp://127.0.0.1:2375"
```

### Run
Run the command `./gradlew bootRun`. The application is started at `http://localhost:8080`.

### Fix linting issues
```console
$ ./gradlew spotlessApply
```

## License
    Code FREAK | Code Feedback Review & Evaluation Kit
    Copyright (C) 2019 Kiel University of Applied Sciences
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
    
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
