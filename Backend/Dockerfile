FROM public.ecr.aws/lambda/java:17

# Copy the JAR into the container
COPY target/s3-to-drive-bridge-1.0-SNAPSHOT.jar /tmp/app.jar

# Unzip the JAR directly into the task root so the 'com' folder is at the top level
RUN jar -xf /tmp/app.jar

# Set the entry point (must match your package exactly)
CMD [ "com.project.Handler::handleRequest" ]
