module com.example.bloggingplatform {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // FXMLLoader injects @FXML fields and handlers by reflection
    opens com.example.bloggingplatform.controller to javafx.fxml;
    exports com.example.bloggingplatform;
}
