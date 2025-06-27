package com.mlfq.mlfq;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SimulationDataManager {

    private ObservableList<HistoryEntry> historyEntries;
    private static final String HISTORY_FILE = "mlfq_sim_history.txt";

    public SimulationDataManager() {
        this.historyEntries = FXCollections.observableArrayList();
    }

    public ObservableList<HistoryEntry> getHistoryEntries() {
        return historyEntries;
    }

    public static class HistoryEntry {
        private String name;
        private List<int[]> processData;
        private double avgWaitingTime;
        private double throughput;
        private double avgResponseTime;
        private double cpuUtilization;

        public HistoryEntry(String name, List<int[]> processData, double avgWaitingTime, double throughput, double avgResponseTime, double cpuUtilization) {
            this.name = name;
            this.processData = new ArrayList<>();
            for (int[] p : processData) {
                this.processData.add(new int[]{p[0], p[1]});
            }
            this.avgWaitingTime = avgWaitingTime;
            this.throughput = throughput;
            this.avgResponseTime = avgResponseTime;
            this.cpuUtilization = cpuUtilization;
        }

        public String getName() {
            return name;
        }

        public List<int[]> getProcessData() {
            List<int[]> copy = new ArrayList<>();
            for (int[] p : processData) {
                copy.add(new int[]{p[0], p[1]});
            }
            return copy;
        }

        public double getAvgWaitingTime() {
            return avgWaitingTime;
        }

        public double getThroughput() {
            return throughput;
        }

        public double getAvgResponseTime() {
            return avgResponseTime;
        }

        public double getCpuUtilization() {
            return cpuUtilization;
        }

        @Override
        public String toString() {
            return name;
        }

        public String serialize() {
            StringBuilder sb = new StringBuilder(name);
            for (int[] p : processData) {
                sb.append(";").append(p[0]).append(",").append(p[1]);
            }
            sb.append(";").append(String.format("%.2f", avgWaitingTime));
            sb.append(";").append(String.format("%.4f", throughput));
            sb.append(";").append(String.format("%.2f", avgResponseTime));
            sb.append(";").append(String.format("%.4f", cpuUtilization));
            return sb.toString();
        }

        public static HistoryEntry deserialize(String data) {
            String[] parts = data.split(";");
            if (parts.length < 5) {
                System.err.println("Skipping malformed history entry: " + data);
                return null;
            }

            String name = parts[0];
            List<int[]> processData = new ArrayList<>();
            double avgWait = 0.0;
            double throughput = 0.0;
            double avgResponse = 0.0;
            double cpuUtil = 0.0;

            try {
                cpuUtil = Double.parseDouble(parts[parts.length - 1]);
                avgResponse = Double.parseDouble(parts[parts.length - 2]);
                throughput = Double.parseDouble(parts[parts.length - 3]);
                avgWait = Double.parseDouble(parts[parts.length - 4]);

                for (int i = 1; i < parts.length - 4; i++) {
                    String[] pData = parts[i].split(",");
                    if (pData.length == 2) {
                        processData.add(new int[]{Integer.parseInt(pData[0]), Integer.parseInt(pData[1])});
                    } else {
                        System.err.println("Error parsing process data in history: " + parts[i]);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing numeric data in history entry: " + e.getMessage());
                return null;
            }
            return new HistoryEntry(name, processData, avgWait, throughput, avgResponse, cpuUtil);
        }
    }

    public void addCurrentSimulationToHistory(List<int[]> initialProcessData, List<Process> completedProcesses, int simulationTotalTime, int cpuBusyTime) {
        if (initialProcessData.isEmpty() || completedProcesses.isEmpty() || completedProcesses.size() != initialProcessData.size()) {
            return;
        }

        double avgWait = 0.0;
        double throughput = 0.0;
        double avgResponse = 0.0;
        double cpuUtil = 0.0;

        double totalWaitingTime = completedProcesses.stream()
                .mapToDouble(Process::getWaitingTime)
                .sum();
        if (completedProcesses.size() > 0) {
            avgWait = totalWaitingTime / completedProcesses.size();
        }

        int maxCompletionTime = completedProcesses.stream()
                .mapToInt(Process::getCompletionTime)
                .max()
                .orElse(0);
        int minArrivalTime = initialProcessData.stream()
                .mapToInt(data -> data[0])
                .min()
                .orElse(0);

        if (maxCompletionTime - minArrivalTime > 0) {
            throughput = (double) completedProcesses.size() / (maxCompletionTime - minArrivalTime);
        } else if (completedProcesses.size() > 0) {
            throughput = (double) completedProcesses.size();
        }

        double totalResponseTime = completedProcesses.stream()
                .mapToInt(p -> p.getExecutionStartTime() - p.getArrivalTime())
                .sum();
        if (completedProcesses.size() > 0) {
            avgResponse = totalResponseTime / completedProcesses.size();
        }

        if (simulationTotalTime > 0) {
            cpuUtil = (double) cpuBusyTime / simulationTotalTime;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String name = "模拟 (" + timestamp + String.format(", 等待:%.2f", avgWait) + String.format(", 吞吐:%.4f", throughput) + String.format(", 响应:%.2f", avgResponse) + String.format(", CPU:%.2f%%", cpuUtil * 100) + ")";
        HistoryEntry newEntry = new HistoryEntry(name, initialProcessData, avgWait, throughput, avgResponse, cpuUtil);
        historyEntries.add(0, newEntry);
        saveHistoryToFile();
    }

    public void loadHistoryFromFile() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            System.out.println("历史记录文件不存在，无需加载。");
            return;
        }
        historyEntries.clear();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                HistoryEntry entry = HistoryEntry.deserialize(line);
                if (entry != null) {
                    historyEntries.add(entry);
                }
            }
            System.out.println("历史记录已从文件加载。加载了 " + historyEntries.size() + " 条记录。");
        } catch (IOException e) {
            System.err.println("加载历史记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveHistoryToFile() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(HISTORY_FILE), StandardCharsets.UTF_8))) {
            for (HistoryEntry entry : historyEntries) {
                writer.write(entry.serialize());
                writer.newLine();
            }
            System.out.println("历史记录已保存到文件。");
        } catch (IOException e) {
            System.err.println("保存历史记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearHistory() {
        historyEntries.clear();
        saveHistoryToFile();
    }
}
