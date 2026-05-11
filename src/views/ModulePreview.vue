
<template>
  <div class="module-preview">
    <el-card title="模块预览" class="card">
      <el-alert type="info" title="预览提示" description="此页面只预览 AI 生成的功能效果，不写入系统。确认无误后可发布运行到系统中。" />
      
      <div v-if="moduleData" class="preview-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模块名称">{{ moduleData.moduleName }}</el-descriptions-item>
          <el-descriptions-item label="模块编码">{{ moduleData.moduleCode }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ moduleData.description }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider />
        
        <el-tabs v-if="moduleData.tables" v-model="activeTab">
          <el-tab-pane label="功能预览" name="feature">
            <div class="feature-preview">
              <div class="preview-toolbar">
                <div>
                  <h3>{{ moduleData.moduleName }}列表</h3>
                  <p>{{ moduleData.description }}</p>
                </div>
                <div>
                  <el-button v-if="hasPermission('import')" plain>导入</el-button>
                  <el-button v-if="hasPermission('export')" plain>导出</el-button>
                  <el-button v-if="hasPermission('statistics')" plain>统计</el-button>
                  <el-button type="primary">新增</el-button>
                </div>
              </div>

              <el-table :data="previewRows" border>
                <el-table-column
                  v-for="field in visibleFields"
                  :key="field.fieldName"
                  :prop="field.fieldName"
                  :label="field.label || field.fieldName"
                  min-width="140"
                />
                <el-table-column label="操作" width="260" fixed="right">
                  <template #default>
                    <el-button size="small">详情</el-button>
                    <el-button size="small">编辑</el-button>
                    <el-button v-if="hasPermission('approve')" size="small" type="success">审批</el-button>
                    <el-button v-if="hasPermission('notify')" size="small">提醒</el-button>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-row :gutter="20" class="preview-panels">
                <el-col :span="12">
                  <h4>表单预览</h4>
                  <el-form label-width="110px">
                    <el-form-item
                      v-for="field in formFields"
                      :key="field.fieldName"
                      :label="field.label || field.fieldName"
                      :required="field.required"
                    >
                      <el-input-number v-if="field.type === 'number'" class="full-control" :model-value="0" :controls="false" />
                      <el-date-picker v-else-if="field.type === 'date'" class="full-control" type="date" />
                      <el-date-picker v-else-if="field.type === 'datetime'" class="full-control" type="datetime" />
                      <el-select v-else-if="field.type === 'select'" class="full-control" placeholder="请选择">
                        <el-option v-for="option in fieldOptions(field)" :key="option.value" :label="option.label" :value="option.value" />
                      </el-select>
                      <el-input v-else-if="field.type === 'textarea'" type="textarea" :rows="3" :placeholder="field.placeholder" />
                      <el-input v-else :placeholder="field.placeholder" />
                    </el-form-item>
                  </el-form>
                </el-col>
                <el-col :span="12">
                  <h4>详情预览</h4>
                  <el-descriptions :column="1" border>
                    <el-descriptions-item v-for="field in formFields" :key="field.fieldName" :label="field.label || field.fieldName">
                      {{ previewRows[0]?.[field.fieldName] || '-' }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>
          <el-tab-pane label="数据库表" name="tables">
            <div v-for="table in moduleData.tables" :key="table.tableName" class="table-preview">
              <h4>{{ table.tableName }} ({{ table.tableComment }})</h4>
              <el-table :data="table.columns" border>
                <el-table-column prop="columnName" label="字段名" />
                <el-table-column prop="dataType" label="类型" />
                <el-table-column prop="comment" label="注释" />
                <el-table-column prop="nullable" label="可空" :formatter="boolFormatter" />
                <el-table-column prop="primaryKey" label="主键" :formatter="boolFormatter" />
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="页面路由" name="pages">
            <el-table :data="moduleData.pages" border>
              <el-table-column prop="pageName" label="页面名称" />
              <el-table-column prop="pageType" label="页面类型" />
              <el-table-column prop="path" label="路由路径" />
              <el-table-column prop="component" label="组件路径" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="API接口" name="apis">
            <el-table :data="moduleData.apis" border>
              <el-table-column prop="apiName" label="接口名称" />
              <el-table-column prop="method" label="方法" />
              <el-table-column prop="path" label="路径" />
              <el-table-column prop="description" label="描述" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="权限点" name="permissions">
            <el-table :data="moduleData.permissions" border>
              <el-table-column prop="permissionCode" label="权限编码" />
              <el-table-column prop="permissionName" label="权限名称" />
              <el-table-column prop="moduleName" label="所属模块" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="菜单" name="menus">
            <el-table :data="moduleData.menus" border>
              <el-table-column prop="menuName" label="菜单名称" />
              <el-table-column prop="path" label="路径" />
              <el-table-column prop="component" label="组件" />
              <el-table-column prop="menuType" label="类型" :formatter="menuTypeFormatter" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="moduleData.sqlScript" label="SQL脚本" name="sql">
            <pre class="code-block">{{ moduleData.sqlScript }}</pre>
          </el-tab-pane>
          <el-tab-pane v-if="moduleData.frontendCode" label="前端代码" name="frontend">
            <pre class="code-block">{{ moduleData.frontendCode }}</pre>
          </el-tab-pane>
          <el-tab-pane v-if="moduleData.backendCode" label="后端代码" name="backend">
            <pre class="code-block">{{ moduleData.backendCode }}</pre>
          </el-tab-pane>
        </el-tabs>
        <el-empty v-else description="该模块已发布为运行模块，可在模块管理中点击运行进入通用运行页。" />
      </div>
      
      <div class="btn-group" style="margin-top: 20px;">
        <el-button @click="goBack">返回</el-button>
        <el-button type="success" :loading="publishing" :disabled="!moduleData" @click="publish">发布运行到系统</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { moduleApi, aiApi } from '../api'

const route = useRoute()
const router = useRouter()

const moduleData = ref(null)
const activeTab = ref('feature')
const publishing = ref(false)

const systemColumns = ['id', 'create_by', 'create_time', 'update_by', 'update_time', 'del_flag', 'project_id']

const primaryTable = computed(() => moduleData.value?.tables?.[0] || {})
const visibleFields = computed(() => {
  const listPage = moduleData.value?.pages?.find((page) => page.pageType === 'list')
  if (listPage?.fields?.length) return listPage.fields.filter((field) => !systemColumns.includes(field.fieldName))
  return (primaryTable.value.columns || [])
    .filter((column) => !systemColumns.includes(column.columnName) && !column.primaryKey)
    .map((column) => ({
      fieldName: column.columnName,
      label: column.comment,
      type: inferFieldType(column),
      required: !column.nullable,
      placeholder: `请输入${column.comment || column.columnName}`,
      options: column.columnName === 'status' ? 'draft:草稿,submitted:已提交,approved:已通过,rejected:已驳回' : ''
    }))
})
const formFields = computed(() => visibleFields.value.filter((field) => field.fieldName !== 'create_time'))
const previewRows = computed(() => {
  const row = {}
  visibleFields.value.forEach((field) => {
    row[field.fieldName] = previewValue(field)
  })
  return [row]
})

const boolFormatter = (row) => {
  return row ? '是' : '否'
}

const inferFieldType = (column) => {
  const name = column.columnName || ''
  const dataType = (column.dataType || '').toUpperCase()
  if (name === 'status' || name.endsWith('_status')) return 'select'
  if (dataType.includes('DATETIME')) return 'datetime'
  if (dataType === 'DATE') return 'date'
  if (dataType.includes('INT') || dataType.includes('DECIMAL')) return 'number'
  if (dataType.includes('500') || dataType === 'TEXT' || name.includes('remark') || name.includes('desc')) return 'textarea'
  return 'input'
}

const fieldOptions = (field) => String(field.options || '')
  .split(',')
  .filter(Boolean)
  .map((item) => {
    const [value, label] = item.split(':')
    return { value, label: label || value }
  })

const previewValue = (field) => {
  if (field.type === 'number') return 1
  if (field.type === 'date') return '2026-05-11'
  if (field.type === 'datetime') return '2026-05-11 09:00:00'
  if (field.type === 'select') return fieldOptions(field)[0]?.label || '草稿'
  if (field.fieldName?.includes('no') || field.fieldName?.includes('code')) return `${moduleData.value?.moduleCode || 'biz'}-001`
  return field.label ? `示例${field.label}` : '示例数据'
}

const hasPermission = (action) => {
  return moduleData.value?.permissions?.some((item) => item.permissionCode?.endsWith(`:${action}`))
}

const menuTypeFormatter = (row) => {
  const types = { 1: '目录', 2: '菜单', 3: '按钮' }
  return types[row.menuType] || row.menuType
}

const loadModule = async () => {
  const moduleId = route.params.id
  if (moduleId === 'draft') {
    const cached = sessionStorage.getItem('bizagent_preview_module')
    moduleData.value = cached ? JSON.parse(cached) : null
    if (moduleData.value?.projectId) {
      localStorage.setItem('bizagent_project_id', String(moduleData.value.projectId))
    }
    return
  }
  try {
    const response = await moduleApi.get(moduleId)
    moduleData.value = response.data.data
  } catch (error) {
    console.error('加载模块失败:', error)
  }
}

const goBack = () => {
  router.push('/system/module')
}

const publish = async () => {
  if (!moduleData.value) return
  publishing.value = true
  try {
    const response = await aiApi.publish({
      ...moduleData.value,
      projectId: moduleData.value.projectId || Number(localStorage.getItem('bizagent_project_id') || 1)
    })
    const module = response.data.data
    sessionStorage.removeItem('bizagent_preview_module')
    window.dispatchEvent(new Event('bizagent-login'))
    ElMessage.success('模块已发布运行到系统')
    router.push(`/module-runtime/${module.moduleCode || moduleData.value.moduleCode}`)
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error(error.response?.data?.message || '发布失败，请检查 AI 生成结果和后端日志')
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  loadModule()
})
</script>

<style>
.module-preview {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.preview-content {
  margin-top: 20px;
}

.feature-preview {
  padding: 4px 0;
}

.preview-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.preview-toolbar h3 {
  margin: 0;
}

.preview-toolbar p {
  margin: 6px 0 0;
  color: #64748b;
}

.preview-panels {
  margin-top: 22px;
}

.full-control {
  width: 100%;
}

.table-preview {
  margin-bottom: 20px;
}

.table-preview h4 {
  margin-bottom: 10px;
  color: #666;
}

.btn-group button {
  margin-right: 10px;
}

.code-block {
  max-height: 420px;
  padding: 16px;
  overflow: auto;
  border-radius: 8px;
  color: #dbeafe;
  background: #0f172a;
  line-height: 1.7;
}
</style>
