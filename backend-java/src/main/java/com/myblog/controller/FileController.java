package com.myblog.controller;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.service.file.FileService;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @GetMapping
    public Result<PageResult<FileOutVO>> list(@AuthenticationPrincipal CurrentUser actor,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(name = "page_size", defaultValue = "20") long size) {
        return Result.ok(files.list(actor, page, size));
    }

    @PostMapping("/upload")
    public Result<FileOutVO> upload(@AuthenticationPrincipal CurrentUser actor,
                                    @RequestPart("file") MultipartFile file) throws Exception {
        UploadFile command = new UploadFile(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream());
        return Result.ok(files.upload(actor, command));
    }

    @GetMapping("/presigned/{id}")
    public Result<Map<String, String>> presign(@AuthenticationPrincipal CurrentUser actor,
                                               @PathVariable UUID id) throws Exception {
        return Result.ok(files.presign(actor, id));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        files.delete(actor, id);
        return Result.ok(null, "file deletion queued");
    }
}
