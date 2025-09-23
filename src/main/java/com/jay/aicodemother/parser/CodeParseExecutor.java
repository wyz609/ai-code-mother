/**
 * Class name: CodeParseExecutor
 * Package: com.jay.aicodemother.parser
 * Description:
 *
 * @Create: 2025/9/23 17:53
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother.parser;

import com.jay.aicodemother.exception.BusinessException;
import com.jay.aicodemother.exception.ErrorCode;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;

/**
 * 代码解析执行器
 *  根据代码生成类型执行相应的解析逻辑
 */
public class CodeParseExecutor {

    /**
     * HTML代码解析器
     */
    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    /**
     * 多文件代码解析器
     */
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 根据代码生成类型获取对应的解析器 执行代码解析
     *
     * @param codeContent 代码内容
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 解析结果
     * @throws BusinessException 当codeGenTypeEnum为null或不支持时抛出业务异常
     */
    public static Object getParser(String codeContent , CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum){
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型: " + codeGenTypeEnum);
        };
    }

}