<template>
  <div class="inspection-list">
    <div class="search-area">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="设备名称">
          <el-input v-model="queryParams.device_name" placeholder="请输入设备名称" clearable />
        </el-form-item>
        <el-form-item label="设备位置">
          <el-input v-model="queryParams.device_location" placeholder="请输入设备位置" clearable />
        </el-form-item>
        <el-form-item label="巡检计划">
          <el-input v-model="queryParams.inspection_plan" placeholder="请输入巡检计划" clearable />
        </el-form-item>
        <el-form-item label="巡检人">
          <el-input v-model="queryParams.inspector" placeholder="请输入巡检人" clearable />
        </el-form-item>
        <el-form-item label="巡检时间">
          <el-date-picker v-model="queryParams.inspection_time" type="datetime" placeholder="选择巡检时间" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="待巡检" value="pending" />
            <el-option label="已完成" value="completed" />
            <el-option label="异常" value="exception" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleAdd" v-permission="'equipment_inspection:add'">新增</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="inspectionList" v-loading="loading" border stripe>
      <el-table-column prop="device_name" label="设备名称" min-width="120" />
      <el-table-column prop="device_location" label="设备位置" min-width="150" />
      <el-table-column prop="inspection_plan" label="巡检计划" min-width="120" />
      <el-table-column prop="inspector" label="巡检人" min-width="100" />
      <el-table-column prop="inspection_time" label="巡检时间" min-width="160">
        <template #default="scope">
          {{ scope.row.inspection_time ? scope.row.inspection_time : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'pending' ? 'warning' : scope.row.status === 'completed' ? 'success' : 'danger'">
            {{ scope.row.status === 'pending' ? '待巡检' : scope.row.status === 'completed' ? '已完成' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right">
        <template #default="scope">
          <el-button type="text" size="small" @click="handleDetail(scope.row)" v-permission="'equipment_inspection:detail'">详情</el-button>
          <el-button type="text" size="small" @click="handleEdit(scope.row)" v-permission="'equipment_inspection:edit'">编辑</el-button>
          <el-button type="text" size="small" @click="handleDelete(scope.row)" v-permission="'equipment_inspection:delete'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      :current-page="queryParams.pageNum"
      :page-size="queryParams.pageSize"
      :total="total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      background
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <Form :inspection="currentInspection" @success="handleFormSuccess" />
    </el-dialog>

    <el-dialog v-model="detailVisible" title="巡检详情" width="600px" @close="handleDetailClose">
      <Detail :inspection="currentInspection" @edit="handleEditFromDetail" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInspections, deleteInspection } from './api'
import Form from './Form.vue'
import Detail from './Detail.vue'

const loading = ref(false)
const inspectionList = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  device_name: '',
  device_location: '',
  inspection_plan: '',
  inspector: '',
  inspection_time: '',
  status: ''
})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentInspection = ref(null)
const detailVisible = ref(false)

function handleQuery() {
  queryParams.pageNum = 1
  loadList()
}

function handleReset() {
  queryParams.device_name = ''
  queryParams.device_location = ''
  queryParams.inspection_plan = ''
  queryParams.inspector = ''
  queryParams.inspection_time = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  loadList()
}

function handleAdd() {
  currentInspection.value = null
  dialogTitle.value = '新增巡检'
  dialogVisible.value = true
}

function handleEdit(row) {
  currentInspection.value = { ...row }
  dialogTitle.value = '编辑巡检'
  dialogVisible.value = true
}

function handleDetail(row) {
  currentInspection.value = { ...row }
  detailVisible.value = true
}

function handleEditFromDetail() {
  detailVisible.value = false
  handleEdit(currentInspection.value)
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该巡检记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteInspection(row.id)
      ElMessage.success('删除成功')
      loadList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

function handlePageChange(page) {
  queryParams.pageNum = page
  loadList()
}

function handleSizeChange(size) {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadList()
}

function handleDialogClose() {
  dialogVisible.value = false
}

function handleDetailClose() {
  detailVisible.value = false
}

function handleFormSuccess() {
  dialogVisible.value = false
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const response = await listInspections(queryParams)
    inspectionList.value = response.data.rows || []
    total.value = response.data.total || 0
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.inspection-list {
  padding: 20px;
}
.search-area {
  margin-bottom: 20px;
}
</style>