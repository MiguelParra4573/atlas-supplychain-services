# Se construye desde la raíz de este repo:
#   docker build -f auth-server/Dockerfile -t atlas/auth-server .
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY auth-server/build.gradle.kts auth-server/build.gradle.kts
COPY auth-server/lombok.config auth-server/lombok.config
COPY auth-server/src auth-server/src
RUN ./gradlew :auth-server:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 atlas
COPY --from=build /workspace/auth-server/build/libs/auth-server.jar app.jar
USER atlas
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
