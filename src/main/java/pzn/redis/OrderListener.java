package pzn.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

//make custom listener from stream listener
@Slf4j
@Component
public class OrderListener implements StreamListener<String, ObjectRecord<String, Order>> {
    //this onMessage will read stream data automatically
    @Override
    public void onMessage(ObjectRecord<String, Order> message) {
        log.info("Received order: {}", message.getValue());
    }
}
