package com.quickcommerce.user.repository;

import com.quickcommerce.user.model.Role;
import com.quickcommerce.user.model.User;
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
public class UserRepositoryTest {
    @Autowired
    UserRepository users;

    @Test
    void findsAUserByEmail() {
        users.save(new User("a@b.com", "hash", Role.CUSTOMER));
        assertTrue(users.findByEmail("a@b.com").isPresent());
        assertTrue(users.existsByEmail("a@b.com"));
        assertFalse(users.existsByEmail("other@b.com"));
    }

    @Test
    void rejectsADuplicateEmail() {
        users.saveAndFlush(new User("a@b.com", "hash", Role.CUSTOMER));
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(new User("a@b.com", "other", Role.RIDER)));
    }
}
