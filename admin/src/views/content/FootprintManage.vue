<template>
  <div class="footprint-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>足迹管理</span>
          <el-button type="primary" @click="openDrawer()">
            <i class="ri-add-line" />
            新建足迹
          </el-button>
        </div>
      </template>

      <el-table :data="footprints" v-loading="loading" stripe>
        <el-table-column label="城市" width="120">
          <template #default="{ row }">
            <div class="city-name">
              <span class="city-dot" :class="{ 'is-self': row.isSelf }" />
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="tag" label="标签" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tip.coords" label="坐标" min-width="160" />
        <el-table-column prop="tip.scene" label="场景描述" min-width="200" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button type="primary" text size="small" @click="openDrawer(row)">
                编辑
              </el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑足迹' : '新建足迹'"
      size="500px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="城市名称" prop="name">
          <el-input v-model="form.name" placeholder="如：北京、上海" />
        </el-form-item>

        <el-form-item label="标签" prop="tag">
          <el-input v-model="form.tag" placeholder="如：探索更多" />
        </el-form-item>

        <el-form-item label="是否为当前位置">
          <el-switch v-model="form.isSelf" />
          <span class="form-tip">当前位置会在地图上显示头像标记</span>
        </el-form-item>

        <el-divider content-position="left">地图坐标</el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="X 坐标 (%)" prop="positionX">
              <el-input-number
                v-model="form.positionX"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Y 坐标 (%)" prop="positionY">
              <el-input-number
                v-model="form.positionY"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="坐标预览">
          <div class="position-preview">
            <div class="preview-map">
              <div class="preview-map-placeholder">
                <div
                  class="preview-marker"
                  :style="{ left: form.positionX + '%', top: form.positionY + '%' }"
                />
              </div>
            </div>
          </div>
        </el-form-item>

        <el-divider content-position="left">提示信息</el-divider>

        <el-form-item label="标题" prop="tip.title">
          <el-input v-model="form.tip.title" placeholder="如：北京" />
        </el-form-item>

        <el-form-item label="经纬度" prop="tip.coords">
          <el-input v-model="form.tip.coords" placeholder="如：39.91°N · 116.39°E" />
        </el-form-item>

        <el-form-item label="场景描述" prop="tip.scene">
          <el-input
            v-model="form.tip.scene"
            type="textarea"
            :rows="2"
            placeholder="如：文化 · 历史 · city walk"
          />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { Footprint } from '@/types'
import { getFootprintsApi, createFootprintApi, updateFootprintApi, deleteFootprintApi } from '@/api/footprint'
import { addLog } from '@/api/log'

const footprints = ref<Footprint[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  tag: '探索更多',
  isSelf: false,
  positionX: 50,
  positionY: 50,
  tip: {
    title: '',
    coords: '',
    scene: ''
  }
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入城市名称', trigger: 'blur' }],
  tag: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  positionX: [{ required: true, message: '请输入X坐标', trigger: 'blur' }],
  positionY: [{ required: true, message: '请输入Y坐标', trigger: 'blur' }],
  'tip.title': [{ required: true, message: '请输入标题', trigger: 'blur' }],
  'tip.coords': [{ required: true, message: '请输入坐标', trigger: 'blur' }],
  'tip.scene': [{ required: true, message: '请输入场景描述', trigger: 'blur' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    footprints.value = await getFootprintsApi()
  } finally {
    loading.value = false
  }
}

// 打开抽屉
const openDrawer = (row?: Footprint) => {
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    Object.assign(form, {
      name: row.name,
      tag: row.tag,
      isSelf: row.isSelf || false,
      positionX: row.position.x,
      positionY: row.position.y,
      tip: { ...row.tip }
    })
  } else {
    isEdit.value = false
    currentId.value = ''
  }
  drawerVisible.value = true
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    name: '',
    tag: '探索更多',
    isSelf: false,
    positionX: 50,
    positionY: 50,
    tip: {
      title: '',
      coords: '',
      scene: ''
    }
  })
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data = {
      name: form.name,
      tag: form.tag,
      isSelf: form.isSelf,
      position: {
        x: form.positionX,
        y: form.positionY
      },
      tip: { ...form.tip }
    }

    if (isEdit.value) {
      await updateFootprintApi(currentId.value, data)
      addLog('更新', `足迹：${form.name}`, 'success')
      ElMessage.success('更新成功')
    } else {
      await createFootprintApi(data)
      addLog('新建', `足迹：${form.name}`, 'success')
      ElMessage.success('创建成功')
    }

    drawerVisible.value = false
    await loadData()
  } catch (e) {
    // 验证失败
  } finally {
    submitLoading.value = false
  }
}

// 删除
const handleDelete = async (row: Footprint) => {
  try {
    await ElMessageBox.confirm(`确定要删除足迹「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteFootprintApi(row.id)
    addLog('删除', `足迹：${row.name}`, 'success')
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.footprint-manage {
  :deep(.el-card) {
    border: none;
    border-radius: 8px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  span {
    font-size: 16px;
    font-weight: 500;
  }
}

.city-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0;
}

.city-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #FF6B6B;
  flex-shrink: 0;

  &.is-self {
    width: 10px;
    height: 10px;
    background: transparent;
    border: 2px solid #FF6B6B;
    box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.3);
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.position-preview {
  .preview-map {
    position: relative;
    width: 100%;
    aspect-ratio: 1029 / 823;
    border-radius: 8px;
    overflow: hidden;
    background: linear-gradient(135deg, #f0f2f5, #e8f4fd);
    border: 1px dashed #dcdfe6;

    .preview-map-placeholder {
      position: absolute;
      inset: 0;
    }

    .preview-marker {
      position: absolute;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #FF6B6B;
      border: 2px solid #fff;
      transform: translate(-50%, -50%);
      box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.4);
      transition: all 0.2s;
    }
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
</style>
