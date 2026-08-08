package com.smartchat.service;

import com.smartchat.common.BusinessException;
import com.smartchat.config.AiProperties;
import com.smartchat.dto.AuthDtos;
import com.smartchat.dto.ConfigDtos;
import com.smartchat.entity.ApiConfig;
import com.smartchat.entity.User;
import com.smartchat.repository.ApiConfigRepository;
import com.smartchat.repository.UserRepository;
import com.smartchat.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务：注册 / 登录 / 资料 / API 配置
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApiConfigRepository apiConfigRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final AiProperties aiProperties;

    public UserService(UserRepository userRepository,
                       ApiConfigRepository apiConfigRepository,
                       BCryptPasswordEncoder encoder,
                       JwtUtil jwtUtil,
                       AiProperties aiProperties) {
        this.userRepository = userRepository;
        this.apiConfigRepository = apiConfigRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.aiProperties = aiProperties;
    }

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(encoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? req.username() : req.nickname());
        userRepository.save(user);
        return login(new AuthDtos.LoginRequest(req.username(), req.password()));
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!user.isEnabled()) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        return new AuthDtos.LoginResponse(jwtUtil.generate(user), AuthDtos.UserInfo.from(user));
    }

    @Transactional
    public AuthDtos.UserInfo updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow();
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        return AuthDtos.UserInfo.from(userRepository.save(user));
    }

    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    /** 读取用户 AI 配置；未配置时返回系统默认值（用于前端展示） */
    public ConfigDtos.ApiConfigInfo getApiConfig(Long userId) {
        ApiConfig cfg = apiConfigRepository.findByUserId(userId).orElse(null);
        if (cfg == null) {
            return new ConfigDtos.ApiConfigInfo(
                    aiProperties.getProvider(),
                    aiProperties.getBaseUrl(),
                    null,
                    defaultModel(aiProperties.getProvider()));
        }
        return new ConfigDtos.ApiConfigInfo(
                cfg.getProvider(), cfg.getBaseUrl(), maskKey(cfg.getApiKey()), cfg.getModel());
    }

    /** 保存用户 AI 配置（API Key 为明文存储，仅本机部署使用，README 已说明） */
    @Transactional
    public ConfigDtos.ApiConfigInfo saveApiConfig(Long userId, ConfigDtos.ApiConfigRequest req) {
        ApiConfig cfg = apiConfigRepository.findByUserId(userId).orElseGet(() -> {
            ApiConfig c = new ApiConfig();
            c.setUserId(userId);
            return c;
        });
        cfg.setProvider(req.provider());
        cfg.setBaseUrl(req.baseUrl().replaceAll("/+$", ""));
        cfg.setApiKey(req.apiKey().trim());
        cfg.setModel(req.model().trim());
        apiConfigRepository.save(cfg);
        return new ConfigDtos.ApiConfigInfo(cfg.getProvider(), cfg.getBaseUrl(), maskKey(cfg.getApiKey()), cfg.getModel());
    }

    /** 指定 provider 的默认模型 */
    public String defaultModel(String provider) {
        if (!isBlank(aiProperties.getModel())) {
            return aiProperties.getModel();
        }
        return ApiConfig.PROVIDER_ANTHROPIC.equals(provider) ? "claude-opus-4-8" : "deepseek-chat";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** API Key 打码：sk-abc123xyz → sk-a***xyz */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return key;
        }
        return key.substring(0, Math.min(5, key.length() - 4)) + "***" + key.substring(key.length() - 4);
    }
}
