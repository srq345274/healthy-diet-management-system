<template>
  <view class="page">

    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        ←
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area">
      <!-- 食物贴纸区域 -->
      <view class="sticker-section">
        <view class="sticker-frame">
          <image
            v-if="stickerSrc"
            class="sticker-img"
            :src="stickerSrc"
            mode="aspectFit"
          ></image>
        </view>
      </view>

      <!-- 菜名与热量 -->
      <view class="title-section">
        <input
          v-if="editingName"
          class="name-input"
          v-model="dishName"
          focus
          @blur="editingName = false"
          @confirm="editingName = false"
        />
        <text v-else class="dish-name">{{ dishName }}</text>

        <!-- 新增：显示食物分类 -->
        <text class="food-type">{{ category }}</text>

        <text class="calorie">{{ calories }} kcal</text>
      </view>

      <!-- 营养网格 -->
      <view class="nutrition-grid">
        <view class="nut-item" v-for="item in nutritionList" :key="item.label">
          <text class="nut-label">{{ item.label }}</text>
          <text class="nut-value">{{ item.value }}</text>
        </view>
      </view>

      <!-- Tips 卡片 -->
      <view class="tips-card">
        <text class="tips-title">Tips</text>
        <text class="tips-body">{{ tips }}</text>
        <view class="tips-footer">
          <text class="tips-ai">参考 WHO & USDA</text>
        </view>
      </view>

      <text class="timestamp">{{ timestamp }}</text>
      <view class="scroll-bottom-space"></view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-btn white-circle" @click="cancel">✕</view>
      <view class="bar-btn black-oval" @click="confirmSave">✓</view>
      <view class="bar-btn white-circle" @click="startEditName">✎</view>
    </view>
  </view>
</template>

<script>
import { api } from '../../../../utils/request.js'

export default {
  data() {
    return {
      groupId: '',
      groupName: '',
      targetKcal: 500,
      stickerBase64: '',
      dishName: '',
      category: '食物', // 新增
      calories: 0,
      carbG: 0,
      proteinG: 0,
      fatG: 0,
      fiberG: 0,
      sugarG: 0,
      sodiumMg: 0,
      tips: '',
      editingName: false,
      timestamp: '',
      listId: null // 需要在页面或父页面传入今天的listId
    }
  },
  computed: {
    stickerSrc() {
      if (!this.stickerBase64) return ''
      return this.stickerBase64.startsWith('data:')
        ? this.stickerBase64
        : 'data:image/png;base64,' + this.stickerBase64
    },
    nutritionList() {
      return [
        { label: '碳水', value: this.fmt(this.carbG, 'g') },
        { label: '蛋白质', value: this.fmt(this.proteinG, 'g') },
        { label: '脂肪', value: this.fmt(this.fatG, 'g') },
        { label: '纤维', value: this.fmt(this.fiberG, 'g') },
        { label: '糖', value: this.fmt(this.sugarG, 'g') },
        { label: '盐', value: this.fmt(this.sodiumMg, 'mg') }
      ]
    }
  },
  async onLoad(options) {
    const key = options.key
    if (key) {
      try {
        const data = uni.getStorageSync(key)
        if (data) {
          this.groupId = data.groupId || ''
          this.groupName = data.groupName || ''
          this.targetKcal = data.targetKcal || 500
          this.stickerBase64 = data.stickerBase64 || ''
          this.dishName = data.dishName || '未知菜品'
          this.category = data.category || '食物' // 新增
          this.calories = data.calories || 0
          this.carbG = data.carbG || 0
          this.proteinG = data.proteinG || 0
          this.fatG = data.fatG || 0
          this.fiberG = data.fiberG || 0
          this.sugarG = data.sugarG || 0
          this.sodiumMg = data.sodiumMg || 0
          this.tips = data.tips || ''
          this.listId = data.listId || null
          uni.removeStorageSync(key)
        }
      } catch (e) {
        uni.showToast({ title: '数据加载失败', icon: 'none' })
      }
    }

    const now = new Date()
    const pad = (n) => (n < 10 ? '0' + n : '' + n)
    this.timestamp = `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
  },
  methods: {
    fmt(val, unit) {
      const n = Number(val)
      if (isNaN(n)) return '--'
      return n.toFixed(1) + ' ' + unit
    },
    startEditName() { this.editingName = true },
    goBack() { uni.navigateBack() },
    cancel() { uni.navigateBack({ delta: 2 }) },

    async confirmSave() {
      const token = uni.getStorageSync('token')
    
      if (!token) {
        uni.showToast({
          title: '请先登录',
          icon: 'none'
        })
        return
      }
    
      if (!this.listId) {
        uni.showToast({
          title: '未找到清单',
          icon: 'none'
        })
        return
      }
    
      try {
        uni.showLoading({
          title: '保存中...'
        })
    
        console.log(
          '图片大小KB=',
          Math.round((this.stickerBase64?.length || 0) / 1024)
        )
    
        await api.saveDietRecord({
          listId: this.listId,
    
          dishName: this.dishName,
          category: this.category,
    
          imageBase64: this.stickerBase64,
    
          calories: this.calories,
    
          carbG: this.carbG,
          proteinG: this.proteinG,
          fatG: this.fatG,
          fiberG: this.fiberG,
          sugarG: this.sugarG,
          sodiumMg: this.sodiumMg,
    
          tips: this.tips
        })
    
        uni.hideLoading()
    
        uni.showToast({
          title: '记录成功',
          icon: 'success'
        })
    
        setTimeout(() => {
          uni.navigateBack({
            delta: 2
          })
        }, 1000)
    
      } catch (e) {
        uni.hideLoading()
    
        console.error('保存失败=', e)
    
        uni.showToast({
          title: e.message || '保存失败',
          icon: 'none'
        })
      }
    }
  }
}
</script>
<style scoped>
.page {
  min-height: 100vh;
  background: #f3efe6;
  position: relative;
  padding-top: 180rpx; /* 给导航栏留出空间 */
}

.scroll-area {
  height: calc(100vh - 180rpx);
  box-sizing: border-box;
  padding-top: 20rpx;
  padding-bottom: 260rpx;
}

.sticker-section {
  display: flex;
  justify-content: center;
  padding: 20rpx 0 20rpx;
}

.sticker-frame {
  width: 420rpx;
  height: 420rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx dashed rgba(255,255,255,0.9);
  border-radius: 48rpx;
  background: rgba(255,255,255,0.25);
  clip-path: polygon(15% 0%,85% 0%,100% 15%,100% 85%,85% 100%,15% 100%,0% 85%,0% 15%);
}

.sticker-img {
  width: 360rpx;
  height: 360rpx;
}

.title-section {
  text-align: center;
  padding: 24rpx 40rpx 16rpx;
}

.dish-name {
  display: block;
  font-size: 52rpx;
  font-weight: 600;
  color: #3a3a3a;
  letter-spacing: 2rpx;
}

.name-input {
  display: block;
  width: 100%;
  text-align: center;
  font-size: 48rpx;
  font-weight: 600;
  color: #3a3a3a;
  border-bottom: 2rpx solid #ccc;
  padding: 8rpx 0;
}

.food-type {
  display:block;
  margin-top:10rpx;
  font-size:28rpx;
  color:#8a8a8a;
}

.calorie {
  display: block;
  margin-top: 12rpx;
  font-size: 32rpx;
  color: #888;
}

.nutrition-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 48rpx;
  gap: 0;
}

.nut-item {
  width: 33.33%;
  text-align: center;
  padding: 20rpx 0;
}

.nut-label {
  display: block;
  font-size: 24rpx;
  color: #aaa;
  margin-bottom: 8rpx;
}

.nut-value {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #3a3a3a;
}

.tips-card {
  margin:20rpx 40rpx;
  padding:40rpx;
  background:#fff;
  border-radius:36rpx;
  box-shadow:0 8rpx 30rpx rgba(0,0,0,.05);
}

.tips-title {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.tips-body {
  display: block;
  color:#555;
  line-height:1.9;
  font-size:28rpx;
  white-space:pre-wrap;
}

.tips-footer {
  margin-top: 24rpx;
}

.tips-ai {
  font-size: 22rpx;
  color: #bbb;
}

.timestamp {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #bbb;
  margin-top: 24rpx;
}

.scroll-bottom-space {
  height: 260rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 180rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80rpx;
  background: linear-gradient(to top, #f3efe6 60%, transparent);
  padding-bottom: env(safe-area-inset-bottom);
}

.bar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
}

.white-circle {
  width: 88rpx;
  height: 88rpx;
  background: #fff;
  border-radius: 50%;
  font-size: 36rpx;
  color: #333;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
}

.black-oval {
  width: 160rpx;
  height: 100rpx;
  background: #1a1a1a;
  border-radius: 50rpx;
  color: #fff;
  font-size: 44rpx;
  font-weight: bold;
}

.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  padding-top: 48rpx; /* 增加顶部安全区留白 */
  background: #f3efe6;
  display: flex;
  align-items: center;
  padding-left: 30rpx;
}

.back-btn {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42rpx;
  color: #333;
}
</style>