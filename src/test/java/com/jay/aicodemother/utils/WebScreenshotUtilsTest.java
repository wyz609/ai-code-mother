package com.jay.aicodemother.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {

        String testUtl = "https://bailian.console.aliyun.com/?utm_content=se_1021228143&gclid=CjwKCAjwjffHBhBuEiwAKMb8pEaLFNVeX5LQ3Akog6vAWOV0sG6yq7y7zv_87g6kHR22I6Y1vn31FxoC_PMQAvD_BwE&tab=home#/home";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUtl);
        Assertions.assertNotNull(webPageScreenshot);

    }
}