package pzn.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RedisTest {
    //this test is created to see, if redis template automatically create or not
    @Autowired
    private StringRedisTemplate template;

    @Test
    void redisTemplate() {
        assertNotNull(template);
    }

    //test for using value operation
    //cause data structure in redis most of them using String, we can use key-value which
    //available in value operation class
    @Test
    void string() throws InterruptedException {
        ValueOperations<String, String> ops = template.opsForValue();

        ops.set("name", "world", Duration.ofSeconds(2));
        assertEquals("world", ops.get("name"));

        Thread.sleep(Duration.ofSeconds(3).toMillis());
        assertNull(ops.get("name"));
        template.delete("name");
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
        assertEquals("fin", ops.leftPop("names"));
        assertEquals("world", ops.leftPop("names"));
        assertEquals("fi", ops.leftPop("names"));

        template.delete("names");
    }

    //test for using and manipulate set operation
    @Test
    void set() {
        SetOperations<String, String> ops = template.opsForSet();
        //cause data on set can't be same, so if the value we add same they'll read by 1
        //method add to adding key with value to set
        ops.add("students","fin");
        ops.add("students", "fin");
        ops.add("students", "world");
        ops.add("students", "world");
        ops.add("students", "fi");
        ops.add("students", "fi");

        //method member for search set with key that already add
        assertEquals(3, Objects.requireNonNull(ops.members("students")).size());
        assertThat(ops.members("students"), hasItems("fin", "world", "fi"));

        //method to delete all data from redis base on key
        template.delete("students");
    }

    //test for using and manipulate sorted set using z set operation
    @Test
    void zSet() {
        ZSetOperations<String, String> ops = template.opsForZSet();
        //in z set, if we make the value same but the score is different
        //it will treat like different data
        ops.add("score", "eko", 100);
        ops.add("score", "budi", 85);
        ops.add("score", "bri", 90);

        // if we want to take away the highest score we can use popMax.
        // if you want the lowest score use popMin
        assertEquals("eko", Objects.requireNonNull(ops.popMax("score")).getValue());
        assertEquals("bri", Objects.requireNonNull(ops.popMax("score")).getValue());
        assertEquals("budi", Objects.requireNonNull(ops.popMax("score")).getValue());

        template.delete("score");
    }

    //test for using and manipulate hash using hash operation
    @Test
    void hash() {
        HashOperations<String, Object, Object> ops = template.opsForHash();
        ops.put("user:1", "id", "1");
        ops.put("user:1", "name", "fin");
        ops.put("user:1", "email", "fin@bro.com");

        assertEquals("1", ops.get("user:1", "id"));
        assertEquals("fin", ops.get("user:1", "name"));
        assertEquals("fin@bro.com", ops.get("user:1", "email"));

        //if you already make map, use putAll, like this :
//        Map<Object, Object> map = new HashMap<>();
//        map.put("id","1");
//        map.put("name","fin");
//        map.put("email","fin@bro.com");
//        ops.putAll("user:1", map);

        template.delete("user:1");
    }
}
