<template>
  <div>
    <div style="margin-bottom: 16px;">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="客户名称">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户名称" clearable />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="queryParams.contactPerson" placeholder="请输入联系人" clearable />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="queryParams.contactPhone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-input v-model="queryParams.industry" placeholder="请输入所属行业" clearable />
        </el-form-item>
        <el-form-item label="客户来源">
          <el-input v-model="queryParams.source" placeholder="请输入客户来源" clearable />
        </el-form-item>
        <el-form-item label="跟进人">
          <el-input v-model="queryParams.followUpPerson" placeholder="请输入跟进人" clearable />
        </el-form-item>
        <el-form-item label="跟进状态">
          <el-select v-model="queryParams.status" placeholder="请选择跟进状态" clearable style="width: 180px;">
            <el-option label="潜在" value="potential" />
            <el-option label="已联系" value="contacted" />
            <el-option label="洽谈中" value="negotiating" />
            <el-option label="已成交" value="converted" />
            <el-option label="已流失" value="lost" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-bottom: 16px;">
      <el-button type="primary" @click="handleAdd">新增</el-button>
      <el-button type="info" @click="handleStatistics">统计</el-button>
    </div>
    <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="customerName" label="客户名称" min-width="120" />
      <el-table-column prop="contactPerson" label="联系人" min-width="100" />
      <el-table-column prop="contactPhone" label="联系电话" min-width="120" />
      <el-table-column prop="industry" label="所属行业" min-width="100" />
      <el-table-column prop="source" label="客户来源" min-width="100" />
      <el-table-column prop="followUpPerson" label="跟进人" min-width="100" />
      <el-table-column prop="status" label="跟进状态" min-width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'potential'" type="info">潜在</el-tag>
          <el-tag v-else-if="scope.row.status === 'contacted'" type="warning">已联系</el-tag>
          <el-tag v-else-if="scope.row.status === 'negotiating'" type="primary">洽谈中</el-tag>
          <el-tag v-else-if="scope.row.status === 'converted'" type="success">已成交</el-tag>
          <el-tag v-else-if="scope.row.status === 'lost'" type="danger">已流失</el-tag>
          <span v-else>{{ scope.row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button type="text" size="small" @click="handleDetail(scope.row)">详情</el-button>
          <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" style="color: #F56C6C;" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 16px; text-align: right;">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listCustomer, deleteCustomer } from './api'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  customerName: '',
  contactPerson: '',
  contactPhone: '',
  industry: '',
  source: '',
  followUpPerson: '',
  status: ''
})

function handleQuery() {
  loading.value = true
  listCustomer(queryParams).then(res => {
    tableData.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
  }).catch(() => {
    ElMessage.error('查询客户列表失败')
  }).finally(() => {
    loading.value = false
  })
}

function handleReset() {
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.customerName = ''
  queryParams.contactPerson = ''
  queryParams.contactPhone = ''
  queryParams.industry = ''
  queryParams.source = ''
  queryParams.followUpPerson = ''
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  router.push('/module-runtime/customer/add')
}

function handleEdit(row) {
  router.push(`/module-runtime/customer/edit/${row.id}`)
}

function handleDetail(row) {
  router.push(`/module-runtime/customer/detail/${row.id}`)
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该客户吗？此操作不可恢复。', '确认删除', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteCustomer(row.id).then(() => {
      ElMessage.success('删除成功')
      handleQuery()
    }).catch(() => {
      ElMessage.error('删除失败')
    })
  }).catch(() => {})
}

function handleStatistics() {
  router.push('/module-runtime/customer/statistics')
}

function handleSelectionChange(val) {
  // 留空
}

onMounted(() => {
  handleQuery()
})
</script>