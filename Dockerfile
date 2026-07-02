FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw
RUN ./mvnw -B -s .github/maven-settings.xml clean package -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app

ENV SERVER_PORT=8080

RUN mkdir -p /app/uploads
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
