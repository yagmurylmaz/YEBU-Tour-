package com.hotel.ui;

import com.hotel.MainApp;
import com.hotel.model.ExtraServiceDefinition;
import com.hotel.service.ExtraServiceCatalogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminServiceController {
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> billingTypeCombo;
    @FXML private CheckBox activeCheck;
    @FXML private Label statusLabel;

    @FXML private TableView<ExtraServiceDefinition> serviceTable;
    @FXML private TableColumn<ExtraServiceDefinition, String> colCode;
    @FXML private TableColumn<ExtraServiceDefinition, String> colName;
    @FXML private TableColumn<ExtraServiceDefinition, String> colPrice;
    @FXML private TableColumn<ExtraServiceDefinition, String> colBilling;
    @FXML private TableColumn<ExtraServiceDefinition, String> colActive;

    private final ExtraServiceCatalogService service = new ExtraServiceCatalogService();
    private ExtraServiceDefinition selected;

    @FXML
    private void initialize() {
        if (!SessionManager.getInstance().ensureAdminAccess()) return;
        billingTypeCombo.setItems(FXCollections.observableArrayList("PER_NIGHT", "PER_STAY"));
        billingTypeCombo.setValue("PER_NIGHT");
        activeCheck.setSelected(true);
        setupTable();
        loadServices();
    }

    private void setupTable() {
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCode()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(String.format("₺%.2f", d.getValue().getPrice())));
        colBilling.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBillingType()));
        colActive.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Inactive"));
        serviceTable.getSelectionModel().selectedItemProperty().addListener((obs, old, s) -> {
            if (s != null) populateForm(s);
        });
    }

    private void loadServices() {
        var all = service.getAllServices();
        serviceTable.setItems(FXCollections.observableArrayList(all));
        statusLabel.setText("Listed " + all.size() + " service(s).");
    }

    private void populateForm(ExtraServiceDefinition s) {
        selected = s;
        codeField.setText(s.getCode());
        nameField.setText(s.getName());
        priceField.setText(String.valueOf(s.getPrice()));
        descriptionField.setText(s.getDescription());
        billingTypeCombo.setValue(s.getBillingType());
        activeCheck.setSelected(s.isActive());
    }

    @FXML
    private void handleAdd() {
        try {
            int id = service.addService(buildForm());
            showInfo("Service added. (ID: " + id + ")");
            clearForm();
            loadServices();
        } catch (Exception e) {
            showWarn(e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selected == null) { showWarn("Select a service first."); return; }
        try {
            ExtraServiceDefinition d = buildForm();
            d.setId(selected.getId());
            if (service.updateService(d)) {
                showInfo("Service updated.");
                clearForm();
                loadServices();
            }
        } catch (Exception e) {
            showWarn(e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selected == null) { showWarn("Select a service first."); return; }
        if (service.deleteService(selected.getId())) {
            showInfo("Service deleted.");
            clearForm();
            loadServices();
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleBack() {
        MainApp.navigateTo("admin-dashboard.fxml");
    }

    private ExtraServiceDefinition buildForm() {
        ExtraServiceDefinition d = new ExtraServiceDefinition();
        d.setCode(codeField.getText() == null ? "" : codeField.getText().trim().toUpperCase());
        d.setName(nameField.getText() == null ? "" : nameField.getText().trim());
        d.setPrice(Double.parseDouble(priceField.getText().trim()));
        d.setDescription(descriptionField.getText() == null ? "" : descriptionField.getText().trim());
        d.setBillingType(billingTypeCombo.getValue());
        d.setActive(activeCheck.isSelected());
        return d;
    }

    private void clearForm() {
        selected = null;
        serviceTable.getSelectionModel().clearSelection();
        codeField.clear();
        nameField.clear();
        priceField.clear();
        descriptionField.clear();
        billingTypeCombo.setValue("PER_NIGHT");
        activeCheck.setSelected(true);
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }
}

