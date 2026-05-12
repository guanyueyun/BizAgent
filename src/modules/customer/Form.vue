<template>
  <div>
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
      <el-form-item label="客户名称" prop="customerName">
        <el-input v-model="formData.customerName" placeholder="请输入客户名称" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input v-model="formData.contactPerson" placeholder="请输入联系人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="所属行业" prop="industry">
        <el-input v-model="formData.industry" placeholder="请输入所属行业" />
      </el-form-item>
      <el-form-item label="客户来源" prop="source">
        <el-input v-model="formData.source" placeholder="请输入客户来源" />
      </el-form-item>
      <el-form-item label="跟进人" prop="followUpPerson">
        <el-input v-model="formData.followUpPerson" placeholder="请输入跟进人" />
      </el-form-item>
      <el-form-item label="跟进状态" prop="status">
        <el-select v-model="formData.status" placeholder="请选择跟进状态" style="width: 100%;">
          <el-option label="潜在" value="potential" />
          <el-option label="已联系" value="contacted" />
          <el-option label="洽谈中" value="negotiating" />
          <el-option label="已成交" value="converted" />
          <el-option label="已流失" value="lost" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addCustomer, getCustomer, updateCustomer } from './api'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const isEdit = ref(false)
const formData = reactive({
  customerName: '',
  contactPerson: '',
  contactPhone: '',
  industry: '',
  source: '',
  followUpPerson: '',
  status: 'potential'
})

const rules = {
  customerName: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '跟进状态不能为空', trigger: 'change' }]
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    if (isEdit.value) {
      updateCustomer(route.params.id, formData).then(() => {
        ElMessage.success('更新成功')
        router.push('/module-runtime/customer/list')
      }).catch(() => {
        ElMessage.error('更新失败')
      })
    } else {
      addCustomer(formData).then(() => {
        ElMessage.success('新增成功')
        router.push('/module-runtime/customer/list')
      }).catch(() => {
        ElMessage.error('新增失败')
      })
    }
  })
}

function handleCancel() {
  router.push('/module-runtime/customer/list')
}

function loadDetail(id) {
  getCustomer(id).then(res => {
    const data = res.data
    formData.customerName = data.customerName
    formData.contactPerson = data.contactPerson
    formData.contactPhone = data.contactPhone
    formData.industry = data.industry
    formData.source = data.source
    formData.followUpPerson = data.followUpPerson
    formData.status = data.status
  }).catch(() => {
    ElMessage.error('加载客户信息失败')
  })
}

onMounted(() => {
  if (route.params.id) {
    isEdit.value = true
    loadDetail(route.params.id)
  }
})
</script>