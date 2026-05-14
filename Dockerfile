# ── Etapa 1: compilar con Maven ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS buildstage

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY src /app/src

RUN mvn clean package -DskipTests

# ── Etapa 2: imagen final ─────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk

COPY --from=buildstage /app/target/ms-docucloud-1.0.0.jar /app/app.jar

# Directorio de montaje para Amazon EFS
RUN mkdir -p /app/efs

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
