package com.quickcommerce.inventory.repository;

import com.quickcommerce.inventory.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
public class ProductRepositoryTest {
    @Autowired
    ProductRepository products;

    @Test
    void findsAProductBySku() {
        products.save(new Product("MILK-1L", "Milk 1L", 6500, 40));
        assertTrue(products.findBySku("MILK-1L").isPresent());
        assertTrue(products.existsBySku("MILK-1L"));
        assertFalse(products.existsBySku("BREAD"));
    }

    @Test
    void rejectsADuplicateSku() {
        products.saveAndFlush(new Product("MILK-1L", "Milk 1L", 6500, 40));
        assertThrows(DataIntegrityViolationException.class,
                () -> products.saveAndFlush(new Product("MILK-1L", "Other", 100, 1)));
    }

}
