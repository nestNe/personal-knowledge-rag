<script setup>
import { ref } from 'vue'
import { authApi } from './api/auth'

// 切换状态：true为登录，false为注册
const isLogin = ref(true)

// 登录表单数据
const loginForm = ref({
  account: '', // 用户名或邮箱
  password: ''
})

// 注册表单数据
const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 加载状态
const loading = ref(false)

// 错误信息
const error = ref('')

// 成功信息
const success = ref('')

// 登录方法
const login = async () => {
  try {
    loading.value = true
    error.value = ''
    success.value = ''
    
    // 使用authApi调用登录接口
    const response = await authApi.login({
      account: loginForm.value.account,
      password: loginForm.value.password
    })
    
    // 处理登录成功
    console.log('登录成功:', response)
    // 存储token
    if (response.data && response.data.token) {
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('refreshToken', response.data.refreshToken)
    }
    // 显示成功信息
    success.value = '登录成功！'
    // 可以在这里处理页面跳转
    setTimeout(() => {
      success.value = ''
      // 这里可以添加跳转到首页的逻辑
    }, 2000)
  } catch (err) {
    // 处理登录失败
    error.value = err.response?.data?.message || '登录失败，请检查账号和密码'
    console.error('登录失败:', err)
  } finally {
    loading.value = false
  }
}

// 注册方法
const register = async () => {
  try {
    // 验证密码是否一致
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      error.value = '两次输入的密码不一致'
      return
    }
    
    loading.value = true
    error.value = ''
    success.value = ''
    
    // 使用authApi调用注册接口
    const response = await authApi.register({
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password
    })
    
    // 处理注册成功
    console.log('注册成功:', response)
    success.value = '注册成功！请登录'
    // 切换回登录表单
    setTimeout(() => {
      success.value = ''
      isLogin.value = true
    }, 2000)
  } catch (err) {
    // 处理注册失败
    error.value = err.response?.data?.message || '注册失败，请稍后重试'
    console.error('注册失败:', err)
  } finally {
    loading.value = false
  }
}

// 切换表单
const toggleForm = () => {
  isLogin.value = !isLogin.value
  error.value = '' // 切换时清空错误信息
  success.value = '' // 切换时清空成功信息
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>Agent Guard</h1>
        <p>AI 权限管理系统</p>
      </div>
      
      <!-- 登录表单 -->
      <form v-if="isLogin" @submit.prevent="login" class="login-form">
        <div class="form-group">
          <label for="login-account">账号</label>
          <input 
            type="text" 
            id="login-account" 
            v-model="loginForm.account" 
            placeholder="请输入用户名或邮箱"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="login-password">密码</label>
          <input 
            type="password" 
            id="login-password" 
            v-model="loginForm.password" 
            placeholder="请输入密码"
            required
          />
        </div>
        
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <div v-if="success" class="success-message">
          {{ success }}
        </div>
        
        <button 
          type="submit" 
          class="login-button" 
          :disabled="loading"
        >
          <span v-if="loading" class="loading-spinner"></span>
          {{ loading ? '登录中...' : '登录' }}
        </button>
        
        <div class="form-toggle">
          <span>还没有账号？</span>
          <button type="button" class="toggle-button" @click="toggleForm">
            立即注册
          </button>
        </div>
      </form>
      
      <!-- 注册表单 -->
      <form v-else @submit.prevent="register" class="login-form">
        <div class="form-group">
          <label for="register-username">用户名</label>
          <input 
            type="text" 
            id="register-username" 
            v-model="registerForm.username" 
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="register-email">邮箱</label>
          <input 
            type="email" 
            id="register-email" 
            v-model="registerForm.email" 
            placeholder="请输入邮箱"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="register-password">密码</label>
          <input 
            type="password" 
            id="register-password" 
            v-model="registerForm.password" 
            placeholder="请输入密码（6-20个字符）"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="confirm-password">确认密码</label>
          <input 
            type="password" 
            id="confirm-password" 
            v-model="registerForm.confirmPassword" 
            placeholder="请再次输入密码"
            required
          />
        </div>
        
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <div v-if="success" class="success-message">
          {{ success }}
        </div>
        
        <button 
          type="submit" 
          class="login-button" 
          :disabled="loading"
        >
          <span v-if="loading" class="loading-spinner"></span>
          {{ loading ? '注册中...' : '注册' }}
        </button>
        
        <div class="form-toggle">
          <span>已有账号？</span>
          <button type="button" class="toggle-button" @click="toggleForm">
            立即登录
          </button>
        </div>
      </form>
      
      <div class="login-footer">
        <p>© 2026 Agent Guard. 保留所有权利。</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
  transition: transform 0.3s ease;
}

.login-card:hover {
  transform: translateY(-2px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  color: #333;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.login-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  color: #555;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #4a90e2;
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1);
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 13px;
  margin-top: -10px;
  margin-bottom: 10px;
}

.success-message {
  background-color: #e8f5e8;
  color: #2e7d32;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 13px;
  margin-top: -10px;
  margin-bottom: 10px;
}

.login-button {
  background-color: #4a90e2;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.login-button:hover:not(:disabled) {
  background-color: #357abd;
  transform: translateY(-1px);
}

.login-button:disabled {
  background-color: #a0c4f1;
  cursor: not-allowed;
}

.form-toggle {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.toggle-button {
  background: none;
  border: none;
  color: #4a90e2;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: color 0.3s ease;
}

.toggle-button:hover {
  color: #357abd;
  text-decoration: underline;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  color: #999;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }
  
  .login-header h1 {
    font-size: 20px;
  }
  
  .form-toggle {
    font-size: 13px;
  }
}

/* 注册表单额外样式 */
.login-form:last-child .form-group {
  margin-bottom: 8px;
}
</style>
