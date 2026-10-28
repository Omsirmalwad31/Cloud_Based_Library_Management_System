FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* && useradd --create-home --shell /bin/bash spring
WORKDIR /app
COPY --from=build /workspace/target/library-backend-*.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl --fail --silent http://127.0.0.1:8081/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
