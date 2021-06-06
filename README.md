# Awesome-ApacheKafka


Apache Kafka is an open-source stream processing platform developed by the Apache Software Foundation written in Scala and Java. 
Kafka® is used for building real-time data pipelines and streaming apps. It is horizontally scalable, fault-tolerant, wicked fast, and runs in production in thousands of companies.
The project aims to provide a reach contents and build a open source community.

## download kafka
http://kafka.apache.org/downloads

## zookeeper : 
Apache Kafka uses Zookeeper to store metadata about the Kafka cluster, as well as consumer client details

## kafka broker/ kafka server/ kafka :
A single Kafka server is called a broker. The broker receives messages from producers,
assigns offsets to them, and commits the messages to storage on disk. It also services
consumers, responding to fetch requests for partitions and responding with the messages that have been committed to disk.

Kafka brokers are designed to operate as part of a cluster. Within a cluster of brokers,
one broker will also function as the cluster controller (elected automatically from the
live members of the cluster). The controller is responsible for administrative operations, including assigning partitions to brokers and monitoring for broker failures. A partition is owned by a single broker in the cluster, and that broker is called the leader of the partition. A partition may be assigned to multiple brokers, which will result in the partition being replicated.
This provides redundancy of messages in the partition, such that another broker can take over leadership if there is a broker failure. However, all consumers and producers operating on that partition must connect to the leader.

## topic : 
a topic is similar to a folder in a filesystem, and the events are the files in that folder.
parameters including partion and retention can be set while creating topics.
1. no. of partitions:  default 1 partitions. it can only increased the partition, 
partition counts use for balance the message load across the entire cluster as brokers are added. 
partition can be evenly distributed to broker.

i.e if I want to be able to write and read 1 GB/sec from a topic, and I know each consumer can only process 50 MB/s, then I know I need at least 20 partitions. This way, I can have 20 consumers reading from the topic and achieve 1 GB/sec.

2. retentions: 
    2.1 by time:log.retention.minutes and log.retention.ms after that time logs will be deleted.
    2.2 by size: log.retention.bytes after that size 

3. log segment: As messages are produced to the Kafka broker, they are appended to the current log segment for the partition.
For example, if a topic receives only 100 megabytes per day of messages, and log.segment.bytes is set to the default, it will take 10 days to fill one segment. As messages cannot be expired until the log segment is closed, if log.retention.ms is
set to 604800000 (1 week), there will actually be up to 17 days of messages retained until the closed log segment expires. This is because once the log segment is closed with the current 10 days of messages, that log segment must be retained for 7 days
before it expires based on the time policy

4. message.max.bytes:  max size of message. degault 1 mb 
producer that tries to send a message larger than this will receive an error back from the broker, and the message will not be accepted.

## producer: 
producing the message in kafka by creating ProduceRecord, which include topic we want to send the record to and a value. Optionally we can also specify key and/or a partititon.

producer will serialize that key and value object into ByteArray so they can be sent over the network.

Next data will be sent to partitioner, If we specified partition than partitioner doesn't do anything and simply returns the partition we specified. If we don't then partitioner will choose partition for us (usually produceRecords key).
Once the partition is selected,producer know which topic and partition record will go to.

broker recieve the message It send response if the messages were successfully written in kafka. it will return RecordMetaData with topic , partition and offset .

If failed it will return error. when producer reciewve an error it may re-try sending message few more times. 

### producer properties:
    1. bootstrap.servers: list of host:port of kafka cluster. this list doesn't need to include all broker, since the producer will get more information after the initial connection.
    2. key.serializer: name of the class that will be used to serialize key :- org.apache.kafka.common.serialization.Serializer
    3. value.serializer: name of the class that will be used to serialize value :- org.apache.kafka.common.serialization.StringSerializer

### producer code snipet

private Properties kafkaProps = new Properties();
kafkaProps.put("bootstrap.servers", "broker1:9092,broker2:9092");
kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
kafkaProps.put("value.serializer",
"org.apache.kafka.common.serialization.StringSerializer");
producer = new KafkaProducer<String, String>(kafkaProps);


### three primary methods of sending message
ProducerRecord<String, String> record = new ProducerRecord<>("CustomerCountry", "Precision Products","France");

    1. Fire and forget : send message to server and really don't care if it arrive successfully or not.
        // fire and forget
        try {
            producer.send(record);
        } catch (Exception e) {
            e.printStackTrace();
        }


    2. Synchronous : We send a message, the send() method returns a Future object, and we use get() to wait on the future and see if the send() was successful or not.
        
        try {
            producer.send(record).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    // here we are using Future.get() to wait for a reply from Kafka. This method will throw an exception if the record is not sent successfully to Kafka. If there were no errors, we will get a RecordMetadata object that we can use to retrieve
    the offset the message was written to
    
    3. Asynchronous : We call the send() method with a callback function, which gets triggered when it receives a response from the Kafka broker.


    private class DemoProducerCallback implements Callback {
    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
    }}

    producer.send(record, new DemoProducerCallback());

    // implements the org.apache.kafka.clients.producer.Callback interface, which has a single function— onCompletion() .
    // If Kafka returned an error, onCompletion() will have a nonnull exception. Here we “handle” it by printing, but production code will probably have more robust error handling functions.

### partitions:
ProduceRecords object : topic, key and value

Keys serve two goals: They are addi‐tional information that gets stored with the message, and they are also used to decide which one of the topic partitions the message will be written to. All messages with the same key will go to the same partition.

When the key is null and the default partitioner is used, the record will be sent to one of the available partitions of the topic at random. A round-robin algorithm will be used to balance the messages among the partitions.

kafka used to hash the key and use the result to map the the message in specific partition.

## kafka consumer:
Consumer group:- is a multi-threaded or multi-machine consumption from Kafka topics.

### facts of consumers:
Consumers can join a group by using the samegroup.id.
The maximum parallelism of a group is that the number of consumers in the group ← no of partitions.
Kafka assigns the partitions of a topic to the consumer in a group, so that each partition is consumed by exactly one consumer in the group.

Kafka guarantees that a message is only ever read by a single consumer in the group.
(for this check by running TestKafkaConsumer.java and TestKafkaConsumer1.java)
only one of them consumer will consume message.
if u kill one which was consuming then the idle one will start consuming messages which call rebalance.
consumers in a consumer group share ownership of the partitions in the topics they subscribe to.
Moving partition ownership from one consumer to another is called a rebalance.
-- the consumer loses its current state; if it was caching any data, it will need to refresh its caches—slowing down the application until the consumer sets up its state again.


### how rebalance works in consumers:
1. The way consumers maintain membership in a consumer group and ownership of the partitions assigned to them is by sending heartbeats to a Kafka broker designated as the group coordinator
2. As long as the consumer is sending heartbeats at regular intervals, it is assumed to be
alive, well, and processing messages from its partitions. Heartbeats are sent when the
consumer polls (i.e., retrieves records) and when it commits records it has consumed.
3. If the consumer stops sending heartbeats for long enough, its session will time out
and the group coordinator will consider it dead and trigger a rebalance. If a consumer
crashed and stopped processing messages, it will take the group coordinator a few
seconds without heartbeats to decide it is dead and trigger the rebalance. During
those seconds, no messages will be processed from the partitions owned by the dead
consumer. When closing a consumer cleanly, the consumer will notify the group
coordinator that it is leaving, and the group coordinator will trigger a rebalance
immediately

Consumers can see the message in the order they were stored in the log.

### How Does the Process of Assigning Partitions to Brokers Work?
When a consumer wants to join a group, it sends a JoinGroup request to the group coordinator. The first consumer to join the group becomes the group leader. The leader receives a list of all
consumers in the group from the group coordinator (this will include all consumers that sent a heartbeat recently and which are therefore considered alive) and is responsible for assigning a subset
of partitions to each consumer. It uses an implementation of PartitionAssignor to decide which partitions should be handled by which consumer.

After deciding on the partition assignment, the consumer leader sends the list of assignments to the GroupCoordinator , which sends this information to all the consumers. Each consumer only sees his own assignment the leader is the only client process that has the full list of consumers in the group and their assignments. This process repeats every time a rebalance happens.


### code snipet for consumers


	    Properties props = new Properties();
	    props.put("bootstrap.servers", "localhost:9092");
	    props.put("group.id", group);
	    props.put("enable.auto.commit", "true");
	    props.put("auto.commit.interval.ms", "1000");
	    props.put("session.timeout.ms", "30000");
	    props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
	    props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
	    
	    KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);

	    consumer.subscribe(Arrays.asList(topic)); // topic is name in string

	    while (true) {
		ConsumerRecords<String, String> records = consumer.poll(100);
		for (ConsumerRecord<String, String> record : records)
		    System.out.printf("offset = %d, key = %s, value = %s\n", 
		    record.offset(), record.key(), record.value());
	    } 


# hands on for simple java kafka demo

    open terminal (1) for zookeeper
    >cd kafka_2.13-2.6.2
1. config zookeper properties (kafka_2.13-2.6.2/config/zookeeper.properties) file and start zookeeper
    >bin/zookeeper-server-start.sh config/zookeeper.properties
2. config kafka properties (kafka_2.13-2.6.2/config/zookeeper.properties) start kafka servers
    here I have added 2 properties :- server1 and server2
    which have unique id, logs dir, and port
    > open another terminal (2) (for kafka server/kafka broker)
    >bin/kafka-server-start.sh config/server1.properties 

    > open another terminal (3) (for kafka server/kafka broker)
    >bin/kafka-server-start.sh config/server2.properties 
3. create topic
    > open another terminal (4) (for kafka topic)
    > bin/kafka-topics.sh --create --topic quickstart-events --bootstrap-server localhost:9092
4. write event on topic (start producer)
    > open another terminal (5) (for kafka producer)
    > bin/kafka-console-producer.sh --topic quickstart-events --bootstrap-server localhost:9092
    >This is my first event
    >This is my second event

    -- another way is run TestkafkaProducer.java
5. read the events
    > open another terminal (6) (for kafka consumer)
    > bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092
        o/p
        >This is my first event
        >This is my second event
    
    --another way is run TestKafkaConsumer.java and TestKafkaConsumer2.java





# <img src="https://raw.githubusercontent.com/Awesome-Windows/Awesome/master/media/chrome_2016-06-11_19-02-31.png" alt="table of contents">
- [Documentions](#documentions)
- [Vedios](#vedios)




## Documentions
- [Officail Docs: Apache Kafka](https://kafka.apache.org/)
- [Confluent Blog](https://www.confluent.io/blog/)
- [Kafka The Definitive Guide: Neha Narkhede](/Docs/confluent-kafka-definitive-guide-complete.pdf)
- [Sample Programs for Apache Kafka : MapR Technologies](https://mapr.com/blog/getting-started-sample-programs-apache-kafka-09/)

## Vedios

### Conferences
- [GOTO 2017 • Stream All Things - Patterns of Modern Data Integration • Gwen Shapira](https://www.youtube.com/watch?v=Hjae0Cw9oew)
- [Reducing Microservice Complexity with Kafka and Reactive Streams - by Jim Riecken](https://www.youtube.com/watch?v=k_Y5ieFHGbs)
- [GOTO 2016 • Applications in the Emerging World of Stream Processing • Neha Narkhede](https://www.youtube.com/watch?v=WuBQBTET8Qg&t=3s)
- [ETL Is Dead, Long Live Streams: real-time streams w/ Apache Kafka](https://www.youtube.com/watch?v=I32hmY4diFY&t=14s)
- [Distributed stream processing with Apache Kafka](https://www.youtube.com/watch?v=rXzuZb3fHHM)
- [Developing Real-Time Data Pipelines with Apache Kafka](https://www.youtube.com/watch?v=GRPLRONVDWY)

### Tutorails
- [Apache Kafka® Streams API : Confluent](https://www.youtube.com/channel/UCmZz-Gj3caLLzEWBtbYUXaA/playlists)
- [Apache Kafka Tutorials : Prashant](https://www.youtube.com/playlist?list=PLkz1SCf5iB4enAR00Z46JwY9GGkaS2NON)


