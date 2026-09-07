/**
 * 图片裁剪弹窗：cropperjs 裁剪，支持切换裁剪比例，确认后输出 JPEG blob 交由父组件上传 OSS。
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { Modal, Radio, type RadioChangeEvent } from 'antd'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import styles from './ImageCropperModal.module.scss'

interface ImageCropperModalProps {
  open: boolean
  src: string
  confirming?: boolean
  onConfirm: (blob: Blob) => void
  onCancel: () => void
}

const ImageCropperModal = ({ open, src, confirming, onConfirm, onCancel }: ImageCropperModalProps) => {
  const imageRef = useRef<HTMLImageElement | null>(null)
  const cropperRef = useRef<Cropper | null>(null)
  const [ratio, setRatio] = useState(0) // 0 表示自由比例
  const ratioRef = useRef(0) // ready 回调用 ref 读最新比例，避免闭包过期

  const destroyCropper = useCallback(() => {
    cropperRef.current?.destroy()
    cropperRef.current = null
  }, [])

  const applyRatio = useCallback((value: number) => {
    cropperRef.current?.setAspectRatio(value > 0 ? value : NaN)
  }, [])

  const initCropper = useCallback(() => {
    const imageEl = imageRef.current
    if (!imageEl) return
    destroyCropper()
    cropperRef.current = new Cropper(imageEl, {
      viewMode: 1, // 裁剪框不超出图片边界
      autoCropArea: 0.9,
      checkCrossOrigin: false, // crossorigin 已在 img 上手动设置
      ready: () => applyRatio(ratioRef.current)
    })
  }, [applyRatio, destroyCropper])

  // 关闭时销毁实例并重置比例；打开时图片若已缓存完成（onLoad 不会再触发）需主动初始化
  useEffect(() => {
    if (!open) {
      destroyCropper()
      setRatio(0)
      ratioRef.current = 0
      return
    }
    const imageEl = imageRef.current
    if (imageEl?.complete && imageEl.naturalWidth > 0) initCropper()
  }, [open, initCropper, destroyCropper])

  // 组件卸载兜底销毁 Cropper 实例
  useEffect(() => destroyCropper, [destroyCropper])

  const handleRatioChange = (e: RadioChangeEvent) => {
    const value = Number(e.target.value)
    setRatio(value)
    ratioRef.current = value
    applyRatio(value)
  }

  const handleOk = () => {
    const cropper = cropperRef.current
    if (!cropper) return
    const canvas = cropper.getCroppedCanvas({
      maxWidth: 2000,
      maxHeight: 2000,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high'
    })
    canvas.toBlob(blob => {
      if (blob) onConfirm(blob)
    }, 'image/jpeg', 0.92)
  }

  return (
    <Modal
      open={open}
      title="裁剪图片"
      width={760}
      okText="确认裁剪"
      cancelText="取消"
      confirmLoading={confirming}
      maskClosable={false}
      onOk={handleOk}
      onCancel={onCancel}
    >
      <div className={styles['ratio-bar']}>
        <span>裁剪比例：</span>
        <Radio.Group value={ratio} size="small" onChange={handleRatioChange}>
          <Radio.Button value={0}>自由</Radio.Button>
          <Radio.Button value={1}>1 : 1</Radio.Button>
          <Radio.Button value={4 / 3}>4 : 3</Radio.Button>
          <Radio.Button value={3 / 4}>3 : 4</Radio.Button>
          <Radio.Button value={16 / 9}>16 : 9</Radio.Button>
        </Radio.Group>
      </div>
      <div className={styles['cropper-stage']}>
        {/* key 强制换图时重建 img，保证 onLoad 再次触发；crossOrigin 避免 canvas 被跨域污染 */}
        <img
          key={src}
          ref={imageRef}
          src={src}
          crossOrigin="anonymous"
          alt="待裁剪图片"
          onLoad={initCropper}
        />
      </div>
      <p className={styles['tip']}>
        拖动选框调整区域，滚轮缩放图片；确认后裁剪结果将作为新图片上传到 OSS 素材库。
      </p>
    </Modal>
  )
}

export default ImageCropperModal
