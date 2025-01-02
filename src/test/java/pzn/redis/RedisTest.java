package pzn.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

@SpringBootTest
public class RedisTest {
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

    //test for using and manipulate list operation
    @Test
    void list() {
        //opForList() is method for list operation in redis template
        ListOperations<String, String> ops = template.opsForList();
        //to add list in the right, using rightPush, names as key, and value with the data
        ops.rightPush("names", "fin");
        ops.rightPush("names", "world");
        ops.rightPush("names", "fi");
        //the data of the list will be fin,world,fi

        //to get data and release from the left, using leftPop
        Assertions.assertEquals("fin", ops.leftPop("names"));
        Assertions.assertEquals("world", ops.leftPop("names"));
        Assertions.assertEquals("fi", ops.leftPop("names"));


    }
}
