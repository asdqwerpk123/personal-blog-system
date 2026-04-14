package org.example.personalblogsystem.controller;

import org.example.personalblogsystem.config.OpenApiConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpenApiDocumentationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.profiles.active=test")
class OpenApiDocumentationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(OpenApiConfig.class)
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        DataSource dataSource() {
            return new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new SQLException("Test datasource should never be used");
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    throw new SQLException("Test datasource should never be used");
                }
            };
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldExposeOpenApiJsonWithCustomMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Personal Blog API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.info.description").value("Personal blog backend APIs"));
    }
}
