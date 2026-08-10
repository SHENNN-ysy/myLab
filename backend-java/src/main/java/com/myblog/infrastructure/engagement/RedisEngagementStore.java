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

/** Redis 实时统计实现。所有跨键计数变化均由 Lua 原子完成。 */
@Component
public class RedisEngagementStore implements EngagementStore {
    static final String SITE_METRICS_KEY = "blog:site:metrics";
    static final String DIRTY_STATS_KEY = "blog:dirty:stats";
    static final String PROCESSING_STATS_KEY = "blog:processing:stats";
    static final String DIRTY_SITE_KEY = "blog:dirty:site";
    static final String PROCESSING_SITE_KEY = "blog:processing:site";
    static final String DIRTY_DAILY_KEY = "blog:dirty:daily";
    static final String PROCESSING_DAILY_KEY = "blog:processing:daily";
    private static final String SNAPSHOT_LOCK_KEY = "blog:lock:snapshot";
    private static final Duration VISITOR_TTL = Duration.ofHours(72);
    private static final long DAILY_TTL_SECONDS = Duration.ofDays(120).toSeconds();

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

    public void restoreEngagement(EngagementDtos.EngagementSummary value) {
        redis.opsForHash().putIfAbsent(engagementKey(value.postKey()), "view_count", Long.toString(value.viewCount()));
        redis.opsForHash().putIfAbsent(engagementKey(value.postKey()), "like_count", Long.toString(value.likeCount()));
    }

    public void restoreSite(EngagementDtos.SiteStatisticsView value) {
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "visit_count", Long.toString(value.visitCount()));
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "total_view_count", Long.toString(value.totalViewCount()));
        redis.opsForHash().putIfAbsent(SITE_METRICS_KEY, "total_like_count", Long.toString(value.totalLikeCount()));
    }

    public void restoreDaily(EngagementDtos.DailyStatisticsView value) {
        String key = dailyKey(value.date());
        redis.opsForHash().putIfAbsent(key, "visit_count", Long.toString(value.visitCount()));
        redis.opsForHash().putIfAbsent(key, "view_count", Long.toString(value.viewCount()));
        redis.opsForHash().putIfAbsent(key, "like_count", Long.toString(value.likeCount()));
        redis.expire(key, Duration.ofDays(120));
    }

    public String trySnapshotLock() {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(SNAPSHOT_LOCK_KEY, token, Duration.ofSeconds(50)))
                ? token : null;
    }

    public void releaseSnapshotLock(String token) {
        redis.execute(UNLOCK_SCRIPT, List.of(SNAPSHOT_LOCK_KEY), token);
    }

    public ClaimedSnapshot claimSnapshot() {
        recoverProcessing();
        Set<String> posts = moveAll(DIRTY_STATS_KEY, PROCESSING_STATS_KEY);
        Set<String> days = moveAll(DIRTY_DAILY_KEY, PROCESSING_DAILY_KEY);
        boolean site = Boolean.TRUE.equals(redis.opsForSet().move(DIRTY_SITE_KEY, "site", PROCESSING_SITE_KEY));
        return new ClaimedSnapshot(posts, days, site);
    }

    public void recoverProcessing() {
        moveAll(PROCESSING_STATS_KEY, DIRTY_STATS_KEY);
        moveAll(PROCESSING_DAILY_KEY, DIRTY_DAILY_KEY);
        redis.opsForSet().move(PROCESSING_SITE_KEY, "site", DIRTY_SITE_KEY);
    }

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
