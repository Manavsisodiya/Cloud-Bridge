# Use a standard Java 17 runtime instead of Lambda
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the jar file (Ensure this name matches your SNAPSHOT jar name)
COPY target/s3-to-drive-bridge-1.0-SNAPSHOT.jar app.jar

# Expose the port Spring Boot uses
EXPOSE 8080

# Run the jar as a standard web application
ENTRYPOINT ["java", "-jar", "app.jar"]
