package pzn.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

@SpringBootTest
public class StringTest {
    //this test is created to see, if redis template automatically create or not
    @Autowired
    private StringRedisTemplate template;

    @Test
    void redisTemplate() {
        Assertions.assertNotNull(template);
    }

    //test for using value operation
    //cause data structure in redis most of them using String, we can use key-value which
    //available in value operation class
    @Test
    void string() throws InterruptedException {
        ValueOperations<String, String> ops = template.opsForValue();

        ops.set("name", "world", Duration.ofSeconds(2));
        Assertions.assertEquals("world", ops.get("name"));

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        Assertions.assertNull(ops.get("name"));
    }
}
