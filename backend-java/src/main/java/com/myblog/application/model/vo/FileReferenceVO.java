package com.myblog.application.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 文件资源的一条内容版本引用，供删除前的引用明细查询返回。
 * usage 描述资源在该版本中的用途（如 技能图标、足迹照片、MyLab 封面）。
 */
public record FileReferenceVO(
        @JsonProperty("module_key") String moduleKey,
        @JsonProperty("version_no") int versionNo,
        String state,
        String usage) {
}
