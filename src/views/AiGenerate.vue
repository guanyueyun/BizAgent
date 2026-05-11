
<template>
  <div class="ai-generate">
    <section class="generate-hero">
      <div class="hero-copy">
        <h2>把业务需求变成可运行模块</h2>
        <p>描述流程、字段和权限，BizAgent 会生成模块设计、SQL、前端页面和后端接口草案。</p>
      </div>
      <div class="hero-metrics">
        <div>
          <strong>4</strong>
          <span>生成步骤</span>
        </div>
        <div>
          <strong>{{ form.options.length }}</strong>
          <span>已选能力</span>
        </div>
        <div>
          <strong>{{ design ? '就绪' : '待生成' }}</strong>
          <span>设计状态</span>
        </div>
      </div>
    </section>

    <div class="workspace-grid">
      <el-card class="card input-panel" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>模块需求</h3>
              <p>越具体，生成结果越接近真实业务。</p>
            </div>
            <el-icon><MagicStick /></el-icon>
          </div>
        </template>

        <el-form ref="formRef" :model="form" label-position="top">
          <el-form-item label="需求描述">
            <el-input
              v-model="form.requirement"
              type="textarea"
              :rows="7"
              resize="none"
              placeholder="例如：我要一个设备巡检管理模块，包含巡检计划、巡检记录、异常上报、审批流和统计看板"
            />
            <div class="requirement-tools">
              <el-button size="small" :loading="optimizing" @click="optimizeRequirement">
                <el-icon><MagicStick /></el-icon>
                AI优化描述
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="所属项目">
            <el-select v-model="form.projectId" placeholder="请选择项目" class="full-control">
              <el-option
                v-for="item in projectOptions"
                :key="item.id"
                :label="`${item.projectName} / ${item.projectCode}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模型配置">
            <el-select v-model="form.modelConfigId" placeholder="请选择模型配置" class="full-control" clearable>
              <el-option
                v-for="item in modelOptions"
                :key="item.id"
                :label="`${item.configName} / ${item.modelName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="功能选项">
            <div class="option-grid">
              <button
                v-for="item in optionItems"
                :key="item.value"
                type="button"
                class="option-card"
                :class="{ selected: form.options.includes(item.value) }"
                @click="toggleOption(item.value)"
              >
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </button>
            </div>
          </el-form-item>
        </el-form>

        <div class="btn-group">
          <el-button size="large" type="primary" :loading="analyzing" @click="analyze">
            <el-icon><DataAnalysis /></el-icon>
            需求分析
          </el-button>
          <el-button size="large" :loading="questioning" @click="askQuestions">
            <el-icon><QuestionFilled /></el-icon>
            需求追问
          </el-button>
          <el-button size="large" type="success" :loading="generating" @click="generate">
            <el-icon><Finished /></el-icon>
            一键生成
          </el-button>
        </div>

        <div v-if="questions.length" class="question-list">
          <h4>建议补充</h4>
          <button v-for="item in questions" :key="item" type="button" @click="appendQuestion(item)">
            {{ item }}
          </button>
        </div>
      </el-card>

      <aside class="guide-panel">
        <div class="guide-card">
          <el-icon><Connection /></el-icon>
          <h3>推荐描述结构</h3>
          <ul>
            <li>业务对象：例如客户、设备、工单</li>
            <li>核心流程：新增、审批、归档、统计</li>
            <li>字段规则：状态、时间、负责人、附件</li>
            <li>权限范围：角色、菜单、操作点</li>
          </ul>
        </div>
        <div class="guide-card muted">
          <el-icon><Document /></el-icon>
          <h3>生成结果包含</h3>
          <p>模块设计、表结构、页面清单、API 接口、权限点和三类代码片段。</p>
        </div>
      </aside>
    </div>

    <el-card v-if="design" class="card result-panel" shadow="never">
      <template #header>
        <div class="card-header result-header">
          <div>
            <h3>模块设计预览</h3>
            <p>{{ design.moduleName }} · {{ design.moduleCode }}</p>
          </div>
          <div class="result-actions">
            <el-button type="primary" @click="preview">预览模块</el-button>
            <el-button type="success" :loading="publishing" @click="publish">发布模块</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="设计概览" name="overview">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="模块名称">{{ design.moduleName }}</el-descriptions-item>
            <el-descriptions-item label="模块编码">{{ design.moduleCode }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ design.description }}</el-descriptions-item>
          </el-descriptions>
          
          <el-divider />
          
          <div class="section">
            <h4>数据库表结构</h4>
            <el-table :data="design.tables" border>
              <el-table-column prop="tableName" label="表名" />
              <el-table-column prop="tableComment" label="注释" />
              <el-table-column prop="columns" label="字段数" :formatter="(row) => row.columns.length" />
            </el-table>
          </div>
          
          <div class="section">
            <h4>页面列表</h4>
            <el-table :data="design.pages" border>
              <el-table-column prop="pageName" label="页面名称" />
              <el-table-column prop="pageType" label="页面类型" />
              <el-table-column prop="path" label="路由路径" />
            </el-table>
          </div>
          
          <div class="section">
            <h4>API接口</h4>
            <el-table :data="design.apis" border>
              <el-table-column prop="apiName" label="接口名称" />
              <el-table-column prop="method" label="方法" />
              <el-table-column prop="path" label="路径" />
            </el-table>
          </div>
          
          <div class="section">
            <h4>权限点</h4>
            <el-table :data="design.permissions" border>
              <el-table-column prop="permissionCode" label="权限编码" />
              <el-table-column prop="permissionName" label="权限名称" />
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="SQL脚本" name="sql">
          <pre class="code-block">{{ sqlScript }}</pre>
        </el-tab-pane>
        <el-tab-pane label="前端代码" name="frontend">
          <pre class="code-block">{{ frontendCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="后端代码" name="backend">
          <pre class="code-block">{{ backendCode }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="optimizeVisible" title="AI优化建议" width="760px">
      <div class="optimize-compare">
        <section>
          <h4>原始描述</h4>
          <pre>{{ form.requirement }}</pre>
        </section>
        <section>
          <h4>优化描述</h4>
          <pre>{{ optimizedRequirement }}</pre>
        </section>
      </div>
      <template #footer>
        <el-button @click="optimizeVisible = false">不采纳</el-button>
        <el-button @click="optimizeRequirement">重新优化</el-button>
        <el-button type="primary" @click="adoptOptimizedRequirement">采纳优化描述</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Connection, DataAnalysis, Document, Finished, MagicStick, Monitor, QuestionFilled, Upload } from '@element-plus/icons-vue'
import { aiApi, modelConfigApi, projectApi } from '../api'

const router = useRouter()
const formRef = ref(null)
const form = reactive({
  requirement: '',
  projectId: null,
  modelConfigId: null,
  options: []
})

const design = ref(null)
const activeTab = ref('overview')
const sqlScript = ref('')
const frontendCode = ref('')
const backendCode = ref('')
const modelOptions = ref([])
const projectOptions = ref([])
const questions = ref([])
const optimizeVisible = ref(false)
const optimizing = ref(false)
const analyzing = ref(false)
const questioning = ref(false)
const generating = ref(false)
const publishing = ref(false)
const optimizedRequirement = ref('')
const generatedModule = ref(null)

const optionItems = [
  { label: '审批流程', value: 'approval', icon: Finished },
  { label: '移动端适配', value: 'mobile', icon: Monitor },
  { label: '导入导出', value: 'importExport', icon: Upload },
  { label: '统计分析', value: 'statistics', icon: DataAnalysis },
  { label: '消息提醒', value: 'notification', icon: Bell }
]

const toggleOption = (value) => {
  const index = form.options.indexOf(value)
  if (index === -1) {
    form.options.push(value)
  } else {
    form.options.splice(index, 1)
  }
}

const requestPayload = () => {
  persistProject()
  return {
    requirement: form.requirement,
    projectId: form.projectId,
    modelConfigId: form.modelConfigId,
    needApproval: form.options.includes('approval'),
    needMobile: form.options.includes('mobile'),
    needImportExport: form.options.includes('importExport'),
    needStatistics: form.options.includes('statistics'),
    needNotification: form.options.includes('notification')
  }
}

const persistProject = () => {
  if (form.projectId) {
    localStorage.setItem('bizagent_project_id', String(form.projectId))
  }
}

const askQuestions = async () => {
  if (!form.requirement.trim()) {
    ElMessage.warning('请先输入模块需求')
    return
  }
  questioning.value = true
  const loadingMessage = ElMessage({
    message: 'AI 正在生成需求追问，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await aiApi.questions(requestPayload())
    questions.value = response.data.data || []
    if (questions.value.length) {
      ElMessage.success('需求追问已生成')
    } else {
      ElMessage.warning('暂未生成追问，请补充更多需求上下文后重试')
    }
  } catch (error) {
    console.error('需求追问失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? '需求追问超时，请稍后重试'
      : error.response?.data?.message || '需求追问失败，请确认后端服务已启动'
    ElMessage.error(message)
  } finally {
    loadingMessage.close()
    questioning.value = false
  }
}

const appendQuestion = (question) => {
  form.requirement = `${form.requirement.trim()}\n补充：${question} `
}

const optimizeRequirement = async () => {
  if (!form.requirement.trim()) {
    ElMessage.warning('请先输入模块需求')
    return
  }
  optimizing.value = true
  const loadingMessage = ElMessage({
    message: 'AI 正在优化需求描述，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await aiApi.optimize(requestPayload())
    optimizedRequirement.value = response.data.data || ''
    if (!optimizedRequirement.value.trim()) {
      ElMessage.warning('未生成优化内容，请检查模型配置后重试')
      return
    }
    optimizeVisible.value = true
    ElMessage.success('需求优化完成，请选择是否采纳')
  } catch (error) {
    console.error('需求优化失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? 'AI 优化超时，请稍后重试或检查模型接口超时时间'
      : error.response?.data?.message || 'AI 优化失败，请确认后端服务已启动且模型配置可用'
    ElMessage.error(message)
  } finally {
    loadingMessage.close()
    optimizing.value = false
  }
}

const adoptOptimizedRequirement = () => {
  if (optimizedRequirement.value.trim()) {
    form.requirement = optimizedRequirement.value.trim()
    optimizeVisible.value = false
    ElMessage.success('已采纳优化描述')
  }
}

const analyze = async () => {
  if (!form.requirement.trim()) {
    ElMessage.warning('请先输入模块需求')
    return
  }
  analyzing.value = true
  const loadingMessage = ElMessage({
    message: 'AI 正在分析需求并生成模块设计，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await aiApi.analyze(requestPayload())
    design.value = response.data.data
    const codeGenerated = await generateCodes()
    ElMessage.success(codeGenerated ? '需求分析完成' : '需求分析完成，代码片段稍后可重试生成')
  } catch (error) {
    console.error('分析失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? '需求分析超时，请稍后重试或检查模型接口'
      : error.response?.data?.message || '需求分析失败，请确认后端服务已启动且模型配置可用'
    ElMessage.error(message)
  } finally {
    loadingMessage.close()
    analyzing.value = false
  }
}

const generateCodes = async () => {
  if (!design.value) return false
  
  try {
    const [sqlRes, frontendRes, backendRes] = await Promise.all([
      aiApi.generateSql(design.value),
      aiApi.generateFrontend(design.value),
      aiApi.generateBackend(design.value)
    ])
    
    sqlScript.value = sqlRes.data.data
    frontendCode.value = frontendRes.data.data
    backendCode.value = backendRes.data.data
    return true
  } catch (error) {
    console.error('代码生成失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? '代码片段生成超时，请稍后重试'
      : error.response?.data?.message || '代码片段生成失败，请检查后端服务'
    ElMessage.error(message)
    return false
  }
}

const generate = async () => {
  if (!form.requirement.trim()) {
    ElMessage.warning('请先输入模块需求')
    return
  }
  generating.value = true
  const loadingMessage = ElMessage({
    message: 'AI 正在一键生成模块设计和代码，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await aiApi.complete(requestPayload())
    design.value = response.data.data.design
    sqlScript.value = response.data.data.sqlScript
    frontendCode.value = response.data.data.frontendCode
    backendCode.value = response.data.data.backendCode
    generatedModule.value = response.data.data.module
    window.dispatchEvent(new Event('bizagent-login'))
    ElMessage.success('一键生成、发布并加载完成')
    router.push(response.data.data.runtimePath || `/module-runtime/${design.value.moduleCode}`)
  } catch (error) {
    console.error('生成失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? '一键生成超时，请稍后重试或检查模型接口'
      : error.response?.data?.message || '一键生成失败，请确认后端服务已启动且模型配置可用'
    ElMessage.error(message)
  } finally {
    loadingMessage.close()
    generating.value = false
  }
}

const preview = () => {
  if (!design.value) return
  sessionStorage.setItem('bizagent_preview_module', JSON.stringify({
    ...design.value,
    sqlScript: sqlScript.value,
    frontendCode: frontendCode.value,
    backendCode: backendCode.value
  }))
  router.push('/module/preview/draft')
}

const publish = async () => {
  if (!design.value) {
    ElMessage.warning('请先生成模块设计')
    return
  }
  publishing.value = true
  const loadingMessage = ElMessage({
    message: '正在发布模块并挂载菜单权限，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    await aiApi.publish({ ...design.value, projectId: form.projectId })
    window.dispatchEvent(new Event('bizagent-login'))
    ElMessage.success('模块发布成功')
    router.push(`/module-runtime/${design.value.moduleCode}`)
  } catch (error) {
    console.error('发布失败:', error)
    const message = error.code === 'ECONNABORTED'
      ? '模块发布超时，请检查数据库和后端服务状态'
      : error.response?.data?.message || '模块发布失败，请检查后端日志'
    ElMessage.error(message)
  } finally {
    loadingMessage.close()
    publishing.value = false
  }
}

const loadProjectOptions = async () => {
  try {
    const response = await projectApi.list({ pageNum: 1, pageSize: 100 })
    projectOptions.value = response.data.data.records.filter((item) => item.status === 1)
    const cachedProjectId = Number(localStorage.getItem('bizagent_project_id') || 1)
    form.projectId = projectOptions.value.find((item) => item.id === cachedProjectId)?.id || projectOptions.value[0]?.id || 1
    persistProject()
  } catch (error) {
    console.error('加载项目列表失败:', error)
    form.projectId = Number(localStorage.getItem('bizagent_project_id') || 1)
  }
}

const loadModelOptions = async () => {
  try {
    const response = await modelConfigApi.list({ pageNum: 1, pageSize: 100 })
    modelOptions.value = response.data.data.records.filter((item) => item.status === 1)
    const defaultModel = modelOptions.value.find((item) => item.defaultFlag === 1)
    form.modelConfigId = defaultModel?.id || modelOptions.value[0]?.id || null
  } catch (error) {
    console.error('加载模型配置失败:', error)
    ElMessage.warning('模型配置加载失败，将使用后端默认策略')
  }
}

onMounted(() => {
  loadProjectOptions()
  loadModelOptions()
})
</script>

<style>
.ai-generate {
  max-width: 1240px;
  margin: 0 auto;
}

.generate-hero {
  min-height: 176px;
  padding: 30px;
  margin-bottom: 22px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(20, 83, 99, 0.9)),
    radial-gradient(circle at 80% 10%, rgba(52, 211, 153, 0.46), transparent 30%);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
}

.hero-copy h2 {
  max-width: 560px;
  margin: 0;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 760;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 620px;
  margin: 14px 0 0;
  color: #cbd5e1;
  font-size: 15px;
  line-height: 1.7;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(92px, 1fr));
  gap: 10px;
}

.hero-metrics div {
  min-width: 92px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
}

.hero-metrics strong {
  display: block;
  font-size: 22px;
  line-height: 1.2;
}

.hero-metrics span {
  display: block;
  margin-top: 6px;
  color: #a7f3d0;
  font-size: 12px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  align-items: start;
}

.card {
  margin-bottom: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.card :deep(.el-card__header) {
  padding: 20px 22px;
  border-bottom-color: #edf2f7;
}

.card :deep(.el-card__body) {
  padding: 22px;
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
  line-height: 1.3;
}

.card-header p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.card-header > .el-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #0891b2;
  background: #ecfeff;
}

.input-panel :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 650;
}

.input-panel :deep(.el-textarea__inner),
.input-panel :deep(.el-input__wrapper),
.input-panel :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe3ef inset;
}

.input-panel :deep(.el-textarea__inner) {
  padding: 14px 15px;
  line-height: 1.7;
}

.requirement-tools {
  width: 100%;
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.full-control {
  width: 100%;
}

.option-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.option-card {
  min-height: 82px;
  padding: 13px 8px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  color: #475569;
  background: #ffffff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease, color 0.18s ease;
}

.option-card:hover {
  border-color: #67e8f9;
  color: #0e7490;
  transform: translateY(-1px);
}

.option-card.selected {
  border-color: #22d3ee;
  color: #0f766e;
  background: linear-gradient(180deg, #ecfeff, #f0fdfa);
  box-shadow: 0 10px 26px rgba(34, 211, 238, 0.12);
}

.option-card .el-icon {
  font-size: 22px;
}

.option-card span {
  font-size: 13px;
  white-space: nowrap;
}

.btn-group {
  margin-top: 22px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-group :deep(.el-button) {
  min-width: 128px;
  border-radius: 8px;
  font-weight: 650;
}

.question-list {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.question-list h4 {
  margin: 0 0 10px;
  color: #334155;
  font-size: 14px;
}

.question-list button {
  width: 100%;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  color: #334155;
  background: #f8fafc;
  text-align: left;
  line-height: 1.5;
  cursor: pointer;
}

.question-list button:hover {
  border-color: #67e8f9;
  color: #0e7490;
  background: #ecfeff;
}

.optimize-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.optimize-compare section {
  min-width: 0;
}

.optimize-compare h4 {
  margin: 0 0 8px;
  color: #334155;
  font-size: 14px;
}

.optimize-compare pre {
  min-height: 260px;
  max-height: 420px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #1f2937;
  background: #f8fafc;
  line-height: 1.7;
  font-family: inherit;
  font-size: 13px;
}

.guide-panel {
  display: grid;
  gap: 14px;
}

.guide-card {
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #dbeafe;
  background: #ffffff;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.05);
}

.guide-card .el-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  color: #0e7490;
  background: #ecfeff;
}

.guide-card h3 {
  margin: 14px 0 10px;
  color: #111827;
  font-size: 16px;
}

.guide-card ul {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  line-height: 1.9;
  font-size: 13px;
}

.guide-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.8;
}

.guide-card.muted {
  background: #f8fafc;
}

.result-panel {
  margin-top: 22px;
}

.result-header {
  align-items: flex-start;
}

.result-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.section {
  margin-bottom: 24px;
}

.section h4 {
  margin: 0 0 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.code-block {
  background: #0f172a;
  color: #dbeafe;
  padding: 18px;
  border-radius: 8px;
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.7;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .guide-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .generate-hero {
    padding: 22px;
    display: block;
  }

  .hero-copy h2 {
    font-size: 25px;
  }

  .hero-metrics {
    margin-top: 18px;
    grid-template-columns: 1fr;
  }

  .option-grid,
  .guide-panel,
  .optimize-compare {
    grid-template-columns: 1fr;
  }

  .result-header {
    display: block;
  }

  .result-actions {
    justify-content: flex-start;
    margin-top: 14px;
  }
}
</style>
