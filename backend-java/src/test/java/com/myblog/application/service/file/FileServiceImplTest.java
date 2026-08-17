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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void listingSiteImageDoesNotRequireOss() {
        FileRecord image = resource("/assets/avatar.png", "image/png");
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(image), 1, 20, 1));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().getFirst().url()).isEqualTo("/assets/avatar.png");
        verify(storage, never()).publicUrl(anyString());
    }

    @Test
    void presigningSiteMarkdownReturnsItsRelativeUrl() throws Exception {
        FileRecord markdown = resource("/mylab/first-post.md", "text/markdown");
        when(files.findById(markdown.getId())).thenReturn(markdown);

        Map<String, String> result = service.presign(admin, markdown.getId());

        assertThat(result.get("url")).isEqualTo("/mylab/first-post.md");
        verify(storage, never()).signedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void listingByDirectoryUsesLogicalDirectoryPrefix() {
        when(files.findPage(1, 20, "hero/")).thenReturn(PageResult.of(List.of(), 1, 20, 0));

        service.list(admin, 1, 20, "HERO");

        verify(files).findPage(1, 20, "hero/");
    }

    @Test
    void uploadStoresImageUnderSelectedDirectory() {
        when(props.ossObjectPrefix()).thenReturn("");
        when(props.ossMaxFileSizeMb()).thenReturn(10);
        when(props.ossBucket()).thenReturn("ysy-myblog");
        UploadFile upload = new UploadFile("icon", "logo.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        FileOutVO result = service.upload(admin, upload);

        verify(storage).upload(startsWith("icon/"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq("image/png"));
        assertThat(result.directory()).isEqualTo("icon");
        assertThat(result.objectKey()).startsWith("icon/");
    }

    @Test
    void referencesReturnsContentVersionsUsingTheFile() {
        FileRecord image = resource("hobbies/2026/08/x.png", "image/png");
        FileReferenceVO reference = new FileReferenceVO("hobbies", 3, "PUBLISHED", "爱好图片");
        when(files.findById(image.getId())).thenReturn(image);
        when(files.findReferences(image.getId())).thenReturn(List.of(reference));

        List<FileReferenceVO> result = service.references(admin, image.getId());

        assertThat(result).containsExactly(reference);
    }

    @Test
    void referencesOfMissingFileIsNotFound() {
        UUID id = UUID.randomUUID();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.references(admin, id))
                .isInstanceOf(com.myblog.common.exception.NotFoundException.class);
    }

    @Test
    void listRejectsInvalidDirectory() {
        assertThatThrownBy(() -> service.list(admin, 1, 20, "unknown"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void listTreatsBlankDirectoryAsNoFilter() {
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(), 1, 20, 0));

        service.list(admin, 1, 20, "   ");

        verify(files).findPage(1, 20, null);
    }

    @Test
    void listReturnsNullUrlAndDirectoryForNonImageOrUnknownKey() {
        FileRecord document = resource("mylab/2026/08/a.pdf", "application/pdf");
        FileRecord keyless = resource(null, "image/png");
        keyless.setMimeType(null);
        FileRecord oddKey = resource("stray-file", "image/png");
        when(files.findPage(1, 20, null))
                .thenReturn(PageResult.of(List.of(document, keyless, oddKey), 1, 20, 3));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().get(0).url()).isNull();
        assertThat(result.records().get(0).directory()).isEqualTo("mylab");
        assertThat(result.records().get(1).url()).isNull();
        assertThat(result.records().get(1).directory()).isNull();
        assertThat(result.records().get(2).directory()).isNull();
    }

    @Test
    void listStripsConfiguredPrefixWhenResolvingDirectory() {
        when(props.ossObjectPrefix()).thenReturn("blog");
        FileRecord image = resource("blog/hero/2026/08/banner.png", "image/png");
        when(storage.publicUrl("blog/hero/2026/08/banner.png")).thenReturn("https://cdn.example.com/banner.png");
        when(files.findPage(1, 20, null)).thenReturn(PageResult.of(List.of(image), 1, 20, 1));

        PageResult<FileOutVO> result = service.list(admin, 1, 20, null);

        assertThat(result.records().getFirst().directory()).isEqualTo("hero");
        assertThat(result.records().getFirst().url()).isEqualTo("https://cdn.example.com/banner.png");
    }

    @Test
    void uploadRejectsBlankDirectory() {
        UploadFile upload = new UploadFile("  ", "a.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadRejectsEmptyFile() {
        UploadFile upload = new UploadFile("icon", "a.png", "image/png", 0,
                new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadRejectsUnsupportedMediaType() {
        UploadFile upload = new UploadFile("icon", "a.zip", "application/zip", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadRejectsMissingContentType() {
        UploadFile upload = new UploadFile("icon", "a.png", null, 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void mylabDirectoryRejectsImages() {
        UploadFile upload = new UploadFile("mylab", "a.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void imageDirectoryRejectsDocuments() {
        UploadFile upload = new UploadFile("hero", "a.md", "text/markdown", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadRejectsFileExceedingSizeLimit() {
        when(props.ossMaxFileSizeMb()).thenReturn(1);
        UploadFile upload = new UploadFile("icon", "big.png", "image/png", 2L * 1024 * 1024,
                new ByteArrayInputStream(new byte[] {1}));

        assertThatThrownBy(() -> service.upload(admin, upload))
                .isInstanceOf(ValidationException.class);
        verify(storage, never()).upload(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void uploadUsesConfiguredObjectPrefix() {
        when(props.ossObjectPrefix()).thenReturn(" /blog/ ");
        when(props.ossMaxFileSizeMb()).thenReturn(10);
        when(props.ossBucket()).thenReturn("ysy-myblog");
        UploadFile upload = new UploadFile("icon", "logo.png", "image/png", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        FileOutVO result = service.upload(admin, upload);

        assertThat(result.objectKey()).startsWith("blog/icon/");
        assertThat(result.directory()).isEqualTo("icon");
    }

    @Test
    void uploadDerivesExtensionFromMediaType() {
        when(props.ossObjectPrefix()).thenReturn("");
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

        Map<String, String> documentTypes = Map.of(
                "application/pdf", ".pdf",
                "text/markdown", ".md",
                "text/plain", ".txt");
        documentTypes.forEach((type, extension) -> {
            FileOutVO result = service.upload(admin, new UploadFile("mylab", "x", type, 3,
                    new ByteArrayInputStream(new byte[] {1, 2, 3})));
            assertThat(result.objectKey()).endsWith(extension);
        });
    }

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

    @Test
    void presignReturnsPublicUrlForPublicImages() throws Exception {
        FileRecord image = resource("hero/2026/08/x.png", "image/png");
        when(files.findById(image.getId())).thenReturn(image);
        when(storage.publicUrl("hero/2026/08/x.png")).thenReturn("https://cdn.example.com/x.png");

        Map<String, String> result = service.presign(admin, image.getId());

        assertThat(result.get("url")).isEqualTo("https://cdn.example.com/x.png");
        verify(storage, never()).signedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void presignSignsNonPublicResources() throws Exception {
        FileRecord document = resource("mylab/2026/08/a.pdf", "application/pdf");
        when(files.findById(document.getId())).thenReturn(document);
        when(storage.signedUrl("mylab/2026/08/a.pdf", 3600)).thenReturn("https://oss.example.com/signed");

        Map<String, String> result = service.presign(admin, document.getId());

        assertThat(result.get("url")).isEqualTo("https://oss.example.com/signed");
    }

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

    @Test
    void deleteRejectsFileStillReferencedByContent() {
        FileRecord image = resource("hero/2026/08/x.png", "image/png");
        when(files.findById(image.getId())).thenReturn(image);
        when(files.hasReferences(image.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.delete(admin, image.getId()))
                .isInstanceOf(ConflictException.class);
        verify(storage, never()).deleteAsync(anyString());
    }

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
