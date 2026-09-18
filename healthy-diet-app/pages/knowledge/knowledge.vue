<template>
  <!-- 最外层：页面包装器【导航完全不动】 -->
  <view class="page-wrapper">

    <!-- 自定义顶部导航栏【原封不动】 -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">AI助手</view>
    </view>

    <!-- 可滚动内容 -->
    <scroll-view class="content" scroll-y="true" :scroll-top="scrollTop">
      <view class="box">

        <!-- 1. 时令饮食（可折叠） -->
        <view class="card">
          <view class="collapse-header" @click="showSeason = !showSeason">
            <text class="card-title">🍃 {{ currentMonth }}月时令饮食建议</text>
            <text class="arrow">{{ showSeason ? '▲' : '▼' }}</text>
          </view>
          <view v-if="showSeason" class="collapse-body">
            <view class="ans-block" v-if="seasonAns">
              <text class="ans">{{ seasonAns }}</text>
            </view>
            <view class="loading" v-else>
              <text>加载当月饮食建议中…</text>
            </view>
          </view>
        </view>

        <!-- 2. AI 对话区域 -->
        <view class="chat-container">
          <!-- 聊天记录 -->
          <view class="chat-list">
            <block v-for="(item, idx) in chatList" :key="idx">
              <view class="chat-item me">
                <view class="msg">{{ item.q }}</view>
              </view>
              <view class="chat-item ai" v-if="item.a">
                <view class="msg">{{ item.a }}</view>
              </view>
            </block>
            <!-- AI加载占位 -->
            <view v-if="isLoading" class="chat-item ai">
              <view class="msg">...</view>
            </view>
          </view>
        </view>

      </view>
    </scroll-view>

    <!-- 空状态提示：固定屏幕居中 -->
    <view class="empty-tip" v-if="chatList.length === 0 && !isLoading">
      <text>💬 向AI饮食助手提问吧</text>
    </view>

    <!-- 固定在底部：快捷标签 + 输入框 + 按钮 -->
    <view class="footer-input">
      <view class="quick-row">
        <scroll-view class="quick-scroll" scroll-x>
          <view class="quick-wrap">
            <text class="quick-tag" v-for="item in showQuickList" :key="item" @click="fillQuick(item)" :class="{disabled:isLoading}">{{ item }}</text>
          </view>
        </scroll-view>
        <text class="refresh" @click="refreshQuick" :class="{disabled:isLoading}">换一换</text>
      </view>

      <textarea class="area" v-model="q" placeholder="输入你的问题..." :disabled="isLoading"></textarea>
      <button class="btn-primary black" @click="askAi" :disabled="isLoading">{{ isLoading ? '请求中...' : '发送' }}</button>
    </view>

  </view>
</template>

<script>
import { api } from '../../utils/request.js'
export default {
  data() {
    return {
      q: '',
      seasonAns: '',
      currentMonth: new Date().getMonth() + 1,
      showSeason: true,
      chatList: [],
      scrollTop: 0,
      isLoading: false, // 请求锁
      quickGroups: [
        ['查询鸡蛋营养','减脂吃什么','熬夜吃什么'],
        ['控糖怎么吃','补钙食物','去湿气饮食'],
        ['养胃吃什么','便秘吃什么','增强免疫力'],
        ['降血压饮食','贫血怎么补','儿童长高食谱'],
        ['美容养颜食物','祛湿排毒','清淡饮食推荐'],
        ['蛋白质食物','低热量食材','养胃粥做法']
      ],
      showQuickList: [],
      groupIndex: 0
    }
  },
  onShow() {
    this.showQuickList = this.quickGroups[0]
    this.loadAutoSeasonAdvice()
  },
  methods: {
    goBack() { uni.navigateBack() },
    fillQuick(text) {
      if(this.isLoading) return
      this.q = text
    },
    refreshQuick() {
      if(this.isLoading) return
      this.groupIndex = (this.groupIndex + 1) % this.quickGroups.length
      this.showQuickList = this.quickGroups[this.groupIndex]
    },
    async loadAutoSeasonAdvice() {
      const token = uni.getStorageSync('token')
      if (!token) return
      try {
        const month = this.currentMonth
        const prompt = `现在是${month}月，请结合气候、节气给出详实饮食建议，300字内，连贯段落，不分点。`
        const data = await api.aiAdvice(prompt, true)
        this.seasonAns = data.answer || '暂无当月建议'
      } catch (e) {
        this.seasonAns = '加载失败，请重试'
      }
    },
    async askAi() {
      if(this.isLoading) return
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      const inputTxt = this.q.trim()
      if (!inputTxt) return

      // 1、立刻插入用户提问，马上显示出来
      this.chatList.push({ q: inputTxt, a: '' })
      this.q = ''
      this.isLoading = true
      // 滚到底
      this.$nextTick(() => { this.scrollTop = 99999 })

      let prompt = inputTxt + '，专业简洁，不要markdown。'
      try {
        const res = await api.aiAdvice(prompt, true)
        const answer = res.answer || '暂无回答'
        // 接口回来回填AI内容
        this.chatList[this.chatList.length - 1].a = answer
      } catch (e) {
        this.chatList[this.chatList.length - 1].a = '请求失败，请重试'
        uni.showToast({ title: '请求失败', icon: 'none' })
      } finally {
        this.isLoading = false
        this.$nextTick(() => { this.scrollTop = 99999 })
      }
    }
  }
}
</script>

<style scoped>
/* 原版导航不动 */
.page-wrapper {
  width: 100%;
  height: 100vh;
  background: #ffffff;
  position: relative;
}
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

.content {
  height: calc(100vh - 160rpx);
  margin-top: 160rpx;
  padding-bottom: 360rpx;
  box-sizing: border-box;
}
.box {
  padding: 24rpx;
}

.card {
  background: #fff;
  padding: 40rpx;
  border-radius: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
}
.card-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 0;
  display: block;
}
.collapse-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.arrow {
  font-size: 28rpx;
  color: #666;
}
.collapse-body {
  margin-top: 20rpx;
}

.empty-tip {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
  font-size: 34rpx;
  z-index: 10;
}

.chat-container {
  padding-bottom: 20rpx;
}
.chat-list {
  width: 100%;
}
.chat-item {
  display: flex;
  margin-bottom: 20rpx;
}
.chat-item.me {
  justify-content: flex-end;
}
.chat-item.ai {
  justify-content: flex-start;
}
.msg {
  max-width: 78%;
  padding: 20rpx 26rpx;
  border-radius: 20rpx;
  font-size: 27rpx;
  line-height: 1.6;
}
.me .msg {
  background: #000;
  color: #fff;
  border-bottom-right-radius: 8rpx;
}
.ai .msg {
  background: #f2f2f2;
  color: #333;
  border-bottom-left-radius: 8rpx;
}

/* 底部输入区 */
.footer-input {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background: #fff;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.05);
  z-index: 99;
}

.quick-row {
  display: flex;
  align-items: center;
  gap:20rpx;
  margin-bottom: 16rpx;
}
.quick-scroll{
  flex:1;
}
.quick-wrap {
  display: flex;
  gap:12rpx;
  white-space: nowrap;
}
.quick-tag {
  padding: 10rpx 20rpx;
  background: #f5f5f5;
  border-radius: 50rpx;
  font-size: 24rpx;
  color: #333;
  flex-shrink:0;
}
.refresh {
  font-size: 24rpx;
  color: #666;
  flex-shrink:0;
}
.disabled{
  opacity:0.4;
}

/* 输入框 */
.area {
  width: 100%;
  min-height: 100rpx !important;
  max-height: 140rpx !important;
  background: #f7f7f7;
  border-radius: 20rpx !important;
  padding: 20rpx 24rpx;
  font-size: 27rpx;
  box-sizing: border-box;
  border: none;
  margin-bottom: 16rpx;
}
.area:disabled{
  background:#eee;
}
.btn-primary {
  width: 100% !important;
  height: 80rpx !important;
  line-height: 80rpx !important;
  background: #000000 !important;
  border-radius: 50rpx !important;
  font-size: 27rpx !important;
  border: none !important;
  color: #fff !important;
}
.btn-primary:disabled{
  background:#666 !important;
}

.ans {
  font-size: 26rpx;
  color: #333;
  line-height: 1.7;
}
.loading {
  color: #999;
  font-size: 26rpx;
}
</style>