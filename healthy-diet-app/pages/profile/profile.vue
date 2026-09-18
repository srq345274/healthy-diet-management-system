<template>
  <view class="page-wrapper">

    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">健康档案</view>
    </view>

    <view class="profile-page box">
      <view class="header" v-if="token">
        <text class="subtitle">管理你的个人健康信息</text>
      </view>

      <view class="tip" v-if="!token">
        请先<text class="a" @click="goLogin">登录</text>
      </view>

      <block v-else>
        <!-- 年龄和性别 -->
        <view class="field readonly">
          <text class="label">年龄：</text>
          <text>{{ age }}岁</text>
        </view>
        <view class="field readonly">
          <text class="label">性别：</text>
          <text>{{ gender }}</text>
        </view>

        <!-- 身高体重 -->
        <view class="field">
          <text class="label">身高(cm)</text>
          <input v-model="form.height" type="digit" placeholder="请输入身高" />
        </view>
        <view class="field">
          <text class="label">体重(kg)</text>
          <input v-model="form.weight" type="digit" placeholder="请输入体重" />
        </view>

        <!-- 病史 -->
<view class="field">
  <text class="label">基础病</text>
  <checkbox-group @change="onDiseaseChange">
    <label
      v-for="item in diseases"
      :key="item.value"
      class="option"
    >
      <checkbox
        :value="item.value"
        :checked="form.disease.includes(item.value)"
        :disabled="
          form.disease.includes('无') &&
          item.value !== '无'
        "
      />
      <text>{{item.label}}</text>
    </label>
  </checkbox-group>
  <input v-if="showDiseaseOther" v-model="form.diseaseOther" class="other-input" placeholder="请输入其他病史"/>
</view>

<!-- 过敏 -->
<view class="field">
  <text class="label">过敏物</text>
  <checkbox-group @change="onAllergyChange">
    <label v-for="item in allergiesList" :key="item.value" class="option">
      <checkbox
        :value="item.value"
        :checked="form.allergy.includes(item.value)"
        :disabled="
          form.allergy.includes('无') &&
          item.value !== '无'
        "
      />
      <text>{{item.label}}</text>
    </label>
  </checkbox-group>
  <input v-if="showAllergyOther" v-model="form.allergyOther" class="other-input" placeholder="请输入其他过敏物"/>
</view>

<!-- 忌口 -->
<view class="field">
  <text class="label">忌口</text>
  <checkbox-group @change="onAvoidChange">
    <label v-for="item in avoidList" :key="item.value" class="option">
      <checkbox
        :value="item.value"
        :checked="form.avoidFood.includes(item.value)"
        :disabled="
          form.avoidFood.includes('无') &&
          item.value !== '无'
        "
      />
      <text>{{item.label}}</text>
    </label>
  </checkbox-group>
  <input v-if="showAvoidOther" v-model="form.avoidOther" class="other-input" placeholder="请输入其他忌口"/>
</view>

<button
  class="btn-primary black"
  type="primary"
  @click="save"
>
  保存档案
</button>
      </block>
    </view>
  </view>
</template>

<script>
import { api } from '../../utils/request.js'

export default {
  data() {
    return {
      token: '',
      age: '',
      gender: '',
      form: {
        height: '',
        weight: '',
        disease: [],
        allergy: [],
        avoidFood: [],
        diseaseOther: '',
        allergyOther: '',
        avoidOther: ''
      },
      showDiseaseOther: false,
      showAllergyOther: false,
      showAvoidOther: false,
      diseases: [
		{ label: '无', value: '无' },
        { label: '高血压', value: '高血压' },
        { label: '糖尿病', value: '糖尿病' },
        { label: '高血脂', value: '高血脂' },
        { label: '痛风', value: '痛风' },
        { label: '胃病', value: '胃病' },
        { label: '其他', value: '其他' }
      ],
      allergiesList: [
	    { label: '无', value: '无' },
        { label: '花生', value: '花生' },
        { label: '海鲜', value: '海鲜' },
        { label: '牛奶', value: '牛奶' },
        { label: '鸡蛋', value: '鸡蛋' },
        { label: '其他', value: '其他' }
      ],
      avoidList: [
		{ label: '无', value: '无' },
        { label: '辣', value: '辣' },
        { label: '油炸', value: '油炸' },
        { label: '甜食', value: '甜食' },
        { label: '生冷', value: '生冷' },
        { label: '海鲜', value: '海鲜' },
        { label: '其他', value: '其他' }
      ]
    }
  },
  onShow() {
    this.token = uni.getStorageSync('token') || ''
    if (this.token) {
      this.loadProfile()
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    getAge(birthday) {
      if (!birthday) return ''
      const birth = new Date(birthday)
      const now = new Date()
      let age = now.getFullYear() - birth.getFullYear()
      const m = now.getMonth() - birth.getMonth()
      if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) age--
      return age
    },
    async loadProfile() {
      try {
        const data = await api.getProfile()
        // 用户信息
        this.gender = data.user.gender === 1 ? '男' :
                      data.user.gender === 2 ? '女' : '未知'
        this.age = this.getAge(data.user.birthday)

        // 健康数据
        this.form.height = data.health?.height || ''
        this.form.weight = data.health?.weight || ''
        this.form.disease = data.health?.disease || []
        this.form.allergy = data.health?.allergy || []
        this.form.avoidFood = data.health?.avoidFood || []

        this.showDiseaseOther = this.form.disease.includes('其他')
        this.showAllergyOther = this.form.allergy.includes('其他')
        this.showAvoidOther = this.form.avoidFood.includes('其他')

      } catch (e) {
        console.error('加载健康档案失败', e)
      }
    },
    onDiseaseChange(e) {
      const values = e.detail.value
    
      if (values.includes('无')) {
        this.form.disease = ['无']
        this.showDiseaseOther = false
        this.form.diseaseOther = ''
        return
      }
    
      this.form.disease = values
    
      this.showDiseaseOther = values.includes('其他')
    
      if (!this.showDiseaseOther) {
        this.form.diseaseOther = ''
      }
    },
    onAllergyChange(e) {
      const values = e.detail.value
    
      if (values.includes('无')) {
        this.form.allergy = ['无']
        this.showAllergyOther = false
        this.form.allergyOther = ''
        return
      }
    
      this.form.allergy = values
    
      this.showAllergyOther = values.includes('其他')
    
      if (!this.showAllergyOther) {
        this.form.allergyOther = ''
      }
    },
    onAvoidChange(e) {
      const values = e.detail.value
    
      if (values.includes('无')) {
        this.form.avoidFood = ['无']
        this.showAvoidOther = false
        this.form.avoidOther = ''
        return
      }
    
      this.form.avoidFood = values
    
      this.showAvoidOther = values.includes('其他')
    
      if (!this.showAvoidOther) {
        this.form.avoidOther = ''
      }
    },
    async save() {
      const payload = {
        height: this.form.height,
        weight: this.form.weight,
        disease: [
          ...this.form.disease.filter(i => i !== '其他'),
          ...(this.form.diseaseOther ? [this.form.diseaseOther] : [])
        ],
        allergy: [
          ...this.form.allergy.filter(i => i !== '其他'),
          ...(this.form.allergyOther ? [this.form.allergyOther] : [])
        ],
        avoidFood: [
          ...this.form.avoidFood.filter(i => i !== '其他'),
          ...(this.form.avoidOther ? [this.form.avoidOther] : [])
        ]
      }

      try {
        await api.saveProfile(payload)
        uni.showToast({ title: '已保存', icon: 'success' })
      } catch (e) {
        console.error('保存失败', e)
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
.page-wrapper {
  width: 100%;
  min-height: 100vh;
  background: #fff;
}
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #fff;
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
.box {
  padding: 24rpx;
  margin-top: 160rpx;
}
.profile-page {
  background-color: #fff;
  min-height: 100vh;
  box-sizing: border-box;
}
.header {
  margin-top: 40rpx;
  margin-bottom: 30rpx;
}
.subtitle {
  font-size: 26rpx;
  color: #666;
  display: block;
}
.field {
  background: #f7f7f7;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.field.readonly {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.field input {
  width: 100%;
  margin-top: 12rpx;
  border: none;
  background: transparent;
  font-size: 28rpx;
}
.label {
  font-size: 28rpx;
  color: #333;
}
.multi-select .options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.btn-primary {
  width: 100% !important;
  height: 88rpx !important;
  line-height: 88rpx !important;
  background: #000 !important;
  border-radius: 50rpx !important;
  font-size: 28rpx !important;
  margin-bottom: 24rpx !important;
  border: none !important;
  color: #fff !important;
}
.btn-primary.black {
  margin-top: 40rpx;
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
</style>