package com.myblog.application.service.file;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.enumeration.ErrorCode;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.properties.AppProperties;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import com.myblog.application.port.ObjectStorage;
import com.myblog.common.security.Authorization;
import com.myblog.application.model.vo.FileOutVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/svg+xml",
            "image/gif", "application/pdf", "text/markdown", "text/plain"
    );

    private final FileRepository files;
    private final ObjectStorage storage;
    private final AppProperties props;

    public FileServiceImpl(FileRepository files, ObjectStorage storage,
                           AppProperties props) {
        this.files = files;
        this.storage = storage;
        this.props = props;
    }

    @Override
    public PageResult<FileOutVO> list(CurrentUser actor, long page, long size) {
        Authorization.requireAdmin(actor);
        PageResult<FileRecord> result = files.findPage(page, size);
        return PageResult.of(result.records().stream().map(this::toVo).toList(),
                page, size, result.total());
    }

    @Override
    @Transactional
    public FileOutVO upload(CurrentUser actor, UploadFile file) {
        Authorization.requireAdmin(actor);
        if (file.empty()) {
            throw new ValidationException(ErrorCode.FILE_EMPTY, null);
        }
        String contentType = file.contentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED, "媒体类型：" + contentType);
        }
        long maxBytes = (long) props.ossMaxFileSizeMb() * 1024L * 1024L;
        if (file.size() > maxBytes) {
            throw new ValidationException(ErrorCode.FILE_TOO_LARGE,
                    "最大允许 " + props.ossMaxFileSizeMb() + "MB");
        }
        String name = Objects.requireNonNullElse(file.originalName(), "file");
        String ext = extensionFor(contentType);
        String datePath = LocalDate.now().toString().replace("-", "/").substring(0, 7);
        String prefix = props.ossObjectPrefix().replaceAll("^/+|/+$", "");
        String key = prefix + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        storage.upload(key, file.content(), file.size(), contentType);

        FileRecord record = new FileRecord();
        record.setId(UUID.randomUUID());
        record.setObjectKey(key);
        record.setBucket(props.ossBucket());
        record.setOriginalName(name);
        record.setMimeType(contentType);
        record.setSize(file.size());
        record.setUploadedBy(actor.id());
        OffsetDateTime now = OffsetDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        files.add(record);
        return toVo(record);
    }

    @Override
    public Map<String, String> presign(CurrentUser actor, UUID id) throws Exception {
        Authorization.requireAdmin(actor);
        FileRecord record = files.findById(id);
        if (record == null || record.getDeletedAt() != null) {
            throw new NotFoundException(ErrorCode.FILE_NOT_FOUND, null);
        }
        String url = isPublicImage(record.getMimeType())
                ? storage.publicUrl(record.getObjectKey())
                : storage.signedUrl(record.getObjectKey(), 3600);
        return Map.of("url", url);
    }

    @Override
    @Transactional
    public void delete(CurrentUser actor, UUID id) {
        Authorization.requireAdmin(actor);
        FileRecord record = files.findById(id);
        if (record == null || record.getDeletedAt() != null) {
            throw new NotFoundException(ErrorCode.FILE_NOT_FOUND, null);
        }
        if (files.hasReferences(id)) {
            throw new com.myblog.common.exception.ConflictException(
                    ErrorCode.RESOURCE_CONFLICT, "资源仍被内容草稿或历史版本引用");
        }
        record.setDeletedAt(OffsetDateTime.now());
        record.setUpdatedAt(OffsetDateTime.now());
        files.save(record);
        storage.deleteAsync(record.getObjectKey());
    }

    private FileOutVO toVo(FileRecord record) {
        String url = isPublicImage(record.getMimeType())
                ? storage.publicUrl(record.getObjectKey())
                : null;
        return new FileOutVO(record.getId(), record.getObjectKey(), record.getBucket(),
                record.getOriginalName(), record.getMimeType(), record.getSize(),
                record.getCreatedAt(), url);
    }

    private static boolean isPublicImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            case "image/gif" -> "gif";
            case "application/pdf" -> "pdf";
            case "text/markdown" -> "md";
            case "text/plain" -> "txt";
            default -> throw new ValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED, "媒体类型：" + contentType);
        };
    }
}
