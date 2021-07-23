package com.example.scheduler.Configuration;

import com.example.scheduler.Model.ChildTask;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.BatchLoggingErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${scheduler.group_batch}")
    private String groupBatch;
    @Value("${sprint.kafka.maxpoll}")
    private int maxPollRecords;

    @Bean
    public ProducerFactory producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory(props);
    }

    @Bean
    public KafkaTemplate<String, ChildTask> childTaskKafkaTemplate(){
        return new KafkaTemplate<String, ChildTask>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, ChildTask> childKafkaListenerContainerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupBatch);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.format("%d", maxPollRecords));
        return new DefaultKafkaConsumerFactory<String, ChildTask>(props, new StringDeserializer(), new JsonDeserializer<>(ChildTask.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChildTask> childConsumerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChildTask> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(childKafkaListenerContainerFactory());
        factory.setBatchListener(true);
        factory.setBatchErrorHandler(new BatchLoggingErrorHandler());
        return factory;
    }
}
