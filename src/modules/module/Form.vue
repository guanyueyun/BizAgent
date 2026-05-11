<template>
  <div class="generated-module">
    <el-form ref="formRef" :model="form" label-width="120px">
      <el-form-item label="业务编号" required><el-input v-model="form.biz_no" /></el-form-item>
      <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="负责人" ><el-input v-model="form.owner_name" /></el-form-item>
      <el-form-item label="业务时间" ><el-date-picker v-model="form.biz_time" type="datetime" style="width:100%" /></el-form-item>
      <el-form-item label="状态" required><el-select v-model="form.status"><el-option label="草稿" value="draft" /><el-option label="已提交" value="submitted" /><el-option label="已通过" value="approved" /></el-select></el-form-item>
      <el-form-item label="备注" ><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <div style="margin-top: 20px; text-align: right;">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as api from './api'

defineOptions({ name: 'ModuleForm' })

const props = defineProps({ id: [Number, String] })
const emit = defineEmits(['close'])

const formRef = ref(null)
const form = reactive({})

const initForm = () => {
}

const handleSubmit = async () => {
  if (props.id) await api.update(props.id, form)
  else await api.create(form)
  emit('close')
}

const handleCancel = () => emit('close')

onMounted(() => {
  initForm()
  if (props.id) api.detail(props.id).then(res => Object.assign(form, res.data.data))
})
</script>

<style scoped>
.generated-module { padding: 20px; }
</style>
