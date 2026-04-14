<template>
  <div class="chat-page">
    <h2 class="page-title">AI对话</h2>
    <div class="chat-container">
      <div class="chat-messages" ref="messagesContainer">
        <div class="message" v-for="(msg, index) in messages" :key="index" :class="msg.role">
          <div class="message-content">
            <div class="message-header">
              <span class="message-role">{{ msg.role === 'user' ? '我' : '阿古' }}</span>
              <span class="message-time">{{ msg.time }}</span>
            </div>
            <div class="message-body" v-html="msg.content"></div>
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
        <div class="model-selector">
          <label for="chat-model">回答模式</label>
          <select id="chat-model" v-model="selectedMode">
            <option value="normal">普通聊天模型</option>
            <option value="kb">知识库检索模型</option>
          </select>
        </div>
        <div class="input-row">
          <textarea
            v-model="inputMessage"
            placeholder="请输入您的问题..."
            @keyup.enter.exact="sendMessage"
            @keyup.enter.shift="inputMessage += '\n'"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="loading || !inputMessage.trim()">
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { chatApi } from '../api/chat'
import { marked } from 'marked'

const messages = ref([
  {
    role: 'ai',
    content: '你好！我是阿古，有什么我可以帮助你的吗？',
    time: new Date().toLocaleTimeString()
  }
])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const selectedMode = ref('normal')

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content,
    time: new Date().toLocaleTimeString()
  })
  
  inputMessage.value = ''
  loading.value = true
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  try {
    // 调用API
    const response = selectedMode.value === 'kb'
      ? await chatApi.sendKbMessage(content)
      : await chatApi.sendMessage(content)
    
    // 提取data字段的内容
    const aiContent = response.data || '抱歉，获取AI回复失败'
    
    // 解析markdown
    const htmlContent = marked(aiContent)
    
    // 添加AI回复
    messages.value.push({
      role: 'ai',
      content: htmlContent,
      time: new Date().toLocaleTimeString()
    })
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      role: 'ai',
      content: '抱歉，发送消息失败，请稍后重试',
      time: new Date().toLocaleTimeString()
    })
  } finally {
    loading.value = false
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 组件挂载后滚动到底部
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - 180px);
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #3d3d3d;
  margin: 0;
  text-align: center;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  max-width: 80%;
  margin-bottom: 12px;
}

.message.user {
  align-self: flex-end;
}

.message.ai {
  align-self: flex-start;
}

.message-content {
  background-color: #ede6d8;
  border-radius: 8px;
  padding: 12px 16px;
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
  margin-bottom: 8px;
  font-size: 12px;
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
  line-height: 1.5;
  font-size: 14px;
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
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #e0d5c5;
  background-color: #ede6d8;
}

.model-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-selector label {
  color: #6b6b6b;
  font-size: 13px;
  white-space: nowrap;
}

.model-selector select {
  width: 220px;
  padding: 8px 12px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 6px;
  color: #3d3d3d;
  font-size: 13px;
}

.model-selector select:focus {
  outline: none;
  border-color: #d4a574;
}

.input-row {
  display: flex;
  gap: 12px;
}

.chat-input textarea {
  width: 100%;
  padding: 12px 16px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 4px;
  color: #3d3d3d;
  font-size: 14px;
  resize: none;
  min-height: 60px;
  max-height: 200px;
  font-family: inherit;
}

.chat-input textarea:focus {
  outline: none;
  border-color: #d4a574;
}

.send-btn {
  padding: 0 24px;
  background-color: #d4a574;
  color: #3d3d3d;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
  align-self: flex-end;
}

.send-btn:hover:not(:disabled) {
  background-color: #c99564;
}

.send-btn:disabled {
  background-color: #e0d5c5;
  color: #9b9b9b;
  cursor: not-allowed;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f5f0e6;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d0c5b5;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #c7b9a6;
}
</style>