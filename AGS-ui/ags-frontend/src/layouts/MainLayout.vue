<template>
  <div class="main-layout">
    <header class="top-nav">
      <div class="top-nav-main">
        <h1 class="logo">AGS</h1>
        <nav class="feature-nav">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <span class="nav-icon">
              <Home theme="outline" size="16" fill="currentColor" />
            </span>
            <span class="nav-text">首页</span>
          </router-link>
          <router-link to="/users" class="nav-item" active-class="active">
            <span class="nav-icon">
              <EveryUser theme="outline" size="16" fill="currentColor" />
            </span>
            <span class="nav-text">用户管理</span>
          </router-link>
          <router-link to="/roles" class="nav-item" active-class="active">
            <span class="nav-icon">
              <Peoples theme="outline" size="16" fill="currentColor" />
            </span>
            <span class="nav-text">角色管理</span>
          </router-link>
          <router-link to="/permissions" class="nav-item" active-class="active">
            <span class="nav-icon">
              <Lock theme="outline" size="16" fill="currentColor" />
            </span>
            <span class="nav-text">权限管理</span>
          </router-link>
          <router-link to="/chat" class="nav-item" active-class="active">
            <span class="nav-icon">
              <MessageOne theme="outline" size="16" fill="currentColor" />
            </span>
            <span class="nav-text">AI对话</span>
          </router-link>
        </nav>
        <div class="user-actions">
          <span class="user-email">{{ userStore.userInfo?.email }}</span>
          <button class="logout-btn" @click="logout">退出</button>
        </div>
      </div>
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span v-if="$route.name !== 'Dashboard'"> / {{ $route.meta.title }}</span>
      </div>
    </header>

    <main class="content">
      <div class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { Home, EveryUser, Peoples, Lock, MessageOne } from '@icon-park/vue-next'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

// 退出登录
const logout = async () => {
  try {
    await authApi.logout()
  } catch (error) {
    console.error('登出失败:', error)
  } finally {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: #f5f0e6;
  color: #3d3d3d;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background-color: #ede6d8;
  border-bottom: 1px solid #e0d5c5;
  padding: 14px 24px 10px;
}

.top-nav-main {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #8b7355;
  margin: 0;
}

.feature-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex: 1;
  flex-wrap: wrap;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  color: #6b6b6b;
  text-decoration: none;
  border-radius: 999px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: #e5dcc8;
  color: #3d3d3d;
}

.nav-item.active {
  background-color: #d4a574;
  color: #3d3d3d;
  border-color: #c99564;
}

.nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.breadcrumb {
  max-width: 1280px;
  margin: 10px auto 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb a {
  color: #8b7355;
  text-decoration: none;
}

.breadcrumb span {
  color: #6b6b6b;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-email {
  color: #6b6b6b;
  font-size: 14px;
}

.logout-btn {
  background-color: #d4a574;
  border: 1px solid #c99564;
  color: #3d3d3d;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
}

.logout-btn:hover {
  background-color: #c99564;
}

.content {
  width: 100%;
}

.page-content {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px;
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 992px) {
  .top-nav-main {
    flex-direction: column;
    align-items: stretch;
  }

  .feature-nav {
    justify-content: flex-start;
  }

  .user-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .top-nav,
  .page-content {
    padding-left: 14px;
    padding-right: 14px;
  }
}
</style>