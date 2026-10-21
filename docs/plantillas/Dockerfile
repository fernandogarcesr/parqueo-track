# =====================================================================================
# PLANTILLA para la Fase 9. Cópiala a la RAÍZ del repo con el nombre "Dockerfile"
# (sin extensión). Un Dockerfile es la receta para construir una imagen.
# =====================================================================================

# ---- Etapa 1: BUILD (aquí compilo; esta etapa NO llega a la imagen final) ----
# "AS build" le pone nombre a la etapa para poder referirme a ella después.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copio todo el repo (lo que ponga en .dockerignore se queda fuera)
COPY . .
# -pl parqueo-api = solo este módulo | -am = también lo que necesita (el POM padre)
# -DskipTests = las pruebas ya corrieron en CI; aquí no las repito (decisión consciente)
RUN mvn -B package -DskipTests -pl parqueo-api -am

# ---- Etapa 2: RUNTIME (lo único que viaja) ----
# JRE = solo lo necesario para EJECUTAR Java (sin compilador). alpine = Linux mínimo.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Traigo SOLO el .jar de la etapa anterior: sin Maven, sin JDK, sin código fuente.
COPY --from=build /app/parqueo-api/target/*.jar app.jar
# Documenta el puerto (no lo abre; lo abre el -p al hacer docker run)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
