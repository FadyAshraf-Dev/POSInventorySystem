module org.example.posinventorysystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.posinventorysystem to javafx.fxml;
    exports org.example.posinventorysystem;
}