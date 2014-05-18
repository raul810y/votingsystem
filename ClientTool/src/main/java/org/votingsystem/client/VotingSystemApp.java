package org.votingsystem.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.votingsystem.client.dialog.DecompressFileDialog;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.dialog.SignedDocumentsBrowser;
import org.votingsystem.client.util.BrowserVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.AppHostVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.NifUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VotingSystemApp extends Application implements DecompressFileDialog.Listener, AppHostVS {

    private static Logger logger = Logger.getLogger(VotingSystemApp.class);

    private BrowserVS browserVS;
    private SettingsDialog settingsDialog;
    public static String locale = "es";

    @Override public void stop() {
        logger.debug("stop");
        //Platform.exit();
        System.exit(0);
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        ContextVS.initSignatureClient(this, "log4jClientTool.properties",
                "clientToolMessages.properties", locale);

        logger.debug("NifUtils.getNif(555666): " + NifUtils.getNif(555666));


        VBox verticalBox = new VBox(100);
        Button voteButton = new Button(ContextVS.getMessage("voteButtonLbl"));
        voteButton.setGraphic(new ImageView(Utils.getImage(this, "fa-envelope")));
        voteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingPage();
            }});
        voteButton.setPrefWidth(500);

        Button selectRepresentativeButton = new Button(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeButton.setGraphic(new ImageView(Utils.getImage(this, "fa-hand-o-right")));
        selectRepresentativeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSelectRepresentativePage();
            }});
        selectRepresentativeButton.setPrefWidth(500);

        Button votingSystemProceduresButton = new Button(ContextVS.getMessage("votingSystemProceduresLbl"));
        votingSystemProceduresButton.setGraphic(new ImageView(Utils.getImage(this, "fa-cogs")));
        votingSystemProceduresButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVotingSystemProceduresPage();
            }});
        votingSystemProceduresButton.setPrefWidth(500);

        Button openSignedFileButton = new Button(ContextVS.getMessage("openSignedFileButtonLbl"));
        openSignedFileButton.setGraphic(new ImageView(Utils.getImage(this, "application-certificate")));
        openSignedFileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSignedFile();
            }});
        openSignedFileButton.setPrefWidth(500);

        final Button openBackupButton = new Button(ContextVS.getMessage("openBackupButtonLbl"));
        openBackupButton.setGraphic(new ImageView(Utils.getImage(this, "fa-archive")));
        openBackupButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                openBackup();
            }
        });
        openBackupButton.setPrefWidth(500);

        Button vicketUsersProceduresButton = new Button(ContextVS.getMessage("vicketUsersLbl"));
        vicketUsersProceduresButton.setGraphic(new ImageView(Utils.getImage(this, "fa-money")));
        vicketUsersProceduresButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVicketUserProcedures();
            }});
        vicketUsersProceduresButton.setPrefWidth(500);


        Button vicketAdminProceduresButton = new Button(ContextVS.getMessage("vicketAdminLbl"));
        vicketAdminProceduresButton.setGraphic(new ImageView(Utils.getImage(this, "fa-money")));
        vicketAdminProceduresButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openVicketAdminProcedures();
            }});
        vicketAdminProceduresButton.setPrefWidth(500);

        Button settingsButton = new Button(ContextVS.getMessage("settingsLbl"));
        settingsButton.setGraphic(new ImageView(Utils.getImage(this, "fa-wrench")));
        settingsButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                openSettings();
            }});

        HBox footerButtonsBox = new HBox(10);

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setGraphic(new ImageView(Utils.getImage(this, "cancel_16")));
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                VotingSystemApp.this.stop();
            }});

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerButtonsBox.getChildren().addAll(settingsButton, spacer, cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 10, 0, 10));

        verticalBox.getChildren().addAll(voteButton, selectRepresentativeButton, votingSystemProceduresButton,
                openSignedFileButton, openBackupButton, vicketUsersProceduresButton, vicketAdminProceduresButton,
                footerButtonsBox);
        verticalBox.getStyleClass().add("modal-dialog");
        verticalBox.setStyle("-fx-max-width: 1000px;");

        primaryStage.setScene(new Scene(verticalBox, Color.TRANSPARENT));
        primaryStage.getScene().getStylesheets().add(((Object)this).getClass().getResource(
                "/resources/css/modal-dialog.css").toExternalForm());
        primaryStage.initStyle(StageStyle.UNDECORATED);

        // allow the UNDECORATED Stage to be dragged around.
        final Node root = primaryStage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = primaryStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = primaryStage.getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                primaryStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                primaryStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
        primaryStage.show();
    }

    private void openSignedFile() {
        logger.debug("openSignedFile");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    SignedDocumentsBrowser documentBrowser =  new SignedDocumentsBrowser(new JFrame(), false);
                    documentBrowser.openSignedFile();
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void openBackup() {
        logger.debug("openBackup");
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    final JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.getName().toLowerCase().endsWith(".zip") || f.isDirectory();
                        }

                        public String getDescription() {
                            return "ZIP Files";
                        }
                    });
                    int returnVal = chooser.showOpenDialog(new JFrame());
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File result = chooser.getSelectedFile();
                        String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
                        DecompressFileDialog dialog = new DecompressFileDialog(new JFrame(), true);
                        dialog.unZipBackup(VotingSystemApp.this, result.getAbsolutePath(), outputFolder);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });

    }

    private void openVotingSystemProceduresPage() {
        logger.debug("openVotingSystemProceduresPage");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://www.sistemavotacion.org/AccessControl/app/admin?menu=admin");
            }
        });
    }

    private void openVicketUserProcedures() {
        logger.debug("openGroupAdmin");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://vickets/Vickets/app/user?menu=user");
            }
        });
    }

    private void openVicketAdminProcedures() {
        logger.debug("openGroupAdmin");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://vickets/Vickets/app/admin?menu=admin");
            }
        });
    }

    private void openVotingPage() {
        logger.debug("openVotingPage");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://www.sistemavotacion.org/AccessControl/eventVSElection/main?menu=user");
            }
        });
    }

    private void openSelectRepresentativePage() {
        logger.debug("openSelectRepresentativePage");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if(browserVS == null) browserVS = new BrowserVS();
                browserVS.loadURL("http://www.sistemavotacion.org/AccessControl/representative/main?menu=user");
            }
        });
    }


    private void openSettings() {
        logger.debug("openSettings");
        Platform.runLater(new Runnable() {
            @Override public void run() {
                if (settingsDialog == null) settingsDialog = new SettingsDialog();
                settingsDialog.show();
            }
        });
    }

    @Override public void sendMessageToHost(OperationVS operation) {
        logger.debug("### sendMessageToHost");
    }

    /*private void clickShow(ActionEvent event) {
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(YourClassController.class.getResource("YourClass.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("My modal window");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(((Node)event.getSource()).getScene().getWindow() );
        stage.show();
    }*/

    class Delta { double x, y; }

    @Override public void processDecompressedFile(ResponseVS response) {
        logger.debug("processDecompressedFile - statusCode:" + response.getStatusCode());
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            SignedDocumentsBrowser documentBrowser = new SignedDocumentsBrowser(new JFrame(), false);
            documentBrowser.setVisible((String) response.getData());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}