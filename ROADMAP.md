Code FREAK Roadmap
===
We still have many ideas for improving Code FREAK that we collect in this document. There are three categories:
* **Top priority**: We definitely need this feature and will start working on this next
* **Mid priority**: Cool features that should land in CF soon
* **Nice to have features**: Some preliminary ideas with unsure value for CF

Please check the Git history of this file to determine when it was modified last.

**Anything missing? Feel free to open an issue on GitHub!**

🔥 Top priority
---

### Better file manager for students and teachers
The current file upload and download is done by a combination of Ants Upload Component and the file viewer.
You can only upload single files that overwrite everything or archives with a full directory structure.
It lacks basic file operations like uploading or deleting single files, creating directories or moving files around.
We would love to see a full featured file list like Cloud file storages like Dropbox, OwnCloud, Nextcloud, Seafile, etc.
have in their browser view.

This list should be created as a standalone React component and should offer the following features:
* Upload files via drag&drop and file browsing
* Upload full directory structures without archiving
* Delete single/multiple files and directories
* Create new directories
* Move files and directories around
* Rename files and directories
* Download individual files

The new file manager should be used by teachers to manage task files and by students to manage their answer files.

### New evaluation runner model with support for Win/MacOS
Evaluation execution is part of the backend application currently and relies on the connection to a Docker daemon.
For each EvaluationStep/Runner combination a container with an idling process is created and all evaluation commands
are run via Exec on the container. Our current "Runners" are responsible for both execution and parsing of results.

The problems of our current model are:
* No horizontal scaling. Code FREAK is connected to exactly one Docker daemon. Even if there is a dedicated Docker daemon
you can only scale vertically.
* Evaluation can only be run in Linux environments. There is no way to evaluate on Windows or MacOS.
* One Docker instance for all. Teachers can not use their individual systems for evaluation.
* No shell environment. Because commands are run via `exec` you cannot set environment variables or run commands that
rely on a shell environment (`cd`, etc.).
* No custom runners for feedback. At the moment a custom runner can only be created with the JUnit runner by creating
some a valid JUnit XML file. Teachers should be able to introduce their own way of generating automated feedback.

The solution to many of these problems is a standalone runner/agent/worker model like GitLab has. Beside the Code FREAK backend
application there are 1-n workers that connect to the backend application via an API. The workers ask the backend
application for work and report back after they finished execution.

In addition to the runner model there should be different "parsers" that will handle the runners output. Our current
"Runners" will be renamed to "Parsers", and they are only responsible for transforming the execution result into
feedback. Maybe we can also put the parsers into individual containers and create a pipe like `Runner -> Parser -> Database`.

### Manual grading of results
The current output of Code FREAK is only some textual feedback (automatically or manually), but it lacks support for
grading. Auto grading is no top priority, but teachers should at least be able to manually add a score to students
answers/submissions.

Our `Task` entity already has a "weight" property that allows different weighting of scores per task. What is missing
is:
* Add a field to UI to modify the weight property for each task
* Allow to set a score for each answer
* Calculation and display of the final grade per submission
* (opt.) send a mail to student that his submission has been graded

An additional cool feature would be a "review mode" where a teacher can click through all submissions and view evaluation
results and files to give his final grade.

### Better LTI 1.3 integration with scoring support
Code FREAK's current LTI integration only uses the deep linking and SSO features of LTI 1.3.
There is an additional feature that allows writing back scores to the LMS if a student accesses an assignment via
LMS resource link.

A better integration should do the following:
* Only allow access to assignments via LTI resource link (at least the first access to check permissions and create a connection to the LMS)
* Write (manual) scoring results back to the LMS

Depends on: Manual grading of results

### Kubernetes support
Evaluation and IDE currently relies heavily on Docker and the Docker API. We need some form of abstraction for the evaluation
runners that allows using different forms of container engines and platforms. Beside Docker there should be support
for at least Kubernetes.

When support for Kubernetes is integrated we should also offer an official Helm chart to deploy Code FREAK.

🔜 Mid priority
---

### Make evaluation results only visible to teachers
Students tend to focus on "making tests green" and not on creating a good solution to the problem. It should be possible
for teachers to hide evaluation results from students. The student sees if their result compiles (throws no error) but
not the individual feedback. Only the teacher can see the feedback for grading.

### Git integration with OAuth and hooks
We need a Git integration for the most popular Git servers (GitHub, GitLab, Bitbucket) that offers secure OAuth support
and handles webhooks to automatically import changes. Students can use this to manage their answers in a Git repository
and teachers can use it to version their tasks.


✨ Nice to have features
---
### Parametrized/Random tasks
Teachers should be able to define placeholders of various types (strings, numbers, …) and different sources (lists, RNG, …).
These placeholders can be used inside files (replace) and during evaluation (environment variables) to create slightly different
tasks for each student. The actual value will be generated when the initial answer is created from the template.

### Organize students and assignments in classroom
Teachers currently share their assignments by publishing the unique link. Everyone with the link (and an account) can
access the assignment and create submissions. There is no way to define a list of students that must complete an assignment.

Because Code FREAK is just an LTI tool and will be used with an LMS this feature is not high priority.
The LMS should manage students, permissions and tasks.
If we have better LTI 1.3 support with scoring there should be no need to manage classrooms and/or students in Code FREAK.

### Auto grading
This may sounds nice, but the problem with auto grading is that it will never be fair.
Basically, this should somehow convert the evaluation results to a grade between 0-100%.
It should be possible to configure partial auto grading where n% are graded automagically and m% is a manual score from
the teacher.

### Internationalization (i18n) and Localization (l10n)
I18n is not a one-shot feature but requires continuous effort to keep translations up to date.
Lingua franca in computer science is English, so we have to discuss if we really need to support individual languages
for a system like Code FREAK.
L10n should primarily focus on correct time zones and date/number formats.

### Gamification / Motivational elements
Give students some motivation to finish or enhance his answers. Could be based on statistics like Lines of Code / day,
history of failed/successful tests, etc.

### Crowd Feedback/Grading
Students can give feedback to other students work after the deadline has been reached.
This improves their review skills and shows them other solutions to problems.

### Peer Feedback/Grading
Teachers can configure a set of students (e.g. Master students or higher semester students) for each assignment
that will be responsible for grading the answers. The reviewer students receive a notification when grading is required
and land in their own "review mode". The final grade will always be decided by the teacher.
