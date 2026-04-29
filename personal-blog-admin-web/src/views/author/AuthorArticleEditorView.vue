<template>
  <section class="author-page">
    <div class="page-heading">
      <h1>{{ pageTitle }}</h1>
      <el-button text :icon="Back" @click="router.push('/author/articles')">返回列表</el-button>
    </div>

    <section class="panel author-editor-panel">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="author-article-form">
        <div class="form-grid">
          <el-form-item label="标题" prop="articleTitle">
            <el-input v-model.trim="form.articleTitle" maxlength="200" placeholder="请输入文章标题" />
          </el-form-item>
          <el-form-item label="Slug" prop="articleSlug">
            <el-input v-model.trim="form.articleSlug" maxlength="200" placeholder="文章 URL 标识符，如 my-first-post" />
          </el-form-item>
        </div>

        <el-form-item label="摘要" prop="articleSummary">
          <el-input
            v-model="form.articleSummary"
            maxlength="500"
            placeholder="请输入文章摘要（可选）"
            resize="none"
            :rows="3"
            type="textarea"
          />
        </el-form-item>

        <div class="form-grid form-grid--three">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" class="full-control" clearable filterable placeholder="请选择分类">
              <el-option
                v-for="category in categoryOptions"
                :key="category.id"
                :label="category.categoryName"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签" prop="tagIds">
            <el-select v-model="form.tagIds" class="full-control" multiple filterable collapse-tags placeholder="请选择标签">
              <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.tagName" :value="tag.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="articleStatus">
            <el-select v-model="form.articleStatus" class="full-control" placeholder="请选择状态">
              <el-option v-for="status in statusOptions" :key="status.value" :label="status.label" :value="status.value" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item prop="articleContent">
          <template #label>
            <span>正文</span>
            <span class="label-hint">支持 Markdown 格式</span>
          </template>
          <el-input
            v-model="form.articleContent"
            placeholder="请在此输入文章正文内容..."
            resize="vertical"
            :rows="16"
            type="textarea"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button @click="router.push('/author/articles')">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">保存文章</el-button>
        </div>
      </el-form>
    </section>
  </section>
</template>

<script setup>
import { Back } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createAuthorArticle,
  getAuthorArticle,
  getAuthorCategories,
  getAuthorTags,
  updateAuthorArticle
} from '@/api/articles.js';

const route = useRoute();
const router = useRouter();
const formRef = ref();
const saving = ref(false);
const categoryOptions = ref([]);
const tagOptions = ref([]);

const form = reactive({
  articleTitle: '',
  articleSlug: '',
  articleSummary: '',
  categoryId: '',
  tagIds: [],
  articleStatus: 'DRAFT',
  articleContent: ''
});

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '私密', value: 'PRIVATE' },
  { label: '已下线', value: 'OFFLINE' }
];

const isEdit = computed(() => Boolean(route.params.id));
const pageTitle = computed(() => (isEdit.value ? '编辑文章' : '新建文章'));
const rules = {
  articleTitle: [{ required: true, message: '文章标题不能为空', trigger: 'blur' }],
  articleStatus: [{ required: true, message: '请选择文章状态', trigger: 'change' }],
  articleContent: [{ required: true, message: '文章内容不能为空', trigger: 'blur' }]
};

function extractData(response) {
  return response?.data ?? response ?? {};
}

function normalizeList(response) {
  const data = extractData(response);
  return Array.isArray(data) ? data : data.records || data.list || [];
}

function fillForm(article) {
  form.articleTitle = article.articleTitle || '';
  form.articleSlug = article.articleSlug || '';
  form.articleSummary = article.articleSummary || '';
  form.categoryId = article.categoryId ?? '';
  form.tagIds = Array.isArray(article.tagIds) ? article.tagIds : [];
  form.articleStatus = article.articleStatus || 'DRAFT';
  form.articleContent = article.articleContent || '';
}

function buildPayload() {
  return {
    articleTitle: form.articleTitle,
    articleSlug: form.articleSlug,
    articleSummary: form.articleSummary,
    categoryId: form.categoryId || null,
    tagIds: form.tagIds,
    articleStatus: form.articleStatus,
    articleContent: form.articleContent
  };
}

async function loadOptions() {
  try {
    const [categories, tags] = await Promise.all([getAuthorCategories(), getAuthorTags()]);
    categoryOptions.value = normalizeList(categories);
    tagOptions.value = normalizeList(tags);
  } catch (error) {
    ElMessage.warning(error.message || '分类或标签加载失败');
  }
}

async function loadArticle() {
  if (!isEdit.value) {
    return;
  }

  try {
    fillForm(extractData(await getAuthorArticle(route.params.id)));
  } catch (error) {
    ElMessage.error(error.message || '文章原数据加载失败');
    router.push('/author/articles');
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  saving.value = true;

  try {
    if (isEdit.value) {
      await updateAuthorArticle(route.params.id, buildPayload());
      ElMessage.success('文章已保存');
    } else {
      await createAuthorArticle(buildPayload());
      ElMessage.success('文章已创建');
    }
    router.push('/author/articles');
  } catch (error) {
    ElMessage.error(error.message || '文章保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  await loadOptions();
  await loadArticle();
});
</script>

<style scoped>
.author-editor-panel {
  padding: 28px;
}

.author-article-form {
  max-width: 1100px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.form-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.full-control {
  width: 100%;
}

.label-hint {
  margin-left: 8px;
  color: var(--color-muted);
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
}

@media (max-width: 900px) {
  .form-grid,
  .form-grid--three {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
