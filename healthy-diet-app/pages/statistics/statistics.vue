<template>
  <view class="page-wrapper">

    <!-- 顶部 -->
    <view class="custom-nav">
      <view class="nav-title">数据统计</view>
    </view>

    <view class="content-box">

      <view v-if="!token" class="tip">
        请先
        <text class="a" @click="goLogin">登录</text>
        使用
      </view>

      <block v-else>

        <!-- 体重趋势 -->
        <view class="card">

          <view class="card-header">
            <text class="card-title">体重变化趋势</text>
            <text class="update-btn" @click="goProfile">
              更新体重
            </text>
          </view>

          <view class="weight-info">
            <view>
              当前体重：
              <text class="bold">
                {{ currentWeight }} kg
              </text>
            </view>

            <view>
              BMI：
              <text class="bold">
                {{ bmi }}
              </text>
            </view>

            <view>
              最后更新：
              <text class="bold">
                {{ lastUpdate }}
              </text>
            </view>
          </view>

          <view v-if="weights.length" class="chart-wrap">
            <canvas
              canvas-id="weightLineChart"
              class="chart-canvas"
              :style="{
                width: chartWidth + 'px',
                height: chartHeight + 'px'
              }"
            ></canvas>
          </view>

          <view v-else class="empty-tip">
            暂无体重数据
          </view>

        </view>

        <!-- 减重进度 -->
        <view v-if="weightStats.totalLost > 0" class="card">
          <text class="card-title">减重进度分析</text>

          <view class="stats-row">
            <view class="stat-item">
              <text class="stat-value">{{ weightStats.totalLost }}</text>
              <text class="stat-label">累计减重 (kg)</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ weightStats.avgWeeklyLoss }}</text>
              <text class="stat-label">周均减重 (kg)</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ weightStats.days }}</text>
              <text class="stat-label">记录天数</text>
            </view>
          </view>

          <view class="progress-section">
            <view class="progress-labels">
              <text>{{ weightStats.startWeight }} kg</text>
              <text>{{ weightStats.endWeight }} kg</text>
            </view>
            <view class="progress-track">
              <view
                class="progress-fill"
                :style="{ width: weightStats.progressPercent + '%' }"
              ></view>
            </view>
            <text class="progress-desc">
              从 {{ weightStats.startDate }} 至 {{ weightStats.endDate }}，
              共减重 {{ weightStats.totalLost }} kg
            </text>
          </view>
        </view>

        <!-- BMI 趋势 -->
        <view v-if="bmiTrend.length && height > 0" class="card">
          <text class="card-title">BMI 变化趋势</text>
          <view class="chart-wrap">
            <canvas
              canvas-id="bmiLineChart"
              class="chart-canvas"
              :style="{
                width: chartWidth + 'px',
                height: chartHeight + 'px'
              }"
            ></canvas>
          </view>
          <view class="bmi-range">
            <text>当前 BMI {{ bmi }}，</text>
            <text class="bold">{{ bmiStatus }}</text>
          </view>
        </view>

        <!-- 每月减重量 -->
        <view v-if="monthlyLoss.length" class="card">
          <text class="card-title">每月减重量</text>
          <view class="chart-wrap">
            <canvas
              canvas-id="monthlyBarChart"
              class="chart-canvas"
              :style="{
                width: chartWidth + 'px',
                height: chartHeight + 'px'
              }"
            ></canvas>
          </view>
        </view>

        <!-- 最近7天热量 -->
        <view class="card">

          <text class="card-title">
            最近7天热量摄入
          </text>

          <view
            v-if="calories.length"
            class="cal-chart"
          >

            <view
              v-for="item in calories"
              :key="item.date"
              class="bar-item"
            >

              <view class="bar-box">

                <view
                  class="bar"
                  :style="{
                    height:
                    (item.kcal / maxKcal * 260) + 'rpx'
                  }"
                ></view>

              </view>

              <text class="bar-date">
                {{ item.date.slice(5) }}
              </text>

              <text class="bar-value">
                {{ item.kcal }} kcal
              </text>

            </view>

          </view>

          <view v-else class="empty-tip">
            暂无热量数据
          </view>

        </view>

      </block>

    </view>

    <!-- 底部导航栏 -->
    <view class="tabbar" style="position:fixed;bottom:0;left:0;right:0;background:#fff;z-index:9999;">
      <view class="tab-item">
        <text class="tab-text bold">数据统计</text>
      </view>
      <view class="tab-item center" @click="goHome">
        <text class="tab-text">首页</text>
      </view>
      <view class="tab-item" @click="goMy">
        <text class="tab-text">我的</text>
      </view>
    </view>

  </view>
</template>

<script>
import { api } from '../../utils/request.js'
import { drawLineChart, drawBarChart, getChartSize } from '../../utils/chartHelper.js'

export default {

  data() {
    const { width, height } = getChartSize()
    return {
      token: '',

      weights: [],

      calories: [],

      currentWeight: '--',

      bmi: '--',

      lastUpdate: '--',

      height: 0,

      maxKcal: 1,

      chartWidth: width,

      chartHeight: height,

      bmiTrend: [],

      monthlyLoss: [],

      weightStats: {
        totalLost: 0,
        avgWeeklyLoss: 0,
        days: 0,
        startWeight: 0,
        endWeight: 0,
        startDate: '',
        endDate: '',
        progressPercent: 0
      }
    }
  },

  computed: {
    bmiStatus() {
      const val = Number(this.bmi)
      if (isNaN(val)) return '--'
      if (val < 18.5) return '偏瘦'
      if (val < 24) return '正常范围'
      if (val < 28) return '超重'
      return '肥胖'
    }
  },

  onShow() {
    this.token = uni.getStorageSync('token') || ''

    if (this.token) {
      this.loadAllData()
    }
  },

  methods: {

    goLogin() {
      uni.navigateTo({
        url: '/pages/login/login'
      })
    },

    goHome() {
      uni.navigateTo({
        url: '/pages/index/index'
      })
    },

    goMy() {
      uni.navigateTo({
        url: '/pages/index/my/my'
      })
    },

    goProfile() {
      uni.navigateTo({
        url: '/pages/index/profile/profile'
      })
    },

    buildAnalysisData() {
      if (!this.weights.length) return

      const sorted = [...this.weights].sort(
        (a, b) => new Date(a.date) - new Date(b.date)
      )

      const first = sorted[0]
      const last = sorted[sorted.length - 1]
      const startWeight = Number(first.weight)
      const endWeight = Number(last.weight)
      const totalLost = Math.max(0, startWeight - endWeight)

      const startTime = new Date(first.date).getTime()
      const endTime = new Date(last.date).getTime()
      const days = Math.max(
        1,
        Math.round((endTime - startTime) / (1000 * 60 * 60 * 24)) + 1
      )
      const weeks = Math.max(days / 7, 1)

      this.weightStats = {
        totalLost: Number(totalLost.toFixed(1)),
        avgWeeklyLoss: Number((totalLost / weeks).toFixed(2)),
        days,
        startWeight,
        endWeight,
        startDate: first.date,
        endDate: last.date,
        progressPercent: startWeight > 0
          ? Math.min(100, Math.round((totalLost / startWeight) * 100))
          : 0
      }

      if (this.height > 0) {
        const heightM = this.height / 100
        this.bmiTrend = sorted.map(item => ({
          date: item.date,
          bmi: Number(
            (Number(item.weight) / (heightM * heightM)).toFixed(1)
          )
        }))
      } else {
        this.bmiTrend = []
      }

      const monthMap = {}
      sorted.forEach(item => {
        const month = item.date.slice(0, 7)
        monthMap[month] = Number(item.weight)
      })

      const months = Object.keys(monthMap).sort()
      this.monthlyLoss = months.slice(1).map((month, i) => {
        const prevMonth = months[i]
        const loss = monthMap[prevMonth] - monthMap[month]
        return {
          month: month.slice(5) + '月',
          loss: Number(loss.toFixed(1))
        }
      })
    },

    renderCharts() {
      this.$nextTick(() => {
        setTimeout(() => {
          if (this.weights.length) {
            const sorted = [...this.weights].sort(
              (a, b) => new Date(a.date) - new Date(b.date)
            )
            drawLineChart('weightLineChart', this, {
              labels: sorted.map(item => item.date),
              values: sorted.map(item => item.weight),
              color: '#333'
            })
          }

          if (this.bmiTrend.length) {
            drawLineChart('bmiLineChart', this, {
              labels: this.bmiTrend.map(item => item.date),
              values: this.bmiTrend.map(item => item.bmi),
              color: '#007aff'
            })
          }

          if (this.monthlyLoss.length) {
            drawBarChart('monthlyBarChart', this, {
              labels: this.monthlyLoss.map(item => item.month),
              values: this.monthlyLoss.map(item => item.loss),
              positiveColor: '#333',
              signed: true
            })
          }
        }, 300)
      })
    },

    async loadAllData() {

      try {

        const profileRes = await api.getProfile()

        const health =
          profileRes.health ||
          profileRes.data?.health ||
          {}

        const weight =
          Number(health.weight || 0)

        this.height =
          Number(health.height || 0)

        this.currentWeight = weight || '--'

        if (this.height > 0 && weight > 0) {

          const heightM = this.height / 100

          this.bmi =
            (
              weight /
              (heightM * heightM)
            ).toFixed(1)

        }

        const statRes =
          await api.weekCalorie()

        this.weights =
          statRes.weights || []

        this.calories =
          Object.entries(
            statRes.caloriesByDay || {}
          ).map(([date, kcal]) => ({
            date,
            kcal: Number(kcal)
          }))

        if (this.weights.length) {

          const sorted = [...this.weights].sort(
            (a, b) => new Date(a.date) - new Date(b.date)
          )

          const last = sorted[sorted.length - 1]

          this.lastUpdate = last.date
        }

        if (this.calories.length) {

          this.maxKcal =
            Math.max(
              ...this.calories.map(
                item => item.kcal
              )
            )

        }

        this.buildAnalysisData()
        this.renderCharts()

      } catch (e) {

        console.error(e)

        uni.showToast({
          title: '数据加载失败',
          icon: 'none'
        })

      }
    }

  }
}
</script>

<style scoped>

.page-wrapper{
  min-height:100vh;
  background:#f7f7f7;
  padding-bottom:160rpx;
}

.custom-nav{
  position:fixed;
  top:0;
  left:0;
  width:100%;
  background:#fff;
  z-index:999;
  padding-top:100rpx;
  padding-bottom:20rpx;
}

.nav-title{
  text-align:center;
  font-size:36rpx;
  font-weight:bold;
}

.content-box{
  padding:24rpx;
  margin-top:160rpx;
}

.card{
  background:#fff;
  border-radius:24rpx;
  padding:30rpx;
  margin-bottom:24rpx;
}

.card-header{
  display:flex;
  justify-content:space-between;
  align-items:center;
}

.card-title{
  font-size:32rpx;
  font-weight:bold;
}

.update-btn{
  color:#007aff;
  font-size:26rpx;
}

.weight-info{
  margin-top:20rpx;
  line-height:2;
  font-size:26rpx;
}

.bold{
  font-weight:bold;
}

.chart-wrap{
  margin-top:20rpx;
  width:100%;
  overflow:hidden;
}

.chart-canvas{
  display:block;
  width:100%;
}

.stats-row{
  display:flex;
  justify-content:space-between;
  margin-top:24rpx;
}

.stat-item{
  flex:1;
  text-align:center;
}

.stat-value{
  display:block;
  font-size:40rpx;
  font-weight:bold;
  color:#333;
}

.stat-label{
  display:block;
  margin-top:8rpx;
  font-size:22rpx;
  color:#999;
}

.progress-section{
  margin-top:30rpx;
}

.progress-labels{
  display:flex;
  justify-content:space-between;
  font-size:24rpx;
  color:#666;
  margin-bottom:12rpx;
}

.progress-track{
  height:16rpx;
  background:#eee;
  border-radius:20rpx;
  overflow:hidden;
}

.progress-fill{
  height:100%;
  background:#333;
  border-radius:20rpx;
  transition:width 0.3s;
}

.progress-desc{
  display:block;
  margin-top:16rpx;
  font-size:22rpx;
  color:#999;
}

.bmi-range{
  margin-top:16rpx;
  font-size:24rpx;
  color:#666;
}

.cal-chart{
  display:flex;
  align-items:flex-end;
  height:360rpx;
  margin-top:20rpx;
}

.bar-item{
  flex:1;
  display:flex;
  flex-direction:column;
  align-items:center;
}

.bar-box{
  width:50rpx;
  height:260rpx;
  background:#eee;
  display:flex;
  align-items:flex-end;
  border-radius:12rpx;
  overflow:hidden;
}

.bar{
  width:100%;
  background:#000;
}

.bar-date{
  margin-top:10rpx;
  font-size:20rpx;
}

.bar-value{
  font-size:20rpx;
  color:#666;
}

.empty-tip{
  text-align:center;
  color:#999;
  padding:30rpx;
}

.tip{
  text-align:center;
  padding:100rpx 0;
}

.a{
  color:#007aff;
}

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
.tab-item { 
  flex:1;
  display:flex;
  align-items:center;
  justify-content:center;
  height:100%; 
}
.tab-text {
  font-size: 26rpx;
  color: #333;
}
.tab-text.bold { 
  font-weight:bold;
  color:#000; 
}
</style>
