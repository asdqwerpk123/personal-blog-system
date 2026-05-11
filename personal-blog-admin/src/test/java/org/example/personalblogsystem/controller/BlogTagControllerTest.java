package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.mapper.BlogTagMapper;
import org.example.personalblogcommon.result.ResultCodeEnum;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.example.personalblogsystem.testsupport.SecurityMockMvcSupport.secureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class BlogTagControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoSpyBean
    private BlogTagMapper blogTagMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = secureMockMvc(webApplicationContext);
    }

    @AfterEach
    void tearDown() {
        reset(blogTagMapper);
    }

    @Test
    void shouldReturnPagedTags() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void shouldFilterPagedTagsByKeyword() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "Vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].tagName").value("Vue"));
    }

    @Test
    void shouldRejectInvalidTagPageCurrent() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("current must be greater than 0"));
    }

    @Test
    void shouldRejectInvalidTagPageSize() throws Exception {
        mockMvc.perform(get("/admin/tag/page")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("size must not exceed 100"));
    }

    @Test
    void shouldRejectBlankTagNameOnCreate() throws Exception {
        String requestJson = """
                {
                  "tagName": "   ",
                  "description": "temp"
                }
                """;

        mockMvc.perform(post("/admin/tag")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName must not be blank"));
    }

    @Test
    void shouldRejectUnauthenticatedCreateTag() throws Exception {
        String requestJson = """
                {
                  "tagName": "Temp-%s",
                  "description": "temp"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/admin/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowCreateWithoutClientCreatedBy() throws Exception {
        String requestJson = """
                {
                  "tagName": "Temp-%s",
                  "description": "temp"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/admin/tag")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldIgnoreSpoofedCreatedByAndPersistAuthenticatedOwner() throws Exception {
        LoginSession session = loginAndGetSession("root", "123456");
        String tagName = "Temp-" + UUID.randomUUID().toString().substring(0, 8);
        String requestJson = """
                {
                  "tagName": "%s",
                  "description": "temp",
                  "createdBy": 999
                }
                """.formatted(tagName);

        JsonNode createdNode = performJson(post("/admin/tag")
                .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));

        long tagId = createdNode.path("data").path("id").asLong();
        BlogTag persisted = blogTagMapper.selectById(tagId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTagName()).isEqualTo(tagName);
        assertThat(persisted.getCreatedBy()).isEqualTo(session.userId());
    }

    @Test
    void shouldRejectDuplicateTagNameOnCreate() throws Exception {
        String requestJson = """
                {
                  "tagName": "SpringBoot",
                  "description": "duplicate"
                }
                """;

        mockMvc.perform(post("/admin/tag")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName already exists"));
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnCreateToBadRequest() throws Exception {
        doThrow(duplicateConstraintViolation())
                .when(blogTagMapper)
                .insert(any(BlogTag.class));

        String requestJson = """
                {
                  "tagName": "Temp-%s",
                  "description": "temp"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/admin/tag")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName already exists"));
    }

    @Test
    void shouldTranslateDuplicateKeyFailureOnUpdateToBadRequest() throws Exception {
        BlogTag updateRequest = new BlogTag();
        updateRequest.setTagName("SpringBoot-updated");
        updateRequest.setDescription("updated");

        doThrow(duplicateConstraintViolation())
                .when(blogTagMapper)
                .updateById(any(BlogTag.class));

        mockMvc.perform(put("/admin/tag/{id}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tagName already exists"));
    }

    @Test
    void shouldNotTranslateNonDuplicateConstraintFailureToDuplicateMessage() throws Exception {
        doThrow(nonDuplicateConstraintViolation())
                .when(blogTagMapper)
                .insert(any(BlogTag.class));

        String requestJson = """
                {
                  "tagName": "Temp-%s",
                  "description": "temp"
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(post("/admin/tag")
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SYSTEM_ERROR.getCode()));
    }

    @Test
    void shouldCreateUpdateAndDeleteTag() throws Exception {
        LoginSession session = loginAndGetSession("root", "123456");
        jdbcTemplate.update("delete from sys_operation_log");
        String tagName = "Temp-" + UUID.randomUUID().toString().substring(0, 8);
        String createRequest = """
                {
                  "tagName": "%s",
                  "description": "temp"
                }
                """.formatted(tagName);

        JsonNode createdNode = performJson(post("/admin/tag")
                .header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest));

        long tagId = createdNode.path("data").path("id").asLong();
        assertThat(tagId).isPositive();

        BlogTag persisted = blogTagMapper.selectById(tagId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTagName()).isEqualTo(tagName);

        BlogTag updateRequest = new BlogTag();
        updateRequest.setTagName(tagName + "-updated");
        updateRequest.setDescription("updated");

        performJson(put("/admin/tag/{id}", tagId)
                .header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        BlogTag updated = blogTagMapper.selectById(tagId);
        assertThat(updated).isNotNull();
        assertThat(updated.getTagName()).isEqualTo(tagName + "-updated");
        assertThat(updated.getDescription()).isEqualTo("updated");

        mockMvc.perform(delete("/admin/tag/{id}", tagId)
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(blogTagMapper.selectById(tagId)).isNull();
        assertThat(countOperationLogs(session.userId(), "TAG", tagId, "CREATE_TAG")).isEqualTo(1);
        assertThat(countOperationLogs(session.userId(), "TAG", tagId, "UPDATE_TAG")).isEqualTo(1);
        assertThat(countOperationLogs(session.userId(), "TAG", tagId, "DELETE_TAG")).isEqualTo(1);
    }

    @Test
    void shouldRejectDeletingReferencedTag() throws Exception {
        mockMvc.perform(delete("/admin/tag/{id}", 1L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tag is referenced by articles"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingTag() throws Exception {
        BlogTag updateRequest = new BlogTag();
        updateRequest.setTagName("Missing");
        updateRequest.setDescription("Missing");

        mockMvc.perform(put("/admin/tag/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingTag() throws Exception {
        mockMvc.perform(delete("/admin/tag/{id}", 999L)
                        .header("Authorization", "Bearer " + loginAndGetAccessToken("root", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    private JsonNode performJson(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private DataIntegrityViolationException duplicateConstraintViolation() {
        return new DataIntegrityViolationException(
                "translated integrity constraint failure",
                new SQLIntegrityConstraintViolationException(
                        "Integrity constraint violation",
                        "23000",
                        1062));
    }

    private DataIntegrityViolationException nonDuplicateConstraintViolation() {
        return new DataIntegrityViolationException(
                "translated foreign key failure",
                new SQLIntegrityConstraintViolationException(
                        "Cannot add or update a child row: a foreign key constraint fails",
                        "23000",
                        1452));
    }

    private String loginAndGetAccessToken(String userName, String password) throws Exception {
        return loginAndGetSession(userName, password).accessToken();
    }

    private Integer countOperationLogs(long operatorUserId, String targetType, long targetId, String actionType) {
        return jdbcTemplate.queryForObject("""
                        select count(*) from sys_operation_log
                        where operator_user_id = ?
                          and target_type = ?
                          and target_id = ?
                          and action_type = ?
                          and action_result = 'SUCCESS'
                        """,
                Integer.class,
                operatorUserId,
                targetType,
                targetId,
                actionType);
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

    private record LoginSession(String accessToken, long userId) {
    }
}
