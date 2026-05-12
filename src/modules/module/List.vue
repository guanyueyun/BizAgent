<template>
  <div class="generated-module">
    <div class="module-toolbar">
      <h2>业务模块</h2>
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>
    <el-table :data="tableData" border @row-click="handleView">
      <el-table-column prop="biz_no" label="业务编号" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="owner_name" label="负责人" />
      <el-table-column prop="contact_phone" label="联系电话" />
      <el-table-column prop="biz_time" label="业务时间" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
      :current-page="pageNum" :page-sizes="[10, 20, 50]" :page-size="pageSize"
      :total="total" layout="total, sizes, prev, pager, next, jumper" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as api from './api'

defineOptions({ name: 'ModuleList' })

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  const res = await api.list({ pageNum: pageNum.value, pageSize: pageSize.value })
  tableData.value = res.data.data.records
  total.value = res.data.data.total
}

const handleSizeChange = (val) => { pageSize.value = val; loadData() }
const handleCurrentChange = (val) => { pageNum.value = val; loadData() }
const handleAdd = () => { }
const handleEdit = (row) => { }
const handleDelete = (row) => { }
const handleView = (row) => { }

onMounted(() => loadData())
</script>

<style scoped>
.generated-module { padding: 20px; }
.module-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
</style>
