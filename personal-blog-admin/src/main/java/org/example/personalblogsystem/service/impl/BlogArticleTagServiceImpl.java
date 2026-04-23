package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.personalblogsystem.entity.BlogArticle;
import org.example.personalblogsystem.entity.BlogArticleTag;
import org.example.personalblogsystem.entity.BlogTag;
import org.example.personalblogsystem.mapper.BlogArticleMapper;
import org.example.personalblogsystem.mapper.BlogArticleTagMapper;
import org.example.personalblogsystem.mapper.BlogTagMapper;
import org.example.personalblogsystem.service.IBlogArticleTagService;
import org.example.personalblogsystem.service.OperationLogRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BlogArticleTagServiceImpl extends ServiceImpl<BlogArticleTagMapper, BlogArticleTag> implements IBlogArticleTagService {

    private final BlogArticleMapper blogArticleMapper;
    private final BlogTagMapper blogTagMapper;
    private final OperationLogRecordService operationLogRecordService;

    public BlogArticleTagServiceImpl(BlogArticleMapper blogArticleMapper,
                                     BlogTagMapper blogTagMapper,
                                     OperationLogRecordService operationLogRecordService) {
        this.blogArticleMapper = blogArticleMapper;
        this.blogTagMapper = blogTagMapper;
        this.operationLogRecordService = operationLogRecordService;
    }

    @Override
    public List<BlogTag> listTagsByArticleId(Long articleId) {
        return blogTagMapper.selectByArticleId(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BlogTag> replaceArticleTags(Long articleId, List<Long> tagIds) {
        validateArticleExists(articleId);
        List<Long> normalizedTagIds = normalizeTagIds(tagIds);
        validateTagIdsExist(normalizedTagIds);

        List<BlogArticleTag> existingRelations = baseMapper.selectAllByArticleId(articleId);
        Map<Long, BlogArticleTag> relationByTagId = existingRelations.stream()
                .collect(Collectors.toMap(BlogArticleTag::getTagId, Function.identity(), (left, right) -> left));
        Set<Long> targetTagIds = new LinkedHashSet<>(normalizedTagIds);

        for (Long tagId : targetTagIds) {
            BlogArticleTag relation = relationByTagId.get(tagId);
            if (relation == null) {
                BlogArticleTag newRelation = new BlogArticleTag();
                LocalDateTime now = LocalDateTime.now();
                newRelation.setArticleId(articleId);
                newRelation.setTagId(tagId);
                newRelation.setCreateTime(now);
                newRelation.setUpdateTime(now);
                newRelation.setDeleted(false);
                save(newRelation);
                continue;
            }

            if (Boolean.TRUE.equals(relation.getDeleted())) {
                baseMapper.restoreById(relation.getId());
            }
        }

        for (BlogArticleTag relation : existingRelations) {
            if (!targetTagIds.contains(relation.getTagId()) && !Boolean.TRUE.equals(relation.getDeleted())) {
                baseMapper.logicDeleteById(relation.getId());
            }
        }

        List<BlogTag> tags = listTagsByArticleId(articleId);
        operationLogRecordService.recordSuccess("ARTICLE", articleId, "UPDATE_TAGS", "Update article tags success");
        return tags;
    }

    private void validateArticleExists(Long articleId) {
        BlogArticle article = blogArticleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("articleId does not exist");
        }
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            throw new IllegalArgumentException("tagIds must not be null");
        }

        Set<Long> uniqueTagIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId == null) {
                throw new IllegalArgumentException("tagIds must not contain null");
            }
            uniqueTagIds.add(tagId);
        }
        return new ArrayList<>(uniqueTagIds);
    }

    private void validateTagIdsExist(List<Long> tagIds) {
        for (Long tagId : tagIds) {
            BlogTag tag = blogTagMapper.selectById(tagId);
            if (tag == null) {
                throw new IllegalArgumentException("tagId does not exist: " + tagId);
            }
        }
    }
}
