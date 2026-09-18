<template>
  <view class="plan-page">
    <!-- 顶部真实日期栏 → 从 weekPlans 生成 -->
    <scroll-view class="date-bar" scroll-x>
      <view
        class="date-item"
        v-for="(day, idx) in dateList"
        :key="idx"
        :class="{ active: currentDateIndex === idx }"
        @click="selectDate(idx)"
      >
        <text class="date-text">{{ day.date }}</text>
        <text class="week">{{ day.week }}</text>
      </view>
    </scroll-view>

    <view class="box">
      <view class="tip" v-if="!token">
        请先<text class="a" @click="goLogin">登录</text>使用
      </view>

      <block v-else>
        <view class="empty-plan" v-if="!currentPlan">
          <text>暂无配餐数据</text>
          <text class="empty-sub">请确认菜谱库有数据后下拉刷新</text>
        </view>

        <!-- 当日食谱 + 换一换（同行靠右） -->
        <view class="card" v-if="currentPlan">
          <view class="card-header">
            <text class="card-title">当日食谱</text>
            <text class="text-btn" @click="refreshAllMeals">换一换</text>
          </view>

          <!-- 早餐 -->
          <view class="meal-group">
            <view class="meal-header">
              <text class="group-title">早餐</text>
              <text class="text-btn" @click="addDish('breakfast')">+ 添加</text>
            </view>
            <view
              v-for="(item, idx) in currentPlan.breakfast"
              :key="idx"
              class="meal-item"
            >
              <view class="left">
                <text class="name">{{ item.name }}</text>
                <text class="type-tag">{{ getCnCategory(item.category) }}</text>
                <text class="cal">{{ item.calories }} kcal</text>
              </view>
              <view class="btns">
                <button class="mini-btn gray" @click="replaceItem(idx, 'breakfast')">替换</button>
                <button class="mini-btn red" @click="deleteItem(idx, 'breakfast')">删除</button>
              </view>
            </view>
          </view>

          <!-- 午餐 -->
          <view class="meal-group">
            <view class="meal-header">
              <text class="group-title">午餐</text>
              <text class="text-btn" @click="addDish('lunch')">+ 添加</text>
            </view>
            <view
              v-for="(item, idx) in currentPlan.lunch"
              :key="idx"
              class="meal-item"
            >
              <view class="left">
                <text class="name">{{ item.name }}</text>
                <text class="type-tag">{{ getCnCategory(item.category) }}</text>
                <text class="cal">{{ item.calories }} kcal</text>
              </view>
              <view class="btns">
                <button class="mini-btn gray" @click="replaceItem(idx, 'lunch')">替换</button>
                <button class="mini-btn red" @click="deleteItem(idx, 'lunch')">删除</button>
              </view>
            </view>
          </view>

          <!-- 晚餐 -->
          <view class="meal-group">
            <view class="meal-header">
              <text class="group-title">晚餐</text>
              <text class="text-btn" @click="addDish('dinner')">+ 添加</text>
            </view>
            <view
              v-for="(item, idx) in currentPlan.dinner"
              :key="idx"
              class="meal-item"
            >
              <view class="left">
                <text class="name">{{ item.name }}</text>
                <text class="type-tag">{{ getCnCategory(item.category) }}</text>
                <text class="cal">{{ item.calories }} kcal</text>
              </view>
              <view class="btns">
                <button class="mini-btn gray" @click="replaceItem(idx, 'dinner')">替换</button>
                <button class="mini-btn red" @click="deleteItem(idx, 'dinner')">删除</button>
              </view>
            </view>
          </view>

          <button class="btn-primary black" @click="generateShoppingNow">查看购物清单</button>
        </view>
      </block>
    </view>

    <!-- 弹窗 + 搜索框 -->
    <view class="popup" v-if="showPopup">
      <view class="popup-content">
        <view class="popup-header">
          <text class="title">{{ isAddMode ? '添加菜品' : '替换菜品' }}</text>
          <text class="close" @click="closePopup">×</text>
        </view>

        <input
          v-model="searchKey"
          class="search-input"
          placeholder="搜索菜品"
        />

        <scroll-view scroll-y class="popup-list">
          <view
            v-for="r in showFilterList"
            :key="r.id"
            class="recipe-item"
            @click="chooseRecipe(r)"
          >
            <text>{{ r.name }}</text>
            <text class="c">{{ r.calories }} kcal</text>
          </view>
        </scroll-view>
      </view>
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
      dateList: [],
      currentDateIndex: 0,
      weekPlans: [],
      currentPlan: null,

      showPopup: false,
      isAddMode: false,
      replaceTarget: '',
      replaceIndex: -1,
      filterRecipes: [],
      searchKey: '',

      allFoods: [],
    }
  },

  computed: {
    showFilterList() {
      if (!this.searchKey) return this.filterRecipes
      const k = this.searchKey.trim().toLowerCase()
      return this.filterRecipes.filter(i => i.name.toLowerCase().includes(k))
    }
  },

  onShow() {
    this.token = uni.getStorageSync('token') || ''

    if (this.token) {
      this.initPage()
    }
  },

  onPullDownRefresh() {
    if (!this.token) {
      uni.stopPullDownRefresh()
      return
    }
    this.initPage().finally(() => uni.stopPullDownRefresh())
  },

  methods: {
    async initPage() {
      await this.loadAllFoods()
      await this.loadWeekPlan()
    },

    formatDate(d) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },

    normalizeDate(dateVal) {
      if (!dateVal) return ''
      if (Array.isArray(dateVal)) {
        const [y, m, d] = dateVal
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      }
      return String(dateVal).slice(0, 10)
    },

    // 获取本周开始日期（周一）
    getWeekStart() {
      const today = new Date()
      const day = today.getDay() || 7
      today.setDate(today.getDate() - day + 1)
      return this.formatDate(today)
    },

    getWeekEnd() {
      const start = new Date()
      const day = start.getDay() || 7
      start.setDate(start.getDate() - day + 7)
      return this.formatDate(start)
    },

    getCnCategory(category) {
      const map = {
        STAPLE: '主食',
        MEAT: '肉类',
        VEGETABLE: '蔬菜',
        SOUP: '汤',
        DRINK: '饮料',
        FRUIT: '水果',
        OTHER: '其他'
      }
      return map[category] || category
    },

    async loadWeekPlan() {
      try {
        const startDate = this.getWeekStart()
        const endDate = this.getWeekEnd()
        const plans = await api.getWeekPlan(startDate, endDate)
        const planList = Array.isArray(plans) ? plans : []

        // 按日期分组
        const byDate = {}
        planList.forEach(p => {
          const dateStr = this.normalizeDate(p.date)
          if (!byDate[dateStr]) {
            byDate[dateStr] = { date: dateStr, breakfast: [], lunch: [], dinner: [] }
          }
          const recipeObj = this.allFoods.find(f => f.id === p.recipeId) || { id: p.recipeId, name: '未知菜', category: 'OTHER' }
          if (p.mealType === 1) byDate[dateStr].breakfast.push(recipeObj)
          if (p.mealType === 2) byDate[dateStr].lunch.push(recipeObj)
          if (p.mealType === 3) byDate[dateStr].dinner.push(recipeObj)
        })

        const grouped = Object.keys(byDate)
          .sort()
          .map(key => byDate[key])

        this.weekPlans = grouped

        // ===== 用真实计划日期生成顶部日期 =====
        const weekMap = ['日', '一', '二', '三', '四', '五', '六']

        this.dateList = grouped.map(item => {
          const d = new Date(item.date)

          return {
            date: `${d.getMonth() + 1}.${d.getDate()}`,
            week: `周${weekMap[d.getDay()]}`
          }
        })

        // 默认选中第一天
        this.currentDateIndex = 0
        this.currentPlan = grouped[0] || null

        if (!this.currentPlan) {
          uni.showToast({ title: '暂无配餐，请检查菜谱数据', icon: 'none' })
        }

      } catch (err) {
        console.error("加载本周食谱失败", err)
        uni.showToast({ title: '加载本周食谱失败', icon: 'none' })
      }
    },

    goLogin() { uni.navigateTo({ url: '/pages/login/login' }) },
    goHome() { uni.navigateTo({ url: '/pages/index/index' }) },
    goMy() { uni.navigateTo({ url: '/pages/index/my/my' }) },
    goProfile() { uni.navigateTo({ url: '/pages/profile/profile' }) },

    async loadAllFoods() {
      try {
        const res = await api.getAllRecipes()
        this.allFoods = Array.isArray(res) ? res : []
      } catch (e) {
        console.error('获取菜谱失败', e)
        this.allFoods = []
      }
    },

    selectDate(idx) {
      this.currentDateIndex = idx
      this.currentPlan = this.weekPlans[idx]
    },

    refreshAllMeals() {
      const p = this.currentPlan
      p.breakfast = this.randomMeal()
      p.lunch = this.randomMeal()
      p.dinner = this.randomMeal()
      uni.showToast({ title: '已刷新', icon: 'success' })
    },

    randomMeal() {
      if (!Array.isArray(this.allFoods)) return []
      const staples = this.allFoods.filter(f => f.category === 'STAPLE')
      const meats = this.allFoods.filter(f => f.category === 'MEAT')
      const vegs = this.allFoods.filter(f => f.category === 'VEGETABLE')
      const soups = this.allFoods.filter(f => f.category === 'SOUP')
      const drinks = this.allFoods.filter(f => f.category === 'DRINK')
      const fruits = this.allFoods.filter(f => f.category === 'FRUIT')

      const res = []
      if (staples.length) res.push({ ...staples[Math.floor(Math.random() * staples.length)] })
      if (meats.length) res.push({ ...meats[Math.floor(Math.random() * meats.length)] })
      if (vegs.length && Math.random() > 0.5) res.push({ ...vegs[Math.floor(Math.random() * vegs.length)] })
      if (soups.length && Math.random() > 0.5) res.push({ ...soups[Math.floor(Math.random() * soups.length)] })
      if (drinks.length && Math.random() > 0.5) res.push({ ...drinks[Math.floor(Math.random() * drinks.length)] })
      if (fruits.length && Math.random() > 0.5) res.push({ ...fruits[Math.floor(Math.random() * fruits.length)] })
      return res
    },

    replaceItem(idx, type) {
      const item = this.currentPlan[type][idx]
      this.isAddMode = false
      this.replaceTarget = type
      this.replaceIndex = idx
      this.filterRecipes = this.allFoods.filter(i => i.category === item.category)
      this.searchKey = ''
      this.showPopup = true
    },

    addDish(type) {
      this.isAddMode = true
      this.replaceTarget = type
      this.replaceIndex = -1
      this.filterRecipes = this.allFoods
      this.searchKey = ''
      this.showPopup = true
    },

    async chooseRecipe(item) {
      const newItem = { ...item }
      if (this.isAddMode) {
        this.currentPlan[this.replaceTarget].push(newItem)
      } else {
        this.currentPlan[this.replaceTarget][this.replaceIndex] = newItem
      }
      this.closePopup()
      const mealMap = { breakfast: 1, lunch: 2, dinner: 3 }
      await this.saveCurrentMeal(this.replaceTarget, mealMap[this.replaceTarget])
    },

    async deleteItem(idx, type) {
      this.currentPlan[type].splice(idx, 1)
      const mealMap = { breakfast: 1, lunch: 2, dinner: 3 }
      await this.saveCurrentMeal(type, mealMap[type])
    },

    closePopup() {
      this.showPopup = false
    },

    async saveCurrentMeal(type, mealType) {
      const plan = this.currentPlan
      if (!plan) return
      const recipeIds = plan[type].map(r => r.id)
      try {
        await api.updateMeal({ date: plan.date, mealType, recipeIds }, this.token)
        console.log('保存成功', plan.date, mealType, recipeIds)
      } catch (e) {
        console.error('保存失败', e)
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },

    generateShoppingNow() {
      const plan = this.currentPlan
      if (!plan || (!plan.breakfast.length && !plan.lunch.length && !plan.dinner.length)) {
        uni.showToast({ title: '暂无菜谱', icon: 'none' })
        return
      }

      uni.navigateTo({
        url: `/pages/shopping/shopping?date=${plan.date}`
      })
    }
  }
}
</script>

<style scoped>
.plan-page {
  background: #fff;
  min-height: 100vh;
  padding: 20rpx 0 60rpx;
  box-sizing: border-box;
}

.date-bar {
  white-space: nowrap;
  padding: 20rpx 30rpx;
}
.date-item {
  display: inline-block;
  width: 100rpx;
  text-align: center;
  padding: 12rpx 10rpx;
  margin-right: 16rpx;
  border-radius: 20rpx;
  background: #f7f7f7;
}
.date-item.active {
  background: #000;
  color: #fff;
}
.date-text {
  font-size: 28rpx;
  font-weight: 500;
  display: block;
}
.week {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-top: 4rpx;
}
.date-item.active .week {
  color: rgba(255,255,255,0.7);
}

.box {
  padding: 0 30rpx;
}

.tip {
  font-size: 28rpx;
  color: #666;
  text-align: center;
  padding: 60rpx 0;
}
.a {
  color: #007aff;
  font-weight: bold;
}

.card {
  background: #fff;
  padding: 30rpx;
  border-radius: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}
.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.card-title, .group-title {
  font-weight: bold;
}
.text-btn {
  color: #000;
  font-size: 28rpx;
  margin-left: auto;
}

.meal-group {
  margin-bottom: 40rpx;
}
.meal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1px solid #f1f1f1;
}
.meal-item .name {
  font-size: 28rpx;
}
.type-tag {
  display: inline-block;
  font-size: 22rpx;
  color: #fff;
  background: #409eff;
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin: 0 10rpx;
}
.cal {
  font-size: 24rpx;
  color: #666;
}
.btns {
  display: flex;
  gap: 12rpx;
}
.mini-btn {
  padding: 10rpx 18rpx;
  background: #f1f1f7;
  border-radius: 30rpx;
  font-size: 22rpx;
  border: none;
}
.mini-btn.red {
  background: #ff4d4f;
  color: #fff;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #000;
  color: #fff;
  border-radius: 50rpx;
  font-size: 28rpx;
  border: none;
  margin-top: 20rpx;
}

.popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
}
.popup-content {
  background: #fff;
  border-radius: 30rpx 30rpx 0 0;
  width: 100%;
  max-height: 80vh;
  padding: 30rpx;
  box-sizing: border-box;
  position: absolute;
  bottom: 0;
  left: 0;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.popup-header .title {
  font-size: 32rpx;
  font-weight: bold;
}
.close {
  font-size: 40rpx;
}
.search-input {
  width: 100%;
  height: 70rpx;
  background: #f5f5f5;
  border-radius: 30rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
  border: none;
}
.recipe-item {
  padding: 20rpx 0;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
}

.empty-plan {
  text-align: center;
  padding: 80rpx 30rpx;
  color: #666;
  font-size: 28rpx;
}
.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}
</style>