<template>
  <div>
    <el-descriptions :column="2" border v-if="detailData">
      <el-descriptions-item label="客户名称" :span="2">{{ detailData.customer_name }}</el-descriptions-item>
      <el-descriptions-item label="回访时间" :span="2">{{ detailData.visit_time }}</el-descriptions-item>
      <el-descriptions-item label="回访方式" :span="2">{{ detailData.visit_method }}</el-descriptions-item>
      <el-descriptions-item label="回访结果" :span="2">{{ detailData.visit_result }}</el-descriptions-item>
      <el-descriptions-item label="跟进人" :span="2">{{ detailData.follower }}</el-descriptions-item>
      <el-descriptions-item label="状态" :span="2">
        <el-tag v-if="detailData.status === 'pending'" type="warning">待处理</el-tag>
        <el-tag v-else-if="detailData.status === 'completed'" type="success">已完成</el-tag>
        <el-tag v-else-if="detailData.status === 'cancelled'" type="info">已取消</el-tag>
        <span v-else>{{ detailData.status }}</span>
      </el-descriptions-item>
    </el-descriptions>
    <div style="margin-top: 20px; text-align: center;">
      <el-button type="primary" @click="handleEdit">编辑</el-button>
      <el-button @click="handleBack">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVisit } from './api'

const props = defineProps({
  id: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['edit'])

const detailData = ref(null)

function loadDetail() {
  getVisit(props.id).then(res => {
    detailData.value = res.data
  }).catch(() => {
    ElMessage.error('加载详情失败')
  })
}

function handleEdit() {
  emit('edit', props.id)
}

function handleBack() {
  // In a real scenario, this might navigate back, but for modal usage we emit a success event or close
  // For simplicity, we just emit an event to parent
  emit('edit', null)
}

onMounted(() => {
  loadDetail()
})
</script>