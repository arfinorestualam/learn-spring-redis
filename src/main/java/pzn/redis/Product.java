package pzn.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

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
}
