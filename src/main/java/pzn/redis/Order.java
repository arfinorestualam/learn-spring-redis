package pzn.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//make stream listener, cause read data from stream is hard if it's manually
//so we make stream listener, which can read data from stream automatically
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String id;
    private Long amount;
    //this data will be key, and the class will be map data
    // following by value automatically build by spring redis
}
