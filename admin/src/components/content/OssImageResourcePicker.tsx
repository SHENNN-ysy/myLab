/**
 * OSS 图片资源选择器：展示/移除已选资源，弹窗内按 4 列网格选择图片，支持直接上传图片到 OSS。
 */
import { useState } from 'react'
import { App, Button, Empty, Modal, Spin, Upload } from 'antd'
import type { FileResource, ResourceDirectory } from '@/types'
import { getAllFilesApi, getFileAccessUrlApi, uploadFileApi } from '@/api/file'
import styles from './OssImageResourcePicker.module.scss'

export interface OssImageResourceValue {
  id: string
  name: string
  url: string
}

interface OssImageResourcePickerProps {
  value: OssImageResourceValue | null
  onChange: (value: OssImageResourceValue | null) => void
  directory: ResourceDirectory
}

const OssImageResourcePicker = ({ value, onChange, directory }: OssImageResourcePickerProps) => {
  const { message } = App.useApp()
  const [visible, setVisible] = useState(false)
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [files, setFiles] = useState<FileResource[]>([])

  const openPicker = async () => {
    setVisible(true)
    setLoading(true)
    try {
      setFiles((await getAllFilesApi(directory)).filter(file => file.mimeType.startsWith('image/')))
    } finally {
      setLoading(false)
    }
  }

  const selectFile = async (file: FileResource) => {
    const url = file.url || await getFileAccessUrlApi(file.id)
    onChange({ id: file.id, name: file.originalName || file.objectKey, url })
    setVisible(false)
  }

  // beforeUpload 返回 false 阻断 antd 默认上传行为，改走自定义上传流程
  const uploadImage = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      message.error('只能上传图片资源')
      return false
    }
    setUploading(true)
    try {
      const uploaded = await uploadFileApi(file, directory)
      setFiles(prev => [uploaded, ...prev])
      await selectFile(uploaded)
      message.success('图片已上传到 OSS')
    } finally {
      setUploading(false)
    }
    return false
  }

  return (
    <div className={styles['oss-picker']}>
      {value ? (
        <div className={styles['selected-resource']}>
          {value.url && <img src={value.url} alt={value.name} />}
          <div><strong>{value.name}</strong><small>OSS 资源 ID：{value.id}</small></div>
          <Button type="link" danger onClick={() => onChange(null)}>
            移除
          </Button>
        </div>
      ) : (
        <Button block onClick={openPicker}>
          从 OSS 素材库选择
        </Button>
      )}
      {value && (
        <Button size="small" className={styles['replace-button']} onClick={openPicker}>
          更换资源
        </Button>
      )}

      <Modal
        open={visible}
        title="选择 OSS 图片资源"
        width={780}
        footer={null}
        onCancel={() => setVisible(false)}
      >
        <Upload showUploadList={false} beforeUpload={uploadImage} accept="image/*">
          <Button type="primary" loading={uploading}>
            上传图片到 OSS
          </Button>
        </Upload>
        <Spin spinning={loading}>
          <div className={styles['resource-grid']}>
            {files.map(file => (
              <button
                key={file.id}
                type="button"
                className={styles['resource-item']}
                onClick={() => selectFile(file)}
              >
                {file.url ? (
                  <img src={file.url} alt={file.originalName} />
                ) : (
                  <div className={styles['image-placeholder']}>
                    IMG
                  </div>
                )}
                <span>{file.originalName || file.objectKey}</span>
              </button>
            ))}
          </div>
          {!loading && files.length === 0 && <Empty description="OSS 中暂无图片资源" />}
        </Spin>
      </Modal>
    </div>
  )
}

export default OssImageResourcePicker
