package com.myblog.infrastructure.persistence.codec;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.myblog.application.model.entity.AboutBubble;
import com.myblog.application.model.entity.AboutContent;
import com.myblog.application.model.entity.AboutProfileBullet;
import com.myblog.application.model.entity.FileRecord;
import com.myblog.infrastructure.persistence.mapper.about.AboutBubbleMapper;
import com.myblog.infrastructure.persistence.mapper.about.AboutContentMapper;
import com.myblog.infrastructure.persistence.mapper.about.AboutProfileBulletMapper;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.iterable;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.mapOf;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.nullableUuid;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.text;
import static com.myblog.infrastructure.persistence.codec.JsonSnapshots.uuid;

/**
 * about 模块编解码器：about_contents/about_profile_bullets/about_bubbles 三表
 * 与 JSON 快照之间的双向装配。
 * 读写走 MyBatis-Plus；级联软标记与物理清空保留原生 SQL（语义不由 @TableLogic 接管）。
 */
@Component
public class AboutCodec implements ModuleCodec {

    private final JdbcTemplate jdbc;
    private final AboutContentMapper aboutContentMapper;
    private final AboutProfileBulletMapper aboutProfileBulletMapper;
    private final AboutBubbleMapper aboutBubbleMapper;
    private final FileRecordMapper fileRecordMapper;

    public AboutCodec(JdbcTemplate jdbc, AboutContentMapper aboutContentMapper,
                      AboutProfileBulletMapper aboutProfileBulletMapper, AboutBubbleMapper aboutBubbleMapper,
                      FileRecordMapper fileRecordMapper) {
        this.jdbc = jdbc;
        this.aboutContentMapper = aboutContentMapper;
        this.aboutProfileBulletMapper = aboutProfileBulletMapper;
        this.aboutBubbleMapper = aboutBubbleMapper;
        this.fileRecordMapper = fileRecordMapper;
    }

    @Override
    public String moduleKey() {
        return "about";
    }

    @Override
    public void write(UUID releaseId, JsonNode root) {
        writeAbout(releaseId, root);
    }

    @Override
    public Object read(UUID releaseId) {
        return readAbout(releaseId);
    }

    @Override
    public void softDeleteData(UUID releaseId, OffsetDateTime now) {
        String parent = "about_content_id IN (SELECT id FROM about_contents WHERE release_id = ?)";
        CodecSql.softDelete(jdbc, "about_profile_bullets", parent, now, releaseId);
        CodecSql.softDelete(jdbc, "about_bubbles", parent, now, releaseId);
        CodecSql.softDelete(jdbc, "about_contents", "release_id = ?", now, releaseId);
    }

    @Override
    public void deleteData(UUID releaseId) {
        jdbc.update("DELETE FROM about_contents WHERE release_id = ?", releaseId);
    }

    /**
     * about 模块写入：JSON 快照拆为 about_contents 主记录 + 资料要点/气泡子表实体插入，
     * 时间戳走数据库默认值。
     */
    private void writeAbout(UUID releaseId, JsonNode root) {
        JsonNode profile = root.path("profile");
        JsonNode ingredients = root.path("ingredients");
        UUID aboutId = uuid(root, "row_id");
        AboutContent content = new AboutContent();
        content.setId(aboutId);
        content.setReleaseId(releaseId);
        content.setProfileTitle(text(profile, "title"));
        content.setAvatarResourceId(nullableUuid(profile, "avatar_resource_id"));
        content.setAvatarAlt(text(profile, "avatar_alt"));
        content.setIntro(text(profile, "intro"));
        content.setOutro(text(profile, "outro"));
        content.setIngredientsTitle(text(ingredients, "title"));
        content.setIngredientsDescription(text(ingredients, "description"));
        aboutContentMapper.insert(content);

        int bulletOrder = 0;
        for (JsonNode bullet : iterable(profile.path("bullets"))) {
            AboutProfileBullet entity = new AboutProfileBullet();
            entity.setId(UUID.randomUUID());
            entity.setAboutContentId(aboutId);
            entity.setContents(bullet.asText());
            entity.setSortOrder(bulletOrder++);
            aboutProfileBulletMapper.insert(entity);
        }
        int bubbleOrder = 0;
        for (JsonNode bubble : iterable(root.path("bubbles"))) {
            AboutBubble entity = new AboutBubble();
            entity.setId(uuid(bubble, "row_id"));
            entity.setAboutContentId(aboutId);
            entity.setBubbleText(text(bubble, "text"));
            entity.setBubbleSize(text(bubble, "size"));
            entity.setBackgroundColor(text(bubble, "background_color"));
            entity.setTextColor(text(bubble, "text_color"));
            entity.setGlowColor(text(bubble, "glow_color"));
            entity.setSortOrder(bubbleOrder++);
            aboutBubbleMapper.insert(entity);
        }
    }

    /**
     * about 模块读取：查主记录实体（软删除由 @TableLogic 自动过滤），头像对象键单条取出
     * ——以两次简单查询替代原来的 LEFT JOIN resources，行为等价。
     */
    private Map<String, Object> readAbout(UUID releaseId) {
        List<AboutContent> rows = aboutContentMapper.selectList(
                Wrappers.<AboutContent>lambdaQuery().eq(AboutContent::getReleaseId, releaseId));
        if (rows.isEmpty()) return Map.of("profile", Map.of("bullets", List.of()), "ingredients", Map.of(), "bubbles", List.of());
        AboutContent row = rows.getFirst();
        UUID aboutId = row.getId();
        String avatarObjectKey = null;
        if (row.getAvatarResourceId() != null) {
            FileRecord avatar = fileRecordMapper.selectById(row.getAvatarResourceId());
            avatarObjectKey = avatar == null ? null : avatar.getObjectKey();
        }
        Map<String, Object> profile = mapOf(
                "title", row.getProfileTitle(), "avatar_resource_id", row.getAvatarResourceId(),
                "avatar_object_key", avatarObjectKey, "avatar_alt", row.getAvatarAlt(),
                "intro", row.getIntro(), "bullets", readAboutBullets(aboutId), "outro", row.getOutro());
        Map<String, Object> ingredients = mapOf(
                "title", row.getIngredientsTitle(), "description", row.getIngredientsDescription());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("row_id", aboutId);
        result.put("profile", profile);
        result.put("ingredients", ingredients);
        result.put("bubbles", readAboutBubbles(aboutId));
        return result;
    }

    private List<String> readAboutBullets(UUID aboutId) {
        return aboutProfileBulletMapper.selectList(
                        Wrappers.<AboutProfileBullet>lambdaQuery()
                                .eq(AboutProfileBullet::getAboutContentId, aboutId)
                                .orderByAsc(AboutProfileBullet::getSortOrder))
                .stream().map(AboutProfileBullet::getContents).toList();
    }

    private List<Map<String, Object>> readAboutBubbles(UUID aboutId) {
        List<AboutBubble> rows = aboutBubbleMapper.selectList(
                Wrappers.<AboutBubble>lambdaQuery()
                        .eq(AboutBubble::getAboutContentId, aboutId)
                        .orderByAsc(AboutBubble::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AboutBubble row : rows) {
            result.add(mapOf(
                    "row_id", row.getId(), "text", row.getBubbleText(),
                    "size", row.getBubbleSize(), "background_color", row.getBackgroundColor(),
                    "text_color", row.getTextColor(), "glow_color", row.getGlowColor(),
                    "sort_order", row.getSortOrder()));
        }
        return result;
    }
}
