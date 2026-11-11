package ru.sfedu.blog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.servlet.http.Cookie;
import ru.sfedu.blog.dto.PostWithCommentsResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BlogHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM spring_session_attributes");
        jdbc.update("DELETE FROM spring_session");
        jdbc.update("DELETE FROM comments");
        jdbc.update("DELETE FROM post_likes");
        jdbc.update("DELETE FROM posts");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void index_anonymousUser_canOpenNavigationLinks() throws Exception {
        TestResponse response = new Browser().get("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("href=\"/auth/login\""));
        assertTrue(response.body().contains("href=\"/auth/register\""));
        assertTrue(response.body().contains("href=\"/posts\""));
    }

    @Test
    void register_invalidInput_showsFieldErrorsAndRetainsSafeValues() throws Exception {
        TestResponse response = new Browser().register("bad@name", "invalid", "");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Начните с буквы"));
        assertTrue(response.body().contains("Введите корректный email"));
        assertTrue(response.body().contains("value=\"bad@name\""));
        assertTrue(response.body().contains("value=\"invalid\""));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users", Integer.class));
    }

    @Test
    void register_formData_persistsUserWithHashedPasswordAndAuthenticatesSession() throws Exception {
        Browser browser = new Browser();
        TestResponse response = browser.register("alice", "alice@example.com", "secret123");

        assertEquals(302, response.statusCode());
        Map<String, Object> user = jdbc.queryForMap("SELECT username, email, password_hash FROM users WHERE username = ?", "alice");
        assertEquals("alice@example.com", user.get("email"));
        assertNotEquals("secret123", user.get("password_hash"));
        assertTrue(passwordEncoder.matches("secret123", (String) user.get("password_hash")));
        assertTrue(browser.get("/auth/profile").body().contains("alice"));
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class) > 0);
    }

    @Test
    void login_validCredentials_reusesPersistedSessionAndIsolatedBrowserRemainsAnonymous() throws Exception {
        new Browser().register("alice", "alice@example.com", "secret123");
        Browser browser = new Browser();
        assertEquals(302, browser.login("alice", "secret123").statusCode());
        assertTrue(browser.get("/auth/profile").body().contains("alice"));
        assertEquals(200, browser.get("/posts/create").statusCode());
        assertFalse(new Browser().get("/posts").body().contains("alice"));
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM spring_session_attributes WHERE attribute_name = 'SPRING_SECURITY_CONTEXT'", Integer.class) > 0);
    }

    @Test
    void profile_rendersForAnonymousAndAuthenticatedUsers() throws Exception {
        assertTrue(new Browser().get("/auth/profile").body().contains("<title>Профиль</title>"));
        Browser browser = new Browser();
        browser.register("alice", "alice@example.com", "secret123");
        TestResponse response = browser.get("/auth/profile");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<title>Профиль: alice</title>"));
        assertFalse(response.body().contains(">Админ</span>"));

        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE username = 'alice'");
        Browser admin = new Browser();
        admin.login("alice", "secret123");
        TestResponse adminResponse = admin.get("/auth/profile");
        assertEquals(200, adminResponse.statusCode());
        assertTrue(adminResponse.body().contains(">Админ</span>"));
    }

    @Test
    void login_wrongPassword_doesNotAuthenticate() throws Exception {
        new Browser().register("alice", "alice@example.com", "secret123");
        Browser browser = new Browser();
        assertEquals(400, browser.login("alice", "wrong").statusCode());
        assertFalse(browser.get("/posts").body().contains("alice"));
    }

    @Test
    void logout_invalidatesSessionAndRejectsOldCookie() throws Exception {
        Browser browser = new Browser();
        browser.register("alice", "alice@example.com", "secret123");
        assertEquals(200, browser.get("/posts/create").statusCode());
        Cookie oldSession = browser.cookies.get("SESSION");
        assertNotNull(oldSession);

        TestResponse response = browser.submit("POST", "/auth/logout", "/posts/create", Map.of());

        assertEquals(302, response.statusCode());
        assertEquals("/", response.location());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM spring_session_attributes WHERE attribute_name = 'SPRING_SECURITY_CONTEXT'", Integer.class));
        Browser replay = new Browser();
        replay.cookies.put("SESSION", oldSession);
        assertEquals(403, replay.get("/posts/create").statusCode());
        assertFalse(replay.get("/posts").body().contains("alice"));
    }

    @Test
    void createPost_authenticatedUser_persistsContentAndAuthor() throws Exception {
        Browser browser = new Browser();
        browser.register("alice", "alice@example.com", "secret123");
        assertEquals(302, browser.createPost("First post", "Post body").statusCode());
        Map<String, Object> post = jdbc.queryForMap("SELECT p.title, p.content, u.username FROM posts p JOIN users u ON u.id = p.author_id");
        assertEquals("First post", post.get("title"));
        assertEquals("Post body", post.get("content"));
        assertEquals("alice", post.get("username"));
    }

    @Test
    void getPages_anonymousUser_canViewPostsButCannotOpenCreatePage() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Public post", "Public body");
        long id = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        Browser guest = new Browser();
        assertEquals(200, guest.get("/auth/login").statusCode());
        assertEquals(200, guest.get("/auth/register").statusCode());
        TestResponse feed = guest.get("/posts");
        assertEquals(200, feed.statusCode());
        assertTrue(Pattern.compile("Автор:\\s*<span>alice</span>").matcher(feed.body()).find());
        assertEquals(200, guest.get("/posts/" + id).statusCode());
        assertEquals(403, guest.get("/posts/create").statusCode());
    }

    @Test
    void createPost_anonymousUser_isRejectedAndDoesNotPersist() throws Exception {
        Browser guest = new Browser();
        TestResponse response = guest.submit("POST", "/posts", "/auth/login", Map.of("title", "Forbidden", "content", "Body"));
        assertEquals(403, response.statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM posts", Integer.class));
    }

    @Test
    void updateAndDeletePost_anotherUser_isForbiddenAndPostRemainsUnchanged() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Original", "Original body");
        long id = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        Browser other = new Browser();
        other.register("bob", "bob@example.com", "secret123");

        assertEquals(403, other.submit("PUT", "/posts/" + id, "/posts/create", Map.of("title", "Changed", "content", "Changed body")).statusCode());
        assertEquals(403, other.submit("DELETE", "/posts/" + id, "/posts/create", Map.of()).statusCode());
        Map<String, Object> post = jdbc.queryForMap("SELECT title, content FROM posts WHERE id = ?", id);
        assertEquals("Original", post.get("title"));
        assertEquals("Original body", post.get("content"));
    }

    @Test
    void updatePost_author_persistsBothTitleAndContent() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Original", "Original body");
        long id = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        TestResponse response = owner.submit("PUT", "/posts/" + id, "/posts/" + id, Map.of("title", "Updated title", "content", "Updated body"));
        assertEquals(302, response.statusCode());
        assertEquals("/posts/" + id, response.location());
        Map<String, Object> post = jdbc.queryForMap("SELECT title, content FROM posts WHERE id = ?", id);
        assertEquals("Updated title", post.get("title"));
        assertEquals("Updated body", post.get("content"));
    }

    @Test
    void deletePost_author_removesPost() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Original", "Original body");
        long id = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        TestResponse response = owner.submit("DELETE", "/posts/" + id, "/posts/" + id, Map.of());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 400);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM posts WHERE id = ?", Integer.class, id));
    }

    @Test
    void getPost_withComments_returnsOnlyCommentsForThatPost() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("First", "First body");
        owner.createPost("Second", "Second body");
        long firstId = jdbc.queryForObject("SELECT id FROM posts WHERE title = 'First'", Long.class);
        long secondId = jdbc.queryForObject("SELECT id FROM posts WHERE title = 'Second'", Long.class);
        long authorId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'alice'", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "First comment", firstId, authorId);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Second comment", firstId, authorId);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Other post comment", secondId, authorId);

        MvcResult result = mockMvc.perform(get("/posts/{id}", firstId)).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        PostWithCommentsResponse post = assertInstanceOf(PostWithCommentsResponse.class,
                result.getModelAndView().getModel().get("post"));
        assertEquals(firstId, post.id());
        assertEquals("First", post.title());
        assertEquals(2, post.comments().size());
        assertEquals(java.util.Set.of("First comment", "Second comment"),
                post.comments().stream().map(comment -> comment.text()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(post.comments().stream().allMatch(comment -> comment.authorId().equals(authorId)));
    }

    @Test
    void getPost_withoutComments_returnsEmptyList() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Empty", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);

        MvcResult result = mockMvc.perform(get("/posts/{id}", postId)).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        PostWithCommentsResponse post = assertInstanceOf(PostWithCommentsResponse.class,
                result.getModelAndView().getModel().get("post"));
        assertTrue(post.comments().isEmpty());
    }

    @Test
    void postComments_renderControlsAccordingToUserPermissions() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        long ownerId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'alice'", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Hello <world>", postId, ownerId);
        String page = "/posts/" + postId;

        String guestPage = new Browser().get(page).body();
        assertTrue(guestPage.contains("Hello &lt;world&gt;"));
        assertTrue(guestPage.contains("alice"));
        assertTrue(guestPage.contains("чтобы присоединиться к обсуждению"));
        assertFalse(guestPage.contains("id=\"comment-text\""));
        assertFalse(guestPage.contains("class=\"comment-delete-form\""));

        String ownerPage = owner.get(page).body();
        assertTrue(ownerPage.contains("id=\"comment-text\""));
        assertTrue(ownerPage.contains("class=\"comment-delete-form\""));

        Browser other = new Browser();
        other.register("bobby", "bobby@example.com", "secret123");
        String otherPage = other.get(page).body();
        assertTrue(otherPage.contains("id=\"comment-text\""));
        assertFalse(otherPage.contains("class=\"comment-delete-form\""));

        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE username = 'bobby'");
        Browser admin = new Browser();
        admin.login("bobby", "secret123");
        assertTrue(admin.get(page).body().contains("class=\"comment-delete-form\""));
    }

    @Test
    void getPost_missingPost_returnsNotFound() throws Exception {
        assertEquals(404, new Browser().get("/posts/999").statusCode());
    }

    @Test
    void getPost_illegalPost_returnsNotFound() throws Exception {
        assertEquals(403, new Browser().get("/posts/-1").statusCode());
    }

    @Test
    void createComment_authenticatedUser_persistsTextPostAndAuthor() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        author.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);

        TestResponse response = author.createComment(postId, "Hello comment");

        assertEquals(302, response.statusCode());
        assertEquals("/posts/" + postId, response.location());
        Map<String, Object> comment = jdbc.queryForMap("SELECT c.text, c.post_id, u.username FROM comments c JOIN users u ON u.id = c.author_id");
        assertEquals("Hello comment", comment.get("text"));
        assertEquals(postId, ((Number) comment.get("post_id")).longValue());
        assertEquals("alice", comment.get("username"));
    }

    @Test
    void createComment_invalidText_isRejectedWithoutChangingComments() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        author.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        long authorId = jdbc.queryForObject("SELECT id FROM users", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Existing", postId, authorId);

        for (Map<String, String> fields : java.util.List.of(
                Map.<String, String>of(),
                Map.of("text", ""),
                Map.of("text", "   "),
                Map.of("text", "a".repeat(2001)))) {
            TestResponse response = author.submit("POST", "/comments/" + postId, "/posts/create", fields);

            assertEquals(400, response.statusCode(), "Invalid text must be rejected: " + fields);
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
            assertEquals("Existing", jdbc.queryForObject("SELECT text FROM comments", String.class));
        }
    }

    @Test
    void createComment_validTextLengthBoundaries_arePersisted() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        author.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);

        for (String text : java.util.List.of("a", "b".repeat(1000))) {
            TestResponse response = author.createComment(postId, text);

            assertEquals(302, response.statusCode());
            assertEquals("/posts/" + postId, response.location());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comments WHERE post_id = ? AND text = ?",
                    Integer.class, postId, text));
        }
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
    }

    @Test
    void createComment_missingPost_doesNotPersist() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        assertEquals(404, author.createComment(-1, "Missing post").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
    }

    @Test
    void createComment_anonymousUser_doesNotPersist() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        Browser guest = new Browser();
        assertEquals(403, guest.submit("POST", "/comments/" + postId, "/auth/login", Map.of("text", "Forbidden")).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
    }

    @Test
    void deleteComment_author_removesOnlyOwnComment() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        author.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        long authorId = jdbc.queryForObject("SELECT id FROM users", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Keep", postId, authorId);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Delete", postId, authorId);
        long commentId = jdbc.queryForObject("SELECT id FROM comments WHERE text = 'Delete'", Long.class);
        if (commentId == authorId) {
            jdbc.update("DELETE FROM comments WHERE id = ?", commentId);
            jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Delete", postId, authorId);
            commentId = jdbc.queryForObject("SELECT id FROM comments WHERE text = 'Delete'", Long.class);
        }
        assertNotEquals(authorId, commentId);

        TestResponse response = author.deleteComment(commentId);

        assertEquals(204, response.statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comments WHERE id = ?", Integer.class, commentId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comments WHERE text = 'Keep'", Integer.class));
    }

    @Test
    void deleteComment_anotherUser_isForbiddenAndCommentRemains() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        long ownerId = jdbc.queryForObject("SELECT id FROM users", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Keep", postId, ownerId);
        long commentId = jdbc.queryForObject("SELECT id FROM comments", Long.class);
        Browser other = new Browser();
        other.register("bob", "bob@example.com", "secret123");

        assertEquals(403, other.deleteComment(commentId).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comments WHERE id = ?", Integer.class, commentId));
    }

    @Test
    void deleteComment_missingComment_returnsNotFound() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        assertEquals(404, author.deleteComment(999).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
    }

    @Test
    void deleteComment_illegalComment_returnsNotFound() throws Exception {
        Browser author = new Browser();
        author.register("alice", "alice@example.com", "secret123");
        assertEquals(404, author.deleteComment(-1).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comments", Integer.class));
    }


    @Test
    void deleteComment_anonymousUser_isForbiddenAndCommentRemains() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        long ownerId = jdbc.queryForObject("SELECT id FROM users", Long.class);
        jdbc.update("INSERT INTO comments (text, post_id, author_id) VALUES (?, ?, ?)", "Keep", postId, ownerId);
        long commentId = jdbc.queryForObject("SELECT id FROM comments", Long.class);

        Browser guest = new Browser();
        assertEquals(403, guest.submit("DELETE", "/comments/" + commentId, "/auth/login", Map.of()).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM comments WHERE id = ?", Integer.class, commentId));
    }

    @Test
    void likePost_authenticatedUser_persistsLikeForUserAndPost() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        Browser liker = new Browser();
        liker.register("bob", "bob@example.com", "secret123");

        TestResponse response = liker.likePost(postId);

        assertEquals(302, response.statusCode());
        assertEquals("/posts/" + postId, response.location());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM post_likes l JOIN users u ON u.id = l.user_id WHERE l.post_id = ? AND u.username = ?", Integer.class, postId, "bob"));
    }

    @Test
    void deleteLike_authenticatedUser_removesOnlyTheirLike() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        Browser other = new Browser();
        other.register("bob", "bob@example.com", "secret123");
        assertEquals(302, owner.likePost(postId).statusCode());
        assertEquals(302, other.likePost(postId).statusCode());

        TestResponse response = owner.deleteLike(postId);

        assertEquals(204, response.statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM post_likes WHERE post_id = ?", Integer.class, postId));
        assertEquals("bob", jdbc.queryForObject("SELECT u.username FROM post_likes l JOIN users u ON u.id = l.user_id WHERE l.post_id = ?", String.class, postId));
    }

    @Test
    void likeEndpoints_anonymousUser_areForbiddenAndDoNotChangeLikes() throws Exception {
        Browser owner = new Browser();
        owner.register("alice", "alice@example.com", "secret123");
        owner.createPost("Post", "Body");
        long postId = jdbc.queryForObject("SELECT id FROM posts", Long.class);
        assertEquals(302, owner.likePost(postId).statusCode());
        Browser guest = new Browser();

        assertEquals(403, guest.likePost(postId).statusCode());
        assertEquals(403, guest.deleteLike(postId).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM post_likes WHERE post_id = ?", Integer.class, postId));
    }

    private record TestResponse(int statusCode, String body, String location) {}

    private class Browser {
        private final Map<String, Cookie> cookies = new HashMap<>();
        private final Pattern csrf = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
        private final Pattern csrfData = Pattern.compile("data-csrf-token=\"([^\"]+)\"");

        TestResponse get(String path) throws Exception {
            return perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path));
        }

        TestResponse register(String username, String email, String password) throws Exception {
            return submit("POST", "/auth/register", "/auth/register", Map.of("username", username, "email", email, "password", password));
        }

        TestResponse login(String login, String password) throws Exception {
            return submit("POST", "/auth/login", "/auth/login", Map.of("loginInput", login, "password", password));
        }

        TestResponse createPost(String title, String content) throws Exception {
            return submit("POST", "/posts", "/posts/create", Map.of("title", title, "content", content));
        }

        TestResponse createComment(long postId, String text) throws Exception {
            return submit("POST", "/comments/" + postId, "/posts/create", Map.of("text", text));
        }

        TestResponse deleteComment(long commentId) throws Exception {
            return submit("DELETE", "/comments/" + commentId, "/posts/create", Map.of());
        }

        TestResponse likePost(long postId) throws Exception {
            return submit("POST", "/posts/" + postId + "/like", "/posts/" + postId, Map.of());
        }

        TestResponse deleteLike(long postId) throws Exception {
            return submit("DELETE", "/posts/" + postId + "/like", "/posts/" + postId, Map.of());
        }

        TestResponse submit(String method, String path, String formPage, Map<String, String> fields) throws Exception {
            String page = get(formPage).body();
            Matcher matcher = csrf.matcher(page);
            if (!matcher.find()) {
                matcher = csrfData.matcher(page);
                assertTrue(matcher.find(), "CSRF token missing on " + formPage);
            }
            MockHttpServletRequestBuilder request = switch (method) {
                case "POST" -> post(path);
                case "PUT" -> put(path);
                case "DELETE" -> delete(path);
                default -> throw new IllegalArgumentException(method);
            };
            request.contentType("application/x-www-form-urlencoded").header("X-CSRF-TOKEN", matcher.group(1)).header("Referer", formPage);
            fields.forEach(request::param);
            return perform(request);
        }

        private TestResponse perform(MockHttpServletRequestBuilder request) throws Exception {
            if (!cookies.isEmpty()) {
                request.cookie(cookies.values().toArray(Cookie[]::new));
            }
            MvcResult result = mockMvc.perform(request).andReturn();
            for (Cookie cookie : result.getResponse().getCookies()) {
                cookies.put(cookie.getName(), cookie);
            }
            return new TestResponse(result.getResponse().getStatus(), result.getResponse().getContentAsString(), result.getResponse().getHeader("Location"));
        }
    }
}
