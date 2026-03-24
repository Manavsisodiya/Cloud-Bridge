FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the jar
COPY target/s3-to-drive-bridge-1.0-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# FORCED START: We manually point to the Handler class 
# (Make sure "com.project.Handler" matches your actual package and class name)
ENTRYPOINT ["java", "-cp", "app.jar", "com.project.Handler"]
