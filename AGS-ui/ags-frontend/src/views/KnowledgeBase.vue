<template>
  <div class="kb-page">
    <h2 class="page-title">知识库</h2>

    <div class="panel">
      <div class="panel-header">
        <div class="panel-title">导入到知识库</div>
        <div class="panel-subtitle">支持 .txt 文件；导入后将按段落向量化入库</div>
      </div>

      <div class="import-grid">
        <div class="form-item">
          <label>分类（type）</label>
          <input v-model="type" placeholder="例如：Java / MySQL / 个人信息" />
        </div>

        <div class="form-item">
          <label>单文件导入</label>
          <div class="row">
            <input ref="singleFileInput" type="file" accept=".txt" @change="onPickSingle" />
            <button class="btn primary" :disabled="importing || !singleFile || !type.trim()" @click="doImportSingle">
              导入
            </button>
          </div>
          <div class="hint" v-if="singleFile">{{ singleFile.name }}</div>
        </div>

        <div class="form-item">
          <label>多文件导入</label>
          <div class="row">
            <input ref="multiFileInput" type="file" accept=".txt" multiple @change="onPickMulti" />
            <button class="btn primary" :disabled="importing || multiFiles.length === 0 || !type.trim()" @click="doImportMulti">
              批量导入
            </button>
          </div>
          <div class="hint" v-if="multiFiles.length > 0">已选择 {{ multiFiles.length }} 个文件</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header list-header">
        <div>
          <div class="panel-title">我的知识库</div>
          <div class="panel-subtitle">支持关键词模糊搜索（内容/来源/分类）</div>
        </div>
        <div class="actions">
          <div class="search-box">
            <input v-model="keyword" placeholder="搜索关键词" @keyup.enter="search" />
            <button class="btn subtle" @click="search">搜索</button>
          </div>
          <button class="btn subtle" :disabled="loading" @click="refresh">刷新</button>
        </div>
      </div>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th style="width: 180px;">来源</th>
              <th style="width: 120px;">分类</th>
              <th style="width: 120px;">行信息</th>
              <th style="width: 180px;">上传时间</th>
              <th>内容摘要</th>
              <th style="width: 120px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in records" :key="row.id">
              <td class="muted">{{ row.metaData?.source || '-' }}</td>
              <td>
                <span class="tag">{{ row.metaData?.category || '-' }}</span>
              </td>
              <td class="muted">
                {{ row.metaData?.lineNumber || 0 }}/{{ row.metaData?.totalLines || 0 }}
              </td>
              <td class="muted">{{ formatTime(row.metaData?.uploadTime) }}</td>
              <td>
                <div class="snippet" :title="row.content">
                  {{ snippet(row.content) }}
                </div>
              </td>
              <td>
                <button class="btn small" @click="openDetail(row.id)">查看</button>
              </td>
            </tr>

            <tr v-if="loading">
              <td colspan="6" class="state">加载中...</td>
            </tr>
            <tr v-else-if="records.length === 0">
              <td colspan="6" class="state">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="btn subtle" :disabled="current === 1 || loading" @click="changePage(current - 1)">上一页</button>
        <div class="page-info">第 {{ current }} 页 / 共 {{ pages }} 页（{{ total }} 条）</div>
        <button class="btn subtle" :disabled="current >= pages || loading" @click="changePage(current + 1)">下一页</button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog" v-if="showDetail">
      <div class="dialog-content">
        <div class="dialog-header">
          <h3>内容详情</h3>
          <button class="close-btn" @click="closeDetail">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="detail-meta">
            <div class="meta-item"><span class="meta-label">来源</span><span class="meta-value">{{ detail?.metaData?.source || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">分类</span><span class="meta-value">{{ detail?.metaData?.category || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">行信息</span><span class="meta-value">{{ detail?.metaData?.lineNumber || 0 }}/{{ detail?.metaData?.totalLines || 0 }}</span></div>
            <div class="meta-item"><span class="meta-label">上传时间</span><span class="meta-value">{{ formatTime(detail?.metaData?.uploadTime) }}</span></div>
          </div>
          <pre class="detail-text">{{ detail?.content || '' }}</pre>
        </div>
        <div class="dialog-footer">
          <button class="btn subtle" @click="closeDetail">关闭</button>
        </div>
      </div>
    </div>

    <!-- 消息提示 -->
    <div class="message" :class="messageType" v-if="showMessage">{{ message }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { embeddingApi } from '../api/embedding'

const type = ref('')
const importing = ref(false)
const singleFile = ref(null)
const multiFiles = ref([])
const singleFileInput = ref(null)
const multiFileInput = ref(null)

const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const pages = ref(1)
const records = ref([])
const loading = ref(false)

const showDetail = ref(false)
const detail = ref(null)

const message = ref('')
const showMessage = ref(false)
const messageType = ref('success')

const showMsg = (msg, type = 'success') => {
  message.value = msg
  messageType.value = type
  showMessage.value = true
  setTimeout(() => (showMessage.value = false), 3000)
}

const onPickSingle = (e) => {
  const file = e?.target?.files?.[0]
  singleFile.value = file || null
}

const onPickMulti = (e) => {
  const files = Array.from(e?.target?.files || [])
  multiFiles.value = files
}

const doImportSingle = async () => {
  if (!singleFile.value || !type.value.trim()) return
  importing.value = true
  try {
    const res = await embeddingApi.embedBatch(singleFile.value, type.value.trim())
    if (res?.code === 200) {
      showMsg('导入成功')
      singleFile.value = null
      if (singleFileInput.value) singleFileInput.value.value = ''
      refresh()
    } else {
      showMsg(res?.message || '导入失败', 'error')
    }
  } catch (err) {
    console.error(err)
    showMsg('导入失败', 'error')
  } finally {
    importing.value = false
  }
}

const doImportMulti = async () => {
  if (multiFiles.value.length === 0 || !type.value.trim()) return
  importing.value = true
  try {
    const res = await embeddingApi.embedBatches(multiFiles.value, type.value.trim())
    if (res?.code === 200) {
      showMsg('批量导入成功')
      multiFiles.value = []
      if (multiFileInput.value) multiFileInput.value.value = ''
      refresh()
    } else {
      showMsg(res?.message || '批量导入失败', 'error')
    }
  } catch (err) {
    console.error(err)
    showMsg('批量导入失败', 'error')
  } finally {
    importing.value = false
  }
}

const fetchPage = async () => {
  loading.value = true
  try {
    const res = await embeddingApi.page({
      current: current.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    if (res?.code === 200) {
      const data = res.data || {}
      records.value = data.records || []
      total.value = data.total || 0
      pages.value = data.pages || 1
    } else {
      showMsg(res?.message || '获取列表失败', 'error')
    }
  } catch (err) {
    console.error(err)
    showMsg('获取列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const refresh = () => fetchPage()

const search = () => {
  current.value = 1
  fetchPage()
}

const changePage = (p) => {
  if (p < 1 || p > pages.value) return
  current.value = p
  fetchPage()
}

const openDetail = async (id) => {
  showDetail.value = true
  detail.value = null
  try {
    const res = await embeddingApi.detail(id)
    if (res?.code === 200) {
      detail.value = res.data
    } else {
      showMsg(res?.message || '获取详情失败', 'error')
      showDetail.value = false
    }
  } catch (err) {
    console.error(err)
    showMsg('获取详情失败', 'error')
    showDetail.value = false
  }
}

const closeDetail = () => {
  showDetail.value = false
  detail.value = null
}

const snippet = (text) => {
  const s = (text || '').toString().replace(/\s+/g, ' ').trim()
  if (!s) return '-'
  const max = 120
  return s.length > max ? s.slice(0, max) + '…' : s
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.toString().replace('T', ' ').replace('Z', '')
}

onMounted(() => {
  fetchPage()
})
</script>

<style scoped>
.kb-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1200px;
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

.panel {
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  overflow: hidden;
}

.panel-header {
  padding: 16px 18px;
  border-bottom: 1px solid #e0d5c5;
  background-color: #ede6d8;
}

.list-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #3d3d3d;
}

.panel-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #6b6b6b;
}

.import-grid {
  padding: 18px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 14px;
}

.form-item label {
  display: block;
  font-size: 13px;
  color: #6b6b6b;
  margin-bottom: 8px;
}

.form-item input[type="text"],
.form-item input:not([type="file"]) {
  width: 100%;
  padding: 10px 12px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 8px;
  color: #3d3d3d;
  font-size: 14px;
}

.form-item input:focus {
  outline: none;
  border-color: #d4a574;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hint {
  margin-top: 8px;
  font-size: 12px;
  color: #6b6b6b;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-box input {
  width: 260px;
  padding: 10px 12px;
  background-color: #faf6f0;
  border: 1px solid #d0c5b5;
  border-radius: 999px;
  color: #3d3d3d;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #d4a574;
}

.btn {
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #d0c5b5;
  background-color: #ede6d8;
  color: #3d3d3d;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background-color: #e5dcc8;
  border-color: #d4a574;
}

.btn:disabled {
  background-color: #e0d5c5;
  border-color: #e0d5c5;
  color: #9b9b9b;
  cursor: not-allowed;
}

.btn.primary {
  background-color: #d4a574;
  border-color: #c99564;
}

.btn.primary:hover:not(:disabled) {
  background-color: #c99564;
}

.btn.subtle {
  border-radius: 999px;
}

.btn.small {
  padding: 8px 10px;
  border-radius: 999px;
}

.table-wrap {
  overflow: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid #e0d5c5;
  vertical-align: top;
}

.table th {
  background-color: #ede6d8;
  color: #6b6b6b;
  font-weight: 600;
  font-size: 13px;
  position: sticky;
  top: 0;
  z-index: 1;
}

.table tr:hover td {
  background-color: #f4e9d8;
}

.muted {
  color: #6b6b6b;
}

.tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background-color: #ede6d8;
  border: 1px solid #e0d5c5;
  color: #8b7355;
  font-size: 12px;
}

.snippet {
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #3d3d3d;
}

.state {
  text-align: center;
  padding: 30px 0;
  color: #6b6b6b;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 16px 18px;
}

.page-info {
  color: #6b6b6b;
  font-size: 14px;
}

/* 弹窗 */
.dialog {
  position: fixed;
  inset: 0;
  background-color: rgba(61, 61, 61, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  width: min(920px, 92vw);
  max-height: 86vh;
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(139, 115, 85, 0.18);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background-color: #ede6d8;
  border-bottom: 1px solid #e0d5c5;
}

.dialog-header h3 {
  margin: 0;
  font-size: 16px;
  color: #3d3d3d;
}

.close-btn {
  background: none;
  border: none;
  color: #6b6b6b;
  font-size: 22px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.close-btn:hover {
  background-color: #e5dcc8;
  color: #3d3d3d;
}

.dialog-body {
  padding: 16px 18px;
  overflow: auto;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.meta-label {
  font-size: 12px;
  color: #6b6b6b;
  min-width: 54px;
}

.meta-value {
  font-size: 13px;
  color: #3d3d3d;
}

.detail-text {
  background-color: #f5f0e6;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  padding: 14px 16px;
  color: #3d3d3d;
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 13px;
}

.dialog-footer {
  padding: 14px 18px;
  border-top: 1px solid #e0d5c5;
  background-color: #faf6f0;
  display: flex;
  justify-content: flex-end;
}

/* 消息提示 */
.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 18px;
  border-radius: 10px;
  color: #f5f0e6;
  font-size: 14px;
  z-index: 1100;
  animation: slideIn 0.25s ease-out;
}

.message.success {
  background-color: #7a9e6e;
}

.message.error {
  background-color: #c17b7b;
}

@keyframes slideIn {
  from {
    transform: translateX(30%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .import-grid {
    grid-template-columns: 1fr;
  }
  .snippet {
    max-width: 320px;
  }
  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>

