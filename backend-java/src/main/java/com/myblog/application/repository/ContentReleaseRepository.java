package com.myblog.application.repository;

import com.myblog.application.model.entity.ContentRelease;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ContentReleaseRepository {
    ContentRelease findDraft(String moduleKey);
    ContentRelease findCurrent(String moduleKey);
    ContentRelease findPublished(String moduleKey);
    ContentRelease findVersion(String moduleKey, int versionNo);
    List<ContentRelease> findVersions(String moduleKey);
    void lockModule(String moduleKey);
    int nextVersion(String moduleKey);
    void add(ContentRelease release);
    boolean touchDraft(UUID releaseId, OffsetDateTime expectedUpdatedAt, OffsetDateTime nextUpdatedAt);
    void replaceData(ContentRelease release, Object data);
    Object readData(ContentRelease release);
    void publish(ContentRelease draft, ContentRelease current, UUID actorId, OffsetDateTime now);
    void offline(ContentRelease current, OffsetDateTime now);
    void deleteDraft(ContentRelease draft);
}
