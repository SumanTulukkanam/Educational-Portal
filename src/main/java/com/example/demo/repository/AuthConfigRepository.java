package com.example.demo.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.model.AuthConfig;
import java.util.Optional;

@Repository
public interface AuthConfigRepository extends JpaRepository<AuthConfig, Long> {

    @Query("SELECT ac FROM AuthConfig ac WHERE ac.isActive = true")
    Optional<AuthConfig> findActiveAuthConfig();

    Optional<AuthConfig> findByAuthMethodAndIsActive(AuthConfig.AuthMethod authMethod, boolean isActive);
}