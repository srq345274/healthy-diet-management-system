<template>
  <view class="wrap">
    <!-- 自定义顶部导航栏（你给的样式） -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">设置</view>
    </view>

    <!-- 页面内容 -->
    <view class="content">
      <!-- 第一组设置项 -->
      <view class="group">
        <view class="item">
          <view class="left">
            <text class="icon">😊</text>
            <text class="label">账号与安全</text>
          </view>
          <text class="arrow">></text>
        </view>
        
        <!-- 语言选择（可点击切换） -->
        <view class="item" @click="showLangPicker">
          <view class="left">
            <text class="icon">🌐</text>
            <text class="label">语言</text>
          </view>
          <view class="right">
            <text class="desc">{{ langText }}</text>
            <text class="arrow">⌵</text>
          </view>
        </view>
        
        <view class="item">
          <view class="left">
            <text class="icon">⤴</text>
            <text class="label">分享给朋友</text>
          </view>
          <text class="arrow">></text>
        </view>
        <view class="item">
          <view class="left">
            <text class="icon">💬</text>
            <text class="label">联系我们</text>
          </view>
          <text class="arrow">></text>
        </view>
        <view class="item no-border">
          <view class="left">
            <text class="icon">✎</text>
            <text class="label">写个评价</text>
          </view>
          <text class="arrow">></text>
        </view>
      </view>

      <!-- 第二组设置项 -->
      <view class="group">
        <view class="item">
          <view class="left">
            <text class="icon">🍙</text>
            <text class="label">关于 小饭卡</text>
          </view>
          <text class="arrow">></text>
        </view>
        <view class="item no-border">
          <view class="left">
            <text class="icon">ⓘ</text>
            <text class="label">版本信息</text>
          </view>
          <text class="arrow">></text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      // 默认语言
      lang: 'zh-CN',
      langText: '简体中文'
    }
  },
  onShow() {
    // 读取本地保存的语言设置
    const saveLang = uni.getStorageSync('app-lang') || 'zh-CN'
    this.lang = saveLang
    this.updateLangText()
  },
  methods: {
    // 返回
    goBack() {
      uni.navigateBack()
    },
    // 更新显示文字
    updateLangText() {
      const map = {
        'zh-CN': '简体中文',
        'zh-TW': '繁体中文',
        'en': 'English'
      }
      this.langText = map[this.lang]
    },
    // 打开语言选择弹窗
    showLangPicker() {
      uni.showActionSheet({
        itemList: ['简体中文', '繁体中文', 'English'],
        success: (res) => {
          const list = ['zh-CN', 'zh-TW', 'en']
          this.lang = list[res.tapIndex]
          // 保存选择
          uni.setStorageSync('app-lang', this.lang)
          this.updateLangText()
        }
      })
    }
  }
}
</script>

<style scoped>
/* 你提供的导航栏样式 完全不变 */
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
  padding-bottom: 15rpx;
  z-index: 999;
}
.back-btn {
  position: absolute;
  left: 30rpx;
  top: 110rpx;
}
.back-icon {
  font-size: 40rpx;
  font-weight: bold;
  color: #000;
}
.nav-title {
  font-size: 38rpx;
  font-weight: bold;
  color: #000;
}

/* 页面布局 */
.wrap {
  background-color: #fff;
  min-height: 100vh;
}
.content {
  padding-top: 200rpx;
  padding-bottom: 40rpx;
}

/* 设置项样式 */
.group {
  background-color: #f7f7f7;
  border-radius: 20rpx;
  padding: 0 20rpx;
  margin: 30rpx;
}
.item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx 10rpx;
  border-bottom: 1rpx solid #e8e8e8;
}
.item.no-border {
  border-bottom: none;
}
.left {
  display: flex;
  align-items: center;
}
.icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}
.label {
  font-size: 32rpx;
  color: #000;
}
.right {
  display: flex;
  align-items: center;
}
.desc {
  font-size: 28rpx;
  color: #999;
  margin-right: 10rpx;
}
.arrow {
  font-size: 32rpx;
  color: #999;
}
</style>