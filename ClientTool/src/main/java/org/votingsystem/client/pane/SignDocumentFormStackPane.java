package org.votingsystem.client.pane;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignDocumentFormStackPane extends StackPane {

    private static Logger log = Logger.getLogger(SignDocumentFormStackPane.class.getSimpleName());

    public enum Operation {SEND_SMIME, SIGN_SMIME}

    public interface OperationListener {
        public void processResult(Operation operation, ResponseVS responseVS);
    }

    private Text messageText;
    private boolean isCapsLockPressed = false;
    private Text capsLockPressedMessageText;
    private char[] password;
    private PasswordField password1Field;
    private PasswordField password2Field;
    private VBox passwordVBox;
    private Region passwordRegion;
    private String mainMessage = null;
    private Operation operation;
    private String serviceURL;
    private String toUser;
    private String textToSign;
    private String messageSubject;
    private SMIMEMessage smimeMessage;
    private ProgressBar progressBar;
    private Region progressRegion;
    private VBox progressBox;
    private OperationListener operationListener;
    private Text progressMessageText;

    public SignDocumentFormStackPane(OperationListener operationListener) {
        this.operationListener = operationListener;
        progressRegion = new Region();
        progressRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        progressRegion.setPrefSize(240, 160);

        passwordRegion = new Region();
        passwordRegion.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        passwordRegion.setPrefSize(240, 160);

        progressBox = new VBox();
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPrefWidth(400);
        progressBox.setPrefHeight(300);

        progressMessageText = new Text();
        progressMessageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #f9f9f9;");
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setLayoutY(10);
        progressBox.getChildren().addAll(progressMessageText, progressBar);

        passwordVBox = new VBox(10);
        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 16;-fx-font-weight: bold;-fx-fill: #6c0404;");
        VBox.setMargin(messageText, new Insets(0, 0, 15, 0));
        messageText.setTextAlignment(TextAlignment.CENTER);

        capsLockPressedMessageText = new Text(ContextVS.getMessage("capsLockKeyPressed"));
        capsLockPressedMessageText.setWrappingWidth(320);
        capsLockPressedMessageText.setStyle("-fx-font-weight: bold; -fx-fill: #6c0404;");
        VBox.setMargin(messageText, new Insets(0, 0, 15, 0));
        capsLockPressedMessageText.setTextAlignment(TextAlignment.CENTER);

        password1Field = new PasswordField();
        password2Field = new PasswordField();

        Button cancelButton = new Button(ContextVS.getMessage("closeLbl"));
        cancelButton.setOnAction(actionEvent -> setPasswordDialogVisible(false));
        cancelButton.setGraphic(Utils.getIcon(FontAwesomeIcon.TIMES, Utils.COLOR_RED_DARK));

        final Button acceptButton = new Button(ContextVS.getMessage("acceptLbl"));
        acceptButton.setOnAction(actionEvent -> initBackgroundTask());
        acceptButton.setGraphic(Utils.getIcon(FontAwesomeIcon.CHECK));

        password1Field.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if ((event.getCode() == KeyCode.ENTER)) {
                    acceptButton.fire();
                }
                setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                        java.awt.event.KeyEvent.VK_CAPS_LOCK));
            }
        );

        password1Field.addEventHandler(KeyEvent.KEY_PRESSED,
            new EventHandler<KeyEvent>() {
                public void handle(KeyEvent event) {
                    if ((event.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                            java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );

        password2Field.addEventHandler(KeyEvent.KEY_PRESSED,
            new EventHandler<KeyEvent>() {
                public void handle(KeyEvent event) {
                    if ((event.getCode() == KeyCode.ENTER)) {
                        acceptButton.fire();
                    }
                    setCapsLockState(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                            java.awt.event.KeyEvent.VK_CAPS_LOCK));
                }
            }
        );
        HBox footerButtonsBox = new HBox();
        footerButtonsBox.getChildren().addAll(acceptButton, Utils.getSpacer(), cancelButton);
        VBox.setMargin(footerButtonsBox, new Insets(20, 20, 10, 20));

        Text password1Text = new Text(ContextVS.getMessage("password1Lbl"));
        Text password2Text = new Text(ContextVS.getMessage("password2Lbl"));
        password2Text.setStyle("-fx-spacing: 50;");
        passwordVBox.getChildren().addAll(messageText, password1Text, password1Field, password2Text, password2Field,
                footerButtonsBox);
        passwordVBox.getStylesheets().add(Utils.getResource("/css/documentSignerPasswordDialog.css"));
        passwordVBox.getStyleClass().add("modal-dialog");
        passwordVBox.setStyle("-fx-background-color: #f9f9f9; -fx-max-height:280px;-fx-max-width:350px;");
        passwordVBox.autosize();
        setPasswordDialogVisible(false);
        getChildren().addAll(progressRegion, progressBox, passwordRegion, passwordVBox);

        Task<ResponseVS> operationHandlerTask = new OperationHandlerTask();
        progressMessageText.textProperty().bind(operationHandlerTask.messageProperty());
        progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
        progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
        progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
    }

    public void processOperation(Operation operation, String toUser, String textToSign, String subject,
             SMIMEMessage smimeMessage, String serviceURL) {
        this.operation = operation;
        this.smimeMessage = smimeMessage;
        this.toUser = toUser;
        this.messageSubject = subject;
        this.serviceURL = serviceURL;
        this.textToSign = textToSign;
        if(operation == Operation.SEND_SMIME) initBackgroundTask();
        else PlatformImpl.runAndWait(new Runnable() {
                @Override public void run() {
                    setPasswordDialogVisible(true);
                }
            });
    }

    private void initBackgroundTask() {
        log.info("processOperation");
        Task<ResponseVS> operationHandlerTask = new OperationHandlerTask();
        progressMessageText.textProperty().bind(operationHandlerTask.messageProperty());
        progressBar.progressProperty().bind(operationHandlerTask.progressProperty());
        progressRegion.visibleProperty().bind(operationHandlerTask.runningProperty());
        progressBox.visibleProperty().bind(operationHandlerTask.runningProperty());
        new Thread(operationHandlerTask).start();
    }

    public void setPasswordDialogVisible(boolean isVisible) {
        setMessage(ContextVS.getMessage("passwordMissing"));
        passwordVBox.setVisible(isVisible);
        passwordRegion.setVisible(isVisible);
    }

    private void setCapsLockState (boolean pressed) {
        this.isCapsLockPressed = pressed;
        setMessage(ContextVS.getMessage("passwordMissing"));
    }

    public char[] getPassword() {
        return password;
    }

    private void checkPasswords() {
        log.info("checkPasswords");
        PlatformImpl.runLater(new Runnable(){
            @Override public void run() {
                String password1 = new String(password1Field.getText());
                String password2 = new String(password2Field.getText());
                if(password1.trim().isEmpty() && password2.trim().isEmpty()) setMessage(ContextVS.getMessage("passwordMissing"));
                else {
                    if (password1.equals(password2)) {
                        password = password1.toCharArray();
                        setPasswordDialogVisible(false);
                        initBackgroundTask();
                    } else {
                        setMessage(ContextVS.getMessage("passwordError"));
                    }
                    password1Field.setText("");
                    password2Field.setText("");
                }
            }
        });
    }

    private void setMessage (String message) {
        if (message == null) messageText.setText(mainMessage);
        else messageText.setText(message);
        if(isCapsLockPressed) {
            if(!passwordVBox.getChildren().contains(capsLockPressedMessageText))
                passwordVBox.getChildren().add(0, capsLockPressedMessageText);
        }
        else passwordVBox.getChildren().removeAll(capsLockPressedMessageText);
        passwordVBox.autosize();
    }

    public class OperationHandlerTask extends Task<ResponseVS> {

        OperationHandlerTask() { }

        @Override protected ResponseVS call() throws Exception {
            ResponseVS responseVS = null;
            switch(operation) {
                case SEND_SMIME:
                    try {
                        updateMessage(ContextVS.getMessage("sendingDocumentMsg"));
                        updateProgress(20, 100);
                        responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                                serviceURL);
                        updateProgress(80, 100);
                    } catch(Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
                case SIGN_SMIME:
                    try {
                        Map<String, Object> textToSignMap = null;
                        try {
                            textToSignMap = JSON.getMapper().readValue(
                                    textToSign.replaceAll("(\\r|\\n)", "\\\\n"), new TypeReference<HashMap<String, Object>>() {});
                        } catch (Exception ex) {
                            textToSignMap = new HashMap<>();
                            textToSignMap.put("message", textToSign);
                        }
                        textToSignMap.put("UUID", UUID.randomUUID().toString());
                        toUser = StringUtils.getNormalized(toUser);
                        String timeStampService = ActorVS.getTimeStampServiceURL(ContextVS.getMessage("defaultTimeStampServer"));
                        log.info("toUser: " + toUser + " - timeStampService: " + timeStampService);
                        smimeMessage = BrowserSessionService.getSMIME(null, toUser,
                                textToSignMap.toString(), password, messageSubject);
                        updateMessage(ContextVS.getMessage("gettingTimeStampMsg"));
                        updateProgress(40, 100);
                        MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampService);
                        smimeMessage = timeStamper.call();
                        responseVS = ResponseVS.OK(null).setSMIME(smimeMessage);
                    } catch(Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage() + " - " + textToSign.replaceAll("(\\r|\\n)", "\\\\n"), ex);
                        responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                    break;
            }
            if(operationListener != null) operationListener.processResult(operation, responseVS);
            return responseVS;
        }

    }
}
