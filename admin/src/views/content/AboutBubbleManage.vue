<template>
  <div class="about-bubble-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>关于我的悬浮气泡</span>
            <p>管理前台“我的成分”区域里的气泡文字、层级与视觉样式</p>
          </div>
          <div class="header-actions">
            <el-button @click="handleReset">
              <i class="ri-refresh-line" />
              恢复默认
            </el-button>
            <el-button type="primary" @click="openDrawer()">
              <i class="ri-add-line" />
              新建气泡
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="bubbles" v-loading="loading" stripe>
        <el-table-column label="预览" width="110" align="center">
          <template #default="{ row }">
            <div class="table-bubble" :class="`is-${row.tier}`" :style="bubbleStyle(row)">
              <span>{{ row.label || '小气泡' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="label" label="气泡文字" min-width="180" />
        <el-table-column label="层级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getTierType(row.tier)" size="small">{{ getTierText(row.tier) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="颜色" min-width="220">
          <template #default="{ row }">
            <div class="color-list">
              <span class="color-chip" :style="{ background: row.bg }" />
              <span class="color-chip" :style="{ background: row.glow }" />
              <span class="color-chip" :style="{ background: row.textColor }" />
              <span class="color-text">{{ row.textColor }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDrawer(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑悬浮气泡' : '新建悬浮气泡'"
      size="540px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="气泡文字" prop="label">
          <el-input v-model="form.label" maxlength="18" show-word-limit placeholder="例如：技术探索者" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="层级" prop="tier">
              <el-select v-model="form.tier" style="width: 100%">
                <el-option label="大气泡" value="big" />
                <el-option label="中气泡" value="mid" />
                <el-option label="小气泡" value="small" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>

        <el-divider content-position="left">视觉样式</el-divider>

        <el-form-item label="背景色" prop="bg">
          <div class="color-field">
            <el-color-picker v-model="form.bg" show-alpha color-format="rgb" />
            <el-input v-model="form.bg" placeholder="rgba(91, 164, 230, 0.25)" />
          </div>
        </el-form-item>

        <el-form-item label="发光色" prop="glow">
          <div class="color-field">
            <el-color-picker v-model="form.glow" show-alpha color-format="rgb" />
            <el-input v-model="form.glow" placeholder="rgba(91, 164, 230, 0.4)" />
          </div>
        </el-form-item>

        <el-form-item label="文字色" prop="textColor">
          <div class="color-field">
            <el-color-picker v-model="form.textColor" />
            <el-input v-model="form.textColor" placeholder="#81D4FA" />
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="80"
            show-word-limit
            placeholder="后台备注，不影响前台展示"
          />
        </el-form-item>

        <el-form-item label="实时预览">
          <div class="preview-panel">
            <div class="preview-bubble" :class="`is-${form.tier}`" :style="bubbleStyle(form)">
              <span>{{ form.label || '气泡文字' }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
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
const formRef = ref<FormInstance>()

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

const rules: FormRules = {
  label: [{ required: true, message: '请输入气泡文字', trigger: 'blur' }],
  tier: [{ required: true, message: '请选择气泡层级', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  bg: [{ required: true, message: '请输入背景色', trigger: 'blur' }],
  glow: [{ required: true, message: '请输入发光色', trigger: 'blur' }],
  textColor: [{ required: true, message: '请输入文字色', trigger: 'blur' }]
}

const getTierText = (tier: AboutBubble['tier']) => {
  const map = {
    big: '大气泡',
    mid: '中气泡',
    small: '小气泡'
  }
  return map[tier]
}

const getTierType = (tier: AboutBubble['tier']) => {
  const map = {
    big: 'danger',
    mid: 'success',
    small: 'info'
  } as const
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
      ElMessage.success('保存成功')
    } else {
      await createAboutBubbleApi(data)
      addLog('新建', `关于气泡：${form.label}`, 'success')
      ElMessage.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch {
    // 表单校验失败或用户取消
  } finally {
    submitLoading.value = false
  }
}

const handleToggle = async (row: AboutBubble) => {
  await updateAboutBubbleApi(row.id, { enabled: row.enabled })
  addLog(row.enabled ? '启用' : '停用', `关于气泡：${row.label}`, 'success')
  ElMessage.success(row.enabled ? '已启用' : '已停用')
}

const handleDelete = async (row: AboutBubble) => {
  try {
    await ElMessageBox.confirm(`确定要删除气泡“${row.label}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAboutBubbleApi(row.id)
    addLog('删除', `关于气泡：${row.label}`, 'success')
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 用户取消
  }
}

const handleReset = async () => {
  try {
    await ElMessageBox.confirm('确定要恢复默认气泡数据吗？当前本地修改会被覆盖。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    bubbles.value = await resetAboutBubblesApi()
    addLog('重置', '关于我的悬浮气泡', 'success')
    ElMessage.success('已恢复默认数据')
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.about-bubble-manage {
  :deep(.el-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  span {
    font-size: 16px;
    font-weight: 500;
  }

  p {
    margin-top: 4px;
    color: #909399;
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
  border: 1px solid #dcdfe6;
  border-radius: 50%;
}

.color-text {
  color: #606266;
  font-family: var(--el-font-family);
  font-size: 12px;
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

.color-field {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
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
  padding: 16px 20px;
  border-top: 1px solid #f0f2f5;
}

:deep(.el-divider) {
  margin: 16px 0 24px;
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
