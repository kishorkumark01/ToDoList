package com.example.todoappfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;

import java.util.Objects;
import java.util.Optional;



import java.sql.*;
import java.time.LocalDate;

public class TaskApp extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/to_do_list_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "helloworld";

    private boolean databaseExists() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/?serverTimezone=UTC", DB_USER, DB_PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getCatalogs();
            while (resultSet.next()) {
                String databaseName = resultSet.getString("TABLE_CAT");
                if (databaseName.equalsIgnoreCase("to_do_list_db")) {
                    return true; // Database exists
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Database does not exist
    }

    private void createDatabase() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/?serverTimezone=UTC", DB_USER, DB_PASSWORD)) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS to_do_list_db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTaskDetailsTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Statement statement = connection.createStatement();
            String createTableQuery = "CREATE TABLE IF NOT EXISTS task_details (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(255) NOT NULL," +
                    "description TEXT," +
                    "date DATE," +
                    "completed TINYINT(1)" +
                    ")";
            statement.executeUpdate(createTableQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private final ObservableList<Task> taskList = FXCollections.observableArrayList();
    private final ListView<Task> listView = new ListView<>();
//    private final TextArea taskDetailsTextArea = new TextArea();

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("To-Do List App");


        listView.setItems(taskList);
        listView.setCellFactory(param -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Button deleteButton = new Button("Delete");
            private final Label titleLabel = new Label();
            private final Label descriptionLabel = new Label();





            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item.isCompleted());
                    checkBox.setOnAction(event -> toggleTaskCompletion(item));
                    checkBox.getStyleClass().add("checkbox");

                    Label titleLabel = new Label(item.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14pt;");

                    Label dateLabel = new Label(item.getDate().toString());
                    dateLabel.setStyle("-fx-font-size: 14pt;");

                    Label descriptionLabel = new Label(item.getDescription());
                    descriptionLabel.setStyle("-fx-font-size: 10pt;");

                    HBox titleDateBox = new HBox(5, titleLabel, dateLabel);
                    VBox taskDetailsVBox = new VBox(5, titleDateBox, descriptionLabel);

                    if (item.isCompleted()) {
                        titleLabel.setStyle("-fx-text-fill: grey;");
                        dateLabel.setStyle("-fx-text-fill: grey;");
                        descriptionLabel.setStyle("-fx-text-fill: grey;");
                    } else if (item.getDate().isBefore(LocalDate.now())) {
                        titleLabel.setStyle("-fx-text-fill: red;");
                        dateLabel.setStyle("-fx-text-fill: red;");
                        descriptionLabel.setStyle("-fx-text-fill: red;");
                    } else {
                        titleLabel.setStyle("-fx-text-fill: black;");
                        dateLabel.setStyle("-fx-text-fill: black;");
                        descriptionLabel.setStyle("-fx-text-fill: black;");
                    }

                    HBox.setHgrow(taskDetailsVBox, Priority.ALWAYS);
                    HBox.setHgrow(deleteButton, Priority.NEVER);

                    // Remaining code...


                    ImageView trashIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/todoappfx/trash-can-icon.png"))));
                    trashIcon.setFitWidth(16);
                    trashIcon.setFitHeight(16);

                    deleteButton.setGraphic(trashIcon);
                    deleteButton.getStyleClass().add("button-delete");
                    deleteButton.setOnAction(event -> promptDeleteConfirmation(item));

                    HBox hbox = new HBox(10, checkBox, taskDetailsVBox, deleteButton);
                    setGraphic(hbox);
                }
            }




        });


        Button addButton = new Button("+");
        addButton.getStyleClass().add("add-button");
        Object taskDetailsTextArea = null;
//        vBox.getChildren().addAll(listView, taskDetailsTextArea, addButton);
        VBox.setMargin(addButton, new Insets(10)); // Set margin for the button
        addButton.setOnAction(e -> openAddTaskWindow());

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10, 10, 10, 10));
        vBox.getChildren().addAll(listView, addButton);

        Scene scene = new Scene(vBox, 800, 500);

        // Add the CSS file to the scene
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/todoappfx/styles.css")).toExternalForm());

        primaryStage.setScene(scene);


        primaryStage.setOnCloseRequest(event -> {
            // Ensure database connection is closed when the application is closed
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        // Refresh tasks after the UI is shown
        refreshTasks();
    }



    private void openAddTaskWindow() {
        Stage addTaskStage = new Stage();
        addTaskStage.initModality(Modality.APPLICATION_MODAL);
        addTaskStage.setTitle("Add New Task");

        VBox addTaskLayout = new VBox(10);
        addTaskLayout.setPadding(new Insets(10, 10, 10, 10));

        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        DatePicker datePicker = new DatePicker();

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> {
            addTask(titleField.getText(), descriptionArea.getText(), datePicker.getValue());
            addTaskStage.close();
        });

        addTaskLayout.getChildren().addAll(
                new Label("Title:"),
                titleField,
                new Label("Description:"),
                descriptionArea,
                new Label("Due Date:"),
                datePicker,
                addButton
        );

        Scene addTaskScene = new Scene(addTaskLayout, 300, 250);
        addTaskStage.setScene(addTaskScene);
        addTaskStage.showAndWait();
    }


    private void addTask(String title, String description, LocalDate date) {
        if (title.trim().isEmpty()) {
            showAlert("Title cannot be empty");
            return;
        }

        if (date.isBefore(LocalDate.now())) {
            showAlert("Please select a date that is not behind the current date");
            return;
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO task_details (title, description, date, completed) VALUES (?, ?, ?, false)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, title);
                preparedStatement.setString(2, description);
                preparedStatement.setDate(3, Date.valueOf(date));
                preparedStatement.executeUpdate();
                refreshTasks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void updateCheckBoxStyle(CheckBox checkBox, boolean completed) {
        if (completed) {
            checkBox.getStyleClass().add("checkbox-checked");
        } else {
            checkBox.getStyleClass().remove("checkbox-checked");
        }
    }
    private void markTaskCompleted(Task task) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE task_details SET completed = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setBoolean(1, true);
                preparedStatement.setInt(2, task.getId());
                preparedStatement.executeUpdate();
                refreshTasks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        // Update the completion status in the database
        updateTaskCompletionStatus(task);
    }

    private void updateTaskCompletionStatus(Task task) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE task_details SET completed = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setBoolean(1, task.isCompleted());
                preparedStatement.setInt(2, task.getId());
                preparedStatement.executeUpdate();
                refreshTasks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void refreshTasks() {
        taskList.clear();
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM task_details ORDER BY completed, date";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String title = resultSet.getString("title");
                    String description = resultSet.getString("description");
                    LocalDate date = resultSet.getDate("date").toLocalDate();
                    boolean completed = resultSet.getBoolean("completed");

                    Task task = new Task(id, title, description, date, completed);
                    taskList.add(task);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void promptDeleteConfirmation(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Task Deletion");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this task?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteTask(task);
        }
    }

    private void deleteTask(Task task) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "DELETE FROM task_details WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, task.getId());
                preparedStatement.executeUpdate();
                refreshTasks();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }





    private static class Task {
        private final int id;
        private final String title;
        private final String description;
        private final LocalDate date;
        private boolean completed;

        public Task(int id, String title, String description, LocalDate date, boolean completed) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.date = date;
            this.completed = completed;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public LocalDate getDate() {
            return date;
        }

        public boolean isCompleted() {
            return completed;
        }

        public int getId() {
            return id;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        @Override
        public String toString() {
            return title;
        }
    }

}




