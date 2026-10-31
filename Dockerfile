# syntax=docker/dockerfile:1

# =====================================================================
# security-service · imagen multietapa
# Usuarios, autenticación, roles y emisión de JWT.
#
# Se construye desde la raíz de ESTE repositorio (ya no depende de un pom padre ni de otros módulos):
#   docker build -t macostag26/logitrack-security-service:dev .
# =====================================================================

# ---------- Etapa 1: compilación (Maven + JDK 25) ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Primero el pom y después el código: Docker reutiliza la capa de dependencias si solo cambia el código.
COPY pom.xml ./pom.xml
COPY src/main ./src/main

# Solo el código de producción: los tests no entran en la imagen, se corren en el CI.
# La caché de /root/.m2 evita volver a descargar las dependencias en cada build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -Dmaven.test.skip=true \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Etapa 2: ejecución (solo JRE 25, sin Maven ni código fuente) ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

LABEL org.opencontainers.image.title="security-service" \
      org.opencontainers.image.description="LogiTrack · Usuarios, autenticación, roles y emisión de JWT" \
      org.opencontainers.image.source="https://github.com/Nexus-Juy/security-service"

# La aplicación no corre como root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Capas separadas: si solo cambia el código, Docker reutiliza las de dependencias.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8083
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
