package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.entity.BlogFriendLink;
import org.example.personalblogsystem.mapper.BlogFriendLinkMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogFriendLinkControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private BlogFriendLinkMapper blogFriendLinkMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void tearDown() {
        reset(blogFriendLinkMapper);
    }

    @Test
    void shouldReturnPagedFriendLinks() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    void shouldRejectUnauthenticatedCreateFriendLink() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectInvalidFriendLinkPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectInvalidFriendLinkPageSize() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectNonPositiveFriendLinkPageSize() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectBlankSiteNameOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("   ",
                                randomUrl(),
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteName must not be blank"));
    }

    @Test
    void shouldRejectBlankSiteUrlOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                "   ",
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteUrl must not be blank"));
    }

    @Test
    void shouldAllowCreateWithoutClientCreatedBy() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectDuplicateSiteUrlOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Duplicate Link",
                                "https://example.com/study-notes",
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteUrl already exists"));
    }

    @Test
    void shouldRejectInvalidLinkStatusOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                "ARCHIVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("linkStatus must be one of PENDING, APPROVED, REJECTED"));
    }

    @Test
    void shouldAcceptLowercaseLinkStatusInTurkishLocaleOnCreate() throws Exception {
        withLocale(new Locale("tr", "TR"), () -> mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                "https://example.com/logo.png",
                                "Temporary friend link",
                                "approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.linkStatus").value("APPROVED")));
    }

    @Test
    void shouldIgnoreSpoofedCreatedByAndPersistAuthenticatedOwner() throws Exception {
        LoginSession session = loginAndGetSession("root", "123456");
        String siteUrl = randomUrl();
        String requestJson = """
                {
                  "siteName": "Temp Link",
                  "siteUrl": "%s",
                  "siteLogo": "https://example.com/logo.png",
                  "siteDesc": "Temporary friend link",
                  "ownerName": "Sample Owner",
                  "contactEmail": "contact@example.com",
                  "createdBy": 999
                }
                """.formatted(siteUrl);

        JsonNode createdNode = performJson(post("/admin/friend-link")
                .header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        long friendLinkId = createdNode.path("data").path("id").asLong();
        Map<String, Object> persisted = jdbcTemplate.queryForMap(
                "select site_name, site_url, created_by from blog_friend_link where id = ?",
                friendLinkId);
        assertThat(persisted.get("site_name")).isEqualTo("Temp Link");
        assertThat(persisted.get("site_url")).isEqualTo(siteUrl);
        assertThat(persisted.get("created_by")).isEqualTo(session.userId());
    }

    @Test
    void shouldTranslateCreatedByForeignKeyFailureOnCreateToBadRequest() throws Exception {
        doThrow(createdByForeignKeyViolation())
                .when(blogFriendLinkMapper)
                .insert(any(BlogFriendLink.class));

        mockMvc.perform(post("/admin/friend-link")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy is invalid"));
    }

    @Test
    void shouldCreateUpdateAndDeleteFriendLink() throws Exception {
        String siteUrl = randomUrl();
        Map<String, Object> request = friendLinkRequest("Temp Link",
                siteUrl,
                "https://example.com/logo.png",
                "Temporary friend link",
                null);

        JsonNode createdNode = performJson(post("/admin/friend-link")
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        long friendLinkId = createdNode.path("data").path("id").asLong();
        assertThat(friendLinkId).isPositive();

        Map<String, Object> updateRequest = friendLinkRequest("Temp Link Updated",
                siteUrl,
                "https://example.com/logo-updated.png",
                "Updated friend link",
                "Sample Owner");
        updateRequest.put("linkStatus", "APPROVED");

        performJson(put("/admin/friend-link/{id}", friendLinkId)
                .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        Map<String, Object> updatedRow = jdbcTemplate.queryForMap(
                "select site_name, site_url, site_logo, site_desc, owner_name, contact_email, link_status, deleted " +
                        "from blog_friend_link where id = ?",
                friendLinkId);
        assertThat(updatedRow.get("site_name")).isEqualTo("Temp Link Updated");
        assertThat(updatedRow.get("site_url")).isEqualTo(siteUrl);
        assertThat(updatedRow.get("link_status")).isEqualTo("APPROVED");

        mockMvc.perform(delete("/admin/friend-link/{id}", friendLinkId)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted from blog_friend_link where id = ?",
                Integer.class,
                friendLinkId);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingFriendLink() throws Exception {
        mockMvc.perform(put("/admin/friend-link/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Missing Link",
                                randomUrl(),
                                null,
                                null,
                                "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingFriendLink() throws Exception {
        mockMvc.perform(delete("/admin/friend-link/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private Map<String, Object> friendLinkRequest(String siteName,
                                                  String siteUrl,
                                                  String siteLogo,
                                                  String siteDesc,
                                                  String linkStatus) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("siteName", siteName);
        request.put("siteUrl", siteUrl);
        request.put("siteLogo", siteLogo);
        request.put("siteDesc", siteDesc);
        request.put("ownerName", "Sample Owner");
        request.put("contactEmail", "contact@example.com");
        if (linkStatus != null) {
            request.put("linkStatus", linkStatus);
        }
        return request;
    }

    private String randomUrl() {
        return "https://example.org/" + UUID.randomUUID();
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private DataIntegrityViolationException createdByForeignKeyViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails (`blog_friend_link`, CONSTRAINT `fk_blog_friend_link_created_by` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`))",
                        "23000",
                        1452));
    }

    private String loginAndGetAccessToken(String userName, String password) throws Exception {
        return loginAndGetSession(userName, password).accessToken();
    }

    private LoginSession loginAndGetSession(String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(userName, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return new LoginSession(
                root.path("data").path("accessToken").asText(),
                root.path("data").path("id").asLong());
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    private void withLocale(Locale locale, ThrowingRunnable action) throws Exception {
        Locale previous = Locale.getDefault();
        Locale.setDefault(locale);
        try {
            action.run();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record LoginSession(String accessToken, long userId) {
    }
}
