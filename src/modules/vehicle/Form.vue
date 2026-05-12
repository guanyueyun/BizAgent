<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
    <el-form-item label="车牌号" prop="plateNumber">
      <el-input v-model="formData.plateNumber" placeholder="请输入车牌号" />
    </el-form-item>
    <el-form-item label="品牌" prop="brand">
      <el-input v-model="formData.brand" placeholder="请输入品牌" />
    </el-form-item>
    <el-form-item label="型号" prop="model">
      <el-input v-model="formData.model" placeholder="请输入型号" />
    </el-form-item>
    <el-form-item label="颜色" prop="color">
      <el-input v-model="formData.color" placeholder="请输入颜色" />
    </el-form-item>
    <el-form-item label="购买日期" prop="purchaseDate">
      <el-date-picker v-model="formData.purchaseDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
    </el-form-item>
    <el-form-item label="当前里程(公里)" prop="mileage">
      <el-input-number v-model="formData.mileage" :min="0" :step="0.01" style="width: 100%" />
    </el-form-item>
    <el-form-item label="状态" prop="status">
      <el-select v-model="formData.status" placeholder="请选择状态">
        <el-option label="空闲" value="idle" />
        <el-option label="使用中" value="in_use" />
        <el-option label="维修中" value="maintenance" />
        <el-option label="已报废" value="retired" />
      </el-select>
    </el-form-item>
    <el-form-item label="当前驾驶员" prop="driver">
      <el-input v-model="formData.driver" placeholder="请输入当前驾驶员" />
    </el-form-item>
    <el-form-item label="备注" prop="remark">
      <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addVehicle, updateVehicle, getVehicle } from './api'

const props = defineProps({
  id: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['success'])

const formRef = ref(null)
const formData = ref({
  plateNumber: '',
  brand: '',
  model: '',
  color: '',
  purchaseDate: '',
  mileage: 0,
  status: 'idle',
  driver: '',
  remark: ''
})

const rules = {
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const fetchDetail = async () => {
  if (!props.id) return
  try {
    const res = await getVehicle(props.id)
    const data = res.data
    formData.value = {
      plateNumber: data.plateNumber || '',
      brand: data.brand || '',
      model: data.model || '',
      color: data.color || '',
      purchaseDate: data.purchaseDate || '',
      mileage: data.mileage || 0,
      status: data.status || 'idle',
      driver: data.driver || '',
      remark: data.remark || ''
    }
  } catch (e) {
    ElMessage.error('获取车辆信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (props.id) {
      await updateVehicle(props.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await addVehicle(formData.value)
      ElMessage.success('新增成功')
    }
    emit('success')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleCancel = () => {
  emit('success')
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
</style>