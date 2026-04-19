<template>
  <div ref="userMenuRef" class="user-menu-wrap" :class="{ 'placement-top': placement === 'top' }">
    <button
      type="button"
      class="user-trigger"
      :aria-expanded="menuOpen"
      aria-haspopup="true"
      @click.stop="menuOpen = !menuOpen"
    >
      <span class="user-name">{{ displayName }}</span>
      <Down class="caret" :class="{ open: menuOpen }" theme="outline" size="18" fill="currentColor" />
    </button>
    <transition name="dropdown-fade">
      <div v-show="menuOpen" class="dropdown-panel" role="menu" @click.stop>
        <div v-if="showChatLink && route.name !== 'Chat'" class="dropdown-block">
          <router-link to="/chat" class="dropdown-item highlight" role="menuitem" @click="closeMenu">
            <span class="item-icon"><MessageOne theme="outline" size="16" fill="currentColor" /></span>
            AI 对话
          </router-link>
        </div>
        <div class="dropdown-block">
          <span class="dropdown-heading">扩展功能</span>
          <router-link to="/dashboard" class="dropdown-item" role="menuitem" @click="closeMenu">
            <span class="item-icon"><Home theme="outline" size="16" fill="currentColor" /></span>
            控制台概览
          </router-link>
          <router-link
            v-if="userStore.hasPermission('user:list')"
            to="/users"
            class="dropdown-item"
            role="menuitem"
            @click="closeMenu"
          >
            <span class="item-icon"><EveryUser theme="outline" size="16" fill="currentColor" /></span>
            用户管理
          </router-link>
          <router-link
            v-if="userStore.hasPermission('role:list')"
            to="/roles"
            class="dropdown-item"
            role="menuitem"
            @click="closeMenu"
          >
            <span class="item-icon"><Peoples theme="outline" size="16" fill="currentColor" /></span>
            角色管理
          </router-link>
          <router-link
            v-if="userStore.hasPermission('perm:list')"
            to="/permissions"
            class="dropdown-item"
            role="menuitem"
            @click="closeMenu"
          >
            <span class="item-icon"><Lock theme="outline" size="16" fill="currentColor" /></span>
            权限管理
          </router-link>
          <router-link
            v-if="userStore.hasPermission('agent:embedding')"
            to="/knowledge-base"
            class="dropdown-item"
            role="menuitem"
            @click="closeMenu"
          >
            <span class="item-icon"><BookOne theme="outline" size="16" fill="currentColor" /></span>
            知识库
          </router-link>
        </div>
        <div class="dropdown-footer">
          <button type="button" class="logout-btn" role="menuitem" @click="onLogout">退出登录</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Home, EveryUser, Peoples, Lock, MessageOne, BookOne, Down } from '@icon-park/vue-next'
import { useUserStore } from '../stores/user'
import { authApi } from '../api/auth'

defineProps({
  /** 是否在菜单顶部显示「AI 对话」入口（聊天页内为 false） */
  showChatLink: { type: Boolean, default: true },
  /** bottom：在按钮下方展开；top：在按钮上方展开（左下角用户名适用） */
  placement: { type: String, default: 'bottom' }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuOpen = ref(false)
const userMenuRef = ref(null)

const displayName = computed(() => {
  const u = userStore.userInfo
  if (!u) return '用户'
  return u.username || u.email || u.account || '用户'
})

function closeMenu() {
  menuOpen.value = false
}

watch(
  () => route.fullPath,
  () => {
    closeMenu()
  }
)

function onDocClick(e) {
  if (!menuOpen.value) return
  const el = userMenuRef.value
  if (el && !el.contains(e.target)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
})

const onLogout = async () => {
  closeMenu()
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
.user-menu-wrap {
  position: relative;
  width: 100%;
}

.user-menu-wrap.placement-top .dropdown-panel {
  top: auto;
  bottom: calc(100% + 8px);
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  background: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 10px;
  color: #3d3d3d;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
  box-sizing: border-box;
}

.user-trigger:hover {
  background: #f0e8dc;
  border-color: #d4a574;
}

.user-name {
  flex: 1;
  min-width: 0;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  flex-shrink: 0;
  transition: transform 0.2s;
  color: #6b6b6b;
}

.caret.open {
  transform: rotate(180deg);
}

.dropdown-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 260px;
  padding: 8px 0;
  background: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(61, 61, 61, 0.12);
  z-index: 200;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.placement-top .dropdown-fade-enter-from,
.placement-top .dropdown-fade-leave-to {
  transform: translateY(6px);
}

.dropdown-block {
  padding: 4px 0;
  border-bottom: 1px solid #e0d5c5;
}

.dropdown-block:last-of-type {
  border-bottom: none;
}

.dropdown-heading {
  display: block;
  padding: 8px 16px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #9b8b7a;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: #3d3d3d;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.15s;
}

.dropdown-item:hover {
  background: #e5dcc8;
}

.dropdown-item.highlight {
  color: #6b553f;
  font-weight: 500;
}

.item-icon {
  display: inline-flex;
  color: #8b7355;
}

.dropdown-footer {
  padding: 8px 12px 4px;
}

.logout-btn {
  width: 100%;
  padding: 10px 12px;
  background-color: #d4a574;
  border: 1px solid #c99564;
  color: #3d3d3d;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.logout-btn:hover {
  background-color: #c99564;
}
</style>
