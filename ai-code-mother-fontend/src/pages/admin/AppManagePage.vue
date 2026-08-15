<template>
  <div class="admin-page">
    <h1>应用管理</h1>
    <a-table :columns="columns" :data-source="apps" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleEdit(record.id)">编辑</a-button>
          <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'

const loading = ref(false)
const apps = ref<any[]>([])

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
]

const handleEdit = (id: number) => {
  message.info('编辑功能待实现')
}

const handleDelete = async (id: number) => {
  message.info('删除功能待实现')
}

onMounted(() => {
  // 加载应用列表
})
</script>

<style scoped>
.admin-page {
  padding: 24px;
  background: #0a0a0c;
  min-height: calc(100vh - 64px);
}

.admin-page h1 {
  color: #f4f4f5;
  margin-bottom: 24px;
}
</style>
