package com.quickcommerce.inventory.repository;

import com.quickcommerce.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    List<Product> findAllByActiveTrue();
    List<Product> findAllByIdInAndActiveTrue(Set<Long> id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.onHand = p.onHand - :quantity where p.id = :id and p.onHand >= :quantity")
    int decrementOnHand(@Param("id") long id, @Param("quantity") int quantity);
}
