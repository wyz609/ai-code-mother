<template>
  <div class="admin-page">
    <h1>用户管理</h1>
    <a-table :columns="columns" :data-source="users" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
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
const users = ref<any[]>([])

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount' },
  { title: '昵称', dataIndex: 'userName', key: 'userName' },
  { title: '角色', dataIndex: 'userRole', key: 'userRole' },
  { title: '操作', key: 'action' },
]

const handleDelete = async (id: number) => {
  message.info('删除功能待实现')
}

onMounted(() => {
  // 加载用户列表
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
