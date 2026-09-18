<template>
  <view class="container">
    <camera
      v-if="!showImg"
      class="full-camera"
      device-position="back"
      flash="off"
      :stopped="isStoppedCamera"
    ></camera>

    <movable-area v-if="showImg" class="mv-area">
      <movable-view
        class="mv-box"
        direction="all"
        :inertia="false"
        :animation="false"
        :x="mvX"
        :y="mvY"
        :style="{ width: mvAreaW + 'px', height: mvAreaH + 'px' }"
        @change="onMvChange"
      >
        <image
          :key="imageKey"
          class="preview-img"
          :src="imgPath"
          mode="aspectFit"
          :style="{ transform: 'scale(' + totalScale + ')' }"
          @load="onImgLoad"
          @error="onImgError"
        ></image>
      </movable-view>
    </movable-area>

    <view class="scan-box"></view>
    <view class="scan-tip">
      {{ showImg ? '拖动图片对准虚线框，右侧按钮缩放，再点 √' : '请将食物置于框内识别' }}
    </view>

    <view v-if="showImg" class="zoom-bar">
      <view class="zoom-btn" hover-class="zoom-btn-hover" @tap="zoomOut">－</view>
      <text class="zoom-text">{{ Math.round(userScale * 100) }}%</text>
      <view class="zoom-btn" hover-class="zoom-btn-hover" @tap="zoomIn">＋</view>
    </view>

    <view v-if="processing" class="loading-mask">
      <view class="loading-text">正在识别与抠图...</view>
    </view>

    <canvas
      canvas-id="cropCanvas"
      class="crop-canvas"
      :style="{ width: canvasW + 'px', height: canvasH + 'px' }"
    ></canvas>

    <view class="bottom-bg-box">
      <view class="btn-wrap">
        <view class="btn white-btn" hover-class="btn-hover" @tap="openAlbum">相册</view>
        <view class="btn black-btn" hover-class="btn-hover" @tap="confirmFood">√</view>
        <view class="btn white-btn" hover-class="btn-hover" @tap="closePopup">✕</view>
      </view>
    </view>
  </view>
</template>

<script>
import {
  cropScanBox,
  pathToBase64,
  SCAN_BOX,
  fetchImageSize
} from '../../../../utils/imageCrop.js'
import { api } from '../../../../utils/request.js'

export default {
  data() {
    return {
      groupId: '',
      groupName: '',
      targetKcal: 500,
      imgPath: '',
      imageKey: 0,
      showImg: false,
      imgReady: false,
      isProcessing: false,
      processing: false,
      isStoppedCamera: false,
      canvasW: 300,
      canvasH: 340,
      imgW: 0,
      imgH: 0,
      fitScale: 1,
      userScale: 1,
      mvX: 0,
      mvY: 0,
      mvAreaW: 0,
      mvAreaH: 0,
      screenW: 375,
      screenH: 667
    }
  },
  computed: {
    totalScale() {
      return this.fitScale * this.userScale
    }
  },
  onLoad(options) {
    this.groupId = options.groupId || ''
    this.groupName = options.groupName || ''
    this.targetKcal = Number(options.targetKcal) || 500
    const sys = uni.getSystemInfoSync()
    this.screenW = sys.windowWidth
    this.screenH = sys.windowHeight
    this.mvAreaW = this.screenW
    this.mvAreaH = this.screenH
    const r = sys.windowWidth / 750
    this.canvasW = Math.round(SCAN_BOX.widthRpx * r)
    this.canvasH = Math.round(SCAN_BOX.heightRpx * r)
  },
  methods: {
    resetTransform() {
      this.userScale = 1
      this.mvX = 0
      this.mvY = 0
    },
    resetImageState() {
      this.imgW = 0
      this.imgH = 0
      this.fitScale = 1
      this.imgReady = false
      this.resetTransform()
    },
    applyImageSize(w, h) {
      if (!w || !h) return false
      this.imgW = w
      this.imgH = h
      this.fitScale = Math.min(this.screenW / w, this.screenH / h)
      this.imgReady = true
      this.resetTransform()
      return true
    },
    async resolveImageSize(path) {
      const info = await fetchImageSize(path)
      if (info) {
        this.userScale = 1
        this.applyImageSize(info.width, info.height)
        return true
      }
      return false
    },
    onImgLoad(e) {
      const w = e.detail && e.detail.width
      const h = e.detail && e.detail.height
      if (w > 0 && h > 0 && !this.imgReady) {
        this.userScale = 1
        this.applyImageSize(w, h)
      }
    },
    onImgError() {
      uni.showToast({ title: '图片显示失败，请重选', icon: 'none' })
      this.showImg = false
      this.isStoppedCamera = false
      this.resetImageState()
    },
    async setPreviewImage(path) {
      if (!path) return
      this.resetImageState()
      this.imageKey += 1
      this.imgPath = path
      this.showImg = true
      this.isStoppedCamera = true
      await this.resolveImageSize(path)
    },
    onMvChange(e) {
      const d = e.detail || {}
      if (typeof d.x === 'number') this.mvX = d.x
      if (typeof d.y === 'number') this.mvY = d.y
    },
    applyZoom(delta) {
      this.userScale = Math.max(0.5, Math.min(this.userScale + delta, 3))
    },
    zoomIn() { this.applyZoom(0.2) },
    zoomOut() { this.applyZoom(-0.2) },
    getCropTransform() {
      return {
        imgW: this.imgW,
        imgH: this.imgH,
        offsetX: this.mvX,
        offsetY: this.mvY,
        totalScale: this.totalScale
      }
    },
    openAlbum() {
      uni.chooseImage({
        count: 1,
        sizeType: ['original', 'compressed'],
        sourceType: ['album'],
        success: async (res) => {
          try {
            await this.setPreviewImage(res.tempFilePaths[0])
          } catch {
            uni.showToast({ title: '图片加载失败', icon: 'none' })
          }
        }
      })
    },
    async processAndNavigate(imagePath) {
      if (this.processing) return
      this.processing = true
      uni.showLoading({ title: '处理中...', mask: true })
      try {
        const croppedPath = await cropScanBox(
          imagePath,
          'cropCanvas',
          this.getCropTransform(),
          this
        )
        const base64 = await pathToBase64(croppedPath)
        const result = await api.processFood(base64)
        const cacheKey = 'foodResult_' + Date.now()
        uni.setStorageSync(cacheKey, { ...result, listId: this.groupId, groupId: this.groupId, groupName: this.groupName, targetKcal: this.targetKcal })
        uni.navigateTo({ url: `/pages/diet-record/detail-record/food-result/food-result?key=${cacheKey}` })
      } catch (e) {
        uni.showModal({ title: '识别失败', content: e.message || '识别失败', showCancel: false })
      } finally {
        uni.hideLoading()
        this.processing = false
      }
    },
    confirmFood() {
      if (this.showImg && this.imgPath) {
        this.processAndNavigate(this.imgPath)
        return
      }
      const camera = uni.createCameraContext()
      camera.takePhoto({
        quality: 'high',
        success: async (res) => await this.setPreviewImage(res.tempImagePath)
      })
    },
    closePopup() {
      this.isStoppedCamera = false
      this.showImg = false
      this.imgPath = ''
      this.resetImageState()
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>

/* 原样保留你的样式 */

.container {

  width: 100%;

  height: 100vh;

  overflow: hidden;

  position: relative;

  background: #111;

}

.full-camera {

  width: 100%;

  height: 100%;

  position: absolute;

  z-index: 1;

}

.mv-area {

  position: absolute;

  left: 0;

  top: 0;

  width: 100%;

  height: 100%;

  z-index: 5;

  overflow: hidden;

}

.mv-box {

  display: block;

}

.preview-img {

  width: 100%;

  height: 100%;

  display: block;

}

.crop-canvas {

  position: fixed;

  left: 0;

  top: 0;

  opacity: 0.01;

  pointer-events: none;

  z-index: 0;

}

.scan-box {

  position: absolute;

  left: 50%;

  top: 50%;

  width: 480rpx;

  height: 540rpx;

  transform: translate(-50%, -50%);

  border: 6rpx dashed #fff;

  border-radius: 24rpx;

  z-index: 10;

  pointer-events: none;

  background: transparent;

  box-shadow: 0 0 8rpx rgba(0, 0, 0, 0.35);

}

.scan-tip {

  position: absolute;

  left: 50%;

  top: calc(50% + 300rpx);

  transform: translateX(-50%);

  color: #fff;

  font-size: 24rpx;

  z-index: 11;

  text-align: center;

  width: 90%;

  pointer-events: none;

}

.zoom-bar {

  position: absolute;

  right: 24rpx;

  top: 40%;

  z-index: 1001;

  display: flex;

  flex-direction: column;

  align-items: center;

  gap: 16rpx;

  background: rgba(255, 255, 255, 0.92);

  border-radius: 40rpx;

  padding: 20rpx 16rpx;

}

.zoom-btn {

  width: 64rpx;

  height: 64rpx;

  line-height: 64rpx;

  border-radius: 50%;

  background: #1e1e1e;

  color: #fff;

  font-size: 36rpx;

  text-align: center;

}

.zoom-btn-hover {

  opacity: 0.75;

}

.zoom-text {

  font-size: 22rpx;

  color: #333;

}

.loading-mask {

  position: absolute;

  inset: 0;

  z-index: 100;

  background: rgba(0, 0, 0, 0.45);

  display: flex;

  align-items: center;

  justify-content: center;

}

.loading-text {

  color: #fff;

  font-size: 30rpx;

}

.bottom-bg-box {

  position: absolute;

  bottom: 0;

  left: 0;

  width: 100%;

  height: 20vh;

  background: rgba(255, 255, 255, 0.75);

  border-top-left-radius: 80rpx;

  border-top-right-radius: 80rpx;

  z-index: 999;

  display: flex;

  align-items: center;

  justify-content: center;

}

.btn-wrap {

  display: flex;

  align-items: center;

  gap: 120rpx;

}

.btn {

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 50%;

}

.btn-hover {

  opacity: 0.85;

}

.white-btn {

  width: 90rpx;

  height: 90rpx;

  background: #fff;

  font-size: 36rpx;

  font-weight: bold;

  color: #000;

}

.black-btn {

  width: 160rpx;

  height: 160rpx;

  background: #1e1e1e;

  color: #fff;

  font-size: 50rpx;

  font-weight: bold;

}

</style>