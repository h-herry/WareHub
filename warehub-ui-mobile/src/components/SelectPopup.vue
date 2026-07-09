<template>
  <van-popup
    :show="show"
    position="bottom"
    round
    :style="{ height: '70%' }"
    @update:show="(v) => emit('update:show', v)"
  >
    <div class="select-pop">
      <div class="select-title">{{ title }}</div>
      <van-search
        v-if="searchable"
        v-model="keyword"
        placeholder="搜索"
        @update:model-value="onSearch"
      />
      <div class="select-list">
        <van-empty v-if="!filtered.length" description="无数据" :image-size="60" />
        <div
          v-for="opt in filtered"
          :key="opt.value"
          class="select-item"
          @click="onSelect(opt)"
        >
          <div class="select-item-text">{{ opt.text }}</div>
          <div v-if="opt.sub" class="select-item-sub wh-text-3">{{ opt.sub }}</div>
          <van-icon
            v-if="modelValue === opt.value"
            name="success"
            color="#4f5bd5"
            class="select-item-check"
          />
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '请选择' },
  options: { type: Array, default: () => [] },
  modelValue: { type: [String, Number], default: '' },
  searchable: { type: Boolean, default: false }
})
const emit = defineEmits(['update:show', 'update:modelValue', 'select'])

const keyword = ref('')

const filtered = computed(() => {
  if (!props.searchable || !keyword.value) return props.options
  const k = keyword.value.toLowerCase()
  return props.options.filter(
    (o) =>
      String(o.text).toLowerCase().includes(k) ||
      (o.sub && String(o.sub).toLowerCase().includes(k))
  )
})

watch(
  () => props.show,
  (v) => {
    if (v) keyword.value = ''
  }
)

function onSearch() {}
function onSelect(opt) {
  emit('update:modelValue', opt.value)
  emit('select', opt)
  emit('update:show', false)
}
</script>

<style scoped>
.select-pop {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.select-title {
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  padding: 14px;
  border-bottom: 1px solid #f2f3f5;
}
.select-list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
.select-item {
  position: relative;
  padding: 12px 16px;
  border-bottom: 1px solid #f7f8fa;
}
.select-item-text {
  font-size: 14px;
  color: var(--wh-text-1);
}
.select-item-sub {
  font-size: 12px;
  margin-top: 2px;
}
.select-item-check {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}
</style>
