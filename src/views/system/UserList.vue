
<template>
  <div class="user-list">
    <el-card title="用户管理" class="card">
      <el-row :gutter="10" class="mb-4">
        <el-col :span="8">
          <el-input v-model="searchForm.username" placeholder="用户名" />
        </el-col>
        <el-col :span="8">
          <el-input v-model="searchForm.realName" placeholder="真实姓名" />
        </el-col>
        <el-col :span="8">
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-col>
      </el-row>
      
      <el-table :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="status" label="状态" :formatter="statusFormatter" />
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
      
      <el-button type="primary" @click="add" style="margin-top: 20px;">新增用户</el-button>
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input type="password" v-model="form.password" />
        </el-form-item>
        <el-form-item label="真实姓名" required>
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { userApi } from '../../api'

const searchForm = reactive({
  username: '',
  realName: ''
})

const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  status: 1
})

const loadData = async () => {
  try {
    const response = await userApi.list({
      ...pagination,
      username: searchForm.username || undefined,
      realName: searchForm.realName || undefined
    })
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
  searchForm.username = ''
  searchForm.realName = ''
  loadData()
}

const statusFormatter = (row) => {
  return row.status === 1 ? '启用' : '禁用'
}

const add = () => {
  dialogTitle.value = '新增用户'
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.email = ''
  form.phone = ''
  form.status = 1
  dialogVisible.value = true
}

const edit = (row) => {
  dialogTitle.value = '编辑用户'
  form.id = row.id
  form.username = row.username
  form.password = ''
  form.realName = row.realName
  form.email = row.email
  form.phone = row.phone
  form.status = row.status
  dialogVisible.value = true
}

const save = async () => {
  try {
    if (form.id) {
      await userApi.update(form.id, form)
    } else {
      await userApi.create(form)
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const deleteItem = async (id) => {
  if (confirm('确定删除该用户？')) {
    try {
      await userApi.delete(id)
      loadData()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }
}

loadData()
</script>

<style>
.user-list {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
