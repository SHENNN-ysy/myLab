<template>
  <div class="project-manage">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>项目管理</span>
          <a-button type="primary" @click="openDrawer()">
            <template #icon>
              <PlusOutlined />
            </template>
            新建项目
          </a-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar">
        <a-radio-group v-model:value="filterTag" @change="handleFilter">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button value="个人开源项目">个人开源项目</a-radio-button>
          <a-radio-button value="实验室项目">实验室项目</a-radio-button>
          <a-radio-button value="商业项目">商业项目</a-radio-button>
          <a-radio-button value="独立工具">独立工具</a-radio-button>
          <a-radio-button value="Web 实验">Web 实验</a-radio-button>
          <a-radio-button value="GameJam">GameJam</a-radio-button>
        </a-radio-group>
      </div>

      <!-- 列表 -->
      <a-table
        :data-source="filteredProjects"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'cover'">
            <a-image
              :src="record.image"
              :width="80"
              :height="50"
              :preview="true"
              :fallback="fallbackImg"
              class="project-cover"
            />
          </template>
          <template v-else-if="column.key === 'title'">
            <div class="project-title">
              {{ record.title }}
              <a-tag v-if="record.tagType === 'accent'" color="orange">
                {{ record.tag }}
              </a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'tag'">
            <a-tag>{{ record.tag }}</a-tag>
          </template>
          <template v-else-if="column.key === 'tech'">
            <template v-for="tech in (record.tech || []).slice(0, 3)" :key="tech">
              <a-tag style="margin-right: 4px; margin-bottom: 2px;">{{ tech }}</a-tag>
            </template>
            <span v-if="(record.tech?.length || 0) > 3" class="tech-more">
              +{{ (record.tech?.length || 0) - 3 }}
            </span>
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
      :title="isEdit ? '编辑项目' : '新建项目'"
      :width="600"
      @close="resetForm"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="项目名称" name="title">
          <a-input v-model:value="form.title" placeholder="如：Moth and Bat" />
        </a-form-item>

        <a-form-item label="简短描述" name="description">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="一句话描述项目" />
        </a-form-item>

        <a-form-item label="分类标签" name="tag">
          <a-select v-model:value="form.tag" placeholder="选择分类">
            <a-select-option value="GameJam">GameJam</a-select-option>
            <a-select-option value="个人开源项目">个人开源项目</a-select-option>
            <a-select-option value="实验室项目">实验室项目</a-select-option>
            <a-select-option value="商业项目">商业项目</a-select-option>
            <a-select-option value="独立工具">独立工具</a-select-option>
            <a-select-option value="Web 实验">Web 实验</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="是否为商业">
          <a-switch
            :checked="form.tagType === 'accent'"
            @change="(val: boolean) => (form.tagType = val ? 'accent' : 'default')"
          />
          <span class="form-tip">商业项目会在标签上显示特殊样式</span>
        </a-form-item>

        <a-form-item label="年份" name="year">
          <a-input-number v-model:value="form.year" :min="2000" :max="2030" />
        </a-form-item>

        <a-form-item label="封面图片" name="image">
          <a-input v-model:value="form.image" placeholder="图片 URL">
            <template #suffix>
              <a-button type="link" @click="testImage">测试</a-button>
            </template>
          </a-input>
          <div v-if="form.image" class="image-preview">
            <a-image :src="form.image" :width="200" :height="125" />
          </div>
        </a-form-item>

        <a-form-item label="技术栈">
          <a-select
            v-model:value="form.tech"
            mode="multiple"
            placeholder="输入后按回车添加"
            style="width: 100%"
            allow-clear
            show-search
          >
            <a-select-option v-for="tech in allTechs" :key="tech" :value="tech">
              {{ tech }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="详细内容">
          <a-textarea
            v-model:value="form.content"
            :rows="4"
            placeholder="项目详细介绍..."
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
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import type { Project } from '@/types'
import { getProjectsApi, createProjectApi, updateProjectApi, deleteProjectApi } from '@/api/project'
import { addLog } from '@/api/log'

const projects = ref<Project[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const filterTag = ref('')
const formRef = ref()

const fallbackImg = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="50"><rect width="80" height="50" fill="%23f0f0f0"/><text x="40" y="30" font-size="12" fill="%238c8c8c" text-anchor="middle">暂无图片</text></svg>'

const allTechs = [
  'Vue', 'React', 'TypeScript', 'JavaScript', 'Python', 'Java', 'C#', '.NET',
  'Go', 'Node.js', 'Spring Boot', 'Unity', 'Godot', 'Unreal Engine', 'Docker',
  'SQL', 'PostgreSQL', 'MongoDB', 'Redis', 'Nginx', 'AWS', 'Vercel', 'Supabase',
  'Web Audio API', 'Tone.js', 'Phaser', 'Aseprite', 'GDScript', 'C++', 'Lua'
]

const form = reactive({
  title: '',
  description: '',
  tag: '个人开源项目',
  tagType: 'default' as 'default' | 'accent',
  year: new Date().getFullYear(),
  image: '',
  content: '',
  tech: [] as string[]
})

const rules: Record<string, Rule[]> = {
  title: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
  tag: [{ required: true, message: '请选择分类', trigger: 'change' }],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  image: [{ required: true, message: '请输入封面图片URL', trigger: 'blur' }]
}

const columns = [
  {
    title: '封面',
    key: 'cover',
    width: 120,
    align: 'center' as const
  },
  {
    title: '项目名称',
    dataIndex: 'title',
    key: 'title',
    minWidth: 160
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    minWidth: 200,
    ellipsis: true
  },
  {
    title: '分类',
    dataIndex: 'tag',
    key: 'tag',
    width: 120,
    align: 'center' as const
  },
  {
    title: '年份',
    dataIndex: 'year',
    key: 'year',
    width: 100,
    align: 'center' as const,
    sorter: (a: Project, b: Project) => a.year - b.year
  },
  {
    title: '技术栈',
    key: 'tech',
    minWidth: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    align: 'center' as const,
    fixed: 'right' as const
  }
]

const filteredProjects = computed(() => {
  if (!filterTag.value) return projects.value
  return projects.value.filter(p => p.tag === filterTag.value)
})

const loadData = async () => {
  loading.value = true
  try {
    projects.value = await getProjectsApi()
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  // 筛选由 computed 自动处理
}

const openDrawer = (row?: Project) => {
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    Object.assign(form, {
      title: row.title,
      description: row.description,
      tag: row.tag,
      tagType: row.tagType || 'default',
      year: row.year,
      image: row.image,
      content: row.content || '',
      tech: row.tech || []
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
    title: '',
    description: '',
    tag: '个人开源项目',
    tagType: 'default',
    year: new Date().getFullYear(),
    image: '',
    content: '',
    tech: []
  })
}

const testImage = () => {
  if (form.image) {
    window.open(form.image, '_blank')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data = {
      ...form,
      tech: form.tech || []
    }

    if (isEdit.value) {
      await updateProjectApi(currentId.value, data)
      addLog('更新', `项目：${form.title}`, 'success')
      message.success('更新成功')
    } else {
      await createProjectApi(data)
      addLog('新建', `项目：${form.title}`, 'success')
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

const handleDelete = (row: Project) => {
  Modal.confirm({
    title: '提示',
    content: `确定要删除项目「${row.title}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      await deleteProjectApi(row.id)
      addLog('删除', `项目：${row.title}`, 'success')
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
.project-manage {
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

.filter-bar {
  margin-bottom: 16px;
}

.project-cover {
  border-radius: 4px;
}

.project-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.tech-more {
  font-size: 12px;
  color: #8c8c8c;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.image-preview {
  margin-top: 12px;

  :deep(.ant-image) {
    border-radius: 8px;
    overflow: hidden;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
