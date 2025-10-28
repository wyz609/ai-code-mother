package com.jay.aicodemother.ai;

import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    void routeCodeGenType() {
        String userPrompt = "生成一个简单的个人介绍页面";
        CodeGenTypeEnum result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result);
        userPrompt = "做一个公司官网，需要首页，关于我们，联系我们三个页面";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result);
        userPrompt = "做一个电商管理系统， 包含用户管理，商店管理，订单管理，需要路由和状态管理";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求: {} -> {}", userPrompt, result);
    }
}