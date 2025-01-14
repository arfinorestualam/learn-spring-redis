package pzn.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.*;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.support.collections.DefaultRedisMap;
import org.springframework.data.redis.support.collections.RedisList;
import org.springframework.data.redis.support.collections.RedisSet;
import org.springframework.data.redis.support.collections.RedisZSet;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RedisTest {
    //this test is created to see, if redis template automatically create or not
    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

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

    //test for using and manipulate stream data using stream operations
    // 1. testing publish (add data) in stream :
    @Test
    void publish() {
        StreamOperations<String, Object, Object> ops = template.opsForStream();
        MapRecord<String, String, String> record = MapRecord.create("stream-1", Map.of(
                "name", "fin",
                "address", "indonesia"
        ));

        for (int i = 0; i<10; i++) {
            ops.add(record);
        }
    }

    // 2. testing subscribe (read data) in stream :
    @Test
    void subscribe() {
        StreamOperations<String, Object, Object> ops = template.opsForStream();
        try {
            ops.createGroup("stream-1", "sample-group");
        } catch (RedisSystemException exception) {
            //group already exists
        }

        //to read data from first, not last consumed
        List<MapRecord<String, Object, Object>> records = ops.read(Consumer.from("sample-group", "sample-1"),
                StreamOffset.create("stream-1", ReadOffset.from("0")));

        assert records != null;
        for (MapRecord<String, Object, Object> record : records) {
            System.out.println(record);
        }
        // Delete consumer group
        ops.destroyGroup("stream-1", "sample-group");

        // Optionally delete the stream if you no longer need it
        template.delete("stream-1");
    }

    //test for using and manipulate pubsub
    //explanation about different stream listener and pubsub : https://chatgpt.com/share/6783b468-8284-800a-8783-c87b84dfd9f7
    @Test
    void pubSub() {
        Objects.requireNonNull(template.getConnectionFactory()).getConnection().subscribe(
                (message, pattern) ->
                        System.out.println("Message received: " + new String(message.getBody())),
                "my-channel".getBytes());

        for (int i = 0; i < 10; i++) {
            //pubsub doesn't have operations, so use convertAndSend
            template.convertAndSend("my-channel", "Hello Wir: " + i);
            //pubsub doesn't have consumer group, so it'll get data continuously
        }
    }

    //test for using and manipulate java collection list on redis list
    @Test
    void redisList() {
        //this list collection connect to redis using redis list :
        List<String> list = RedisList.create("names", template);
        list.add("fin");
        //if you add to list, it also add to redis
        list.add("budi");
        list.add("kul");
        assertThat(list, hasItems("fin", "budi", "kul"));

        List<String> names = template.opsForList().range("names", 0,-1);

        assertThat(names, hasItems("fin", "budi", "kul"));
        //so if you have collection, you can use redis list to add data, cause it provide java list collection
    }

    //test for using and manipulate java collection set on redis set
    @Test
    void redisSet() {
        Set<String> set = RedisSet.create("traffic", template);
        set.addAll(Set.of("fin", "budi", "kul"));
        set.addAll(Set.of("fun", "budi", "kul"));
        assertThat(set, hasItems("fin", "budi", "kul", "fun"));

        Set<String> members = template.opsForSet().members("traffic");
        assertThat(members, hasItems("fin", "budi", "kul", "fun"));

        //same with the redisList(), RedisSet will connect to Set, when you add data to Set
        //it'll add data to redis too
    }

    //test for using and manipulate java collection set on redisZSet
    @Test
    void redisZSet() {
        RedisZSet<String> set = RedisZSet.create("winner", template);
        set.add("fin", 100);
        set.add("budi", 85);
        set.add("kul", 90);
        assertThat(set, hasItems("fin", "budi", "kul"));

        Set<String> members = template.opsForZSet().range("winner", 0,-1);
        assertThat(members, hasItems("fin", "budi", "kul"));

        assertEquals("fin", set.popLast());
        assertEquals("kul", set.popLast());
        assertEquals("budi", set.popLast());
    }

    //test for using and manipulate java collection map on redisMap
    @Test
    void redisMap() {
        Map<String,String> map = new DefaultRedisMap<>("user:1", template);
        map.put("name", "fin");
        map.put("address", "indonesia");
        assertThat(map, hasEntry("name", "fin"));
        assertThat(map, hasEntry("address", "indonesia"));

        Map<Object,Object> user = template.opsForHash().entries("user:1");
        assertThat(user, hasEntry("name", "fin"));
        assertThat(user, hasEntry("address", "indonesia"));
    }

    //test repository in redis
    @Test
    void repository() {
        Product product = Product.builder()
                .id("1")
                .name("mie")
                .price(20_000L)
                .build();
        productRepository.save(product);

        Map<Object, Object> map = template.opsForHash().entries("products:1");
        System.out.println(map);
        assertEquals(product.getId(), map.get("id"));
        assertEquals(product.getName(), map.get("name"));
        assertEquals(product.getPrice(), map.get("price"));

        Product product2 = productRepository.findById("1").get();
        assertEquals(product, product2);
    }

    //test ttl in redis
    @Test
    void ttl() throws InterruptedException {
        Product product = Product.builder()
                .id("1")
                .name("mie")
                .price(20_000L)
                .ttl(3L)
                .build();
        productRepository.save(product);

        assertTrue(productRepository.findById("1").isPresent());
        Thread.sleep(Duration.ofSeconds(5));
        //the data will be erased after 5 Seconds
        assertFalse(productRepository.findById("1").isPresent());
    }

    //test caching to redis
    @Test
    void cache() {
        Cache sample = cacheManager.getCache("scores");
        //to add using put
        assert sample != null;
        sample.put("fin",100);
        sample.put("budi",85);

        //to get data, we can use get, and convert it with what, in this example, we convert it to int
        assertEquals(100, sample.get("fin", Integer.class));
        assertEquals(85, sample.get("budi", Integer.class));

        //to delete using evict
        sample.evict("fin");
        sample.evict("budi");
        assertNull(sample.get("fin", Integer.class));
        assertNull(sample.get("budi", Integer.class));
        //all this operation is run in redis too, it added data, and delete data both in cache and redis
        //why it's run in redis too, caused we implement the prefix on properties for redis, if we change it
        //will run on the type that we choose.
    }
}
