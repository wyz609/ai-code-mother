package com.jay.aicodemother.core;

import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
//import reactor.test.StepVerifier;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AICodeGeneratorFacadeTest {

    @Mock
    private AiCodeGeneratorService aiCodeGeneratorService;

    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aiCodeGeneratorFacade = new AICodeGeneratorFacade(aiCodeGeneratorService);
    }

    @Test
    void generateAndSaveCode_withHtmlType_shouldReturnFile() {
        // 准备测试数据
        String userMessage = "生成一个登录页面";
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.HTML;
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        htmlCodeResult.setHtmlCode("<!DOCTYPE html><html><body><h1>Login</h1></body></html>");
        htmlCodeResult.setDescription("登录页面");

        // 模拟AI服务返回
        when(aiCodeGeneratorService.generateHtmlCode(anyString())).thenReturn(htmlCodeResult);

        // 执行测试
        File result = aiCodeGeneratorFacade.generateAndSaveCode(userMessage, codeGenType);

        // 验证结果
        assertNotNull(result);
        verify(aiCodeGeneratorService, times(1)).generateHtmlCode(userMessage);
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

        // 模拟AI服务返回
        when(aiCodeGeneratorService.generateMultiFileCode(anyString())).thenReturn(multiFileCodeResult);

        // 执行测试
        File result = aiCodeGeneratorFacade.generateAndSaveCode(userMessage, codeGenType);

        // 验证结果
        assertNotNull(result);
        verify(aiCodeGeneratorService, times(1)).generateMultiFileCode(userMessage);
    }

    @Test
    void generateAndSaveCode_withNullType_shouldThrowBusinessException() {
        // 准备测试数据
        String userMessage = "生成一个页面";

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, null);
        });

        // 验证异常信息
        assertEquals("未指定代码生成类型", exception.getMessage());
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
            aiCodeGeneratorFacade.generateAndSaveCode(userMessage, unsupportedType);
        });

        // 验证异常信息包含类型信息
        assertTrue(exception.getMessage().contains("不支持的生成类型"));
    }

//    @Test
//    void generateAndSaveCodeStream_withHtmlType_shouldReturnFlux() {
//        // 准备测试数据
//        String userMessage = "生成一个登录页面";
//        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.HTML;
//
//        // 模拟AI服务返回流式数据
//        Flux<String> mockFlux = Flux.just("<!DOCTYPE html>", "<html>", "<body>", "<h1>Login</h1>", "</body>", "</html>");
//        when(aiCodeGeneratorService.generateHtmlCodeStream(anyString())).thenReturn(mockFlux);
//
//        // 执行测试
//        Flux<String> result = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenType);
//
//        // 验证结果
//        assertNotNull(result);
//        StepVerifier.create(result)
//                .expectNext("<!DOCTYPE html>")
//                .expectNext("<html>")
//                .expectNext("<body>")
//                .expectNext("<h1>Login</h1>")
//                .expectNext("</body>")
//                .expectNext("</html>")
//                .verifyComplete();
//
//        verify(aiCodeGeneratorService, times(1)).generateHtmlCodeStream(userMessage);
//    }
//
//    @Test
//    void generateAndSaveCodeStream_withMultiFileType_shouldReturnFlux() {
//        // 准备测试数据
//        String userMessage = "生成一个多文件应用";
//        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.MULTI_FILE;
//
//        // 模拟AI服务返回流式数据
//        Flux<String> mockFlux = Flux.just("<!DOCTYPE html>", "<html>", "<head>", "<link rel='stylesheet' href='style.css'>", "</head>", "<body>", "<h1>App</h1>", "<script src='script.js'></script>", "</body>", "</html>");
//        when(aiCodeGeneratorService.generateMultiFileCodeStream(anyString())).thenReturn(mockFlux);
//
//        // 执行测试
//        Flux<String> result = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenType);
//
//        // 验证结果
//        assertNotNull(result);
//        StepVerifier.create(result)
//                .expectNextCount(10)
//                .verifyComplete();
//
//        verify(aiCodeGeneratorService, times(1)).generateMultiFileCodeStream(userMessage);
//    }

    @Test
    void generateAndSaveCodeStream_withNullType_shouldThrowBusinessException() {
        // 准备测试数据
        String userMessage = "生成一个页面";

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, null);
        });

        // 验证异常信息
        assertEquals("未指定代码生成类型", exception.getMessage());
    }
}