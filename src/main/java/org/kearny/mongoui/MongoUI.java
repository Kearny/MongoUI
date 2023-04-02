package org.kearny.mongoui;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;

public class MongoUI
        extends Application {

    private static final Integer ITEMS_PER_PAGE = 10;
    private final Label bottomLabel = new Label();
    private final TableView<DocumentData> tableView = new TableView<>();
    private MongoClient mongoClient;
    private List<String> databaseNames;
    private List<String> databaseCollectionNames;
    private Pagination pagination;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        var javaVersion = System.getProperty("java.version");
        var javafxVersion = System.getProperty("javafx.version");
        var topLabel = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

        var treeView = getMongoTreeView();

        var scene = new Scene(new BorderPane(tableView, topLabel, null, bottomLabel, treeView), 640, 480);
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

    private void loadCollectionData(TreeItem<String> treeItem) {
        var databaseName = treeItem.getParent().getValue();
        var collectionName = treeItem.getValue();

        var collection = mongoClient.getDatabase(databaseName).getCollection(collectionName);

        var documents = StreamSupport.stream(collection.find().limit(50).spliterator(), false)
                .toList();

        var allKeys = new HashSet<String>();
        documents.forEach(document -> allKeys.addAll(document.keySet()));

        var tableColumns = allKeys.stream()
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
            tableView.getColumns().add(column);
        });

        tableView.getItems().clear();
        documents.forEach(document -> tableView.getItems().add(new DocumentData(document)));

        var documentCount = collection.countDocuments();
        int pageCount = (int) Math.ceil((double) documentCount / ITEMS_PER_PAGE);

        pagination = new Pagination(pageCount, 0);
        pagination.setPageFactory(pageIndex -> {
            loadPage(treeItem, pageIndex);

            return tableView;
        });

        // Ajoutez le contrôle Pagination à la mise en page
        var borderPane = (BorderPane) tableView.getScene().getRoot();
        borderPane.setCenter(pagination);

        // Chargez la première page de données
        loadPage(treeItem, 0);
    }

    private void loadPage(TreeItem<String> treeItem, int pageIndex) {
        var databaseName = treeItem.getParent().getValue();
        var collectionName = treeItem.getValue();
        var collection = mongoClient.getDatabase(databaseName).getCollection(collectionName);

        int pageSize = ITEMS_PER_PAGE;
        int skipCount = pageIndex * pageSize;

        var documents = StreamSupport.stream(collection.find().skip(skipCount).limit(pageSize).spliterator(), true)
                .toList();
        tableView.getItems().clear();
        documents.forEach(document -> tableView.getItems().add(new DocumentData(document)));

        bottomLabel.setText("Affichage de " + documents.size() + " documents, page " + (pageIndex + 1) + "/" + pagination.getPageCount());
    }


}
