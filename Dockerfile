FROM openjdk:8-alpine

COPY . /build

WORKDIR /build
RUN ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false clean check bootJar \
    && ./gradlew --stop \
    && rm -r $HOME/.gradle \
    && mv build/libs /app \
    && mv /app/code-freak-*.jar /app/code-freak.jar \
    && rm -r /build /tmp/* \
    && exit 1

WORKDIR /app
CMD ["java", "-jar", "/app/code-freak.jar"]
