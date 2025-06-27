package com.mlfq.mlfq;

public class Process {
    private static int nextId = 1;
    private int processId;
    private int arrivalTime;
    private int burstTime;
    private int remainingBurstTime;
    private int waitingTime;
    private int turnaroundTime;
    private int completionTime;
    private int currentQueueLevel;
    private int executionStartTime;
    private int lastExecutionEndTime;

    public Process(int arrivalTime, int burstTime) {
        this.processId = nextId++;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingBurstTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.completionTime = 0;
        this.currentQueueLevel = 1;
        this.executionStartTime = -1;
        this.lastExecutionEndTime = -1;
    }

    public int getProcessId() {
        return processId;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getRemainingBurstTime() {
        return remainingBurstTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public int getCurrentQueueLevel() {
        return currentQueueLevel;
    }

    public int getExecutionStartTime() {
        return executionStartTime;
    }

    public int getLastExecutionEndTime() {
        return lastExecutionEndTime;
    }

    public void setRemainingBurstTime(int remainingBurstTime) {
        this.remainingBurstTime = remainingBurstTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }

    public void setCurrentQueueLevel(int currentQueueLevel) {
        this.currentQueueLevel = currentQueueLevel;
    }

    public void setExecutionStartTime(int executionStartTime) {
        this.executionStartTime = executionStartTime;
    }

    public void setLastExecutionEndTime(int lastExecutionEndTime) {
        this.lastExecutionEndTime = lastExecutionEndTime;
    }

    public void addWaitingTime(int time) {
        this.waitingTime += time;
    }

    public static void resetNextId() {
        nextId = 1;
    }
}
