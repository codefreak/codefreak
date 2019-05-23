FROM openjdk:8-alpine

# Add some system dependecies required by libraries
# - gcompat for jsass
RUN apk add --no-cache gcompat

COPY . /build

WORKDIR /build
RUN ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false clean bootJar \
    && ./gradlew --stop \
    && rm -r $HOME/.gradle \
    && mv build/libs /app \
    && mv /app/code-freak-*.jar /app/code-freak.jar \
    && rm -r /build /tmp/*

EXPOSE 8080

# Run everything as unprivileged user
RUN addgroup -g 1000 code-freak \
    && adduser -Su 1000 -G code-freak code-freak \
    && chown -R code-freak:code-freak /app
USER code-freak

# Override this when running the container with -e SPRING_PROFILES_ACTIVE="dev"
ENV SPRING_PROFILES_ACTIVE "prod"

WORKDIR /app
CMD ["java", "-jar", "/app/code-freak.jar"]
