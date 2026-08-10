package com.myblog.application.service.file;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.command.file.UploadFile;
import com.myblog.application.model.vo.FileOutVO;
import com.myblog.application.port.ObjectStorage;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.properties.AppProperties;
import com.myblog.common.result.PageResult;
import com.myblog.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    private FileRecord resource(String objectKey, String mimeType) {
        FileRecord record = new FileRecord();
        record.setId(UUID.randomUUID());
        record.setObjectKey(objectKey);
        record.setBucket("local");
        record.setOriginalName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        record.setMimeType(mimeType);
        record.setSize(0L);
        return record;
    }
}
