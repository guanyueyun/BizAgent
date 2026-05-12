
import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json;charset=UTF-8'
  }
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('bizagent_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const projectId = localStorage.getItem('bizagent_project_id') || '1'
  config.headers['X-Project-Id'] = projectId
  return config
})

const withProject = (params = {}) => ({
  ...params,
  projectId: params.projectId || Number(localStorage.getItem('bizagent_project_id') || 1)
})

const readNdjsonStream = async (path, data, onEvent, options = {}) => {
  const token = localStorage.getItem('bizagent_token')
  const projectId = localStorage.getItem('bizagent_project_id') || '1'
  const response = await fetch(`${request.defaults.baseURL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      Accept: 'application/x-ndjson;charset=UTF-8, application/json;charset=UTF-8',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      'X-Project-Id': projectId
    },
    body: JSON.stringify(data),
    signal: options.signal
  })
  if (!response.ok || !response.body) {
    let message = `流式请求失败：${response.status}`
    try {
      const errorBody = await response.json()
      message = errorBody?.message || errorBody?.data?.message || message
    } catch (error) {
      try {
        const errorText = await response.text()
        message = errorText || message
      } catch (ignored) {
        // keep status fallback
      }
    }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finalData = null

  const handleLine = (line) => {
    if (!line.trim()) return
    const event = JSON.parse(line)
    onEvent?.(event)
    if (event.type === 'error') {
      throw new Error(event.detail || event.title || '流式任务失败')
    }
    if (event.type === 'final') {
      finalData = event.data
    }
  }

  while (true) {
    let chunk
    try {
      chunk = await reader.read()
    } catch (error) {
      if (finalData) {
        return finalData
      }
      throw error
    }
    const { value, done } = chunk
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    for (const line of lines) {
      handleLine(line)
    }
  }

  if (buffer.trim()) {
    handleLine(buffer)
  }

  return finalData
}

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('bizagent_token')
      localStorage.removeItem('bizagent_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default request

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me')
}

export const aiApi = {
  analyze: (data) => request.post('/ai/analyze', data, { timeout: 120000 }),
  plan: (data) => request.post('/ai/plan', data, { timeout: 120000 }),
  optimize: (data) => request.post('/ai/optimize', data, { timeout: 120000 }),
  questions: (data) => request.post('/ai/questions', data, { timeout: 120000 }),
  generateFrontend: (data) => request.post('/ai/generate/frontend', data, { timeout: 120000 }),
  generateBackend: (data) => request.post('/ai/generate/backend', data, { timeout: 120000 }),
  generateSql: (data) => request.post('/ai/generate/sql', data, { timeout: 120000 }),
  preview: (data) => request.post('/ai/preview', data, { timeout: 120000 }),
  check: (data) => request.post('/ai/check', data, { timeout: 120000 }),
  publish: (data) => request.post('/ai/publish', data, { timeout: 120000 }),
  complete: (data) => request.post('/ai/complete', data, { timeout: 120000 }),
  completeStream: (data, onEvent, options) => readNdjsonStream('/ai/complete/stream', data, onEvent, options),
  revise: (data) => request.post('/ai/revise', data, { timeout: 120000 }),
  reviseStream: (data, onEvent, options) => readNdjsonStream('/ai/revise/stream', data, onEvent, options)
}

export const userApi = {
  list: (params) => request.get('/system/user/list', { params }),
  get: (id) => request.get(`/system/user/${id}`),
  create: (data) => request.post('/system/user', data),
  update: (id, data) => request.put(`/system/user/${id}`, data),
  delete: (id) => request.delete(`/system/user/${id}`)
}

export const roleApi = {
  list: (params) => request.get('/system/role/list', { params }),
  get: (id) => request.get(`/system/role/${id}`),
  create: (data) => request.post('/system/role', data),
  update: (id, data) => request.put(`/system/role/${id}`, data),
  delete: (id) => request.delete(`/system/role/${id}`)
}

export const menuApi = {
  list: (params) => request.get('/system/menu/list', { params }),
  tree: () => request.get('/system/menu/tree'),
  get: (id) => request.get(`/system/menu/${id}`),
  create: (data) => request.post('/system/menu', data),
  update: (id, data) => request.put(`/system/menu/${id}`, data),
  delete: (id) => request.delete(`/system/menu/${id}`)
}

export const permissionApi = {
  list: (params) => request.get('/system/permission/list', { params }),
  get: (id) => request.get(`/system/permission/${id}`),
  create: (data) => request.post('/system/permission', data),
  update: (id, data) => request.put(`/system/permission/${id}`, data),
  delete: (id) => request.delete(`/system/permission/${id}`)
}

export const projectApi = {
  list: (params) => request.get('/system/project/list', { params }),
  get: (id) => request.get(`/system/project/${id}`),
  create: (data) => request.post('/system/project', data),
  update: (id, data) => request.put(`/system/project/${id}`, data),
  delete: (id) => request.delete(`/system/project/${id}`)
}

export const modelConfigApi = {
  list: (params) => request.get('/system/model-config/list', { params }),
  active: () => request.get('/system/model-config/active'),
  providers: () => request.get('/system/model-config/providers'),
  get: (id) => request.get(`/system/model-config/${id}`),
  create: (data) => request.post('/system/model-config', data),
  update: (id, data) => request.put(`/system/model-config/${id}`, data),
  delete: (id) => request.delete(`/system/model-config/${id}`),
  setDefault: (id) => request.post(`/system/model-config/${id}/default`),
  test: (data) => request.post('/system/model-config/test', data)
}

export const moduleApi = {
  list: (params) => request.get('/system/module/list', { params }),
  get: (id) => request.get(`/system/module/${id}`),
  create: (data) => request.post('/system/module', data),
  update: (id, data) => request.put(`/system/module/${id}`, data),
  delete: (id) => request.delete(`/system/module/${id}`),
  load: (id) => request.post(`/module/container/load/${id}`),
  unload: (id) => request.post(`/module/container/unload/${id}`),
  reload: (id) => request.post(`/module/container/reload/${id}`),
  status: (id) => request.get(`/module/container/status/${id}`),
  runtime: (moduleCode, params = {}) => request.get(`/module/container/runtime/${moduleCode}`, { params: withProject(params) })
}

export const bizApi = {
  list: (moduleCode, params) => request.get(`/biz/${moduleCode}/list`, { params: withProject(params) }),
  get: (moduleCode, id) => request.get(`/biz/${moduleCode}/${id}`, { params: withProject() }),
  create: (moduleCode, data) => request.post(`/biz/${moduleCode}`, data, { params: withProject() }),
  update: (moduleCode, id, data) => request.put(`/biz/${moduleCode}/${id}`, data, { params: withProject() }),
  delete: (moduleCode, id) => request.delete(`/biz/${moduleCode}/${id}`, { params: withProject() }),
  import: (moduleCode, rows) => request.post(`/biz/${moduleCode}/import`, rows, { params: withProject() }),
  export: (moduleCode) => request.get(`/biz/${moduleCode}/export`, { params: withProject() }),
  statistics: (moduleCode) => request.get(`/biz/${moduleCode}/statistics`, { params: withProject() }),
  submit: (moduleCode, id) => request.post(`/biz/${moduleCode}/${id}/submit`, null, { params: withProject() }),
  approve: (moduleCode, id, data) => request.post(`/biz/${moduleCode}/${id}/approve`, data, { params: withProject() }),
  notify: (moduleCode, id, data) => request.post(`/biz/${moduleCode}/${id}/notify`, data, { params: withProject() }),
  notifications: (moduleCode) => request.get(`/biz/${moduleCode}/notifications`, { params: withProject() })
}
