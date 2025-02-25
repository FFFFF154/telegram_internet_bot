FROM gradle:8.6-jdk17 AS build
WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY src ./src

RUN gradle shadowJar

FROM eclipse-temurin:17
COPY --from=build /app/build/libs/*.jar internetBot-0.0.1-SNAPSHOT-all.jar
ENTRYPOINT ["java", "-jar", "internetBot-0.0.1-SNAPSHOT-all.jar"]