import request from '../../api'

export function listInspections(params) {
  return request({
    url: '/biz/equipment_inspection/list',
    method: 'get',
    params
  })
}

export function getInspection(id) {
  return request({
    url: `/biz/equipment_inspection/${id}`,
    method: 'get'
  })
}

export function addInspection(data) {
  return request({
    url: '/biz/equipment_inspection',
    method: 'post',
    data
  })
}

export function updateInspection(id, data) {
  return request({
    url: `/biz/equipment_inspection/${id}`,
    method: 'put',
    data
  })
}

export function deleteInspection(id) {
  return request({
    url: `/biz/equipment_inspection/${id}`,
    method: 'delete'
  })
}