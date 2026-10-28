# syntax=docker/dockerfile:1

# =====================================================================
# security-service · imagen multietapa (S2-08)
# Usuarios, autenticación, roles y emisión de JWT.
#
# Se construye desde la RAÍZ del repositorio, porque el módulo hereda del pom raíz:
#   docker build -f security-service/Dockerfile -t maxiacostag-cloud/security-service:dev .
# =====================================================================

# ---------- Etapa 1: compilación (Maven + JDK 25) ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Maven necesita los pom de todos los módulos para entender el proyecto,
# aunque solo compile este servicio y lo que él necesita.
COPY pom.xml ./pom.xml
COPY common-security/pom.xml ./common-security/pom.xml
COPY config-server/pom.xml ./config-server/pom.xml
COPY shipment-service/pom.xml ./shipment-service/pom.xml
COPY courier-service/pom.xml ./courier-service/pom.xml
COPY security-service/pom.xml ./security-service/pom.xml

# Solo el código de producción: los tests no entran en la imagen, se corren en el CI.
COPY security-service/src/main ./security-service/src/main

# -pl elige el módulo y -am compila también los módulos de los que depende.
# La caché de /root/.m2 evita volver a descargar las dependencias en cada build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl security-service -am package -Dmaven.test.skip=true \
    && cp security-service/target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Etapa 2: ejecución (solo JRE 25, sin Maven ni código fuente) ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

LABEL org.opencontainers.image.title="security-service" \
      org.opencontainers.image.description="LogiTrack · Usuarios, autenticación, roles y emisión de JWT" \
      org.opencontainers.image.source="https://github.com/maxiacostag-cloud/daas-proyecto-final"

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
