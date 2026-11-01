package com.example.bloggingplatform.controller;

/** Lets a screen move the user to another screen; implemented by {@link ShellController}. */
public interface Navigator {

    /** Opens the reading view of a post. */
    void openPost(int postId);

    /** Returns from the reading view to the screen it was opened from. */
    void back();

    /** Shows the admin's post table filtered to one author. */
    void showPostsOf(int authorId);
}
