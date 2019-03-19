Code FREAK<br><small>╰ Code Feedback Review & Evaluation Kit</small>
======

[![Build Status](https://travis-ci.com/code-freak/code-freak.svg?branch=master)](https://travis-ci.com/code-freak/code-freak)
[![Known Vulnerabilities](https://snyk.io/test/github/code-freak/code-freak/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/code-freak/code-freak?targetFile=build.gradle)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-informational.svg)](https://www.gnu.org/licenses/agpl-3.0)

Code FREAK is an online IDE for educational purposes that allows teachers to create and enroll programming tasks
allows students to programm and execute their code without leaving the browser. Submissions can be evaluated
automatically by various testing-methods like unit tests or code smell detection.

## Development environment setup

1) Set up a database (optional). Currently, drivers for PostgreSQL are included. You can also use the embedded in-memory database.
2) Create a file `src/main/resources/application-dev.properties`. For documentation on how to configure the
   server see [application.properties](https://github.com/code-freak/code-freak/blob/master/src/main/resources/application.properties)
   in the same directory. Minimum configuration that uses the in-memory database:
   ```
   spring.jpa.database=HSQL
   spring.jpa.hibernate.ddl-auto=create
   ```
3) Run the command `./gradlew bootRun`. The application is started at http://localhost:8080.

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
