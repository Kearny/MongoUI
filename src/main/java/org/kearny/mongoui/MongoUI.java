package org.kearny.mongoui;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MongoUI
        extends Application {

    private MongoClient mongoClient;
    private TableView<DocumentData> tableView = new TableView();
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
        mongoClient = MongoClients.create(mongoUrl);
        databaseNames = StreamSupport.stream(mongoClient.listDatabaseNames().spliterator(), false)
                                     .toList();
        var databaseNameTreeItems = databaseNames.stream()
                                                 .map(TreeItem::new)
                                                 .toList();

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
        var documentCount = collection.countDocuments();
        bottomLabel.setText("Nombre de documents dans la collection: " + documentCount);

        var documents = StreamSupport.stream(collection.find().spliterator(), true)
                                     .toList();
        var documentSample = documents.stream().findFirst().get();

        var tableColumns = documentSample.keySet().stream()
                .map(key -> {
                    var column = new TableColumn<DocumentData, Object>(key);
                    column.setCellValueFactory(
                            cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().get(key))
                    );

                    return column;
                })
                .toList();


        tableView.getColumns().clear();
        tableColumns.forEach(column -> {
            column.setCellValueFactory(new PropertyValueFactory<>(column.getText()));
            tableView.getColumns().add(column);
        });

        tableView.getItems().clear();
        documents.forEach(document -> tableView.getItems().add(new DocumentData(document)));
    }

    private void loadDatabaseCollections(TreeItem<String> treeItem) {
        var database = mongoClient.getDatabase(treeItem.getValue());
        databaseCollectionNames = StreamSupport.stream(database.listCollectionNames().spliterator(), false)
                                               .toList();
        var collectionNameTreeItems = StreamSupport.stream(database.listCollectionNames().spliterator(), false)
                                                   .map(TreeItem::new)
                                                   .toList();

        treeItem.getChildren().clear();
        treeItem.getChildren().addAll(collectionNameTreeItems);
    }

    public static void main(String[] args) {
        launch();
    }
}
