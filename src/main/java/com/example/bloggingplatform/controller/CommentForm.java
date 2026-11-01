package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.validations.CommentValidator;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/** The text area, error label and banner of the "write / edit a comment" pop-ups in the reading view. */
final class CommentForm {
    final FormErrors errors = new FormErrors();
    final TextArea text;
    final VBox root;

    CommentForm(String content) {
        Label banner = BlogWidgets.formMessage();
        text = new TextArea(content);
        text.setPromptText("Write a comment…");
        text.setWrapText(true);
        text.setPrefRowCount(6);
        Label error = BlogWidgets.fieldError();
        root = new VBox(banner, BlogWidgets.fieldLabel("Comment (2-2000 characters)"), text, error);
        root.getStyleClass().add("form");
        root.setPrefWidth(480);
        errors.bindSummary(banner);
        errors.bind("content", text, error);
        errors.live("content", text, () -> CommentValidator.comment(text.getText()));
    }
}
