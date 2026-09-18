/** 虚线框尺寸（rpx），与 camera-diet 页面 scan-box 保持一致 */
export const SCAN_BOX = {
  widthRpx: 480,
  heightRpx: 540
}

/** 计算屏幕居中虚线框（px） */
export function getScanBoxRect() {
  const sys = uni.getSystemInfoSync()
  const screenW = sys.windowWidth
  const screenH = sys.windowHeight
  const r = screenW / 750
  const boxW = SCAN_BOX.widthRpx * r
  const boxH = SCAN_BOX.heightRpx * r
  return {
    screenW,
    screenH,
    boxW,
    boxH,
    boxLeft: (screenW - boxW) / 2,
    boxTop: (screenH - boxH) / 2
  }
}

/**
 * 按虚线框 + 用户拖动/缩放 裁剪图片
 * @param {string} imagePath
 * @param {string} canvasId
 * @param {object} transform { imgW, imgH, offsetX, offsetY, totalScale }
 * @param {object} componentInstance vue 组件 this，canvas 导出必需
 */
export function cropScanBox(imagePath, canvasId, transform, componentInstance) {
  return new Promise((resolve, reject) => {
    const { screenW, screenH, boxW, boxH, boxLeft, boxTop } = getScanBoxRect()
    const { imgW, imgH, offsetX, offsetY, totalScale } = transform

    const displayW = imgW * totalScale
    const displayH = imgH * totalScale
    const imageLeft = (screenW - displayW) / 2 + offsetX
    const imageTop = (screenH - displayH) / 2 + offsetY

    let srcX = (boxLeft - imageLeft) / totalScale
    let srcY = (boxTop - imageTop) / totalScale
    let srcW = boxW / totalScale
    let srcH = boxH / totalScale

    srcX = Math.max(0, Math.min(srcX, imgW - 1))
    srcY = Math.max(0, Math.min(srcY, imgH - 1))
    if (srcX + srcW > imgW) srcW = imgW - srcX
    if (srcY + srcH > imgH) srcH = imgH - srcY
    if (srcW < 1 || srcH < 1) {
      reject(new Error('裁剪区域无效，请调整图片位置'))
      return
    }

    const destW = Math.round(boxW)
    const destH = Math.round(boxH)

    const ctx = uni.createCanvasContext(canvasId, componentInstance)
    ctx.drawImage(imagePath, srcX, srcY, srcW, srcH, 0, 0, destW, destH)
    ctx.draw(false, () => {
      setTimeout(() => {
        const timer = setTimeout(() => reject(new Error('canvas 导出超时')), 15000)
        uni.canvasToTempFilePath(
          {
            canvasId,
            x: 0,
            y: 0,
            width: destW,
            height: destH,
            destWidth: destW,
            destHeight: destH,
            fileType: 'jpg',
            quality: 0.92,
            success: (res) => {
              clearTimeout(timer)
              resolve(res.tempFilePath)
            },
            fail: (err) => {
              clearTimeout(timer)
              reject(err || new Error('canvas 导出失败'))
            }
          },
          componentInstance
        )
      }, 500)
    })
  })
}

export function pathToBase64(path) {
  return new Promise((resolve, reject) => {
    uni.getFileSystemManager().readFile({
      filePath: path,
      encoding: 'base64',
      success: (res) => resolve(res.data),
      fail: reject
    })
  })
}

/** 读取图片宽高，失败返回 null */
export function fetchImageSize(path) {
  return new Promise((resolve) => {
    uni.getImageInfo({
      src: path,
      success: (info) => {
        if (info.width > 0 && info.height > 0) {
          resolve({ width: info.width, height: info.height })
        } else {
          resolve(null)
        }
      },
      fail: () => resolve(null)
    })
  })
}
