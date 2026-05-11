<template>
  <div class="model-config-list">
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>模型配置</h3>
            <p>维护 AI 生成使用的模型厂商、接口地址和调用参数。</p>
          </div>
          <el-button type="primary" @click="add">
            <el-icon><Plus /></el-icon>
            新增配置
          </el-button>
        </div>
      </template>

      <el-row :gutter="10" class="mb-4">
        <el-col :xs="24" :sm="8">
          <el-input v-model="searchForm.configName" placeholder="配置名称" clearable />
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-select v-model="searchForm.provider" placeholder="模型厂商" clearable class="full-control">
            <el-option v-for="item in providerOptions" :key="item.name" :label="item.label" :value="item.name" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-col>
      </el-row>

      <el-table :data="tableData" border>
        <el-table-column prop="configName" label="配置名称" min-width="140" />
        <el-table-column prop="provider" label="厂商" width="150" />
        <el-table-column prop="modelName" label="模型名称" min-width="160" />
        <el-table-column prop="baseUrl" label="接口地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="temperature" label="温度" width="90" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="120" />
        <el-table-column label="默认" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.defaultFlag === 1" type="success">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="success" :disabled="row.defaultFlag === 1" @click="setDefault(row.id)">
              设为默认
            </el-button>
            <el-button size="small" type="danger" @click="deleteItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="例如：生产环境 GPT-4o" />
        </el-form-item>
        <el-form-item label="模型厂商" prop="provider">
          <el-select v-model="form.provider" filterable allow-create class="full-control" @change="handleProviderChange">
            <el-option v-for="item in providerOptions" :key="item.name" :label="item.label" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="form.baseUrl" :placeholder="currentProvider?.baseUrl || '例如：https://api.openai.com/v1'" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="编辑时留空则保持原值" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-select
            v-model="form.modelName"
            filterable
            allow-create
            class="full-control"
            placeholder="请选择或输入模型名称"
          >
            <el-option v-for="item in modelNameOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="温度">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" class="full-control" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Max Tokens">
              <el-input-number v-model="form.maxTokens" :min="256" :max="128000" :step="512" class="full-control" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时秒数">
              <el-input-number v-model="form.timeoutSeconds" :min="5" :max="600" class="full-control" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.defaultFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="testConfig">校验配置</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { modelConfigApi } from '../../api'

const fallbackProviders = [
  {
    name: 'OpenAI-Compatible',
    label: 'OpenAI 兼容',
    baseUrl: 'https://api.openai.com/v1',
    models: ['gpt-4o-mini', 'gpt-4o', 'gpt-4.1-mini'],
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60
  },
  {
    name: 'DeepSeek',
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com/v1',
    models: ['deepseek-chat', 'deepseek-reasoner'],
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60
  },
  {
    name: '通义千问',
    label: '阿里云通义千问',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    models: ['qwen-plus', 'qwen-max', 'qwen-turbo'],
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60
  },
  {
    name: '智谱 GLM',
    label: '智谱 GLM',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    models: ['glm-4-plus', 'glm-4-flash', 'glm-4-air'],
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60
  },
  {
    name: 'Ollama',
    label: 'Ollama 本地模型',
    baseUrl: 'http://localhost:11434/v1',
    models: ['llama3.1', 'qwen2.5', 'deepseek-r1'],
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 120
  }
]

const providerOptions = ref(fallbackProviders)

const searchForm = reactive({
  configName: '',
  provider: ''
})

const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增模型配置')
const formRef = ref(null)
const form = reactive({
  id: null,
  configName: '',
  provider: 'OpenAI-Compatible',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096,
  timeoutSeconds: 60,
  defaultFlag: 0,
  status: 1,
  description: '',
  projectId: 1
})

const currentProvider = computed(() => providerOptions.value.find((item) => item.name === form.provider))
const modelNameOptions = computed(() => currentProvider.value?.models || [])

const rules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择模型厂商', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
}

const resetModel = () => {
  const defaultProvider = providerOptions.value[0] || fallbackProviders[0]
  Object.assign(form, {
    id: null,
    configName: '',
    provider: defaultProvider.name,
    baseUrl: defaultProvider.baseUrl,
    apiKey: '',
    modelName: defaultProvider.models?.[0] || '',
    temperature: defaultProvider.temperature ?? 0.7,
    maxTokens: defaultProvider.maxTokens ?? 4096,
    timeoutSeconds: defaultProvider.timeoutSeconds ?? 60,
    defaultFlag: 0,
    status: 1,
    description: '',
    projectId: 1
  })
}

const handleProviderChange = () => {
  const preset = currentProvider.value
  if (!preset) return
  form.baseUrl = preset.baseUrl
  form.modelName = preset.models?.[0] || ''
  form.temperature = preset.temperature ?? 0.7
  form.maxTokens = preset.maxTokens ?? 4096
  form.timeoutSeconds = preset.timeoutSeconds ?? 60
}

const loadProviders = async () => {
  try {
    const response = await modelConfigApi.providers()
    if (response.data.data?.length) {
      providerOptions.value = response.data.data
    }
  } catch (error) {
    console.error('加载模型厂商失败:', error)
  }
}

const loadData = async () => {
  try {
    const response = await modelConfigApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      configName: searchForm.configName,
      provider: searchForm.provider
    })
    tableData.value = response.data.data.records
    pagination.total = response.data.data.total
  } catch (error) {
    showRequestError(error, '模型配置加载失败')
  }
}

const resetForm = () => {
  searchForm.configName = ''
  searchForm.provider = ''
  pagination.pageNum = 1
  loadData()
}

const add = () => {
  dialogTitle.value = '新增模型配置'
  resetModel()
  dialogVisible.value = true
}

const edit = (row) => {
  dialogTitle.value = '编辑模型配置'
  resetModel()
  Object.assign(form, row, { apiKey: '' })
  dialogVisible.value = true
}

const save = async () => {
  const valid = await validateForm()
  if (!valid) return

  try {
    if (form.id) {
      await modelConfigApi.update(form.id, form)
    } else {
      await modelConfigApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (error) {
    showRequestError(error, '保存失败')
  }
}

const setDefault = async (id) => {
  try {
    await modelConfigApi.setDefault(id)
    ElMessage.success('默认模型已更新')
    loadData()
  } catch (error) {
    showRequestError(error, '默认模型更新失败')
  }
}

const testConfig = async () => {
  const valid = await validateForm()
  if (!valid) return

  if (!form.baseUrl && form.provider !== 'Ollama') {
    ElMessage.warning('建议填写接口地址')
    return
  }
  if (!form.apiKey && !form.id && form.provider !== 'Ollama') {
    ElMessage.warning('建议填写 API Key')
    return
  }
  ElMessage.success(`配置格式校验通过：${form.provider} / ${form.modelName}`)
}

const deleteItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该模型配置？', '删除确认', { type: 'warning' })
    await modelConfigApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      showRequestError(error, '删除失败')
    }
  }
}

const validateForm = async () => {
  try {
    await formRef.value.validate()
    return true
  } catch (error) {
    ElMessage.warning('请先补全必填项')
    return false
  }
}

const showRequestError = (error, fallbackMessage) => {
  const message = error?.response?.data?.message || error?.message || fallbackMessage
  ElMessage.error(message)
  console.error(fallbackMessage, error)
}

const init = async () => {
  await loadProviders()
  await loadData()
}

init()
</script>

<style scoped>
.model-config-list {
  padding: 20px;
}

.card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-header h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.card-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.mb-4 {
  margin-bottom: 16px;
}

.full-control {
  width: 100%;
}

.el-pagination {
  margin-top: 18px;
  justify-content: flex-end;
}
</style>
