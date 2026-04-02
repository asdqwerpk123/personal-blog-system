package org.example.personalblogsystem.mapper;

import org.example.personalblogsystem.entity.BlogCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class BlogCategoryMapperTest {

    @Autowired
    private BlogCategoryMapper blogCategoryMapper;

    @Test
    void shouldInsertUpdateAndDeleteCategory() {
        BlogCategory category = new BlogCategory();
        category.setCategoryName("TestCategory-" + UUID.randomUUID().toString().substring(0, 8));
        category.setDescription("test");
        category.setSortNo(99);
        category.setCreatedBy(2L);
        category.setDeleted(false);

        int insertRows = blogCategoryMapper.insert(category);
        assertEquals(1, insertRows);
        assertNotNull(category.getId());

        category.setDescription("updated");
        int updateRows = blogCategoryMapper.updateById(category);
        assertEquals(1, updateRows);

        BlogCategory updated = blogCategoryMapper.selectById(category.getId());
        assertNotNull(updated);
        assertEquals("updated", updated.getDescription());

        int deleteRows = blogCategoryMapper.deleteById(category.getId());
        assertEquals(1, deleteRows);
    }
}