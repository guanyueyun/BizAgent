<template>
  <div class="vehicle-list">
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="车牌号">
          <el-input v-model="queryParams.plateNumber" placeholder="请输入车牌号" clearable />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="queryParams.brand" placeholder="请输入品牌" clearable />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="queryParams.model" placeholder="请输入型号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="空闲" value="idle" />
            <el-option label="使用中" value="in_use" />
            <el-option label="维修中" value="maintenance" />
            <el-option label="已报废" value="retired" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="queryParams.approvalStatus" placeholder="请选择审批状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前驾驶员">
          <el-input v-model="queryParams.driver" placeholder="请输入当前驾驶员" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="action-bar">
      <el-button type="primary" size="small" @click="handleAdd" v-permission="'vehicle:add'">新增</el-button>
      <el-button type="success" size="small" @click="handleStatistics" v-permission="'vehicle:statistics'">统计</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="plateNumber" label="车牌号" min-width="120" />
      <el-table-column prop="brand" label="品牌" min-width="100" />
      <el-table-column prop="model" label="型号" min-width="100" />
      <el-table-column prop="color" label="颜色" min-width="80" />
      <el-table-column prop="purchaseDate" label="购买日期" min-width="100" />
      <el-table-column prop="mileage" label="当前里程(公里)" min-width="120" />
      <el-table-column prop="status" label="状态" min-width="80">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approvalStatus" label="审批状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="approvalTagType(row.approvalStatus)">{{ approvalLabel(row.approvalStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="driver" label="当前驾驶员" min-width="100" />
      <el-table-column label="操作" min-width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="text" @click="handleDetail(row)" v-permission="'vehicle:detail'">详情</el-button>
          <el-button size="small" type="text" @click="handleEdit(row)" v-permission="'vehicle:edit'">编辑</el-button>
          <el-button size="small" type="text" @click="handleSubmit(row)" v-if="row.approvalStatus === 'draft'" v-permission="'vehicle:submit'">提交审批</el-button>
          <el-button size="small" type="text" @click="handleApprove(row)" v-if="row.approvalStatus === 'pending'" v-permission="'vehicle:approve'">审批</el-button>
          <el-popconfirm title="确定删除该车辆吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button size="small" type="text" style="color: red" v-permission="'vehicle:delete'">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="handleQuery"
      @current-change="handleQuery"
    />

    <el-dialog v-model="formVisible" :title="formTitle" width="600px" destroy-on-close>
      <Form :id="formId" @success="handleFormSuccess" />
    </el-dialog>

    <el-dialog v-model="detailVisible" title="车辆详情" width="600px" destroy-on-close>
      <Detail :id="detailId" />
    </el-dialog>

    <el-dialog v-model="statisticsVisible" title="车辆统计" width="500px" destroy-on-close>
      <Statistics />
    </el-dialog>

    <el-dialog v-model="approveVisible" title="车辆审批" width="500px" destroy-on-close>
      <Approval :id="approveId" @success="handleApproveSuccess" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listVehicles, deleteVehicle, submitVehicleApproval } from './api'
import Form from './Form.vue'
import Detail from './Detail.vue'
import Statistics from './Statistics.vue'
import Approval from './Approval.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  plateNumber: undefined,
  brand: undefined,
  model: undefined,
  status: undefined,
  approvalStatus: undefined,
  driver: undefined
})

const formVisible = ref(false)
const formTitle = ref('')
const formId = ref(null)

const detailVisible = ref(false)
const detailId = ref(null)

const statisticsVisible = ref(false)

const approveVisible = ref(false)
const approveId = ref(null)

const statusTagType = (status) => {
  const map = { idle: 'success', in_use: 'primary', maintenance: 'warning', retired: 'info' }
  return map[status] || ''
}

const statusLabel = (status) => {
  const map = { idle: '空闲', in_use: '使用中', maintenance: '维修中', retired: '已报废' }
  return map[status] || status
}

const approvalTagType = (status) => {
  const map = { draft: 'info', pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || ''
}

const approvalLabel = (status) => {
  const map = { draft: '草稿', pending: '待审批', approved: '已通过', rejected: '已驳回' }
  return map[status] || status
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await listVehicles(queryParams.value)
    tableData.value = res.data.rows || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    plateNumber: undefined,
    brand: undefined,
    model: undefined,
    status: undefined,
    approvalStatus: undefined,
    driver: undefined
  }
  handleQuery()
}

const handleAdd = () => {
  formId.value = null
  formTitle.value = '新增车辆'
  formVisible.value = true
}

const handleEdit = (row) => {
  formId.value = row.id
  formTitle.value = '编辑车辆'
  formVisible.value = true
}

const handleDetail = (row) => {
  detailId.value = row.id
  detailVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await deleteVehicle(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async (row) => {
  try {
    await submitVehicleApproval(row.id)
    ElMessage.success('提交审批成功')
    handleQuery()
  } catch (e) {
    ElMessage.error('提交审批失败')
  }
}

const handleApprove = (row) => {
  approveId.value = row.id
  approveVisible.value = true
}

const handleStatistics = () => {
  statisticsVisible.value = true
}

const handleFormSuccess = () => {
  formVisible.value = false
  handleQuery()
}

const handleApproveSuccess = () => {
  approveVisible.value = false
  handleQuery()
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.vehicle-list {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
}
.action-bar {
  margin-bottom: 10px;
}
</style>