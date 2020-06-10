FROM openjdk:15-jdk-alpine3.11
MAINTAINER David Campos (david.marques.campos@gmail.com)

# Install Bash
RUN apk add --no-cache bash

# Copy resources
WORKDIR /
COPY wait-for-it.sh wait-for-it.sh
COPY ./ida-platform-1.0-SNAPSHOT-jar-with-dependencies.jar ida-platform.jar

# Wait for Zookeeper and Kafka to be available and run application
CMD ./wait-for-it.sh -s -t 30 $ZOOKEEPER_SERVER -- ./wait-for-it.sh -s -t 30 $KAFKA_SERVER -- ./wait-for-it.sh -s -t 60 $ELASTICSEARCH_SERVER -- java -Xmx512m -jar ida-platform.jar
