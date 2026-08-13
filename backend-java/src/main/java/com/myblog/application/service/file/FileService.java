package com.myblog.application.service.file;

import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;
import com.myblog.application.model.vo.FileReferenceVO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件资源用例接口：后台文件的分页查询、上传、临时访问地址签发与删除。
 */
public interface FileService {

    /**
     * 分页列出文件资源，可按目录过滤（仅管理员）。
     */
    PageResult<FileOutVO> list(CurrentUser actor, long page, long size, String directory);

    /**
     * 上传文件到对象存储并登记文件记录。
     */
    FileOutVO upload(CurrentUser actor, UploadFile file);

    /**
     * 获取文件的访问地址：图片类返回公开地址，其他类型返回限时签名地址。
     */
    Map<String, String> presign(CurrentUser actor, UUID id) throws Exception;

    /**
     * 删除文件：软删记录并异步清理对象存储中的文件。
     */
    void delete(CurrentUser actor, UUID id);

    /**
     * 查询文件被哪些内容版本引用的明细，供删除确认时展示（仅管理员）。
     */
    List<FileReferenceVO> references(CurrentUser actor, UUID id);
}
