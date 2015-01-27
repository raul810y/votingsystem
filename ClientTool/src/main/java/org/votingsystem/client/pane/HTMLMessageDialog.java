package org.votingsystem.client.pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HTMLMessageDialog {

    private static Logger log = Logger.getLogger(HTMLMessageDialog.class);

    private final Stage stage;
    private WebView webView;

    public HTMLMessageDialog() {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        //stage.initOwner(window);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });

        VBox verticalBox = new VBox(10);
        webView = new WebView();
        webView.getEngine().setUserDataDirectory(new File(ContextVS.WEBVIEWDIR));
        webView.setPrefHeight(400);
        VBox.setVgrow(webView, Priority.ALWAYS);
        Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                stage.hide();
            }});
        verticalBox.getChildren().addAll(webView, acceptButton);
        verticalBox.getStyleClass().add("modal-dialog");
        stage.setScene(new Scene(verticalBox, Color.TRANSPARENT));
        stage.getScene().getStylesheets().add(Utils.getResource("/css/modal-dialog.css"));
        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }

    public void showMessage(String message, String caption) {
        webView.getEngine().loadContent(message);
        stage.setTitle(caption);
        stage.centerOnScreen();
        stage.show();
    }

    class Delta { double x, y; }

}