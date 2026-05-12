import request from '../../api'

export function listInspection(params) {
  return request({
    url: '/biz/inspection/list',
    method: 'GET',
    params
  })
}

export function getInspection(id) {
  return request({
    url: `/biz/inspection/${id}`,
    method: 'GET'
  })
}

export function addInspection(data) {
  return request({
    url: '/biz/inspection',
    method: 'POST',
    data
  })
}

export function updateInspection(id, data) {
  return request({
    url: `/biz/inspection/${id}`,
    method: 'PUT',
    data
  })
}

export function deleteInspection(id) {
  return request({
    url: `/biz/inspection/${id}`,
    method: 'DELETE'
  })
}

export function getStatistics(params) {
  return request({
    url: '/biz/inspection/statistics',
    method: 'GET',
    params
  })
}