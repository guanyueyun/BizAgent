<template>
  <div class="module-runtime">
    <el-card class="card" shadow="never">
      <template #header>
        <div class="runtime-header">
          <div>
            <h2>{{ moduleInfo.moduleName || route.params.moduleCode }}</h2>
            <p>{{ moduleInfo.description || '平台内业务模块' }}</p>
          </div>
          <div class="runtime-actions">
            <el-tag :type="loaded ? 'success' : 'warning'">{{ loaded ? '已加载' : '未加载' }}</el-tag>
            <input ref="importInputRef" class="file-input" type="file" accept=".csv,text/csv" @change="importData" />
            <el-button v-if="hasStatistics" @click="loadStatistics">统计</el-button>
            <el-button v-if="canImport" @click="openImport">导入</el-button>
            <el-button v-if="canExport" @click="exportData">导出</el-button>
            <el-button v-if="canAdd" type="primary" @click="openCreate">新增</el-button>
            <el-button @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" border empty-text="暂无数据">
        <el-table-column
          v-for="field in visibleFields"
          :key="field.fieldName"
          :prop="field.fieldName"
          :label="field.label || columnLabels[field.fieldName] || field.fieldName"
          min-width="140"
        >
          <template #default="scope">
            <el-tag v-if="field.type === 'select'" size="small" :type="statusTagType(scope.row[field.fieldName])">
              {{ optionLabel(field, scope.row[field.fieldName]) }}
            </el-tag>
            <span v-else>{{ scope.row[field.fieldName] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button v-if="canEdit" size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button v-if="canSubmit && canSubmitRow(scope.row)" size="small" @click="submit(scope.row)">提交</el-button>
            <el-button v-if="canApprove && canApproveRow(scope.row)" size="small" type="success" @click="openApprove(scope.row)">审批</el-button>
            <el-button v-if="canNotify" size="small" @click="notify(scope.row)">提醒</el-button>
            <el-button v-if="canDelete" size="small" type="danger" @click="remove(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        :current-page="pagination.pageNum"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑数据' : '新增数据'" width="620px">
      <el-form label-width="110px">
        <el-form-item
          v-for="field in editableFields"
          :key="field.fieldName"
          :label="field.label || columnLabels[field.fieldName] || field.fieldName"
          :required="field.required"
        >
          <el-input-number
            v-if="field.type === 'number'"
            v-model="form[field.fieldName]"
            class="full-control"
            :controls="false"
          />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.fieldName]"
            class="full-control"
            type="date"
            value-format="YYYY-MM-DD"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form[field.fieldName]"
            class="full-control"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
          <el-select v-else-if="field.type === 'select'" v-model="form[field.fieldName]" class="full-control">
            <el-option
              v-for="option in fieldOptions(field)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.fieldName]"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder"
          />
          <el-input v-else v-model="form[field.fieldName]" :placeholder="field.placeholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statisticsVisible" title="模块统计" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item v-for="(value, key) in statistics" :key="key" :label="statLabels[key] || key">
          {{ value }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="approvalVisible" title="审批处理" width="520px">
      <el-form label-width="88px">
        <el-form-item label="审批结果">
          <el-select v-model="approvalForm.result" class="full-control">
            <el-option label="通过" value="approved" />
            <el-option label="驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button type="primary" @click="approve">确认审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bizApi, moduleApi } from '../api'

const route = useRoute()
const moduleInfo = ref({})
const design = ref({})
const userPermissions = ref([])
const loaded = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const statisticsVisible = ref(false)
const approvalVisible = ref(false)
const editingId = ref(null)
const approvingRow = ref(null)
const importInputRef = ref(null)
const form = reactive({})
const approvalForm = reactive({
  result: 'approved',
  comment: ''
})
const statistics = ref({})
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const systemColumns = ['id', 'create_by', 'create_time', 'update_by', 'update_time', 'del_flag', 'project_id']
const columnLabels = {
  biz_no: '业务编号',
  inspection_no: '巡检单号',
  device_name: '设备名称',
  inspection_plan: '巡检计划',
  inspector: '巡检人',
  inspection_time: '巡检时间',
  exception_desc: '异常说明',
  warehouse_name: '仓库名称',
  material_code: '物料编码',
  material_name: '物料名称',
  quantity: '数量',
  employee_name: '员工姓名',
  leave_type: '请假类型',
  start_time: '开始时间',
  end_time: '结束时间',
  reason: '原因',
  name: '名称',
  owner_name: '负责人',
  status: '状态',
  remark: '备注'
}
const statLabels = {
  total: '总数',
  draft: '草稿',
  submitted: '已提交',
  approved: '已通过',
  rejected: '已驳回'
}

const moduleCode = computed(() => route.params.moduleCode)
const primaryTableColumns = computed(() => {
  const table = design.value?.tables?.[0]
  return table?.columns || []
})
const pageFields = computed(() => {
  const listPage = design.value?.pages?.find((page) => page.pageType === 'list')
  if (listPage?.fields?.length) {
    return listPage.fields
  }
  return primaryTableColumns.value.map((column) => ({
    fieldName: column.columnName,
    label: column.comment,
    type: inferFieldType(column),
    required: !column.nullable,
    placeholder: `请输入${column.comment || column.columnName}`,
    options: column.columnName === 'status' ? 'draft:草稿,submitted:已提交,approved:已通过,rejected:已驳回,normal:正常,disabled:停用' : ''
  }))
})
const visibleFields = computed(() => {
  if (pageFields.value.length) {
    return pageFields.value
      .concat([{ fieldName: 'create_time', label: '创建时间', type: 'datetime' }])
      .filter((field) => field.fieldName !== 'del_flag')
  }
  const firstRow = tableData.value[0]
  const columns = firstRow ? Object.keys(firstRow).filter((key) => key !== 'del_flag') : ['id', 'name', 'status']
  return columns.map((column) => ({ fieldName: column, label: columnLabels[column] || column, type: column === 'status' ? 'select' : 'input' }))
})
const editableFields = computed(() => visibleFields.value.filter((field) => !systemColumns.includes(field.fieldName)))
const permissionCodes = computed(() => userPermissions.value)
const canAdd = computed(() => permissionCodes.value.includes(`${moduleCode.value}:add`))
const canEdit = computed(() => permissionCodes.value.includes(`${moduleCode.value}:edit`))
const canDelete = computed(() => permissionCodes.value.includes(`${moduleCode.value}:delete`))
const canImport = computed(() => permissionCodes.value.includes(`${moduleCode.value}:import`))
const hasStatistics = computed(() => permissionCodes.value.includes(`${moduleCode.value}:statistics`))
const canExport = computed(() => permissionCodes.value.includes(`${moduleCode.value}:export`))
const canApprove = computed(() => permissionCodes.value.includes(`${moduleCode.value}:approve`))
const canNotify = computed(() => permissionCodes.value.includes(`${moduleCode.value}:notify`))
const canSubmit = computed(() => canEdit.value && primaryTableColumns.value.some((column) => column.columnName === 'status'))

const loadRuntime = async () => {
  const response = await moduleApi.runtime(moduleCode.value)
  moduleInfo.value = response.data.data.module
  design.value = response.data.data.design || {}
  userPermissions.value = response.data.data.userPermissions || []
  loaded.value = response.data.data.loaded
}

const loadData = async () => {
  const response = await bizApi.list(moduleCode.value, pagination)
  tableData.value = response.data.data.records
  pagination.total = response.data.data.total
}

const resetForm = () => {
  Object.keys(form).forEach((key) => delete form[key])
  editableFields.value.forEach((field) => {
    form[field.fieldName] = field.type === 'number' ? 0 : ''
  })
}

const openCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  resetForm()
  editableFields.value.forEach((field) => {
    form[field.fieldName] = row[field.fieldName]
  })
  dialogVisible.value = true
}

const inferFieldType = (column) => {
  const name = column.columnName || ''
  const dataType = (column.dataType || '').toUpperCase()
  if (name === 'status' || name.endsWith('_status')) return 'select'
  if (dataType.includes('DATETIME')) return 'datetime'
  if (dataType === 'DATE') return 'date'
  if (dataType.includes('INT') || dataType.includes('DECIMAL')) return 'number'
  if (dataType.includes('500') || name.includes('remark') || name.includes('desc') || name.includes('reason')) return 'textarea'
  return 'input'
}

const fieldOptions = (field) => {
  return String(field.options || '')
    .split(',')
    .filter(Boolean)
    .map((item) => {
      const [value, label] = item.split(':')
      return { value, label: label || value }
    })
}

const optionLabel = (field, value) => {
  return fieldOptions(field).find((option) => option.value === value)?.label || value || '-'
}

const statusTagType = (value) => {
  const typeMap = {
    approved: 'success',
    normal: 'success',
    submitted: 'warning',
    rejected: 'danger',
    disabled: 'info'
  }
  return typeMap[value] || 'info'
}

const save = async () => {
  const missingField = editableFields.value.find((field) => field.required && !form[field.fieldName])
  if (missingField) {
    ElMessage.warning(`请填写${missingField.label || missingField.fieldName}`)
    return
  }
  if (editingId.value) {
    await bizApi.update(moduleCode.value, editingId.value, form)
    ElMessage.success('更新成功')
  } else {
    await bizApi.create(moduleCode.value, form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  await loadData()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该数据？', '删除确认', { type: 'warning' })
    await bizApi.delete(moduleCode.value, row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const submit = async (row) => {
  await bizApi.submit(moduleCode.value, row.id)
  ElMessage.success('提交成功')
  await loadData()
}

const canSubmitRow = (row) => !row.status || row.status === 'draft' || row.status === 'rejected'
const canApproveRow = (row) => !row.status || row.status === 'submitted'

const openApprove = (row) => {
  approvingRow.value = row
  approvalForm.result = 'approved'
  approvalForm.comment = ''
  approvalVisible.value = true
}

const approve = async () => {
  if (!approvingRow.value) return
  await bizApi.approve(moduleCode.value, approvingRow.value.id, approvalForm)
  ElMessage.success('审批完成')
  approvalVisible.value = false
  await loadData()
}

const notify = async (row) => {
  await bizApi.notify(moduleCode.value, row.id, {
    title: `${moduleInfo.value.moduleName || moduleCode.value}提醒`,
    content: `请及时处理 ${moduleInfo.value.moduleName || moduleCode.value} 数据 #${row.id}`
  })
  ElMessage.success('提醒已发送')
}

const loadStatistics = async () => {
  const response = await bizApi.statistics(moduleCode.value)
  statistics.value = response.data.data
  statisticsVisible.value = true
}

const exportData = async () => {
  const response = await bizApi.export(moduleCode.value)
  const blob = new Blob([`\uFEFF${response.data.data || ''}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${moduleCode.value}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

const openImport = () => {
  importInputRef.value?.click()
}

const importData = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const text = await file.text()
  const rows = parseCsv(text)
  if (!rows.length) {
    ElMessage.warning('导入文件没有可用数据')
    return
  }
  const response = await bizApi.import(moduleCode.value, rows)
  ElMessage.success(`导入完成：${response.data.data.successCount}/${response.data.data.totalCount}`)
  await loadData()
}

const parseCsv = (text) => {
  const rows = []
  let row = []
  let cell = ''
  let quoted = false
  const normalized = text.replace(/^\uFEFF/, '')
  for (let i = 0; i < normalized.length; i++) {
    const char = normalized[i]
    const next = normalized[i + 1]
    if (char === '"' && quoted && next === '"') {
      cell += '"'
      i += 1
    } else if (char === '"') {
      quoted = !quoted
    } else if (char === ',' && !quoted) {
      row.push(cell)
      cell = ''
    } else if ((char === '\n' || char === '\r') && !quoted) {
      if (char === '\r' && next === '\n') i += 1
      row.push(cell)
      if (row.some((item) => item.trim())) rows.push(row)
      row = []
      cell = ''
    } else {
      cell += char
    }
  }
  row.push(cell)
  if (row.some((item) => item.trim())) rows.push(row)
  const [headers = [], ...records] = rows
  return records.map((record) => Object.fromEntries(headers.map((header, index) => [header.trim(), record[index] ?? ''])))
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadData()
}

watch(
  () => moduleCode.value,
  async () => {
    await loadRuntime()
    await loadData()
  },
  { immediate: true }
)
</script>

<style scoped>
.module-runtime {
  padding: 20px;
}

.card {
  border-radius: 8px;
}

.runtime-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.runtime-header h2 {
  margin: 0;
  color: #111827;
  font-size: 22px;
}

.runtime-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.runtime-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pagination {
  margin-top: 18px;
  justify-content: flex-end;
}

.file-input {
  display: none;
}

.full-control {
  width: 100%;
}
</style>
