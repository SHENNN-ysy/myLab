package com.myblog.controller;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.service.file.FileService;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 文件管理接口：媒体文件的分页查询、上传、访问地址获取与逻辑删除。
 * 仅负责请求解析与响应封装，业务逻辑委托给 {@link FileService}。
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "文件管理")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    // 文件应用服务，承载上传/列表/签名/删除等业务逻辑
    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    /**
     * 分页查询媒体文件列表，可按目录过滤。
     */
    @GetMapping
    @Operation(summary = "分页查询媒体文件")
    public Result<PageResult<FileOutVO>> list(@AuthenticationPrincipal CurrentUser actor,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(name = "page_size", defaultValue = "20") long size,
                                              @RequestParam(required = false) String directory) {
        return Result.ok(files.list(actor, page, size, directory));
    }

    /**
     * 上传文件到指定目录，返回文件元信息。
     *
     * @param directory 目标目录，仅支持 footstep、hero、hobbies、icon、mylab、mylab-post
     */
    @PostMapping("/upload")
    @Operation(summary = "上传资源", description = "directory 支持 footstep、hero、hobbies、icon、mylab、mylab-post；mylab 只存正文，其余目录只存图片。")
    public Result<FileOutVO> upload(@AuthenticationPrincipal CurrentUser actor,
                                    @RequestParam String directory,
                                    @Parameter(description = "待上传文件", required = true) @RequestPart("file") MultipartFile file) throws Exception {
        UploadFile command = new UploadFile(
                directory,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream());
        return Result.ok(files.upload(actor, command));
    }

    /**
     * 获取文件访问地址：公开图片返回 CDN 地址，私有文件返回限时签名地址。
     */
    @GetMapping("/presigned/{id}")
    @Operation(summary = "获取文件访问地址", description = "公开图片返回 CDN 地址，私有文件返回一小时有效的签名地址。")
    public Result<Map<String, String>> presign(@AuthenticationPrincipal CurrentUser actor,
                                               @PathVariable UUID id) throws Exception {
        return Result.ok(files.presign(actor, id));
    }

    /**
     * 逻辑删除指定文件。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除文件")
    public Result<?> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id) {
        files.delete(actor, id);
        return Result.ok(null, "文件删除任务已提交");
    }
}
