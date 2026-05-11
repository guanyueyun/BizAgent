<template>
  <div class="generated-module">
    <el-descriptions title="业务模块详情" :column="2" border>
      <el-descriptions-item label="业务编号">{{ form.biz_no || '-' }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ form.name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="负责人">{{ form.owner_name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="业务时间">{{ form.biz_time || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.status || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ form.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
    <div style="margin-top: 20px; text-align: right;">
      <el-button @click="handleEdit">编辑</el-button>
      <el-button @click="handleBack">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import * as api from './api'

defineOptions({ name: 'ModuleDetail' })

const props = defineProps({ id: [Number, String] })
const emit = defineEmits(['back', 'edit'])

const form = reactive({})

const handleEdit = () => emit('edit', props.id)
const handleBack = () => emit('back')

onMounted(() => {
  if (props.id) api.detail(props.id).then(res => Object.assign(form, res.data.data))
})
</script>

<style scoped>
.generated-module { padding: 20px; }
</style>
