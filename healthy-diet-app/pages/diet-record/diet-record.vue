<template>
  <!-- 最外层：页面包装器 -->
  <view class="page-wrapper">

    <!-- 👇 统一的自定义导航栏 -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">饮食记录</view>
    </view>

    <!-- 内容区域 -->
    <view class="box">

      <!-- 全屏蒙层：编辑模式才显示，点击任意位置关闭 -->
      <view 
        class="close-layer" 
        v-if="editMode" 
        @click="closeEdit"
      ></view>

      <view class="tip" v-if="!token"><text class="a" @click="goLogin">登录</text>后使用完整功能</view>
      
      <!-- 已删除内部的“饮食记录”标题 -->

      <view class="card" v-if="todaySum || yesterdaySum">
        <view class="cal-row">
          <view class="green-dot"></view>
          <text class="cal-texta">昨天 {{ yesterdaySum && yesterdaySum.totalKcal || 0 }} Kcal</text>
          <view class="gray-dot"></view>
          <text class="cal-textb">今天为止 {{ todaySum && todaySum.totalKcal || 0 }} Kcal</text>
        </view>
        <text v-if="todaySum && todaySum.alerts && todaySum.alerts.overDailyCalorie" class="warn">已超过目标热量</text>
        <view v-if="todaySum && todaySum.alerts" class="alerts">
          <text v-if="todaySum.alerts.highCarb" class="warn">{{ todaySum.alerts.highCarbHint }}</text>
          <text v-if="todaySum.alerts.highSodium" class="warn">{{ todaySum.alerts.highSodiumHint }}</text>
          <text v-if="todaySum.alerts.highFat" class="warn">{{ todaySum.alerts.highFatHint }}</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-tip" v-if="token && sortedGroups.length === 0">
        你还没有创建饮食清单
      </view>

      <!-- 固定顺序显示清单 -->
      <view
        class="gray-box"
        :class="{ shake: editMode }"
        v-for="item in sortedGroups"
        :key="item.id"
        @longpress.stop="openEditMode"
        @click.stop="!editMode && goDetail(item)"
      >
        <view class="del-dot" v-if="editMode" @click.stop="deleteItem(item.id)">
          -
        </view>

        <view class="dots">
          <span class="dot" v-for="i in 5"></span>
        </view>
        <view class="bottom-content">
          <view class="left-text">
            <view class="line1">{{ item.name }}</view>
            <view class="line2">{{ (item.foodNum || 0) === 0 ? '还没吃' : (item.foodNum || 0) + '食物' }}</view>
          </view>
          <view class="right-content">
            <view class="right-text">{{ item.currentKcal || 0 }}/{{ item.targetKcal || 500 }} Kcal</view>
            <view class="progress-bar">
              <view
                class="progress"
                :style="{
                  width: progressWidth(item),
                  backgroundColor: progressColor(item)
                }"
              ></view>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部添加按钮 -->
      <view class="add-btn-wrap" v-if="token">
        <text class="add-btn" @click="showCreateModal">+ 添加清单</text>
      </view>
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
      yesterday: '',
      todaySum: null,
      yesterdaySum: null,
      groups: [],
      editMode: false
    }
  },
  computed: {
    sortedGroups() {
      const order = ['早餐', '午餐','晚餐', '下午茶', '零食']
      // 先拿出固定顺序的
      const fixed = order
        .map(name => this.groups.find(item => item.name === name))
        .filter(item => item)
      // 再拿出自定义清单（不在固定列表里的）
      const custom = this.groups.filter(item => !order.includes(item.name))
      // 合并返回 → 固定+自定义都显示
      return [...fixed, ...custom]
    }
  },
  onShow() {
    // 1. 先拿 token
    this.token = uni.getStorageSync('token') || ''

    // 2. 生成日期
    const d = new Date()
    const m = d.getMonth() + 1
    const day = d.getDate()
    this.today = d.getFullYear() + '-' + (m < 10 ? '0' : '') + m + '-' + (day < 10 ? '0' : '') + day

    const yes = new Date()
    yes.setDate(yes.getDate() - 1)
    const ym = yes.getMonth() + 1
    const yd = yes.getDate()
    this.yesterday = yes.getFullYear() + '-' + (ym < 10 ? '0' : '') + ym + '-' + (yd < 10 ? '0' : '') + yd

    // 3. 先加载清单（修复顺序！）
    this.loadGroups()

    // 4. 最后请求数据
    this.getDaySum(this.today, 'today')
    this.getDaySum(this.yesterday, 'yesterday')
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    openEditMode() {
      this.editMode = true
    },
    closeEdit() {
      this.editMode = false
    },
    deleteItem(id) {
      uni.showModal({
        title: '确认删除',
        content: '确定删除该清单？',
        success: (res) => {
          if (res.confirm) {
            this.groups = this.groups.filter(item => item.id !== id)
            uni.setStorageSync('dietGroups', JSON.stringify(this.groups))
          }
        }
      })
    },

    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },

    async getDaySum(date, type) {
      if (!this.token) {
        console.log("未登录，不请求")
        return
      }

      try {
        let res = await request({
          url: `/diet-records/summary-day?date=${date}`,
          method: "GET",
          header: {
            "token": this.token
          }
        })
        
        if (type === 'today') {
          this.todaySum = res || {}
        } else {
          this.yesterdaySum = res || {}
        }

      } catch (e) {
        console.error("❌ 请求汇总失败:", e)
      }
    },

    // ========== 修复版 loadGroups：自动创建三餐 ==========
    loadGroups() {
      let list = uni.getStorageSync('dietGroups') || '[]'
      list = JSON.parse(list)

      const localToken = uni.getStorageSync('token')
      
      // 登录 + 清单为空 → 自动创建早餐、午餐、晚餐
      if (localToken && list.length === 0) {
        list = [
          { id: Date.now() + 1, name: '早餐', foodNum: 0, currentKcal: 0, targetKcal: 500, createDate: this.today },
          { id: Date.now() + 2, name: '午餐', foodNum: 0, currentKcal: 0, targetKcal: 500, createDate: this.today },
          { id: Date.now() + 3, name: '晚餐', foodNum: 0, currentKcal: 0, targetKcal: 500, createDate: this.today }
        ]
        uni.setStorageSync('dietGroups', JSON.stringify(list))
      }

      this.groups = list
    },

    showCreateModal() {
      uni.showActionSheet({
        itemList: ['下午茶', '零食', '自定义清单'],
        success: (res) => {
          const types = ['下午茶', '零食', '自定义']
          const name = types[res.tapIndex]
          
          if (name === '自定义') {
            uni.showModal({
              title: '创建自定义清单',
              editable: true,
              placeholderText: '请输入清单名称',
              success: (res) => {
                if (res.confirm && res.content.trim()) {
                  this.createGroup(res.content.trim())
                }
              }
            })
          } else {
            this.createGroup(name)
          }
        }
      })
    },
    createGroup(name) {
      const list = JSON.parse(uni.getStorageSync('dietGroups') || '[]')
      const hasSame = list.some(i => i.name === name)
      if (hasSame) {
        uni.showToast({ title: name+'已存在', icon: 'none' })
        return
      }
      list.push({
          id: Date.now(),
          name: name,
          foodNum: 0,
          currentKcal: 0,
          targetKcal: 500,
          createDate: this.today
      })
      uni.setStorageSync('dietGroups', JSON.stringify(list))
      this.groups = list
    },
    goDetail(item) {
      uni.navigateTo({
        url: `/pages/diet-record/detail-record/detail-record?id=${item.id}&name=${item.name}&target=${item.targetKcal}&createDate=${item.createDate||this.today}`
      })
    },

    progressWidth(item) {
      const current = item.currentKcal || 0
      const target = item.targetKcal || 500
      const ratio = current / target
      return ratio >= 1 ? '100%' : (ratio * 100) + '%'
    },
    progressColor(item) {
      const current = item.currentKcal || 0
      const target = item.targetKcal || 500
      const ratio = current / target
      if (current === 0) return '#ccc'
      if (ratio >= 1) return '#e64340'
      return '#9ae471'
    }
  }
}
</script>

<style scoped>
/* 最外层背景 */
.page-wrapper {
  width: 100%;
  min-height: 100vh;
  background: #ffffff;
}
/* 统一自定义导航栏 */
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

/* 全屏关闭蒙层：编辑模式下铺满整个屏幕 */
.close-layer{
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 5;
}

.box { 
  padding: 24rpx;
  margin-top: 160rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4rpx;
}
.card {
  background: #fff;
  padding: 20rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}
.warn {
  color: #e64340;
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
}
.alerts { margin-top: 8rpx; }
.tip { margin-bottom: 16rpx; }
.a { color: #007aff; }

.cal-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.green-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: #9ae471;
  margin-right: 8rpx;
  flex-shrink: 0;
}
.cal-texta {
  font-size: 22rpx;
  color: #888;
  margin-right: 24rpx;
}
.gray-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: #d9d9d9;
  margin-right: 8rpx;
  flex-shrink: 0;
}
.cal-textb {
  font-size: 22rpx;
  color: #888;
}

.empty-tip {
  text-align: center;
  padding: 60rpx 0;
  color: #999;
  font-size: 28rpx;
}

.gray-box {
  width: 86%;
  height: 220rpx;
  background: #f2f2f2;
  border-radius: 50rpx;
  padding: 32rpx;
  margin: 0 auto 30rpx auto;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  z-index: 10;
}
.del-dot {
  position: absolute;
  left: -5rpx;
  top: -5rpx;
  width: 40rpx;
  height: 40rpx;
  background-color: #ff4757;
  font-weight: bold;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  z-index: 20;
}

/* 高频微颤动画 */
.shake {
  animation: shake 0.1s infinite alternate;
}
@keyframes shake {
  0% { transform: rotate(-0.3deg); }
  100% { transform: rotate(0.3deg); }
}

.dots {
  display: flex;
  justify-content: space-between;
  width: 80%;
  margin: 40rpx auto 0 auto;
}
.dot {
  width: 20rpx;
  height: 20rpx;
  background: #ccc;
  border-radius: 50%;
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
}
.left-text .line2 {
  font-size: 24rpx;
  font-weight: 500;
  color: #525252;
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

.add-btn-wrap {
  position: fixed;
  bottom: 50rpx;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  z-index: 99;
}
.add-btn {
  background: #000;
  color: #fff;
  padding: 30rpx 60rpx;
  border-radius: 60rpx;
  font-size: 28rpx;
}
</style>