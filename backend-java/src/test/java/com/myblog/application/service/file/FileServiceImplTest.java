package com.myblog.application.service.file;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;
import com.myblog.application.model.vo.FileReferenceVO;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.exception.ConflictException;
import com.myblog.common.exception.NotFoundException;
import com.myblog.common.exception.ValidationException;
import com.myblog.common.properties.AppProperties;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FileServiceImpl 单元测试：覆盖文件列表、上传、预签名、引用查询与删除的目录规则、入参校验及 OSS 交互边界。 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {
    @Mock FileRepository files;
    @Mock ObjectStorage storage;
    @Mock AppProperties props;

    private FileServiceImpl service;
    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        service = new FileServiceImpl(files, storage, props);
        admin = new CurrentUser(UUID.randomUUID(), "admin", "admin");
    }

    /** 站内资源（相对路径 key）在列表中原样返回 URL，不调用 OSS 生成地址。 */
    @Test
    void listingSiteImageDoesNotRequireOss() {
        FileRecord image = resource("/assets/avatar.png", "image/png");
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(image), 1, 20, 1));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().getFirst().url()).isEqualTo("/assets/avatar.png");
        verify(storage, never()).publicUrl(anyString());
    }

    /** 站内资源的预签名直接返回相对路径，不走 OSS 签名 URL。 */
    @Test
    void presigningSiteResourceReturnsItsRelativeUrl() throws Exception {
        FileRecord markdown = resource("/documents/readme.md", "text/markdown");
        when(files.findById(markdown.getId())).thenReturn(markdown);

        Map<String, String> result = service.presign(admin, markdown.getId());

        assertThat(result.get("url")).isEqualTo("/documents/readme.md");
        verify(storage, never()).signedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    /** 按目录过滤时逻辑目录名（HERO）归一化为小写带斜杠前缀（hero/）再查库。 */
    @Test
    void listingByDirectoryUsesLogicalDirectoryPrefix() {
        when(files.findPage(1, 20, "hero/")).thenReturn(PageResult.of(List.of(), 1, 20, 0));

        service.list(admin, 1, 20, "HERO");

        verify(files).findPage(1, 20, "hero/");
    }

    /** 上传成功：object key 为 目录/UUID.扩展名，落在所选目录下。 */
    @Test
    void uploadStoresImageUnderSelectedDirectory() {
        when(props.ossMaxFileSizeMb()).thenReturn(10);
        when(props.ossBucket()).thenReturn("ysy-myblog");
        UploadFile upload = new UploadFile("icon", "logo.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        FileOutVO result = service.upload(admin, upload);

        verify(storage).upload(startsWith("icon/"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq("image/png"));
        assertThat(result.directory()).isEqualTo("icon");
        assertThat(result.objectKey())
                .matches("icon/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
    }

    /** 引用查询返回使用该文件的内容版本列表。 */
    @Test
    void referencesReturnsContentVersionsUsingTheFile() {
        FileRecord image = resource("hobbies/2026/08/x.png", "image/png");
        FileReferenceVO reference = new FileReferenceVO("hobbies", 3, "PUBLISHED", "爱好图片");
        when(files.findById(image.getId())).thenReturn(image);
        when(files.findReferences(image.getId())).thenReturn(List.of(reference));

        List<FileReferenceVO> result = service.references(admin, image.getId());

        assertThat(result).containsExactly(reference);
    }

    /** 查询不存在文件的引用抛 NotFoundException。 */
    @Test
    void referencesOfMissingFileIsNotFound() {
        UUID id = UUID.randomUUID();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.references(admin, id))
                .isInstanceOf(com.myblog.common.exception.NotFoundException.class);
    }

    /** 列表过滤传未知目录名时抛 ValidationException。 */
    @Test
    void listRejectsInvalidDirectory() {
        assertThatThrownBy(() -> service.list(admin, 1, 20, "unknown"))
                .isInstanceOf(ValidationException.class);
    }

    /** 目录参数为空白串时视为不过滤。 */
    @Test
    void listTreatsBlankDirectoryAsNoFilter() {
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(), 1, 20, 0));

        service.list(admin, 1, 20, "   ");

        verify(files).findPage(1, 20, null);
    }

    /** 非图片、无 key 或无目录段 key 的记录，url 与 directory 字段返回 null。 */
    @Test
    void listReturnsNullUrlAndDirectoryForNonImageOrUnknownKey() {
        // 覆盖三种异常形态：非图片文档、无 object key、key 中无目录段
        FileRecord document = resource("documents/2026/08/a.pdf", "application/pdf");
        FileRecord keyless = resource(null, "image/png");
        keyless.setMimeType(null);
        FileRecord oddKey = resource("stray-file", "image/png");
        when(files.findPage(1, 20, null))
                .thenReturn(PageResult.of(List.of(document, keyless, oddKey), 1, 20, 3));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().get(0).url()).isNull();
        assertThat(result.records().get(0).directory()).isNull();
        assertThat(result.records().get(1).url()).isNull();
        assertThat(result.records().get(1).directory()).isNull();
        assertThat(result.records().get(2).directory()).isNull();
    }

    /** 兼容带历史统一前缀（blog/）的旧 key，仍能解析出目录并生成公网 URL。 */
    @Test
    void listRecognizesDirectoryInLegacyPrefixedKey() {
        FileRecord image = resource("blog/hero/2026/08/banner.png", "image/png");
        when(storage.publicUrl("blog/hero/2026/08/banner.png")).thenReturn("https://cdn.example.com/banner.png");
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(image), 1, 20, 1));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().getFirst().directory()).isEqualTo("hero");
        assertThat(result.records().getFirst().url()).isEqualTo("https://cdn.example.com/banner.png");
    }

    /** 上传目录为空白串时抛 ValidationException。 */
    @Test
    void uploadRejectsBlankDirectory() {
        UploadFile upload = new UploadFile("  ", "a.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** 空文件（大小为 0）上传被拒绝。 */
    @Test
    void uploadRejectsEmptyFile() {
        UploadFile upload = new UploadFile("icon", "a.png", "image/png", 0,
                new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** 不支持的媒体类型（zip）上传被拒绝。 */
    @Test
    void uploadRejectsUnsupportedMediaType() {
        UploadFile upload = new UploadFile("icon", "a.zip", "application/zip", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** 缺少 Content-Type 时上传被拒绝。 */
    @Test
    void uploadRejectsMissingContentType() {
        UploadFile upload = new UploadFile("icon", "a.png", null, 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** documents 目录不接受图片上传（目录与文件类型不匹配）。 */
    @Test
    void uploadRejectsInvalidDirectory() {
        UploadFile upload = new UploadFile("documents", "a.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** 图片目录（hero）不接受 Markdown 文档上传。 */
    @Test
    void imageDirectoryRejectsDocuments() {
        UploadFile upload = new UploadFile("hero", "a.md", "text/markdown", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    /** 超过大小上限的文件上传被拒绝，且不调用 OSS。 */
    @Test
    void uploadRejectsFileExceedingSizeLimit() {
        when(props.ossMaxFileSizeMb()).thenReturn(1);
        // 上限设为 1MB，声明 2MB 的文件以触发超限
        UploadFile upload = new UploadFile("icon", "big.png", "image/png", 2L * 1024 * 1024,
                new ByteArrayInputStream(new byte[] {1}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
        verify(storage, never()).upload(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    /** 无引用的文档允许删除：软删记录并异步清理 OSS 对象。 */
    @Test
    void deleteAllowsUnreferencedDocument() {
        FileRecord document = resource("documents/archive.md", "text/markdown");
        when(files.findById(document.getId())).thenReturn(document);
        when(files.hasReferences(document.getId())).thenReturn(false);

        service.delete(admin, document.getId());

        verify(files).save(document);
        verify(storage).deleteAsync("documents/archive.md");
    }

    /** object key 不含统一前缀或日期子目录，目录只有一层。 */
    @Test
    void uploadDoesNotCreatePrefixOrDateSubdirectories() {
        when(props.ossMaxFileSizeMb()).thenReturn(10);
        when(props.ossBucket()).thenReturn("ysy-myblog");
        UploadFile upload = new UploadFile("icon", "logo.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        FileOutVO result = service.upload(admin, upload);

        assertThat(result.objectKey()).matches("icon/[^/]+\\.png");
        assertThat(result.directory()).isEqualTo("icon");
    }

    /** 扩展名由媒体类型映射推导（jpeg→.jpg 等），不取原文件名后缀。 */
    @Test
    void uploadDerivesExtensionFromMediaType() {
        when(props.ossMaxFileSizeMb()).thenReturn(10);
        when(props.ossBucket()).thenReturn("ysy-myblog");

        Map<String, String> imageTypes = Map.of(
                "image/jpeg", ".jpg",
                "image/webp", ".webp",
                "image/svg+xml", ".svg",
                "image/gif", ".gif");
        imageTypes.forEach((type, extension) -> {
            FileOutVO result = service.upload(admin, new UploadFile("icon", "x", type, 3,
                    new ByteArrayInputStream(new byte[] {1, 2, 3})));
            assertThat(result.objectKey()).endsWith(extension);
        });

    }

    /** 不存在或已软删的文件预签名均抛 NotFoundException。 */
    @Test
    void presignRejectsMissingOrSoftDeletedFile() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.presign(admin, missing))
                .isInstanceOf(NotFoundException.class);

        FileRecord deleted = resource("hero/2026/08/x.png", "image/png");
        deleted.setDeletedAt(OffsetDateTime.now());
        when(files.findById(deleted.getId())).thenReturn(deleted);
        assertThatThrownBy(() -> service.presign(admin, deleted.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    /** 公开图片的预签名直接返回公网 URL，不生成签名 URL。 */
    @Test
    void presignReturnsPublicUrlForPublicImages() throws Exception {
        FileRecord image = resource("hero/2026/08/x.png", "image/png");
        when(files.findById(image.getId())).thenReturn(image);
        when(storage.publicUrl("hero/2026/08/x.png")).thenReturn("https://cdn.example.com/x.png");

        Map<String, String> result = service.presign(admin, image.getId());

        assertThat(result.get("url")).isEqualTo("https://cdn.example.com/x.png");
        verify(storage, never()).signedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    /** 非公开资源（PDF）预签名返回带有效期的签名 URL。 */
    @Test
    void presignSignsNonPublicResources() throws Exception {
        FileRecord document = resource("documents/2026/08/a.pdf", "application/pdf");
        when(files.findById(document.getId())).thenReturn(document);
        // 非公开资源走签名 URL，有效期 3600 秒
        when(storage.signedUrl("documents/2026/08/a.pdf", 3600)).thenReturn("https://oss.example.com/signed");

        Map<String, String> result = service.presign(admin, document.getId());

        assertThat(result.get("url")).isEqualTo("https://oss.example.com/signed");
    }

    /** 删除不存在或已软删的文件均抛 NotFoundException。 */
    @Test
    void deleteRejectsMissingOrSoftDeletedFile() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.delete(admin, missing))
                .isInstanceOf(NotFoundException.class);

        FileRecord deleted = resource("hero/2026/08/x.png", "image/png");
        deleted.setDeletedAt(OffsetDateTime.now());
        when(files.findById(deleted.getId())).thenReturn(deleted);
        assertThatThrownBy(() -> service.delete(admin, deleted.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    /** 仍被内容引用的文件禁止删除（ConflictException），且不清理 OSS 对象。 */
    @Test
    void deleteRejectsFileStillReferencedByContent() {
        FileRecord image = resource("hero/2026/08/x.png", "image/png");
        when(files.findById(image.getId())).thenReturn(image);
        when(files.hasReferences(image.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(admin, image.getId()))
                .isInstanceOf(ConflictException.class);
        verify(storage, never()).deleteAsync(anyString());
    }

    /** 删除为软删：deletedAt/updatedAt 被打上时间戳，同时异步删除 OSS 对象。 */
    @Test
    void deleteSoftDeletesRecordAndPurgesStoredObject() {
        FileRecord image = resource("hero/2026/08/x.png", "image/png");
        when(files.findById(image.getId())).thenReturn(image);
        when(files.hasReferences(image.getId())).thenReturn(false);

        service.delete(admin, image.getId());

        ArgumentCaptor<FileRecord> saved = ArgumentCaptor.forClass(FileRecord.class);
        verify(files).save(saved.capture());
        assertThat(saved.getValue().getDeletedAt()).isNotNull();
        assertThat(saved.getValue().getUpdatedAt()).isNotNull();
        verify(storage).deleteAsync("hero/2026/08/x.png");
    }

    // 测试文件记录工厂：bucket 固定为 local，originalName 取 key 最后一段
    private FileRecord resource(String objectKey, String mimeType) {
        FileRecord record = new FileRecord();
        record.setId(UUID.randomUUID());
        record.setObjectKey(objectKey);
        record.setBucket("local");
        record.setOriginalName(objectKey == null ? null : objectKey.substring(objectKey.lastIndexOf('/') + 1));
        record.setMimeType(mimeType);
        record.setSize(0L);
        return record;
    }
}
