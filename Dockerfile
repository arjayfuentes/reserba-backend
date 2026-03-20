FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
ENTRYPOINT ["java", "-jar", "target/*.jar"]