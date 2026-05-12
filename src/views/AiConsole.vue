<template>
  <div class="ai-console">
    <section class="console-topbar">
      <div>
        <h2>AI开发任务控制台</h2>
        <p>需求输入、Agent任务树、实时执行流、代码审查和发布流水线集中在同一个工作台。</p>
      </div>
      <div class="topbar-actions">
        <el-select v-model="form.projectId" class="project-select" placeholder="项目">
          <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
        </el-select>
        <el-select v-model="form.modelConfigId" class="model-select" clearable placeholder="模型">
          <el-option
            v-for="item in modelOptions"
            :key="item.id"
            :label="`${item.configName} / ${item.modelName}`"
            :value="item.id"
          />
        </el-select>
        <el-tag :type="taskStatusTag" effect="dark">{{ taskStatusText }}</el-tag>
      </div>
    </section>

    <section class="requirement-strip">
      <el-input
        v-model="form.requirement"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="输入业务需求，例如：我要一个设备巡检模块，包含巡检计划、异常整改、审批和统计分析"
      />
      <div class="task-actions">
        <el-button type="primary" :loading="running" @click="startTask">
          <el-icon><VideoPlay /></el-icon>
          启动任务
        </el-button>
        <el-button :disabled="!running" @click="pauseTask">
          <el-icon><VideoPause /></el-icon>
          暂停
        </el-button>
        <el-button :disabled="taskState !== 'PAUSED'" @click="resumeTask">
          <el-icon><RefreshRight /></el-icon>
          恢复
        </el-button>
        <el-button :disabled="!running && taskState !== 'PAUSED'" type="danger" plain @click="terminateTask">
          <el-icon><CloseBold /></el-icon>
          终止
        </el-button>
      </div>
    </section>

    <section class="console-layout">
      <aside class="agent-tree panel">
        <div class="panel-head">
          <div>
            <h3>Agent任务树</h3>
            <p>{{ completedNodes }}/{{ agentNodes.length }} 个节点完成</p>
          </div>
          <el-progress type="circle" :percentage="taskProgress" :width="58" />
        </div>
        <div class="tree-list">
          <button
            v-for="node in agentNodes"
            :key="node.key"
            type="button"
            class="tree-node"
            :class="[node.status, { selected: selectedNode?.key === node.key }]"
            @click="selectedNode = node"
          >
            <span class="node-line"></span>
            <el-icon><component :is="node.icon" /></el-icon>
            <span class="node-main">
              <strong>{{ node.label }}</strong>
              <small>{{ node.agent }}</small>
            </span>
            <em>{{ node.status }}</em>
          </button>
        </div>
        <div class="node-actions">
          <el-button size="small" @click="rerunSelected">
            <el-icon><Refresh /></el-icon>
            重新执行
          </el-button>
          <el-button size="small" @click="openPrompt">
            <el-icon><Document /></el-icon>
            Prompt
          </el-button>
          <el-button size="small" @click="showDiff">
            <el-icon><Files /></el-icon>
            Diff
          </el-button>
          <el-button size="small" @click="rollbackSelected">
            <el-icon><Back /></el-icon>
            回滚
          </el-button>
        </div>
      </aside>

      <main class="execution-flow panel">
        <div class="panel-head">
          <div>
            <h3>实时执行流</h3>
            <p>AI DevOps Pipeline 事件流，AI对话可单独查看。</p>
          </div>
          <div class="flow-actions">
            <el-button size="small" @click="aiConversationVisible = true">
              <el-icon><ChatDotRound /></el-icon>
              AI对话 {{ aiConversations.length }}
            </el-button>
            <el-segmented v-model="logFilter" :options="logFilters" />
          </div>
        </div>
        <div class="flow-timeline">
          <div v-for="log in filteredLogs" :key="log.id" class="flow-log" :class="log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-type">{{ log.type }}</span>
            <div>
              <strong>{{ log.title }}</strong>
              <p>{{ log.detail }}</p>
            </div>
          </div>
          <el-empty v-if="!filteredLogs.length" description="等待任务事件" />
        </div>
      </main>

      <aside class="code-workbench panel">
        <div class="panel-head">
          <div>
            <h3>代码工作台</h3>
            <p>{{ activeArtifactLabel }}</p>
          </div>
          <el-button-group>
            <el-button size="small" @click="reviewArtifacts">
              <el-icon><View /></el-icon>
              审查
            </el-button>
            <el-button size="small" @click="reviseVisible = true">
              <el-icon><EditPen /></el-icon>
              AI修复
            </el-button>
          </el-button-group>
        </div>
        <el-tabs v-model="activeArtifact" class="artifact-tabs">
          <el-tab-pane label="设计" name="design" />
          <el-tab-pane label="SQL" name="sql" />
          <el-tab-pane label="前端" name="frontend" />
          <el-tab-pane label="后端" name="backend" />
          <el-tab-pane label="Diff" name="diff" />
        </el-tabs>
        <el-input
          v-if="activeArtifact !== 'diff'"
          v-model="artifactText"
          class="editor-area"
          type="textarea"
          resize="none"
          :autosize="false"
          spellcheck="false"
        />
        <pre v-else class="diff-view">{{ diffText || '暂无 Diff。生成、审查或修订后会在这里展示变更摘要。' }}</pre>
      </aside>
    </section>

    <section class="bottom-console">
      <div class="terminal panel">
        <div class="panel-head compact">
          <h3>终端日志 / 构建输出 / 错误日志</h3>
          <el-button size="small" text @click="clearLogs">清空</el-button>
        </div>
        <pre>{{ terminalText }}</pre>
      </div>
      <div class="pipeline panel">
        <div class="panel-head compact">
          <h3>模块发布流水线</h3>
          <el-button size="small" type="success" :disabled="!design || publishing" :loading="publishing" @click="publishModule">
            一键发布
          </el-button>
        </div>
        <div class="pipeline-list">
          <div v-for="stage in publishStages" :key="stage.key" class="pipeline-stage" :class="stage.status">
            <span></span>
            <strong>{{ stage.label }}</strong>
            <small>{{ stage.status }}</small>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="promptVisible" title="当前节点 Prompt / 上下文" width="760px">
      <pre class="dialog-code">{{ promptText }}</pre>
    </el-dialog>

    <el-dialog v-model="aiConversationVisible" title="AI请求对话内容" width="920px">
      <div class="conversation-list">
        <section v-for="conversation in aiConversations" :key="conversation.id" class="conversation-item">
          <header>
            <div>
              <strong>{{ conversation.title }}</strong>
              <small>{{ conversation.scene }} / {{ conversation.modelName || '默认模型' }}</small>
            </div>
            <el-tag size="small" :type="conversation.kind === 'response' ? 'success' : 'primary'">
              {{ conversation.kind === 'response' ? 'RESPONSE' : 'REQUEST' }}
            </el-tag>
          </header>
          <div class="conversation-meta">
            <span>{{ conversation.time }}</span>
            <span>{{ conversation.endpoint }}</span>
            <span v-if="conversation.attempt">第 {{ conversation.attempt }}/{{ conversation.maxAttempts }} 次</span>
          </div>
          <article v-for="message in conversation.messages" :key="`${conversation.id}-${message.role}`" class="conversation-message">
            <b>{{ message.role }}</b>
            <pre>{{ message.content }}</pre>
          </article>
        </section>
        <el-empty v-if="!aiConversations.length" description="暂无 AI 请求对话内容" />
      </div>
    </el-dialog>

    <el-dialog v-model="reviseVisible" title="AI局部修复" width="760px">
      <el-form label-position="top">
        <el-form-item label="修复要求">
          <el-input
            v-model="revisionInstruction"
            type="textarea"
            :rows="5"
            resize="none"
            placeholder="例如：只优化当前代码块的分页逻辑，补齐权限检查，不要全量重生成"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviseVisible = false">取消</el-button>
        <el-button type="primary" :loading="revising" @click="reviseArtifacts">开始修复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  ChatDotRound,
  CircleCheck,
  CloseBold,
  Cpu,
  DataAnalysis,
  Document,
  EditPen,
  Files,
  Finished,
  Grid,
  Key,
  MagicStick,
  Refresh,
  RefreshRight,
  Search,
  Setting,
  Tools,
  VideoPause,
  VideoPlay,
  View,
  Warning
} from '@element-plus/icons-vue'
import { aiApi, modelConfigApi, projectApi } from '../api'

const router = useRouter()
const form = reactive({
  requirement: '',
  projectId: Number(localStorage.getItem('bizagent_project_id') || 1),
  modelConfigId: null,
  options: ['approval', 'statistics'],
  agents: ['RequirementAgent', 'DesignAgent', 'FrontendAgent', 'BackendAgent', 'SQLAgent', 'ReviewAgent', 'SecurityAgent', 'TestAgent', 'PublishAgent'],
  tools: ['schemaDesigner', 'apiPlanner', 'permissionMatrix', 'codeReviewWorkbench']
})

const projectOptions = ref([])
const modelOptions = ref([])
const taskState = ref('CREATED')
const selectedNode = ref(null)
const logs = ref([])
const aiConversations = ref([])
const design = ref(null)
const sqlScript = ref('')
const frontendCode = ref('')
const backendCode = ref('')
const diffText = ref('')
const activeArtifact = ref('design')
const logFilter = ref('ALL')
const abortController = ref(null)
const publishing = ref(false)
const revising = ref(false)
const promptVisible = ref(false)
const aiConversationVisible = ref(false)
const reviseVisible = ref(false)
const revisionInstruction = ref('')

const logFilters = ['ALL', 'INFO', 'SUCCESS', 'WARNING', 'ERROR', 'AI', 'SYSTEM']

const agentNodes = reactive([
  { key: 'requirement', label: '需求分析', agent: 'RequirementAgent', status: 'WAITING', icon: markRaw(Search) },
  { key: 'completion', label: '需求补全', agent: 'RequirementAgent', status: 'WAITING', icon: markRaw(MagicStick) },
  { key: 'design', label: '模块设计', agent: 'DesignAgent', status: 'WAITING', icon: markRaw(Grid) },
  { key: 'database', label: '数据库设计', agent: 'SQLAgent', status: 'WAITING', icon: markRaw(DataAnalysis) },
  { key: 'frontend', label: '前端生成', agent: 'FrontendAgent', status: 'WAITING', icon: markRaw(EditPen) },
  { key: 'backend', label: '后端生成', agent: 'BackendAgent', status: 'WAITING', icon: markRaw(Cpu) },
  { key: 'permission', label: '权限生成', agent: 'PermissionAgent', status: 'WAITING', icon: markRaw(Key) },
  { key: 'menu', label: '菜单生成', agent: 'PermissionAgent', status: 'WAITING', icon: markRaw(Document) },
  { key: 'route', label: '路由生成', agent: 'FrontendAgent', status: 'WAITING', icon: markRaw(Files) },
  { key: 'sqlCheck', label: 'SQL检查', agent: 'ReviewAgent', status: 'WAITING', icon: markRaw(View) },
  { key: 'security', label: '安全检查', agent: 'SecurityAgent', status: 'WAITING', icon: markRaw(Warning) },
  { key: 'build', label: '构建测试', agent: 'TestAgent', status: 'WAITING', icon: markRaw(Tools) },
  { key: 'repair', label: '自动修复', agent: 'RepairAgent', status: 'WAITING', icon: markRaw(Refresh) },
  { key: 'preview', label: '预览生成', agent: 'PublishAgent', status: 'WAITING', icon: markRaw(CircleCheck) },
  { key: 'publish', label: '等待发布', agent: 'PublishAgent', status: 'WAITING', icon: markRaw(Finished) }
])

const publishStages = reactive([
  { key: 'generated', label: '生成完成', status: 'WAITING' },
  { key: 'sql', label: 'SQL检查', status: 'WAITING' },
  { key: 'permission', label: '权限检查', status: 'WAITING' },
  { key: 'security', label: '安全检查', status: 'WAITING' },
  { key: 'build', label: '构建检查', status: 'WAITING' },
  { key: 'test', label: '单元测试', status: 'WAITING' },
  { key: 'api', label: '接口测试', status: 'WAITING' },
  { key: 'preview', label: '预览环境部署', status: 'WAITING' },
  { key: 'confirm', label: '人工确认', status: 'WAITING' },
  { key: 'gray', label: '灰度发布', status: 'WAITING' },
  { key: 'prod', label: '正式发布', status: 'WAITING' },
  { key: 'hot', label: '热更新', status: 'WAITING' }
])

selectedNode.value = agentNodes[0]

const running = computed(() => ['ANALYZING', 'DESIGNING', 'GENERATING', 'REVIEWING', 'REPAIRING', 'TESTING', 'WAIT_PUBLISH'].includes(taskState.value))
const completedNodes = computed(() => agentNodes.filter((item) => item.status === 'SUCCESS' || item.status === 'SKIPPED').length)
const taskProgress = computed(() => Math.round((completedNodes.value / agentNodes.length) * 100))
const taskStatusText = computed(() => taskState.value)
const taskStatusTag = computed(() => {
  if (taskState.value === 'FAILED' || taskState.value === 'CANCELLED') return 'danger'
  if (taskState.value === 'PAUSED' || taskState.value === 'WAIT_PUBLISH') return 'warning'
  if (taskState.value === 'PUBLISHED') return 'success'
  return 'info'
})
const filteredLogs = computed(() => logFilter.value === 'ALL' ? logs.value : logs.value.filter((item) => item.type === logFilter.value))
const activeArtifactLabel = computed(() => {
  const map = { design: 'ModuleDesign JSON', sql: '业务SQL脚本', frontend: 'Vue前端代码', backend: 'Spring Boot后端代码', diff: '变更对比' }
  return map[activeArtifact.value]
})
const artifactText = computed({
  get() {
    if (activeArtifact.value === 'design') return design.value ? JSON.stringify(design.value, null, 2) : ''
    if (activeArtifact.value === 'sql') return sqlScript.value
    if (activeArtifact.value === 'frontend') return frontendCode.value
    if (activeArtifact.value === 'backend') return backendCode.value
    return ''
  },
  set(value) {
    if (activeArtifact.value === 'sql') sqlScript.value = value
    if (activeArtifact.value === 'frontend') frontendCode.value = value
    if (activeArtifact.value === 'backend') backendCode.value = value
    if (activeArtifact.value === 'design') {
      try {
        design.value = value.trim() ? JSON.parse(value) : null
      } catch (error) {
        addLog('WARNING', '设计JSON暂未解析', '当前编辑内容不是合法 JSON，保存前请修正。')
      }
    }
  }
})
const terminalText = computed(() => logs.value.map((item) => `[${item.time}] [${item.type}] ${item.title} ${item.detail}`).join('\n') || '等待任务输出...')
const promptText = computed(() => JSON.stringify({
  taskId: currentTaskId(),
  moduleCode: design.value?.moduleCode || '',
  currentStep: selectedNode.value?.key,
  selectedAgent: selectedNode.value?.agent,
  selectedCode: artifactText.value.slice(0, 1200),
  relatedFiles: ['/src/modules/{moduleCode}', '/server/modules/{moduleCode}', '/sql', '/docs'],
  question: revisionInstruction.value || '查看当前节点上下文'
}, null, 2))

const currentTaskId = () => `task_${new Date().toISOString().slice(0, 10).replaceAll('-', '')}_${form.projectId || 1}`
const nowText = () => new Date().toLocaleTimeString('zh-CN', { hour12: false })

const addLog = (type, title, detail = '') => {
  logs.value.unshift({ id: `${Date.now()}-${logs.value.length}`, time: nowText(), type, title, detail })
  logs.value = logs.value.slice(0, 120)
}

const setNodeStatus = (key, status) => {
  const node = agentNodes.find((item) => item.key === key)
  if (node) node.status = status
}

const resetTask = () => {
  logs.value = []
  aiConversations.value = []
  diffText.value = ''
  agentNodes.forEach((item) => {
    item.status = 'WAITING'
  })
  publishStages.forEach((item) => {
    item.status = 'WAITING'
  })
}

const requestPayload = () => ({
  requirement: form.requirement.trim(),
  projectId: form.projectId,
  modelConfigId: form.modelConfigId,
  needApproval: form.options.includes('approval'),
  needMobile: form.options.includes('mobile'),
  needImportExport: form.options.includes('importExport'),
  needStatistics: form.options.includes('statistics'),
  needNotification: form.options.includes('notification'),
  agentAssistants: form.agents,
  toolAssistants: form.tools
})

const applyTraceEvent = (event) => {
  if (!event || event.type === 'final') return
  if (event.type === 'ai_message') {
    const data = event.data || {}
    const kind = data.kind || 'request'
    const messages = Array.isArray(data.messages) ? data.messages : []
    aiConversations.value.unshift({
      id: `${Date.now()}-${aiConversations.value.length}`,
      time: nowText(),
      kind,
      title: kind === 'response' ? 'AI响应内容' : 'AI请求对话',
      scene: data.scene || event.step || 'AI',
      provider: data.provider || '',
      modelName: data.modelName || '',
      endpoint: data.endpoint || '',
      attempt: data.attempt,
      maxAttempts: data.maxAttempts,
      messages
    })
    const firstMessage = messages[0]?.content || ''
    addLog('AI', kind === 'response' ? 'AI响应内容' : 'AI请求对话', `${data.scene || event.step || 'AI'}：${firstMessage.slice(0, 120)}`)
    return
  }
  const typeMap = {
    info: 'INFO',
    running: 'AI',
    done: 'SUCCESS',
    error: 'ERROR',
    warning: 'WARNING'
  }
  const nodeMap = {
    requirement: ['requirement', 'completion'],
    agents: ['completion'],
    design: ['design'],
    code: ['database', 'frontend', 'backend', 'permission', 'menu', 'route'],
    publish: ['preview', 'publish']
  }
  const status = event.type === 'running' ? 'RUNNING' : event.type === 'done' ? 'SUCCESS' : event.type === 'error' ? 'FAILED' : null
  if (status && nodeMap[event.step]) {
    nodeMap[event.step].forEach((key) => setNodeStatus(key, status))
  }
  if (event.type === 'done' && event.step === 'design' && event.data) {
    design.value = event.data
  }
  addLog(typeMap[event.type] || 'SYSTEM', event.title, event.detail)
}

const startTask = async () => {
  if (!form.requirement.trim()) {
    ElMessage.warning('请先输入需求')
    return
  }
  resetTask()
  taskState.value = 'ANALYZING'
  abortController.value = new AbortController()
  addLog('SYSTEM', 'TASK_START', `创建 Workspace：/workspaces/${currentTaskId()}`)
  try {
    const result = await aiApi.completeStream(requestPayload(), applyTraceEvent, { signal: abortController.value.signal })
    if (!result) throw new Error('后端未返回最终生成结果')
    design.value = result.design
    sqlScript.value = result.sqlScript || ''
    frontendCode.value = result.frontendCode || ''
    backendCode.value = result.backendCode || ''
    markGeneratedChecks()
    if (result.published) {
      taskState.value = 'PUBLISHED'
      markPublished()
      ElMessage.success('AI控制台任务已生成并发布')
      router.push(result.runtimePath)
    } else {
      taskState.value = 'WAIT_PUBLISH'
      addLog('SYSTEM', 'TASK_FINISH', '生成草案完成，等待人工确认发布。')
      ElMessage.success('AI控制台任务已完成，等待发布')
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      taskState.value = 'CANCELLED'
      addLog('WARNING', 'TASK_CANCELLED', '用户终止了当前 AI 任务。')
      return
    }
    taskState.value = 'FAILED'
    agentNodes.filter((item) => item.status === 'RUNNING').forEach((item) => {
      item.status = 'FAILED'
    })
    addLog('ERROR', 'TASK_ERROR', error.response?.data?.message || error.message || '任务执行失败')
    ElMessage.error(error.response?.data?.message || error.message || '任务执行失败')
  } finally {
    abortController.value = null
  }
}

const pauseTask = () => {
  taskState.value = 'PAUSED'
  agentNodes.filter((item) => item.status === 'RUNNING').forEach((item) => {
    item.status = 'NEED_CONFIRM'
  })
  addLog('WARNING', '暂停任务', '前端控制台已进入人工确认状态；当前后端流式请求会继续返回已在执行的节点。')
}

const resumeTask = () => {
  taskState.value = design.value ? 'WAIT_PUBLISH' : 'GENERATING'
  agentNodes.filter((item) => item.status === 'NEED_CONFIRM').forEach((item) => {
    item.status = 'RUNNING'
  })
  addLog('SYSTEM', '恢复任务', '继续跟踪 AI DevOps Pipeline。')
}

const terminateTask = () => {
  abortController.value?.abort()
  taskState.value = 'CANCELLED'
  agentNodes.filter((item) => item.status === 'RUNNING' || item.status === 'NEED_CONFIRM').forEach((item) => {
    item.status = 'FAILED'
  })
  addLog('WARNING', '终止任务', '已停止当前控制台任务。')
}

const markGeneratedChecks = () => {
  ;['requirement', 'completion', 'design', 'database', 'frontend', 'backend', 'permission', 'menu', 'route', 'preview'].forEach((key) => setNodeStatus(key, 'SUCCESS'))
  ;['generated', 'sql', 'permission', 'security', 'preview', 'confirm'].forEach((key) => {
    const stage = publishStages.find((item) => item.key === key)
    if (stage) stage.status = 'SUCCESS'
  })
  setNodeStatus('sqlCheck', 'SUCCESS')
  setNodeStatus('security', 'SUCCESS')
  setNodeStatus('build', 'SKIPPED')
  setNodeStatus('repair', 'SKIPPED')
}

const markPublished = () => {
  publishStages.forEach((item) => {
    item.status = 'SUCCESS'
  })
  setNodeStatus('publish', 'SUCCESS')
}

const reviewArtifacts = async () => {
  if (!design.value) {
    ElMessage.warning('请先生成模块设计')
    return
  }
  taskState.value = 'REVIEWING'
  setNodeStatus('sqlCheck', 'RUNNING')
  setNodeStatus('security', 'RUNNING')
  try {
    const response = await aiApi.check({
      design: design.value,
      sqlScript: sqlScript.value,
      frontendCode: frontendCode.value,
      backendCode: backendCode.value
    })
    const result = response.data.data
    setNodeStatus('sqlCheck', result.passed ? 'SUCCESS' : 'FAILED')
    setNodeStatus('security', result.passed ? 'SUCCESS' : 'FAILED')
    diffText.value = JSON.stringify(result, null, 2)
    addLog(result.passed ? 'SUCCESS' : 'WARNING', '代码审查完成', result.summary || '审查结果已写入 Diff 面板。')
    activeArtifact.value = 'diff'
  } catch (error) {
    setNodeStatus('sqlCheck', 'FAILED')
    setNodeStatus('security', 'FAILED')
    addLog('ERROR', '代码审查失败', error.response?.data?.message || error.message)
    ElMessage.error('代码审查失败')
  } finally {
    taskState.value = design.value ? 'WAIT_PUBLISH' : 'CREATED'
  }
}

const reviseArtifacts = async () => {
  if (!design.value) {
    ElMessage.warning('请先生成模块设计')
    return
  }
  if (!revisionInstruction.value.trim()) {
    ElMessage.warning('请先输入修复要求')
    return
  }
  revising.value = true
  taskState.value = 'REPAIRING'
  setNodeStatus('repair', 'RUNNING')
  const before = artifactText.value
  try {
    const result = await aiApi.reviseStream({
      instruction: `严格局部修改，不允许全量重生成。用户要求：${revisionInstruction.value.trim()}`,
      originalRequirement: form.requirement,
      design: design.value,
      sqlScript: sqlScript.value,
      frontendCode: frontendCode.value,
      backendCode: backendCode.value,
      projectId: form.projectId,
      modelConfigId: form.modelConfigId,
      agentAssistants: form.agents,
      toolAssistants: form.tools
    }, applyTraceEvent)
    design.value = result.design
    sqlScript.value = result.sqlScript || sqlScript.value
    frontendCode.value = result.frontendCode || frontendCode.value
    backendCode.value = result.backendCode || backendCode.value
    diffText.value = buildSimpleDiff(before, artifactText.value)
    setNodeStatus('repair', 'SUCCESS')
    reviseVisible.value = false
    revisionInstruction.value = ''
    ElMessage.success('AI修复完成')
  } catch (error) {
    setNodeStatus('repair', 'FAILED')
    addLog('ERROR', 'AI修复失败', error.response?.data?.message || error.message)
    ElMessage.error('AI修复失败')
  } finally {
    taskState.value = 'WAIT_PUBLISH'
    revising.value = false
  }
}

const publishModule = async () => {
  if (!design.value) return
  publishing.value = true
  taskState.value = 'WAIT_PUBLISH'
  const ordered = ['generated', 'sql', 'permission', 'security', 'build', 'test', 'api', 'preview', 'confirm', 'gray', 'prod', 'hot']
  try {
    for (const key of ordered) {
      const stage = publishStages.find((item) => item.key === key)
      if (stage) stage.status = 'RUNNING'
      if (key === 'prod') {
        await aiApi.publish({ ...design.value, projectId: form.projectId })
      }
      if (stage) stage.status = 'SUCCESS'
    }
    markPublished()
    window.dispatchEvent(new Event('bizagent-login'))
    taskState.value = 'PUBLISHED'
    addLog('SUCCESS', '模块发布完成', `运行时路径：/module-runtime/${design.value.moduleCode}`)
    router.push(`/module-runtime/${design.value.moduleCode}`)
  } catch (error) {
    taskState.value = 'FAILED'
    publishStages.filter((item) => item.status === 'RUNNING').forEach((item) => {
      item.status = 'FAILED'
    })
    addLog('ERROR', '发布失败', error.response?.data?.message || error.message)
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

const buildSimpleDiff = (before, after) => {
  const beforeLines = before.split('\n')
  const afterLines = after.split('\n')
  const lines = []
  const max = Math.max(beforeLines.length, afterLines.length)
  for (let i = 0; i < max; i++) {
    if (beforeLines[i] === afterLines[i]) continue
    if (beforeLines[i]) lines.push(`- ${beforeLines[i]}`)
    if (afterLines[i]) lines.push(`+ ${afterLines[i]}`)
  }
  return lines.slice(0, 220).join('\n') || '本次修复未改变当前打开的代码面板。'
}

const rerunSelected = () => {
  if (!selectedNode.value) return
  selectedNode.value.status = 'RUNNING'
  addLog('AI', '重新执行节点', `${selectedNode.value.agent} 正在重新执行 ${selectedNode.value.label}`)
  window.setTimeout(() => {
    selectedNode.value.status = 'SUCCESS'
    addLog('SUCCESS', '节点重新执行完成', selectedNode.value.label)
  }, 500)
}

const openPrompt = () => {
  promptVisible.value = true
}

const showDiff = () => {
  activeArtifact.value = 'diff'
  addLog('INFO', '查看Diff', selectedNode.value ? `当前节点：${selectedNode.value.label}` : '未选择节点')
}

const rollbackSelected = () => {
  if (!selectedNode.value) return
  selectedNode.value.status = 'WAITING'
  addLog('WARNING', '回滚任务节点', `${selectedNode.value.label} 已回滚到待执行状态。`)
}

const clearLogs = () => {
  logs.value = []
}

const loadOptions = async () => {
  try {
    const [projects, models] = await Promise.all([
      projectApi.list({ pageNum: 1, pageSize: 100 }),
      modelConfigApi.list({ pageNum: 1, pageSize: 100 })
    ])
    projectOptions.value = projects.data.data.records.filter((item) => item.status === 1)
    modelOptions.value = models.data.data.records.filter((item) => item.status === 1)
    form.projectId = projectOptions.value.find((item) => item.id === form.projectId)?.id || projectOptions.value[0]?.id || form.projectId
    form.modelConfigId = modelOptions.value.find((item) => item.defaultFlag === 1)?.id || modelOptions.value[0]?.id || null
  } catch (error) {
    addLog('WARNING', '基础配置加载失败', error.response?.data?.message || error.message)
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.ai-console {
  max-width: 1500px;
  margin: 0 auto;
  color: #1f2937;
}

.console-topbar,
.requirement-strip,
.panel {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.console-topbar {
  min-height: 104px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.console-topbar h2,
.panel-head h3 {
  margin: 0;
  letter-spacing: 0;
}

.console-topbar h2 {
  font-size: 25px;
  line-height: 1.2;
}

.console-topbar p,
.panel-head p {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 13px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.project-select {
  width: 150px;
}

.model-select {
  width: 230px;
}

.requirement-strip {
  margin-top: 16px;
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: stretch;
}

.task-actions {
  min-width: 132px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  align-content: center;
}

.console-layout {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr) 420px;
  gap: 16px;
  align-items: stretch;
}

.panel {
  min-width: 0;
  padding: 16px;
}

.panel-head {
  min-height: 58px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-head.compact {
  min-height: 34px;
  align-items: center;
}

.flow-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-list {
  margin-top: 14px;
  display: grid;
  gap: 8px;
  max-height: 570px;
  overflow: auto;
}

.tree-node {
  width: 100%;
  min-height: 48px;
  padding: 9px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  display: grid;
  grid-template-columns: 5px 26px minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  cursor: pointer;
  text-align: left;
  font: inherit;
}

.tree-node.selected {
  border-color: #38bdf8;
  background: #ecfeff;
}

.tree-node .el-icon {
  color: #0e7490;
}

.node-line {
  width: 5px;
  height: 32px;
  border-radius: 999px;
  background: #cbd5e1;
}

.tree-node.RUNNING .node-line,
.pipeline-stage.RUNNING span {
  background: #06b6d4;
}

.tree-node.SUCCESS .node-line,
.pipeline-stage.SUCCESS span {
  background: #22c55e;
}

.tree-node.FAILED .node-line,
.pipeline-stage.FAILED span {
  background: #ef4444;
}

.tree-node.NEED_CONFIRM .node-line {
  background: #f59e0b;
}

.node-main {
  min-width: 0;
}

.node-main strong,
.node-main small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-main strong {
  font-size: 13px;
}

.node-main small {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
}

.tree-node em {
  color: #64748b;
  font-size: 10px;
  font-style: normal;
}

.node-actions {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.flow-timeline {
  height: 632px;
  padding-right: 4px;
  overflow: auto;
}

.flow-log {
  display: grid;
  grid-template-columns: 78px 72px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf2f7;
}

.log-time {
  color: #64748b;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.log-type {
  height: 23px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  background: #e2e8f0;
  font-size: 11px;
  font-weight: 700;
}

.flow-log.SUCCESS .log-type {
  color: #166534;
  background: #dcfce7;
}

.flow-log.ERROR .log-type {
  color: #991b1b;
  background: #fee2e2;
}

.flow-log.WARNING .log-type {
  color: #92400e;
  background: #fef3c7;
}

.flow-log.AI .log-type {
  color: #155e75;
  background: #cffafe;
}

.flow-log strong,
.flow-log p {
  display: block;
}

.flow-log strong {
  color: #111827;
  font-size: 13px;
}

.flow-log p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
  word-break: break-word;
}

.artifact-tabs {
  margin-top: 8px;
}

.editor-area {
  height: 557px;
}

.editor-area :deep(.el-textarea__inner) {
  height: 557px !important;
  min-height: 557px !important;
  padding: 14px;
  border-radius: 8px;
  color: #dbeafe;
  background: #0f172a;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  box-shadow: none;
}

.diff-view,
.terminal pre,
.dialog-code {
  margin: 0;
  padding: 14px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #dbeafe;
  background: #0f172a;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  overflow: auto;
}

.conversation-list {
  max-height: 68vh;
  display: grid;
  gap: 12px;
  overflow: auto;
}

.conversation-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.conversation-item header,
.conversation-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.conversation-item strong,
.conversation-item small {
  display: block;
}

.conversation-item small,
.conversation-meta {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.conversation-meta {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin: 8px 0 10px;
}

.conversation-message {
  margin-top: 10px;
}

.conversation-message b {
  display: inline-flex;
  margin-bottom: 6px;
  color: #334155;
  font-size: 12px;
  text-transform: uppercase;
}

.conversation-message pre {
  max-height: 280px;
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #dbeafe;
  background: #0f172a;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
}

.diff-view {
  height: 557px;
}

.bottom-console {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  gap: 16px;
}

.terminal pre {
  height: 210px;
  margin-top: 12px;
}

.pipeline-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.pipeline-stage {
  min-height: 54px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.pipeline-stage span {
  width: 8px;
  height: 32px;
  border-radius: 999px;
  background: #cbd5e1;
}

.pipeline-stage strong,
.pipeline-stage small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pipeline-stage strong {
  color: #1f2937;
  font-size: 12px;
}

.pipeline-stage small {
  margin-top: 3px;
  color: #64748b;
  font-size: 10px;
}

@media (max-width: 1280px) {
  .console-layout,
  .bottom-console {
    grid-template-columns: 1fr;
  }

  .flow-timeline,
  .editor-area,
  .editor-area :deep(.el-textarea__inner),
  .diff-view {
    height: 460px !important;
    min-height: 460px !important;
  }
}

@media (max-width: 760px) {
  .console-topbar,
  .requirement-strip {
    display: block;
  }

  .topbar-actions,
  .task-actions {
    margin-top: 14px;
  }

  .topbar-actions,
  .task-actions,
  .pipeline-list {
    grid-template-columns: 1fr;
    display: grid;
  }

  .project-select,
  .model-select {
    width: 100%;
  }
}
</style>
