package pzn.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class StringTest {
    //this test is created to see, if redis template automatically create or not
    @Autowired
    private StringRedisTemplate template;

    @Test
    void redisTemplate() {
        Assertions.assertNotNull(template);
    }
}
