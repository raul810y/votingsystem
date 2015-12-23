package org.votingsystem.client.webextension.pane;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.util.FullScreenHelper;
import org.votingsystem.client.webextension.util.ResizeHelper;
import org.votingsystem.client.webextension.util.Utils;

import java.util.logging.Logger;

/**
 * https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DecoratedPane extends VBox {

    private static Logger log = Logger.getLogger(DecoratedPane.class.getSimpleName());

    private FullScreenHelper fullScreenHelper;
    private Label captionLbl;
    private VBox mainDialog;
    private HBox toolBar;
    private Button closeButton;
    private Stage stage;
    private Pane contentPane;

    public DecoratedPane(String caption, MenuButton menuButton, Pane contentPane, Stage stage) {
        this.stage = stage;
        this.contentPane = contentPane;
        setAlignment(Pos.TOP_CENTER);
        fullScreenHelper = new FullScreenHelper(stage);
        setStyle("-fx-background-insets: 3;-fx-effect: dropshadow(three-pass-box, #888, 5, 0, 0, 0);" +
                "-fx-background-radius: 4; -fx-padding: 5, 5;");
        mainDialog = new VBox();
        mainDialog.setStyle("-fx-background-radius: 4;");
        mainDialog.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(mainDialog, Priority.ALWAYS);
        VBox.setVgrow(mainDialog, Priority.ALWAYS);
        HBox.setHgrow(contentPane, Priority.ALWAYS);
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        toolBar = new HBox();
        toolBar.setSpacing(10);
        toolBar.setStyle("-fx-padding: 3, 20;");
        toolBar.setAlignment(Pos.TOP_CENTER);
        HBox captionBox = new HBox();
        captionBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(captionBox, Priority.ALWAYS);
        captionLbl = new Label();
        captionLbl.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold; -fx-text-fill: #888;");
        captionBox.getChildren().add(captionLbl);
        toolBar.getChildren().add(captionBox);
        if(caption != null) captionLbl.setText(caption);
        closeButton = new Button();
        closeButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> getScene().getWindow().hide());
        if(menuButton != null) {
            menuButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.BARS));
            toolBar.getChildren().add(menuButton);
        }
        toolBar.getChildren().add(closeButton);
        getChildren().add(mainDialog);
        mainDialog.getChildren().addAll(toolBar, contentPane);
        final Delta dragDelta = new Delta();
        AnchorPane.setTopAnchor(toolBar, 0.0);
        toolBar.setOnMousePressed(mouseEvent -> {  // record a delta distance for the drag and drop operation.
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        toolBar.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
        toolBar.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    fullScreenHelper.toggleFullScreen();
                }
            }
        });
    }

    public void setCloseButtonVisible(boolean isVisible) {
        if(isVisible && !toolBar.getChildren().contains(closeButton)) {
            toolBar.getChildren().add(closeButton);
        } else if(!isVisible && toolBar.getChildren().contains(closeButton)) {
            toolBar.getChildren().remove(closeButton);
        }
    }

    public void addMenuButton(MenuButton menuButton) {
        toolBar.getChildren().add(0, menuButton);
    }

    public void setCaption(String caption) {
        captionLbl.setText(caption);
    }

    public void addResizeListener() {//must be called after Scene has been set
        ResizeHelper.addResizeListener(stage);
    }

    public Pane getContentPane() {
        return contentPane;
    }

    static class Delta { double x, y; }
}
