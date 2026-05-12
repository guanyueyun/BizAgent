import request from '../../api'

export function listVisit(params) {
  return request({
    url: '/biz/customer_visit/list',
    method: 'get',
    params
  })
}

export function getVisit(id) {
  return request({
    url: `/biz/customer_visit/${id}`,
    method: 'get'
  })
}

export function addVisit(data) {
  return request({
    url: '/biz/customer_visit',
    method: 'post',
    data
  })
}

export function updateVisit(id, data) {
  return request({
    url: `/biz/customer_visit/${id}`,
    method: 'put',
    data
  })
}

export function deleteVisit(id) {
  return request({
    url: `/biz/customer_visit/${id}`,
    method: 'delete'
  })
}

export function getVisitStatistics() {
  return request({
    url: '/biz/customer_visit/statistics',
    method: 'get'
  })
}