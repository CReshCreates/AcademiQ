package com.softwareprojectmanagement.Repository;

import com.softwareprojectmanagement.Models.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@Email(message="Invalid email format!") String email);

    User findByEmail(String email);
}
