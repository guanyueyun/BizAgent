
<template>
  <div class="module-list">
    <el-card title="模块管理" class="card">
      <el-row :gutter="10" class="mb-4">
        <el-col :span="8">
          <el-input v-model="searchForm.moduleName" placeholder="模块名称" />
        </el-col>
        <el-col :span="8">
          <el-input v-model="searchForm.moduleCode" placeholder="模块编码" />
        </el-col>
        <el-col :span="8">
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-col>
      </el-row>
      
      <el-table :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="moduleName" label="模块名称" />
        <el-table-column prop="moduleCode" label="模块编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" :formatter="statusFormatter" />
        <el-table-column prop="lifecycle" label="生命周期" :formatter="lifecycleFormatter" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="360">
          <template #default="scope">
            <el-button size="small" type="primary" @click="run(scope.row)">运行</el-button>
            <el-button size="small" @click="preview(scope.row)">预览</el-button>
            <el-button size="small" @click="edit(scope.row)">编辑</el-button>
            <el-button size="small" type="warning" @click="loadModule(scope.row.id)">加载</el-button>
            <el-button size="small" type="info" @click="unloadModule(scope.row.id)">卸载</el-button>
            <el-button size="small" type="danger" @click="deleteItem(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.pageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
      />
      
      <el-button type="primary" @click="add" style="margin-top: 20px;">新增模块</el-button>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="模块名称" required>
          <el-input v-model="form.moduleName" />
        </el-form-item>
        <el-form-item label="模块编码" required>
          <el-input v-model="form.moduleCode" />
        </el-form-item>
        <el-form-item label="描述">
          <el-textarea v-model="form.description" :rows="3" />
        </el-form-item>
        <el-form-item label="前端路径">
          <el-input v-model="form.frontPath" />
        </el-form-item>
        <el-form-item label="后端路径">
          <el-input v-model="form.backPath" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="生命周期">
          <el-select v-model="form.lifecycle">
            <el-option label="开发中" :value="0" />
            <el-option label="测试中" :value="1" />
            <el-option label="已发布" :value="2" />
            <el-option label="已停用" :value="3" />
            <el-option label="已卸载" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { moduleApi } from '../../api'

const router = useRouter()

const searchForm = reactive({
  moduleName: '',
  moduleCode: ''
})

const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增模块')
const formRef = ref(null)
const form = reactive({
  id: null,
  moduleName: '',
  moduleCode: '',
  description: '',
  frontPath: '',
  backPath: '',
  status: 1,
  lifecycle: 0
})

const loadData = async () => {
  try {
    const response = await moduleApi.list(pagination)
    tableData.value = response.data.data.records
    pagination.total = response.data.data.total
  } catch (error) {
    console.error('加载失败:', error)
  }
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadData()
}

const handleCurrentChange = (page) => {
  pagination.pageNum = page
  loadData()
}

const resetForm = () => {
  searchForm.moduleName = ''
  searchForm.moduleCode = ''
  loadData()
}

const statusFormatter = (row) => {
  return row.status === 1 ? '启用' : '禁用'
}

const lifecycleFormatter = (row) => {
  const lifecycles = { 0: '开发中', 1: '测试中', 2: '已发布', 3: '已停用', 4: '已卸载' }
  return lifecycles[row.lifecycle] || row.lifecycle
}

const add = () => {
  dialogTitle.value = '新增模块'
  form.id = null
  form.moduleName = ''
  form.moduleCode = ''
  form.description = ''
  form.frontPath = ''
  form.backPath = ''
  form.status = 1
  form.lifecycle = 0
  dialogVisible.value = true
}

const edit = (row) => {
  dialogTitle.value = '编辑模块'
  Object.assign(form, row)
  dialogVisible.value = true
}

const preview = (row) => {
  router.push(`/module/preview/${row.id}`)
}

const run = (row) => {
  router.push(`/module-runtime/${row.moduleCode}`)
}

const save = async () => {
  try {
    if (form.id) {
      await moduleApi.update(form.id, form)
    } else {
      await moduleApi.create(form)
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const deleteItem = async (id) => {
  if (confirm('确定删除该模块？')) {
    try {
      await moduleApi.delete(id)
      loadData()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }
}

const loadModule = async (id) => {
  try {
    await moduleApi.load(id)
    alert('模块加载成功！')
    loadData()
  } catch (error) {
    console.error('加载失败:', error)
  }
}

const unloadModule = async (id) => {
  try {
    await moduleApi.unload(id)
    alert('模块卸载成功！')
    loadData()
  } catch (error) {
    console.error('卸载失败:', error)
  }
}

loadData()
</script>

<style>
.module-list {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
