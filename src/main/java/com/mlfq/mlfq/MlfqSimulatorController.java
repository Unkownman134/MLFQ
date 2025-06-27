package com.mlfq.mlfq;

import com.mlfq.mlfq.SimulationDataManager.HistoryEntry;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MlfqSimulatorController {

    @FXML
    private TextField arrivalTimeField;
    @FXML
    private TextField burstTimeField;
    @FXML
    private VBox processListVBox;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button stepButton;
    @FXML
    private Button modeToggleButton;
    @FXML
    private VBox timeSliderContainer;
    @FXML
    private Slider timeSlider;

    @FXML
    private Label queue1StatusLabel;
    @FXML
    private Label queue2StatusLabel;
    @FXML
    private Label queue3StatusLabel;
    @FXML
    private Label completedProcessesLabel;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label currentProcessLabel;
    @FXML
    private Label avgWaitingTimeLabel;
    @FXML
    private Label throughputLabel;

    @FXML
    private ListView<HistoryEntry> historyListView;
    @FXML
    private TextArea simulationLogTextArea;

    @FXML
    private Label avgResponseTimeLabel;
    @FXML
    private Label cpuUtilizationLabel;

    @FXML
    private StackPane rootStackPane;
    @FXML
    private VBox modalBackground;
    @FXML
    private VBox historyDetailModal;
    @FXML
    private Label modalSimulationNameLabel;
    @FXML
    private Label modalProcessDataLabel;
    @FXML
    private Label modalAvgWaitingTimeLabel;
    @FXML
    private Label modalThroughputLabel;
    @FXML
    private Label modalAvgResponseTimeLabel;
    @FXML
    private Label modalCpuUtilizationLabel;

    @FXML
    private HBox mainContentHBox;

    private enum SimulationMode {
        AUTO, MANUAL
    }

    private MlfqScheduler scheduler;
    private SimulationDataManager dataManager;

    private List<int[]> initialProcessData = new ArrayList<>();

    private AnimationTimer simulationTimer;
    private SimulationMode currentMode;
    private boolean simulationActive;
    private boolean autoModePaused;

    private Stage ganttChartStage;
    private GanttChartController ganttChartController;


    @FXML
    public void initialize() {
        scheduler = new MlfqScheduler();
        dataManager = new SimulationDataManager();

        historyListView.setItems(dataManager.getHistoryEntries());

        dataManager.loadHistoryFromFile();

        historyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                handleHistoryClick();
            }
        });

        currentMode = SimulationMode.AUTO;

        resetSimulationState();

        arrivalTimeField.setText("0,5,10,15");
        burstTimeField.setText("20,30,15,25");
        updateProcessListUI();

        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!timeSlider.isValueChanging()) {
                if (currentMode == SimulationMode.MANUAL) {
                    handleTimeSliderChange();
                }
            }
        });

        historyDetailModal.setVisible(false);
        modalBackground.setVisible(false);
        mainContentHBox.setDisable(false);

        simulationLogTextArea.setText("");
    }

    private void resetSimulationState() {
        if (simulationTimer != null) {
            simulationTimer.stop();
            simulationTimer = null;
        }
        autoModePaused = false;

        scheduler.reset(initialProcessData);

        updateUI();
        playPauseButton.setText("开始模拟");
        simulationLogTextArea.setText("");

        if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText("平均等待时间: 0.0 ms");
        if (cpuUtilizationLabel != null) cpuUtilizationLabel.setText("CPU 利用率: 0.00 %");

        if (ganttChartStage != null && ganttChartStage.isShowing()) {
            ganttChartStage.close();
            ganttChartStage = null;
            ganttChartController = null;
        }
    }

    @FXML
    protected void handleAddProcess() {
        if (simulationActive) {
            showAlert("错误", "模拟正在进行中，无法添加新进程。请先重置模拟。");
            return;
        }

        try {
            String arrivalTimesStr = arrivalTimeField.getText().trim();
            String burstTimesStr = burstTimeField.getText().trim();

            if (arrivalTimesStr.isEmpty() || burstTimesStr.isEmpty()) {
                showAlert("输入错误", "到达时间或突发时间不能为空。");
                return;
            }

            String[] arrivalTimesArray = arrivalTimesStr.split(",");
            String[] burstTimesArray = burstTimesStr.split(",");

            if (arrivalTimesArray.length != burstTimesArray.length) {
                showAlert("输入错误", "到达时间数量与突发时间数量不匹配。");
                return;
            }

            List<int[]> tempProcessData = new ArrayList<>();
            for (int i = 0; i < arrivalTimesArray.length; i++) {
                int arrivalTime = Integer.parseInt(arrivalTimesArray[i].trim());
                int burstTime = Integer.parseInt(burstTimesArray[i].trim());

                if (arrivalTime < 0 || burstTime <= 0) {
                    showAlert("输入错误", "到达时间不能为负，突发时间必须为正数。");
                    return;
                }
                tempProcessData.add(new int[]{arrivalTime, burstTime});
            }

            initialProcessData.clear();
            initialProcessData.addAll(tempProcessData);

            resetSimulationState();
            updateProcessListUI();
            arrivalTimeField.clear();
            burstTimeField.clear();
            appendToLog("已添加新进程数据并重置模拟。");
        } catch (NumberFormatException e) {
            showAlert("输入错误", "请输入有效的整数值。");
            appendToLog("错误: 输入了无效的进程数据。");
        } catch (Exception e) {
            showAlert("错误", "添加进程时发生未知错误: " + e.getMessage());
            appendToLog("错误: 添加进程时发生未知错误 - " + e.getMessage());
        }
    }

    private void updateProcessListUI() {
        Platform.runLater(() -> {
            processListVBox.getChildren().clear();
            if (initialProcessData.isEmpty()) {
                processListVBox.getChildren().add(new Label("无进程"));
            } else {
                int tempId = 1;
                for (int[] data : initialProcessData) {
                    processListVBox.getChildren().add(new Label("P" + tempId++ + " (到达:" + data[0] + ", 突发:" + data[1] + ")"));
                }
            }
        });
    }

    @FXML
    protected void handleModeToggle() {
        if (simulationActive) {
            showAlert("错误", "模拟正在进行中，无法切换模式。请先重置模拟。");
            return;
        }

        if (currentMode == SimulationMode.AUTO) {
            currentMode = SimulationMode.MANUAL;
            modeToggleButton.setText("切换到自动模式");
            appendToLog("模式切换到手动模式。");
        } else {
            currentMode = SimulationMode.AUTO;
            modeToggleButton.setText("切换到手动模式");
            appendToLog("模式切换到自动模式。");
        }
        simulationActive = false;
        resetSimulationState();
    }

    @FXML
    protected void handlePlayPause() {
        if (initialProcessData.isEmpty()) {
            showAlert("错误", "请先添加进程！");
            return;
        }

        if (currentMode == SimulationMode.AUTO) {
            if (!simulationActive || scheduler.isSimulationFinished()) {
                simulationActive = true;
                resetSimulationState();
                openGanttChartWindow();
                createAndStartAnimationTimer();
                playPauseButton.setText("暂停模拟");
                autoModePaused = false;
                appendToLog("自动模式模拟开始。");
            } else {
                if (autoModePaused) {
                    if (simulationTimer != null) {
                        simulationTimer.start();
                    } else {
                        createAndStartAnimationTimer();
                    }
                    playPauseButton.setText("暂停模拟");
                    autoModePaused = false;
                    appendToLog("自动模式模拟继续。");
                } else {
                    if (simulationTimer != null) {
                        simulationTimer.stop();
                    }
                    playPauseButton.setText("继续模拟");
                    autoModePaused = true;
                    appendToLog("自动模式模拟暂停。");
                }
            }
            updateControlsVisibility();
        }
    }

    private void createAndStartAnimationTimer() {
        simulationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long NANO_SECONDS_IN_MILLI = 1_000_000L;
            private final long SIMULATION_STEP_MS = 100;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= SIMULATION_STEP_MS * NANO_SECONDS_IN_MILLI) {
                    lastUpdate = now;
                    Platform.runLater(() -> {
                        if (!autoModePaused) {
                            if (!simulateOneTimeUnitAndLog(true)) {
                                stop();
                                simulationActive = false;
                                playPauseButton.setText("模拟完成");
                                calculateAndDisplayStatistics();
                                showAlert("模拟完成", "所有进程已执行完毕！");
                                dataManager.addCurrentSimulationToHistory(initialProcessData, scheduler.getCompletedProcesses(), scheduler.getCurrentTime(), scheduler.getCpuBusyTime());
                                updateControlsVisibility();
                                appendToLog("模拟完成。所有进程已执行完毕。");
                            }
                            if (ganttChartController != null) {
                                ganttChartController.setData(scheduler.getConsolidatedExecutionLog(), scheduler.getCurrentSimulationProcesses());
                            }
                        }
                    });
                }
            }
        };
        simulationTimer.start();
    }

    @FXML
    protected void handleStep() {
        if (initialProcessData.isEmpty()) {
            showAlert("错误", "请先添加进程！");
            return;
        }
        if (currentMode == SimulationMode.MANUAL) {
            if (!simulationActive) {
                simulationActive = true;
                resetSimulationState();
                appendToLog("手动模式模拟开始。");
            }

            if (scheduler.isSimulationFinished()) {
                showAlert("提示", "模拟已完成。请重置以重新开始。");
                appendToLog("手动模式模拟已完成。");
                return;
            }
            simulateOneTimeUnitAndLog(false);
            updateUI();
        }
    }

    @FXML
    protected void handleResetSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        initialProcessData.clear();
        simulationActive = false;
        autoModePaused = false;
        resetSimulationState();
        updateProcessListUI();
        appendToLog("模拟已完全重置。");
    }

    @FXML
    protected void handleTimeSliderChange() {
        if (currentMode == SimulationMode.MANUAL) {
            int targetTime = (int) timeSlider.getValue();
            if (targetTime != scheduler.getCurrentTime()) {
                if (simulationTimer != null) simulationTimer.stop();

                simulationActive = true;

                resetSimulationState();
                simulateToTime(targetTime);
                appendToLog("手动模式时间滑块调整至 T=" + targetTime + "。");
            }
        }
    }

    private void handleHistoryClick() {
        HistoryEntry selectedEntry = historyListView.getSelectionModel().getSelectedItem();
        if (selectedEntry != null) {
            if (simulationActive) {
                showAlert("提示", "模拟正在进行中，无法查看历史详情。请先重置模拟。");
                return;
            }

            modalSimulationNameLabel.setText(selectedEntry.getName());

            StringBuilder processDetails = new StringBuilder("进程数据:\n");
            int tempId = 1;
            for(int[] p : selectedEntry.getProcessData()) {
                processDetails.append("  P").append(tempId++).append(" (到达:").append(p[0]).append("ms, 突发:").append(p[1]).append("ms)\n");
            }
            modalProcessDataLabel.setText(processDetails.toString().trim());

            modalAvgWaitingTimeLabel.setText(String.format("平均等待时间: %.2f ms", selectedEntry.getAvgWaitingTime()));
            modalThroughputLabel.setText(String.format("系统吞吐量: %.4f 进程/ms", selectedEntry.getThroughput()));
            if (modalAvgResponseTimeLabel != null) modalAvgResponseTimeLabel.setText(String.format("平均响应时间: %.2f ms", selectedEntry.getAvgResponseTime()));
            if (modalCpuUtilizationLabel != null) modalCpuUtilizationLabel.setText(String.format("CPU 利用率: %.2f %%", selectedEntry.getCpuUtilization() * 100));


            historyDetailModal.setVisible(true);
            modalBackground.setVisible(true);
            mainContentHBox.setDisable(true);
            appendToLog("显示历史记录详情：" + selectedEntry.getName());
        }
    }

    @FXML
    protected void handleApplySimulationFromHistory() {
        HistoryEntry selectedEntry = historyListView.getSelectionModel().getSelectedItem();
        if (selectedEntry != null) {
            initialProcessData.clear();
            initialProcessData.addAll(selectedEntry.getProcessData());

            arrivalTimeField.setText(selectedEntry.getProcessData().stream()
                    .map(p -> String.valueOf(p[0]))
                    .collect(Collectors.joining(",")));
            burstTimeField.setText(selectedEntry.getProcessData().stream()
                    .map(p -> String.valueOf(p[1]))
                    .collect(Collectors.joining(",")));

            simulationActive = false;
            resetSimulationState();
            updateProcessListUI();
            showAlert("历史记录加载成功", "已加载模拟：" + selectedEntry.getName());
            appendToLog("已从历史记录加载进程数据并重置模拟：" + selectedEntry.getName());
        }
        handleCloseHistoryModal();
    }

    @FXML
    protected void handleCloseHistoryModal() {
        historyDetailModal.setVisible(false);
        modalBackground.setVisible(false);
        mainContentHBox.setDisable(false);
        appendToLog("关闭历史记录详情模态框。");
    }

    @FXML
    protected void handleClearHistory() {
        dataManager.clearHistory();
        showAlert("历史记录", "历史记录已清空。");
        appendToLog("历史记录已清空。");
    }

    private void updateControlsVisibility() {
        boolean isManualMode = (currentMode == SimulationMode.MANUAL);
        boolean hasProcesses = !initialProcessData.isEmpty();
        boolean simulationDone = scheduler.isSimulationFinished();

        playPauseButton.setVisible(!isManualMode);
        playPauseButton.setDisable(!hasProcesses || simulationDone);
        if (currentMode == SimulationMode.AUTO) {
            if (!simulationActive || simulationDone) {
                playPauseButton.setText("开始模拟");
            } else if (autoModePaused) {
                playPauseButton.setText("继续模拟");
            } else {
                playPauseButton.setText("暂停模拟");
            }
        }

        stepButton.setVisible(isManualMode);
        stepButton.setDisable(!hasProcesses || simulationDone);

        timeSliderContainer.setVisible(isManualMode);
        timeSlider.setDisable(!hasProcesses);

        if (hasProcesses) {
            int totalBurst = initialProcessData.stream().mapToInt(data -> data[1]).sum();
            int maxArrival = initialProcessData.stream().mapToInt(data -> data[0]).max().orElse(0);
            timeSlider.setMax(Math.max(scheduler.getCurrentTime() + 10, maxArrival + totalBurst * 2 + 50));
            timeSlider.setValue(scheduler.getCurrentTime());
        } else {
            timeSlider.setMax(100);
            timeSlider.setValue(0);
        }
    }


    private boolean simulateOneTimeUnitAndLog(boolean isAnimatedStep) {
        int previousTime = scheduler.getCurrentTime();
        Process previousExecutingProcess = scheduler.getCurrentlyExecutingProcess();
        int previousRemainingBurst = previousExecutingProcess != null ? previousExecutingProcess.getRemainingBurstTime() : -1;
        int previousQueueLevel = previousExecutingProcess != null ? previousExecutingProcess.getCurrentQueueLevel() : -1;

        boolean simulationContinues = scheduler.simulateOneTimeUnit();

        int currentTime = scheduler.getCurrentTime();
        Process currentlyExecutingProcess = scheduler.getCurrentlyExecutingProcess();

        for (Process p : scheduler.getCurrentSimulationProcesses()) {
            if (p.getArrivalTime() == currentTime - 1 && p.getExecutionStartTime() == -1 && p.getRemainingBurstTime() == p.getBurstTime()) {
                if (scheduler.getQueue1().contains(p) || (currentlyExecutingProcess != null && currentlyExecutingProcess.equals(p))) {
                    appendToLog("T=" + (currentTime - 1) + ": 进程 P" + p.getProcessId() + " 到达，进入 Q1。");
                }
            }
        }


        if (previousExecutingProcess != null && previousExecutingProcess.getRemainingBurstTime() == 0 && scheduler.getCompletedProcesses().contains(previousExecutingProcess)) {
            appendToLog("T=" + (currentTime -1) + ": 进程 P" + previousExecutingProcess.getProcessId() + " 完成执行。周转时间: " + previousExecutingProcess.getTurnaroundTime() + "ms, 等待时间: " + previousExecutingProcess.getWaitingTime() + "ms.");
        }

        if (currentlyExecutingProcess != null) {
            if (currentlyExecutingProcess.getExecutionStartTime() == currentTime - 1) {
                appendToLog("T=" + (currentTime - 1) + ": 进程 P" + currentlyExecutingProcess.getProcessId() + " 首次开始执行.");
            }
            else if (previousExecutingProcess != null && !currentlyExecutingProcess.equals(previousExecutingProcess)) {
                appendToLog("T=" + (currentTime - 1) + ": 进程 P" + previousExecutingProcess.getProcessId() + " 被抢占/时间片用尽，进程 P" + currentlyExecutingProcess.getProcessId() + " 开始执行 (剩余突发时间: " + currentlyExecutingProcess.getRemainingBurstTime() + ").");
            }
        } else if (previousExecutingProcess != null && currentlyExecutingProcess == null && !scheduler.isSimulationFinished()) {
            if (!isAnimatedStep) {
                appendToLog("T=" + (currentTime - 1) + ": CPU 空闲，等待新进程或队列中有进程。");
            }
        }


        if (previousExecutingProcess != null && previousExecutingProcess.getRemainingBurstTime() > 0 && currentlyExecutingProcess != previousExecutingProcess) {
            int oldLevel = previousExecutingProcess.getCurrentQueueLevel();
            if (oldLevel == 1 && scheduler.getQueue2().contains(previousExecutingProcess)) {
                appendToLog("T=" + (currentTime -1) + ": 进程 P" + previousExecutingProcess.getProcessId() + " 时间片用尽，降级到 Q2。");
            } else if (oldLevel == 2 && scheduler.getQueue3().contains(previousExecutingProcess)) {
                appendToLog("T=" + (currentTime -1) + ": 进程 P" + previousExecutingProcess.getProcessId() + " 时间片用尽，降级到 Q3。");
            }
        }

        if (currentlyExecutingProcess == null && !scheduler.isSimulationFinished() && scheduler.getCurrentSimulationProcesses().size() > scheduler.getCompletedProcesses().size() && scheduler.getQueue1().isEmpty() && scheduler.getQueue2().isEmpty() && scheduler.getQueue3().isEmpty()) {
            appendToLog("T=" + (currentTime -1) + ": CPU 空闲，等待新进程到达。");
        }


        updateUI();

        return simulationContinues;
    }


    private void simulateToTime(int targetTime) {
        if (targetTime < 0) targetTime = 0;

        while (scheduler.getCurrentTime() < targetTime) {
            if (!simulateOneTimeUnitAndLog(false)) {
                appendToLog("模拟在 T=" + scheduler.getCurrentTime() + " 提前完成 (目标时间: " + targetTime + ")。");
                break;
            }
        }
        updateUI();
    }

    private boolean isSimulationFinished() {
        return scheduler.isSimulationFinished();
    }


    private void updateUI() {
        Platform.runLater(() -> {
            currentTimeLabel.setText(String.valueOf(scheduler.getCurrentTime()));
            currentProcessLabel.setText(scheduler.getCurrentlyExecutingProcess() != null ? "P" + scheduler.getCurrentlyExecutingProcess().getProcessId() + " (剩余:" + scheduler.getCurrentlyExecutingProcess().getRemainingBurstTime() + ")" : "无");

            queue1StatusLabel.setText(formatQueueStatus(scheduler.getQueue1()));
            queue2StatusLabel.setText(formatQueueStatus(scheduler.getQueue2()));
            queue3StatusLabel.setText(formatQueueStatus(scheduler.getQueue3()));

            completedProcessesLabel.setText(formatCompletedProcesses(scheduler.getCompletedProcesses()));

            if (scheduler.isSimulationFinished()) {
                calculateAndDisplayStatistics();
            } else {
                avgWaitingTimeLabel.setText("平均等待时间: 0.0 ms");
                throughputLabel.setText("系统吞吐量: 0.0 进程/ms");
                if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText("平均响应时间: 0.0 ms");
                if (cpuUtilizationLabel != null) cpuUtilizationLabel.setText("CPU 利用率: 0.00 %");
            }

            if (currentMode == SimulationMode.MANUAL) {
                timeSlider.setValue(scheduler.getCurrentTime());
            }
            updateControlsVisibility();
        });
    }

    private String formatQueueStatus(Queue<Process> queue) {
        if (queue.isEmpty()) {
            return "空";
        }
        return queue.stream()
                .map(p -> "P" + p.getProcessId() + "(剩余:" + p.getRemainingBurstTime() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatCompletedProcesses(List<Process> completedProcesses) {
        if (completedProcesses.isEmpty()) {
            return "无";
        }
        return completedProcesses.stream()
                .map(p -> "P" + p.getProcessId())
                .collect(Collectors.joining(", "));
    }

    private void calculateAndDisplayStatistics() {
        if (initialProcessData.isEmpty() || scheduler.getCompletedProcesses().size() != scheduler.getCurrentSimulationProcesses().size()) {
            avgWaitingTimeLabel.setText("平均等待时间: 计算中...");
            throughputLabel.setText("系统吞吐量: 计算中...");
            if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText("平均响应时间: 计算中...");
            if (cpuUtilizationLabel != null) cpuUtilizationLabel.setText("CPU 利用率: 计算中...");
            return;
        }

        double totalWaitingTime = scheduler.getCompletedProcesses().stream()
                .mapToDouble(Process::getWaitingTime)
                .sum();
        double avgWaitingTime = totalWaitingTime / scheduler.getCompletedProcesses().size();

        double throughput = 0.0;
        int maxCompletionTime = scheduler.getCompletedProcesses().stream()
                .mapToInt(Process::getCompletionTime)
                .max()
                .orElse(0);

        int minArrivalTime = initialProcessData.stream()
                .mapToInt(data -> data[0])
                .min()
                .orElse(0);

        if (maxCompletionTime - minArrivalTime > 0) {
            throughput = (double) scheduler.getCompletedProcesses().size() / (maxCompletionTime - minArrivalTime);
        } else if (scheduler.getCompletedProcesses().size() > 0) {
            throughput = (double) scheduler.getCompletedProcesses().size();
        }

        double totalResponseTime = scheduler.getCompletedProcesses().stream()
                .mapToInt(p -> p.getExecutionStartTime() - p.getArrivalTime())
                .sum();
        double avgResponseTime = totalResponseTime / scheduler.getCompletedProcesses().size();

        double cpuUtilization = 0.0;
        if (scheduler.getCurrentTime() > 0) {
            cpuUtilization = (double) scheduler.getCpuBusyTime() / scheduler.getCurrentTime();
        }

        avgWaitingTimeLabel.setText(String.format("平均等待时间: %.2f ms", avgWaitingTime));
        throughputLabel.setText(String.format("系统吞吐量: %.4f 进程/ms", throughput));
        if (avgResponseTimeLabel != null) avgResponseTimeLabel.setText(String.format("平均响应时间: %.2f ms", avgResponseTime));
        if (cpuUtilizationLabel != null) cpuUtilizationLabel.setText(String.format("CPU 利用率: %.2f %%", cpuUtilization * 100));
    }


    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void appendToLog(String message) {
        Platform.runLater(() -> {
            simulationLogTextArea.appendText(message + "\n");
            simulationLogTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void openGanttChartWindow() {
        try {
            if (ganttChartStage == null || !ganttChartStage.isShowing()) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("gantt-chart-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                ganttChartController = fxmlLoader.getController();

                ganttChartStage = new Stage();
                ganttChartStage.setTitle("CPU 调度甘特图");
                ganttChartStage.setScene(scene);
                ganttChartStage.initModality(Modality.NONE);
                ganttChartStage.show();

                ganttChartStage.setOnCloseRequest(event -> {
                    ganttChartStage = null;
                    ganttChartController = null;
                });
            } else {
                ganttChartStage.toFront();
            }
            if (ganttChartController != null) {
                ganttChartController.setData(scheduler.getConsolidatedExecutionLog(), scheduler.getCurrentSimulationProcesses());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法打开甘特图窗口: " + e.getMessage());
        }
    }
}
