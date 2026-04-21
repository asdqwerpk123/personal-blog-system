import {
  ElButton,
  ElCheckbox,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMenu,
  ElMenuItem,
  ElTable,
  ElTableColumn
} from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import { createPinia } from 'pinia';
import { createApp } from 'vue';

import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router/index.js';
import './styles/index.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
[
  ElButton,
  ElCheckbox,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElMenu,
  ElMenuItem,
  ElTable,
  ElTableColumn
].forEach((component) => app.use(component, { locale: zhCn }));

app.mount('#app');
