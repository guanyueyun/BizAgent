
<template>
  <div class="menu-list">
    <el-card title="菜单管理" class="card">
      <el-row :gutter="10" class="mb-4">
        <el-col :span="8">
          <el-input v-model="searchForm.menuName" placeholder="菜单名称" />
        </el-col>
        <el-col :span="8">
          <el-input v-model="searchForm.path" placeholder="路径" />
        </el-col>
        <el-col :span="8">
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-col>
      </el-row>
      
      <el-table :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="menuName" label="菜单名称" />
        <el-table-column prop="path" label="路径" />
        <el-table-column prop="component" label="组件" />
        <el-table-column prop="parentId" label="父菜单ID" />
        <el-table-column prop="menuType" label="类型" :formatter="typeFormatter" />
        <el-table-column prop="visible" label="可见" :formatter="visibleFormatter" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="edit(scope.row)">编辑</el-button>
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
      
      <el-button type="primary" @click="add" style="margin-top: 20px;">新增菜单</el-button>
    </el-card>
    
    <el-dialog :visible="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" />
        </el-form-item>
        <el-form-item label="父菜单">
          <el-input v-model="form.parentId" type="number" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="form.sortOrder" type="number" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.menuType">
            <el-option label="目录" :value="1" />
            <el-option label="菜单" :value="2" />
            <el-option label="按钮" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" />
        </el-form-item>
        <el-form-item label="可见">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
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
import { menuApi } from '../../api'

const searchForm = reactive({
  menuName: '',
  path: ''
})

const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const formRef = ref(null)
const form = reactive({
  id: null,
  menuName: '',
  path: '',
  component: '',
  parentId: 0,
  icon: '',
  sortOrder: 0,
  menuType: 1,
  permission: '',
  visible: 1
})

const loadData = async () => {
  try {
    const response = await menuApi.list(pagination)
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
  searchForm.menuName = ''
  searchForm.path = ''
  loadData()
}

const typeFormatter = (row) => {
  const types = { 1: '目录', 2: '菜单', 3: '按钮' }
  return types[row.menuType] || row.menuType
}

const visibleFormatter = (row) => {
  return row.visible === 1 ? '是' : '否'
}

const add = () => {
  dialogTitle.value = '新增菜单'
  form.id = null
  form.menuName = ''
  form.path = ''
  form.component = ''
  form.parentId = 0
  form.icon = ''
  form.sortOrder = 0
  form.menuType = 1
  form.permission = ''
  form.visible = 1
  dialogVisible.value = true
}

const edit = (row) => {
  dialogTitle.value = '编辑菜单'
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  try {
    if (form.id) {
      await menuApi.update(form.id, form)
    } else {
      await menuApi.create(form)
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const deleteItem = async (id) => {
  if (confirm('确定删除该菜单？')) {
    try {
      await menuApi.delete(id)
      loadData()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }
}

loadData()
</script>

<style>
.menu-list {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
