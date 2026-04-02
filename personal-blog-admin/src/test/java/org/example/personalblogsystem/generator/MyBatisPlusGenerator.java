package org.example.personalblogsystem.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

class MyBatisPlusGenerator {

    @Test
    void generateCoreTables() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path moduleDir = currentDir.getFileName() != null && "personal-blog-admin".equals(currentDir.getFileName().toString())
                ? currentDir
                : currentDir.resolve("personal-blog-admin");

        String url = System.getenv().getOrDefault(
                "BLOG_DB_URL",
                "jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        String username = System.getenv().getOrDefault("BLOG_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("BLOG_DB_PASSWORD", "123456");

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author("student")
                        .disableOpenDir()
                        .outputDir(moduleDir.resolve("src/main/java").toString()))
                .packageConfig(builder -> builder
                        .parent("org.example.personalblogsystem")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(
                                OutputFile.xml,
                                moduleDir.resolve("src/main/resources/mapper").toString())))
                .strategyConfig(builder -> builder
                        .addInclude("sys_user", "sys_role", "blog_article", "blog_category")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .controllerBuilder()
                        .enableRestStyle())
                .execute();
    }
}