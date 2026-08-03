<template>
  <div class="footprint-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>足迹管理</span>
          <a-button type="primary" @click="openDrawer()">
            <template #icon>
              <PlusOutlined />
            </template>
            新建足迹
          </a-button>
        </div>
      </template>

      <a-table
        :data-source="footprints"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="city-name">
              <span class="city-dot" :class="{ 'is-self': record.isSelf }" />
              {{ record.name }}
            </div>
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

    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑足迹' : '新建足迹'"
      :width="500"
      @close="resetForm"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="城市名称" name="name">
          <a-input v-model:value="form.name" placeholder="如：北京、上海" />
        </a-form-item>

        <a-form-item label="标签" name="tag">
          <a-input v-model:value="form.tag" placeholder="如：探索更多" />
        </a-form-item>

        <a-form-item label="是否为当前位置">
          <a-switch v-model:checked="form.isSelf" />
          <span class="form-tip">当前位置会在地图上显示头像标记</span>
        </a-form-item>

        <a-divider orientation="left">地图坐标</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="X 坐标 (%)" name="positionX">
              <a-input-number
                v-model:value="form.positionX"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Y 坐标 (%)" name="positionY">
              <a-input-number
                v-model:value="form.positionY"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="坐标预览">
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
        </a-form-item>

        <a-divider orientation="left">提示信息</a-divider>

        <a-form-item label="标题" name="['tip.title']">
          <a-input v-model:value="form.tip.title" placeholder="如：北京" />
        </a-form-item>

        <a-form-item label="经纬度" name="['tip.coords']">
          <a-input v-model:value="form.tip.coords" placeholder="如：39.91°N · 116.39°E" />
        </a-form-item>

        <a-form-item label="场景描述" name="['tip.scene']">
          <a-textarea
            v-model:value="form.tip.scene"
            :rows="2"
            placeholder="如：文化 · 历史 · city walk"
          />
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
import type { Footprint } from '@/types'
import { getFootprintsApi, createFootprintApi, updateFootprintApi, deleteFootprintApi } from '@/api/footprint'
import { addLog } from '@/api/log'

const footprints = ref<Footprint[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const formRef = ref()

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

const rules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入城市名称', trigger: 'blur' }],
  tag: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  positionX: [{ required: true, message: '请输入X坐标', trigger: 'blur' }],
  positionY: [{ required: true, message: '请输入Y坐标', trigger: 'blur' }],
  'tip.title': [{ required: true, message: '请输入标题', trigger: 'blur' }],
  'tip.coords': [{ required: true, message: '请输入坐标', trigger: 'blur' }],
  'tip.scene': [{ required: true, message: '请输入场景描述', trigger: 'blur' }]
}

const columns = [
  {
    title: '城市',
    key: 'name',
    width: 120
  },
  {
    title: '标签',
    dataIndex: 'tag',
    key: 'tag',
    width: 120
  },
  {
    title: '坐标',
    dataIndex: ['tip', 'coords'],
    key: 'tip-coords',
    minWidth: 160
  },
  {
    title: '场景描述',
    dataIndex: ['tip', 'scene'],
    key: 'tip-scene',
    minWidth: 200
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    align: 'center' as const
  }
]

const loadData = async () => {
  loading.value = true
  try {
    footprints.value = await getFootprintsApi()
  } finally {
    loading.value = false
  }
}

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
      message.success('更新成功')
    } else {
      await createFootprintApi(data)
      addLog('新建', `足迹：${form.name}`, 'success')
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

const handleDelete = (row: Footprint) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除足迹「${row.name}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteFootprintApi(row.id)
      addLog('删除', `足迹：${row.name}`, 'success')
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
.footprint-manage {
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

.city-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
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
  color: #8c8c8c;
}

.position-preview {
  .preview-map {
    position: relative;
    width: 100%;
    aspect-ratio: 1029 / 823;
    border-radius: 8px;
    overflow: hidden;
    background: linear-gradient(135deg, #f5f5f5, #e6f4ff);
    border: 1px dashed #d9d9d9;

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
}
</style>
