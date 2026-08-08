package com.smartchat.config;

import com.smartchat.entity.PromptTemplate;
import com.smartchat.entity.User;
import com.smartchat.repository.PromptTemplateRepository;
import com.smartchat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 种子数据：应用启动时自动执行
 * <p>
 * 1. 无用户时创建默认管理员 admin / admin123
 * 2. 无系统模板时创建内置提示词模板
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final UserRepository userRepository;
    private final PromptTemplateRepository templateRepository;
    private final BCryptPasswordEncoder encoder;

    public SeedRunner(UserRepository userRepository,
                      PromptTemplateRepository templateRepository,
                      BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedTemplates();
    }

    private void seedAdmin() {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole(User.ROLE_ADMIN);
        userRepository.save(admin);
        log.info("已创建默认管理员账号: admin / admin123（请尽快修改密码）");
    }

    private void seedTemplates() {
        if (templateRepository.countBySystemTrue() > 0) {
            return;
        }
        List<PromptTemplate> templates = List.of(
                template("翻译助手", "中英互译，保留原意与语气", """
                        你是一位专业的翻译专家，精通中文与英文。
                        请将用户输入的内容翻译成另一门语言：
                        - 若输入为中文，翻译成英文；若输入为英文，翻译成中文；
                        - 保留原文的语气、格式与专业术语；
                        - 只输出译文，不要任何解释。
                        """),
                template("润色助手", "优化文字表达，更流畅自然", """
                        你是一位文字润色专家。请对用户输入的文字进行润色：
                        - 修正语病、错别字与标点问题；
                        - 优化表达，使其更流畅、更自然、更准确；
                        - 保持原文意思与风格不变；
                        - 直接输出润色后的完整文本，不要解释修改了哪里。
                        """),
                template("总结助手", "提炼要点，生成简明摘要", """
                        你是一位内容提炼专家。请对用户输入的内容进行总结：
                        - 提炼核心观点与关键信息，按要点分条输出；
                        - 保持客观，不添加原文没有的内容；
                        - 摘要控制在原文长度的四分之一以内。
                        """),
                template("代码专家", "解释、调试、优化代码", """
                        你是一位资深软件工程师。请针对用户的代码问题提供帮助：
                        - 先复述问题，再给出清晰的解决方案；
                        - 涉及代码时给出可直接运行的完整示例；
                        - 解释关键思路，便于学习者理解。
                        """),
                template("面试官", "模拟技术面试问答", """
                        你是一位严格的面试官。请模拟技术面试：
                        - 根据用户提到的技术方向，从易到难提出 3~5 个问题；
                        - 用户回答后给出点评，并补充标准答案与考察点；
                        - 最后给出评分（满分 10 分）与改进建议。
                        """),
                template("文案写手", "营销文案、朋友圈、短视频脚本", """
                        你是一位资深文案写手。请根据用户的要求创作文案：
                        - 语言有感染力，结构清晰，抓人眼球；
                        - 适合目标场景（朋友圈/短视频/公众号等）；
                        - 提供 2 个不同风格的版本供选择。
                        """)
        );
        templateRepository.saveAll(templates);
        log.info("已创建 {} 个系统提示词模板", templates.size());
    }

    private PromptTemplate template(String name, String description, String systemPrompt) {
        PromptTemplate t = new PromptTemplate();
        t.setName(name);
        t.setDescription(description);
        t.setSystemPrompt(systemPrompt);
        t.setSystem(true);
        return t;
    }
}
