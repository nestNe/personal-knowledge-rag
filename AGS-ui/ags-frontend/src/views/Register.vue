<template>
  <div class="register-page">
    <div class="register-container">
      <h1 class="register-title">注册</h1>
      <form @submit.prevent="handleRegister" class="register-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            placeholder="请输入用户名"
            required
          />
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            type="email"
            id="email"
            v-model="form.email"
            placeholder="请输入邮箱"
            required
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            placeholder="请输入密码（6-20个字符）"
            required
          />
        </div>
        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  email: '',
  password: ''
})

// 处理注册
const handleRegister = async () => {
  try {
    loading.value = true
    await authApi.register(form)
    
    // 注册成功后跳转到登录页
    alert('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
    alert('注册失败，请检查信息')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f0e6;
}

.register-container {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(139, 115, 85, 0.14);
}

.register-title {
  text-align: center;
  color: #8b7355;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.register-form {
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

.register-btn {
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

.register-btn:hover {
  background-color: #c99564;
}

.register-btn:disabled {
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