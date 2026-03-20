FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
RUN mv target/*.jar target/app.jar
ENTRYPOINT ["java", "-jar", "target/app.jar"]