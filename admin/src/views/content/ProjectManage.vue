<template>
  <div class="project-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>项目管理</span>
          <el-button type="primary" @click="openDrawer()">
            <i class="ri-add-line" />
            新建项目
          </el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterTag" @change="handleFilter">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="个人开源项目">个人开源项目</el-radio-button>
          <el-radio-button label="实验室项目">实验室项目</el-radio-button>
          <el-radio-button label="商业项目">商业项目</el-radio-button>
          <el-radio-button label="独立工具">独立工具</el-radio-button>
          <el-radio-button label="Web 实验">Web 实验</el-radio-button>
          <el-radio-button label="GameJam">GameJam</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 列表 -->
      <el-table :data="filteredProjects" v-loading="loading" stripe>
        <el-table-column label="封面" width="120" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.image"
              :preview-src-list="[row.image]"
              fit="cover"
              class="project-cover"
            >
              <template #error>
                <div class="cover-placeholder">
                  <i class="ri-image-line" />
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="项目名称" min-width="160">
          <template #default="{ row }">
            <div class="project-title">
              {{ row.title }}
              <el-tag v-if="row.tagType === 'accent'" type="warning" size="small" effect="dark">
                {{ row.tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="tag" label="分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="100" align="center" sortable />
        <el-table-column label="技术栈" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="tech in (row.tech || []).slice(0, 3)"
              :key="tech"
              size="small"
              style="margin-right: 4px; margin-bottom: 2px;"
            >
              {{ tech }}
            </el-tag>
            <span v-if="(row.tech?.length || 0) > 3" class="tech-more">
              +{{ (row.tech?.length || 0) - 3 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDrawer(row)">
              编辑
            </el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 抽屉表单 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑项目' : '新建项目'"
      size="600px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="title">
          <el-input v-model="form.title" placeholder="如：Moth and Bat" />
        </el-form-item>

        <el-form-item label="简短描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="一句话描述项目"
          />
        </el-form-item>

        <el-form-item label="分类标签" prop="tag">
          <el-select v-model="form.tag" placeholder="选择分类">
            <el-option label="GameJam" value="GameJam" />
            <el-option label="个人开源项目" value="个人开源项目" />
            <el-option label="实验室项目" value="实验室项目" />
            <el-option label="商业项目" value="商业项目" />
            <el-option label="独立工具" value="独立工具" />
            <el-option label="Web 实验" value="Web 实验" />
          </el-select>
        </el-form-item>

        <el-form-item label="是否为商业">
          <el-switch v-model="form.tagType" active-value="accent" inactive-value="default" />
          <span class="form-tip">商业项目会在标签上显示特殊样式</span>
        </el-form-item>

        <el-form-item label="年份" prop="year">
          <el-input-number v-model="form.year" :min="2000" :max="2030" />
        </el-form-item>

        <el-form-item label="封面图片" prop="image">
          <el-input v-model="form.image" placeholder="图片 URL">
            <template #append>
              <el-button @click="testImage">测试</el-button>
            </template>
          </el-input>
          <div v-if="form.image" class="image-preview">
            <el-image :src="form.image" fit="cover" class="preview-img">
              <template #error>
                <div class="preview-error">图片加载失败</div>
              </template>
            </el-image>
          </div>
        </el-form-item>

        <el-form-item label="技术栈">
          <el-select
            v-model="form.tech"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入后按回车添加"
            style="width: 100%"
          >
            <el-option
              v-for="tech in allTechs"
              :key="tech"
              :label="tech"
              :value="tech"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="详细内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="项目详细介绍..."
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
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
const formRef = ref<FormInstance>()

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

const rules: FormRules = {
  title: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
  tag: [{ required: true, message: '请选择分类', trigger: 'change' }],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  image: [{ required: true, message: '请输入封面图片URL', trigger: 'blur' }]
}

const filteredProjects = computed(() => {
  if (!filterTag.value) return projects.value
  return projects.value.filter(p => p.tag === filterTag.value)
})

// 加载数据
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

// 打开抽屉
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

// 重置表单
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

// 测试图片
const testImage = () => {
  if (form.image) {
    window.open(form.image, '_blank')
  }
}

// 提交
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
      ElMessage.success('更新成功')
    } else {
      await createProjectApi(data)
      addLog('新建', `项目：${form.title}`, 'success')
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
const handleDelete = async (row: Project) => {
  try {
    await ElMessageBox.confirm(`确定要删除项目「${row.title}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteProjectApi(row.id)
    addLog('删除', `项目：${row.title}`, 'success')
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
.project-manage {
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

.filter-bar {
  margin-bottom: 16px;

  :deep(.el-radio-button__inner) {
    border-radius: 4px;
  }
}

.project-cover {
  width: 80px;
  height: 50px;
  border-radius: 4px;
}

.cover-placeholder {
  width: 80px;
  height: 50px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 24px;
}

.project-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.tech-more {
  font-size: 12px;
  color: #909399;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.image-preview {
  margin-top: 12px;

  .preview-img {
    width: 200px;
    height: 125px;
    border-radius: 8px;
  }

  .preview-error {
    width: 200px;
    height: 125px;
    background: #f5f7fa;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #909399;
    font-size: 12px;
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0f2f5;
}
</style>
