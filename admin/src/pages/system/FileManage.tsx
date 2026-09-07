/**
 * 文件资源管理：分页列表、目录筛选、多文件上传、删除前引用检查、复制预签名访问地址。
 */
import { useCallback, useEffect, useRef, useState, type ChangeEvent } from 'react'
import { App, Alert, Button, Card, Col, Image, Input, Modal, Row, Select, Space, Table, Tag } from 'antd'
import type { TablePaginationConfig, TableProps } from 'antd'
import {
  DatabaseOutlined,
  FileImageOutlined,
  FileOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined
} from '@ant-design/icons'
import type { FileReference, FileResource, ResourceDirectory } from '@/types'
import { deleteFileApi, getFileAccessUrlApi, getFileListApi, getFileReferencesApi, uploadFileApi } from '@/api/file'
import styles from './FileManage.module.scss'

const imageTypes = 'image/png,image/jpeg,image/webp,image/gif,image/svg+xml'
const acceptedTypes = imageTypes

const filterDirectoryOptions: Array<{ value: ResourceDirectory, label: string }> = [
  { value: 'hero', label: '首页图片 hero' },
  { value: 'icon', label: '图标 icon' },
  { value: 'hobbies', label: '爱好 hobbies' },
  { value: 'footstep', label: '足迹 footstep' },
  { value: 'mylab-post', label: 'MyLab 封面' }
]
const uploadDirectoryOptions = filterDirectoryOptions

const isImage = (file: FileResource) => file.mimeType.startsWith('image/')
const getFileTypeLabel = (type: string) => type.startsWith('image/') ? '图片' : type
const formatFileSize = (size: number) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 ** 2) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 ** 3) return `${(size / 1024 ** 2).toFixed(1)} MB`
  return `${(size / 1024 ** 3).toFixed(2)} GB`
}
const formatTime = (value: string) => value ? new Date(value).toLocaleString('zh-CN') : '-'
const directoryLabel = (directory?: ResourceDirectory) =>
  filterDirectoryOptions.find(item => item.value === directory)?.label || '未分类资源'
const moduleLabel = (moduleKey: string) => ({
  home: '首页图片',
  about: '关于我',
  skills: '技术栈',
  footprints: '足迹',
  hobbies: '爱好',
  vibe: 'Vibe Coding',
  mylab: 'MyLab'
} as Record<string, string>)[moduleKey] || moduleKey
const versionStateLabel = (state: string) => ({
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
  OFFLINE: '已下线'
} as Record<string, string>)[state] || state

const FileManage = () => {
  const { message } = App.useApp()
  const [files, setFiles] = useState<FileResource[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [filterDirectory, setFilterDirectory] = useState<ResourceDirectory>()
  const [uploadDirectory, setUploadDirectory] = useState<ResourceDirectory>('hero')
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [total, setTotal] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState<FileResource | null>(null)
  const [deleteReferences, setDeleteReferences] = useState<FileReference[]>([])
  const [deleting, setDeleting] = useState(false)

  // 加载列表：分页参数显式传入，避免 setState 后读到旧值
  const loadData = useCallback(async (nextPage: number, nextPageSize: number, directory?: ResourceDirectory) => {
    setLoading(true)
    try {
      const result = await getFileListApi(nextPage, nextPageSize, directory)
      setFiles(result.records)
      setTotal(result.total)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData(1, 20)
  }, [loadData])

  const currentPageSizeText = formatFileSize(files.reduce((sum, file) => sum + file.size, 0))
  const keyword = searchKeyword.trim().toLowerCase()
  const filteredFiles = files.filter(file =>
    !keyword || file.originalName.toLowerCase().includes(keyword) || file.objectKey.toLowerCase().includes(keyword)
  )

  const handleTableChange = (value: TablePaginationConfig) => {
    const nextPage = value.current || 1
    const nextPageSize = value.pageSize || 20
    setPage(nextPage)
    setPageSize(nextPageSize)
    void loadData(nextPage, nextPageSize, filterDirectory)
  }

  const onDirectoryFilterChange = (value?: ResourceDirectory) => {
    setFilterDirectory(value)
    setPage(1)
    void loadData(1, pageSize, value)
  }

  const triggerUpload = () => fileInputRef.current?.click()
  const handleUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files
    if (!selected?.length) return
    setLoading(true)
    try {
      await Promise.all(Array.from(selected).map(file => uploadFileApi(file, uploadDirectory)))
      message.success(`已上传 ${selected.length} 个文件`)
      setPage(1)
      await loadData(1, pageSize, filterDirectory)
    } finally {
      setLoading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const copyAccessUrl = async (file: FileResource) => {
    const url = file.url || await getFileAccessUrlApi(file.id)
    await navigator.clipboard.writeText(url)
    message.success('访问地址已复制，有效期以接口返回策略为准')
  }

  // 删除前先查引用，再打开受控确认弹窗；仍被引用时禁用删除按钮
  const handleDelete = async (file: FileResource) => {
    const references = await getFileReferencesApi(file.id)
    setDeleteReferences(references)
    setDeleteTarget(file)
  }

  const confirmDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteFileApi(deleteTarget.id)
      // 删空当前页时回退一页
      const nextPage = files.length === 1 && page > 1 ? page - 1 : page
      setPage(nextPage)
      message.success('文件删除任务已提交')
      setDeleteTarget(null)
      await loadData(nextPage, pageSize, filterDirectory)
    } finally {
      setDeleting(false)
    }
  }

  const columns: TableProps<FileResource>['columns'] = [
    {
      title: '预览',
      key: 'preview',
      width: 80,
      align: 'center',
      render: (_, record) => isImage(record) && record.url ? (
        <Image src={record.url} width={50} height={50} className={styles['file-thumb']} />
      ) : (
        <div className={styles['file-icon']}>
          {record.mimeType.startsWith('image/') ? <FileImageOutlined /> : <FileOutlined />}
        </div>
      )
    },
    {
      title: '文件',
      key: 'name',
      render: (_, record) => (
        <div className={styles['file-name']}>
          <strong>{record.originalName || '未命名文件'}</strong>
          <span>{record.objectKey}</span>
        </div>
      )
    },
    {
      title: 'MIME',
      key: 'mimeType',
      width: 150,
      align: 'center',
      render: (_, record) => (
        <Tag color={record.mimeType.startsWith('image/') ? 'success' : 'default'}>
          {getFileTypeLabel(record.mimeType)}
        </Tag>
      )
    },
    {
      title: '目录',
      key: 'directory',
      width: 130,
      align: 'center',
      render: (_, record) => <Tag>{directoryLabel(record.directory)}</Tag>
    },
    {
      title: '大小',
      key: 'size',
      width: 110,
      align: 'right',
      render: (_, record) => formatFileSize(record.size)
    },
    { title: '存储桶', dataIndex: 'bucket', key: 'bucket', width: 130 },
    {
      title: '上传时间',
      key: 'createdAt',
      width: 180,
      render: (_, record) => formatTime(record.createdAt)
    },
    {
      title: '操作',
      key: 'actions',
      width: 210,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => copyAccessUrl(record)}>
            复制访问地址
          </Button>
          <Button type="link" danger size="small" onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className={styles['file-manage']}>
      <Card
        variant="borderless"
        title={(
          <div className={styles['card-header']}>
            <span>文件资源</span>
            <div className={styles['header-actions']}>
              <Input
                value={searchKeyword}
                placeholder="筛选当前页文件名"
                allowClear
                style={{ width: 210 }}
                prefix={<SearchOutlined />}
                onChange={event => setSearchKeyword(event.target.value)}
              />
              <Select
                value={filterDirectory}
                placeholder="筛选 OSS 目录"
                allowClear
                style={{ width: 150 }}
                options={filterDirectoryOptions}
                onChange={onDirectoryFilterChange}
              />
              <Select
                value={uploadDirectory}
                style={{ width: 150 }}
                options={uploadDirectoryOptions}
                aria-label="上传目标目录"
                onChange={setUploadDirectory}
              />
              <Button type="primary" icon={<UploadOutlined />} onClick={triggerUpload}>
                上传文件
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept={acceptedTypes}
                className={styles['hidden-input']}
                onChange={handleUpload}
              />
              <Button icon={<ReloadOutlined />} onClick={() => void loadData(page, pageSize, filterDirectory)}>
                刷新
              </Button>
            </div>
          </div>
        )}
      >
        <Alert
          type="info"
          showIcon
          className={styles['file-tip']}
          message="删除前后端会检查所有草稿、线上和历史版本，仍被引用的资源不可删除。"
        />

        <Row gutter={16} className={styles['stat-row']}>
          <Col xs={24} sm={12}>
            <div className={styles['stat-item']}>
              <FileOutlined className={`${styles['stat-icon']} ${styles['icon-blue']}`} />
              <div>
                <div className={styles['stat-value']}>{total}</div>
                <div className={styles['stat-label']}>资源总数</div>
              </div>
            </div>
          </Col>
          <Col xs={24} sm={12}>
            <div className={styles['stat-item']}>
              <DatabaseOutlined className={`${styles['stat-icon']} ${styles['icon-orange']}`} />
              <div>
                <div className={styles['stat-value']}>{currentPageSizeText}</div>
                <div className={styles['stat-label']}>当前页占用</div>
              </div>
            </div>
          </Col>
        </Row>

        <Table<FileResource>
          dataSource={filteredFiles}
          columns={columns}
          loading={loading}
          rowKey="id"
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: value => `共 ${value} 个资源`
          }}
          onChange={handleTableChange}
        />
      </Card>

      <Modal
        open={!!deleteTarget}
        title={`确认删除「${deleteTarget ? deleteTarget.originalName || deleteTarget.objectKey : ''}」？`}
        okText="删除"
        okButtonProps={{ danger: true, disabled: deleteReferences.length > 0 }}
        confirmLoading={deleting}
        onOk={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      >
        {deleteReferences.length ? (
          <div>
            <p>该资源仍被以下内容版本引用，解除引用后才可删除：</p>
            <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
              {deleteReferences.map((ref, index) => (
                <li key={index}>
                  {`${moduleLabel(ref.moduleKey)} · v${ref.versionNo}（${versionStateLabel(ref.state)}）· ${ref.usage}`}
                </li>
              ))}
            </ul>
          </div>
        ) : (
          '未被任何内容版本引用，可安全删除。'
        )}
      </Modal>
    </div>
  )
}

export default FileManage
