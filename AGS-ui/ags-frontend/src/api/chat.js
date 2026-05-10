import service from './axios'

const CHAT_TIMEOUT_MS = 120000

// AI对话相关接口（与 ChatController 一致）
export const chatApi = {
  /**
   * POST /api/ai/chat — 统一聊天（Manager 模型），不传 sessionId 时服务端创建新会话
   * @param {string} message
   * @param {string} [sessionId]
   */
  sendMessage: (message, sessionId) => {
    let url = `/ai/chat?message=${encodeURIComponent(message)}`
    if (sessionId) {
      url += `&sessionId=${encodeURIComponent(sessionId)}`
    }
    return service.post(url, {}, { timeout: CHAT_TIMEOUT_MS })
  },

  /** GET /api/ai/sessions — 当前用户会话列表 */
  getSessions: () => {
    return service.get('/ai/sessions')
  },

  /** GET /api/ai/sessions/{sessionId}/messages — 会话详情与聊天记录 */
  getSessionMessages: (sessionId) => {
    return service.get(`/ai/sessions/${encodeURIComponent(sessionId)}/messages`)
  }
}