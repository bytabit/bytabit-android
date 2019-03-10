/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        okButton.setOnAction(evt -> dialog.hide());
        dialog.getButtons().add(okButton);
        dialog.showAndWait();
    }

}
