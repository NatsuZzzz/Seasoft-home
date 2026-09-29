# ---------- Stage 1: build jar (backend + frontend Html/ trong classpath:/static) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Tai dependency truoc -> doi code khong phai tai lai (cache layer)
COPY seasoft/pom.xml .
RUN mvn -q -B dependency:go-offline

COPY seasoft/src ./src
# Frontend da build san (tailwind.css, vendor/) -> chi can copy vao static
COPY Html ./src/main/resources/static
RUN rm -f src/main/resources/static/css/tailwind.input.css \
    && mvn -q -B package -DskipTests

# ---------- Stage 2: chi JRE + jar ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app

# Hosting free thuong 512 MB RAM -> gioi han heap theo RAM container
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT}/actuator/health" | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
