<template>
  <div>
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="客户名称">
        <el-input v-model="queryParams.customer_name" placeholder="请输入客户名称" clearable />
      </el-form-item>
      <el-form-item label="回访时间">
        <el-date-picker
          v-model="queryParams.visit_time"
          type="datetime"
          placeholder="选择回访时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          clearable
        />
      </el-form-item>
      <el-form-item label="回访方式">
        <el-select v-model="queryParams.visit_method" placeholder="请选择回访方式" clearable>
          <el-option label="电话" value="电话" />
          <el-option label="邮件" value="邮件" />
          <el-option label="上门" value="上门" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="跟进人">
        <el-input v-model="queryParams.follower" placeholder="请输入跟进人" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待处理" value="pending" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain @click="handleStatistics">统计</el-button>
      </el-col>
    </el-row>

    <el-table :data="visitList" v-loading="loading" border stripe>
      <el-table-column label="客户名称" prop="customer_name" min-width="120" />
      <el-table-column label="回访时间" prop="visit_time" min-width="160" />
      <el-table-column label="回访方式" prop="visit_method" min-width="100" />
      <el-table-column label="跟进人" prop="follower" min-width="100" />
      <el-table-column label="状态" prop="status" min-width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'pending'" type="warning">待处理</el-tag>
          <el-tag v-else-if="scope.row.status === 'completed'" type="success">已完成</el-tag>
          <el-tag v-else-if="scope.row.status === 'cancelled'" type="info">已取消</el-tag>
          <span v-else>{{ scope.row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button type="text" @click="handleDetail(scope.row)">详情</el-button>
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" style="color: red" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" append-to-body>
      <Form :id="currentId" @success="handleSuccess" />
    </el-dialog>

    <el-dialog title="回访详情" v-model="detailVisible" width="700px" append-to-body>
      <Detail :id="currentId" @edit="handleEditFromDetail" />
    </el-dialog>

    <el-dialog title="回访统计" v-model="statisticsVisible" width="800px" append-to-body>
      <Statistics />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVisit, deleteVisit } from './api'
import Form from './Form.vue'
import Detail from './Detail.vue'
import Statistics from './Statistics.vue'

const loading = ref(false)
const visitList = ref([])
const total = ref(0)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  customer_name: '',
  visit_time: '',
  visit_method: '',
  follower: '',
  status: ''
})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentId = ref(null)
const detailVisible = ref(false)
const statisticsVisible = ref(false)

function getList() {
  loading.value = true
  listVisit(queryParams.value).then(res => {
    visitList.value = res.data.rows || []
    total.value = res.data.total || 0
  }).catch(() => {
    ElMessage.error('查询失败')
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    customer_name: '',
    visit_time: '',
    visit_method: '',
    follower: '',
    status: ''
  }
  getList()
}

function handleAdd() {
  currentId.value = null
  dialogTitle.value = '新增回访'
  dialogVisible.value = true
}

function handleEdit(row) {
  currentId.value = row.id
  dialogTitle.value = '编辑回访'
  dialogVisible.value = true
}

function handleDetail(row) {
  currentId.value = row.id
  detailVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该回访记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteVisit(row.id).then(() => {
      ElMessage.success('删除成功')
      getList()
    }).catch(() => {
      ElMessage.error('删除失败')
    })
  }).catch(() => {})
}

function handleStatistics() {
  statisticsVisible.value = true
}

function handleSuccess() {
  dialogVisible.value = false
  getList()
}

function handleEditFromDetail(id) {
  detailVisible.value = false
  currentId.value = id
  dialogTitle.value = '编辑回访'
  dialogVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>