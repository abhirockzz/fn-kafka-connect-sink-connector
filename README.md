# Kafka Connector to invoke functions

(experimential) Kafka Sink Connector which invokes Functions using [OCI Signature](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/signingrequests.htm). This example uses Docker to run everything including Zookeeper, Kafka and the Kafka Connect

**TL;DR**

- setup and start your connector
- drop messages in your kafka topic ...
- ... your function(s) gets invoked (with the payload which your sent to Kafka)

**Parallelism**

Depends on `tasks.max` and no. of partitions in the (source) Kafka topic. For e.g., if the topic has 5 partitions and `tasks.max` is also 5, then at a given point 5 tasks (in separate) will be spawned in parallel to accept data from partitions and call your functions - which will result in 5 function instances as well

> It is assumed that you've already deployed your function 

## Infra setup

> This is not required if your Kafka cluster is already set up. Just create the `test-sink-topic` and move on to the `Kafka Connect Connector` section

### Zookeeper

- Start Zookeeper - `docker run -d --rm --name zk --net=host -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper`
- Check - `docker logs zk`

### Kafka

- Start Kafka - `docker run -d --rm --name kafka --net=host -e KAFKA_BROKER_ID=1 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 confluentinc/cp-kafka`
- Check - `docker logs kafka`
- Create topic - `docker run --net=host --rm confluentinc/cp-kafka kafka-topics --create --topic test-sink-topic --partitions 4 --replication-factor 1 --if-not-exists --zookeeper localhost:2181`

`Created topic "test-sink-topic".`

- check - `docker run --net=host --rm confluentinc/cp-kafka kafka-topics --describe --topic test-sink-topic --zookeeper localhost:2181`

		Topic:test-sink-topic   PartitionCount:4        ReplicationFactor:1     Configs:
		        Topic: test-sink-topic  Partition: 0    Leader: 1       Replicas: 1     Isr: 1
		        Topic: test-sink-topic  Partition: 1    Leader: 1       Replicas: 1     Isr: 1
		        Topic: test-sink-topic  Partition: 2    Leader: 1       Replicas: 1     Isr: 1
		        Topic: test-sink-topic  Partition: 3    Leader: 1       Replicas: 1     Isr: 1

## Kafka Connect Connector

### Setup...

- Clone this repo and `cd fn-kafka-connect-sink-connector`
- build connector JAR - `mvn clean install`

		[INFO] ------------------------------------------------------------------------
		[INFO] BUILD SUCCESS
		[INFO] ------------------------------------------------------------------------
		[INFO] Total time: 6.207 s

- Copy your private key to current folder
- build Kafka Connect container (with custom Connector) - `docker build --build-arg PRIVATE_KEY_NAME=<your_private_key> -t fn-kafka-connect-sink .` e.g. `docker build --build-arg PRIVATE_KEY_NAME=private_key.pem -t fn-kafka-connect-sink .`

		....
		Successfully built bea369ecb8b4
		Successfully tagged fn-kafka-connect-sink:latest

### Install

> `CONNECT_BOOTSTRAP_SERVERS` can also point to an existing Kafka cluster (make sure you have created the 

- Start Kafka Connect in docker - `docker run --rm -it --name=kafka-connect --net=host -e CONNECT_BOOTSTRAP_SERVERS=localhost:9092 -e CONNECT_REST_PORT=8082 -e CONNECT_GROUP_ID="default" -e CONNECT_CONFIG_STORAGE_TOPIC="default.config" -e CONNECT_OFFSET_STORAGE_TOPIC="default.offsets" -e CONNECT_STATUS_STORAGE_TOPIC="default.status" -e CONNECT_KEY_CONVERTER="org.apache.kafka.connect.storage.StringConverter" -e CONNECT_VALUE_CONVERTER="org.apache.kafka.connect.storage.StringConverter" -e CONNECT_INTERNAL_KEY_CONVERTER="org.apache.kafka.connect.json.JsonConverter" -e CONNECT_INTERNAL_VALUE_CONVERTER="org.apache.kafka.connect.json.JsonConverter" -e CONNECT_REST_ADVERTISED_HOST_NAME="localhost" -e CONNECT_PLUGIN_PATH=/usr/share/java,/etc/kafka-connect/jars -e CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1 -e CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1 -e CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1 fn-kafka-connect-sink`

- keep following info ready 
	- URL of your function and replace `<FUNCTION_URL>` in below command
	- OCI tenant OCID
	- OCI user OCID
	- Public key fingerprint
	- Name of private key
- Install the connector (invoke Kafka Connect REST API with connector configuration)

		curl -X POST \
		  http://localhost:8082/connectors \
		  -H 'content-type: application/json' \
		  -d '{
		  "name": "FnSinkConnector",
		  "config": {
		    "connector.class": "io.fnproject.kafkaconnect.sink.FnSinkConnector",
		    "tasks.max": "2",
		    "topics": "test-sink-topic",
		    "tenant_ocid": "<tenant_ocid>",
		    "user_ocid": "<user_ocid>",
		    "public_fingerprint": "<public_fingerprint>",
		    "private_key_location": "/etc/kafka-connect/secrets/<private_key_name>",
		    "function_url": "<FUNCTION_URL>"
		  }
		}'

- check logs
	
		.......
		Connector started with config {connector.class=io.fnproject.kafkaconnect.sink.FnSinkConnector, tasks.max=2, topics=test-sink-topic, tenant_ocid=<tenant_ocid>, function_url=<FUNCTION_URL>, public_fingerprint=<public_fingerprint>, private_key_location=/etc/kafka-connect/secrets/private_key.pem, name=FnSinkConnector, user_ocid=<user_ocid>}
		....
		Task started with config... {connector.class=io.fnproject.kafkaconnect.sink.FnSinkConnector, task.class=io.fnproject.kafkaconnect.sink.FnInvocationTask, tasks.max=2, topics=test-sink-topic, tenant_ocid=<tenant_ocid>, function_url=<FUNCTION_URL>, public_fingerprint=<public_fingerprint>, private_key_location=/etc/kafka-connect/secrets/private_key.pem, name=FnSinkConnector, user_ocid=<user_ocid>}
		[2018-11-02 17:52:14,254] INFO WorkerSinkTask{id=FnSinkConnector-1} Sink task finished initialization and start (org.apache.kafka.connect.runtime.WorkerSinkTask)
		Task started with config... {connector.class=io.fnproject.kafkaconnect.sink.FnSinkConnector, task.class=io.fnproject.kafkaconnect.sink.FnInvocationTask, tasks.max=2, topics=test-sink-topic, tenant_ocid=<tenant_ocid>, function_url=<FUNCTION_URL>, public_fingerprint=<public_fingerprint>, private_key_location=/etc/kafka-connect/secrets/private_key.pem, name=FnSinkConnector, user_ocid=<user_ocid>}
		.......
		[2018-11-02 17:52:20,306] INFO [Consumer clientId=consumer-5, groupId=connect-FnSinkConnector] Resetting offset for partition test-sink-topic-3 to offset 0. (org.apache.kafka.clients.consumer.internals.Fetcher)
		Task assigned partition 2 in topic test-sink-topic
		Task assigned partition 3 in topic test-sink-topic
		[2018-11-02 17:52:20,309] INFO [Consumer clientId=consumer-4, groupId=connect-FnSinkConnector] Resetting offset for partition test-sink-topic-0 to offset 0. (org.apache.kafka.clients.consumer.internals.Fetcher)
		[2018-11-02 17:52:20,309] INFO [Consumer clientId=consumer-4, groupId=connect-FnSinkConnector] Resetting offset for partition test-sink-topic-1 to offset 0. (org.apache.kafka.clients.consumer.internals.Fetcher)
		Task assigned partition 0 in topic test-sink-topic
		Task assigned partition 1 in topic test-sink-topic'
		......

## Test

Produce data to topic

`docker run --net=host --rm confluentinc/cp-kafka bash -c "echo -n '{\"name\":\"users\",\"value\":\"kehsihba\"}' | kafka-console-producer --request-required-acks 1 --broker-list localhost:9092 --topic test-sink-topic"`
