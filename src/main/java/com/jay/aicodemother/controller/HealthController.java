package com.jay.aicodemother.controller;

import com.jay.aicodemother.common.BaseResponse;
import com.jay.aicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查 控制层。
 *
 * <p>用于负载均衡器 / 部署探针 / 运维监控确认服务是否存活。</p>
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查接口：服务可正常响应即返回 ok。
     *
     * @return 固定返回 "ok"
     */
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        // 只要请求能到达这里，说明应用已启动且路由正常，返回 ok 即可
        return ResultUtils.success( "ok");
    }
}

