package com.smartchat.repository;

import com.smartchat.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    /** 系统内置模板 */
    List<PromptTemplate> findBySystemTrueOrderByIdAsc();

    /** 某用户的自建模板 */
    List<PromptTemplate> findBySystemFalseAndUserIdOrderByUpdatedAtDesc(Long userId);

    long countBySystemTrue();
}
