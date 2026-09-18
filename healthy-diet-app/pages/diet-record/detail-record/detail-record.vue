<template>
  <view class="page-wrapper">
    <!-- 自定义顶部导航栏 -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>

      <view class="nav-title">{{ groupName }}</view>
      <view class="nav-date">{{ formatDate(createDate) }}</view>
    </view>

    <view class="box">
      <view class="nine-dot-box">
        <view
          class="dot-item"
          v-for="(item, idx) in 12"
          :key="idx"
        >
      
          <image
            v-if="foods[idx] && foods[idx].image"
            class="food-img"
            :src="
              foods[idx].image.startsWith('data:')
                ? foods[idx].image
                : 'data:image/png;base64,' + foods[idx].image
            "
            mode="aspectFit"
          />
      
          <view
            v-else
            class="gray-dot"
          ></view>
      
        </view>
      </view>

      <!-- 白色全屏圆角背景 -->
      <view class="white-bottom-card">

        <!-- 第一个灰色框 -->
        <view class="gray-box">
          <view class="bottom-content">
            <view class="left-text">
              <view class="line1">{{ groupName }}已摄入</view>
              <view class="line2">
                {{ (foodNum || drinkNum || 0) === 0 ? '还没吃' : `${foodNum || 0}食物 ${drinkNum || 0}饮料` }}
              </view>
            </view>
            <view class="right-content">
              <view class="right-text">{{ currentKcal || 0 }}/{{ targetKcal || 500 }} Kcal</view>
              <view class="progress-bar">
                <view class="progress" :style="{ width: getProgressWidth(currentKcal, targetKcal), backgroundColor: getProgressColor(currentKcal, targetKcal) }"></view>
              </view>
            </view>
          </view>
        </view>

        <!-- 第二个灰色框 -->
        <view class="gray-box">
          <view class="bottom-content">
            <view class="left-text">
              <view class="line1">占全天摄入计划</view>
              <view class="line2">配额正常，放心吃</view>
            </view>
            <view class="right-content">
              <view class="right-text">{{ allDayPercent }}%</view>
              <view class="progress-bar">
                <view class="progress" :style="{ width: allDayPercent + '%', backgroundColor: '#9ae471' }"></view>
              </view>
            </view>
          </view>
        </view>

      </view>

      <!-- 底部按钮 -->
      <view class="btn-fixed-wrap">
        <view class="btn-left" @click="changeDietList">切换清单</view>
        <view class="btn-right" @click="goAddDiet">记录饮食</view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '../../../utils/request.js'

export default {
  data() {
    return {
      groupId: '',
      groupName: '',
      createDate: '',
      targetKcal: 500,
      currentKcal: 0,
      foodNum: 0,
      drinkNum: 0,
      allDayTotal: 2000,
      token: '',
      foods: [],
      firstLoad: true
    }
  },

  computed: {
    allDayPercent() {
      let res = (this.currentKcal / this.allDayTotal) * 100
      return res > 100 ? 100 : parseInt(res)
    }
  },

  onLoad(options) {
    this.groupId = options.id || ''
    this.createDate = options.createDate || ''
    this.token = uni.getStorageSync('token') || ''

    console.log('groupId=', this.groupId)
    console.log('createDate=', this.createDate)

    this.loadDetail()
  },

  onShow() {
    if (this.firstLoad) {
      this.firstLoad = false
      return
    }

    console.log('页面返回，重新刷新详情')

    this.loadDetail()
  },

  methods: {
    goBack() {
      const pages = getCurrentPages()
    
      // 找首页
      const indexPage = pages.find(
        p => p.route === 'pages/index/index'
      )
    
      if (indexPage) {
        indexPage.$vm.selectDate = this.createDate
        indexPage.$vm.loadGroups()
      }
    
      uni.navigateBack()
    },

    formatDate(dateStr) {
      if (!dateStr) return '未知日期'

      const arr = dateStr.split('-')

      if (arr.length < 3) return dateStr

      return arr[0] + '年' + arr[1] + '月' + arr[2] + '日'
    },

    async loadDetail() {
      try {
        let url = `/food-list/detail?id=${this.groupId}`

        if (this.createDate) {
          url += `&date=${this.createDate}`
        }

        console.log('请求地址=', url)

        const res = await request({
          url,
          method: 'GET',
          header: {
            token: this.token
          }
        })

        console.log('详情返回=', res)

        this.groupName = res.mealType || ''
        this.targetKcal = res.targetKcal || 500
        this.currentKcal = res.currentKcal || 0

        this.foods = res.foods || []

        this.foodNum = this.foods.filter(
          item => item.category !== '饮料'
        ).length

        this.drinkNum = this.foods.filter(
          item => item.category === '饮料'
        ).length

      } catch (e) {
        console.error('获取详情失败', e)

        uni.showToast({
          title: '获取详情失败',
          icon: 'none'
        })
      }
    },

    getProgressWidth(current, target) {
      const ratio = current / target

      return ratio >= 1
        ? '100%'
        : (ratio * 100) + '%'
    },

    getProgressColor(current, target) {
      const ratio = current / target

      if (current === 0) return '#ccc'

      if (ratio >= 1) return '#e64340'

      return '#9ae471'
    },

    changeDietList() {
      uni.showToast({
        title: '请返回首页切换清单',
        icon: 'none'
      })
    },

    goAddDiet() {
      if (!this.groupId) return

      const url =
        `/pages/diet-record/detail-record/camera-diet/camera-diet` +
        `?groupId=${this.groupId}` +
        `&groupName=${this.groupName}` +
        `&targetKcal=${this.targetKcal}`

      uni.navigateTo({
        url
      })
    }
  }
}
</script>

<style scoped>
.page-wrapper {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(to bottom, #ffffff 0%, #f3f3f3 50%, #f3f3f3 100%);
}
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #ffffff;
  border-bottom: 1rpx solid #ffffff;
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
.nav-date {
  font-size: 24rpx;
  color: #888;
  margin-top: 6rpx;
}

.box { 
  padding: 24rpx;
  padding-bottom: 160rpx;
  margin-top: 200rpx;
}

.white-bottom-card {
  position: fixed;
  left: 0;
  width: 100%;
  height: calc(100vh - 480rpx);
  background: #fff;
  border-top-left-radius: 50rpx;
  border-top-right-radius: 50rpx;
  z-index: 1;
  padding: 32rpx 0rpx;
}

.gray-box {
  width: 86%;
  height: 80rpx;
  background: #f2f2f2;
  border-radius: 50rpx;
  padding: 32rpx;
  margin: 0 auto 30rpx auto;
  display: flex;
  flex-direction: column;
  justify-content: center; 
  position: relative;
  z-index: 2;
}
.bottom-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.left-text .line1 {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
  color: #333;
}
.left-text .line2 {
  font-size: 24rpx;
  font-weight: 500;
  color: #525252;
}
.right-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.right-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #333333;
  margin-bottom: 8rpx;
}
.progress-bar {
  width: 180rpx;
  height: 12rpx;
  background: #e0e0e0;
  border-radius: 6rpx;
  overflow: hidden;
}
.progress {
  height: 100%;
  border-radius: 6rpx;
}

.nine-dot-box {
  width: 90%;
  margin: 0 auto 50rpx auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  height: 600rpx;
}
.dot-item {
  width: 30%;
  height: 200rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gray-dot {
  width: 24rpx;
  height: 24rpx;
  background: #ccc;
  border-radius: 50%;
}

.btn-fixed-wrap {
  position: fixed;
  bottom: 50rpx;
  left: 80rpx;
  right: 80rpx;
  display: flex;
  gap: 60rpx;
  z-index: 10;
}
.btn-left {
  flex: 1;
  text-align: center;
  background: #f2f2f2;
  color: #333;
  font-size: 28rpx;
  padding: 30rpx 0; 
  border-radius: 50rpx;
}
.btn-right {
  flex: 1;
  text-align: center;
  background: #000;
  color: #fff;
  font-size: 28rpx;
  padding: 30rpx 0;
  border-radius: 50rpx;
}
.food-img {
  width: 180rpx;
  height: 180rpx;
  border-radius: 60rpx;
}
</style>