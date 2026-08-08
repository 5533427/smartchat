package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.dto.TemplateDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提示词模板 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /** 系统模板 + 我的模板 */
    @GetMapping
    public ApiResponse<TemplateDtos.TemplateGroup> list() {
        return ApiResponse.ok(templateService.list(AuthContext.currentUserId()));
    }

    /** 新建个人模板 */
    @PostMapping
    public ApiResponse<TemplateDtos.TemplateInfo> create(@Valid @RequestBody TemplateDtos.TemplateRequest req) {
        return ApiResponse.ok(templateService.create(AuthContext.currentUserId(), req));
    }

    /** 更新（个人模板本人；系统模板管理员） */
    @PutMapping("/{id}")
    public ApiResponse<TemplateDtos.TemplateInfo> update(
            @PathVariable Long id, @Valid @RequestBody TemplateDtos.TemplateRequest req) {
        return ApiResponse.ok(templateService.update(
                AuthContext.currentUserId(), AuthContext.currentUser().getRole(), id, req));
    }

    /** 删除（个人模板本人；系统模板管理员） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(AuthContext.currentUserId(), AuthContext.currentUser().getRole(), id);
        return ApiResponse.ok();
    }
}
