/**
 * Class name: CodeFileSaver
 * Package: com.jay.aicodemother.core
 * Description: 代码文件保存工具类，用于将AI生成的代码保存到本地文件系统中
 *
 * @Create: 2025/9/22 17:24
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.core;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.ai.model.HtmlCodeResult;
import com.jay.aicodemother.ai.model.MultiFileCodeResult;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件保存工具类
 * 提供将AI生成的代码结果保存为文件的功能
 */
public class CodeFileSaver {
    
    /**
     * 文件保存的根目录
     * 使用系统属性"user.dir"获取项目根目录，然后拼接"/tmp/code_output"作为代码输出目录
     */
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";
    
    /**
     * 保存HTML代码结果到文件
     * 该方法会创建一个唯一的目录，并将HTML代码保存为index.html文件
     *
     * @param htmlCodeResult HTML代码结果对象，包含生成的HTML代码
     * @return 返回保存文件的目录File对象
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult){
        // 根据代码生成类型HTML构建唯一目录
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        // 将HTML代码写入index.html文件
        writeToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());
        // 返回保存文件的目录
        return new File(baseDirPath);
    }

    /**
     * 保存多文件代码结果到文件
     * 该方法会创建一个唯一的目录，并将HTML、CSS、JS代码分别保存为对应的文件
     *
     * @param multiFileCodeResult 多文件代码结果对象，包含HTML、CSS、JS代码
     * @return 返回保存文件的目录File对象
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult){
        // 根据代码生成类型MULTI_FILE构建唯一目录
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 将HTML代码写入index.html文件
        writeToFile(baseDirPath, "index.html", multiFileCodeResult.getHtmlCode());
        // 将CSS代码写入style.css文件
        writeToFile(baseDirPath, "style.css", multiFileCodeResult.getCssCode());
        // 将JS代码写入script.js文件
        writeToFile(baseDirPath, "script.js", multiFileCodeResult.getJsCode());
        // 返回保存文件的目录
        return new File(baseDirPath);
    }
    
    /**
     * 构建唯一目录路径
     * 通过业务类型和雪花ID生成唯一的目录名称，避免文件冲突
     *
     * @param bizType 业务类型，如"html"或"multi_file"
     * @return 返回构建好的唯一目录路径
     */
    private static String buildUniqueDir(String bizType) {
        // 使用StrUtil.format格式化目录名，格式为"{bizType}_{雪花ID}"
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        // 构建完整目录路径
        String dirPath = StrUtil.format("{}/{}", FILE_SAVE_ROOT_DIR, uniqueDirName);
        // 创建目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }
    
    /**
     * 将代码内容写入指定文件
     * 使用UTF-8 编码将代码内容写入指定路径的文件中
     *
     * @param baseDirPath 基础目录路径
     * @param fileName 文件名
     * @param fileContent 文件内容
     */
    private static void writeToFile(String baseDirPath, String fileName, String fileContent) {
        // 构建完整文件路径
        String filePath = baseDirPath + File.separator + fileName;
        // 将内容写入文件
        FileUtil.writeString(fileContent, filePath, StandardCharsets.UTF_8);
    }

}