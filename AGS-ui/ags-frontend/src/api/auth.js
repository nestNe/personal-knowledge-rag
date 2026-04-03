import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 统一处理错误
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，跳转到登录页
          break
        case 403:
          // 禁止访问
          break
        case 404:
          // 接口不存在
          break
        case 500:
          // 服务器错误
          break
        default:
          // 其他错误
          break
      }
    }
    return Promise.reject(error)
  }
)

// 认证相关API
export const authApi = {
  // 注册
  register: (data) => {
    return api.post('/auth/register', data)
  },
  
  // 登录
  login: (data) => {
    return api.post('/auth/login', data)
  },
  
  // 刷新token
  refreshToken: (refreshToken) => {
    return api.post('/auth/refresh', {}, {
      headers: {
        'X-Refresh-Token': refreshToken
      }
    })
  },
  
  // 登出
  logout: () => {
    return api.post('/auth/logout')
  },
  
  // 获取当前用户信息
  getCurrentUser: () => {
    return api.get('/auth/me')
  }
}

export default api