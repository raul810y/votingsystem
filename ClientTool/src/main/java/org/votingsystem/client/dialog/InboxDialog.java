package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.apache.log4j.Logger;
import org.votingsystem.client.pane.InboxMessageRow;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.WebSocketMessage;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxDialog extends DialogVS implements InboxMessageRow.Listener {

    private static Logger log = Logger.getLogger(InboxDialog.class);

    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainPane;
    @FXML private Label message;
    @FXML private ProgressBar progressBar;
    @FXML private Label closeAdviceMsg;
    @FXML private VBox messageListPanel;

    private static final Map<WebSocketMessage, HBox> messageMap = new HashMap<WebSocketMessage, HBox>();

    public InboxDialog(PrivateKey privateKey) throws IOException {
        super("/fxml/Inbox.fxml", StageStyle.DECORATED);
        getStage().setTitle(ContextVS.getMessage("messageVSInboxCaption"));
        Task<ObservableList<WebSocketMessage>> task = new DecryptMessageTask(privateKey);
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.visibleProperty().bind(task.runningProperty());
        task.setOnSucceeded(event -> mainPane.getChildren().remove(progressBar));
        NotificationService.getInstance().registerToEventBus(new EventBusMessageListener());
        new Thread(task).start();
    }

    private void refreshView() {
        PlatformImpl.runLater(() ->  {
            messageListPanel.getChildren().clear();
            if(messageMap.size() > 0) {
                messageListPanel.getChildren().addAll(messageMap.values());
            } else {
                hide();
            }
            message.setText(ContextVS.getMessage("inboxNumMessagesMsg", messageMap.size()));
            scrollPane.getScene().getWindow().sizeToScene();
        });
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
    }

    public static void show(PrivateKey privateKey) {
        PlatformImpl.runLater(() -> {
            try {
                InboxDialog dialog = new InboxDialog(privateKey);
                dialog.show();
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        });
    }

    @Override public void removeMessage(WebSocketMessage webSocketMessage) {
        log.debug("onMessageButtonClick - operation: " + webSocketMessage.getOperation());
        messageMap.remove(webSocketMessage);
        InboxService.getInstance().removeMessage(webSocketMessage);
        refreshView();
    }

    class EventBusMessageListener {
        @Subscribe public void notificationChanged(WebSocketMessage webSocketMessage) {
            log.debug("EventBusMessageListener - notification: " + webSocketMessage.getOperation() +
                    " - state: " + webSocketMessage.getState());
            switch (webSocketMessage.getState()) {
                case PROCESSED:
                    messageMap.remove(webSocketMessage);
                    refreshView();
                    break;
            }
        }
    }

    public class DecryptMessageTask extends Task<ObservableList<WebSocketMessage>> {

        PrivateKey privateKey;
        public DecryptMessageTask(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

        @Override protected ObservableList<WebSocketMessage> call() throws Exception {
            List<WebSocketMessage> messageList = InboxService.getInstance().getMessageList();
            try {
                int i = 0;
                for(WebSocketMessage webSocketMessage : messageList) {
                    updateProgress(i++, messageList.size());
                    webSocketMessage.decryptMessage(privateKey);
                    messageMap.put(webSocketMessage, new InboxMessageRow(webSocketMessage, InboxDialog.this).getMainPane());
                }
                refreshView();
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
            return null;
        }
    }

}
