<template>
  <div
    class="app-card"
    :class="{ clickable }"
    @click="handleCardClick"
  >
    <div class="card-cover">
      <img
        v-if="app.cover && !coverUnavailable"
        :src="app.cover"
        :alt="`${app.appName || '应用'}的预览图`"
        @error="coverUnavailable = true"
      />
      <div v-else class="cover-placeholder" aria-hidden="true">
        <span>{{ app.appName || '未命名应用' }}</span>
      </div>
    </div>
    <div class="card-content">
      <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
      <p class="app-desc">{{ app.initPrompt || '暂无描述' }}</p>
      <div class="card-actions">
        <a-button type="primary" size="small" @click.stop="$emit('view-chat', app.id)">
          对话
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  app: API.AppVO
  featured?: boolean
  clickable?: boolean
}>()

const coverUnavailable = ref(false)

watch(
  () => props.app.cover,
  () => {
    coverUnavailable.value = false
  },
)

const emit = defineEmits<{
  (e: 'view-chat', id: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'view-history', id: string | number | undefined): void
}>()

const handleCardClick = () => {
  if (props.clickable) {
    emit('view-chat', props.app.id)
  }
}
</script>

<style scoped>
.app-card {
  background: #0b0b0e;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, color 0.3s ease, transform 0.3s ease;
}

.app-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.18);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
  transform: translateY(-2px);
}

.app-card.clickable {
  cursor: pointer;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px 20px;
}

.card-cover {
  aspect-ratio: 16 / 9;
  background: #15151b;
  overflow: hidden;
}

.card-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  color: rgba(255, 255, 255, 0.75);
  background: linear-gradient(135deg, #262631, #121217);
}

.cover-placeholder span {
  max-width: 100%;
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-name {
  font-size: 18px;
  font-weight: 600;
  color: #f4f4f5;
  margin: 0;
  letter-spacing: 0.2px;
}

.app-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
