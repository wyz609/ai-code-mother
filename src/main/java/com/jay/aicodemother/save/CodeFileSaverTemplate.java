package com.jay.aicodemother.save;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存器 - 模板方法模式
 *
 * @author Jay
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入（结果对象不能为空）
        validateInput(result);
        // 2. 构建唯一目录（目录名：{类型}_{appId}）
        String baseDirPath = buildUniqueDir(appId);
        // 3. 保存文件（具体实现由子类提供：HTML 存单文件，多文件存三个文件）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        // 结果对象为空则抛出系统异常
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一目录路径
     *
     * @return 目录路径
     * @param appId 应用ID
     */
    protected final String buildUniqueDir(Long appId) {
        // 应用 ID 为空则报错
        if (appId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用ID不能为空");
        }
        // 取代码生成类型值（如 html、multi_file）
        String codeType = getCodeType().getValue();
        // 目录名格式：{类型}_{appId}（保证每个应用有独立目录）
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        // 拼接根目录路径
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        // 创建目录（含父目录）
        FileUtil.mkdir(dirPath);
        // 返回目录路径
        return dirPath;
    }

    /**
     * 写入单个文件的工具方法
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        // 内容非空才写文件（避免生成空文件）
        if (StrUtil.isNotBlank(content)) {
            // 拼接文件完整路径
            String filePath = dirPath + File.separator + filename;
            // 以 UTF-8 编码写入
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
