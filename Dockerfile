FROM confluentinc/cp-kafka-connect-base

# Copy connector JAR
ENV CONNECTOR_JAR_PATH target/fn-kafkaconnect-sink-connector-1.0.jar
COPY $CONNECTOR_JAR_PATH /etc/kafka-connect/jars

# Copy OCI user private key
ARG PRIVATE_KEY_NAME
RUN echo "PRIVATE_KEY_NAME is " $PRIVATE_KEY_NAME
COPY $PRIVATE_KEY_NAME /etc/kafka-connect/secrets/$PRIVATE_KEY_NAME