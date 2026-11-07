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
        assertTrue(browser.get("/posts").body().contains("alice"));
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class) > 0);
    }

    @Test
    void login_validCredentials_reusesPersistedSessionAndIsolatedBrowserRemainsAnonymous() throws Exception {
        new Browser().register("alice", "alice@example.com", "secret123");
        Browser browser = new Browser();
        assertEquals(302, browser.login("alice", "secret123").statusCode());
        assertTrue(browser.get("/posts").body().contains("alice"));
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
        assertEquals(200, guest.get("/posts").statusCode());
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

    private record TestResponse(int statusCode, String body, String location) {}

    private class Browser {
        private final Map<String, Cookie> cookies = new HashMap<>();
        private final Pattern csrf = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

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

        TestResponse submit(String method, String path, String formPage, Map<String, String> fields) throws Exception {
            Matcher matcher = csrf.matcher(get(formPage).body());
            assertTrue(matcher.find(), "CSRF token missing on " + formPage);
            MockHttpServletRequestBuilder request = switch (method) {
                case "POST" -> post(path);
                case "PUT" -> put(path);
                case "DELETE" -> delete(path);
                default -> throw new IllegalArgumentException(method);
            };
            request.contentType("application/x-www-form-urlencoded").header("X-CSRF-TOKEN", matcher.group(1));
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
