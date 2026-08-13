package com.myblog.infrastructure.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import com.myblog.common.exception.EngagementUnavailableException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Redis 互动实时统计实现：浏览/点赞/访问的实时计数只活在 Redis，匿名访客
 * 以 HMAC 散列后的 visitorHash 作为键（72h TTL），不持久化任何访客明细。
 * <p>
 * 一致性模型：
 * <ul>
 *   <li>所有跨键计数变化均由 Lua 脚本原子完成（校验访客、去重、加减计数、
 *       登记脏集合一次提交），避免并发请求下计数与脏标记脱节；</li>
 *   <li>每次成功计数都把受影响对象登记进 dirty 集合（文章/站点/按日三类），
 *       快照任务按 dirty→processing→ack 三步认领：先整体搬入 processing，
 *       落 PG 成功后才从 processing 删除（ack）；任务中途失败时 processing
 *       中的标记保留，下次认领前由 {@link #recoverProcessing()} 搬回 dirty 重试，
 *       保证"至少一次"落库而不丢更新；</li>
 *   <li>PG 中的值是 Redis 绝对值的周期性快照（整体覆盖而非增量），
 *       重启时再从 PG 回填 Redis 兜底（putIfAbsent，不覆盖 Redis 已有值）。
 * </ul>
 */
@Component
public class RedisEngagementStore implements EngagementStore {
    static final String SITE_METRICS_KEY = "blog:site:metrics";
    // dirty/processing 三类脏集合：stats=文章维度、site=站点总计、daily=按日统计
    static final String DIRTY_STATS_KEY = "blog:dirty:stats";
    static final String PROCESSING_STATS_KEY = "blog:processing:stats";
    static final String DIRTY_SITE_KEY = "blog:dirty:site";
    static final String PROCESSING_SITE_KEY = "blog:processing:site";
    static final String DIRTY_DAILY_KEY = "blog:dirty:daily";
    static final String PROCESSING_DAILY_KEY = "blog:processing:daily";
    private static final String SNAPSHOT_LOCK_KEY = "blog:lock:snapshot";
    private static final Duration VISITOR_TTL = Duration.ofHours(72);
    private static final long DAILY_TTL_SECONDS = Duration.ofDays(120).toSeconds();

    /**
     * 浏览脚本：访客过期直接报错；30 分钟去重窗口内重复浏览不计数；
     * 首次计数时同步累加文章、站点总计、当日三处计数，并把受影响对象登记进三类脏集合。
     */
    private static final DefaultRedisScript<List> VIEW_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            local counted = redis.call('SET', KEYS[2], '1', 'EX', 1800, 'NX')
            if counted then
              redis.call('HINCRBY', KEYS[3], 'view_count', 1)
              redis.call('HINCRBY', KEYS[4], 'total_view_count', 1)
              redis.call('HINCRBY', KEYS[5], 'view_count', 1)
              redis.call('EXPIRE', KEYS[5], ARGV[2])
              redis.call('SADD', KEYS[7], ARGV[1])
              redis.call('SADD', KEYS[8], 'site')
              redis.call('SADD', KEYS[9], ARGV[3])
            end
            return {
              tonumber(redis.call('HGET', KEYS[3], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'like_count') or '0'),
              redis.call('SISMEMBER', KEYS[6], ARGV[1]),
              tonumber(redis.call('HGET', KEYS[4], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_like_count') or '0')
            }
            """);

    /**
     * 点赞脚本：SADD 天然幂等（重复点赞不重复计数）；点赞集合的 TTL 跟随访客键，
     * 使访客过期后其点赞关系一并失效；仅首次点赞时累加计数并登记脏集合。
     */
    private static final DefaultRedisScript<List> LIKE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl <= 0 then return redis.error_reply('VISITOR_EXPIRED') end
            local added = redis.call('SADD', KEYS[2], ARGV[1])
            redis.call('PEXPIRE', KEYS[2], ttl)
            if added == 1 then
              redis.call('HINCRBY', KEYS[3], 'like_count', 1)
              redis.call('HINCRBY', KEYS[4], 'total_like_count', 1)
              redis.call('HINCRBY', KEYS[5], 'like_count', 1)
              redis.call('EXPIRE', KEYS[5], ARGV[2])
              redis.call('SADD', KEYS[6], ARGV[1])
              redis.call('SADD', KEYS[7], 'site')
              redis.call('SADD', KEYS[8], ARGV[3])
            end
            return {
              tonumber(redis.call('HGET', KEYS[3], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'like_count') or '0'),
              1,
              tonumber(redis.call('HGET', KEYS[4], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_like_count') or '0')
            }
            """);

    /**
     * 取消点赞脚本：仅当点赞集合中确实存在（SREM=1）才减计数；减前判断当前值 > 0，
     * 防止 Redis 计数被快照/回填流程重置后出现负数。
     */
    private static final DefaultRedisScript<List> UNLIKE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            local removed = redis.call('SREM', KEYS[2], ARGV[1])
            if removed == 1 then
              local postLikes = tonumber(redis.call('HGET', KEYS[3], 'like_count') or '0')
              local siteLikes = tonumber(redis.call('HGET', KEYS[4], 'total_like_count') or '0')
              if postLikes > 0 then redis.call('HINCRBY', KEYS[3], 'like_count', -1) end
              if siteLikes > 0 then redis.call('HINCRBY', KEYS[4], 'total_like_count', -1) end
              redis.call('SADD', KEYS[5], ARGV[1])
              redis.call('SADD', KEYS[6], 'site')
            end
            return {
              tonumber(redis.call('HGET', KEYS[3], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'like_count') or '0'),
              0,
              tonumber(redis.call('HGET', KEYS[4], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[4], 'total_like_count') or '0')
            }
            """);

    /**
     * 访问脚本：30 分钟会话窗口去重（窗口内访问只续期不计数）；
     * 新会话才累加站点与当日访问数并登记脏集合。
     */
    private static final DefaultRedisScript<List> VISIT_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            if redis.call('EXISTS', KEYS[2]) == 1 then
              redis.call('PEXPIRE', KEYS[2], 1800000)
            else
              redis.call('SET', KEYS[2], '1', 'PX', 1800000)
              redis.call('HINCRBY', KEYS[3], 'visit_count', 1)
              redis.call('HINCRBY', KEYS[4], 'visit_count', 1)
              redis.call('EXPIRE', KEYS[4], ARGV[1])
              redis.call('SADD', KEYS[5], 'site')
              redis.call('SADD', KEYS[6], ARGV[2])
            end
            return {
              tonumber(redis.call('HGET', KEYS[3], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_like_count') or '0')
            }
            """);

    /** 解锁脚本：仅当锁值与自己持有的 token 一致才删除，避免误删他人（超时后新持锁者）的锁 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisEngagementStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean visitorExists(String visitorHash) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(visitorKey(visitorHash)));
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    @Override
    public void createVisitor(String visitorHash) {
        try {
            redis.opsForValue().set(visitorKey(visitorHash), "1", VISITOR_TTL);
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    @Override
    public List<EngagementDtos.EngagementSummary> engagement(List<String> postKeys) {
        if (postKeys.isEmpty()) return List.of();
        try {
            List<Object> pipelined = redis.executePipelined((RedisCallback<Object>) connection -> {
                byte[] viewField = bytes("view_count");
                byte[] likeField = bytes("like_count");
                for (String postKey : postKeys) {
                    connection.hashCommands().hMGet(bytes(engagementKey(postKey)), viewField, likeField);
                }
                return null;
            });
            List<EngagementDtos.EngagementSummary> result = new ArrayList<>(postKeys.size());
            for (int index = 0; index < postKeys.size(); index++) {
                Object row = pipelined.get(index);
                List<?> values = row instanceof List<?> list ? list : List.of();
                result.add(new EngagementDtos.EngagementSummary(
                        postKeys.get(index),
                        values.isEmpty() ? 0 : asLong(values.get(0)),
                        values.size() < 2 ? 0 : asLong(values.get(1))));
            }
            return result;
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    @Override
    public EngagementDtos.EngagementView registerView(String visitorHash, String postKey, LocalDate date) {
        List<?> values = execute(VIEW_SCRIPT, List.of(
                visitorKey(visitorHash), "blog:view:dedupe:" + postKey + ":" + visitorHash,
                engagementKey(postKey), SITE_METRICS_KEY, dailyKey(date), likesKey(visitorHash),
                DIRTY_STATS_KEY, DIRTY_SITE_KEY, DIRTY_DAILY_KEY),
                postKey, Long.toString(DAILY_TTL_SECONDS), date.toString());
        return engagementView(postKey, values);
    }

    @Override
    public EngagementDtos.EngagementView like(String visitorHash, String postKey, LocalDate date) {
        List<?> values = execute(LIKE_SCRIPT, List.of(
                visitorKey(visitorHash), likesKey(visitorHash), engagementKey(postKey), SITE_METRICS_KEY,
                dailyKey(date), DIRTY_STATS_KEY, DIRTY_SITE_KEY, DIRTY_DAILY_KEY),
                postKey, Long.toString(DAILY_TTL_SECONDS), date.toString());
        return engagementView(postKey, values);
    }

    @Override
    public EngagementDtos.EngagementView unlike(String visitorHash, String postKey) {
        List<?> values = execute(UNLIKE_SCRIPT, List.of(
                visitorKey(visitorHash), likesKey(visitorHash), engagementKey(postKey), SITE_METRICS_KEY,
                DIRTY_STATS_KEY, DIRTY_SITE_KEY), postKey);
        return engagementView(postKey, values);
    }

    @Override
    public EngagementDtos.SiteStatisticsView registerVisit(String visitorHash, LocalDate date) {
        List<?> values = execute(VISIT_SCRIPT, List.of(
                visitorKey(visitorHash), "blog:site:session:" + visitorHash, SITE_METRICS_KEY,
                dailyKey(date), DIRTY_SITE_KEY, DIRTY_DAILY_KEY),
                Long.toString(DAILY_TTL_SECONDS), date.toString());
        return siteView(values, 0);
    }

    @Override
    public EngagementDtos.SiteStatisticsView siteStatistics() {
        try {
            List<Object> values = redis.opsForHash().multiGet(
                    SITE_METRICS_KEY, List.of("visit_count", "total_view_count", "total_like_count"));
            return siteView(values, 0);
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    @Override
    public EngagementDtos.DailyStatisticsView dailyStatistics(LocalDate date) {
        try {
            List<Object> values = redis.opsForHash().multiGet(
                    dailyKey(date), List.of("visit_count", "view_count", "like_count"));
            return new EngagementDtos.DailyStatisticsView(date, asLong(values.get(0)),
                    asLong(values.get(1)), asLong(values.get(2)));
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    /** 从 PG 快照回填文章计数；putIfAbsent 保证不覆盖 Redis 中已有的更新值（Redis 为准） */
    public void restoreEngagement(EngagementDtos.EngagementSummary value) {
        redis.opsForHash().putIfAbsent(engagementKey(value.postKey()), "view_count", Long.toString(value.viewCount()));
        redis.opsForHash().putIfAbsent(engagementKey(value.postKey()), "like_count", Long.toString(value.likeCount()));
    }

    /** 从 PG 快照回填站点总计；同样不覆盖 Redis 已有值 */
    public void restoreSite(EngagementDtos.SiteStatisticsView value) {
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "visit_count", Long.toString(value.visitCount()));
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "total_view_count", Long.toString(value.totalViewCount()));
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "total_like_count", Long.toString(value.totalLikeCount()));
    }

    /** 从 PG 快照回填当日统计并重置 120 天 TTL（与 DAILY_TTL_SECONDS 口径一致） */
    public void restoreDaily(EngagementDtos.DailyStatisticsView value) {
        String key = dailyKey(value.date());
        redis.opsForHash().putIfAbsent(key, "visit_count", Long.toString(value.visitCount()));
        redis.opsForHash().putIfAbsent(key, "view_count", Long.toString(value.viewCount()));
        redis.opsForHash().putIfAbsent(key, "like_count", Long.toString(value.likeCount()));
        redis.expire(key, Duration.ofDays(120));
    }

    /**
     * 尝试获取快照分布式锁（SET NX，TTL 50 秒，小于 60 秒调度周期）。
     *
     * @return 成功返回锁 token（解锁时校验用），已有他人在跑则返回 null
     */
    public String trySnapshotLock() {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(SNAPSHOT_LOCK_KEY, token, Duration.ofSeconds(50)))
                ? token : null;
    }

    /** 按 token 校验释放快照锁（见 UNLOCK_SCRIPT），token 不匹配时静默不删 */
    public void releaseSnapshotLock(String token) {
        redis.execute(UNLOCK_SCRIPT, List.of(SNAPSHOT_LOCK_KEY), token);
    }

    /**
     * 认领本轮快照：先恢复上轮遗留的 processing 标记，再把三类 dirty 集合整体
     * 搬入对应 processing 集合并返回认领清单。SMOVE 逐个搬移，期间新产生的
     * 脏标记留在 dirty 中，自然归入下一轮。
     */
    public ClaimedSnapshot claimSnapshot() {
        recoverProcessing();
        Set<String> posts = moveAll(DIRTY_STATS_KEY, PROCESSING_STATS_KEY);
        Set<String> days = moveAll(DIRTY_DAILY_KEY, PROCESSING_DAILY_KEY);
        boolean site = Boolean.TRUE.equals(redis.opsForSet().move(DIRTY_SITE_KEY, "site", PROCESSING_SITE_KEY));
        return new ClaimedSnapshot(posts, days, site);
    }

    /** 把上轮任务失败遗留的 processing 标记搬回 dirty，使其在下轮快照中被重试 */
    public void recoverProcessing() {
        moveAll(PROCESSING_STATS_KEY, DIRTY_STATS_KEY);
        moveAll(PROCESSING_DAILY_KEY, DIRTY_DAILY_KEY);
        redis.opsForSet().move(PROCESSING_SITE_KEY, "site", DIRTY_SITE_KEY);
    }

    /**
     * 落库成功后的确认：从 processing 集合移除已持久化的标记。
     * 仅在 PG 写入成功后调用——失败则不 ack，标记留在 processing 等待恢复重试。
     */
    public void acknowledge(ClaimedSnapshot claimed) {
        removeAll(PROCESSING_STATS_KEY, claimed.postKeys());
        removeAll(PROCESSING_DAILY_KEY, claimed.days());
        if (claimed.site()) redis.opsForSet().remove(PROCESSING_SITE_KEY, "site");
    }

    private Set<String> moveAll(String source, String target) {
        Set<String> members = redis.opsForSet().members(source);
        if (members == null || members.isEmpty()) return Set.of();
        Set<String> moved = new LinkedHashSet<>();
        for (String member : members) {
            if (Boolean.TRUE.equals(redis.opsForSet().move(source, member, target))) moved.add(member);
        }
        return moved;
    }

    private void removeAll(String key, Collection<String> values) {
        if (!values.isEmpty()) redis.opsForSet().remove(key, values.toArray());
    }

    private List<?> execute(DefaultRedisScript<List> script, List<String> keys, String... args) {
        try {
            List<?> result = redis.execute(script, keys, (Object[]) args);
            if (result == null) throw new IllegalStateException("empty redis script response");
            return result;
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    private static EngagementDtos.EngagementView engagementView(String postKey, List<?> values) {
        return new EngagementDtos.EngagementView(postKey, asLong(values.get(0)), asLong(values.get(1)),
                asLong(values.get(2)) == 1, siteView(values, 3));
    }

    private static EngagementDtos.SiteStatisticsView siteView(List<?> values, int offset) {
        return new EngagementDtos.SiteStatisticsView(asLong(values.get(offset)), asLong(values.get(offset + 1)),
                asLong(values.get(offset + 2)), OffsetDateTime.now());
    }

    private static long asLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof byte[] raw) return Long.parseLong(new String(raw, StandardCharsets.UTF_8));
        return Long.parseLong(value.toString());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String visitorKey(String visitorHash) {
        return "blog:visitor:" + visitorHash;
    }

    private static String likesKey(String visitorHash) {
        return "blog:visitor:likes:" + visitorHash;
    }

    private static String engagementKey(String postKey) {
        return "blog:engagement:" + postKey;
    }

    private static String dailyKey(LocalDate date) {
        return "blog:daily:" + date;
    }

    private static DefaultRedisScript<List> script(String source) {
        return new DefaultRedisScript<>(source, List.class);
    }

    public record ClaimedSnapshot(Set<String> postKeys, Set<String> days, boolean site) {
        public boolean empty() {
            return postKeys.isEmpty() && days.isEmpty() && !site;
        }
    }
}
