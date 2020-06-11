# CovidEagle Platform

## Requirements

**[Docker](https://www.docker.com/products/docker-desktop)**

## Environment

- Docker
- PowerShell

## Running the platform

- Go to the <code>ida-platform</code> folder
- Execute <code>docker build -t flink-consumer .\flink-consumer\\</code> _(Build a necesary image)_
- Execute <code>docker-compose up -d</code> _(Run the environment)_
- Execute <code>Invoke-RestMethod -Method POST -URI http://localhost:8083/connectors -ContentType application/json -InFile .\connect-mqtt-source.json</code> _(Register the MQTT Connector to Kafka)_

### Publish MQTT messages

- <code>docker run -it --rm --name mqtt-publisher --network ida-platform\*bridge efrecon/mqtt-client pub -h mosquitto -t "covideagle" -m "customer:1"</code> _(Add a customer to the confined area)_

- <code>docker run -it --rm --name mqtt-publisher --network ida-platform\*bridge efrecon/mqtt-client pub -h mosquitto -t "covideagle" -m "customer:-1"</code> _(Remove a customer from the confined area)_

### Check if Kafka received the message

<code>docker exec -it ida-platform_kafka_1 kafka-console-consumer --bootstrap-server kafka:9092 --topic mqtt.covideagle --from-beginning</code>
