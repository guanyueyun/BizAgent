<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
    <el-form-item label="设备名称" prop="device_name">
      <el-input v-model="formData.device_name" placeholder="请输入设备名称" />
    </el-form-item>
    <el-form-item label="设备位置" prop="device_location">
      <el-input v-model="formData.device_location" placeholder="请输入设备位置" />
    </el-form-item>
    <el-form-item label="巡检计划" prop="inspection_plan">
      <el-input v-model="formData.inspection_plan" placeholder="请输入巡检计划" />
    </el-form-item>
    <el-form-item label="巡检人" prop="inspector">
      <el-input v-model="formData.inspector" placeholder="请输入巡检人" />
    </el-form-item>
    <el-form-item label="巡检时间" prop="inspection_time">
      <el-date-picker v-model="formData.inspection_time" type="datetime" placeholder="选择巡检时间" style="width:100%" />
    </el-form-item>
    <el-form-item label="异常描述" prop="exception_desc">
      <el-input v-model="formData.exception_desc" type="textarea" :rows="3" placeholder="请输入异常描述" />
    </el-form-item>
    <el-form-item label="状态" prop="status">
      <el-select v-model="formData.status" placeholder="请选择状态" style="width:100%">
        <el-option label="待巡检" value="pending" />
        <el-option label="已完成" value="completed" />
        <el-option label="异常" value="exception" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { addInspection, updateInspection } from './api'

const props = defineProps({
  inspection: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['success'])

const formRef = ref(null)
const formData = reactive({
  device_name: '',
  device_location: '',
  inspection_plan: '',
  inspector: '',
  inspection_time: '',
  exception_desc: '',
  status: ''
})
const rules = {
  device_name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  device_location: [{ required: true, message: '请输入设备位置', trigger: 'blur' }],
  inspector: [{ required: true, message: '请输入巡检人', trigger: 'blur' }],
  inspection_time: [{ required: true, message: '请选择巡检时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

onMounted(() => {
  if (props.inspection) {
    Object.assign(formData, props.inspection)
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (props.inspection && props.inspection.id) {
      await updateInspection(props.inspection.id, formData)
      ElMessage.success('编辑成功')
    } else {
      await addInspection(formData)
      ElMessage.success('新增成功')
    }
    emit('success')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

function handleCancel() {
  emit('success')
}
</script>

<style scoped>
</style>