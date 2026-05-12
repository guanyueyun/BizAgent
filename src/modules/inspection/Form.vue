<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
    <el-form-item label="设备名称" prop="device_name">
      <el-input v-model="formData.device_name" placeholder="请输入设备名称" />
    </el-form-item>
    <el-form-item label="设备位置" prop="device_location">
      <el-input v-model="formData.device_location" placeholder="请输入设备位置" />
    </el-form-item>
    <el-form-item label="巡检计划名称" prop="plan_name">
      <el-input v-model="formData.plan_name" placeholder="请输入巡检计划名称" />
    </el-form-item>
    <el-form-item label="巡检人" prop="inspector">
      <el-input v-model="formData.inspector" placeholder="请输入巡检人" />
    </el-form-item>
    <el-form-item label="巡检时间" prop="inspection_time">
      <el-date-picker
        v-model="formData.inspection_time"
        type="datetime"
        placeholder="选择巡检时间"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
    </el-form-item>
    <el-form-item label="状态" prop="status">
      <el-select v-model="formData.status" placeholder="请选择状态">
        <el-option label="待巡检" value="pending" />
        <el-option label="已完成" value="completed" />
        <el-option label="异常" value="abnormal" />
      </el-select>
    </el-form-item>
    <template v-if="id">
      <el-form-item label="异常描述" prop="exception_desc">
        <el-input v-model="formData.exception_desc" type="textarea" placeholder="请输入异常描述" />
      </el-form-item>
      <el-form-item label="整改状态" prop="rectification_status">
        <el-select v-model="formData.rectification_status" placeholder="请选择整改状态">
          <el-option label="无需整改" value="none" />
          <el-option label="待整改" value="pending" />
          <el-option label="已整改" value="rectified" />
        </el-select>
      </el-form-item>
      <el-form-item label="整改措施" prop="rectification_measure">
        <el-input v-model="formData.rectification_measure" type="textarea" placeholder="请输入整改措施" />
      </el-form-item>
      <el-form-item label="整改责任人" prop="rectification_person">
        <el-input v-model="formData.rectification_person" placeholder="请输入整改责任人" />
      </el-form-item>
      <el-form-item label="整改完成时间" prop="rectification_time">
        <el-date-picker
          v-model="formData.rectification_time"
          type="datetime"
          placeholder="选择整改完成时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
    </template>
    <el-form-item>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInspection, addInspection, updateInspection } from './api'

const props = defineProps({
  id: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['success'])

const formRef = ref(null)
const formData = ref({
  device_name: '',
  device_location: '',
  plan_name: '',
  inspector: '',
  inspection_time: '',
  status: 'pending',
  exception_desc: '',
  rectification_status: 'none',
  rectification_measure: '',
  rectification_person: '',
  rectification_time: ''
})

const rules = ref({
  device_name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  device_location: [{ required: true, message: '请输入设备位置', trigger: 'blur' }],
  plan_name: [{ required: true, message: '请输入巡检计划名称', trigger: 'blur' }],
  inspector: [{ required: true, message: '请输入巡检人', trigger: 'blur' }],
  inspection_time: [{ required: true, message: '请选择巡检时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const loadDetail = async () => {
  if (!props.id) return
  try {
    const res = await getInspection(props.id)
    formData.value = { ...res.data }
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (props.id) {
      await updateInspection(props.id, formData.value)
      ElMessage.success('编辑成功')
    } else {
      await addInspection(formData.value)
      ElMessage.success('新增成功')
    }
    emit('success')
  } catch (e) {
    ElMessage.error(props.id ? '编辑失败' : '新增失败')
  }
}

const handleCancel = () => {
  emit('success')
}

onMounted(() => {
  loadDetail()
})
</script>