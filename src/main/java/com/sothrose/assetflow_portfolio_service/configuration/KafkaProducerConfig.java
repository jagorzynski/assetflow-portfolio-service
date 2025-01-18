package com.sothrose.assetflow_portfolio_service.configuration;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

import com.sothrose.assetflow_portfolio_service.model.PortfolioUpdatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {
  @Value("${kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.retries:5}")
  private int retries;

  @Value("${kafka.retries-backoff-in-ms:1000}")
  private int backoff;

  @Bean
  public ProducerFactory<String, PortfolioUpdatedEvent> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    config.put(RETRIES_CONFIG, retries);
    config.put(RETRY_BACKOFF_MS_CONFIG, backoff);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public KafkaTemplate<String, PortfolioUpdatedEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
