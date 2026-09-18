<template>
  <view class="page-wrapper">
    <!-- 顶部导航：仅删除返回箭头、修改文字，样式完全保留 -->
    <view class="custom-nav">
      <view class="nav-title">小饭卡</view>
    </view>

    <!-- 原有内容区域 100% 保留 -->
    <view class="box">
      <view class="close-layer" v-if="editMode" @click="closeEdit"></view>
      <view class="tip" v-if="!token"><text class="a" @click="goLogin">登录</text>后使用完整功能</view>

      <view class="card" v-if="token">
		  
        <view class="cal-row" style="display:flex; justify-content:space-between; align-items:center;">
          <view style="display:flex; align-items:center;">
            <view class="green-dot"></view>
            <text class="cal-textb">{{ selectDate }} 共 {{ totalKcal }} kcal</text>
          </view>
          <picker mode="date" :value="selectDate" :end="today" @change="onDateChange">
            <view class="calendar-icon">
              <text>📅</text>
            </view>
          </picker>
        </view>

        <text v-if="todaySum && todaySum.alerts && todaySum.alerts.overDailyCalorie" class="warn">已超过目标热量</text>
        <view v-if="todaySum && todaySum.alerts" class="alerts">
          <text v-if="todaySum.alerts.highCarb" class="warn">{{ todaySum.alerts.highCarbHint }}</text>
          <text v-if="todaySum.alerts.highSodium" class="warn">{{ todaySum.alerts.highSodiumHint }}</text>
          <text v-if="todaySum.alerts.highFat" class="warn">{{ todaySum.alerts.highFatHint }}</text>
        </view>
      </view>

      <view class="empty-tip" v-if="token && sortedGroups.length === 0">
        你还没有创建饮食清单
      </view>

      <view class="gray-box" :class="{ shake: editMode }" v-for="item in sortedGroups" :key="item.id"
        @longpress.stop="openEditMode" @click.stop="!editMode && goDetail(item)">
        <view class="del-dot" v-if="editMode" @click.stop="deleteItem(item.id)">-</view>
        <view class="dots">
        
          <view
            class="dot-wrap"
            v-for="i in 5"
            :key="i"
          >
        
            <image
              v-if="
                item.foods &&
                item.foods[i] &&
                item.foods[i].image
              "
              class="food-dot-img"
              :src="
                item.foods[i].image.startsWith('data:')
                  ? item.foods[i].image
                  : 'data:image/png;base64,' + item.foods[i].image
              "
              mode="aspectFit"
            />
        
            <view
              v-else
              class="dot"
            ></view>
        
          </view>
        
        </view>
        <view class="bottom-content">
          <view class="left-text">
            <view class="line1">{{ item.name }}</view>
            <view class="line2">
              {{
                item.foodNum + item.drinkNum === 0
                  ? '还没吃'
                  : item.foodNum + '食物 ' + item.drinkNum + '饮料'
              }}
            </view>
          </view>
          <view class="right-text">
            <view class="right-text">{{ item.currentKcal || 0 }}/{{ item.targetKcal || 500 }} Kcal</view>
            <view class="progress-bar">
              <view class="progress" :style="{ width: progressWidth(item), backgroundColor: progressColor(item) }"></view>
            </view>
          </view>
        </view>
      </view>

      <view class="add-btn-wrap" v-if="token">
        <text class="add-btn" @click="showCreateModal">+ 添加清单</text>
      </view>
    </view>

    <view class="tabbar" style="position:fixed;bottom:0;left:0;right:0;background:#fff;z-index:9999;">
      <view class="tab-item" @click="goNeedLogin('/pages/statistics/statistics')">
        <text class="tab-text">数据统计</text>
      </view>
      <view class="tab-item center" @click="goNeedCreate">
        <text class="tab-text bold">首页</text>
      </view>
      <view class="tab-item" @click="go('/pages/index/my/my')">
        <text class="tab-text">我的</text>
      </view>
    </view>

    <!-- AI悬浮球 100% 保留 -->
    <view class="ai-float" :style="{ transform: `translate(${x}px, ${y}px)` }"
      @click="goNeedLogin('/pages/knowledge/knowledge')"
      @touchstart="dragStart" @touchmove.stop="dragMove" @touchend.stop="dragEnd">
      <text class="ai-icon">🤖</text>
      <view class="ai-tip" :class="tipSide" v-show="showTip">有疑问？问AI助手</view>
    </view>
  </view>
</template>

<script>
import { request } from '../../utils/request.js'

export default {
  data() {
    return {
      token: '',
      today: '',
      selectDate: '',
      groups: [],
      editMode: false,
      totalKcal: 0,
      x: 0,
      y: 0,
      startX: 0,
      startY: 0,
      initX: 0,
      initY: 0,
      screenW: 0,
      screenH: 0,
      tipSide: "tip-left",
      showTip: false,
      tipTimer: null
    }
  },
  computed: {
    sortedGroups() {
      const order = ['早餐', '午餐', '晚餐', '下午茶', '零食']
      const fixed = order.map(name => this.groups.find(item => item.name === name)).filter(item => item)
      const custom = this.groups.filter(item => !order.includes(item.name))
      return [...fixed, ...custom]
    }
  },
  onShow() {
    this.token = uni.getStorageSync('token') || ''
  
    const d = new Date()
    const m = d.getMonth() + 1
    const day = d.getDate()
  
    this.today =
      d.getFullYear() +
      '-' +
      (m < 10 ? '0' : '') +
      m +
      '-' +
      (day < 10 ? '0' : '') +
      day
  
    // 只在第一次进入首页时设置今天
    if (!this.selectDate) {
      this.selectDate = this.today
    }
  
    const sys = uni.getSystemInfoSync()
  
    this.screenW = sys.windowWidth
    this.screenH = sys.windowHeight
  
    if (!this.x && !this.y) {
      this.x = this.screenW - uni.upx2px(90)
      this.y = this.screenH - uni.upx2px(260)
    }
  
    this.loadGroups()
  
    if (!this.tipTimer) {
      this.autoTip()
    }
  },
  onUnload() {
    clearInterval(this.tipTimer)
  },
  methods: {
    async loadGroups() {
      if (!this.token) {
        this.groups = []
        this.totalKcal = 0
        return
      }
    
      try {
        const res = await request({
          url: `/food-list/date?date=${this.selectDate}`,
          method: 'GET',
          header: { token: this.token }
        })

        this.totalKcal = res.totalKcal || 0

        this.groups = (res.list || []).map(item => {
          // 计算食物和饮料数量
          const foods = item.foods || []
          const foodCount = foods.filter(f => f.category !== '饮料').length
          const drinkCount = foods.filter(f => f.category === '饮料').length

          // 过滤掉空的 food 对象，保证第一个不显示空图片
          const validFoods = foods.filter(f => f.image && f.image.trim() !== '')

          return {
            id: item.id,
            name: item.mealType,
            currentKcal: item.currentKcal || 0,
            targetKcal: item.targetKcal || 500,
            foodNum: foodCount,
            drinkNum: drinkCount,
            createDate: item.date,
            foods: validFoods
          }
        })

      } catch (e) {
        console.error(e)
        uni.showToast({ title: '获取清单失败', icon: 'none' })
      }
    },

    go(url) {
      uni.navigateTo({ url })
    },

    goNeedLogin(url) {
      if (!this.token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url })
    },

    goNeedCreate() {
      if (!this.token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      this.showCreateModal()
    },

    openEditMode() {
      this.editMode = true
    },

    closeEdit() {
      this.editMode = false
    },

    deleteItem(id) {
      this.groups = this.groups.filter(item => item.id !== id)
      uni.showToast({ title: '删除成功', icon: 'none' })
    },

    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },

    showCreateModal() {
      uni.showActionSheet({
        itemList: ['下午茶', '零食', '自定义清单'],
        success: (res) => {
          const arr = ['下午茶', '零食', '自定义']
          const mealType = arr[res.tapIndex]

          if (mealType === '自定义') {
            uni.showModal({
              title: '创建自定义清单',
              editable: true,
              success: (r) => {
                if (r.confirm && r.content.trim()) {
                  this.createGroup(r.content.trim())
                }
              }
            })
          } else {
            this.createGroup(mealType)
          }
        }
      })
    },

    async createGroup(mealType) {
      try {
        await request({
          url: '/food-list/add',
          method: 'POST',
          header: { token: this.token },
          data: { mealType }
        })
        this.loadGroups()
      } catch (e) {
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    },

    goDetail(item) {
      uni.navigateTo({
        url: `/pages/diet-record/detail-record/detail-record?id=${item.id}&createDate=${this.selectDate}`
      })
    },

    progressWidth(item) {
      const ratio = (item.currentKcal || 0) / (item.targetKcal || 500)
      return ratio >= 1 ? '100%' : ratio * 100 + '%'
    },

    progressColor(item) {
      const ratio = (item.currentKcal || 0) / (item.targetKcal || 500)
      if (item.currentKcal === 0) return '#ccc'
      if (ratio >= 1) return '#e64340'
      return '#9ae471'
    },

    onDateChange(e) {
      this.selectDate = e.detail.value
      this.loadGroups()
    },

    autoTip() {
      const show = () => {
        this.showTip = true
        setTimeout(() => { this.showTip = false }, 5000)
      }
      show()
      this.tipTimer = setInterval(show, 300000)
    },

    dragStart(e) {
      this.startX = e.touches[0].clientX
      this.startY = e.touches[0].clientY
      this.initX = this.x
      this.initY = this.y
    },

    dragMove(e) {
      this.x = this.initX + (e.touches[0].clientX - this.startX)
      this.y = this.initY + (e.touches[0].clientY - this.startY)
    },

    dragEnd() {
      const centerX = this.screenW / 2
      const btnW = uni.upx2px(90)
      if (this.x < centerX) {
        this.x = 5
        this.tipSide = 'tip-right'
      } else {
        this.x = this.screenW - btnW
        this.tipSide = 'tip-left'
      }
    }
  }
}
</script>

<style scoped>
/* 外层容器：取消固定高度限制，允许页面滚动 */
.page-wrapper { 
  width: 100%; 
  min-height: 100vh; 
  background: #ffffff; 
  /* 关键：留出底部导航高度，避免内容被遮挡 */
  padding-bottom: 160rpx;
}
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #ffffff;
  padding-top: 100rpx;
  padding-bottom: 15rpx;
  z-index: 999;
  padding-left: 30rpx;
  box-sizing: border-box;
}
.back-btn { display: none; }
.back-icon { display: none; }
.nav-title { font-size: 38rpx; font-weight: bold; color: #000; }
.close-layer{ position: fixed; left: 0; top: 0; right: 0; bottom: 0; z-index: 5; }
.box { 
  padding: 24rpx; 
  margin-top: 160rpx; /* 顶部导航占位 */
}
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4rpx; }
.card { background: #fff; padding: 20rpx; border-radius: 12rpx; margin-bottom: 0rpx; }
.warn { color: #e64340; display: block; margin-top: 8rpx; font-size: 26rpx; }
.alerts { margin-top: 8rpx; }
.tip { margin-bottom: 16rpx; }
.a { color: #007aff; }
.cal-row { display: flex; align-items: center; margin-bottom: 8rpx; }
.green-dot { width: 13rpx; height: 13rpx; border-radius: 50%; background-color: #9ae471; margin-right: 8rpx; flex-shrink: 0; }
.cal-textb { font-size: 24rpx; color: #888; }
.empty-tip { text-align: center; padding: 60rpx 0; color: #999; font-size: 28rpx; }
.gray-box { width: 86%; height: 220rpx; background: #f2f2f2; border-radius: 50rpx; padding: 32rpx; margin: 0 auto 30rpx auto; display: flex; flex-direction: column; justify-content: space-between; position: relative; z-index: 10; }
.del-dot { position: absolute; left: -5rpx; top: -5rpx; width: 40rpx; height: 40rpx; background-color: #ff4757; font-weight: bold; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32rpx; z-index: 20; }
.shake { animation: shake 0.1s infinite alternate; }
@keyframes shake { 0% { transform: rotate(-0.3deg); } 100% { transform: rotate(0.3deg); } }
.bottom-content { display: flex; justify-content: space-between; align-items: flex-end; }
.left-text .line1 { font-size: 30rpx; font-weight: bold; margin-bottom: 8rpx; }
.left-text .line2 { font-size: 24rpx; font-weight: 500; color: #525252; }
.right-text { font-size: 24rpx; font-weight: 600; color: #333333; margin-bottom: 8rpx; }
.progress-bar { width: 180rpx; height: 12rpx; background: #e0e0e0; border-radius: 6rpx; overflow: hidden; }
.progress { height: 100%; border-radius: 6rpx; }
.add-btn-wrap {
  position: fixed;
  bottom: 90rpx;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
}
.add-btn { background: #000; color: #fff; padding: 30rpx 60rpx; border-radius: 60rpx; font-size: 28rpx; }

/* 底部导航样式 完全保留 */
.tabbar { height: 160rpx; background-color: #fff; border-top: 1rpx solid #eee; display: flex; align-items: center; justify-content: space-around; padding-bottom: 20rpx; box-sizing: border-box; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; height: 100%; }
.tab-item.center { transform: translateY(-10rpx); }
.tab-text { font-size: 26rpx; color: #333; }
.tab-text.bold { font-weight: bold; color: #000; }

/* AI悬浮球 完全保留 */
.ai-float { position: fixed; top: 0; left: 0; width: 80rpx; height: 80rpx; border-radius: 50%; background: #000; display: flex; align-items: center; justify-content: center; z-index: 9999; }
.ai-icon { font-size: 36rpx; color: #fff; }
.ai-tip { position: absolute; background: #333; color: #fff; font-size: 24rpx; padding: 8rpx 14rpx; border-radius: 10rpx; white-space: nowrap; }
.tip-left { right: 90rpx; }
.tip-right { left: 90rpx; }

/* 日历图标样式 */
.calendar-icon {
  font-size: 40rpx;
  padding: 0 10rpx;
  color: #666;
}
.dots {
  display: flex;
  justify-content: space-around;
  width: 98%;
  margin: 40rpx auto 0 auto;
}

.dot-wrap {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.food-dot-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 30rpx
}

.dot {
  width: 20rpx;
  height: 20rpx;
  background: #ccc;
  border-radius: 50%;
}
</style>