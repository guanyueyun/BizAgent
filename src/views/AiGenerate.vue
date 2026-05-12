
<template>
  <div class="ai-generate">
    <section class="generate-hero">
      <div class="hero-copy">
        <h2>把业务需求变成可运行模块</h2>
        <p>描述流程、字段和权限，BizAgent 会生成模块设计、SQL、前端页面和后端接口草案。</p>
      </div>
      <div class="hero-metrics">
        <div>
          <strong>{{ completedStepCount }}/{{ generationSteps.length }}</strong>
          <span>生成进度</span>
        </div>
        <div>
          <strong>{{ form.agents.length }}</strong>
          <span>协同 Agent</span>
        </div>
        <div>
          <strong>{{ form.tools.length }}</strong>
          <span>工具辅助</span>
        </div>
      </div>
    </section>

    <section class="generation-board">
      <div class="board-head">
        <div>
          <h3>生成过程可视化</h3>
          <p>{{ currentStageText }}</p>
        </div>
        <el-progress :percentage="generationProgress" :stroke-width="10" />
      </div>
      <div class="step-rail">
        <div
          v-for="step in generationSteps"
          :key="step.key"
          class="pipeline-step"
          :class="step.status"
        >
          <el-icon><component :is="step.icon" /></el-icon>
          <div>
            <strong>{{ step.title }}</strong>
            <span>{{ step.description }}</span>
          </div>
        </div>
      </div>
      <div class="agent-flow">
        <div
          v-for="agent in selectedAgentItems"
          :key="agent.value"
          class="agent-node"
          :class="{ active: isRunning }"
        >
          <el-icon><component :is="agent.icon" /></el-icon>
          <span>{{ agent.label }}</span>
        </div>
      </div>
      <div v-if="generationLogs.length" class="generation-log">
        <div class="log-title">执行轨迹</div>
        <div v-for="item in generationLogs" :key="item.id" class="log-line" :class="item.type">
          <span class="log-time">{{ item.time }}</span>
          <span class="log-type">{{ item.typeText }}</span>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.detail }}</p>
          </div>
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
          <el-form-item label="Agent 辅助">
            <div class="assist-grid agent-grid">
              <button
                v-for="item in agentItems"
                :key="item.value"
                type="button"
                class="assist-card"
                :class="{ selected: form.agents.includes(item.value) }"
                @click="toggleAgent(item.value)"
              >
                <el-icon><component :is="item.icon" /></el-icon>
                <strong>{{ item.label }}</strong>
                <span>{{ item.desc }}</span>
              </button>
            </div>
          </el-form-item>
          <el-form-item label="工具辅助">
            <div class="assist-grid tool-grid">
              <button
                v-for="item in toolItems"
                :key="item.value"
                type="button"
                class="tool-chip"
                :class="{ selected: form.tools.includes(item.value) }"
                @click="toggleTool(item.value)"
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
          <h3>Agent 协作视角</h3>
          <ul>
            <li v-for="item in selectedAgentItems" :key="item.value">{{ item.label }}：{{ item.desc }}</li>
          </ul>
        </div>
        <div class="guide-card muted">
          <el-icon><Document /></el-icon>
          <h3>工具输出范围</h3>
          <p>{{ selectedToolSummary }}</p>
        </div>
        <div class="guide-card muted">
          <el-icon><DataLine /></el-icon>
          <h3>推荐描述结构</h3>
          <p>业务对象、核心流程、字段规则、权限范围、统计维度、审批节点和消息触发条件。</p>
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
            <el-button :loading="revising" @click="reviseVisible = true">
              <el-icon><MagicStick /></el-icon>
              AI继续修改
            </el-button>
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

    <el-dialog v-model="reviseVisible" title="AI继续修改当前结果" width="760px">
      <el-form label-position="top">
        <el-form-item label="修改意见">
          <el-input
            v-model="revisionInstruction"
            type="textarea"
            :rows="6"
            resize="none"
            placeholder="例如：把客户名称改成必填；增加合同到期提醒；列表增加负责人筛选；审批通过后状态改为已归档"
          />
        </el-form-item>
      </el-form>
      <div class="revision-hint">
        本次会把当前模块设计、SQL、前端代码和后端代码一起提交给后端 AI 修订接口，返回后直接替换当前结果。
      </div>
      <template #footer>
        <el-button @click="reviseVisible = false">取消</el-button>
        <el-button type="primary" :loading="revising" @click="reviseGeneration">开始修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, markRaw, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Brush,
  Connection,
  Cpu,
  DataAnalysis,
  DataLine,
  Document,
  Finished,
  Grid,
  Key,
  MagicStick,
  Monitor,
  Operation,
  QuestionFilled,
  Search,
  Tools,
  Upload,
  Warning
} from '@element-plus/icons-vue'
import { aiApi, modelConfigApi, projectApi } from '../api'

const router = useRouter()
const formRef = ref(null)
const form = reactive({
  requirement: '',
  projectId: null,
  modelConfigId: null,
  options: [],
  agents: ['businessAnalyst', 'dataArchitect', 'frontendEngineer', 'backendEngineer', 'qaReviewer'],
  tools: ['schemaDesigner', 'apiPlanner', 'uiBuilder', 'permissionMatrix']
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
const revising = ref(false)
const optimizedRequirement = ref('')
const generatedModule = ref(null)
const generationLogs = ref([])
const reviseVisible = ref(false)
const revisionInstruction = ref('')
const isRunning = computed(() => analyzing.value || questioning.value || generating.value || optimizing.value || publishing.value)

const generationSteps = reactive([
  { key: 'requirement', title: '需求理解', description: '整理业务对象和目标', status: 'waiting', icon: markRaw(Search) },
  { key: 'agents', title: 'Agent 协同', description: '多角色审查和补全', status: 'waiting', icon: markRaw(Cpu) },
  { key: 'design', title: '模块设计', description: '表、页面、接口、权限', status: 'waiting', icon: markRaw(Grid) },
  { key: 'code', title: '代码生成', description: 'SQL、前端、后端草案', status: 'waiting', icon: markRaw(Tools) },
  { key: 'publish', title: '发布加载', description: '菜单、权限、运行时挂载', status: 'waiting', icon: markRaw(Finished) }
])

const optionItems = [
  { label: '审批流程', value: 'approval', icon: markRaw(Finished) },
  { label: '移动端适配', value: 'mobile', icon: markRaw(Monitor) },
  { label: '导入导出', value: 'importExport', icon: markRaw(Upload) },
  { label: '统计分析', value: 'statistics', icon: markRaw(DataAnalysis) },
  { label: '消息提醒', value: 'notification', icon: markRaw(Bell) }
]

const agentItems = [
  { label: '业务分析', value: 'businessAnalyst', icon: markRaw(Search), desc: '拆流程、对象和状态' },
  { label: '数据架构', value: 'dataArchitect', icon: markRaw(DataLine), desc: '校准表字段和类型' },
  { label: '前端工程', value: 'frontendEngineer', icon: markRaw(Brush), desc: '生成可操作页面' },
  { label: '后端工程', value: 'backendEngineer', icon: markRaw(Cpu), desc: '补齐接口和服务逻辑' },
  { label: '测试审查', value: 'qaReviewer', icon: markRaw(Finished), desc: '检查闭环和边界' },
  { label: '安全审查', value: 'securityReviewer', icon: markRaw(Warning), desc: '关注权限和数据范围' }
]

const toolItems = [
  { label: '表结构设计器', value: 'schemaDesigner', icon: markRaw(DataLine) },
  { label: 'API 规划器', value: 'apiPlanner', icon: markRaw(Operation) },
  { label: '页面搭建器', value: 'uiBuilder', icon: markRaw(Brush) },
  { label: '权限矩阵', value: 'permissionMatrix', icon: markRaw(Key) },
  { label: '校验规则', value: 'validationRules', icon: markRaw(Finished) },
  { label: '流程编排', value: 'workflowOrchestrator', icon: markRaw(Connection) },
  { label: '通知助手', value: 'notificationAssistant', icon: markRaw(Bell) },
  { label: '移动适配', value: 'mobileAdapter', icon: markRaw(Monitor) }
]

const completedStepCount = computed(() => generationSteps.filter((item) => item.status === 'done').length)
const generationProgress = computed(() => Math.round((completedStepCount.value / generationSteps.length) * 100))
const currentStageText = computed(() => {
  const running = generationSteps.find((item) => item.status === 'running')
  if (running) return `${running.title}进行中：${running.description}`
  if (completedStepCount.value === generationSteps.length) return '生成链路已完成，可预览或发布运行模块。'
  return '等待开始生成，选择 Agent 和工具后会在这里展示推进状态。'
})
const selectedAgentItems = computed(() => agentItems.filter((item) => form.agents.includes(item.value)))
const selectedToolSummary = computed(() => {
  const labels = toolItems.filter((item) => form.tools.includes(item.value)).map((item) => item.label)
  return labels.length ? labels.join('、') : '未选择工具辅助，将只使用基础模块生成能力。'
})

const toggleOption = (value) => {
  const index = form.options.indexOf(value)
  if (index === -1) {
    form.options.push(value)
  } else {
    form.options.splice(index, 1)
  }
}

const toggleFromList = (list, value) => {
  const index = list.indexOf(value)
  if (index === -1) {
    list.push(value)
  } else {
    list.splice(index, 1)
  }
}

const toggleAgent = (value) => toggleFromList(form.agents, value)

const toggleTool = (value) => toggleFromList(form.tools, value)

const resetGenerationSteps = (activeKey) => {
  generationLogs.value = []
  generationSteps.forEach((item) => {
    item.status = item.key === activeKey ? 'running' : 'waiting'
  })
}

const logTypeText = {
  running: '执行',
  done: '完成',
  error: '失败',
  info: '信息'
}

const nowText = () => new Date().toLocaleTimeString('zh-CN', { hour12: false })

const addExecutionLog = (type, title, detail) => {
  generationLogs.value.unshift({
    id: `${Date.now()}-${generationLogs.value.length}`,
    time: nowText(),
    type,
    typeText: logTypeText[type] || '信息',
    title,
    detail
  })
  generationLogs.value = generationLogs.value.slice(0, 12)
}

const setStepStatus = (key, status, title, detail) => {
  const step = generationSteps.find((item) => item.key === key)
  if (step) {
    step.status = status
  }
  if (title) {
    addExecutionLog(status === 'waiting' ? 'info' : status, title, detail || step?.description || '')
  }
}

const runStep = async (key, message, task) => {
  setStepStatus(key, 'running', message, generationSteps.find((item) => item.key === key)?.description)
  const result = await task()
  setStepStatus(key, 'done', `${generationSteps.find((item) => item.key === key)?.title || '步骤'}完成`, '后端接口已返回结果，进入下一步。')
  return result
}

const failActiveSteps = () => {
  generationSteps.forEach((item) => {
    if (item.status === 'running') {
      item.status = 'error'
    }
  })
}

const apiErrorMessage = (error, fallback = '请求失败') => (
  error?.response?.data?.message
  || error?.response?.data?.data?.message
  || error?.message
  || fallback
)

const applyServerTraceEvent = (event) => {
  if (!event || event.type === 'final') return
  if (event.type === 'info') {
    addExecutionLog('info', `后端：${event.title}`, event.detail || '')
    return
  }
  if (event.step) {
    setStepStatus(event.step, event.type, `后端：${event.title}`, event.detail || '')
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
    needNotification: form.options.includes('notification'),
    agentAssistants: [...form.agents],
    toolAssistants: [...form.tools]
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
  resetGenerationSteps('requirement')
  const loadingMessage = ElMessage({
    message: 'AI 正在生成需求追问，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await runStep('requirement', '需求追问 Agent 正在检查缺失信息', () => aiApi.questions(requestPayload()))
    questions.value = response.data.data || []
    setStepStatus('agents', 'done', '已根据当前 Agent 和工具选择生成补充问题')
    if (questions.value.length) {
      ElMessage.success('需求追问已生成')
    } else {
      ElMessage.warning('暂未生成追问，请补充更多需求上下文后重试')
    }
  } catch (error) {
    failActiveSteps()
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
  resetGenerationSteps('requirement')
  const loadingMessage = ElMessage({
    message: 'AI 正在优化需求描述，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await runStep('requirement', '需求分析 Agent 正在重写业务描述', () => aiApi.optimize(requestPayload()))
    optimizedRequirement.value = response.data.data || ''
    if (!optimizedRequirement.value.trim()) {
      ElMessage.warning('未生成优化内容，请检查模型配置后重试')
      return
    }
    optimizeVisible.value = true
    setStepStatus('agents', 'done', '需求优化已注入已选 Agent 和工具视角')
    ElMessage.success('需求优化完成，请选择是否采纳')
  } catch (error) {
    failActiveSteps()
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
  resetGenerationSteps('requirement')
  const loadingMessage = ElMessage({
    message: 'AI 正在分析需求并生成模块设计，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const response = await runStep('requirement', '正在整理需求上下文和生成选项', async () => {
      setStepStatus('agents', 'running', `已启用 ${form.agents.length} 个 Agent、${form.tools.length} 个工具辅助`)
      const result = await aiApi.analyze(requestPayload())
      setStepStatus('agents', 'done', 'Agent 协同审查完成')
      return result
    })
    setStepStatus('design', 'running', '正在装配模块设计结果')
    design.value = response.data.data
    setStepStatus('design', 'done', '模块设计已生成')
    const codeGenerated = await generateCodes()
    ElMessage.success(codeGenerated ? '需求分析完成' : '需求分析完成，代码片段稍后可重试生成')
  } catch (error) {
    failActiveSteps()
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
    setStepStatus('code', 'running', '开始生成 SQL 脚本', '调用 /ai/generate/sql，生成表结构和索引脚本。')
    const sqlRes = await aiApi.generateSql(design.value)
    sqlScript.value = sqlRes.data.data

    setStepStatus('code', 'running', '开始生成前端代码', '调用 /ai/generate/frontend，生成 api.js、列表、表单和详情页。')
    const frontendRes = await aiApi.generateFrontend(design.value)
    frontendCode.value = frontendRes.data.data

    setStepStatus('code', 'running', '开始生成后端代码', '调用 /ai/generate/backend，生成 Entity、Mapper、Service 和 Controller。')
    const backendRes = await aiApi.generateBackend(design.value)
    backendCode.value = backendRes.data.data

    setStepStatus('code', 'done', '三类代码片段已生成', 'SQL、前端和后端代码均已返回，可切换结果标签查看。')
    return true
  } catch (error) {
    setStepStatus('code', 'error', '代码片段生成失败', error.response?.data?.message || error.message || '请检查模型配置和后端日志。')
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
  resetGenerationSteps('requirement')
  const loadingMessage = ElMessage({
    message: 'AI 正在一键生成模块设计和代码，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    const result = await aiApi.completeStream(requestPayload(), applyServerTraceEvent)
    if (!result) {
      throw new Error('后端未返回最终生成结果')
    }
    design.value = result.design
    sqlScript.value = result.sqlScript
    frontendCode.value = result.frontendCode
    backendCode.value = result.backendCode
    generatedModule.value = result.module
    window.dispatchEvent(new Event('bizagent-login'))
    if (result.published && result.runtimePath) {
      ElMessage.success('一键生成、发布并加载完成')
      router.push(result.runtimePath)
    } else {
      ElMessage.warning('生成草案完成，当前账号缺少发布权限，可继续修改或预览')
    }
  } catch (error) {
    failActiveSteps()
    addExecutionLog('error', '一键生成链路中断', error.response?.data?.message || error.message || '请检查模型配置和后端服务状态。')
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

const reviseGeneration = async () => {
  if (!design.value) {
    ElMessage.warning('请先生成模块设计')
    return
  }
  if (!revisionInstruction.value.trim()) {
    ElMessage.warning('请先输入修改意见')
    return
  }
  revising.value = true
  resetGenerationSteps('requirement')
  const payload = {
    instruction: revisionInstruction.value.trim(),
    originalRequirement: form.requirement,
    design: design.value,
    sqlScript: sqlScript.value,
    frontendCode: frontendCode.value,
    backendCode: backendCode.value,
    projectId: form.projectId,
    modelConfigId: form.modelConfigId,
    agentAssistants: [...form.agents],
    toolAssistants: [...form.tools]
  }
  try {
    let result
    try {
      result = await aiApi.reviseStream(payload, applyServerTraceEvent)
    } catch (streamError) {
      addExecutionLog('info', '流式修改不可用，切换普通修订接口', apiErrorMessage(streamError, '正在改用 /ai/revise。'))
      setStepStatus('design', 'running', '调用普通 AI 修订接口', '后端仍会基于当前结果和修改意见真实调用模型。')
      const response = await aiApi.revise(payload)
      result = response.data.data
      setStepStatus('design', 'done', '模块设计修订完成', result.design ? `${result.design.moduleName} / ${result.design.moduleCode}` : '修订结果已返回')
      setStepStatus('code', 'done', '代码重新生成完成', 'SQL、前端和后端草案已全部更新。')
    }
    if (!result) {
      throw new Error('后端未返回修订结果')
    }
    design.value = result.design
    sqlScript.value = result.sqlScript
    frontendCode.value = result.frontendCode
    backendCode.value = result.backendCode
    generatedModule.value = null
    if (Array.isArray(result.warnings) && result.warnings.length) {
      result.warnings.forEach((warning) => addExecutionLog('warning', '继续修改产物提示', warning))
      ElMessage.warning('模块设计已修改，部分代码产物保留了修改前内容')
    }
    reviseVisible.value = false
    revisionInstruction.value = ''
    ElMessage.success('AI 继续修改完成，当前结果已更新')
  } catch (error) {
    failActiveSteps()
    const message = apiErrorMessage(error, '请检查模型配置和后端服务状态。')
    addExecutionLog('error', 'AI 继续修改中断', message)
    console.error('AI继续修改失败:', error)
    ElMessage.error(message)
  } finally {
    revising.value = false
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
  if (!generationSteps.some((item) => item.status === 'done')) {
    resetGenerationSteps('publish')
  } else {
    setStepStatus('publish', 'running', '正在同步表结构、菜单和权限')
  }
  const loadingMessage = ElMessage({
    message: '正在发布模块并挂载菜单权限，请稍候...',
    type: 'info',
    duration: 0,
    showClose: true
  })
  try {
    await aiApi.publish({ ...design.value, projectId: form.projectId })
    setStepStatus('publish', 'done', '模块发布成功，运行时已加载')
    window.dispatchEvent(new Event('bizagent-login'))
    ElMessage.success('模块发布成功')
    router.push(`/module-runtime/${design.value.moduleCode}`)
  } catch (error) {
    failActiveSteps()
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

.generation-board {
  margin-bottom: 22px;
  padding: 22px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.board-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 20px;
  align-items: center;
}

.board-head h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.board-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.step-rail {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.pipeline-step {
  min-height: 90px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  display: flex;
  gap: 11px;
  align-items: flex-start;
}

.pipeline-step .el-icon {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 8px;
  color: #64748b;
  background: #ffffff;
}

.pipeline-step strong,
.pipeline-step span {
  display: block;
}

.pipeline-step strong {
  color: #1f2937;
  font-size: 14px;
}

.pipeline-step span {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.pipeline-step.running {
  border-color: #22d3ee;
  background: #ecfeff;
}

.pipeline-step.running .el-icon {
  color: #0e7490;
  background: #cffafe;
}

.pipeline-step.done {
  border-color: #86efac;
  background: #f0fdf4;
}

.pipeline-step.done .el-icon {
  color: #15803d;
  background: #dcfce7;
}

.pipeline-step.error {
  border-color: #fecaca;
  background: #fef2f2;
}

.pipeline-step.error .el-icon {
  color: #b91c1c;
  background: #fee2e2;
}

.agent-flow {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.agent-node {
  padding: 8px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  color: #475569;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.agent-node.active {
  border-color: #67e8f9;
  color: #0e7490;
  background: #ecfeff;
}

.generation-log {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #334155;
  background: #f8fafc;
  font-size: 12px;
  line-height: 1.7;
}

.log-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-weight: 700;
}

.log-line {
  display: grid;
  grid-template-columns: 76px 42px minmax(0, 1fr);
  gap: 10px;
  padding: 9px 0;
  border-top: 1px solid #e5e7eb;
}

.log-time {
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.log-type {
  height: 22px;
  border-radius: 8px;
  color: #475569;
  background: #e2e8f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.log-line.running .log-type {
  color: #0e7490;
  background: #cffafe;
}

.log-line.done .log-type {
  color: #15803d;
  background: #dcfce7;
}

.log-line.error .log-type {
  color: #b91c1c;
  background: #fee2e2;
}

.log-line strong {
  color: #1f2937;
}

.log-line p {
  margin: 2px 0 0;
  color: #64748b;
  line-height: 1.5;
  word-break: break-word;
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

.assist-grid {
  width: 100%;
  display: grid;
  gap: 10px;
}

.agent-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.tool-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assist-card,
.tool-chip {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  color: #475569;
  background: #ffffff;
  cursor: pointer;
  font: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.assist-card {
  min-height: 96px;
  padding: 13px;
  text-align: left;
}

.assist-card .el-icon,
.tool-chip .el-icon {
  color: #0e7490;
  font-size: 18px;
}

.assist-card strong,
.assist-card span {
  display: block;
}

.assist-card strong {
  margin-top: 10px;
  color: #1f2937;
  font-size: 14px;
}

.assist-card span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.tool-chip {
  min-height: 42px;
  padding: 9px 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: 13px;
}

.assist-card:hover,
.tool-chip:hover,
.assist-card.selected,
.tool-chip.selected {
  border-color: #22d3ee;
  color: #0f766e;
  background: #ecfeff;
  box-shadow: 0 10px 24px rgba(34, 211, 238, 0.1);
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

.revision-hint {
  padding: 12px 14px;
  border-radius: 8px;
  color: #475569;
  background: #f8fafc;
  line-height: 1.7;
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

  .step-rail,
  .agent-grid {
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

  .board-head,
  .option-grid,
  .guide-panel,
  .optimize-compare,
  .step-rail,
  .agent-grid,
  .tool-grid {
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
