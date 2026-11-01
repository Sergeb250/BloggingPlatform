package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.util.Icons;
import com.example.bloggingplatform.util.PopupWindow;
import com.example.bloggingplatform.util.Toast;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Message, error and delete-confirmation pop-ups in the application's style, plus the toast
 * used for success messages.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Window owner, String header, String message) {
        PopupWindow popup = message(owner, "Information", Icons.INFO, header, message);
        Button ok = popup.addButton("OK", null, popup::close);
        ok.setDefaultButton(true);
        popup.showAndWait(ok);
    }

    public static void error(Window owner, String header, String message) {
        PopupWindow popup = message(owner, "Error", Icons.WARNING, header, message, true);
        Button ok = popup.addButton("OK", null, popup::close);
        ok.setDefaultButton(true);
        popup.showAndWait(ok);
    }

    /** Asks before deleting. "Cancel" has the focus, so pressing Enter never deletes. */
    public static boolean confirmDelete(Window owner, String header, String message) {
        PopupWindow popup = message(owner, "Confirm delete", Icons.DELETE, header, message);
        boolean[] confirmed = {false};
        Button cancel = popup.addButton("Cancel", null, popup::close);
        cancel.setDefaultButton(true);
        popup.addButton("Delete", "btn-delete", () -> {
            confirmed[0] = true;
            popup.close();
        });
        popup.showAndWait(cancel);
        return confirmed[0];
    }

    /** A success message that disappears by itself after a few seconds. */
    public static void toast(Window owner, String message) {
        Toast.show(owner, message);
    }

    private static PopupWindow message(Window owner, String title, String icon, String header, String message) {
        return message(owner, title, icon, header, message, false);
    }

    /** @param error true for a red heading */
    private static PopupWindow message(Window owner, String title, String icon, String header, String message,
                                       boolean error) {
        Label heading = new Label(header);
        heading.getStyleClass().add("popup-header");
        if (error) {
            heading.getStyleClass().add("popup-header-error");
        }
        heading.setWrapText(true);
        Label text = new Label(message);
        text.getStyleClass().add("popup-message");
        text.setWrapText(true);
        text.setMinHeight(Region.USE_PREF_SIZE);   // never cut long messages
        VBox texts = new VBox(6, heading, text);
        texts.setPrefWidth(380);
        HBox content = new HBox(14, Icons.of(icon, 30), texts);
        content.getStyleClass().add("popup-message-box");

        PopupWindow popup = new PopupWindow(owner, title);
        popup.setContent(content);
        return popup;
    }
}
