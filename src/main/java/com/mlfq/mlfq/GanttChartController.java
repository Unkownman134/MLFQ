package com.mlfq.mlfq;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GanttChartController {

    @FXML
    private Pane ganttDrawingPane;

    private List<ExecutionSegment> executionLog;
    private List<Process> allProcesses;
    private Map<Integer, Color> processColors;

    private final double Y_AXIS_WIDTH = 60;
    private final double X_AXIS_HEIGHT = 30;
    private final double BAR_HEIGHT = 20;
    private final double BAR_GAP = 10;
    private double timeScale = 5.0;

    @FXML
    public void initialize() {
        processColors = new HashMap<>();
    }

    public void setData(List<ExecutionSegment> log, List<Process> processes) {
        this.executionLog = log;
        this.allProcesses = processes;
        generateProcessColors();
        drawChart();
    }

    public void drawChart() {
        ganttDrawingPane.getChildren().clear();

        if (executionLog == null || executionLog.isEmpty() || allProcesses == null || allProcesses.isEmpty()) {
            Label noDataLabel = new Label("无CPU活动数据");
            noDataLabel.setFont(new Font(16));
            ganttDrawingPane.getChildren().add(noDataLabel);
            return;
        }

        int maxTime = executionLog.stream()
                .mapToInt(ExecutionSegment::getEndTime)
                .max()
                .orElse(0);

        double desiredWidth = ganttDrawingPane.getWidth() - Y_AXIS_WIDTH - 20;
        if (maxTime > 0) {
            timeScale = desiredWidth / maxTime;
            if (timeScale < 1.0) timeScale = 1.0;
            if (timeScale > 10.0) timeScale = 10.0;
        } else {
            timeScale = 5.0;
        }

        double totalHeightNeeded = allProcesses.size() * (BAR_HEIGHT + BAR_GAP) + X_AXIS_HEIGHT + 20;
        ganttDrawingPane.setPrefHeight(Math.max(ganttDrawingPane.getMinHeight(), totalHeightNeeded));

        Map<Integer, Double> processYPositions = new HashMap<>();
        double currentY = X_AXIS_HEIGHT + BAR_GAP;

        for (Process p : allProcesses) {
            Label processLabel = new Label("P" + p.getProcessId());
            processLabel.setLayoutX(5);
            processLabel.setLayoutY(currentY + (BAR_HEIGHT / 2) - (processLabel.prefHeight(0) / 2));
            processLabel.setFont(new Font(12));
            ganttDrawingPane.getChildren().add(processLabel);
            processYPositions.put(p.getProcessId(), currentY);
            currentY += (BAR_HEIGHT + BAR_GAP);
        }

        double maxDrawingWidth = maxTime * timeScale;
        double currentX = Y_AXIS_WIDTH;
        int tickInterval = (int) Math.max(1, maxTime / 10.0);

        for (int t = 0; t <= maxTime + tickInterval; t += tickInterval) {
            double x = Y_AXIS_WIDTH + t * timeScale;
            if (x > ganttDrawingPane.getWidth()) break;

            Line tickLine = new Line(x, X_AXIS_HEIGHT, x, X_AXIS_HEIGHT + 5);
            ganttDrawingPane.getChildren().add(tickLine);

            Text timeText = new Text(String.valueOf(t));
            timeText.setLayoutX(x - timeText.getLayoutBounds().getWidth() / 2);
            timeText.setLayoutY(X_AXIS_HEIGHT - 5);
            ganttDrawingPane.getChildren().add(timeText);
        }

        Line xAxisLine = new Line(Y_AXIS_WIDTH, X_AXIS_HEIGHT, Y_AXIS_WIDTH + maxDrawingWidth + 20, X_AXIS_HEIGHT);
        ganttDrawingPane.getChildren().add(xAxisLine);


        for (ExecutionSegment segment : executionLog) {
            double startX = Y_AXIS_WIDTH + segment.getStartTime() * timeScale;
            double endX = Y_AXIS_WIDTH + segment.getEndTime() * timeScale;
            double width = endX - startX;

            if (width <= 0) continue;

            Double yPos = processYPositions.get(segment.getProcessId());
            if (yPos == null) {
                if (segment.getProcessId() == -1) {
                    Rectangle idleRect = new Rectangle(startX, X_AXIS_HEIGHT + 5, width, ganttDrawingPane.getPrefHeight() - X_AXIS_HEIGHT - 5);
                    idleRect.setFill(Color.LIGHTGRAY.deriveColor(1, 1, 1, 0.3));
                    ganttDrawingPane.getChildren().add(idleRect);
                }
                continue;
            }

            Rectangle rect = new Rectangle(startX, yPos, width, BAR_HEIGHT);
            rect.setFill(processColors.getOrDefault(segment.getProcessId(), Color.GRAY));
            rect.setStroke(Color.BLACK);
            rect.setArcWidth(5);
            rect.setArcHeight(5);

            ganttDrawingPane.getChildren().add(rect);

            if (width > 20) {
                Text segmentText = new Text("P" + segment.getProcessId());
                segmentText.setFont(new Font(10));
                segmentText.setLayoutX(startX + (width / 2) - (segmentText.getLayoutBounds().getWidth() / 2));
                segmentText.setLayoutY(yPos + (BAR_HEIGHT / 2) + (segmentText.getLayoutBounds().getHeight() / 4));
                ganttDrawingPane.getChildren().add(segmentText);
            }
        }

        ganttDrawingPane.setPrefWidth(Y_AXIS_WIDTH + maxDrawingWidth + 50);

    }

    private void generateProcessColors() {
        if (allProcesses == null || allProcesses.isEmpty()) return;

        List<Color> colors = new ArrayList<>();
        colors.add(Color.web("#FF5733"));
        colors.add(Color.web("#33FF57"));
        colors.add(Color.web("#3357FF"));
        colors.add(Color.web("#FF33F5"));
        colors.add(Color.web("#F5FF33"));
        colors.add(Color.web("#33F5FF"));
        colors.add(Color.web("#FF9933"));
        colors.add(Color.web("#99FF33"));
        colors.add(Color.web("#3399FF"));
        colors.add(Color.web("#FF3399"));

        for (int i = 0; i < allProcesses.size(); i++) {
            int processId = allProcesses.get(i).getProcessId();
            processColors.put(processId, colors.get(i % colors.size()));
        }
    }
}
