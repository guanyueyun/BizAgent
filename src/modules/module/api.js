import request from '../../api'

export const moduleCode = 'module'
export const list = (params) => request.get(`/biz/${moduleCode}/list`, { params })
export const detail = (id) => request.get(`/biz/${moduleCode}/${id}`)
export const create = (data) => request.post(`/biz/${moduleCode}`, data)
export const update = (id, data) => request.put(`/biz/${moduleCode}/${id}`, data)
export const remove = (id) => request.delete(`/biz/${moduleCode}/${id}`)
