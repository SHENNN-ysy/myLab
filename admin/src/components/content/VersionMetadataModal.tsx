import { useCallback, useRef, useState, type ReactElement } from 'react'
import { Form, Input, Modal } from 'antd'
import type { VersionMetadata } from '@/api/content'

export type { VersionMetadata }

interface VersionMetadataFormValues {
  versionName: string
  versionDescription: string
}

/**
 * 版本元数据弹窗 hook：requestMetadata 弹出版本名/描述表单，
 * 确认后 resolve 归一化结果，取消时 resolve null；
 * metadataModal 需渲染在调用方 JSX 中。
 */
export const useVersionMetadata = () => {
  const [form] = Form.useForm<VersionMetadataFormValues>()
  const [open, setOpen] = useState(false)
  // 当前挂起请求的 Promise resolve，同一时间至多一个
  const resolverRef = useRef<((value: VersionMetadata | null) => void) | null>(null)

  const requestMetadata = useCallback((initial?: Partial<VersionMetadata>) => {
    form.setFieldsValue({
      versionName: initial?.versionName || '',
      versionDescription: initial?.versionDescription || '',
    })
    setOpen(true)
    return new Promise<VersionMetadata | null>(resolve => {
      resolverRef.current = resolve
    })
  }, [form])

  const settle = (value: VersionMetadata | null) => {
    setOpen(false)
    resolverRef.current?.(value)
    resolverRef.current = null
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      settle({
        versionName: values.versionName.trim(),
        versionDescription: values.versionDescription.trim(),
      })
    } catch {
      // 校验未通过：保持弹窗打开，错误信息由 Form 展示
    }
  }

  const metadataModal: ReactElement = (
    <Modal
      title="保存草稿版本"
      width={560}
      okText="保存草稿"
      cancelText="取消"
      open={open}
      onOk={() => void handleOk()}
      onCancel={() => settle(null)}
      forceRender
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="versionName"
          label="版本名称"
          rules={[{ required: true, whitespace: true, message: '请填写版本名称' }]}
        >
          <Input
            maxLength={120}
            showCount
            placeholder="例如：首页轮播图秋季更新"
          />
        </Form.Item>
        <Form.Item
          name="versionDescription"
          label="版本描述"
          rules={[{ required: true, whitespace: true, message: '请填写版本描述' }]}
        >
          <Input.TextArea
            maxLength={2000}
            showCount
            rows={5}
            placeholder="说明本版本修改了什么，以及修改原因"
          />
        </Form.Item>
      </Form>
    </Modal>
  )

  return { requestMetadata, metadataModal }
}
