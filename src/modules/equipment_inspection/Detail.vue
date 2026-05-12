<template>
  <div>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="设备名称">{{ inspection.device_name }}</el-descriptions-item>
      <el-descriptions-item label="设备位置">{{ inspection.device_location }}</el-descriptions-item>
      <el-descriptions-item label="巡检计划">{{ inspection.inspection_plan || '-' }}</el-descriptions-item>
      <el-descriptions-item label="巡检人">{{ inspection.inspector }}</el-descriptions-item>
      <el-descriptions-item label="巡检时间">{{ inspection.inspection_time ? inspection.inspection_time : '-' }}</el-descriptions-item>
      <el-descriptions-item label="异常描述">{{ inspection.exception_desc || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="inspection.status === 'pending' ? 'warning' : inspection.status === 'completed' ? 'success' : 'danger'">
          {{ inspection.status === 'pending' ? '待巡检' : inspection.status === 'completed' ? '已完成' : '异常' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <div style="margin-top: 20px; text-align: right;">
      <el-button @click="handleBack">返回</el-button>
      <el-button type="primary" @click="handleEdit">编辑</el-button>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  inspection: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['edit'])

function handleBack() {
  emit('edit')
}

function handleEdit() {
  emit('edit')
}
</script>

<style scoped>
</style>