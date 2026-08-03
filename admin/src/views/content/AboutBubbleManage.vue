<template>
  <div class="about-bubble-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <div>
            <span>关于我的悬浮气泡</span>
            <p>管理前台“我的成分”区域里的气泡文字、层级与视觉样式</p>
          </div>
          <div class="header-actions">
            <a-button @click="handleReset">
              <template #icon>
                <ReloadOutlined />
              </template>
              恢复默认
            </a-button>
            <a-button type="primary" @click="openDrawer()">
              <template #icon>
                <PlusOutlined />
              </template>
              新建气泡
            </a-button>
          </div>
        </div>
      </template>

      <a-table
        :data-source="bubbles"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'preview'">
            <div class="table-bubble" :class="`is-${record.tier}`" :style="bubbleStyle(record)">
              <span>{{ record.label || '小气泡' }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'tier'">
            <a-tag :color="getTierColor(record.tier)">{{ getTierText(record.tier) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'colors'">
            <div class="color-list">
              <span class="color-chip" :style="{ background: record.bg }" />
              <span class="color-chip" :style="{ background: record.glow }" />
              <span class="color-chip" :style="{ background: record.textColor }" />
              <span class="color-text">{{ record.textColor }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              checked-children="启用"
              un-checked-children="停用"
              @change="(val: boolean) => handleToggle(record, val)"
            />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openDrawer(record)">编辑</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑悬浮气泡' : '新建悬浮气泡'"
      :width="540"
      @close="resetForm"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="气泡文字" name="label">
          <a-input v-model:value="form.label" :maxlength="18" placeholder="例如：技术探索者" show-count />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="层级" name="tier">
              <a-select v-model:value="form.tier" style="width: 100%">
                <a-select-option value="big">大气泡</a-select-option>
                <a-select-option value="mid">中气泡</a-select-option>
                <a-select-option value="small">小气泡</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="1" :max="999" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="是否启用">
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" />
        </a-form-item>

        <a-divider orientation="left">视觉样式</a-divider>

        <a-form-item label="背景色" name="bg">
          <div class="color-field">
            <a-input v-model:value="form.bg" placeholder="rgba(91, 164, 230, 0.25)">
              <template #prefix>
                <div class="color-preview" :style="{ background: form.bg }" />
              </template>
            </a-input>
          </div>
        </a-form-item>

        <a-form-item label="发光色" name="glow">
          <div class="color-field">
            <a-input v-model:value="form.glow" placeholder="rgba(91, 164, 230, 0.4)">
              <template #prefix>
                <div class="color-preview" :style="{ background: form.glow }" />
              </template>
            </a-input>
          </div>
        </a-form-item>

        <a-form-item label="文字色" name="textColor">
          <div class="color-field">
            <a-input v-model:value="form.textColor" placeholder="#81D4FA">
              <template #prefix>
                <div class="color-preview" :style="{ background: form.textColor }" />
              </template>
            </a-input>
          </div>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            :rows="2"
            :maxlength="80"
            show-count
            placeholder="后台备注，不影响前台展示"
          />
        </a-form-item>

        <a-form-item label="实时预览">
          <div class="preview-panel">
            <div class="preview-bubble" :class="`is-${form.tier}`" :style="bubbleStyle(form)">
              <span>{{ form.label || '气泡文字' }}</span>
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
import { reactive, ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import type { AboutBubble } from '@/types'
import {
  createAboutBubbleApi,
  deleteAboutBubbleApi,
  getAboutBubblesApi,
  resetAboutBubblesApi,
  updateAboutBubbleApi
} from '@/api/aboutBubble'
import { addLog } from '@/api/log'

type BubbleForm = Omit<AboutBubble, 'id'>

const bubbles = ref<AboutBubble[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref()

const defaultForm: BubbleForm = {
  label: '',
  tier: 'mid',
  bg: 'rgba(91, 164, 230, 0.25)',
  glow: 'rgba(91, 164, 230, 0.4)',
  textColor: '#81D4FA',
  enabled: true,
  sort: 1,
  remark: ''
}

const form = reactive<BubbleForm>({ ...defaultForm })

const rules: Record<string, Rule[]> = {
  label: [{ required: true, message: '请输入气泡文字', trigger: 'blur' }],
  tier: [{ required: true, message: '请选择气泡层级', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  bg: [{ required: true, message: '请输入背景色', trigger: 'blur' }],
  glow: [{ required: true, message: '请输入发光色', trigger: 'blur' }],
  textColor: [{ required: true, message: '请输入文字色', trigger: 'blur' }]
}

const columns = [
  {
    title: '预览',
    key: 'preview',
    width: 110,
    align: 'center' as const
  },
  { title: '气泡文字', dataIndex: 'label', key: 'label', minWidth: 180 },
  {
    title: '层级',
    key: 'tier',
    width: 110,
    align: 'center' as const
  },
  {
    title: '颜色',
    key: 'colors',
    minWidth: 220
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort',
    width: 90,
    align: 'center' as const
  },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    align: 'center' as const
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    align: 'center' as const,
    fixed: 'right' as const
  }
]

const getTierText = (tier: AboutBubble['tier']) => {
  const map: Record<AboutBubble['tier'], string> = {
    big: '大气泡',
    mid: '中气泡',
    small: '小气泡'
  }
  return map[tier]
}

const getTierColor = (tier: AboutBubble['tier']) => {
  const map: Record<AboutBubble['tier'], string> = {
    big: 'red',
    mid: 'green',
    small: 'default'
  }
  return map[tier]
}

const bubbleStyle = (bubble: Pick<AboutBubble, 'bg' | 'glow' | 'textColor' | 'tier'>) => ({
  background: bubble.bg,
  color: bubble.textColor,
  borderColor: bubble.tier === 'small' ? 'rgba(255, 255, 255, 0.16)' : 'rgba(255, 255, 255, 0.34)',
  boxShadow: bubble.tier === 'big'
    ? `0 12px 38px ${bubble.glow}, inset 0 1px 2px rgba(255, 255, 255, 0.16)`
    : `inset 0 1px 2px rgba(255, 255, 255, 0.16)`
})

const loadData = async () => {
  loading.value = true
  try {
    bubbles.value = await getAboutBubblesApi()
  } finally {
    loading.value = false
  }
}

const openDrawer = (row?: AboutBubble) => {
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    Object.assign(form, {
      label: row.label,
      tier: row.tier,
      bg: row.bg,
      glow: row.glow,
      textColor: row.textColor,
      enabled: row.enabled,
      sort: row.sort,
      remark: row.remark || ''
    })
  } else {
    isEdit.value = false
    currentId.value = ''
    Object.assign(form, {
      ...defaultForm,
      sort: bubbles.value.length + 1
    })
  }
  drawerVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data: BubbleForm = { ...form }
    if (isEdit.value) {
      await updateAboutBubbleApi(currentId.value, data)
      addLog('更新', `关于气泡：${form.label}`, 'success')
      message.success('保存成功')
    } else {
      await createAboutBubbleApi(data)
      addLog('新建', `关于气泡：${form.label}`, 'success')
      message.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch {
    // 表单校验失败或用户取消
  } finally {
    submitLoading.value = false
  }
}

const handleToggle = async (row: AboutBubble, val: boolean) => {
  row.enabled = val
  await updateAboutBubbleApi(row.id, { enabled: val })
  addLog(val ? '启用' : '停用', `关于气泡：${row.label}`, 'success')
  message.success(val ? '已启用' : '已停用')
}

const handleDelete = (row: AboutBubble) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除气泡“${row.label}”吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteAboutBubbleApi(row.id)
      addLog('删除', `关于气泡：${row.label}`, 'success')
      message.success('删除成功')
      await loadData()
    }
  })
}

const handleReset = () => {
  Modal.confirm({
    title: '提示',
    content: '确定要恢复默认气泡数据吗？当前本地修改会被覆盖。',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      bubbles.value = await resetAboutBubblesApi()
      addLog('重置', '关于我的悬浮气泡', 'success')
      message.success('已恢复默认数据')
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.about-bubble-manage {
  :deep(.ant-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;

  span {
    font-size: 16px;
    font-weight: 500;
  }

  p {
    margin-top: 4px;
    color: #8c8c8c;
    font-size: 13px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-list {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-chip {
  width: 18px;
  height: 18px;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
}

.color-text {
  color: #595959;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.color-preview {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.color-field {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.table-bubble,
.preview-bubble {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid;
  border-radius: 50%;
  text-align: center;
  line-height: 1.3;
  word-break: break-word;
  overflow: hidden;
  backdrop-filter: blur(8px) saturate(180%);
}

.table-bubble {
  width: 48px;
  height: 48px;
  padding: 8px;
  margin: 0 auto;
  font-size: 10px;
  font-weight: 700;

  &.is-big {
    width: 58px;
    height: 58px;
  }

  &.is-small {
    width: 28px;
    height: 28px;
    font-size: 0;
  }
}

.preview-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 220px;
  background: linear-gradient(135deg, #1B4965 0%, #0D1B2A 100%);
  border-radius: 8px;
  overflow: hidden;
}

.preview-bubble {
  width: 108px;
  height: 108px;
  padding: 18px;
  font-size: 14px;
  font-weight: 700;

  &.is-big {
    width: 128px;
    height: 128px;
    font-size: 15px;
  }

  &.is-small {
    width: 42px;
    height: 42px;
    padding: 0;
    font-size: 0;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
