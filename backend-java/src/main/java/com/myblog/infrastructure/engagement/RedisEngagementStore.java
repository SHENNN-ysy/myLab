package com.myblog.infrastructure.engagement;

import com.myblog.application.model.dto.EngagementDtos;
import com.myblog.application.port.EngagementStore;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.EngagementUnavailableException;
import com.myblog.common.properties.VisitorProperties;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 互动实时统计实现：浏览/点赞/访问的实时计数只活在 Redis，匿名访客
 * 以 HMAC 散列后的 visitorHash 作为 Hash 键（24h 滑动 TTL），不持久化任何访客明细。
 * <p>
 * 一致性模型：
 * <ul>
 *   <li>所有跨键计数变化均由 Lua 脚本原子完成（校验访客、去重、加减计数、
 *       追加 Stream 通知一次提交），避免并发请求下计数与落库通知脱节；</li>
 *   <li>每次有效计数都在同一 Lua 中追加 Redis Stream 脏聚合通知；Consumer Group
 *       批量读取 Redis 最新绝对值，落 PG 成功后才确认消息，提供至少一次处理；</li>
 *   <li>PG 中的值是 Redis 绝对值的周期性快照（整体覆盖而非增量），
 *       重启时再从 PG 回填 Redis 兜底（putIfAbsent，不覆盖 Redis 已有值）。
 * </ul>
 */
@Component
public class RedisEngagementStore implements EngagementStore {
    static final String SITE_METRICS_KEY = RedisKeyPrefix.BLOG + "site:metrics";
    private static final long DAILY_TTL_SECONDS = Duration.ofDays(120).toSeconds();

    /** 创建只含元数据的新访客 Hash；访问与页面去重字段由后续业务脚本原子写入。 */
    private static final DefaultRedisScript<Long> CREATE_VISITOR_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'created_at', ARGV[1], 'last_seen_at', ARGV[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    /**
     * 页面浏览脚本：一份凭证只登记一次访问，每个页面字段只登记一次浏览；
     * 详情页同时累加文章浏览，全部状态与聚合更新在一个 Lua 事务内完成。
     */
    private static final DefaultRedisScript<List> PAGE_VIEW_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            redis.call('HSET', KEYS[1], 'last_seen_at', ARGV[7])
            redis.call('PEXPIRE', KEYS[1], ARGV[6])
            local firstVisit = redis.call('HSETNX', KEYS[1], 'visit', ARGV[7])
            local firstView = redis.call('HSETNX', KEYS[1], ARGV[2], ARGV[7])
            if firstVisit == 1 then
              redis.call('HINCRBY', KEYS[3], 'visit_count', 1)
              redis.call('HINCRBY', KEYS[4], 'visit_count', 1)
            end
            if firstView == 1 then
              redis.call('HINCRBY', KEYS[3], 'total_view_count', 1)
              redis.call('HINCRBY', KEYS[4], 'view_count', 1)
              if ARGV[8] == '1' then redis.call('HINCRBY', KEYS[2], 'view_count', 1) end
            end
            if firstVisit == 1 or firstView == 1 then
              redis.call('EXPIRE', KEYS[4], ARGV[4])
              if ARGV[8] == '1' and firstView == 1 then
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VIEW', 'post_key', ARGV[1], 'stat_date', ARGV[5],
                  'post_dirty', '1', 'site_dirty', '1', 'daily_dirty', '1')
              else
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VISIT', 'post_key', '', 'stat_date', ARGV[5],
                  'post_dirty', '0', 'site_dirty', '1', 'daily_dirty', '1')
              end
            end
            return {
              tonumber(redis.call('HGET', KEYS[2], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[2], 'like_count') or '0'),
              redis.call('HEXISTS', KEYS[1], ARGV[3]),
              tonumber(redis.call('HGET', KEYS[3], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_like_count') or '0')
            }
            """);

    /**
     * 点赞脚本：点赞字段 HSETNX 天然幂等；若详情浏览尚未登记，则同时补记访问和浏览，
     * 保证新口径下每次访问至少对应一个页面浏览。
     */
    private static final DefaultRedisScript<List> LIKE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            redis.call('HSET', KEYS[1], 'last_seen_at', ARGV[7])
            redis.call('PEXPIRE', KEYS[1], ARGV[6])
            local firstVisit = redis.call('HSETNX', KEYS[1], 'visit', ARGV[7])
            local firstView = redis.call('HSETNX', KEYS[1], ARGV[2], ARGV[7])
            local added = redis.call('HSETNX', KEYS[1], ARGV[3], ARGV[7])
            if firstVisit == 1 then
              redis.call('HINCRBY', KEYS[3], 'visit_count', 1)
              redis.call('HINCRBY', KEYS[4], 'visit_count', 1)
            end
            if firstView == 1 then
              redis.call('HINCRBY', KEYS[2], 'view_count', 1)
              redis.call('HINCRBY', KEYS[3], 'total_view_count', 1)
              redis.call('HINCRBY', KEYS[4], 'view_count', 1)
            end
            if added == 1 then
              redis.call('HINCRBY', KEYS[2], 'like_count', 1)
              redis.call('HINCRBY', KEYS[3], 'total_like_count', 1)
              redis.call('HINCRBY', KEYS[4], 'like_count', 1)
            end
            if firstVisit == 1 or firstView == 1 or added == 1 then
              redis.call('EXPIRE', KEYS[4], ARGV[4])
              if added == 1 then
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'LIKE', 'post_key', ARGV[1], 'stat_date', ARGV[5],
                  'post_dirty', '1', 'site_dirty', '1', 'daily_dirty', '1')
              elseif firstView == 1 then
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VIEW', 'post_key', ARGV[1], 'stat_date', ARGV[5],
                  'post_dirty', '1', 'site_dirty', '1', 'daily_dirty', '1')
              else
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VISIT', 'post_key', '', 'stat_date', ARGV[5],
                  'post_dirty', '0', 'site_dirty', '1', 'daily_dirty', '1')
              end
            end
            return {
              tonumber(redis.call('HGET', KEYS[2], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[2], 'like_count') or '0'),
              1,
              tonumber(redis.call('HGET', KEYS[3], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_like_count') or '0')
            }
            """);

    /**
     * 取消点赞脚本：HDEL 成功才减计数；若详情尚未浏览则先补记访问和浏览。
     * 浏览与取消点赞可能分别产生 VIEW、UNLIKE 两条脏通知，消费者读取绝对值后幂等落库。
     */
    private static final DefaultRedisScript<List> UNLIKE_SCRIPT = script("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return redis.error_reply('VISITOR_EXPIRED') end
            redis.call('HSET', KEYS[1], 'last_seen_at', ARGV[7])
            redis.call('PEXPIRE', KEYS[1], ARGV[6])
            local firstVisit = redis.call('HSETNX', KEYS[1], 'visit', ARGV[7])
            local firstView = redis.call('HSETNX', KEYS[1], ARGV[2], ARGV[7])
            local removed = redis.call('HDEL', KEYS[1], ARGV[3])
            if firstVisit == 1 then
              redis.call('HINCRBY', KEYS[3], 'visit_count', 1)
              redis.call('HINCRBY', KEYS[4], 'visit_count', 1)
            end
            if firstView == 1 then
              redis.call('HINCRBY', KEYS[2], 'view_count', 1)
              redis.call('HINCRBY', KEYS[3], 'total_view_count', 1)
              redis.call('HINCRBY', KEYS[4], 'view_count', 1)
            end
            if removed == 1 then
              local postLikes = tonumber(redis.call('HGET', KEYS[2], 'like_count') or '0')
              local siteLikes = tonumber(redis.call('HGET', KEYS[3], 'total_like_count') or '0')
              if postLikes > 0 then redis.call('HINCRBY', KEYS[2], 'like_count', -1) end
              if siteLikes > 0 then redis.call('HINCRBY', KEYS[3], 'total_like_count', -1) end
            end
            if firstVisit == 1 or firstView == 1 then
              redis.call('EXPIRE', KEYS[4], ARGV[4])
              if firstView == 1 then
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VIEW', 'post_key', ARGV[1], 'stat_date', ARGV[5],
                  'post_dirty', '1', 'site_dirty', '1', 'daily_dirty', '1')
              else
                redis.call('XADD', KEYS[5], '*',
                  'event_type', 'VISIT', 'post_key', '', 'stat_date', ARGV[5],
                  'post_dirty', '0', 'site_dirty', '1', 'daily_dirty', '1')
              end
            end
            if removed == 1 then
              redis.call('XADD', KEYS[5], '*',
                'event_type', 'UNLIKE', 'post_key', ARGV[1], 'stat_date', '',
                'post_dirty', '1', 'site_dirty', '1', 'daily_dirty', '0')
            end
            return {
              tonumber(redis.call('HGET', KEYS[2], 'view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[2], 'like_count') or '0'),
              0,
              tonumber(redis.call('HGET', KEYS[3], 'visit_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_view_count') or '0'),
              tonumber(redis.call('HGET', KEYS[3], 'total_like_count') or '0')
            }
            """);

    private final StringRedisTemplate redis;
    private final long visitorTtlMillis;

    public RedisEngagementStore(StringRedisTemplate redis, VisitorProperties visitorProperties) {
        this.redis = redis;
        this.visitorTtlMillis = visitorProperties.visitorIdentityTtl().toMillis();
    }

    /** 判断访客凭证是否仍在有效期内；该查询不刷新滑动 TTL。 */
    @Override
    public boolean visitorExists(String visitorHash) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(visitorKey(visitorHash)));
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    /** 登记新访客身份元数据并设置滑动 TTL；访问与浏览去重字段由后续业务脚本原子写入。 */
    @Override
    public void createVisitor(String visitorHash) {
        try {
            String now = Long.toString(System.currentTimeMillis());
            redis.execute(CREATE_VISITOR_SCRIPT, List.of(visitorKey(visitorHash)),
                    now, Long.toString(visitorTtlMillis));
        } catch (RuntimeException exception) {
            throw new EngagementUnavailableException();
        }
    }

    /** 流水线批量读取文章浏览/点赞绝对值，缺失计数按 0 返回；结果顺序与入参一致。 */
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

    /** 登记页面浏览；非详情页共用 "_page" 计数桶只做页面级去重，不累加文章计数。 */
    @Override
    public EngagementDtos.PageViewResult registerPageView(
            String visitorHash, EngagementDtos.PageType pageType, String postKey, LocalDate date) {
        boolean detail = pageType == EngagementDtos.PageType.MYLAB_DETAIL;
        String effectivePostKey = detail ? postKey : "_page";
        List<?> values = execute(PAGE_VIEW_SCRIPT, List.of(
                visitorKey(visitorHash), engagementKey(effectivePostKey), SITE_METRICS_KEY, dailyKey(date),
                RedisEngagementEventStream.STREAM_KEY),
                detail ? postKey : "", viewField(pageType, postKey), likeField(effectivePostKey),
                Long.toString(DAILY_TTL_SECONDS), date.toString(), Long.toString(visitorTtlMillis),
                Long.toString(System.currentTimeMillis()), detail ? "1" : "0");
        return new EngagementDtos.PageViewResult(pageType.wireValue(), detail ? postKey : null,
                detail ? asLong(values.get(0)) : null,
                detail ? asLong(values.get(1)) : null,
                detail ? asLong(values.get(2)) == 1 : null,
                siteView(values, 3));
    }

    /** 原子登记点赞（HSETNX 幂等），返回文章与站点最新绝对值。 */
    @Override
    public EngagementDtos.EngagementView like(String visitorHash, String postKey, LocalDate date) {
        List<?> values = execute(LIKE_SCRIPT, List.of(
                visitorKey(visitorHash), engagementKey(postKey), SITE_METRICS_KEY, dailyKey(date),
                RedisEngagementEventStream.STREAM_KEY),
                postKey, viewField(EngagementDtos.PageType.MYLAB_DETAIL, postKey), likeField(postKey),
                Long.toString(DAILY_TTL_SECONDS), date.toString(), Long.toString(visitorTtlMillis),
                Long.toString(System.currentTimeMillis()));
        return engagementView(postKey, values);
    }

    /** 取消点赞（HDEL 成功才减计数，保护性不为负），返回文章与站点最新绝对值。 */
    @Override
    public EngagementDtos.EngagementView unlike(String visitorHash, String postKey, LocalDate date) {
        List<?> values = execute(UNLIKE_SCRIPT, List.of(
                visitorKey(visitorHash), engagementKey(postKey), SITE_METRICS_KEY, dailyKey(date),
                RedisEngagementEventStream.STREAM_KEY),
                postKey, viewField(EngagementDtos.PageType.MYLAB_DETAIL, postKey), likeField(postKey),
                Long.toString(DAILY_TTL_SECONDS), date.toString(), Long.toString(visitorTtlMillis),
                Long.toString(System.currentTimeMillis()));
        return engagementView(postKey, values);
    }

    /** 读取站点访问量、总浏览量、总点赞量的实时绝对值。 */
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

    /** 读取指定业务日期的访问量、浏览量、点赞量实时绝对值。 */
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
        return RedisKeyPrefix.BLOG + "visitor:v2:" + visitorHash;
    }

    private static String viewField(EngagementDtos.PageType pageType, String postKey) {
        return switch (pageType) {
            case HOME -> "view:home";
            case MYLAB -> "view:mylab";
            case MYLAB_DETAIL -> "view:post:" + postKey;
        };
    }

    private static String likeField(String postKey) {
        return "like:" + postKey;
    }

    private static String engagementKey(String postKey) {
        return RedisKeyPrefix.BLOG + "engagement:" + postKey;
    }

    private static String dailyKey(LocalDate date) {
        return RedisKeyPrefix.BLOG + "daily:" + date;
    }

    private static DefaultRedisScript<List> script(String source) {
        return new DefaultRedisScript<>(source, List.class);
    }

}
