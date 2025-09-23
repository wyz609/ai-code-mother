package com.jay.aicodemother;

import com.jay.aicodemother.ai.AiCodeGeneratorService;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.core.AICodeGeneratorFacade;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class AiCodeMotherApplicationTests {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void contextLoads() {
    }

    @Test
    void generateHtmlCode(){
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个程序员阿阳的工作记录小工具，代码不超过100行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode(){
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("做一个程序员阿阳的留言板，不超过50行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateAndSaveCodeStream() {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("开始生成代码...");
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("做一个番茄计时器，可以进行规划学习时间，倒计时，待办事件等功能" +
                "页面进行现代化UI设计，可以适量添加一些毛玻璃效果样式，代码不超过700行", CodeGenTypeEnum.MULTI_FILE);
//        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("做一个打卡网页，代码不超过50行", CodeGenTypeEnum.MULTI_FILE);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
        System.out.println("生成代码完成，耗时：" + (LocalDateTime.now().getNano() - startTime.getNano()));
    }

    @Test
    void generateAndSaveHtmlCodeStream(){
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("制作一个计算器，代码控制在20行左右", CodeGenTypeEnum.HTML);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}
