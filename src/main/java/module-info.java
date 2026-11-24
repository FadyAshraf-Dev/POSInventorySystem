module org.example.posinventorysystem {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.example.posinventorysystem.app to javafx.fxml, javafx.graphics;
    opens org.example.posinventorysystem.controller to javafx.fxml;

    exports org.example.posinventorysystem.app;
}