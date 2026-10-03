package com.jay.aicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.constant.UserConstant;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.exception.ThrowUtils;
import com.jay.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.model.entity.ChatHistory;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.jay.aicodemother.service.AppService;
import com.jay.aicodemother.service.ChatHistoryService;
import com.jay.aicodemother.mapper.ChatHistoryMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/wyz609">程序员阿阳</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    // 应用服务（@Lazy 延迟注入，避免与 AppService 循环依赖）
    @Resource
    @Lazy
    private AppService appService;

    /**
     * 添加一条对话历史消息（用户消息或 AI 消息）
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 基础校验：应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 消息内容不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        // 消息类型不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        // 用户 ID 必须为正数
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效（user/ai 等）
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        // 非法类型则报错
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        // 插入数据库：构建对话历史实体
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
//                // 初始化消息序号为0，实际使用时应该根据已有消息数量+1
//                .messageOrder(0)
                .build();
        // 保存并返回是否成功
        return this.save(chatHistory);
    }

    /**
     * 删除某应用下的全部对话历史（应用删除时级联清理）
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        // 应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 构建按 appId 匹配的删除条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        // 执行批量删除
        return this.remove(queryWrapper);
    }

    /**
     * 分页（游标）查询某应用的对话历史
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        // 应用 ID 必须为正数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        // 每页数量限制在 1-50 之间
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        // 用户必须登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 记录查询日志
        log.info("开始查询 appId: {} 的对话历史", appId);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        // 应用不存在则 404
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 判断是否管理员
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        // 判断是否应用创建者
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        // 既非管理员也非创建者则无权限
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        // 设置应用 ID 过滤条件
        queryRequest.setAppId(appId);
        // 设置游标时间（只查询早于该时间的记录，实现分页）
        queryRequest.setLastCreateTime(lastCreateTime);
        // 构建查询包装器
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据（游标分页，恒为第 1 页）
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 将历史对话加载到 AI 聊天记忆（MessageWindowChatMemory）中，
     * 使 AI 在本次会话中拥有上下文。
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 应用 ID 不能为空
            ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
            // 加载数量必须大于 0
            ThrowUtils.throwIf(maxCount <= 0, ErrorCode.PARAMS_ERROR, "最大数量必须大于0");
            // 记忆对象不能为空
            ThrowUtils.throwIf(chatMemory == null, ErrorCode.PARAMS_ERROR, "聊天记忆对象不能为空");
            
            // 按应用 ID 查询最近的 maxCount 条历史记录（按创建时间倒序）
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false) // 按时间倒序
                    .limit(1,maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            // 无历史记录时清空记忆并返回 0
            if (CollUtil.isEmpty(historyList)) {
                chatMemory.clear();
                return 0;
            }
            // 反转列表，确保按照时间正序（老的在前，新的在后）
            Collections.reverse(historyList);
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            // 按照时间顺序将消息添加到记忆中
            int loadedCount = 0;
            // 遍历每条历史记录
            for (ChatHistory history : historyList) {
                // 用户消息：转为 langchain4j 的 UserMessage 加入记忆
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    // AI 消息：转为 AiMessage 加入记忆
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
                // 忽略其他类型的消息，但仍然计入处理总数中
            }
            // 记录成功加载的数量
            log.info("成功为 appId: {} 加载 {} 条历史消息", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            // 加载失败仅记录日志
            log.error("加载历史对话失败，appId: {}", appId, e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 创建空查询包装器
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 请求体为空则返回空条件（查询全部）
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        // 取出 ID 精确匹配条件
        Long id = chatHistoryQueryRequest.getId();
        // 消息内容模糊查询
        String message = chatHistoryQueryRequest.getMessage();
        // 消息类型精确查询
        String messageType = chatHistoryQueryRequest.getMessageType();
        // 应用 ID 精确查询
        Long appId = chatHistoryQueryRequest.getAppId();
        // 用户 ID 精确查询
        Long userId = chatHistoryQueryRequest.getUserId();
        // 游标时间（用于分页）
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        // 排序字段
        String sortField = chatHistoryQueryRequest.getSortField();
        // 排序方向
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("content", message) // 修改为正确的字段名
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }
}