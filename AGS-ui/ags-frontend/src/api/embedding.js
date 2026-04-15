import service from './axios'

// 知识库/向量库相关接口
export const embeddingApi = {
  // 单文件导入（向量化入库）
  embedBatch: (file, type) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)
    return service.post('/embedding/batch', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000
    })
  },

  // 多文件导入（向量化入库）
  embedBatches: (files, type) => {
    const formData = new FormData()
    ;(files || []).forEach(f => formData.append('files', f))
    formData.append('type', type)
    return service.post('/embedding/batches', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 120000
    })
  },

  // 分页查询当前用户知识库（支持模糊搜索）
  page: (params) => {
    return service.get('/embedding/page', { params })
  },

  // 详情
  detail: (id) => {
    return service.get(`/embedding/${id}`)
  }
}

