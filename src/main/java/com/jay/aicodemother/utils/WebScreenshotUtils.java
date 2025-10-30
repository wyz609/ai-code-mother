package com.jay.aicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

/**
 * 网页截图工具类，提供根据 URL 生成截图文件并返回路径方法
 */
@Slf4j
public class WebScreenshotUtils {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    
    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            driver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            driverThreadLocal.set(driver);
        }
        return driver;
    }

    public static void removeDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    public static String saveWebPageScreenshot(String webUrl){
        if(StrUtil.isBlank(webUrl)){
            log.error("网页截图失败， URL为空");
            return null;
        }

        WebDriver webDriver = getDriver();

        // 创建临时目录
        try {
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots";
            FileUtil.mkdir(rootPath);
            // 定义图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始图片保存路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) +IMAGE_SUFFIX;
            // 访问网页
            webDriver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(webDriver);
            // 进行截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(imageSavePath, screenshotBytes);
            log.info("原始图片截取成功：{}", imageSavePath);
            // 进行压缩图片
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("图片压缩成功：{}", compressedImagePath);
            // 删除原始图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            removeDriver(); // 确保总是释放资源
        }
    }

    private static void waitForPageLoad(WebDriver driver){
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为 complete
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            // 等待页面加载完成 确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成....");
        } catch (InterruptedException e) {
            log.error("等待页面加载异常，继续执行截图",e);
            e.printStackTrace();
        }
    }

    /**
     * 图片压缩
     * @param imageSavePath
     * @param compressedImagePath
     */
    private static void compressImage(String imageSavePath, String compressedImagePath) {
        // 设置压缩图片质量
        final float COMPRESSION_QUALITY;
        try {
            COMPRESSION_QUALITY = 0.8f;
            ImgUtil.compress(
                    FileUtil.file(imageSavePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("图片压缩失败: {}", imageSavePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }


    /*
    保存图片到文件
     */
    private static void saveImage(String imageSavePath, byte[] screenshotBytes) {
        try {
            FileUtil.writeBytes(screenshotBytes, imageSavePath);
            log.info("图片保存成功：{}", imageSavePath);
        } catch (IORuntimeException e) {
            log.error("保存图片失败: {}", imageSavePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }
}