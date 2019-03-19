FROM openjdk:8-alpine

COPY . /build

WORKDIR /build
RUN ./gradlew clean check bootJar \
    && mv build/libs /app \
    && mv /app/code-freak-*.jar /app/code-freak.jar \
    && rm -r /build /tmp/*

WORKDIR /app
CMD ["java", "-jar", "/app/code-freak.jar"]
