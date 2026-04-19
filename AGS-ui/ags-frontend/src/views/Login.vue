<template>
  <div class="login-page">
    <div class="login-container">
      <h1 class="login-title">登录</h1>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="account">账号</label>
          <input
            type="text"
            id="account"
            v-model="form.account"
            placeholder="请输入用户名或邮箱"
            required
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            placeholder="请输入密码"
            required
          />
        </div>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  account: '',
  password: ''
})

// 处理登录
const handleLogin = async () => {
  try {
    loading.value = true
    const response = await authApi.login(form)
    
    const { accessToken, refreshToken, userInfo } = response.data
    const permissions = userInfo.permissions || []
    
    // 保存用户信息
    userStore.setUserInfo(userInfo, accessToken, refreshToken, permissions)
    
    // 跳转到原页面或 AI 对话
    const redirect = route.query.redirect || '/chat'
    router.push(redirect)
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f0e6;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(139, 115, 85, 0.14);
}

.login-title {
  text-align: center;
  color: #8b7355;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: #6b6b6b;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  padding: 12px 16px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 8px;
  color: #3d3d3d;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #d4a574;
}

.login-btn {
  padding: 12px;
  background-color: #d4a574;
  color: #3d3d3d;
  border: 1px solid #c99564;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: 10px;
}

.login-btn:hover {
  background-color: #c99564;
}

.login-btn:disabled {
  background-color: #e0d5c5;
  border-color: #e0d5c5;
  color: #9b9b9b;
  cursor: not-allowed;
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  font-size: 14px;
  color: #6b6b6b;
}

.form-footer a {
  color: #8b7355;
  text-decoration: none;
  transition: color 0.2s;
}

.form-footer a:hover {
  color: #d4a574;
}
</style>