import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import { register } from '../src/api/auth.js';
import RegisterView from '../src/views/RegisterView.vue';

vi.mock('../src/api/auth.js', () => ({
  register: vi.fn()
}));

describe('RegisterView', () => {
  it('submits a normal USER registration and returns to login', async () => {
    register.mockResolvedValue({ code: 200, data: { userName: 'writer' } });

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/register', component: RegisterView },
        { path: '/login', component: { template: '<div>login</div>' } }
      ]
    });
    router.push('/register');
    await router.isReady();

    const wrapper = mount(RegisterView, {
      global: {
        plugins: [router, ElementPlus]
      }
    });

    wrapper.vm.form.userName = 'writer';
    wrapper.vm.form.password = '123456';
    wrapper.vm.form.confirmPassword = '123456';
    wrapper.vm.form.nickName = 'Writer';
    wrapper.vm.form.email = 'writer@example.com';
    wrapper.vm.form.phone = '13800000099';
    await wrapper.vm.handleRegister();

    expect(register).toHaveBeenCalledWith({
      userName: 'writer',
      password: '123456',
      nickName: 'Writer',
      email: 'writer@example.com',
      phone: '13800000099'
    });
    expect(router.currentRoute.value.path).toBe('/login');
  });
});
