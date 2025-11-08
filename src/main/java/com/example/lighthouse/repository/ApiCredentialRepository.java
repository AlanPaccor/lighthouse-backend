// src/main/java/com/example/lighthouse/repository/ApiCredentialRepository.java
package com.example.lighthouse.repository;

import com.example.lighthouse.Model.ApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiCredentialRepository extends JpaRepository<ApiCredential, String> {
    Optional<ApiCredential> findByProviderAndIsActiveTrue(String provider);
    Optional<ApiCredential> findByProvider(String provider);
}