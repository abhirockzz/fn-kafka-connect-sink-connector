# Kafka Connector to invoke functions (using OCI Signature)

(experimential) Kafka Sink Connector which invokes Functions using OCI Signature

**TL;DR**

- setup and start your connector
- drop messages in your kafka topic ...
- ... your function(s) gets invoked (with the payload which your sent to Kafka)



It is assumed that you've already deployed your function 

## Infra setup

> This is not required if your Kafka cluster is already set up - just point Kafka Connect to the existing Kafka cluster in that case

### Zookeeper

- Start Zookeeper - `docker run -d --rm --name zk --net=host -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper`
- Check - `docker logs zk`

		[2018-11-02 17:42:30,151] INFO tickTime set to 3000 (org.apache.zookeeper.server.ZooKeeperServer)
		[2018-11-02 17:42:30,151] INFO minSessionTimeout set to -1 (org.apache.zookeeper.server.ZooKeeperServer)
		[2018-11-02 17:42:30,151] INFO maxSessionTimeout set to -1 (org.apache.zookeeper.server.ZooKeeperServer)
		[2018-11-02 17:42:30,167] INFO Using org.apache.zookeeper.server.NIOServerCnxnFactory as server connection factory (org.apache.zookeeper.server.ServerCnxnFactory)
		[2018-11-02 17:42:30,175] INFO binding to port 0.0.0.0/0.0.0.0:2181 (org.apache.zookeeper.server.NIOServerCnxnFactory)

### Kafka

- Start Kafka - `docker run -d --rm --name kafka --net=host -e KAFKA_BROKER_ID=1 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 confluentinc/cp-kafka`
- Check - `docker logs kafka`

		[2018-11-02 17:43:18,638] INFO Cluster ID: cN5tr6lnQUKnVcpjIeIHbQ (org.apache.kafka.clients.Metadata)
		[2018-11-02 17:43:18,783] INFO Updated PartitionLeaderEpoch. New: {epoch:0, offset:0}, Current: {epoch:-1, offset:-1} for Partition: __confluent.support.metrics-0. Cache now contains 0 entries. (kafka.server.epoch.LeaderEpochFileCache)
		[2018-11-02 17:43:18,809] INFO [Producer clientId=producer-1] Closing the Kafka producer with timeoutMillis = 9223372036854775807 ms. (org.apache.kafka.clients.producer.KafkaProducer)
		[2018-11-02 17:43:18,816] INFO Successfully submitted metrics to Kafka topic __confluent.support.metrics (io.confluent.support.metrics.submitters.KafkaSubmitter)
		[2018-11-02 17:43:20,257] INFO Successfully submitted metrics to Confluent via secure endpoint (io.confluent.support.metrics.submitters.ConfluentSubmitter)

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
		[INFO] Finished at: 2018-11-02T17:45:59Z
		[INFO] Final Memory: 19M/70M
		[INFO] ------------------------------------------------------------------------


- Copy your private key to current folder
- build Kafka Connect container (with custom Connector) - `docker build --build-arg PRIVATE_KEY_NAME=<your_private_key> -t fn-kafka-connect-sink .` e.g. `docker build --build-arg PRIVATE_KEY_NAME=private_key.pem -t fn-kafka-connect-sink .`

		....
		Successfully built bea369ecb8b4
		Successfully tagged fn-kafka-connect-sink:latest

### Install

- Start Kafka Connect in docker - `docker run --rm -it --name=kafka-connect --net=host -e CONNECT_BOOTSTRAP_SERVERS=localhost:9092 -e CONNECT_REST_PORT=8082 -e CONNECT_GROUP_ID="default" -e CONNECT_CONFIG_STORAGE_TOPIC="default.config" -e CONNECT_OFFSET_STORAGE_TOPIC="default.offsets" -e CONNECT_STATUS_STORAGE_TOPIC="default.status" -e CONNECT_KEY_CONVERTER="org.apache.kafka.connect.storage.StringConverter" -e CONNECT_VALUE_CONVERTER="org.apache.kafka.connect.storage.StringConverter" -e CONNECT_INTERNAL_KEY_CONVERTER="org.apache.kafka.connect.json.JsonConverter" -e CONNECT_INTERNAL_VALUE_CONVERTER="org.apache.kafka.connect.json.JsonConverter" -e CONNECT_REST_ADVERTISED_HOST_NAME="localhost" -e CONNECT_PLUGIN_PATH=/usr/share/java,/etc/kafka-connect/jars -e CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1 -e CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1 -e CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1 fn-kafka-connect-sink`

Logs...


		[2018-11-02 17:48:38,256] INFO [Worker clientId=connect-1, groupId=default] (Re-)joining group (org.apache.kafka.clients.consumer.internals.AbstractCoordinator)
		[2018-11-02 17:48:41,329] INFO [Worker clientId=connect-1, groupId=default] Successfully joined group with generation 1 (org.apache.kafka.clients.consumer.internals.AbstractCoordinator)
		[2018-11-02 17:48:41,331] INFO Joined group and got assignment: Assignment{error=0, leader='connect-1-6c8faeb1-5771-4049-9587-866bc4fcc92c', leaderUrl='http://localhost:8082/', offset=-1, connectorIds=[], taskIds=[]} (org.apache.kafka.connect.runtime.distributed.DistributedHerder)
		[2018-11-02 17:48:41,332] INFO Starting connectors and tasks using config offset -1 (org.apache.kafka.connect.runtime.distributed.DistributedHerder)
		[2018-11-02 17:48:41,332] INFO Finished starting connectors and tasks (org.apache.kafka.connect.runtime.distributed.DistributedHerder)

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
