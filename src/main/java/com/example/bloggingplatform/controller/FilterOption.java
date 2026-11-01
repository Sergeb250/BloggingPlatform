package com.example.bloggingplatform.controller;

import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An entry of a filter combo box: either "All ..." ({@code value == null}) or one value.
 * Using an explicit "All" entry avoids putting {@code null} into a combo box.
 */
record FilterOption<T>(String label, T value) {

    static <T> FilterOption<T> all(String label) {
        return new FilterOption<>(label, null);
    }

    static <T> FilterOption<T> of(T value) {
        return new FilterOption<>(value.toString(), value);
    }

    /** The chosen value, or {@code null} when "All" is chosen. */
    static <T> T selected(ComboBox<FilterOption<T>> box) {
        FilterOption<T> option = box.getValue();
        return option == null ? null : option.value();
    }

    /**
     * Replaces the options with "All" plus {@code values}, keeping the current choice if a value
     * with the same key is still present (records are re-created on every reload).
     */
    static <T> void fill(ComboBox<FilterOption<T>> box, String allLabel, List<T> values, Function<T, ?> key) {
        T current = selected(box);
        Object currentKey = current == null ? null : key.apply(current);
        List<FilterOption<T>> options = new ArrayList<>();
        options.add(all(allLabel));
        values.forEach(value -> options.add(of(value)));
        box.getItems().setAll(options);
        box.setValue(options.stream()
                .filter(option -> option.value() != null && Objects.equals(key.apply(option.value()), currentKey))
                .findFirst()
                .orElse(options.getFirst()));
    }

    @Override
    public String toString() {
        return label;
    }
}
