package org.kearny.mongoui;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MongoUI
        extends Application {

    private MongoClient mongoClient;
    private TableView tableView = new TableView();
    private List<String> databaseNames;
    private List<String> databaseCollectionNames;
    private final Label bottomLabel = new Label();

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label topLabel = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

        TreeView<String> treeView = getMongoTreeView();

        Scene scene = new Scene(new BorderPane(tableView, topLabel, null, bottomLabel, treeView), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    private TreeView<String> getMongoTreeView() {
        var mongoUrl = "mongodb://localhost:27017";
        MongoClientURI connectionString = new MongoClientURI(mongoUrl);
        mongoClient = new MongoClient(connectionString);
        databaseNames = StreamSupport.stream(mongoClient.listDatabaseNames().spliterator(), false)
                                     .collect(Collectors.toUnmodifiableList());
        var databaseNameTreeItems = databaseNames.stream()
                                                 .map(TreeItem::new)
                                                 .collect(Collectors.toUnmodifiableList());

        TreeItem<String> root = new TreeItem<>(mongoUrl);
        root.setExpanded(true);
        root.getChildren().addAll(databaseNameTreeItems);
        TreeView<String> treeView = new TreeView<>(root);

        treeView.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                var treeItem = treeView.getSelectionModel().getSelectedItem();

                if (databaseNames.contains(treeItem.getValue())) {
                    loadDatabaseCollections(treeItem);
                }

                if (databaseCollectionNames.contains(treeItem.getValue())) {
                    loadCollectionData(treeItem);
                }
            }
        });

        return treeView;
    }

    private void loadCollectionData(TreeItem<String> treeItem) {
        var databaseName = treeItem.getParent().getValue();
        var collectionName = treeItem.getValue();

        var collection = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        var count = collection.count();
        bottomLabel.setText("Nombre de documents dans la collection: " + count);

        var documents = StreamSupport.stream(collection.find().spliterator(), true)
                                     .collect(Collectors.toUnmodifiableList());
        var documentSample = documents.stream().findFirst().get();

        var tableColumns = documentSample.keySet().stream()
                                         .map(TableColumn::new)
                                         .collect(Collectors.toUnmodifiableList());

        tableView.getColumns().addAll(tableColumns);

        tableView.getItems().add(documents);
    }

    private void loadDatabaseCollections(TreeItem<String> treeItem) {
        var database = mongoClient.getDatabase(treeItem.getValue());
        databaseCollectionNames = StreamSupport.stream(database.listCollectionNames().spliterator(), false)
                                               .collect(Collectors.toUnmodifiableList());
        var collectionNameTreeItems = StreamSupport.stream(database.listCollectionNames().spliterator(), false)
                                                   .map(TreeItem::new)
                                                   .collect(Collectors.toUnmodifiableList());

        treeItem.getChildren().clear();
        treeItem.getChildren().addAll(collectionNameTreeItems);
    }

    public static void main(String[] args) {
        launch();
    }

    private class ColumnValue {

        private String column;
        private Object value;

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
