package com.example.bloggingplatform.model;

/** A row of the {@code tags} table. Names are lower-case slugs such as {@code spring-boot}. */
public class Tag {

    private int id;          // 0 until the tag is saved
    private String name;

    public Tag() {
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Two Tag objects are the same tag when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Tag tag && tag.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** Text shown in drop-down lists. */
    @Override
    public String toString() {
        return name;
    }
}
