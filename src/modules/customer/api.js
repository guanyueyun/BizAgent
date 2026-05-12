import request from '../../api'

export function listCustomer(params) {
  return request({
    url: '/biz/customer/list',
    method: 'get',
    params
  })
}

export function getCustomer(id) {
  return request({
    url: `/biz/customer/${id}`,
    method: 'get'
  })
}

export function addCustomer(data) {
  return request({
    url: '/biz/customer',
    method: 'post',
    data
  })
}

export function updateCustomer(id, data) {
  return request({
    url: `/biz/customer/${id}`,
    method: 'put',
    data
  })
}

export function deleteCustomer(id) {
  return request({
    url: `/biz/customer/${id}`,
    method: 'delete'
  })
}

export function getCustomerStatistics() {
  return request({
    url: '/biz/customer/statistics',
    method: 'get'
  })
}