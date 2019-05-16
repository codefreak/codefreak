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

# Run everything as unprivileged system-user
RUN adduser -S code-freak
USER code-freak

WORKDIR /app
CMD ["java", "-jar", "/app/code-freak.jar"]
