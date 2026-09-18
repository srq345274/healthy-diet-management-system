<template>
  <view class="my-page">
    <view class="header">
      <view class="avatar-wrap">
        <view class="avatar" @click="goProfileInfo" @error="handleAvatarError">
          <template v-if="avatarUrl">
            <image :src="avatarUrl" class="avatar-img" mode="aspectFill" />
          </template>
          <template v-else>
            <text class="avatar-placeholder">{{ nickname ? nickname.charAt(0) : '未登录' }}</text>
          </template>
        </view>

        <view class="login-tip" v-if="!nickname" @click="goLogin">
          <text>点击登录</text>
          <text class="arrow">></text>
        </view>
        <view class="nickname" v-else>
          <text>{{ nickname }}</text>
        </view>
      </view>
    </view>

    <view class="service-list">
      <view class="service-item" @click="goMealplan">
        <view class="icon-wrap">🍽️</view>
        <text class="item-text">我的食谱</text>
        <text class="arrow">></text>
      </view>
      
      <view class="service-item" @click="goProfile">
        <view class="icon-wrap">🩺</view>
        <text class="item-text">健康档案</text>
        <text class="arrow">></text>
      </view>

      <view class="service-item" @click="goSetting">
        <view class="icon-wrap">⚙️</view>
        <text class="item-text">设置</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="logout-btn-wrap" v-if="nickname">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>

    <!-- 底部tab：当前页【我的】加粗，统计跳数据、中间跳首页 -->
    <view class="tabbar" style="position:fixed;bottom:0;left:0;right:0;background:#fff;z-index:9999;">
      <view class="tab-item" @click="goStatistics">
        <text class="tab-text">数据统计</text>
      </view>
      <view class="tab-item center" @click="goHome">
        <text class="tab-text">首页</text>
      </view>
      <view class="tab-item">
        <text class="tab-text bold">我的</text>
      </view>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../../utils/config.js'

export default {
  data() {
    return {
      nickname: '',
      avatarUrl: '/static/default-avatar.png', // 默认头像
    }
  },

  onShow() {
    this.fetchUserInfo()
  },

  methods: {
    // 获取用户信息
    async fetchUserInfo() {
      const token = uni.getStorageSync('token')
      if (!token) {
        this.nickname = ''
        this.avatarUrl = '/static/default-avatar.png'
        return
      }

      uni.request({
        url: API_BASE + '/api/user/info', // 已加入白名单，不走 JWT
        method: 'GET',
        header: { token }, // 直接用数据库 token
        success: (res) => {
          if (res.data.code === 0 && res.data.data) {
            this.nickname = res.data.data.nickname || '未登录'
            this.avatarUrl = res.data.data.avatar || '/static/default-avatar.png'
          } else {
            this.nickname = ''
            this.avatarUrl = '/static/default-avatar.png'
          }
        },
        fail: () => {
          this.nickname = ''
          this.avatarUrl = '/static/default-avatar.png'
        }
      })
    },

    // 页面跳转
    goHome() {
      uni.navigateTo({ url: '/pages/index/index' })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    // =====================
    // 退出登录
    // =====================
    handleLogout() {
      const token = uni.getStorageSync('token')
      if (token) {
        uni.removeStorageSync('avatar_' + token)
      }
    
      uni.removeStorageSync('token')
      this.nickname = ''
      this.avatarUrl = '/static/default-avatar.png'
      uni.showToast({ title: '已退出', icon: 'none' })
    },
    goProfileInfo() {
      if (!this.nickname) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/index/my/profile-info/profile-info' })
    },
    goMealplan() {
      if (!this.nickname) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/meal-plan/meal-plan' })
    },
    goProfile() {
      if (!this.nickname) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/profile/profile' })
    },
    goSetting() {
      uni.navigateTo({ url: '/pages/index/my/setting/setting' })
    },
    goStatistics() {
      if (!this.nickname) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/statistics/statistics' })
    }
  }
}
</script>

<style>
.my-page {
  background-color: #fff;
  min-height: 100vh;
  padding: 0;
  padding-bottom:160rpx;
}
.header {
  background-color: #faf5e3;
  padding: 240rpx 40rpx 60rpx;
}
.avatar-wrap {
  display: flex;
  align-items: center;
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background-color: #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 30rpx;
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-placeholder {
  font-size: 32rpx;
  color: #333;
}
.login-tip {
  display: flex;
  align-items: center;
  color: #000;
  font-size: 32rpx;
  padding: 10rpx 0;
}
.arrow {
  margin-left: 20rpx;
  color: #999;
}
.nickname {
  font-size: 36rpx;
  color: #000;
  font-weight: 500;
}
.service-list {
  margin: 40rpx;
  background-color: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  padding-bottom: 40rpx;
}
.service-item {
  display: flex;
  align-items: center;
  padding: 30rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.service-item:last-child {
  border-bottom: none;
}
.icon-wrap {
  font-size: 36rpx;
  margin-right: 20rpx;
}
.item-text {
  flex: 1;
  font-size: 32rpx;
  color: #333;
}
.logout-btn-wrap {
  margin: 80rpx 40rpx 40rpx 40rpx;
}
.logout-btn {
  width: 100%;
  height: 100rpx;
  background-color: #000;
  color: #fff;
  border-radius: 50rpx;
  font-size: 31rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
/*tab样式照搬*/
.tabbar {
  height: 160rpx;
  background-color: #fff;
  border-top: 1rpx solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-bottom: 20rpx;
  box-sizing: border-box;
}
.tab-item { flex:1;display:flex;align-items:center;justify-content:center;height:100%; }
.tab-text {
  font-size: 26rpx;
  color: #333;
}
.tab-text.bold { font-weight:bold;color:#000; }
</style>