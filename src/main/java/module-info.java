module com.example.bloggingplatform {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires java.naming;   // the MongoDB driver finds the Atlas servers (mongodb+srv) with a DNS lookup through JNDI

    // FXMLLoader injects @FXML fields and handlers by reflection
    opens com.example.bloggingplatform.controller to javafx.fxml;
    exports com.example.bloggingplatform;
}
