import request from '../../api'

export function listVehicles(params) {
  return request({
    url: '/biz/vehicle/list',
    method: 'get',
    params
  })
}

export function getVehicle(id) {
  return request({
    url: `/biz/vehicle/${id}`,
    method: 'get'
  })
}

export function addVehicle(data) {
  return request({
    url: '/biz/vehicle',
    method: 'post',
    data
  })
}

export function updateVehicle(id, data) {
  return request({
    url: `/biz/vehicle/${id}`,
    method: 'put',
    data
  })
}

export function deleteVehicle(id) {
  return request({
    url: `/biz/vehicle/${id}`,
    method: 'delete'
  })
}

export function getVehicleStatistics(params) {
  return request({
    url: '/biz/vehicle/statistics',
    method: 'get',
    params
  })
}

export function submitVehicleApproval(id) {
  return request({
    url: `/biz/vehicle/${id}/submit`,
    method: 'post'
  })
}

export function approveVehicle(id, data) {
  return request({
    url: `/biz/vehicle/${id}/approve`,
    method: 'post',
    data
  })
}