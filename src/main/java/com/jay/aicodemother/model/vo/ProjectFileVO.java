package com.jay.aicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Generated project source file.
 */
@Data
@AllArgsConstructor
public class ProjectFileVO implements Serializable {

    private String path;

    private String language;

    private String content;

    private static final long serialVersionUID = 1L;
}
