package com.bytegen.common.http.magic;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class TimeTracker {

    private long startTime;

    public TimeTracker() {
        super();
        startTime = currentMillis();
    }

    public long getDuration() {
        return currentMillis() - startTime;
    }

    public void reset() {
        startTime = currentMillis();
    }

    private long currentMillis() {
        return System.currentTimeMillis();
    }
}
