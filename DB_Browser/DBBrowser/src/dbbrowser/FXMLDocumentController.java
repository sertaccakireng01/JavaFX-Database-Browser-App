package dbbrowser;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;    
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.scene.control.TableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

//@author Sertac & Berk
public class FXMLDocumentController implements Initializable {

    @FXML private Button btnConnected;
    @FXML private ListView<String> listTables;
    @FXML private TabPane mainTabPane;
    @FXML private TableView tableMain;
    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private HBox inputArea;
    @FXML private Button btnDoIt;
    @FXML private Button btnCancel; 
    @FXML private TextArea txtQuery;
    @FXML private Button btnExecute;
    @FXML private TableView tableQuery;
    @FXML private Label lblStatus;

    private ObservableList<ObservableList> data;
    private Connection conn;
    private String currentTableName = "";
    private List<TextField> inputFields = new ArrayList<>();
    private List<String> columnNames = new ArrayList<>();

    private String originalPK = null;
    private String originalPK2 = null;

    // Modes 
    private enum OperationMode {
        NONE, ADD, UPDATE
    }
    private OperationMode currentMode = OperationMode.NONE;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            mainTabPane.setVisible(false);
            listTables.setVisible(false);
            btnConnected.setText("Click to Connect");
            btnConnected.setStyle("-fx-base: #95a5a6;");
            lblStatus.setText("Status: Waiting for user to connect...");

            inputArea.setVisible(false);
            btnDoIt.setVisible(false);
            if (btnCancel != null) btnCancel.setVisible(false);

            // Listener: Table selection
            listTables.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (newVal != null) {
                        currentTableName = newVal;
                        loadTableData(newVal);
                        lblStatus.setText("Selected Table: " + newVal);
                        resetMode();
                    }
                } catch (Exception e) {
                    AlertHelper.showWarningMessage("Selection Error", "Error selecting table: " + e.getMessage());
                }
            });

            // Listener: Row selection (Dynamic PK Detection)
            tableMain.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                try {
                    if (newSelection != null) {
                        ObservableList<String> row = (ObservableList<String>) newSelection;

                        if (!row.isEmpty()) {
                            List<String> pkNames = getPrimaryKeyColumnsMeta(currentTableName);
                            
                            if (!pkNames.isEmpty()) {
                                // 1. First Primary Key
                                int idx1 = columnNames.indexOf(pkNames.get(0));
                                if (idx1 != -1) originalPK = row.get(idx1);

                                // 2. Second Primary Key (Composite Key for Enrollment)
                                if (pkNames.size() > 1) {
                                    int idx2 = columnNames.indexOf(pkNames.get(1));
                                    if (idx2 != -1) originalPK2 = row.get(idx2);
                                }
                            } else {
                                // Fallback: default to first column if no PK found
                                originalPK = row.get(0);
                            }
                        }

                        if (currentMode == OperationMode.UPDATE) {
                            populateInputFields(row);
                        }
                    }
                } catch (Exception e) {
                    // Log internally, do not disrupt UI for selection errors
                    System.out.println("Selection logic error: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            AlertHelper.showWarningMessage("Initialization Error", "Failed to initialize app: " + e.getMessage());
        }
    }

    // Resets the UI state and clears selections
    private void resetMode() {
        try {
            currentMode = OperationMode.NONE;
            inputArea.setVisible(false);
            btnDoIt.setVisible(false);
            if (btnCancel != null) btnCancel.setVisible(false);
            originalPK = null;
            originalPK2 = null;
            
            tableMain.getSelectionModel().clearSelection();
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Failed to reset mode: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            resetMode();
            lblStatus.setText("Status: Operation cancelled.");
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Error cancelling operation: " + e.getMessage());
        }
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        try {
            if (conn != null) {
                return;
            }
            connectToDatabase();
            if (conn != null) {
                loadTableList();
                listTables.setVisible(true);
                mainTabPane.setVisible(true);
                btnConnected.setText("Database Connected");
                btnConnected.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
            }
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Connection Error", "Failed to initiate connection: " + e.getMessage());
        }
    }

    private void connectToDatabase() {
        try {
            conn = DatabaseConnection.getConnection();
            if (conn != null) {
                lblStatus.setText("Status: Database Connected Successfully.");
            } else {
                lblStatus.setText("Status: Connection Failed. Check XAMPP.");
                AlertHelper.showWarningMessage("Connection Failed", "Could not connect to the database. Please check if MySQL is running.");
            }
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Critical Error", "Database connection failed: " + e.getMessage());
        }
    }

    private boolean showConfirmation(String title, String content) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Failed to show confirmation: " + e.getMessage());
            return false;
        }
    }

    // Validates input fields (checks if PK is empty)
    private boolean validateInputs() {
        try {
            if (inputFields.isEmpty()) return false;
            
            if (inputFields.get(0).getText().trim().isEmpty()) {
                AlertHelper.showWarningMessage("Missing Input", "The Primary Key (" + columnNames.get(0) + ") cannot be empty.");
                return false;
            }
            return true;
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Validation Error", "Error validating inputs: " + e.getMessage());
            return false;
        }
    }

    private void loadTableList() {
        try {
            if (conn == null) return;
            DatabaseMetaData dbmd = conn.getMetaData();
            String catalog = conn.getCatalog();
            ResultSet rs = dbmd.getTables(catalog, null, "%", new String[]{"TABLE"});
            ObservableList<String> tables = FXCollections.observableArrayList();
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (!tableName.startsWith("pma__") && !tableName.startsWith("sys_")) {
                    tables.add(tableName);
                }
            }
            listTables.setItems(tables);
        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, null); // Table unknown
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Error", "Failed to load table list: " + e.getMessage());
        }
    }

    private void loadTableData(String tableName) {
        try {
            tableMain.getColumns().clear();
            data = FXCollections.observableArrayList();
            inputArea.getChildren().clear();
            inputFields.clear();
            columnNames.clear();

            String sql = "SELECT * FROM " + tableName;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Create columns dynamically
            for (int i = 0; i < columnCount; i++) {
                final int j = i;
                String colName = metaData.getColumnName(i + 1);
                columnNames.add(colName);

                TableColumn<ObservableList, String> col = new TableColumn<>(colName);

                // 1 Data binding
                col.setCellValueFactory(param -> {
                    Object value = param.getValue().get(j);
                    return new SimpleStringProperty(value != null ? value.toString() : "");
                });

                // 2 Right-click Context Menu
                col.setCellFactory(column -> new TableCell<ObservableList, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setContextMenu(null);
                        } else {
                            setText(item);

                            ContextMenu cellMenu = new ContextMenu();
                            MenuItem copyItem = new MenuItem("Copy"); 

                            copyItem.setOnAction(event -> {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(item);
                                Clipboard.getSystemClipboard().setContent(content);
                                lblStatus.setText("Copied: " + item);
                            });

                            cellMenu.getItems().add(copyItem);
                            setContextMenu(cellMenu);
                        }
                    }
                });

                tableMain.getColumns().add(col);

                // dynamic input fields
                VBox fieldContainer = new VBox(2);
                Label lbl = new Label(colName);
                lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                TextField tf = new TextField();
                tf.setPromptText(colName);
                tf.setPrefWidth(100);
                inputFields.add(tf);
                fieldContainer.getChildren().addAll(lbl, tf);
                inputArea.getChildren().add(fieldContainer);
            }

            // Fill data
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }
            tableMain.setItems(data);

        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, tableName);
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Data Load Error", "Failed to load table data: " + e.getMessage());
        }
    }

    // Fills input fields with data from the selected row
    private void populateInputFields(ObservableList<String> rowData) {
        try {
            for (int i = 0; i < inputFields.size(); i++) {
                if (i < rowData.size()) {
                    inputFields.get(i).setText(rowData.get(i));
                }
            }
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Failed to populate fields: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        try {
            if (currentTableName.isEmpty()) {
                AlertHelper.showWarningMessage("Warning", "Please select a table first!");
                return;
            }
            currentMode = OperationMode.ADD;
            for (TextField tf : inputFields) {
                tf.clear();
            }

            inputArea.setVisible(true);
            btnDoIt.setVisible(true);
            btnDoIt.setText("Confirm Add");
            if (btnCancel != null) btnCancel.setVisible(true); 
            lblStatus.setText("Mode: Adding New Entry.");
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Failed to switch to Add mode: " + e.getMessage());
        }
    }

   
    @FXML
    private void handleUpdate(ActionEvent event) {
        try {
            if (currentTableName.isEmpty()) {
                AlertHelper.showWarningMessage("Warning", "Please select a table first!");
                return;
            }
            if (tableMain.getSelectionModel().getSelectedItem() == null) {
                AlertHelper.showWarningMessage("Warning", "Please select a row to update!");
                return;
            }

            currentMode = OperationMode.UPDATE;
            populateInputFields((ObservableList<String>) tableMain.getSelectionModel().getSelectedItem());

            inputArea.setVisible(true);
            btnDoIt.setVisible(true);
            btnDoIt.setText("Confirm Update");
            if (btnCancel != null) btnCancel.setVisible(true); 
            lblStatus.setText("Mode: Updating Entry.");
        } catch (Exception e) {
            AlertHelper.showWarningMessage("UI Error", "Failed to switch to Update mode: " + e.getMessage());
        }
    }

    private List<String> getPrimaryKeyColumnsMeta(String tableName) throws SQLException {
        List<String> pkCols = new ArrayList<>();
        try {
            DatabaseMetaData md = conn.getMetaData();
            String catalog = conn.getCatalog();

            try (ResultSet rs = md.getPrimaryKeys(catalog, null, tableName)) {
                class Pk {
                    int seq;
                    String col;
                    Pk(int s, String c) { seq = s; col = c; }
                }
                List<Pk> tmp = new ArrayList<>();

                while (rs.next()) {
                    tmp.add(new Pk(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME")));
                }

                tmp.sort((a, b) -> Integer.compare(a.seq, b.seq));
                for (Pk p : tmp) {
                    pkCols.add(p.col);
                }
            }
        } catch (SQLException e) {
            throw e; // Pass to caller to handle
        }
        return pkCols;
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        try {
            if (tableMain.getSelectionModel().getSelectedItem() == null) {
                AlertHelper.showWarningMessage("Warning", "Please select a row to delete!");
                return;
            }

            boolean confirmed = showConfirmation("Delete Confirmation", "Are you sure you want to DELETE this record?\nThis action cannot be undone.");
            if (!confirmed) return;

            @SuppressWarnings("unchecked")
            ObservableList<String> selectedRow
                    = (ObservableList<String>) tableMain.getSelectionModel().getSelectedItem();

            List<String> pkCols = getPrimaryKeyColumnsMeta(currentTableName);

            StringBuilder sql = new StringBuilder("DELETE FROM " + currentTableName + " WHERE ");
            List<Integer> paramIndexes = new ArrayList<>();

            if (!pkCols.isEmpty()) {
                // PK exists: delete using PKs
                for (int i = 0; i < pkCols.size(); i++) {
                    if (i > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(pkCols.get(i)).append(" = ?");

                    int idx = columnNames.indexOf(pkCols.get(i));
                    if (idx < 0) {
                        throw new SQLException("PK column not found in loaded columns: " + pkCols.get(i));
                    }
                    paramIndexes.add(idx);
                }
            } else {
                // No PK: match all columns
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(columnNames.get(i)).append(" = ?");
                    paramIndexes.add(i);
                }
                sql.append(" LIMIT 1");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < paramIndexes.size(); i++) {
                    pstmt.setString(i + 1, selectedRow.get(paramIndexes.get(i)));
                }

                int affected = pstmt.executeUpdate();
                if (affected > 0) {
                    lblStatus.setText("Success: Row deleted.");
                    AlertHelper.showInfoMessage("Success", "Row deleted successfully.");
                    loadTableData(currentTableName);
                    resetMode();
                } else {
                    lblStatus.setText("Error: Could not delete row.");
                }
            }

        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, currentTableName);
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Delete Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleDoIt(ActionEvent event) {
        try {
            if (!validateInputs()) {
                return;
            }

            if (currentMode == OperationMode.ADD) {
                performInsert();
            } else if (currentMode == OperationMode.UPDATE) {
                performUpdate();
            }
        } catch (Exception e) {
             AlertHelper.showWarningMessage("Operation Error", "Failed to execute operation: " + e.getMessage());
        }
    }

    private void performInsert() {
        try {
            boolean confirmed = showConfirmation("Add Confirmation", "Add this new record to " + currentTableName + "?");
            if (!confirmed) return;

            StringBuilder sql = new StringBuilder("INSERT INTO " + currentTableName + " (");
            StringBuilder values = new StringBuilder("VALUES (");
            for (int i = 0; i < columnNames.size(); i++) {
                sql.append(columnNames.get(i));
                values.append("?");
                if (i < columnNames.size() - 1) {
                    sql.append(", ");
                    values.append(", ");
                }
            }
            sql.append(") ").append(values).append(")");

            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < inputFields.size(); i++) {
                String val = inputFields.get(i).getText();
                if (val.trim().isEmpty()) {
                     pstmt.setString(i + 1, null); 
                } else {
                     pstmt.setString(i + 1, val);
                }
            }
            pstmt.executeUpdate();

            lblStatus.setText("Success: New row added.");
            AlertHelper.showInfoMessage("Success", "New record added successfully.");
            loadTableData(currentTableName);
            resetMode();

        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, currentTableName);
        } catch (Exception e) {
             AlertHelper.showWarningMessage("Insert Error", "Failed to insert record: " + e.getMessage());
        }
    }

    private void performUpdate() {
        try {
            boolean confirmed = showConfirmation("Update Confirmation", "Save changes to this record?");
            if (!confirmed) return;

            PreparedStatement pstmt;

            // Enrollment table (double keys)
            if (currentTableName.equalsIgnoreCase("enrollment")) {
                StringBuilder sb = new StringBuilder("UPDATE " + currentTableName + " SET ");
                for (int i = 0; i < columnNames.size(); i++) {
                    sb.append(columnNames.get(i)).append(" = ?");
                    if (i < columnNames.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(" WHERE ssn = ? AND courseID = ?");
                pstmt = conn.prepareStatement(sb.toString());

                for (int i = 0; i < inputFields.size(); i++) {
                     String val = inputFields.get(i).getText();
                     if(val.trim().isEmpty()) pstmt.setString(i+1, null);
                     else pstmt.setString(i + 1, val);
                }
                pstmt.setString(inputFields.size() + 1, originalPK);  
                pstmt.setString(inputFields.size() + 2, originalPK2); 

            } else {
                // Other tables (single key)
                StringBuilder sb = new StringBuilder("UPDATE " + currentTableName + " SET ");
                String pkColumn = columnNames.get(0);

                for (int i = 0; i < columnNames.size(); i++) {
                    sb.append(columnNames.get(i)).append(" = ?");
                    if (i < columnNames.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(" WHERE ").append(pkColumn).append(" = ?");

                pstmt = conn.prepareStatement(sb.toString());
                for (int i = 0; i < inputFields.size(); i++) {
                     String val = inputFields.get(i).getText();
                     if(val.trim().isEmpty()) pstmt.setString(i+1, null);
                     else pstmt.setString(i + 1, val);
                }
                pstmt.setString(inputFields.size() + 1, originalPK);
            }

            pstmt.executeUpdate();

            lblStatus.setText("Success: Row updated.");
            AlertHelper.showInfoMessage("Success", "Record updated successfully.");
            loadTableData(currentTableName);
            resetMode();

        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, currentTableName);
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Update Error", "Failed to update record: " + e.getMessage());
        }
    }

    @FXML
    private void handleExecute(ActionEvent event) {
        try {
            String query = txtQuery.getText().trim();
            if (query.isEmpty()) return;

            tableQuery.getColumns().clear();
            ObservableList<ObservableList> qData = FXCollections.observableArrayList();

            PreparedStatement pstmt = conn.prepareStatement(query);
            if (query.toUpperCase().startsWith("SELECT")) {
                ResultSet rs = pstmt.executeQuery();
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();

                for (int i = 0; i < colCount; i++) {
                    final int j = i;
                    TableColumn col = new TableColumn(md.getColumnName(i + 1));
                    col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>) param -> {
                        Object val = param.getValue().get(j);
                        return new SimpleStringProperty(val != null ? val.toString() : "");
                    });
                    tableQuery.getColumns().add(col);
                }
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getString(i));
                    }
                    qData.add(row);
                }
                tableQuery.setItems(qData);
                lblStatus.setText("Status: Query executed.");
            } else {
                int count = pstmt.executeUpdate();
                lblStatus.setText("Status: Query OK. Rows affected: " + count);
                AlertHelper.showInfoMessage("Query Executed", "Rows affected: " + count);
                loadTableList();
                if (!currentTableName.isEmpty()) {
                    loadTableData(currentTableName);
                }
            }
        } catch (SQLException e) {
            AlertHelper.showDatabaseError(e, null); // Unknown table for custom query
        } catch (Exception e) {
            AlertHelper.showWarningMessage("Query Error", "An error occurred while executing query: " + e.getMessage());
        }
    }
}
