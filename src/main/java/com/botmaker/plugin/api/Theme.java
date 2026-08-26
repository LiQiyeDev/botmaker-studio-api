package com.botmaker.plugin.api;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

/**
 * Making a plugin's own windows look like the rest of the application.
 *
 * <p>The host owns light/dark and the stylesheet that expresses it, and a plugin must never read which is
 * active and branch: it hands its scene over and gets the current look, whatever that is now and after the
 * next release. That is the difference between a plugin that keeps matching the application and one that
 * matched it once.
 */
public interface Theme {

    /** Applies the current look to a scene. */
    void apply(Scene scene);

    /** Applies the current look to a stage and its scene. */
    void apply(Stage stage);

    /** Applies the current look to a dialog. */
    void apply(Dialog<?> dialog);

    /** Applies the current look to a dialog pane — for a dialog built by hand rather than through {@link Dialog}. */
    void apply(DialogPane pane);

    /** A themed scene around this root, sized to its content. */
    Scene scene(Parent root);

    /** A themed scene around this root, at an explicit size. */
    Scene scene(Parent root, double width, double height);

    /** A themed alert. */
    Alert alert(Alert.AlertType type);

    /** A themed alert with a message and buttons. */
    Alert alert(Alert.AlertType type, String message, ButtonType... buttons);

    /**
     * Adds the host's style class to a node's subtree, for a node that will be attached to a scene the host
     * already themed.
     */
    void applyThemeClass(Parent root);
}
