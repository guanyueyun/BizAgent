<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
    <el-form-item label="客户名称" prop="customer_name">
      <el-input v-model="formData.customer_name" placeholder="请输入客户名称" />
    </el-form-item>
    <el-form-item label="回访时间" prop="visit_time">
      <el-date-picker
        v-model="formData.visit_time"
        type="datetime"
        placeholder="选择回访时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="回访方式" prop="visit_method">
      <el-select v-model="formData.visit_method" placeholder="请选择回访方式" style="width: 100%">
        <el-option label="电话" value="电话" />
        <el-option label="邮件" value="邮件" />
        <el-option label="上门" value="上门" />
        <el-option label="其他" value="其他" />
      </el-select>
    </el-form-item>
    <el-form-item label="回访结果" prop="visit_result">
      <el-input v-model="formData.visit_result" type="textarea" :rows="3" placeholder="请输入回访结果" />
    </el-form-item>
    <el-form-item label="跟进人" prop="follower">
      <el-input v-model="formData.follower" placeholder="请输入跟进人" />
    </el-form-item>
    <el-form-item label="状态" prop="status">
      <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
        <el-option label="待处理" value="pending" />
        <el-option label="已完成" value="completed" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">提交</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getVisit, addVisit, updateVisit } from './api'

const props = defineProps({
  id: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['success'])

const formRef = ref(null)
const formData = ref({
  customer_name: '',
  visit_time: '',
  visit_method: '',
  visit_result: '',
  follower: '',
  status: 'pending'
})
const submitLoading = ref(false)

const rules = {
  customer_name: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }],
  visit_time: [{ required: true, message: '回访时间不能为空', trigger: 'change' }],
  visit_method: [{ required: true, message: '回访方式不能为空', trigger: 'change' }],
  follower: [{ required: true, message: '跟进人不能为空', trigger: 'blur' }]
}

function loadDetail() {
  if (props.id) {
    getVisit(props.id).then(res => {
      formData.value = res.data
    }).catch(() => {
      ElMessage.error('加载详情失败')
    })
  }
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (valid) {
      submitLoading.value = true
      const apiCall = props.id ? updateVisit(props.id, formData.value) : addVisit(formData.value)
      apiCall.then(() => {
        ElMessage.success(props.id ? '编辑成功' : '新增成功')
        emit('success')
      }).catch(() => {
        ElMessage.error('操作失败')
      }).finally(() => {
        submitLoading.value = false
      })
    }
  })
}

function handleCancel() {
  emit('success')
}

onMounted(() => {
  loadDetail()
})
</script>