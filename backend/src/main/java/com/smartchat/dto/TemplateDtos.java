package com.smartchat.dto;

import com.smartchat.entity.PromptTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 提示词模板 DTO */
public final class TemplateDtos {

    private TemplateDtos() {
    }

    public record TemplateInfo(
            Long id, String name, String description, String systemPrompt,
            boolean system, Long userId, LocalDateTime createdAt
    ) {
        public static TemplateInfo from(PromptTemplate t) {
            return new TemplateInfo(t.getId(), t.getName(), t.getDescription(),
                    t.getSystemPrompt(), t.isSystem(), t.getUserId(), t.getCreatedAt());
        }
    }

    /** 创建/更新模板 */
    public record TemplateRequest(
            @NotBlank(message = "模板名称不能为空")
            @Size(max = 50, message = "模板名称最长 50 字符") String name,

            @Size(max = 200, message = "描述最长 200 字符") String description,

            @NotBlank(message = "系统提示词不能为空")
            @Size(max = 8000, message = "系统提示词最长 8000 字符") String systemPrompt
    ) {
    }

    public record TemplateGroup(java.util.List<TemplateInfo> system, java.util.List<TemplateInfo> mine) {
    }
}
