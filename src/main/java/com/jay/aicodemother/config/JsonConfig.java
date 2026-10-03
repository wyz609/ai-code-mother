package com.jay.aicodemother.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     *
     * <p>业务说明：雪花 ID 等 Long 类型超过 JS Number 安全范围（2^53）会精度丢失，
     * 统一序列化为字符串，避免前端拿到被截断的 ID。</p>
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建非 XML 模式的 ObjectMapper
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 创建自定义模块
        SimpleModule module = new SimpleModule();
        // Long 及其包装类型统一转字符串序列化（防止精度丢失）
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 注册模块到 ObjectMapper
        objectMapper.registerModule(module);
        // 返回配置完成的 ObjectMapper
        return objectMapper;
    }
}
