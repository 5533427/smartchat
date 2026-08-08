package com.smartchat.repository;

import com.smartchat.entity.ApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {

    Optional<ApiConfig> findByUserId(Long userId);
}
