package com.mlfq.mlfq;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class MlfqScheduler {

    private List<Process> currentSimulationProcesses;
    private LinkedList<Process> newArrivals;
    private Queue<Process> queue1;
    private Queue<Process> queue2;
    private Queue<Process> queue3;
    private List<Process> completedProcesses;

    private final int TIME_QUANTUM_Q1 = 5;
    private final int TIME_QUANTUM_Q2 = 10;
    private int currentTime;
    private Process currentlyExecutingProcess;
    private int currentQuantumUsed;
    private int cpuBusyTime;

    private List<Map.Entry<Integer, Integer>> cpuLog;

    public MlfqScheduler() {
        this.currentSimulationProcesses = new ArrayList<>();
        this.newArrivals = new LinkedList<>();
        this.queue1 = new LinkedList<>();
        this.queue2 = new LinkedList<>();
        this.queue3 = new LinkedList<>();
        this.completedProcesses = new ArrayList<>();
        this.cpuLog = new ArrayList<>();
        reset();
    }

    public void reset(List<int[]> initialProcessData) {
        Process.resetNextId();
        currentSimulationProcesses.clear();
        newArrivals.clear();
        queue1.clear();
        queue2.clear();
        queue3.clear();
        completedProcesses.clear();
        cpuLog.clear();

        currentTime = 0;
        currentlyExecutingProcess = null;
        currentQuantumUsed = 0;
        cpuBusyTime = 0;

        for (int[] data : initialProcessData) {
            Process newProcess = new Process(data[0], data[1]);
            currentSimulationProcesses.add(newProcess);
        }
        currentSimulationProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));
        newArrivals.addAll(currentSimulationProcesses);
    }

    public void reset() {
        reset(new ArrayList<>());
    }

    public boolean simulateOneTimeUnit() {
        if (isSimulationFinished()) {
            if (cpuLog.isEmpty() || cpuLog.get(cpuLog.size() - 1).getKey() < currentTime) {
                cpuLog.add(new AbstractMap.SimpleEntry<>(currentTime, -1));
            }
            return false;
        }

        newArrivals.removeIf(p -> {
            if (p.getArrivalTime() <= currentTime) {
                queue1.offer(p);
                return true;
            }
            return false;
        });

        for (Process p : queue1) {
            p.addWaitingTime(1);
        }
        for (Process p : queue2) {
            p.addWaitingTime(1);
        }
        for (Process p : queue3) {
            p.addWaitingTime(1);
        }

        Process previousExecutingProcess = currentlyExecutingProcess;

        if (currentlyExecutingProcess == null || currentlyExecutingProcess.getRemainingBurstTime() == 0) {
            if (currentlyExecutingProcess != null && currentlyExecutingProcess.getRemainingBurstTime() == 0) {
                currentlyExecutingProcess.setCompletionTime(currentTime);
                currentlyExecutingProcess.setTurnaroundTime(currentlyExecutingProcess.getCompletionTime() - currentlyExecutingProcess.getArrivalTime());
                completedProcesses.add(currentlyExecutingProcess);
                currentlyExecutingProcess.setLastExecutionEndTime(currentTime);
                currentlyExecutingProcess = null;
            }

            if (!queue1.isEmpty()) {
                currentlyExecutingProcess = queue1.poll();
                currentQuantumUsed = 0;
            } else if (!queue2.isEmpty()) {
                currentlyExecutingProcess = queue2.poll();
                currentQuantumUsed = 0;
            } else if (!queue3.isEmpty()) {
                currentlyExecutingProcess = queue3.poll();
                currentQuantumUsed = 0;
            } else {
                if (completedProcesses.size() == currentSimulationProcesses.size()) {
                    cpuLog.add(new AbstractMap.SimpleEntry<>(currentTime, -1));
                    currentTime++;
                    return false;
                } else {
                    currentlyExecutingProcess = null;
                    cpuLog.add(new AbstractMap.SimpleEntry<>(currentTime, -1));
                    currentTime++;
                    return true;
                }
            }

            if (currentlyExecutingProcess != null && currentlyExecutingProcess.getExecutionStartTime() == -1) {
                currentlyExecutingProcess.setExecutionStartTime(currentTime);
            }
        }

        if (currentlyExecutingProcess != null) {
            currentlyExecutingProcess.setRemainingBurstTime(currentlyExecutingProcess.getRemainingBurstTime() - 1);
            currentQuantumUsed++;
            cpuBusyTime++;

            cpuLog.add(new AbstractMap.SimpleEntry<>(currentTime, currentlyExecutingProcess.getProcessId()));

            boolean quantumExpired = false;
            if (currentlyExecutingProcess.getCurrentQueueLevel() == 1 && currentQuantumUsed >= TIME_QUANTUM_Q1) {
                quantumExpired = true;
            } else if (currentlyExecutingProcess.getCurrentQueueLevel() == 2 && currentQuantumUsed >= TIME_QUANTUM_Q2) {
                quantumExpired = true;
            }

            if (currentlyExecutingProcess.getRemainingBurstTime() == 0) {
                currentlyExecutingProcess.setCompletionTime(currentTime + 1);
                currentlyExecutingProcess.setTurnaroundTime(currentlyExecutingProcess.getCompletionTime() - currentlyExecutingProcess.getArrivalTime());
                completedProcesses.add(currentlyExecutingProcess);
                currentlyExecutingProcess.setLastExecutionEndTime(currentTime + 1);
                currentlyExecutingProcess = null;
            } else if (quantumExpired) {
                int oldQueueLevel = currentlyExecutingProcess.getCurrentQueueLevel();
                if (currentlyExecutingProcess.getCurrentQueueLevel() == 1) {
                    currentlyExecutingProcess.setCurrentQueueLevel(2);
                    queue2.offer(currentlyExecutingProcess);
                } else if (currentlyExecutingProcess.getCurrentQueueLevel() == 2) {
                    currentlyExecutingProcess.setCurrentQueueLevel(3);
                    queue3.offer(currentlyExecutingProcess);
                }
                currentlyExecutingProcess.setLastExecutionEndTime(currentTime + 1);
                currentlyExecutingProcess = null;
                currentQuantumUsed = 0;
            }
        } else {
            cpuLog.add(new AbstractMap.SimpleEntry<>(currentTime, -1));
        }

        currentTime++;

        return !isSimulationFinished();
    }

    public boolean isSimulationFinished() {
        if (currentSimulationProcesses.isEmpty()) return false;

        return completedProcesses.size() == currentSimulationProcesses.size() &&
                newArrivals.isEmpty() &&
                queue1.isEmpty() && queue2.isEmpty() && queue3.isEmpty() &&
                currentlyExecutingProcess == null;
    }


    public int getCurrentTime() {
        return currentTime;
    }

    public Process getCurrentlyExecutingProcess() {
        return currentlyExecutingProcess;
    }

    public Queue<Process> getQueue1() {
        return queue1;
    }

    public Queue<Process> getQueue2() {
        return queue2;
    }

    public Queue<Process> getQueue3() {
        return queue3;
    }

    public List<Process> getCompletedProcesses() {
        return completedProcesses;
    }

    public int getCpuBusyTime() {
        return cpuBusyTime;
    }

    public List<Process> getCurrentSimulationProcesses() {
        return currentSimulationProcesses;
    }

    public List<Map.Entry<Integer, Integer>> getCpuLog() {
        return new ArrayList<>(cpuLog);
    }

    public List<ExecutionSegment> getConsolidatedExecutionLog() {
        List<ExecutionSegment> consolidatedLog = new ArrayList<>();
        if (cpuLog.isEmpty()) {
            return consolidatedLog;
        }

        int currentProcessId = -2;
        int segmentStartTime = 0;

        for (int i = 0; i < cpuLog.size(); i++) {
            Map.Entry<Integer, Integer> entry = cpuLog.get(i);
            int time = entry.getKey();
            int processId = entry.getValue();

            if (i == 0) {
                currentProcessId = processId;
                segmentStartTime = time;
            } else if (processId != currentProcessId) {
                consolidatedLog.add(new ExecutionSegment(currentProcessId, segmentStartTime, time));
                currentProcessId = processId;
                segmentStartTime = time;
            }

            if (i == cpuLog.size() - 1) {
                consolidatedLog.add(new ExecutionSegment(currentProcessId, segmentStartTime, time + 1));
            }
        }
        return consolidatedLog;
    }
}
