
<template>
  <div class="module-preview">
    <el-card title="模块预览" class="card">
      <el-alert type="info" title="预览提示" description="此页面用于预览AI生成的模块设计，确认无误后可发布到系统。" />
      
      <div v-if="moduleData" class="preview-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模块名称">{{ moduleData.moduleName }}</el-descriptions-item>
          <el-descriptions-item label="模块编码">{{ moduleData.moduleCode }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ moduleData.description }}</el-descriptions-item>
        </el-descriptions>
        
        <el-divider />
        
        <el-tabs v-if="moduleData.tables" v-model="activeTab">
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
        <el-button type="success" @click="publish">发布模块</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { moduleApi, aiApi } from '../api'

const route = useRoute()
const router = useRouter()

const moduleData = ref(null)
const activeTab = ref('tables')

const boolFormatter = (row) => {
  return row ? '是' : '否'
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
  try {
    await aiApi.publish({
      ...moduleData.value,
      projectId: moduleData.value.projectId || Number(localStorage.getItem('bizagent_project_id') || 1)
    })
    alert('模块发布成功！')
    goBack()
  } catch (error) {
    console.error('发布失败:', error)
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
