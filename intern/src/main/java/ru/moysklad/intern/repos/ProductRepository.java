package ru.moysklad.intern.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.Product;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    // maybe I will add additional methods for pagination...
}
