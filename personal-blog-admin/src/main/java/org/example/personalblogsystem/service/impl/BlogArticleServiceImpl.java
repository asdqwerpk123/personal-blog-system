package org.example.personalblogsystem.service.impl;

import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.service.IBlogArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements IBlogArticleService {

}
