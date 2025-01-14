package pzn.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductService {

    //too store the data on cache, we can use this annotation:
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(String id) {
        log.info("Get Product {}", id);
        return Product.builder().id(id).name("sample").build();
    }
}
