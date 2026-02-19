package com.devpro.code_runner_service.config.redis_configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;


@Configuration
public class RedisListenerConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisExpirationListener listener) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        MessageListenerAdapter adapter =
                new MessageListenerAdapter(listener);

        container.addMessageListener(
                adapter,
                new PatternTopic("__keyevent@0__:expired")
        );

        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisExpirationListener listener) {
        return new MessageListenerAdapter(listener);
    }


}



