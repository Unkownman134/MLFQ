module com.mlfq.mlfq {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.mlfq.mlfq to javafx.fxml;
    exports com.mlfq.mlfq;
}