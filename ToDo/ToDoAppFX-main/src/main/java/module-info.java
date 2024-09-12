module com.example.todoappfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.todoappfx to javafx.fxml;
    exports com.example.todoappfx;
}