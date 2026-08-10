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

/**
 * 文件服务实现：负责上传校验（类型/目录/大小）、对象存储 key 生成与文件记录的生命周期管理。
 */
@Service
public class FileServiceImpl implements FileService {

    // 允许上传的媒体类型白名单
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/svg+xml",
            "image/gif", "application/pdf", "text/markdown", "text/plain"
    );
    // 允许上传的业务目录白名单
    private static final Set<String> ALLOWED_DIRECTORIES = Set.of(
            "footstep", "hero", "hobbies", "icon", "mylab", "mylab-post"
    );
    private static final Set<String> MYLAB_DOCUMENT_TYPES = Set.of(
            "application/pdf", "text/markdown", "text/plain"
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
    /**
     * 分页查询：目录参数归一化后转为对象存储 key 前缀过滤。
     */
    public PageResult<FileOutVO> list(CurrentUser actor, long page, long size, String directory) {
        Authorization.requireAdmin(actor);
        String normalizedDirectory = normalizeOptionalDirectory(directory);
        // 查询按逻辑目录匹配，不依赖部署环境是否配置额外的 OSS 公共前缀。
        String objectKeyPrefix = normalizedDirectory == null ? null : normalizedDirectory + "/";
        PageResult<FileRecord> result = files.findPage(page, size, objectKeyPrefix);
        return PageResult.of(result.records().stream().map(this::toVo).toList(),
                page, size, result.total());
    }

    @Override
    @Transactional
    /**
     * 上传：校验目录、类型、大小后，按「目录/年月/UUID.扩展名」生成对象 key 上传，并落库文件记录。
     */
    public FileOutVO upload(CurrentUser actor, UploadFile file) {
        Authorization.requireAdmin(actor);
        String directory = normalizeRequiredDirectory(file.directory());
        if (file.empty()) {
            throw new ValidationException(ErrorCode.FILE_EMPTY, null);
        }
        String contentType = file.contentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED, "媒体类型：" + contentType);
        }
        if ("mylab".equals(directory) && !MYLAB_DOCUMENT_TYPES.contains(contentType)) {
            throw new ValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED,
                    "mylab 目录只允许上传 Markdown、纯文本或 PDF 正文资源");
        }
        if (!"mylab".equals(directory) && !contentType.startsWith("image/")) {
            throw new ValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED,
                    directory + " 目录只允许上传图片资源");
        }
        long maxBytes = (long) props.ossMaxFileSizeMb() * 1024L * 1024L;
        if (file.size() > maxBytes) {
            throw new ValidationException(ErrorCode.FILE_TOO_LARGE,
                    "最大允许 " + props.ossMaxFileSizeMb() + "MB");
        }
        String name = Objects.requireNonNullElse(file.originalName(), "file");
        String ext = extensionFor(contentType);
        String datePath = LocalDate.now().toString().replace("-", "/").substring(0, 7);
        String key = directoryPrefix(directory) + datePath + "/" + UUID.randomUUID().toString().replace("-", "")
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
    /**
     * 取访问地址：站内路径原样返回；公开图片用公开地址；其余资源签发 1 小时有效的签名地址。
     */
    public Map<String, String> presign(CurrentUser actor, UUID id) throws Exception {
        Authorization.requireAdmin(actor);
        FileRecord record = files.findById(id);
        if (record == null || record.getDeletedAt() != null) {
            throw new NotFoundException(ErrorCode.FILE_NOT_FOUND, null);
        }
        String url = isSiteUrl(record.getObjectKey())
                ? record.getObjectKey()
                : isPublicImage(record.getMimeType())
                    ? storage.publicUrl(record.getObjectKey())
                    : storage.signedUrl(record.getObjectKey(), 3600);
        return Map.of("url", url);
    }

    @Override
    @Transactional
    /**
     * 删除：仍被内容草稿或历史版本引用时拒绝；否则软删记录并异步删除存储对象。
     */
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

    /** 记录转视图：仅公开图片直接附带访问 URL，其余类型置空（需走 presign）。 */
    private FileOutVO toVo(FileRecord record) {
        String url = isPublicImage(record.getMimeType())
                ? isSiteUrl(record.getObjectKey()) ? record.getObjectKey() : storage.publicUrl(record.getObjectKey())
                : null;
        return new FileOutVO(record.getId(), record.getObjectKey(), directoryOf(record.getObjectKey()), record.getBucket(),
                record.getOriginalName(), record.getMimeType(), record.getSize(),
                record.getCreatedAt(), url);
    }

    /** 拼接对象 key 前缀：配置的公共前缀（可选）+ 业务目录。 */
    private String directoryPrefix(String directory) {
        String configuredPrefix = Objects.requireNonNullElse(props.ossObjectPrefix(), "").trim()
                .replaceAll("^/+|/+$", "");
        return configuredPrefix.isEmpty()
                ? directory + "/"
                : configuredPrefix + "/" + directory + "/";
    }

    /** 从对象 key 反推业务目录，剥离配置前缀后取首段；不在白名单内返回 null。 */
    private String directoryOf(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        String normalized = objectKey.replace('\\', '/').replaceFirst("^/+", "");
        String configuredPrefix = Objects.requireNonNullElse(props.ossObjectPrefix(), "").trim()
                .replaceAll("^/+|/+$", "");
        if (!configuredPrefix.isEmpty() && normalized.startsWith(configuredPrefix + "/")) {
            normalized = normalized.substring(configuredPrefix.length() + 1);
        }
        int separator = normalized.indexOf('/');
        String candidate = separator < 0 ? normalized : normalized.substring(0, separator);
        return ALLOWED_DIRECTORIES.contains(candidate) ? candidate : null;
    }

    /** 归一化并校验目录入参，为空或不在白名单时抛校验异常。 */
    private static String normalizeRequiredDirectory(String directory) {
        String normalized = normalizeOptionalDirectory(directory);
        if (normalized == null) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED,
                    "资源目录不能为空，可选值：footstep、hero、hobbies、icon、mylab、mylab-post");
        }
        return normalized;
    }

    /** 归一化可选目录入参：空白返回 null，非法值抛校验异常。 */
    private static String normalizeOptionalDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return null;
        }
        String normalized = directory.trim().toLowerCase();
        if (!ALLOWED_DIRECTORIES.contains(normalized)) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED,
                    "资源目录仅支持：footstep、hero、hobbies、icon、mylab、mylab-post");
        }
        return normalized;
    }

    /** 是否为可公开访问的图片类型。 */
    private static boolean isPublicImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /** 是否已是站内路径或完整 URL（无需再签名/拼接）。 */
    private static boolean isSiteUrl(String objectKey) {
        return objectKey != null && (objectKey.startsWith("/")
                || objectKey.startsWith("http://") || objectKey.startsWith("https://"));
    }

    /** 按媒体类型推导文件扩展名，未知类型抛校验异常。 */
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
