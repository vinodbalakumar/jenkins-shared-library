#!/bin/bash

# Build the Spring Boot application
./mvnw clean package

# Build Docker image
docker build -t myapp:${BUILD_NUMBER} .

# Stop and remove existing container (if any)
docker stop myapp || true
docker rm myapp || true

# Run the new container
docker run -d -p 8080:8080 --name myapp myapp:${BUILD_NUMBER}