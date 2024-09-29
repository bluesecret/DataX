package com.alibaba.datax.core.job.event;

public class JobStatistics {

    private long startTimeStamp;

    private long endTimeStamp;

    private long totalCosts;

    private long byteSpeedPerSecond;

    private long recordSpeedPerSecond;

    private long totalReadRecords;

    private long totalErrorRecords;

    public JobStatistics(long startTimeStamp, long endTimeStamp, long totalCosts, long byteSpeedPerSecond, long recordSpeedPerSecond, long totalReadRecords, long totalErrorRecords) {
        this.startTimeStamp = startTimeStamp;
        this.endTimeStamp = endTimeStamp;
        this.totalCosts = totalCosts;
        this.byteSpeedPerSecond = byteSpeedPerSecond;
        this.recordSpeedPerSecond = recordSpeedPerSecond;
        this.totalReadRecords = totalReadRecords;
        this.totalErrorRecords = totalErrorRecords;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public long getEndTimeStamp() {
        return endTimeStamp;
    }

    public void setEndTimeStamp(long endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    public long getTotalCosts() {
        return totalCosts;
    }

    public void setTotalCosts(long totalCosts) {
        this.totalCosts = totalCosts;
    }

    public long getByteSpeedPerSecond() {
        return byteSpeedPerSecond;
    }

    public void setByteSpeedPerSecond(long byteSpeedPerSecond) {
        this.byteSpeedPerSecond = byteSpeedPerSecond;
    }

    public long getRecordSpeedPerSecond() {
        return recordSpeedPerSecond;
    }

    public void setRecordSpeedPerSecond(long recordSpeedPerSecond) {
        this.recordSpeedPerSecond = recordSpeedPerSecond;
    }

    public long getTotalReadRecords() {
        return totalReadRecords;
    }

    public void setTotalReadRecords(long totalReadRecords) {
        this.totalReadRecords = totalReadRecords;
    }

    public long getTotalErrorRecords() {
        return totalErrorRecords;
    }

    public void setTotalErrorRecords(long totalErrorRecords) {
        this.totalErrorRecords = totalErrorRecords;
    }
}
