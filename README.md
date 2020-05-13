Code FREAK<br><small>╰ Code Feedback, Review & Evaluation Kit</small>
======

[![Build Status](https://travis-ci.com/codefreak/codefreak.svg?branch=master)](https://travis-ci.com/codefreak/codefreak)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=code-freak_code-freak&metric=alert_status)](https://sonarcloud.io/dashboard?id=code-freak_code-freak)
[![Docker Image](https://images.microbadger.com/badges/version/cfreak/codefreak.svg)](https://microbadger.com/images/cfreak/codefreak)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-informational.svg)](https://www.gnu.org/licenses/agpl-3.0)

Code FREAK is an online IDE for educational purposes that allows teachers to create and enroll programming tasks
allows students to program and execute their code without leaving the browser. Submissions can be evaluated
automatically by various testing-methods like unit tests or code smell detection.

## Try with Docker 🐋

```shell script
docker run --rm --user=root \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -p 8080:8080 \
    cfreak/codefreak
```

The UI is accessible at http://localhost:8080.
Log in using `admin` and password `123`.

This will use you local Docker daemon for evaluation and IDE instances.

## Documentation
For instructions how to run, configure and use the application please find the documentation in the
[`/docs`](docs/README.md) directory or at [docs.codefreak.org](https://docs.codefreak.org).

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
