package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.service.AppContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Loads FXML views from {@code /com/example/bloggingplatform/view/}. Controllers are created
 * with {@code new XController(AppContext)} instead of a no-argument constructor, so they receive
 * the services; the same factory is used for views pulled in with {@code <fx:include>}.
 */
public final class Views {

    /** A loaded view and its controller. */
    public record View<C>(Parent root, C controller) {
    }

    private static final String FOLDER = "/com/example/bloggingplatform/view/";

    private Views() {
    }

    public static <C> View<C> load(String fxml, AppContext app) {
        FXMLLoader loader = new FXMLLoader(Views.class.getResource(FOLDER + fxml));
        loader.setControllerFactory(type -> createController(type, app));
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + fxml, e);
        }
    }

    private static Object createController(Class<?> type, AppContext app) {
        try {
            return type.getConstructor(AppContext.class).newInstance(app);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
    }
}
