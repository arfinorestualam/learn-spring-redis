package pzn.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.time.Duration;
import java.util.List;
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

    //test for using and manipulate geo using geo operation
    @Test
    void geo() {
        GeoOperations<String, String> ops = template.opsForGeo();
        ops.add("sellers", new Point(106.822702, -6.177590), "toko a");
        ops.add("sellers", new Point(106.820889, -6.174964), "toko b");

        Distance distance = ops.distance("selers", "toko a", "toko b", Metrics.KILOMETERS);
        assert distance != null;
        assertEquals(0.3543, distance.getValue());

        //to search by radius
        GeoResults<RedisGeoCommands.GeoLocation<String>> sellers = ops
                .search("sellers", new Circle(
                        new Point(106.821825, -6.175105),
                        new Distance(5, Metrics.KILOMETERS)));

        assert sellers != null;
        assertEquals(2, sellers.getContent().size());
        assertEquals("toko a", sellers.getContent().get(0).getContent().getName());
        assertEquals("toko b", sellers.getContent().get(1).getContent().getName());

        template.delete("sellers");
    }

    //test for using and manipulate hyperloglog using hyperloglog operation
    @Test
    void hyperLogLog() {
        HyperLogLogOperations<String, String> ops = template.opsForHyperLogLog();
        ops.add("traffics", "fin", "fi", "if");
        ops.add("traffics", "fin", "end", "kul");
        ops.add("traffics", "end", "kul", "luk");
        //same with set, if the value has been added, it'll not count the same data
        //the different is, we only now the total of unique data, but we can get the data

        assertEquals(6L, ops.size("traffics"));
        template.delete("traffics");
    }

    //test for using and manipulate transaction
    //in transaction the connection to redis must be same, one connection per transaction, or it'll fail
    @Test
    void transaction() {
        //using this execute so we run the transaction in one connection
        template.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                redisOperations.opsForValue().set("test1", "fin", Duration.ofSeconds(2));
                redisOperations.opsForValue().set("test2", "budi", Duration.ofSeconds(2));
                //you must execute using this :
                redisOperations.exec();
                return null;
            }
        });

        assertEquals("fin", template.opsForValue().get("test1"));
        assertEquals("budi", template.opsForValue().get("test2"));
        template.delete("test1");
        template.delete("test2");
    }

    //test for using and manipulate pipeline
    @Test
    void pipeline() {
        //so make a list that will execute using pipeline but different from transaction
        //there is no multi() or exec()
        List<Object> list = template.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.opsForValue().set("test1", "fin", Duration.ofSeconds(2));
                redisOperations.opsForValue().set("test2", "budi", Duration.ofSeconds(2));
                redisOperations.opsForValue().set("test3", "kul", Duration.ofSeconds(2));
                redisOperations.opsForValue().set("test4", "fin", Duration.ofSeconds(2));
                return null;
            }
        });

        assertThat(list, hasSize(4));
        assertThat(list, hasItem(true));
        assertThat(list, not(hasItem(false)));
    }
}
