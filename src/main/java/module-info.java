module org.example.posinventorysystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;

    opens org.example.posinventorysystem.app to javafx.fxml, javafx.graphics;
    opens org.example.posinventorysystem.controller to javafx.fxml;
    opens org.example.posinventorysystem.model to javafx.base, javafx.fxml;

    exports org.example.posinventorysystem.app;
    exports org.example.posinventorysystem.model;
    exports org.example.posinventorysystem.repo;
    exports org.example.posinventorysystem.data;
}