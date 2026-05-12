<template>
  <div>
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="设备名称">
        <el-input v-model="queryParams.device_name" placeholder="请输入设备名称" clearable />
      </el-form-item>
      <el-form-item label="设备位置">
        <el-input v-model="queryParams.device_location" placeholder="请输入设备位置" clearable />
      </el-form-item>
      <el-form-item label="巡检人">
        <el-input v-model="queryParams.inspector" placeholder="请输入巡检人" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待巡检" value="pending" />
          <el-option label="已完成" value="completed" />
          <el-option label="异常" value="abnormal" />
        </el-select>
      </el-form-item>
      <el-form-item label="巡检时间">
        <el-date-picker
          v-model="queryParams.inspection_time"
          type="datetime"
          placeholder="选择巡检时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="整改状态">
        <el-select v-model="queryParams.rectification_status" placeholder="请选择整改状态" clearable>
          <el-option label="无需整改" value="none" />
          <el-option label="待整改" value="pending" />
          <el-option label="已整改" value="rectified" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd" v-permission="'inspection:add'">新增</el-button>
      <el-button @click="handleStatistics" v-permission="'inspection:statistics'">统计</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column label="设备名称" prop="device_name" min-width="120" />
      <el-table-column label="设备位置" prop="device_location" min-width="120" />
      <el-table-column label="巡检计划名称" prop="plan_name" min-width="150" />
      <el-table-column label="巡检人" prop="inspector" min-width="100" />
      <el-table-column label="巡检时间" prop="inspection_time" min-width="150" />
      <el-table-column label="状态" prop="status" min-width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'pending'" type="warning">待巡检</el-tag>
          <el-tag v-else-if="row.status === 'completed'" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 'abnormal'" type="danger">异常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="整改状态" prop="rectification_status" min-width="120">
        <template #default="{ row }">
          <el-tag v-if="row.rectification_status === 'none'" type="info">无需整改</el-tag>
          <el-tag v-else-if="row.rectification_status === 'pending'" type="warning">待整改</el-tag>
          <el-tag v-else-if="row.rectification_status === 'rectified'" type="success">已整改</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="text" @click="handleDetail(row.id)">详情</el-button>
          <el-button type="text" @click="handleEdit(row.id)" v-permission="'inspection:edit'">编辑</el-button>
          <el-button type="text" style="color: red" @click="handleDelete(row.id)" v-permission="'inspection:delete'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handlePageChange"
      @current-change="handlePageChange"
    />

    <el-dialog v-model="formVisible" :title="formTitle" width="600px" @close="handleFormClose">
      <Form :id="formId" @success="handleFormSuccess" />
    </el-dialog>

    <el-dialog v-model="detailVisible" title="巡检详情" width="700px">
      <Detail :id="detailId" @edit="handleEditFromDetail" />
    </el-dialog>

    <el-dialog v-model="statisticsVisible" title="巡检统计" width="800px">
      <Statistics />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInspection, deleteInspection } from './api'
import Form from './Form.vue'
import Detail from './Detail.vue'
import Statistics from './Statistics.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = ref({
  device_name: '',
  device_location: '',
  inspector: '',
  status: '',
  inspection_time: '',
  rectification_status: '',
  pageNum: 1,
  pageSize: 10
})

const formVisible = ref(false)
const formTitle = ref('')
const formId = ref(null)

const detailVisible = ref(false)
const detailId = ref(null)

const statisticsVisible = ref(false)

const getList = async () => {
  loading.value = true
  try {
    const res = await listInspection(queryParams.value)
    tableData.value = res.data.records || res.data.rows || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNum = 1
  getList()
}

const handleReset = () => {
  queryParams.value = {
    device_name: '',
    device_location: '',
    inspector: '',
    status: '',
    inspection_time: '',
    rectification_status: '',
    pageNum: 1,
    pageSize: 10
  }
  getList()
}

const handlePageChange = () => {
  getList()
}

const handleAdd = () => {
  formId.value = null
  formTitle.value = '新增巡检'
  formVisible.value = true
}

const handleEdit = (id) => {
  formId.value = id
  formTitle.value = '编辑巡检'
  formVisible.value = true
}

const handleDetail = (id) => {
  detailId.value = id
  detailVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确认删除该巡检记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteInspection(id)
      ElMessage.success('删除成功')
      getList()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleFormSuccess = () => {
  formVisible.value = false
  getList()
}

const handleFormClose = () => {
  formId.value = null
}

const handleEditFromDetail = (id) => {
  detailVisible.value = false
  handleEdit(id)
}

const handleStatistics = () => {
  statisticsVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.search-form {
  padding: 20px 0 0 0;
}
.action-bar {
  margin-bottom: 10px;
}
</style>