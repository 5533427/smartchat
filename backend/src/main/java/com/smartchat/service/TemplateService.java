package com.smartchat.service;

import com.smartchat.common.BusinessException;
import com.smartchat.dto.TemplateDtos;
import com.smartchat.entity.PromptTemplate;
import com.smartchat.entity.User;
import com.smartchat.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提示词模板服务
 */
@Service
public class TemplateService {

    private final PromptTemplateRepository templateRepository;

    public TemplateService(PromptTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public TemplateDtos.TemplateGroup list(Long userId) {
        List<TemplateDtos.TemplateInfo> system =
                templateRepository.findBySystemTrueOrderByIdAsc().stream().map(TemplateDtos.TemplateInfo::from).toList();
        List<TemplateDtos.TemplateInfo> mine =
                templateRepository.findBySystemFalseAndUserIdOrderByUpdatedAtDesc(userId).stream()
                        .map(TemplateDtos.TemplateInfo::from).toList();
        return new TemplateDtos.TemplateGroup(system, mine);
    }

    @Transactional
    public TemplateDtos.TemplateInfo create(Long userId, TemplateDtos.TemplateRequest req) {
        PromptTemplate t = new PromptTemplate();
        t.setName(req.name());
        t.setDescription(req.description());
        t.setSystemPrompt(req.systemPrompt());
        t.setSystem(false);
        t.setUserId(userId);
        return TemplateDtos.TemplateInfo.from(templateRepository.save(t));
    }

    @Transactional
    public TemplateDtos.TemplateInfo update(Long userId, String role, Long templateId, TemplateDtos.TemplateRequest req) {
        PromptTemplate t = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));
        checkPermission(userId, role, t);
        t.setName(req.name());
        t.setDescription(req.description());
        t.setSystemPrompt(req.systemPrompt());
        t.setUpdatedAt(LocalDateTime.now());
        return TemplateDtos.TemplateInfo.from(templateRepository.save(t));
    }

    @Transactional
    public void delete(Long userId, String role, Long templateId) {
        PromptTemplate t = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));
        checkPermission(userId, role, t);
        templateRepository.delete(t);
    }

    /** 系统模板仅管理员可改删；个人模板仅本人 */
    private void checkPermission(Long userId, String role, PromptTemplate t) {
        if (t.isSystem() && !User.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "系统模板仅管理员可维护");
        }
        if (!t.isSystem() && !t.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该模板");
        }
    }
}
