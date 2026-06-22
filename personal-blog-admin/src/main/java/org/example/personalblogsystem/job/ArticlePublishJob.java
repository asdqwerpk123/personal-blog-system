package org.example.personalblogsystem.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArticlePublishJob {

    private final IBlogArticleService blogArticleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticlePublishJob(IBlogArticleService blogArticleService) {
        this.blogArticleService = blogArticleService;
    }

    @XxlJob("publishArticleJob")
    public void publishArticleJob() {
        String jobParam = XxlJobHelper.getJobParam();
        if (!StringUtils.hasText(jobParam)) {
            int count = blogArticleService.publishScheduledArticles();
            XxlJobHelper.handleSuccess("publish scheduled articles: " + count);
            return;
        }

        Long articleId = parseArticleId(jobParam);
        int count = blogArticleService.publishArticleById(articleId);
        XxlJobHelper.handleSuccess("publish article id " + articleId + ": " + count);
    }

    private Long parseArticleId(String jobParam) {
        try {
            JsonNode root = objectMapper.readTree(jobParam);
            JsonNode articleIdNode = root.path("articleId");
            if (articleIdNode.isMissingNode() || articleIdNode.isNull()) {
                throw new IllegalArgumentException("job param articleId is required");
            }
            return articleIdNode.asLong();
        } catch (Exception exception) {
            throw new IllegalArgumentException("job param must be JSON like {\"articleId\":104}", exception);
        }
    }
}
