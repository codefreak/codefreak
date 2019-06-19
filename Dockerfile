FROM gradle:jdk8 AS build

ARG GIT_COMMIT=""
ARG GIT_TAG=""

COPY . /build

WORKDIR /build
RUN ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false clean bootJar

FROM openjdk:8-alpine

# Add some system dependecies required by libraries
# - gcompat for jsass
RUN apk add --no-cache gcompat

EXPOSE 8080

COPY --from=build /build/build/libs/ /app

# Run everything as unprivileged user
RUN addgroup -g 1000 code-freak \
    && adduser -Su 1000 -G code-freak code-freak \
    && chown -R code-freak:code-freak /app
USER code-freak

# Override this when running the container with -e SPRING_PROFILES_ACTIVE="dev"
ENV SPRING_PROFILES_ACTIVE "prod"
ENV SENTRY_ENVIRONMENT "prod"
ENV SENTRY_RELEASE "${GIT_TAG}"

WORKDIR /app
CMD ["java", "-jar", "/app/code-freak.jar"]
