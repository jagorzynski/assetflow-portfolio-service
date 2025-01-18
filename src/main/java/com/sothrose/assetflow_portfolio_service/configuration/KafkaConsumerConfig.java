package com.sothrose.assetflow_portfolio_service.configuration;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES;

import com.sothrose.assetflow_portfolio_service.model.TradeCreatedEvent;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

  @Value("${kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.consumer.group-id}")
  private String groupId;

  @Value("${kafka.consumer.max-poll-interval:30000}")
  private int maxPollInterval;

  @Value("${kafka.consumer.trusted-packages}")
  private String trustedPackages;

  @Value("${kafka.consumer.concurrency:3}")
  private int concurrency;

  @Value("${kafka.consumer.batch-listener:false}")
  private boolean batchListener;

  @Bean
  public ConsumerFactory<String, TradeCreatedEvent> consumerFactory() {
    return new DefaultKafkaConsumerFactory<>(
        Map.of(
            BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            GROUP_ID_CONFIG, groupId,
            KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval,
            TRUSTED_PACKAGES, trustedPackages),
        new StringDeserializer(),
        new JsonDeserializer<>(TradeCreatedEvent.class, false));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent>
      kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    factory.setConcurrency(concurrency);
    factory.setBatchListener(batchListener);
    return factory;
  }
}
