package org.example.personalblogsystem.testsupport;

import jakarta.servlet.Filter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public final class SecurityMockMvcSupport {

    private SecurityMockMvcSupport() {
    }

    public static MockMvc secureMockMvc(WebApplicationContext webApplicationContext) {
        Filter springSecurityFilterChain = webApplicationContext.getBean("springSecurityFilterChain", Filter.class);
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }
}
