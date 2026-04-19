package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.service.ReportService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.Map;

public class AdminReportController {

    @FXML private Label   labelTotal;
    @FXML private Label   labelPending;
    @FXML private Label   labelApproved;
    @FXML private Label   labelCancelled;
    @FXML private Label   labelRevenue;
    @FXML private TextArea distributionArea;

    private final ReportService reportService = new ReportService();

    @FXML
    private void initialize() {
        loadReport();
    }

    private void loadReport() {
        Map<String, Object> summary = reportService.getAdminSummary();

        labelTotal.setText(summary.get("totalReservations").toString());
        labelPending.setText(summary.get("pending").toString());
        labelApproved.setText(summary.get("approved").toString());
        labelCancelled.setText(summary.get("cancelled").toString());
        labelRevenue.setText(String.format("₺%.2f", summary.get("totalRevenue")));
        Map<String, Long> dist = reportService.getReservationsByRoomType();
        StringBuilder sb = new StringBuilder("Reservation Distribution by Room Type:\n");
        sb.append("─".repeat(40)).append("\n");
        dist.forEach((type, count) ->
            sb.append(String.format("  %-15s : %d reservations%n", type, count))
        );
        distributionArea.setText(sb.toString());
    }

    @FXML private void handleRefresh() { loadReport(); }
    @FXML private void handleBack()    { MainApp.navigateTo("admin-dashboard.fxml"); }
    @FXML private void handleLogout()  {
        SessionManager.getInstance().logout();
        MainApp.navigateTo("login.fxml");
    }
}
