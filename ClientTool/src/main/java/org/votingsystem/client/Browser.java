package org.votingsystem.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dto.SignalVSDto;
import org.votingsystem.client.pane.BrowserVSPane;
import org.votingsystem.client.pane.BrowserVSTabPane;
import org.votingsystem.client.pane.BrowserVSToolbar;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.util.*;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Browser extends VBox implements BrowserVS, PasswordDialog.Listener  {

    private static Logger log = java.util.logging.Logger.getLogger(Browser.class.getSimpleName());

    private Stage browserStage;
    private FullScreenHelper fullScreenHelper;
    private BrowserVSToolbar toolBar;
    private BrowserVSTabPane tabPaneVS;
    private BrowserVSPane browserHelper;
    private Map<String, WebView> webViewMap = new HashMap<>();
    private static Browser INSTANCE;

    public static Browser init(Stage browserStage) {
        INSTANCE = new Browser(browserStage);
        return INSTANCE;
    }

    public static Browser getInstance() {
        if(INSTANCE == null) INSTANCE = new Browser(new Stage());
        return INSTANCE;
    }

    public void show() {
        browserStage.show();
        browserStage.toFront();
    }

    public void minimize() {
        log.info("minimize");
        browserStage.setIconified(true);
        browserStage.toBack();
    }

    private Browser(Stage browserStage) {
        getStylesheets().add(Utils.getResource("/css/browservs.css"));
        getStyleClass().add("main-dialog");
        this.browserStage = browserStage;
        fullScreenHelper = new FullScreenHelper(this.browserStage);
        browserHelper = new BrowserVSPane();
        toolBar = new BrowserVSToolbar(this.browserStage);
        tabPaneVS = new BrowserVSTabPane(toolBar);
        Platform.setImplicitExit(false);
        browserHelper.getSignatureService().setOnSucceeded(event -> {
            log.info("signatureService - OnSucceeded");
            try {
                ResponseVS responseVS = browserHelper.getSignatureService().getValue();
                if(ResponseVS.SC_INITIALIZED == responseVS.getStatusCode()) {
                    log.info("signatureService - OnSucceeded - ResponseVS.SC_INITIALIZED");
                } else {
                    if(browserHelper.getSignatureService().getOperationVS().getCallerCallback() == null) {
                        showOperationResult(responseVS);
                        return;
                    }
                    if(ContentTypeVS.JSON == responseVS.getContentType()) {
                        invokeOperationCallback(responseVS.getMessage(),
                                browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                    } else invokeOperationCallback(new MessageDto(responseVS.getStatusCode(), responseVS.getMessage()),
                            browserHelper.getSignatureService().getOperationVS().getCallerCallback());
                }
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
        });
        browserStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        browserStage.setResizable(true);
        browserStage.setOnCloseRequest(event -> {
            event.consume();
            browserStage.hide();
            browserHelper.getSignatureService().cancel();
            log.info("browserStage.setOnCloseRequest");
        });
        browserHelper.getChildren().add(0, this);
        browserStage.setScene(new Scene(browserHelper));
        browserStage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        browserStage.initStyle(StageStyle.TRANSPARENT);
        //browserStage.getScene().setFill(Color.TRANSPARENT);//-> problems resizing
        ResizeHelper.addResizeListener(browserStage);
        getChildren().addAll(toolBar, tabPaneVS);
    }

    public WebView newTab(String URL, String tabCaption, String jsCommand) {
        return tabPaneVS.newTab(URL, tabCaption, jsCommand);
    }

    private void showOperationResult(ResponseVS responseVS) throws IOException {
        showMessage(responseVS.getStatusCode(), responseVS.getMessage());
    }

    @Override public void invokeOperationCallback(Object dto, String callerCallback) throws JsonProcessingException {
        invokeOperationCallback(JSON.getMapper().writeValueAsString(dto), callerCallback);
    }

    public void invokeOperationCallback(String jsonStr, String callerCallback) throws JsonProcessingException {
        log.info("invokeOperationCallback - jsonStr: " + MsgUtils.truncateLog(jsonStr));
        try {
            WebView operationWebView = webViewMap.remove(callerCallback);
            final String jsCommand = "setClientToolMessage('" + callerCallback + "','" +
                    Base64.getEncoder().encodeToString(jsonStr.getBytes("UTF8")) + "')";
            PlatformImpl.runLater(() -> {  operationWebView.getEngine().executeScript(jsCommand); });
        } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
    }

    @Override public void processOperationVS(OperationVS operationVS, String passwordDialogMessage) {
        browserHelper.processOperationVS(operationVS, passwordDialogMessage);
    }

    @Override public void processOperationVS(char[] password, OperationVS operationVS) {
        browserHelper.processOperationVS(password, operationVS);
    }

    @Override public void processSignalVS(SignalVSDto signal) {//{title:, url:}
        log.info("processSignalVS - caption: " + signal.getCaption());
        tabPaneVS.getSelectionModel().getSelectedItem().setText(signal.getCaption());
    }

    public void toggleFullScreen() {
        fullScreenHelper.toggleFullScreen();
    }

    public void openCurrencyURL(final String URL, final String caption) {
        log.info("openCurrencyURL: " + URL);
        if(ContextVS.getInstance().getCurrencyServer() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
        } else Platform.runLater(() -> tabPaneVS.newTab(URL, caption, null));
    }

    public void openVotingSystemURL(final String URL, final String caption) {
        log.info("openVotingSystemURL: " + URL);
        if(ContextVS.getInstance().getAccessControl() == null) {
            showMessage(ContextVS.getMessage("connectionErrorMsg"), ContextVS.getMessage("errorLbl"));
        } else Platform.runLater(() -> tabPaneVS.newTab(URL, caption, null));
    }

    public static void showMessage(ResponseVS responseVS) {
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) showMessage(responseVS.getStatusCode(), responseVS.getMessage());
        else showMessage(responseVS.getStatusCode(), responseVS.getMessage());
    }

    public static void showMessage(Integer statusCode, String message) {
        PlatformImpl.runLater(() ->  new MessageDialog(getInstance().getScene().getWindow()).showMessage(statusCode, message));
    }

    public static void showMessage(final String message, final Button optionButton) {
        PlatformImpl.runLater(() -> new MessageDialog(getInstance().getScene().getWindow()).showHtmlMessage(message, optionButton));
    }

    public static void showMessage(final String message, final Parent parent, Window parentWindow) {
        PlatformImpl.runLater(() -> new MessageDialog(parentWindow).showHtmlMessage(message, parent));
    }

    public static void showMessage(final String message, final String caption) {
        PlatformImpl.runLater(() -> new MessageDialog(getInstance().getScene().getWindow()).showHtmlMessage(message, caption));
    }


    public void newTab(final Pane tabContent, final String caption){
        PlatformImpl.runLater(() -> {
            tabPaneVS.newTab(tabContent, caption);
            show();
        });
    }

    public void runJSCommand(String jsCommand) {
        PlatformImpl.runLater(() -> {
            for(WebView webView : webViewMap.values()) {
                webView.getEngine().executeScript(jsCommand);
            }
        });
    }

    public void setCurrencyServerAvailable(boolean available) {
        toolBar.setCurrencyServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        toolBar.setVotingSystemAvailable(available);
    }

    public void runJSCommandCurrentView(String jsCommand) {
        if(tabPaneVS.getSelectionModel().getSelectedItem() != null) {
            PlatformImpl.runLater(() -> {
                Object currentContent = tabPaneVS.getSelectionModel().getSelectedItem().getContent();
                if (currentContent instanceof WebView) {
                    ((WebView) currentContent).getEngine().executeScript(jsCommand);
                } else log.log(Level.SEVERE, "current content is not instance of WebView: " + currentContent.getClass());
            });
        } else {
            log.info("Browser has no active windows");
        }
    }

    public void registerCallerCallbackView(String callerCallback, WebView webView) {
        webViewMap.put(callerCallback, webView);
    }

    public void fireCoreSignal(String name, Object data, boolean fireToAllTabs) {
        //this.fire('iron-signal', {name: "vs-session-data", data: sessionDataJSON});
        try {
            Map coreSignal = new HashMap<>();
            coreSignal.put("name", name);
            coreSignal.put("data", data);
            String jsCommand = "fireCoreSignal('" + Base64.getEncoder().encodeToString(
                    JSON.getMapper().writeValueAsString(coreSignal).getBytes("UTF-8")) + "')";
            if(fireToAllTabs) runJSCommand(jsCommand);
            else runJSCommandCurrentView(jsCommand);
        } catch (UnsupportedEncodingException | JsonProcessingException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void saveWallet() {
        PasswordDialog.showWithoutPasswordConfirm(TypeVS.WALLET_SAVE, this, ContextVS.getMessage("walletPinMsg"));
    }

    @Override public void setPassword(TypeVS passwordType, char[] password) {
        switch (passwordType) {
            case WALLET_SAVE:
                if(password != null) {
                    try {
                        Wallet.getWallet(password);
                        Browser.getInstance().fireCoreSignal("vs-wallet-save", null, false);
                        InboxService.getInstance().removeMessagesByType(TypeVS.CURRENCY_IMPORT);
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }
                break;
        }
    }

    public static Window getWindow() {

        return getInstance().getScene().getWindow();
    }
}