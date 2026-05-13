FROM quay.io/strimzi/kafka:latest-kafka-4.2.0
USER root
RUN mkdir -p /opt/kafka/plugins/debezium-mysql \
 && curl -L https://repo1.maven.org/maven2/io/debezium/debezium-connector-mysql/2.6.1.Final/debezium-connector-mysql-2.6.1.Final-plugin.tar.gz \
 | tar -xz -C /opt/kafka/plugins/debezium-mysql
USER 1001