package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.validations.TagValidator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** Tags screen (admins): create, rename and delete tags. */
public class TagController extends BaseController {

    @FXML private Label countLabel;
    @FXML private Button newButton;
    @FXML private TableView<Tag> table;
    @FXML private TableColumn<Tag, Integer> idColumn;
    @FXML private TableColumn<Tag, String> nameColumn;
    @FXML private TableColumn<Tag, Tag> actionsColumn;

    @FXML private VBox form;
    @FXML private Label formMessage;
    @FXML private TextField nameField;
    @FXML private Label nameError;

    private List<Tag> loaded = List.of();

    public TagController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(idColumn, Tag::getId);
        column(nameColumn, Tag::getName);
        RowActions.install(table, actionsColumn, this::edit, RowActions.edit(this::edit), RowActions.delete(this::delete));
        newButton(newButton);
        errors.bindSummary(formMessage);
        errors.bind("name", nameField, nameError);
        errors.live("name", nameField, () -> TagValidator.name(TagValidator.slug(nameField.getText())));
        FormErrors.limit(nameField, TagValidator.NAME_MAX);
    }

    @Override
    public void refresh() {
        load(() -> loaded = app.tags().findAll());
        applyQuery();
    }

    @Override
    public String searchPrompt() {
        return "Search tags";
    }

    @Override
    protected void applyQuery() {
        List<Tag> shown = loaded.stream().filter(tag -> matches(query(), tag.getName())).toList();
        table.getItems().setAll(shown);
        countLabel.setText(countText(shown.size(), loaded.size(), "tag", "tags"));
    }

    @Override
    protected Node root() {
        return table;
    }

    @Override
    public void openNewForm() {
        errors.clear();
        nameField.clear();
        openForm("New tag", form, true, nameField, () -> {
            boolean saved = save("Tag created.", () -> app.tags().create(nameField.getText())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    @FXML
    private void onNew() {
        openNewForm();
    }

    private void edit(Tag tag) {
        errors.clear();
        nameField.setText(tag.getName());
        openForm("Rename tag #" + tag.getId(), form, false, nameField, () -> {
            boolean saved = save("Tag renamed.", () -> app.tags().rename(tag.getId(), nameField.getText())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    private void delete(Tag tag) {
        if (confirmDelete("tag #" + tag.getName(), "It is removed from every post that uses it; the posts stay.")
                && perform("Tag deleted.", () -> app.tags().delete(tag.getId()))) {
            refresh();
        }
    }
}
