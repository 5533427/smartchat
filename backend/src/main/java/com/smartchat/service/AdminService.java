package com.smartchat.service;

import com.smartchat.common.BusinessException;
import com.smartchat.common.PageResult;
import com.smartchat.dto.AdminDtos;
import com.smartchat.entity.User;
import com.smartchat.repository.ConversationRepository;
import com.smartchat.repository.MessageRepository;
import com.smartchat.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台服务：用户管理与统计
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public AdminService(UserRepository userRepository,
                        ConversationRepository conversationRepository,
                        MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /** 用户列表（分页 + 关键字），附带会话数/消息数 */
    public PageResult<AdminDtos.AdminUserItem> listUsers(int page, int size, String keyword) {
        Page<User> result;
        if (keyword != null && !keyword.isBlank()) {
            result = userRepository.findByUsernameContainingOrNicknameContaining(
                    keyword, keyword, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            result = userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        // 一次性统计各用户的会话数 / 消息数
        Map<Long, Long> convCounts = groupCount(conversationRepository.countGroupByUserId());
        Map<Long, Long> msgCounts = groupCount(messageRepository.countGroupByUserId());

        List<AdminDtos.AdminUserItem> items = result.getContent().stream()
                .map(u -> AdminDtos.AdminUserItem.from(u,
                        convCounts.getOrDefault(u.getId(), 0L),
                        msgCounts.getOrDefault(u.getId(), 0L)))
                .toList();
        return new PageResult<>(items, result.getTotalElements(), page, size);
    }

    /** 禁用 / 启用用户（不能操作自己，不能禁用其他管理员） */
    @Transactional
    public void updateStatus(Long operatorId, Long targetId, boolean enabled) {
        if (operatorId.equals(targetId)) {
            throw new BusinessException("不能修改自己的状态");
        }
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /** 删除用户及其全部数据 */
    @Transactional
    public void deleteUser(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BusinessException("不能删除自己的账号");
        }
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能删除管理员账号");
        }
        // 先删消息与会话，再删用户
        conversationRepository.findByUserIdOrderByUpdatedAtDesc(targetId)
                .forEach(c -> messageRepository.deleteByConversationId(c.getId()));
        conversationRepository.deleteAll(
                conversationRepository.findByUserIdOrderByUpdatedAtDesc(targetId));
        userRepository.delete(user);
    }

    private Map<Long, Long> groupCount(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }
}
