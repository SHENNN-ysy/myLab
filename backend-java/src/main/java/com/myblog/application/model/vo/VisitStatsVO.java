package com.myblog.application.model.vo;

/** Aggregate visit counters returned to admins. */
public record VisitStatsVO(
        String date,
        Long pv,
        Long uv,
        @com.fasterxml.jackson.annotation.JsonProperty("total_pv") Long totalPv,
        @com.fasterxml.jackson.annotation.JsonProperty("total_uv") Long totalUv,
        @com.fasterxml.jackson.annotation.JsonProperty("total_visits") Long totalVisits) {
}
