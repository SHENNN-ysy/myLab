<template>
  <div class="skill-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>技术栈管理</span>
          <a-button type="primary" @click="openDrawer()">
            <template #icon>
              <PlusOutlined />
            </template>
            新建技术栈
          </a-button>
        </div>
      </template>

      <!-- 列表 -->
      <a-table
        :data-source="skills"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <div class="skill-icon" v-html="getIcon(record.icon)" />
          </template>
          <template v-else-if="column.key === 'percentage'">
            <div class="skill-progress">
              <a-progress
                :percent="record.percentage"
                :stroke-width="8"
                :stroke-color="getProgressColor(record.level)"
                :show-info="false"
              />
              <span class="progress-text">{{ record.percentage }}%</span>
            </div>
          </template>
          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">
              {{ record.levelText }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'barStyle'">
            <a-tag v-if="record.barStyle" :color="record.barStyle === 'coral' ? 'error' : 'success'">
              {{ record.barStyle === 'coral' ? '珊瑚色' : '青色' }}
            </a-tag>
            <span v-else class="text-muted">默认</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDrawer(record)">
                编辑
              </a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 抽屉表单 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑技术栈' : '新建技术栈'"
      :width="500"
      @close="resetForm"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="技术名称" name="name">
          <a-input v-model:value="form.name" placeholder="如：C# / .NET" />
        </a-form-item>

        <a-form-item label="熟练度" name="percentage">
          <a-slider v-model:value="form.percentage" :min="0" :max="100" />
        </a-form-item>

        <a-form-item label="等级" name="level">
          <a-select v-model:value="form.level" placeholder="选择等级">
            <a-select-option value="proficient">熟练</a-select-option>
            <a-select-option value="competent">熟练</a-select-option>
            <a-select-option value="novice">入门</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="等级文字" name="levelText">
          <a-input v-model:value="form.levelText" placeholder="如：熟练、入门" />
        </a-form-item>

        <a-form-item label="图标" name="icon">
          <a-select v-model:value="form.icon" placeholder="选择图标">
            <a-select-option
              v-for="icon in iconOptions"
              :key="icon.value"
              :value="icon.value"
            >
              {{ icon.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="进度条风格" name="barStyle">
          <a-radio-group v-model:value="form.barStyle">
            <a-radio value="coral">珊瑚色</a-radio>
            <a-radio value="teal">青色</a-radio>
            <a-radio :value="undefined">默认</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="实时预览">
          <div class="skill-preview">
            <div
              class="skill-preview-item"
              :class="form.barStyle ? `has-${form.barStyle}-bar` : ''"
            >
              <div class="skill-icon" v-html="getIcon(form.icon)" />
              <div class="skill-name">{{ form.name || '技术名称' }}</div>
              <div class="skill-track">
                <div
                  class="skill-fill"
                  :class="form.level"
                  :style="{ width: form.percentage + '%' }"
                />
              </div>
              <div class="skill-meta">
                <span>{{ form.percentage }}%</span>
                <span>{{ form.levelText || '等级' }}</span>
              </div>
            </div>
          </div>
        </a-form-item>
      </a-form>

      <template #footer>
        <div class="drawer-footer">
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存' : '创建' }}
          </a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import type { Skill } from '@/types'
import { getSkillsApi, createSkillApi, updateSkillApi, deleteSkillApi } from '@/api/skill'
import { addLog } from '@/api/log'

const skills = ref<Skill[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref()

const form = reactive({
  name: '',
  percentage: 50,
  level: 'competent' as Skill['level'],
  levelText: '熟练',
  icon: 'code',
  barStyle: 'coral' as Skill['barStyle'] | undefined
})

const rules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入技术名称', trigger: 'blur' }],
  percentage: [{ required: true, message: '请设置熟练度', trigger: 'change' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }],
  levelText: [{ required: true, message: '请输入等级文字', trigger: 'blur' }],
  icon: [{ required: true, message: '请选择图标', trigger: 'change' }]
}

const iconOptions = [
  { label: '代码', value: 'code' },
  { label: '终端', value: 'terminal' },
  { label: '网格', value: 'grid' },
  { label: '服务器', value: 'server' },
  { label: '安全', value: 'shield' },
  { label: '容器', value: 'box' },
  { label: '移动端', value: 'smartphone' },
  { label: '画笔', value: 'pen' },
  { label: '原子', value: 'atom' },
  { label: '图层', value: 'layers' }
]

const columns = [
  {
    title: '图标',
    key: 'icon',
    width: 80,
    align: 'center' as const
  },
  { title: '技术名称', dataIndex: 'name', key: 'name', minWidth: 160 },
  {
    title: '熟练度',
    key: 'percentage',
    width: 200
  },
  {
    title: '等级',
    key: 'level',
    width: 100,
    align: 'center' as const
  },
  {
    title: '进度条风格',
    key: 'barStyle',
    width: 120,
    align: 'center' as const
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    align: 'center' as const,
    fixed: 'right' as const
  }
]

const getIcon = (type: string) => {
  const icons: Record<string, string> = {
    code: '<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="3"/><path d="M8 9l-3 3 3 3M16 9l3 3-3 3M13.5 7.5l-3 9"/><path d="M7 19h10"/></svg>',
    atom: '<svg viewBox="0 0 24 24"><path d="M12 3c3.8 0 7 4.2 7 9s-3.2 9-7 9-7-4.2-7-9 3.2-9 7-9z"/><path d="M3.8 8.5c3.3-2.1 8.7-1.2 12.1 1.9s3.7 7.2.7 9.1"/><path d="M20.2 8.5c-3.3-2.1-8.7-1.2-12.1 1.9s-3.7 7.2-.7 9.1"/><path d="M9 9.5l3 6 3-6"/><circle cx="12" cy="12" r="1.4"/></svg>',
    grid: '<svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>',
    terminal: '<svg viewBox="0 0 24 24"><path d="M7 8l-4 4 4 4M17 8l4 4-4 4M14 4l-4 16"/></svg>',
    layers: '<svg viewBox="0 0 24 24"><path d="M11 4H8.5A4.5 4.5 0 0 0 4 8.5V11h8V7a3 3 0 0 1 3-3h.5"/><path d="M13 20h2.5A4.5 4.5 0 0 0 20 15.5V13h-8v4a3 3 0 0 1-3 3h-.5"/><circle cx="8" cy="8" r=".8"/><circle cx="16" cy="16" r=".8"/></svg>',
    pen: '<svg viewBox="0 0 24 24"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><circle cx="6" cy="6" r="2"/></svg>',
    smartphone: '<svg viewBox="0 0 24 24"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>',
    shield: '<svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>',
    box: '<svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>',
    server: '<svg viewBox="0 0 24 24"><path d="M4 17l6-6-4-4M12 17l6-6-4-4M16 17l4-4"/></svg>'
  }
  return icons[type] || icons.code
}

const getProgressColor = (level: string) => {
  const colors: Record<string, string> = {
    proficient: '#1677ff',
    competent: '#52c41a',
    novice: '#8c8c8c'
  }
  return colors[level] || '#1677ff'
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    proficient: 'blue',
    competent: 'orange',
    novice: 'default'
  }
  return colors[level] || 'default'
}

const loadData = async () => {
  loading.value = true
  try {
    skills.value = await getSkillsApi()
  } finally {
    loading.value = false
  }
}

const openDrawer = (row?: Skill) => {
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    Object.assign(form, {
      name: row.name,
      percentage: row.percentage,
      level: row.level,
      levelText: row.levelText,
      icon: row.icon,
      barStyle: row.barStyle
    })
  } else {
    isEdit.value = false
    currentId.value = ''
  }
  drawerVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    name: '',
    percentage: 50,
    level: 'competent',
    levelText: '熟练',
    icon: 'code',
    barStyle: 'coral'
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    if (isEdit.value) {
      await updateSkillApi(currentId.value, form)
      addLog('更新', `技术栈：${form.name}`, 'success')
      message.success('更新成功')
    } else {
      await createSkillApi(form)
      addLog('新建', `技术栈：${form.name}`, 'success')
      message.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch {
    // 校验失败
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row: Skill) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除技术栈「${row.name}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteSkillApi(row.id)
      addLog('删除', `技术栈：${row.name}`, 'success')
      message.success('删除成功')
      await loadData()
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.skill-manage {
  :deep(.ant-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  span {
    font-size: 16px;
    font-weight: 500;
  }
}

.skill-icon {
  width: 24px;
  height: 24px;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(svg) {
    width: 100%;
    height: 100%;
    stroke: currentColor;
    fill: none;
    stroke-width: 1.4;
  }
}

.skill-progress {
  display: flex;
  align-items: center;
  gap: 8px;

  :deep(.ant-progress) {
    flex: 1;
  }

  .progress-text {
    font-size: 13px;
    color: #595959;
    min-width: 36px;
  }
}

.text-muted {
  color: #8c8c8c;
  font-size: 13px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.skill-preview {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.skill-preview-item {
  background: rgba(13, 27, 42, 0.6);
  border: 1px solid rgba(91, 164, 230, 0.2);
  border-radius: 12px;
  padding: 16px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .skill-icon {
    width: 28px;
    height: 28px;
    color: #fff;
  }

  .skill-name {
    font-size: 14px;
    font-weight: 500;
    color: #fff;
  }

  .skill-track {
    height: 5px;
    background: rgba(91, 164, 230, 0.15);
    border-radius: 100px;
    overflow: hidden;
  }

  .skill-fill {
    height: 100%;
    border-radius: 100px;
    background: linear-gradient(90deg, #5BA4E6, #2EC4B6);
    transition: width 0.3s;
  }

  .has-coral-bar .skill-fill {
    background: linear-gradient(90deg, #5BA4E6, #f0a090);
  }

  .has-teal-bar .skill-fill {
    background: linear-gradient(90deg, #5BA4E6, #2EC4B6);
  }

  .skill-meta {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
