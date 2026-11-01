package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.UserValidator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** Users screen (admins): create, edit and delete accounts, change roles, jump to a user's posts. */
public class UserController extends BaseController {

    private static final String PASSWORD_RULE = "At least 6 characters, with a letter, a number and a symbol (e.g. ! @ # $ -).";

    @FXML private Label countLabel;
    @FXML private Button newButton;
    @FXML private ComboBox<FilterOption<Role>> roleFilter;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, Role> roleColumn;
    @FXML private TableColumn<User, String> createdColumn;
    @FXML private TableColumn<User, User> actionsColumn;

    @FXML private VBox form;
    @FXML private Label formMessage;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHint;
    @FXML private ComboBox<Role> roleBox;
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label roleError;

    private List<User> loaded = List.of();
    private boolean creating;   // the password is required for a new user, optional when editing

    public UserController(AppContext app) {
        super(app);
    }

    @FXML
    private void initialize() {
        column(idColumn, User::getId);
        column(usernameColumn, User::getUsername);
        column(emailColumn, User::getEmail);
        column(roleColumn, User::getRole);
        column(createdColumn, user -> format(user.getCreatedAt()));
        RowActions.install(table, actionsColumn, this::edit,
                RowActions.view("View this user's posts", (User user) -> navigator.showPostsOf(user.getId()))
                        .onlyWhen(user -> user.getRole().canWritePosts()),
                RowActions.edit(this::edit),
                RowActions.delete(this::delete).onlyWhen(user -> user.getId() != me().getId()));
        newButton(newButton);
        roleFilter.getItems().add(FilterOption.all("All roles"));
        for (Role role : Role.values()) {
            roleFilter.getItems().add(FilterOption.of(role));
        }
        roleFilter.getSelectionModel().selectFirst();
        roleBox.getItems().setAll(Role.values());
        errors.bindSummary(formMessage);
        errors.bind("username", usernameField, usernameError);
        errors.bind("email", emailField, emailError);
        errors.bind("password", passwordField, passwordError);
        errors.bind("role", roleBox, roleError);
        errors.live("username", usernameField, () -> UserValidator.username(usernameField.getText()));
        errors.live("email", emailField, () -> UserValidator.email(emailField.getText()));
        errors.live("password", passwordField, () -> creating ? UserValidator.password(passwordField.getText())
                : UserValidator.newPassword(passwordField.getText()));
        FormErrors.limit(usernameField, UserValidator.USERNAME_MAX);
        FormErrors.limit(emailField, UserValidator.EMAIL_MAX);
        FormErrors.limit(passwordField, UserValidator.PASSWORD_MAX);
    }

    @Override
    public void refresh() {
        load(() -> loaded = app.users().findAll());
        applyQuery();
    }

    @Override
    public String searchPrompt() {
        return "Search users by name, email or role";
    }

    @Override
    protected void applyQuery() {
        Role role = FilterOption.selected(roleFilter);
        List<User> shown = loaded.stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> matches(query(), user.getUsername(), user.getEmail(), user.getRole()))
                .toList();
        table.getItems().setAll(shown);
        long admins = loaded.stream().filter(user -> user.getRole() == Role.ADMIN).count();
        long authors = loaded.stream().filter(user -> user.getRole() == Role.AUTHOR).count();
        countLabel.setText(countText(shown.size(), loaded.size(), "user", "users")
                + " · %d admins · %d authors · %d readers".formatted(admins, authors, loaded.size() - admins - authors));
    }

    @Override
    protected Node root() {
        return table;
    }

    @Override
    public void openNewForm() {
        errors.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        creating = true;
        passwordHint.setText(PASSWORD_RULE);
        roleBox.setValue(Role.READER);
        openForm("New user", form, true, usernameField, () -> {
            boolean saved = save("User created.", () -> app.users().create(usernameField.getText(),
                    emailField.getText(), passwordField.getText(), roleBox.getValue())).isPresent();
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

    @FXML
    private void onFilterChanged() {
        applyQuery();
    }

    private void edit(User user) {
        errors.clear();
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        passwordField.clear();   // the stored hash is never shown
        creating = false;
        passwordHint.setText("Leave blank to keep the current password. " + PASSWORD_RULE);
        roleBox.setValue(user.getRole());
        openForm("Edit user #" + user.getId(), form, false, usernameField, () -> {
            boolean saved = save("User updated.", () -> app.users().update(user.getId(), usernameField.getText(),
                    emailField.getText(), passwordField.getText(), roleBox.getValue())).isPresent();
            if (saved) {
                refresh();
            }
            return saved;
        });
    }

    private void delete(User user) {
        if (confirmDelete("user " + user.getUsername(), "All posts, comments and reviews written by this user are deleted too.")
                && perform("User deleted.", () -> app.users().delete(user.getId()))) {
            refresh();
        }
    }
}
