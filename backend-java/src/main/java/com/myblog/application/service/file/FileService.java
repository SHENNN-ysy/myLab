package com.myblog.application.service.file;

import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;

import java.util.Map;
import java.util.UUID;

public interface FileService {

    PageResult<FileOutVO> list(CurrentUser actor, long page, long size);

    FileOutVO upload(CurrentUser actor, UploadFile file);

    Map<String, String> presign(CurrentUser actor, UUID id) throws Exception;

    void delete(CurrentUser actor, UUID id);
}
