package pzn.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

//not only stream that can make listener, pubsub also can make listener
//the difference in stream we implement stream listener, and in pubsub we implement
//message listener
@Slf4j
@Component
public class CustomerListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("Receive message: {}", new String(message.getBody()));
    }
}
