package org.votingsystem.client;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DecompressBackupPane;
import org.votingsystem.client.util.*;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.HttpHelper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VotingSystemApp extends Application implements DecompressBackupPane.Listener, AppHostVS {

    private static Logger logger = Logger.getLogger(VotingSystemApp.class);

    private BrowserVS browserVS;
    private SettingsDialog settingsDialog;
    public static String locale = "es";
    private static VotingSystemApp INSTANCE;

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return ContextVS.getInstance().getVotingSystemSSLCerts().toArray(new X509Certificate[]{});
            }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                logger.debug("trustAllCerts - checkClientTrusted");
            }
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType ) throws CertificateException {
                logger.debug("trustAllCerts - checkServerTrusted");
                try {
                    CertUtil.verifyCertificate(ContextVS.getInstance().getVotingSystemSSLTrustAnchors(), false,
                            Arrays.asList(certs));
                } catch(Exception ex) {
                    throw new CertificateException(ex.getMessage());
                }
            }
        }
    };

    @Override public void stop() {
        logger.debug("stop");
        //Platform.exit();
        System.exit(0);
    }

    public static VotingSystemApp getInstance() {
        return INSTANCE;
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        ContextVS.initSignatureClient(this, "log4jClientTool.properties", "clientToolMessages.properties", locale);
        browserVS = new BrowserVS();
        new Thread(new Runnable() {
            @Override public void run() {
                boolean loadedFromJar = false;
                if(VotingSystemApp.class.getResource(VotingSystemApp.this.getClass().getSimpleName() +
                        ".class").toString().contains("jar:file")) {
                    loadedFromJar = true;
                }
                logger.debug("ServerLoaderTask - loadedFromJar: " + loadedFromJar);
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (GeneralSecurityException ex) {
                    logger.error(ex.getMessage(), ex);
                }

                WebSocketService webSocketService = new WebSocketService(ContextVS.getInstance().
                        getVotingSystemSSLCerts().iterator().next(), "wss://vickets:8443/Vickets/websocket/service");
                webSocketService.restart();

                String accessControlServerURL = null;
                String vicketsServerURL = null;
                if(loadedFromJar) {
                    accessControlServerURL = ContextVS.getMessage("prodAccessControlServerURL");
                    vicketsServerURL = ContextVS.getMessage("prodVicketsServerURL");
                } else {
                    accessControlServerURL = ContextVS.getMessage("devAccessControlServerURL");
                    vicketsServerURL = ContextVS.getMessage("devVicketsServerURL");
                }
                try {SignatureService.checkServer(accessControlServerURL);}
                catch(Exception ex) {logger.error(ex.getMessage(), ex);}
                try {SignatureService.checkServer(vicketsServerURL);}
                catch(Exception ex) {logger.error(ex.getMessage(), ex);}
            }
        }).start();

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
                SignedDocumentsBrowser.showDialog();

            }});
        openSignedFileButton.setPrefWidth(500);

        final Button openBackupButton = new Button(ContextVS.getMessage("openBackupButtonLbl"));
        openBackupButton.setGraphic(new ImageView(Utils.getImage(this, "fa-archive")));
        openBackupButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent actionEvent) {
                DecompressBackupPane.showDialog(VotingSystemApp.this);
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

        primaryStage.setScene(new Scene(verticalBox));
        primaryStage.getScene().getStylesheets().add(((Object)this).getClass().getResource(
                "/resources/css/modal-dialog.css").toExternalForm());
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle(ContextVS.getMessage("mainDialogCaption"));

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

    private void openVotingSystemProceduresPage() {
        logger.debug("openVotingSystemProceduresPage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getAccessControl().getProceduresPageURL(),
                        ContextVS.getMessage("votingSystemProceduresLbl"));
            }
        });
    }

    private void openVotingPage() {
        logger.debug("openVotingPage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                        ContextVS.getMessage("voteButtonLbl"));
            }
        });
    }

    private void openSelectRepresentativePage() {
        logger.debug("openSelectRepresentativePage");
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                        ContextVS.getMessage("selectRepresentativeButtonLbl"));
            }
        });
    }

    private void openVicketUserProcedures() {
        logger.debug("openVicketUserProcedures");
        if(ContextVS.getInstance().getVicketServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getVicketServer().getUserProceduresPageURL(),
                        ContextVS.getMessage("vicketUsersLbl"));
            }
        });
    }

    private void openVicketAdminProcedures() {
        logger.debug("openVicketAdminProcedures");
        if(ContextVS.getInstance().getVicketServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"));
            return;
        }
        Platform.runLater(new Runnable() {
            @Override public void run() {
                browserVS.loadURL(ContextVS.getInstance().getVicketServer().getAdminProceduresPageURL(),
                        ContextVS.getMessage("vicketAdminLbl"));

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

    MessageDialog messageDialog;
    public void showMessage(final String message) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                if (messageDialog == null) messageDialog = new MessageDialog();
                messageDialog.showMessage(message);
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
            SignedDocumentsBrowser documentBrowser = new SignedDocumentsBrowser();
            documentBrowser.setVisible((String) response.getData());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}