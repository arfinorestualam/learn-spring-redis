package pzn.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
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

    //to update data in cache without access to method with annotation Cacheable,
    //we can use @CachePut
    @CachePut(value = "products", key = "#product.id")
    public Product save(Product product) {
        log.info("Save Product {}", product);
        return product;
    }
    //in above example, we want to change id of product but without using getProduct method
    //so if the save method run, it'll update the id of the product.
}
