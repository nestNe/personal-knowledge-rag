<template>
  <div class="main-layout" :class="{ 'layout-chat': route.name === 'Chat' }">
    <!-- 聊天页：仅渲染子路由，由 Chat.vue 自管左右分栏 -->
    <template v-if="route.name === 'Chat'">
      <main class="content content-chat-only">
        <div class="chat-route-shell">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </template>

    <template v-else>
      <header class="top-nav">
        <div class="top-nav-main">
          <router-link to="/chat" class="logo" title="AI 对话">AGS</router-link>
          <UserExtensionMenu :show-chat-link="true" placement="bottom" />
        </div>
        <div class="breadcrumb">
          <router-link to="/chat">AI 对话</router-link>
          <span class="sep">/</span>
          <span class="crumb-current">{{ route.meta.title }}</span>
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
    </template>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import UserExtensionMenu from '../components/UserExtensionMenu.vue'

const route = useRoute()
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f0e6;
  color: #3d3d3d;
}

.main-layout.layout-chat {
  height: 100vh;
  height: 100dvh;
  max-height: 100vh;
  max-height: 100dvh;
  overflow: hidden;
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

.top-nav-main :deep(.user-menu-wrap) {
  width: auto;
}

.top-nav-main :deep(.user-trigger) {
  width: auto;
  border-radius: 999px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #8b7355;
  margin: 0;
  text-decoration: none;
  letter-spacing: 0.02em;
}

.logo:hover {
  color: #6b553f;
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

.breadcrumb a:hover {
  text-decoration: underline;
}

.sep {
  color: #c7b9a6;
}

.crumb-current {
  color: #6b6b6b;
}

.content {
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.content-chat-only {
  padding: 0;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.chat-route-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-route-shell > * {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.page-content {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px;
}

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

@media (max-width: 640px) {
  .top-nav,
  .page-content {
    padding-left: 14px;
    padding-right: 14px;
  }
}
</style>
