package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.validations.UserValidator;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.util.Icons;
import com.example.bloggingplatform.util.NotebookArt;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sign-in page with a registration form. Both forms talk to {@code UserService}; hashing a
 * password takes a moment (that is what makes PBKDF2 safe), so the work runs on a background
 * thread while the button shows "Signing in…".
 */
public class LoginController {

    private static final System.Logger LOG = System.getLogger(LoginController.class.getName());

    @FXML private StackPane brandLogo;
    @FXML private Label readFeature;
    @FXML private Label writeFeature;
    @FXML private Label manageFeature;

    @FXML private VBox signInForm;
    @FXML private Label signInMessage;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label loginError;
    @FXML private Label passwordError;
    @FXML private Button signInButton;

    @FXML private VBox registerForm;
    @FXML private Label registerMessage;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmField;
    @FXML private HBox roleChoices;
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmError;
    @FXML private Label roleError;
    @FXML private Button registerButton;

    private final AppContext app;
    private final FormErrors signInErrors = new FormErrors();
    private final FormErrors registerErrors = new FormErrors();
    private final ToggleGroup roles = new ToggleGroup();
    private Runnable onSignedIn = () -> { };

    public LoginController(AppContext app) {
        this.app = app;
    }

    @FXML
    private void initialize() {
        brandLogo.getChildren().setAll(NotebookArt.logo(72));
        readFeature.setGraphic(Icons.of(Icons.DISCOVER, 18));
        writeFeature.setGraphic(Icons.of(Icons.WRITE, 18));
        manageFeature.setGraphic(Icons.of(Icons.DASHBOARD, 18));

        signInErrors.bindSummary(signInMessage);
        signInErrors.bind("login", loginField, loginError);
        signInErrors.bind("password", passwordField, passwordError);
        registerErrors.bindSummary(registerMessage);
        registerErrors.bind("username", usernameField, usernameError);
        registerErrors.bind("email", emailField, emailError);
        registerErrors.bind("password", newPasswordField, newPasswordError);
        registerErrors.bind("confirm", confirmField, confirmError);
        registerErrors.bind("role", roleChoices, roleError);

        // the same rules the service applies, checked as the user types
        signInErrors.live("login", loginField, () -> UserValidator.login(loginField.getText()));
        signInErrors.live("password", passwordField, () -> UserValidator.loginPassword(passwordField.getText()));
        registerErrors.live("username", usernameField, () -> UserValidator.username(usernameField.getText()));
        registerErrors.live("email", emailField, () -> UserValidator.email(emailField.getText()));
        registerErrors.live("password", newPasswordField, () -> UserValidator.password(newPasswordField.getText()));
        registerErrors.live("confirm", confirmField,
                () -> UserValidator.confirmPassword(newPasswordField.getText(), confirmField.getText()), newPasswordField);
        FormErrors.limit(loginField, UserValidator.EMAIL_MAX);
        FormErrors.limit(usernameField, UserValidator.USERNAME_MAX);
        FormErrors.limit(emailField, UserValidator.EMAIL_MAX);
        FormErrors.limit(newPasswordField, UserValidator.PASSWORD_MAX);
        FormErrors.limit(confirmField, UserValidator.PASSWORD_MAX);

        for (Role role : app.auth().registrableRoles()) {
            roleChoices.getChildren().add(roleChoice(role));
        }
        roles.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                roles.selectToggle(old);   // one account type is always chosen
            }
        });
        roles.selectToggle(roles.getToggles().getFirst());
        showForm(true);
    }

    /** Called with the signed-in user available from the session. */
    public void setOnSignedIn(Runnable onSignedIn) {
        this.onSignedIn = onSignedIn;
    }

    @FXML
    private void onSignIn() {
        String login = loginField.getText();
        String password = passwordField.getText();
        runInBackground(signInButton, "Signing in…", signInErrors,
                () -> app.auth().login(login, password));
    }

    @FXML
    private void onRegister() {
        Role role = (Role) roles.getSelectedToggle().getUserData();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = newPasswordField.getText();
        String confirm = confirmField.getText();
        runInBackground(registerButton, "Creating account…", registerErrors,
                () -> app.auth().register(username, email, password, confirm, role));
    }

    @FXML
    private void onShowRegister() {
        showForm(false);
    }

    @FXML
    private void onShowSignIn() {
        showForm(true);
    }

    private void showForm(boolean signIn) {
        signInErrors.clear();
        registerErrors.clear();
        signInForm.setVisible(signIn);
        signInForm.setManaged(signIn);
        registerForm.setVisible(!signIn);
        registerForm.setManaged(!signIn);
        // only the visible form's button may answer the Enter key
        signInButton.setDefaultButton(signIn);
        registerButton.setDefaultButton(!signIn);
        Platform.runLater(() -> (signIn ? loginField : usernameField).requestFocus());
    }

    private ToggleButton roleChoice(Role role) {
        Label title = new Label(switch (role) {
            case READER -> "Reader";
            case AUTHOR -> "Author";
            case ADMIN -> "Admin";
        });
        title.getStyleClass().add("role-title");
        Label description = new Label(switch (role) {
            case READER -> "Read, comment and rate posts";
            case AUTHOR -> "Write and publish your own posts";
            case ADMIN -> "First account: manage everything";
        });
        description.getStyleClass().add("role-description");
        ToggleButton choice = new ToggleButton();
        choice.setGraphic(new VBox(2, title, description));
        choice.setUserData(role);
        choice.setToggleGroup(roles);
        choice.getStyleClass().add("role-choice");
        choice.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(choice, Priority.ALWAYS);
        return choice;
    }

    /** Runs {@code work} off the JavaFX thread; on success opens the workspace, on failure marks the form. */
    private void runInBackground(Button button, String busyText, FormErrors errors, Supplier<?> work) {
        errors.clear();
        String text = button.getText();
        VBox form = button == signInButton ? signInForm : registerForm;
        button.setText(busyText);
        form.setDisable(true);
        Task<Object> task = new Task<>() {
            @Override
            protected Object call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSignedIn.run());
        task.setOnFailed(e -> {
            button.setText(text);
            form.setDisable(false);
            Throwable error = task.getException();
            switch (error) {
                case ValidationException invalid -> errors.show(invalid.getMessage(), invalid.fieldErrors());
                case DaoException database -> {
                    LOG.log(Level.WARNING, "Sign-in or registration failed", database);
                    errors.show(database.getMessage(), Map.of());
                }
                default -> {
                    LOG.log(Level.ERROR, "Unexpected error", error);
                    errors.show("Unexpected error: " + error.getMessage(), Map.of());
                }
            }
        });
        Thread thread = new Thread(task, "authentication");
        thread.setDaemon(true);
        thread.start();
    }
}
