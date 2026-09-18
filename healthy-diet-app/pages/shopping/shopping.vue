<template>
  <view class="shop-page">
    <!-- 页面头部 -->
    <view class="header">
      <text class="date-title">
        {{ currentDateText }} 食材采购清单
      </text>
    
      <text class="count-text">
        共 {{ showItems.length }} 种食材
      </text>
    </view>

    <!-- 提示未登录 -->
    <view class="tip" v-if="!token">
      请先<text class="login-link" @click="goLogin">登录</text>使用
    </view>

    <!-- 食材卡片 -->
    <block v-else>
      <view class="card">
        <view class="item-list" v-if="showItems.length">
          <view class="item" v-for="(item, idx) in showItems" :key="idx">
            <text class="name">{{ item.name }}</text>
            <text class="qty">{{ item.quantity }}</text>
          </view>
        </view>
        <view class="empty" v-else>
          当天暂无食谱，无法生成食材清单
        </view>
      </view>
    </block>

    <!-- 底部提示 -->
    <view class="footer">
      <text class="tip-text">💡 建议按清单采购，避免遗漏，合理规划更健康！</text>
    </view>
  </view>
</template>

<script>
import { api } from '../../utils/request.js'
import dayjs from 'dayjs'

export default {
  data() {
    return {
      token: '',
      currentDate: '',
      currentDateText: '',
      showItems: [],
    }
  },

  onLoad(options) {
    this.token = uni.getStorageSync('token') || ''
    if (options.date) {
      this.currentDate = options.date
      this.currentDateText = dayjs(options.date).format('MM.DD')
      this.loadTodayIngredients()
    }
  },

  methods: {
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },

    async loadTodayIngredients() {
      if (!this.currentDate || !this.token) {
        this.showItems = []
        return
      }

      try {
        uni.showLoading({ title: '加载中' })

        let todayPlans = await api.getDayPlan(this.currentDate)
        if (!Array.isArray(todayPlans)) todayPlans = []

        if (todayPlans.length === 0) {
          this.showItems = []
          return
        }

        const recipeIds = [...new Set(todayPlans.filter(p => p && p.recipeId).map(p => p.recipeId))]
        if (recipeIds.length === 0) {
          this.showItems = []
          return
        }

        let ingredients = await api.getIngredientsByRecipeIds(recipeIds)
        if (!Array.isArray(ingredients)) ingredients = []

        const map = {}
        ingredients.forEach(item => {
          if (!item || !item.name) return
          map[item.name] = item
        })

        this.showItems = Object.values(map)
      } catch (err) {
        console.error('加载食材失败', err)
        this.showItems = []
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style scoped>
.shop-page {
  background-color: #f5f8f7;
  min-height: 100vh;
  padding: 20rpx;
  box-sizing: border-box;
}

.header{
  padding: 20rpx 10rpx 30rpx;
}

.date-title{
  display:block;
  font-size: 44rpx;
  font-weight: 700;
  color:#2E7D32;
  line-height: 1.4;
}

.count-text{
  display:block;
  margin-top:12rpx;
  font-size:26rpx;
  color:#7A7A7A;
}
.date-text {
  font-size: 24rpx;
  color: #666;
}
.main-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-top: 5rpx;
  color: #1f3d2e;
}
.total-text {
  font-size: 22rpx;
  color: #999;
  margin-top: 5rpx;
}

.tip {
  font-size: 28rpx;
  color: #666;
  text-align: center;
  padding: 60rpx 0;
}
.login-link {
  color: #2e8b57;
  font-weight: bold;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.08);
  margin-bottom: 20rpx;
}

.item-list {
  margin-bottom: 10rpx;
}
.item {
  display: flex;
  justify-content: space-between;
  padding: 15rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.item:last-child {
  border-bottom: none;
}
.name {
  font-size: 28rpx;
  color: #333;
}
.qty {
  font-size: 26rpx;
  color: #2e8b57;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
  font-size: 28rpx;
}

.footer {
  padding: 10rpx 0;
  text-align: center;
}
.tip-text {
  font-size: 24rpx;
  color: #666;
  line-height: 36rpx;
}
</style>