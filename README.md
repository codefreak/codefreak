# Code FREAK

[![Build Status](https://travis-ci.com/code-freak/code-freak.svg?branch=master)](https://travis-ci.com/code-freak/code-freak)

Code Feedback Review & Evaluation Kit | Educational Online IDE

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
