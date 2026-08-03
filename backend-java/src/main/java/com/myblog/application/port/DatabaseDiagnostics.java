package com.myblog.application.port;

public interface DatabaseDiagnostics {

    boolean available();

    long tableCount();

    long databaseSize();

    long connectionCount();
}
