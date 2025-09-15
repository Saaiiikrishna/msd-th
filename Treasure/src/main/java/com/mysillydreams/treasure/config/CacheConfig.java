package com.mysillydreams.treasure.config;

import com.mysillydreams.treasure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port) {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(60));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put(CacheNames.CATEGORIES,   defaultConfig.entryTtl(Duration.ofHours(24)));
        configs.put(CacheNames.SUBCATS,      defaultConfig.entryTtl(Duration.ofHours(24)));
        configs.put(CacheNames.PLAN_DETAIL,  defaultConfig.entryTtl(Duration.ofHours(6)));
        configs.put(CacheNames.PLAN_SEARCH,  defaultConfig.entryTtl(Duration.ofHours(1)));
        configs.put(CacheNames.FILTERS_DICT, defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }
}

