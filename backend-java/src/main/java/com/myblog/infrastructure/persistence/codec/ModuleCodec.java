package com.myblog.infrastructure.persistence.codec;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 内容模块的版本化数据编解码器：负责某一内容模块在 JSON 快照与关系表之间的双向装配。
 * 每个模块一个实现（Spring 组件），由 {@code JdbcContentReleaseRepository}
 * 按 {@link #moduleKey()} 建立注册表分发 replaceData/readData/softDeleteVersion。
 */
public interface ModuleCodec {

    /** 模块标识（home/about/skills/footprints/hobbies/vibe/mylab） */
    String moduleKey();

    /** 用 JSON 快照整体写入某次发布的模块数据（调用方已先执行 {@link #deleteData(UUID)} 清空旧数据） */
    void write(UUID releaseId, JsonNode data);

    /** 读取某次发布的模块数据并聚合为 JSON 结构（与 {@link #write(UUID, JsonNode)} 的写入结构互逆） */
    Object read(UUID releaseId);

    /** 软删除某次发布的模块数据：级联为子表/孙表数据行打 deleted_at 标记（幂等），解除资源引用 */
    void softDeleteData(UUID releaseId, OffsetDateTime now);

    /**
     * 物理清空某次发布的模块数据（replaceData 前置步骤）。
     * 必须是物理 DELETE——不能走 MyBatis-Plus 的 delete（@TableLogic 会把它变成逻辑删除）。
     */
    void deleteData(UUID releaseId);
}
