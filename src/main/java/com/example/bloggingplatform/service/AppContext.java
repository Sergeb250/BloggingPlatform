package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.ReportDAO;
import com.example.bloggingplatform.dao.ReviewDAO;
import com.example.bloggingplatform.dao.TagDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.dao.impl.CommentDAOImpl;
import com.example.bloggingplatform.dao.impl.PostDAOImpl;
import com.example.bloggingplatform.dao.impl.ReportDAOImpl;
import com.example.bloggingplatform.dao.impl.ReviewDAOImpl;
import com.example.bloggingplatform.dao.impl.TagDAOImpl;
import com.example.bloggingplatform.dao.impl.UserDAOImpl;
import com.example.bloggingplatform.util.PasswordHasher;

/**
 * All the services of the app. {@link #create()} is the one place that chooses the MySQL
 * implementations of the DAO interfaces and connects the layers: JavaFX UI → Service → DAO →
 * MySQL. Controllers receive this object and only ever talk to services. All services share one
 * {@link Session} (the signed-in user whose role they check) and one {@link PostCache} (the
 * published posts in memory, emptied by every service that changes them).
 */
public record AppContext(Session session, AuthService auth, UserService users, PostService posts,
                         CommentService comments, ReviewService reviews, TagService tags, ReportService reports) {

    public static AppContext create() {
        UserDAO userDAO = new UserDAOImpl();
        PostDAO postDAO = new PostDAOImpl();
        TagDAO tagDAO = new TagDAOImpl();
        CommentDAO commentDAO = new CommentDAOImpl();
        ReviewDAO reviewDAO = new ReviewDAOImpl();
        ReportDAO reportDAO = new ReportDAOImpl();

        Session session = new Session();
        PostCache cache = new PostCache(postDAO);
        PasswordHasher hasher = new PasswordHasher();
        UserService userService = new UserService(userDAO, hasher, session, cache);
        TagService tagService = new TagService(tagDAO, session, cache);

        return new AppContext(session,
                new AuthService(userDAO, userService, hasher, session),
                userService,
                new PostService(postDAO, tagService, userDAO, session, cache),
                new CommentService(commentDAO, postDAO, userDAO, session, cache),
                new ReviewService(reviewDAO, postDAO, userDAO, session, cache),
                tagService,
                new ReportService(reportDAO, postDAO, session));
    }
}
