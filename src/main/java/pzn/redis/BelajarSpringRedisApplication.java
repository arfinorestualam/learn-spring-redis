package pzn.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

@Slf4j
@SpringBootApplication
@EnableScheduling
//add this on application to enable feature repository on redis:
@EnableRedisRepositories
//to enable feature caching, we need add annotation like this :
@EnableCaching
//this feature for saving data temporarily in memory, like cache, and we can
//integrate with redis, to save the cache automatically
public class BelajarSpringRedisApplication {

    @Autowired
    private StringRedisTemplate redisTemplate;

    //make a bean container that will read and gather all listener that we make
    @Bean(destroyMethod = "stop", initMethod = "start")
    public StreamMessageListenerContainer<String, ObjectRecord<String, Order>> orderContainer(RedisConnectionFactory connectionFactory) {
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(5))
                .targetType(Order.class)
                .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    //make subscription using stream listener that already made, so we can make which group the stream is
    //and register our listener to the container
    @Bean
    public Subscription orderSubscription(StreamMessageListenerContainer<String, ObjectRecord<String, Order>> container,
                                          OrderListener orderListener) {
        try {
            redisTemplate.opsForStream().createGroup("orders", "my-group");
        } catch (Throwable e) {
            //consumer group already exists
        }

        var offset = StreamOffset.create("orders", ReadOffset.lastConsumed());
        var consumer = Consumer.from("my-group", "consumer-1");
        var readRequest = StreamMessageListenerContainer.StreamReadRequest.builder(offset)
                .consumer(consumer)
                .autoAcknowledge(true)
                .cancelOnError(throwable -> false)
                .errorHandler(throwable -> log.warn(throwable.getMessage()))
                .build();

        return container.register(readRequest, orderListener);
    }

    //make listener container for pubsub, the difference between this container and stream listener container
    //the pubsub container can get new data only, but stream, can get whatever data you need (last consumed, or with different clause)

    @Bean
    //yes, it's using bean too
    public RedisMessageListenerContainer messageListenerContainer(RedisConnectionFactory connectionFactory
                                                                  , CustomerListener customerListener) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(customerListener, new ChannelTopic("customers"));
        return container;
        //it's simpler than stream container
        //we don't need to subscribe, cause pubsub will always get data, doesn't have any clause
    }

    public static void main(String[] args) {
        SpringApplication.run(BelajarSpringRedisApplication.class, args);
    }

}
