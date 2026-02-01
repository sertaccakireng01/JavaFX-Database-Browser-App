package dbbrowser;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import java.sql.SQLException;


//@author Sertac
public class AlertHelper {

    public static void showDatabaseError(SQLException ex, String tableName) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("Operation Failed");
                
        String userMessage = mapSqlErrorToUserMessage(ex, tableName);
        alert.setContentText(userMessage);

        //Show details
        String sqlDetails = "Table Context: " + (tableName == null ? "N/A" : tableName) + "\n" +
                            "Error Code: " + ex.getErrorCode() + "\n" +
                            "SQL State: " + ex.getSQLState() + "\n" +
                            "Message: " + ex.getMessage();

        Label label = new Label("Technical SQL Error Details:");
        TextArea textArea = new TextArea(sqlDetails);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    public static void showInfoMessage(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarningMessage(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String mapSqlErrorToUserMessage(SQLException ex, String tableName) {
        int errorCode = ex.getErrorCode();
        String rawMessage = ex.getMessage().toLowerCase();
        
        if (tableName == null) tableName = ""; 

        switch (errorCode) {
            // Case 1062: Duplicate Entry (Primary Key Violation)
            case 1062:
                if (tableName.equalsIgnoreCase("enrollment")) {
                    return "Already Enrolled: This student has already taken this course.\n" +
                           "You cannot add the same course to the same student twice.";
                } 
                else if (tableName.equalsIgnoreCase("course")) {
                    return "Course Code Taken: The CourseID you entered already exists.\n" +
                           "Please try a different CourseID.";
                }
                else if (tableName.equalsIgnoreCase("student")) {
                    return "Record Exists: The SSN you entered is already registered.\n" +
                           "Each student must have a unique SSN.";
                }
                else {
                    return "Duplicate Record Error: The ID or record you entered already exists in the database.";
                }

            // Case 1451: Foreign Key Delete Constraint (Cannot delete parent row)
            case 1451:
                if (tableName.equalsIgnoreCase("student")) {
                     return "Cannot Delete Student: This student has active course enrollments and grades.\n" +
                            "Please delete their records from the 'Enrollment' table first.";
                } 
                else if (tableName.equalsIgnoreCase("course")) {
                     return "Cannot Delete Course: There are students enrolled in this course.\n" +
                            "Please delete related records from the 'Enrollment' table first.";
                }
                else {
                    return "Delete Failed: This record cannot be deleted because it is referenced in another table.";
                }

            // Case 1452: Foreign Key Insert Constraint (Invalid Reference)
            case 1452:
                if (rawMessage.contains("student") || rawMessage.contains("ssn")) {
                    return "Student Not Found: The SSN you entered does not exist.\n" + 
                           "Please register the student in the Student table first.";
                } else if (rawMessage.contains("course")) {
                    return "Course Not Found: The CourseID you entered is invalid.\n" + 
                           "Please enter a valid code from the Course table.";
                } else {
                    return "Invalid Reference: The ID you entered was not found in the database.";
                }
            
            // Case 1048: Not Null Constraint (Missing Required Field)
            case 1048:
                 if (rawMessage.contains("subjectid")) return "Missing Info: 'Subject ID' cannot be empty.";
                 if (rawMessage.contains("coursenum")) return "Missing Info: 'Course Num' cannot be empty.";
                 if (rawMessage.contains("title")) return "Missing Info: 'Title' cannot be empty.";
                 if (rawMessage.contains("fname")) return "Missing Info: 'First Name' cannot be empty.";
                 if (rawMessage.contains("lname")) return "Missing Info: 'Last Name' cannot be empty.";
                 if (rawMessage.contains("datereg")) return "Missing Info: 'Date Reg' cannot be empty.";
                 return "Missing Required Field: Please ensure all mandatory fields are filled.";

            // Case 1366: Incorrect Integer Value (Type Mismatch)
            case 1366:
                if (rawMessage.contains("numcredit")) return "Invalid Credit: You must enter a NUMBER for 'NumCredit'.";
                if (rawMessage.contains("coursenum")) return "Invalid Course No: You must enter a NUMBER for 'CourseNum'.";
                return "Data Type Error: You entered text into a numeric field.";

            // Case 1406: Data Too Long (Character Limit Exceeded)
            case 1406:
                if (rawMessage.contains("grade")) return "Invalid Grade: 'Grade' can only be 1 character (A, B, C...).";
                if (rawMessage.contains("mi")) return "Invalid MI: 'MI' (Middle Initial) can only be 1 character.";
                return "Input Too Long: The text you entered exceeds the limit for this field.";

            case 0:
                return "Connection Error: Could not reach the database. Please ensure the MySQL service is running.";

            default:
                return "Unexpected Database Error: " + rawMessage;
        }
    }
}