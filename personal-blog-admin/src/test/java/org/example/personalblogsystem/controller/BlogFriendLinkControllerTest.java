package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnPagedFriendLinks() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    void shouldRejectInvalidFriendLinkPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectInvalidFriendLinkPageSize() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectNonPositiveFriendLinkPageSize() throws Exception {
        mockMvc.perform(get("/admin/friend-link/page")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectBlankSiteNameOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("   ",
                                randomUrl(),
                                null,
                                null,
                                null,
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteName must not be blank"));
    }

    @Test
    void shouldRejectBlankSiteUrlOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                "   ",
                                null,
                                null,
                                null,
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteUrl must not be blank"));
    }

    @Test
    void shouldRejectMissingCreatedByOnCreate() throws Exception {
        Map<String, Object> request = friendLinkRequest("Temp Link",
                randomUrl(),
                null,
                null,
                null,
                null);

        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("createdBy must not be null"));
    }

    @Test
    void shouldRejectInvalidCreatedByOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                null,
                                999L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectDuplicateSiteUrlOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Duplicate Link",
                                "https://example.com/study-notes",
                                null,
                                null,
                                null,
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("siteUrl already exists"));
    }

    @Test
    void shouldRejectInvalidLinkStatusOnCreate() throws Exception {
        mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                null,
                                null,
                                "ARCHIVED",
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("linkStatus must be one of PENDING, APPROVED, REJECTED"));
    }

    @Test
    void shouldAcceptLowercaseLinkStatusInTurkishLocaleOnCreate() throws Exception {
        withLocale(new Locale("tr", "TR"), () -> mockMvc.perform(post("/admin/friend-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Temp Link",
                                randomUrl(),
                                "https://example.com/logo.png",
                                "Temporary friend link",
                                "approved",
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.linkStatus").value("APPROVED")));
    }

    @Test
    void shouldCreateUpdateAndDeleteFriendLink() throws Exception {
        String siteUrl = randomUrl();
        Map<String, Object> request = friendLinkRequest("Temp Link",
                siteUrl,
                "https://example.com/logo.png",
                "Temporary friend link",
                null,
                2L);

        JsonNode createdNode = performJson(post("/admin/friend-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        long friendLinkId = createdNode.path("data").path("id").asLong();
        assertThat(friendLinkId).isPositive();

        Map<String, Object> updateRequest = friendLinkRequest("Temp Link Updated",
                siteUrl,
                "https://example.com/logo-updated.png",
                "Updated friend link",
                "Sample Owner",
                null);
        updateRequest.put("linkStatus", "APPROVED");

        performJson(put("/admin/friend-link/{id}", friendLinkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        Map<String, Object> updatedRow = jdbcTemplate.queryForMap(
                "select site_name, site_url, site_logo, site_desc, owner_name, contact_email, link_status, deleted " +
                        "from blog_friend_link where id = ?",
                friendLinkId);
        assertThat(updatedRow.get("site_name")).isEqualTo("Temp Link Updated");
        assertThat(updatedRow.get("site_url")).isEqualTo(siteUrl);
        assertThat(updatedRow.get("link_status")).isEqualTo("APPROVED");

        mockMvc.perform(delete("/admin/friend-link/{id}", friendLinkId))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(friendLinkRequest("Missing Link",
                                randomUrl(),
                                null,
                                null,
                                "APPROVED",
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingFriendLink() throws Exception {
        mockMvc.perform(delete("/admin/friend-link/{id}", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private Map<String, Object> friendLinkRequest(String siteName,
                                                  String siteUrl,
                                                  String siteLogo,
                                                  String siteDesc,
                                                  String linkStatus,
                                                  Long createdBy) {
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
        if (createdBy != null) {
            request.put("createdBy", createdBy);
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
}
