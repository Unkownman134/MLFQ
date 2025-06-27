package com.mlfq.mlfq;

public class ExecutionSegment {
    private int processId;
    private int startTime;
    private int endTime;

    public ExecutionSegment(int processId, int startTime, int endTime) {
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getProcessId() {
        return processId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "P" + processId + " [" + startTime + "-" + endTime + ")";
    }
}
