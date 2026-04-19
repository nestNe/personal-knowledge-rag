<template>
  <div class="chat-page">
    <div class="left-rail-outer">
      <aside
        id="chat-left-rail"
        class="chat-left-rail"
        :class="{ 'is-collapsed': railCollapsed }"
        :aria-hidden="railCollapsed"
      >
        <div class="rail-inner">
          <div class="rail-logo">
            <router-link to="/chat" class="rail-logo-link" title="AI 对话">AGS</router-link>
          </div>

          <div class="rail-sessions">
            <template v-if="kbMode">
              <button type="button" class="new-session-btn" @click="startNewSession">
                + 新建会话
              </button>
              <div v-if="!loadingSessions" class="session-list">
                <button
                  v-for="s in sessions"
                  :key="s.sessionId"
                  type="button"
                  class="session-item"
                  :class="{ active: currentSessionId === s.sessionId && !isDraftSession }"
                  @click="selectSession(s.sessionId)"
                >
                  <span class="session-title">{{ s.title || '新会话' }}</span>
                  <span class="session-meta">{{ formatSessionTime(s.lastMessageTime || s.updatedAt) }}</span>
                </button>
                <p v-if="sessions.length === 0" class="session-empty">暂无历史会话</p>
              </div>
              <div v-else class="session-loading">加载中…</div>
            </template>
            <p v-else class="rail-mode-hint">
              当前为普通对话，不产生会话记录。切换为「知识库检索模型」可在此管理会话与记忆。
            </p>
          </div>

          <div class="rail-user">
            <UserExtensionMenu :show-chat-link="false" placement="top" />
          </div>
        </div>
      </aside>
      <button
        type="button"
        class="rail-toggle"
        :class="{ 'is-collapsed': railCollapsed }"
        :title="railCollapsed ? '展开会话栏' : '收起会话栏'"
        :aria-expanded="!railCollapsed"
        aria-controls="chat-left-rail"
        @click="railCollapsed = !railCollapsed"
      >
        <MenuFold v-if="!railCollapsed" theme="outline" size="18" fill="currentColor" />
        <MenuUnfold v-else theme="outline" size="18" fill="currentColor" />
      </button>
    </div>

    <div class="chat-right">
      <header class="chat-right-toolbar">
        <div class="toolbar-row">
          <div class="toolbar-title-block">
            <span class="toolbar-session-title" :title="chatToolbarTitle">{{ chatToolbarTitle }}</span>
          </div>
          <div class="toolbar-mode-wrap">
            <label class="toolbar-mode-label" for="chat-model">回答模式</label>
            <div class="mode-select-wrap">
              <select id="chat-model" v-model="selectedMode" class="mode-select-pill">
                <option value="kb">知识库检索</option>
                <option value="normal">普通聊天</option>
              </select>
            </div>
          </div>
        </div>
        <p v-if="!kbMode" class="toolbar-subhint">
          普通对话不保存会话记录；切换为「知识库检索」可使用左侧会话与记忆。
        </p>
      </header>

      <div class="chat-messages" ref="messagesContainer">
        <div
          class="message"
          v-for="(msg, index) in displayMessages"
          :key="index"
          :class="msg.role"
        >
          <div class="message-content">
            <div class="message-header">
              <span class="message-role">{{ msg.role === 'user' ? '我' : '阿古' }}</span>
              <span class="message-time">{{ msg.time }}</span>
            </div>
            <div
              class="message-body"
              :class="{ 'is-plain': msg.role === 'user' }"
              v-if="msg.role === 'user'"
            >{{ msg.content }}</div>
            <div class="message-body" v-else v-html="msg.content"></div>
          </div>
        </div>
        <div v-if="loading" class="message ai">
          <div class="message-content">
            <div class="message-header">
              <span class="message-role">阿古</span>
              <span class="message-time">{{ new Date().toLocaleTimeString() }}</span>
            </div>
            <div class="message-body loading">
              <div class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <div class="input-composer">
          <div class="input-shell">
            <textarea
              v-model="inputMessage"
              class="input-field"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              rows="1"
              @keyup.enter.exact="sendMessage"
              @keyup.enter.shift="inputMessage += '\n'"
            ></textarea>
            <button
              type="button"
              class="send-fab"
              :disabled="loading || !inputMessage.trim() || loadingSessionDetail"
              aria-label="发送"
              @click="sendMessage"
            >
              <ArrowUp theme="outline" size="22" fill="currentColor" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { chatApi } from '../api/chat'
import { marked } from 'marked'
import UserExtensionMenu from '../components/UserExtensionMenu.vue'
import { ArrowUp, MenuFold, MenuUnfold } from '@icon-park/vue-next'

const WELCOME_HTML = marked.parse(
  '你好！我是阿古，有什么我可以帮助你的吗？'
)

const selectedMode = ref('kb')
const kbMode = computed(() => selectedMode.value === 'kb')

const sessions = ref([])
const loadingSessions = ref(false)

/** 左侧会话栏是否收起 */
const railCollapsed = ref(false)

const currentSessionId = ref(null)
const isDraftSession = ref(true)

const messages = ref([
  {
    role: 'ai',
    content: WELCOME_HTML,
    time: formatTime(new Date())
  }
])

const inputMessage = ref('')
const loading = ref(false)
const loadingSessionDetail = ref(false)
const messagesContainer = ref(null)

const displayMessages = computed(() => messages.value)

/** 顶部标题：当前会话名 / 新对话 / 普通对话 */
const chatToolbarTitle = computed(() => {
  if (!kbMode.value) return '普通对话'
  if (isDraftSession.value && !currentSessionId.value) return '新对话'
  const s = sessions.value.find((x) => x.sessionId === currentSessionId.value)
  const t = s?.title?.trim()
  return t || '新会话'
})

function formatTime(d) {
  if (!d) return ''
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString()
}

function formatSessionTime(iso) {
  if (!iso) return ''
  return formatTime(iso)
}

function sortMemoriesByTime(list) {
  return [...(list || [])].sort(
    (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
  )
}

function memoriesToMessages(memories) {
  const sorted = sortMemoriesByTime(memories)
  return sorted.map((m) => {
    const isUser = m.type === 'USER'
    const raw = m.content || ''
    return {
      role: isUser ? 'user' : 'ai',
      content: isUser ? raw : marked.parse(raw),
      time: formatTime(m.timestamp)
    }
  })
}

async function fetchSessions() {
  loadingSessions.value = true
  try {
    const res = await chatApi.getSessions()
    if (res?.code === 200 && Array.isArray(res.data)) {
      sessions.value = res.data
    } else {
      sessions.value = []
    }
  } catch (e) {
    console.error('获取会话列表失败:', e)
    sessions.value = []
  } finally {
    loadingSessions.value = false
  }
}

async function loadSessionMessages(sessionId) {
  loadingSessionDetail.value = true
  try {
    const res = await chatApi.getSessionMessages(sessionId)
    if (res?.code === 200 && res.data?.chatMemories?.length) {
      messages.value = memoriesToMessages(res.data.chatMemories)
    } else if (res?.code === 200) {
      messages.value = [
        {
          role: 'ai',
          content: WELCOME_HTML,
          time: formatTime(new Date())
        }
      ]
    }
  } catch (e) {
    console.error('加载会话消息失败:', e)
    messages.value = [
      {
        role: 'ai',
        content: marked.parse('无法加载该会话，请稍后重试。'),
        time: formatTime(new Date())
      }
    ]
  } finally {
    loadingSessionDetail.value = false
    await nextTick()
    scrollToBottom()
  }
}

function startNewSession() {
  currentSessionId.value = null
  isDraftSession.value = true
  messages.value = [
    {
      role: 'ai',
      content: WELCOME_HTML,
      time: formatTime(new Date())
    }
  ]
  nextTick().then(scrollToBottom)
}

async function selectSession(sessionId) {
  if (currentSessionId.value === sessionId && !isDraftSession.value) return
  currentSessionId.value = sessionId
  isDraftSession.value = false
  await loadSessionMessages(sessionId)
}

function resolveSessionAfterCreate(previousIds, list) {
  const next = list || []
  const created = next.find((s) => !previousIds.has(s.sessionId))
  if (created) return created
  return next[0] || null
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value || loadingSessionDetail.value) return

  inputMessage.value = ''
  loading.value = true

  if (selectedMode.value === 'normal') {
    messages.value.push({
      role: 'user',
      content,
      time: formatTime(new Date())
    })
    await nextTick()
    scrollToBottom()
    try {
      const response = await chatApi.sendMessage(content)
      const aiContent =
        response?.code === 200 && response.data != null
          ? response.data
          : '抱歉，获取AI回复失败'
      const htmlContent = marked.parse(typeof aiContent === 'string' ? aiContent : String(aiContent))
      messages.value.push({
        role: 'ai',
        content: htmlContent,
        time: formatTime(new Date())
      })
    } catch (error) {
      console.error('发送消息失败:', error)
      messages.value.push({
        role: 'ai',
        content: marked.parse('抱歉，发送消息失败，请稍后重试'),
        time: formatTime(new Date())
      })
    } finally {
      loading.value = false
      await nextTick()
      scrollToBottom()
    }
    return
  }

  // 知识库模式
  const previousIds = new Set(sessions.value.map((s) => s.sessionId))
  messages.value.push({
    role: 'user',
    content,
    time: formatTime(new Date())
  })
  await nextTick()
  scrollToBottom()

  try {
    const response = await chatApi.sendKbMessage(
      content,
      isDraftSession.value ? undefined : currentSessionId.value || undefined
    )
    const aiText =
      response?.code === 200 && response.data != null
        ? response.data
        : '抱歉，获取AI回复失败'
    const htmlContent = marked.parse(typeof aiText === 'string' ? aiText : String(aiText))

    if (isDraftSession.value) {
      await fetchSessions()
      const session = resolveSessionAfterCreate(previousIds, sessions.value)
      if (session?.sessionId) {
        currentSessionId.value = session.sessionId
        isDraftSession.value = false
        await loadSessionMessages(session.sessionId)
        return
      }
      messages.value.push({
        role: 'ai',
        content: htmlContent,
        time: formatTime(new Date())
      })
    } else {
      messages.value.push({
        role: 'ai',
        content: htmlContent,
        time: formatTime(new Date())
      })
      await fetchSessions()
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      role: 'ai',
      content: marked.parse('抱歉，发送消息失败，请稍后重试'),
      time: formatTime(new Date())
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(() => {
  fetchSessions()
  scrollToBottom()
})

watch(kbMode, (kb) => {
  if (kb) {
    fetchSessions()
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: row;
  flex: 1;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  background-color: #f5f0e6;
}

/* 左侧：Logo + 会话 + 底部用户菜单 + 收展 */
.left-rail-outer {
  display: flex;
  flex-direction: row;
  flex-shrink: 0;
  align-items: stretch;
  min-height: 0;
}

.chat-left-rail {
  flex-shrink: 0;
  width: clamp(248px, 26vw, 300px);
  min-width: 0;
  max-width: 300px;
  min-height: 0;
  overflow: hidden;
  background-color: #f5f0e6;
  border-right: 1px solid #e0d5c5;
  box-sizing: border-box;
  transition:
    width 0.42s cubic-bezier(0.32, 0.72, 0, 1),
    border-color 0.35s ease;
}

.chat-left-rail.is-collapsed {
  width: 0;
  max-width: 0;
  border-right-color: transparent;
}

.rail-inner {
  display: flex;
  flex-direction: column;
  width: clamp(248px, 26vw, 300px);
  max-width: 300px;
  min-height: 100%;
  height: 100%;
  box-sizing: border-box;
}

.rail-toggle {
  flex-shrink: 0;
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  min-height: 56px;
  margin: 0;
  padding: 0;
  border: 1px solid #e0d5c5;
  border-left: none;
  border-radius: 0 10px 10px 0;
  background: linear-gradient(180deg, #faf6f0 0%, #f3ebe0 100%);
  color: #6b6b6b;
  cursor: pointer;
  box-shadow: 2px 0 8px rgba(61, 61, 61, 0.06);
  transition:
    background 0.25s ease,
    color 0.2s ease,
    box-shadow 0.3s ease,
    border-color 0.25s ease,
    transform 0.2s ease;
  z-index: 2;
}

.rail-toggle:hover {
  color: #3d3d3d;
  background: #ede6d8;
  border-color: #d0c5b5;
  box-shadow: 2px 0 12px rgba(61, 61, 61, 0.1);
}

.rail-toggle:active {
  transform: scale(0.97);
}

.rail-toggle.is-collapsed {
  border-left: 1px solid #e0d5c5;
  border-radius: 0 10px 10px 0;
  margin-left: 0;
}

.rail-toggle:focus-visible {
  outline: 2px solid #d4a574;
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .chat-left-rail {
    transition-duration: 0.01ms !important;
  }
}

.rail-logo {
  flex-shrink: 0;
  padding: 20px 16px 16px;
  border-bottom: 1px solid #e0d5c5;
}

.rail-logo-link {
  font-size: 24px;
  font-weight: 700;
  color: #8b7355;
  text-decoration: none;
  letter-spacing: 0.04em;
  display: inline-block;
}

.rail-logo-link:hover {
  color: #6b553f;
}

.rail-sessions {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
  overflow: hidden;
}

.rail-mode-hint {
  font-size: 13px;
  color: #6b6b6b;
  line-height: 1.55;
  margin: 0;
}

.new-session-btn {
  width: 100%;
  padding: 12px 14px;
  margin-bottom: 12px;
  background-color: #d4a574;
  color: #3d3d3d;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.new-session-btn:hover {
  background-color: #c99564;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  color: #3d3d3d;
  font-size: 14px;
  transition: background 0.15s;
}

.session-item:hover {
  background: #ede6d8;
}

.session-item.active {
  background: #d4a574;
  border-color: #c99564;
}

.session-title {
  font-weight: 600;
  color: #3d3d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}

.session-meta {
  font-size: 11px;
  color: #6b6b6b;
}

.session-empty,
.session-loading {
  font-size: 12px;
  color: #6b6b6b;
  padding: 8px 4px;
}

.rail-user {
  flex-shrink: 0;
  padding: 12px;
  border-top: 1px solid #e0d5c5;
  background-color: #f5f0e6;
}

/* 右侧：与页面同底色，无大卡片外框；气泡区略收窄以视觉居中 */
.chat-right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: #f5f0e6;
}

/* 顶部工具栏：与页面背景一致 */
.chat-right-toolbar {
  flex-shrink: 0;
  width: 100%;
  max-width: min(1040px, 100%);
  margin: 0 auto;
  box-sizing: border-box;
  padding: 14px clamp(20px, 2.5vw, 40px) 12px;
  background-color: #f5f0e6;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-title-block {
  min-width: 0;
  flex: 1;
}

.toolbar-session-title {
  display: block;
  font-size: 17px;
  font-weight: 600;
  color: #3d3d3d;
  letter-spacing: 0.02em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-mode-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toolbar-mode-label {
  font-size: 12px;
  color: #6b6b6b;
  white-space: nowrap;
}

.mode-select-wrap {
  position: relative;
}

.mode-select-wrap::after {
  content: '';
  pointer-events: none;
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid #8b7355;
}

.mode-select-pill {
  appearance: none;
  -webkit-appearance: none;
  margin: 0;
  padding: 9px 36px 9px 16px;
  min-width: 128px;
  font-size: 14px;
  font-family: inherit;
  color: #3d3d3d;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 999px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.mode-select-pill:hover {
  border-color: #c7b9a6;
}

.mode-select-pill:focus {
  outline: none;
  border-color: #d4a574;
  box-shadow: 0 0 0 2px rgba(212, 165, 116, 0.25);
}

.toolbar-subhint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #6b6b6b;
  line-height: 1.45;
}

.chat-messages {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  max-width: min(1040px, 100%);
  margin: 0 auto;
  padding: clamp(12px, 1.5vh, 20px) clamp(20px, 2.5vw, 40px) clamp(16px, 2vh, 24px);
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background-color: #f5f0e6;
  -webkit-overflow-scrolling: touch;
  box-sizing: border-box;
}

.message {
  max-width: min(88%, 52rem);
  margin-bottom: 12px;
}

.message.user {
  align-self: flex-end;
}

.message.ai {
  align-self: flex-start;
}

.message-content {
  background-color: #faf6f0;
  border-radius: 10px;
  padding: 14px 18px;
  border: 1px solid #e0d5c5;
}

.message.user .message-content {
  background-color: #f4e9d8;
  border-color: #d4a574;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
}

.message-role {
  font-weight: 600;
  color: #8b7355;
}

.message-time {
  color: #6b6b6b;
}

.message-body {
  color: #3d3d3d;
  line-height: 1.65;
  font-size: 16px;
}

.message-body :deep(p) {
  margin: 0.4em 0;
}

.message-body :deep(p:first-child) {
  margin-top: 0;
}

.message-body :deep(p:last-child) {
  margin-bottom: 0;
}

.message-body.is-plain {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-body.loading {
  min-height: 20px;
}

.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background-color: #d4a574;
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  z-index: 3;
  width: 100%;
  max-width: min(1040px, 100%);
  margin: 0 auto;
  box-sizing: border-box;
  padding: 12px clamp(20px, 2.5vw, 40px) clamp(16px, 2.5vh, 28px);
  background-color: #f5f0e6;
}

.input-composer {
  width: 100%;
}

.input-shell {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 12px 10px 18px;
  min-height: 52px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 24px;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-shell:focus-within {
  border-color: #d4a574;
  box-shadow: 0 0 0 2px rgba(212, 165, 116, 0.2);
}

.input-field {
  flex: 1;
  min-width: 0;
  min-height: 28px;
  max-height: min(36vh, 240px);
  padding: 6px 4px 6px 0;
  margin: 0;
  border: none;
  background: transparent;
  color: #3d3d3d;
  font-size: 16px;
  line-height: 1.5;
  resize: none;
  font-family: inherit;
  outline: none;
}

.input-field::placeholder {
  color: #a09080;
}

.send-fab {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  margin-bottom: 2px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background-color: #d4a574;
  color: #3d3d3d;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.15s, opacity 0.2s;
}

.send-fab:hover:not(:disabled) {
  background-color: #c99564;
}

.send-fab:active:not(:disabled) {
  transform: scale(0.96);
}

.send-fab:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background-color: #e0d5c5;
}

.chat-messages::-webkit-scrollbar,
.session-list::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track,
.session-list::-webkit-scrollbar-track {
  background: #f5f0e6;
}

.chat-messages::-webkit-scrollbar-thumb,
.session-list::-webkit-scrollbar-thumb {
  background: #d0c5b5;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.session-list::-webkit-scrollbar-thumb:hover {
  background: #c7b9a6;
}
</style>
