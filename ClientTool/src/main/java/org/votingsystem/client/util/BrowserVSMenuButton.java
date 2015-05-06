package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.SignDocumentFormPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSMenuButton extends MenuButton {

    private static Logger log = Logger.getLogger(BrowserVSMenuButton.class.getSimpleName());

    private MenuItem voteMenuItem;
    private MenuItem selectRepresentativeMenuItem;
    private MenuItem currencyUsersProceduresMenuItem;
    private MenuItem walletMenuItem;
    private MenuItem votingSystemAdminMenuItem;
    private MenuItem currencyAdminMenuItem;


    public BrowserVSMenuButton () {
        setGraphic(Utils.getIcon(FontAwesomeIcons.BARS));
        MenuItem openFileMenuItem = new MenuItem(ContextVS.getMessage("openFileButtonLbl"));
        openFileMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.FOLDER_OPEN));
        openFileMenuItem.setOnAction(actionEvent -> {
            DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(null, null);
            Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
        });
        MenuItem signDocumentMenuItem = new MenuItem(ContextVS.getMessage("signDocumentButtonLbl"));
        signDocumentMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.CERTIFICATE));
        signDocumentMenuItem.setOnAction(actionEvent -> SignDocumentFormPane.showDialog());

        voteMenuItem = new MenuItem(ContextVS.getMessage("voteButtonLbl"));
        voteMenuItem.setVisible(false);
        voteMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.ENVELOPE));
        voteMenuItem.setOnAction(event -> {
            Browser.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                    ContextVS.getMessage("voteButtonLbl"));
        });
        selectRepresentativeMenuItem = new MenuItem(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeMenuItem.setVisible(false);
        selectRepresentativeMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.HAND_ALT_RIGHT));
        selectRepresentativeMenuItem.setOnAction(event -> {
            Browser.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                    ContextVS.getMessage("selectRepresentativeButtonLbl"));
        });

        currencyUsersProceduresMenuItem = new MenuItem(ContextVS.getMessage("financesLbl"));
        currencyUsersProceduresMenuItem.setVisible(false);
        currencyUsersProceduresMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.BAR_CHART));
        currencyUsersProceduresMenuItem.setOnAction(event -> {
            Browser.getInstance().openCurrencyURL(ContextVS.getInstance().getCurrencyServer().getUserDashBoardURL(),
                    ContextVS.getMessage("currencyUsersLbl"));
        });
        walletMenuItem = new MenuItem(ContextVS.getMessage("walletLbl"));
        walletMenuItem.setVisible(false);
        walletMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.MONEY));
        walletMenuItem.setOnAction(event -> WalletPane.showDialog(Browser.getInstance().getScene().getWindow()));

        MenuItem settingsMenuItem = new MenuItem(ContextVS.getMessage("settingsLbl"));
        settingsMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.COG));
        settingsMenuItem.setOnAction(actionEvent -> SettingsDialog.showDialog());
        currencyAdminMenuItem = new MenuItem(ContextVS.getMessage("currencyAdminLbl"));
        currencyAdminMenuItem.setOnAction(actionEvent -> Browser.getInstance().openCurrencyURL(
                ContextVS.getInstance().getCurrencyServer().getAdminDashBoardURL(),
                ContextVS.getMessage("currencyAdminLbl")));
        currencyAdminMenuItem.setVisible(false);
        votingSystemAdminMenuItem = new MenuItem(ContextVS.getMessage("votingSystemProceduresLbl"));
        votingSystemAdminMenuItem.setOnAction(actionEvent -> Browser.getInstance().openVotingSystemURL(
                ContextVS.getInstance().getAccessControl().getDashBoardURL(),
                ContextVS.getMessage("votingSystemProceduresLbl")));
        votingSystemAdminMenuItem.setVisible(false);

        Menu adminsMenu = new Menu(ContextVS.getMessage("adminsMenuLbl"));
        adminsMenu.setGraphic(Utils.getIcon(FontAwesomeIcons.USERS));
        adminsMenu.getItems().addAll(currencyAdminMenuItem, votingSystemAdminMenuItem);

        getItems().addAll(voteMenuItem, selectRepresentativeMenuItem, new SeparatorMenuItem(),
                walletMenuItem, currencyUsersProceduresMenuItem, new SeparatorMenuItem(),
                signDocumentMenuItem, openFileMenuItem, new SeparatorMenuItem(),
                settingsMenuItem, new SeparatorMenuItem(), adminsMenu);
    }

    public void setVotingSystemAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            voteMenuItem.setVisible(available);
            selectRepresentativeMenuItem.setVisible(available);
            votingSystemAdminMenuItem.setVisible(available);
        });
    }

    public void setCurrencyServerAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            currencyUsersProceduresMenuItem.setVisible(available);
            walletMenuItem.setVisible(available);
            currencyAdminMenuItem.setVisible(available);
        });
    }

}
