import { h, reactive } from 'vue'
import { Input, message, Modal } from 'ant-design-vue'

export interface VersionMetadata {
  versionName: string
  versionDescription: string
}

/** 弹出版本元数据表单，确认后返回归一化结果，取消时返回 null。 */
export const requestVersionMetadata = (initial?: Partial<VersionMetadata>): Promise<VersionMetadata | null> => {
  const form = reactive({
    versionName: initial?.versionName || '',
    versionDescription: initial?.versionDescription || '',
  })

  return new Promise(resolve => {
    let settled = false
    Modal.confirm({
      title: '保存草稿版本',
      width: 560,
      okText: '保存草稿',
      cancelText: '取消',
      content: () => h('div', { class: 'version-metadata-form' }, [
        h('div', { style: 'margin-top: 20px;' }, [
          h('label', { style: 'display:block;margin-bottom:8px;font-weight:500;' }, '版本名称'),
          h(Input, {
            value: form.versionName,
            maxlength: 120,
            showCount: true,
            placeholder: '例如：首页轮播图秋季更新',
            'onUpdate:value': (value: string) => { form.versionName = value },
          }),
        ]),
        h('div', { style: 'margin-top: 18px;' }, [
          h('label', { style: 'display:block;margin-bottom:8px;font-weight:500;' }, '版本描述'),
          h(Input.TextArea, {
            value: form.versionDescription,
            maxlength: 2000,
            showCount: true,
            rows: 5,
            placeholder: '说明本版本修改了什么，以及修改原因',
            'onUpdate:value': (value: string) => { form.versionDescription = value },
          }),
        ]),
      ]),
      onOk: () => {
        const versionName = form.versionName.trim()
        const versionDescription = form.versionDescription.trim()
        if (!versionName || !versionDescription) {
          message.error('请填写版本名称和版本描述')
          return Promise.reject(new Error('version metadata required'))
        }
        settled = true
        resolve({ versionName, versionDescription })
        return undefined
      },
      onCancel: () => {
        if (!settled) resolve(null)
      },
    })
  })
}
