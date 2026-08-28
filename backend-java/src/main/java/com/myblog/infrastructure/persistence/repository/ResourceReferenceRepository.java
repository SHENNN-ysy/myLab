package com.myblog.infrastructure.persistence.repository;

import com.myblog.application.model.entity.FileRecord;
import com.myblog.application.model.vo.FileReferenceVO;
import com.myblog.application.repository.FileRepository;
import com.myblog.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myblog.infrastructure.persistence.mapper.file.FileRecordMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 文件资源仓储：实现应用层 {@link FileRepository} 端口。
 * 常规 CRUD 走 MyBatis-Plus 的 {@link FileRecordMapper}，跨表引用计数走原生 JDBC。
 */
@Repository
public class ResourceReferenceRepository implements FileRepository {
    private final FileRecordMapper mapper; // 文件记录的基础 CRUD
    private final JdbcTemplate jdbc;       // 跨业务表的引用计数查询

    public ResourceReferenceRepository(FileRecordMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    /**
     * 分页查询未删除的文件记录。
     *
     * @param objectKeyPrefix 可选的 OSS objectKey 前缀过滤（左模糊匹配）
     */
    @Override public PageResult<FileRecord> findPage(long page, long size, String objectKeyPrefix) {
        LambdaQueryWrapper<FileRecord> query = new LambdaQueryWrapper<FileRecord>()
                .isNull(FileRecord::getDeletedAt);
        if (objectKeyPrefix != null && !objectKeyPrefix.isBlank()) {
            // 同时兼容根目录 key（hero/...）与带公共前缀的 key（images/hero/...）。
            query.apply("(object_key LIKE CONCAT({0}, '%') OR object_key LIKE CONCAT('%/', {0}, '%'))",
                    objectKeyPrefix);
        }
        Page<FileRecord> result = mapper.selectPage(new Page<>(page, size),
                query.orderByDesc(FileRecord::getCreatedAt));
        return PageResult.of(result.getRecords(), page, size, result.getTotal());
    }
    @Override public FileRecord findById(UUID id) { return mapper.selectById(id); }
    @Override public void add(FileRecord record) { mapper.insert(record); }
    @Override public void save(FileRecord record) { mapper.updateById(record); }

    /**
     * 检查文件资源是否仍被各业务模块（首页图、关于、技能、足迹、爱好、实验室卡片）引用，
     * 用于删除前的安全校验。
     *
     * @return 存在任一未删除的引用则返回 true
     */
    @Override
    public boolean hasReferences(UUID id) {
        Long count = jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM home_images WHERE image_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM about_contents WHERE avatar_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM skills WHERE icon_resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM footprint_resources WHERE resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM hobby_resources WHERE resource_id = ? AND deleted_at IS NULL)
                  + (SELECT COUNT(*) FROM mylab_resources
                     WHERE image_resource_id = ? AND deleted_at IS NULL)
                """, Long.class, id, id, id, id, id, id);
        return count != null && count > 0;
    }

    /**
     * 查询文件被哪些内容版本引用：沿各业务子表（必要时经父表）关联到 content_releases，
     * 取出模块、版本号、版本状态与用途。与 hasReferences 的口径一致——只看未删除的数据行，
     * 版本软删除时其子表行已级联标记 deleted_at，无需再过滤版本本体。
     */
    @Override
    public List<FileReferenceVO> findReferences(UUID id) {
        return jdbc.query("""
                SELECT module_key, version_no, state, usage FROM (
                    SELECT cr.module_key, cr.version_no, cr.state, '首页图片' AS usage
                    FROM home_images h JOIN content_releases cr ON cr.id = h.release_id
                    WHERE h.image_resource_id = ? AND h.deleted_at IS NULL
                    UNION ALL
                    SELECT cr.module_key, cr.version_no, cr.state, '关于页头像'
                    FROM about_contents a JOIN content_releases cr ON cr.id = a.release_id
                    WHERE a.avatar_resource_id = ? AND a.deleted_at IS NULL
                    UNION ALL
                    SELECT cr.module_key, cr.version_no, cr.state, '技能图标'
                    FROM skills s JOIN content_releases cr ON cr.id = s.release_id
                    WHERE s.icon_resource_id = ? AND s.deleted_at IS NULL
                    UNION ALL
                    SELECT cr.module_key, cr.version_no, cr.state, '足迹照片'
                    FROM footprint_resources fr
                    JOIN footprints f ON f.id = fr.footprint_id
                    JOIN content_releases cr ON cr.id = f.release_id
                    WHERE fr.resource_id = ? AND fr.deleted_at IS NULL
                    UNION ALL
                    SELECT cr.module_key, cr.version_no, cr.state, '爱好图片'
                    FROM hobby_resources hr
                    JOIN hobbies hb ON hb.id = hr.hobby_id
                    JOIN content_releases cr ON cr.id = hb.release_id
                    WHERE hr.resource_id = ? AND hr.deleted_at IS NULL
                    UNION ALL
                    SELECT cr.module_key, cr.version_no, cr.state, 'MyLab 封面'
                    FROM mylab_resources mr
                    JOIN mylab_cards mc ON mc.id = mr.card_id
                    JOIN content_releases cr ON cr.id = mc.release_id
                    WHERE mr.image_resource_id = ? AND mr.deleted_at IS NULL
                ) refs
                ORDER BY module_key, version_no DESC
                """, (rs, n) -> new FileReferenceVO(
                rs.getString("module_key"), rs.getInt("version_no"),
                rs.getString("state"), rs.getString("usage")),
                id, id, id, id, id, id);
    }
}
