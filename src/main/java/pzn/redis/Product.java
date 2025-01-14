package pzn.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
//if in entity jpa we use @Entity, in redis we use @KeySpace to define it
@KeySpace("products")
public class Product {
    //still we need @Id as the identity
    @Id
    private String id;
    //in redis, the data will be saved like keyspace:id, ex: products:1

    private String name;

    private Long price;

    //Entity time-to-live (TTL): This annotation determines how long the data will exist before being erased.
    //If you set it to -1, the data will never be deleted from Redis.
    @TimeToLive(unit = TimeUnit.SECONDS)
    private long ttl = -1L;
}
