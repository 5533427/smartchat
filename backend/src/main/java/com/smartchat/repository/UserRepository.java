package com.smartchat.repository;

import com.smartchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(String role);

    /** 管理后台关键字搜索（用户名或昵称） */
    Page<User> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);
}
