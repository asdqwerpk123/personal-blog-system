package org.example.personalblogsystem.service.impl;

import org.example.personalblogsystem.entity.BlogCategory;
import org.example.personalblogsystem.mapper.BlogCategoryMapper;
import org.example.personalblogsystem.service.IBlogCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章分类表 服务实现类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements IBlogCategoryService {

}
