package pzn.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CustomerPublisher {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 10L, timeUnit = TimeUnit.SECONDS)
    public void publishCustomer() {
        //look at this, in pubsub we publish with convert and send, cause pubsub doesn't have operation
        redisTemplate.convertAndSend("customers", "customer " + UUID.randomUUID());
    }
}
