package org.example.personalblogsystem.job;

import com.xxl.job.core.context.XxlJobContext;
import org.example.personalblogsystem.service.IBlogArticleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ArticlePublishJobTest {

    @AfterEach
    void tearDown() {
        XxlJobContext.setXxlJobContext(null);
    }

    @Test
    void shouldPublishArticleByIdFromJobParam() {
        IBlogArticleService blogArticleService = mock(IBlogArticleService.class);
        when(blogArticleService.publishArticleById(104L)).thenReturn(1);
        XxlJobContext.setXxlJobContext(new XxlJobContext(
                1L,
                "{\"articleId\":104}",
                "test.log",
                0,
                1));
        ArticlePublishJob articlePublishJob = new ArticlePublishJob(blogArticleService);

        articlePublishJob.publishArticleJob();

        verify(blogArticleService).publishArticleById(104L);
        verifyNoMoreInteractions(blogArticleService);
        assertThat(XxlJobContext.getXxlJobContext().getHandleCode()).isEqualTo(XxlJobContext.HANDLE_CODE_SUCCESS);
    }

    @Test
    void shouldPublishScheduledArticlesWhenJobParamIsBlank() {
        IBlogArticleService blogArticleService = mock(IBlogArticleService.class);
        when(blogArticleService.publishScheduledArticles()).thenReturn(2);
        XxlJobContext.setXxlJobContext(new XxlJobContext(1L, "", "test.log", 0, 1));
        ArticlePublishJob articlePublishJob = new ArticlePublishJob(blogArticleService);

        articlePublishJob.publishArticleJob();

        verify(blogArticleService).publishScheduledArticles();
        verifyNoMoreInteractions(blogArticleService);
        assertThat(XxlJobContext.getXxlJobContext().getHandleCode()).isEqualTo(XxlJobContext.HANDLE_CODE_SUCCESS);
    }
}
