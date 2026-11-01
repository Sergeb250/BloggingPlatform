package com.example.bloggingplatform;

import com.example.bloggingplatform.controller.Dialogs;
import com.example.bloggingplatform.controller.MainController;
import com.example.bloggingplatform.controller.Views;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.service.AppContext;
import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.util.NotebookArt;
import com.example.bloggingplatform.util.SplashScreen;
import com.example.bloggingplatform.util.Theme;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Starts the app: shows the splash screen while the database connection is checked on a
 * background thread, then opens the main window on the sign-in page.
 */
public class Main extends Application {

    /** The splash stays at least this long, so its animation is seen even when MySQL answers at once. */
    private static final Duration SPLASH_TIME = Duration.millis(2600);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        List<Image> icons = NotebookArt.icons();
        SplashScreen splash = new SplashScreen(icons);
        splash.show();

        // connect off the JavaFX thread, so the splash animation keeps running meanwhile
        CompletableFuture<AppContext> startup = CompletableFuture.supplyAsync(Main::connect);
        PauseTransition minimum = new PauseTransition(SPLASH_TIME);
        minimum.setOnFinished(done -> startup.whenComplete((app, error) -> Platform.runLater(() -> {
            if (error == null) {
                openMainWindow(stage, app, icons);
                splash.close();   // after the main window is up, so the app never has zero windows
            } else {
                splash.fail("Could not connect");
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                Dialogs.error(null, "The application cannot start", cause.getMessage());
                Platform.exit();
            }
        })));
        minimum.play();
    }

    /** Checks that MySQL answers, then creates the services. Runs on a background thread. */
    private static AppContext connect() {
        try (Connection connection = DBConnection.getConnection()) {
            if (!connection.isValid(2)) {
                throw new SQLException("The database connection is not valid", "08000");
            }
        } catch (SQLException e) {
            throw new DaoException("Could not connect to the database.", e);
        }
        return AppContext.create();
    }

    private static void openMainWindow(Stage stage, AppContext app, List<Image> icons) {
        Views.View<MainController> main = Views.load("main-view.fxml", app);
        Scene scene = new Scene(main.root(), Color.TRANSPARENT);   // transparent around the rounded window
        Theme.apply(scene);
        stage.initStyle(StageStyle.TRANSPARENT);   // no OS title bar: the app draws its own
        stage.setScene(scene);
        stage.setTitle("Smart Blogging Platform");
        stage.getIcons().setAll(icons);
        main.controller().attach(stage);
        stage.show();
    }
}
