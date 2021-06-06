package com.kafka.simple;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

public class TestKafkaProducer {

	public static void main(String[] args) {

		ProducerRecord<String, String> record = new ProducerRecord("quickstart-events", "val1");
		Properties prop = new Properties();
		prop.put("bootstrap.servers", "localhost:9094");
		prop.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		prop.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		ProducerConfig config = new ProducerConfig(prop);

		Producer<String, String> producer = new KafkaProducer<String, String>(prop);

		for (int i = 0; i < 10; i++)
			producer.send(
					new ProducerRecord<String, String>("quickstart-events", Integer.toString(i), Integer.toString(i)));
		System.out.println("Message sent successfully");
		producer.close();

	}

}
