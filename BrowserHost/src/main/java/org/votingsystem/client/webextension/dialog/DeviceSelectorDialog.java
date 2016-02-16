package org.votingsystem.client.webextension.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.NifUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DeviceSelectorDialog extends DialogVS {

    public interface Listener {
        public void setSelectedDevice(DeviceVSDto device);
        public void cancelSelection();
    }

    private static Logger log = Logger.getLogger(DeviceSelectorDialog.class.getName());

    @FXML private VBox mainPane;
    @FXML private Button acceptButton;
    @FXML private Button searchDeviceButton;
    @FXML private Label messageLbl;
    @FXML private TextField nifTextField;
    @FXML private ProgressBar progressBar;
    @FXML private VBox deviceListBox;
    @FXML private HBox footerBox;
    private Listener listener;
    private ToggleGroup deviceToggleGroup;

    private DeviceSelectorDialog(String caption, String message, Listener listener) throws IOException {
        super("/fxml/DeviceSelectorDialog.fxml", caption);
        this.listener = listener;
        messageLbl.setText(message);
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        mainPane.getChildren().removeAll(progressBar);
        acceptButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CHECK));
        acceptButton.setText(ContextVS.getMessage("acceptLbl"));
        nifTextField.setPromptText(ContextVS.getMessage("nifLbl"));
        searchDeviceButton.setText(ContextVS.getMessage("searchDevicesLbl"));
        searchDeviceButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.SEARCH));
        footerBox.getChildren().remove(acceptButton);
        addCloseListener(event -> {
            if(deviceToggleGroup == null || deviceToggleGroup.getSelectedToggle() == null)
                    listener.cancelSelection();
        });
    }

    public void searchButton(ActionEvent actionEvent) {
        try {
            final String nif = NifUtils.validate(nifTextField.getText());
            if(!mainPane.getChildren().contains(progressBar)) mainPane.getChildren().add(1, progressBar);
            mainPane.getScene().getWindow().sizeToScene();
            new Thread(() -> {
                try {
                    ResultListDto<DeviceVSDto> resultListDto = HttpHelper.getInstance().getData(
                            new TypeReference<ResultListDto<DeviceVSDto>>(){},
                            ContextVS.getInstance().getCurrencyServer().getDeviceListByNifServiceURL(nif), MediaTypeVS.JSON);
                    updateDeviceList(resultListDto.getResultList());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }

            }).start();
        } catch(Exception ex) {
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }

    }

    private void updateDeviceList(Collection<DeviceVSDto> deviceList) {
        PlatformImpl.runLater(() -> {
            if(mainPane.getChildren().contains(progressBar)) mainPane.getChildren().remove(progressBar);
            if(!deviceListBox.getChildren().isEmpty()) deviceListBox.getChildren().removeAll(
                    deviceListBox.getChildren());
            deviceToggleGroup = new ToggleGroup();
            deviceToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
                    if (deviceToggleGroup.getSelectedToggle() != null) {
                        if(!footerBox.getChildren().contains(acceptButton))
                            footerBox.getChildren().add(acceptButton);
                    } else footerBox.getChildren().remove(acceptButton);
                }
            });
            for(DeviceVSDto dto : deviceList) {
                try {
                    if(!BrowserSessionService.getInstance().getDevice().getDeviceId().equals(dto.getDeviceId())) {
                        RadioButton radioButton = new RadioButton(dto.getDeviceName());
                        radioButton.setUserData(dto);
                        radioButton.setToggleGroup(deviceToggleGroup);
                        deviceListBox.getChildren().add(radioButton);
                    }
                } catch (Exception ex) {
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
            }
            mainPane.getScene().getWindow().sizeToScene();
        });
    }

    public void acceptButton(ActionEvent actionEvent) {
        if(deviceToggleGroup != null && deviceToggleGroup.getSelectedToggle() != null)
            listener.setSelectedDevice((DeviceVSDto) deviceToggleGroup.getSelectedToggle().getUserData());
        hide();
    }

    public void onEnterNifTextField(ActionEvent actionEvent) {
        searchButton(actionEvent);
    }

    public static void show(String caption, String message, Listener listener) {
        Platform.runLater(() -> {
            try {
                DeviceSelectorDialog dialog = new DeviceSelectorDialog(caption, message, listener);
                dialog.show();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

}