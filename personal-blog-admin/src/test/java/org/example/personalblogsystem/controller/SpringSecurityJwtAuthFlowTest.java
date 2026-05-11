package org.example.personalblogsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.utils.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import jakarta.servlet.Filter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PersonalBlogSystemApplication.class,
        properties = "spring.profiles.active=test")
@Transactional
class SpringSecurityJwtAuthFlowTest {

    private static final String ROOT_LOGIN_KEY = "login:user:1";
    private static final String JERRY_LOGIN_KEY = "login:user:5";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @MockitoBean
    private RedisCache redisCache;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> redisStore = new ConcurrentHashMap<>();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        redisStore.clear();
        configureInMemoryRedisCache();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void shouldStoreLoginUserInRedisAndAllowBearerTokenAccess() throws Exception {
        String accessToken = loginAndGetAccessToken("/admin/auth/login", "root", "123456");

        assertThat(redisStore).containsKey(ROOT_LOGIN_KEY);

        mockMvc.perform(get("/admin/profile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userName").value("root"));
    }

    @Test
    void shouldDeleteRedisLoginStateOnAdminLogoutAndRejectOldToken() throws Exception {
        String accessToken = loginAndGetAccessToken("/admin/auth/login", "root", "123456");
        assertThat(redisStore).containsKey(ROOT_LOGIN_KEY);

        mockMvc.perform(post("/admin/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(redisStore).doesNotContainKey(ROOT_LOGIN_KEY);

        mockMvc.perform(get("/admin/profile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldKeepAdminAndUserEndpointsRoleIsolated() throws Exception {
        String adminToken = loginAndGetAccessToken("/admin/auth/login", "root", "123456");
        String userToken = loginAndGetAccessToken("/user/auth/login", "jerry", "123456");

        assertThat(redisStore).containsKeys(ROOT_LOGIN_KEY, JERRY_LOGIN_KEY);

        mockMvc.perform(get("/user/profile/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/admin/profile/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowPublicEndpointsWithoutTokenButRejectProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/public/categories/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/admin/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRoundTripLoginUserThroughRedisJsonSerializer() {
        LoginUser loginUser = new LoginUser(loginUserRow(1L, "root", "SUPER_ADMIN"));
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        Object deserialized = serializer.deserialize(serializer.serialize(loginUser));

        assertThat(deserialized).isInstanceOf(LoginUser.class);
        LoginUser restored = (LoginUser) deserialized;
        assertThat(restored.getId()).isEqualTo(1L);
        assertThat(restored.getUserName()).isEqualTo("root");
        assertThat(restored.getPermissions()).containsExactly("ROLE_SUPER_ADMIN");
    }

    private String loginAndGetAccessToken(String path, String userName, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(userName, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("accessToken").asText();
    }

    private LoginRequest loginRequest(String userName, String password) {
        LoginRequest request = new LoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    private LoginUserQueryRow loginUserRow(Long id, String userName, String roleCode) {
        LoginUserQueryRow row = new LoginUserQueryRow();
        row.setId(id);
        row.setUserName(userName);
        row.setPasswordHash("password-hash");
        row.setRoleId(1L);
        row.setRoleCode(roleCode);
        row.setRoleName(roleCode);
        row.setUserStatus("ENABLED");
        return row;
    }

    private void configureInMemoryRedisCache() {
        doAnswer(this::rememberCacheObject)
                .when(redisCache)
                .setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));
        doAnswer(invocation -> redisStore.get(invocation.getArgument(0, String.class)))
                .when(redisCache)
                .getCacheObject(anyString());
        doAnswer(invocation -> redisStore.remove(invocation.getArgument(0, String.class)) != null)
                .when(redisCache)
                .deleteObject(anyString());
    }

    private Object rememberCacheObject(InvocationOnMock invocation) {
        redisStore.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
        return null;
    }
}
