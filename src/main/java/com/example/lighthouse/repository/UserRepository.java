package com.example.lighthouse.repository;

import com.example.lighthouse.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // You can add custom queries here
}
