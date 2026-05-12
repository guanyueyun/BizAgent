<template>
  <div>
    <el-descriptions title="客户详情" :column="2" border>
      <el-descriptions-item label="客户名称">{{ detailData.customerName }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ detailData.contactPerson }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
      <el-descriptions-item label="所属行业">{{ detailData.industry }}</el-descriptions-item>
      <el-descriptions-item label="客户来源">{{ detailData.source }}</el-descriptions-item>
      <el-descriptions-item label="跟进人">{{ detailData.followUpPerson }}</el-descriptions-item>
      <el-descriptions-item label="跟进状态">
        <el-tag v-if="detailData.status === 'potential'" type="info">潜在</el-tag>
        <el-tag v-else-if="detailData.status === 'contacted'" type="warning">已联系</el-tag>
        <el-tag v-else-if="detailData.status === 'negotiating'" type="primary">洽谈中</el-tag>
        <el-tag v-else-if="detailData.status === 'converted'" type="success">已成交</el-tag>
        <el-tag v-else-if="detailData.status === 'lost'" type="danger">已流失</el-tag>
        <span v-else>{{ detailData.status }}</span>
      </el-descriptions-item>
    </el-descriptions>
    <div style="margin-top: 16px;">
      <el-button @click="handleBack">返回</el-button>
      <el-button type="primary" @click="handleEdit">编辑</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomer } from './api'

const route = useRoute()
const router = useRouter()
const detailData = ref({})

function loadDetail(id) {
  getCustomer(id).then(res => {
    detailData.value = res.data
  }).catch(() => {
    ElMessage.error('获取客户详情失败')
  })
}

function handleBack() {
  router.push('/module-runtime/customer/list')
}

function handleEdit() {
  router.push(`/module-runtime/customer/edit/${route.params.id}`)
}

onMounted(() => {
  if (route.params.id) {
    loadDetail(route.params.id)
  } else {
    ElMessage.error('缺少客户ID')
  }
})
</script>