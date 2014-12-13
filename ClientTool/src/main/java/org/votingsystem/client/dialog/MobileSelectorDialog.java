package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.NifUtils;

import java.io.IOException;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MobileSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(JSONObject deviceDataJSON);
    }

    private static Logger log = Logger.getLogger(MobileSelectorDialog.class);

    @FXML private VBox mainPane;
    @FXML private Label captionLbl;
    @FXML private Button acceptButton;
    @FXML private Button cancelButton;
    @FXML private Button searchDeviceButton;
    @FXML private Label messageLbl;
    @FXML private TextField nifTextField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox deviceListBox;
    @FXML private HBox footerBox;
    private Listener listener;
    private ToggleGroup deviceToggleGroup;

    private MobileSelectorDialog(String caption, String message, Listener listener) throws IOException {
        super("/fxml/MobileSelectorDialog.fxml");
        this.listener = listener;
        captionLbl.setText(caption);
        messageLbl.setText(message);
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.getChildren().removeAll(progressBar);

        acceptButton.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        cancelButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        cancelButton.setText(ContextVS.getMessage("cancelLbl"));
        nifTextField.setPromptText(ContextVS.getMessage("nifLbl"));
        searchDeviceButton.setText(ContextVS.getMessage("searchDevicesLbl"));
        searchDeviceButton.setGraphic(Utils.getImage(FontAwesome.Glyph.SEARCH));
        footerBox.getChildren().remove(acceptButton);
    }

    public void searchButton(ActionEvent actionEvent) {
        try {
            final String nif = NifUtils.validate(nifTextField.getText());
            if(!mainPane.getChildren().contains(progressBar)) mainPane.getChildren().add(1, progressBar);
            mainPane.getScene().getWindow().sizeToScene();
            new Thread(new Runnable() {
                @Override public void run() {
                    ResponseVS responseVS = HttpHelper.getInstance().getData(
                            ContextVS.getInstance().getCooinServer().getDeviceListByNifServiceURL(nif), ContentTypeVS.JSON);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        updateDeviceList((JSONArray) ((JSONObject)responseVS.getMessageJSON()).getJSONArray("deviceList"));
                    }
                }
            }).start();
        } catch(Exception ex) {
            showMessage(null, ex.getMessage());
        }

    }

    private void updateDeviceList(JSONArray deviceArray) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if(mainPane.getChildren().contains(progressBar)) mainPane.getChildren().remove(progressBar);
                if(!deviceListBox.getChildren().isEmpty()) deviceListBox.getChildren().removeAll(
                        deviceListBox.getChildren());
                deviceToggleGroup = new ToggleGroup();
                deviceToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                    public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                        if (deviceToggleGroup.getSelectedToggle() != null) {
                            if(!footerBox.getChildren().contains(acceptButton))
                                footerBox.getChildren().add(2, acceptButton);
                        } else footerBox.getChildren().remove(acceptButton);
                    }
                });
                for(int i = 0; i < deviceArray.size() ; i++) {
                    JSONObject deviceData = (JSONObject) deviceArray.get(i);
                    RadioButton radioButton = new RadioButton(deviceData.getString("deviceName"));
                    radioButton.setUserData(deviceData);
                    radioButton.setToggleGroup(deviceToggleGroup);
                    deviceListBox.getChildren().add(radioButton);
                }
                mainPane.getScene().getWindow().sizeToScene();
            }
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((JSONObject) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
    }

    public void cancelButton(ActionEvent actionEvent) {
        hide();
    }

    public void onEnterNifTextField(ActionEvent actionEvent) {
        searchButton(actionEvent);
    }

    public static void show(String caption, String message, Listener listener) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    MobileSelectorDialog dialog = new MobileSelectorDialog(caption, message, listener);
                    dialog.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

}