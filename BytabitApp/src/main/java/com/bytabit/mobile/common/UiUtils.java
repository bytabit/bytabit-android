package com.bytabit.mobile.common;

import com.gluonhq.charm.glisten.control.Dialog;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiUtils {

    public static void showErrorDialog(Throwable throwable) {
        log.warn(throwable.getMessage());
        Dialog<Label> dialog = new Dialog<>();
        dialog.setTitle(new Label(throwable.getMessage()));
        dialog.setContent(new Label());
        Button okButton = new Button("OK");
        okButton.setOnAction(evt -> {
            dialog.hide();
        });
        dialog.getButtons().add(okButton);
        dialog.showAndWait();
    }

}
