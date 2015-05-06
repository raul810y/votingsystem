package org.votingsystem.client.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.votingsystem.client.Browser;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogVS {

    private Stage stage;
    private DecoratedPane decoratedPane;

    public DialogVS(String fxmlFilePath) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
    }

    public DialogVS(String fxmlFilePath, String caption) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
        setCaption(caption);
    }

    public DialogVS(String fxmlFilePath, StageStyle stageStyle) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath));
        stage = new Stage(stageStyle);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(Browser.getInstance().getScene().getWindow());
        fxmlLoader.setController(this);
        stage.centerOnScreen();

        decoratedPane = new DecoratedPane(null, null, fxmlLoader.load(), stage);

        stage.setScene(new Scene(decoratedPane));
        Utils.addMouseDragSupport(stage);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        decoratedPane.getScene().getStylesheets().add(Utils.getResource("/css/dialogvs.css"));
        decoratedPane.getStyleClass().add("glassBox");
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
    }

    public void setCaption(String caption) {
        if(decoratedPane != null) decoratedPane.setCaption(caption);
        else stage.setTitle(caption);
    }

    public DialogVS(Pane pane) {
        stage = new Stage(StageStyle.TRANSPARENT);


        decoratedPane = new DecoratedPane(null, null, pane, stage);
        stage.setScene(new Scene(decoratedPane));
        Utils.addMouseDragSupport(stage);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        decoratedPane.getScene().getStylesheets().add(Utils.getResource("/css/dialogvs.css"));
        decoratedPane.getStyleClass().add("glassBox");
        decoratedPane.getScene().setFill(Color.TRANSPARENT);

        //stage.setScene(new Scene(pane));
        //stage.getScene().setFill(Color.TRANSPARENT);
        //stage.initModality(Modality.APPLICATION_MODAL);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        stage.centerOnScreen();
        //Utils.addMouseDragSupport(stage);
    }

    public Parent getParent() {
        return stage.getScene().getRoot();
    }

    public Stage getStage() {
        return stage;
    }


    public void show() {
        stage.sizeToScene();
        stage.show();
        stage.toFront();
    }

    public void hide() {
        try {
            stage.close();
            this.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}


