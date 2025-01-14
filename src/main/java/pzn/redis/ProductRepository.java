package pzn.redis;

import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Repository;

//in redis, for the repository use extend KeyValueRepository as the inheritance
//you don't need added annotation Repository cause KeyValueRepository inheritance from Repository class.
//but I add it as marker
@Repository
public interface ProductRepository extends KeyValueRepository<Product, String> {
}
