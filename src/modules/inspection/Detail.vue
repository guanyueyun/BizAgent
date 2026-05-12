<template>
  <el-descriptions :column="2" border v-loading="loading">
    <el-descriptions-item label="设备名称">{{ detailData.device_name }}</el-descriptions-item>
    <el-descriptions-item label="设备位置">{{ detailData.device_location }}</el-descriptions-item>
    <el-descriptions-item label="巡检计划名称">{{ detailData.plan_name }}</el-descriptions-item>
    <el-descriptions-item label="巡检人">{{ detailData.inspector }}</el-descriptions-item>
    <el-descriptions-item label="巡检时间">{{ detailData.inspection_time }}</el-descriptions-item>
    <el-descriptions-item label="状态">
      <el-tag v-if="detailData.status === 'pending'" type="warning">待巡检</el-tag>
      <el-tag v-else-if="detailData.status === 'completed'" type="success">已完成</el-tag>
      <el-tag v-else-if="detailData.status === 'abnormal'" type="danger">异常</el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="异常描述" :span="2">{{ detailData.exception_desc || '-' }}</el-descriptions-item>
    <el-descriptions-item label="整改状态">
      <el-tag v-if="detailData.rectification_status === 'none'" type="info">无需整改</el-tag>
      <el-tag v-else-if="detailData.rectification_status === 'pending'" type="warning">待整改</el-tag>
      <el-tag v-else-if="detailData.rectification_status === 'rectified'" type="success">已整改</el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="整改措施">{{ detailData.rectification_measure || '-' }}</el-descriptions-item>
    <el-descriptions-item label="整改责任人">{{ detailData.rectification_person || '-' }}</el-descriptions-item>
    <el-descriptions-item label="整改完成时间">{{ detailData.rectification_time || '-' }}</el-descriptions-item>
  </el-descriptions>
  <div style="text-align: center; margin-top: 20px;">
    <el-button @click="handleBack">返回</el-button>
    <el-button type="primary" @click="handleEdit" v-permission="'inspection:edit'">编辑</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInspection } from './api'

const props = defineProps({
  id: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['edit'])

const loading = ref(false)
const detailData = ref({})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getInspection(props.id)
    detailData.value = res.data
  } catch (e) {
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  emit('edit', null)
}

const handleEdit = () => {
  emit('edit', props.id)
}

onMounted(() => {
  loadDetail()
})
</script>