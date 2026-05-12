<template>
  <div class="vehicle-detail">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="车牌号">{{ detailData.plateNumber }}</el-descriptions-item>
      <el-descriptions-item label="品牌">{{ detailData.brand }}</el-descriptions-item>
      <el-descriptions-item label="型号">{{ detailData.model }}</el-descriptions-item>
      <el-descriptions-item label="颜色">{{ detailData.color }}</el-descriptions-item>
      <el-descriptions-item label="购买日期">{{ detailData.purchaseDate }}</el-descriptions-item>
      <el-descriptions-item label="当前里程(公里)">{{ detailData.mileage }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailData.status)">{{ statusLabel(detailData.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="审批状态">
        <el-tag :type="approvalTagType(detailData.approvalStatus)">{{ approvalLabel(detailData.approvalStatus) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="当前驾驶员">{{ detailData.driver }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ detailData.remark }}</el-descriptions-item>
    </el-descriptions>
    <div style="margin-top: 20px; text-align: center;">
      <el-button @click="handleBack">返回</el-button>
      <el-button type="primary" @click="handleEdit">编辑</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVehicle } from './api'

const props = defineProps({
  id: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['edit', 'back'])

const detailData = ref({
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  purchaseDate: '',
  mileage: 0,
  status: '',
  approvalStatus: '',
  driver: '',
  remark: ''
})

const statusTagType = (status) => {
  const map = { idle: 'success', in_use: 'primary', maintenance: 'warning',