package com.myblog.application.port;

public interface VisitCounter {

    void record(String day, String visitorId);

    long pageViews(String day);

    long uniqueVisitors(String day);
}
