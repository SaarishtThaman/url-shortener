FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jdk-alpine
RUN apk add --no-cache postgresql16 redis su-exec
WORKDIR /app
COPY --from=build /app/build/libs/urlshortener-0.0.1-SNAPSHOT.jar app.jar
COPY init.sql /app/init.sql
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
