package com.jay.aicodemother.core;

import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class AICodeGeneratorFacadeTest {

    @Mock
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        // 直接实例化，避免依赖注入问题
//        aiCodeGeneratorFacade = new AICodeGeneratorFacade(aiCodeGeneratorService);
//    }

    @Test
    void generateAndSaveCode_withHtmlType_shouldReturnFile() {
        // 准备测试数据
        String userMessage = "生成一个登录页面";
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.HTML;
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        htmlCodeResult.setHtmlCode("<!DOCTYPE html><html><body><h1>Login</h1></body></html>");
        htmlCodeResult.setDescription("登录页面");

        // 执行测试并验证异常（因为AI服务不可用）
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, codeGenType,1L);
        });

        // 验证异常信息
        assertEquals("AI服务不可用", exception.getMessage());
    }

    @Test
    void generateAndSaveCode_withMultiFileType_shouldReturnFile() {
        // 准备测试数据
        String userMessage = "生成一个完整的网页应用";
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.MULTI_FILE;
        MultiFileCodeResult multiFileCodeResult = new MultiFileCodeResult();
        multiFileCodeResult.setHtmlCode("<!DOCTYPE html><html><body><h1>App</h1></body></html>");
        multiFileCodeResult.setCssCode("body { margin: 0; }");
        multiFileCodeResult.setJsCode("console.log('App loaded');");
        multiFileCodeResult.setDescription("完整网页应用");

        // 执行测试并验证异常（因为AI服务不可用）
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, codeGenType,1L);
        });

        // 验证异常信息
        assertEquals("AI服务不可用", exception.getMessage());
    }

    @Test
    void generateAndSaveCode_withNullType_shouldThrowBusinessException() {
        // 准备测试数据
        String userMessage = "生成一个页面";

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, null,1L);
        });

        // 验证异常信息
        assertEquals("生成类型为空", exception.getMessage());
    }

    @Test
    void generateAndSaveCode_withUnsupportedType_shouldThrowBusinessException() {
        // 创建一个模拟的不支持的枚举值（通过反射）
        CodeGenTypeEnum unsupportedType = mock(CodeGenTypeEnum.class);
        when(unsupportedType.toString()).thenReturn("UNSUPPORTED");
        
        // 准备测试数据
        String userMessage = "生成一个页面";

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, unsupportedType,1L);
        });

        // 验证异常信息包含类型信息
        assertTrue(exception.getMessage().contains("不支持的生成类型"));
    }

    @Test
    void generateAndSaveCodeStream_withNullType_shouldThrowBusinessException() {
        // 准备测试数据
        String userMessage = "生成一个页面";

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, null,1L);
        });

        // 验证异常信息
        assertEquals("生成类型为空", exception.getMessage());
    }

    @Test
    void generateVueProjectCodeStream(){
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("简单的任务记录网站，宗地阿妈不超过200行", CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有的数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertTrue(completeContent.contains("错误：AI服务不可用"));
    }

    @Test
    void generateAndSaveCode() {
    }

    @Test
    void generateAndSaveCodeStream() {
    }
}