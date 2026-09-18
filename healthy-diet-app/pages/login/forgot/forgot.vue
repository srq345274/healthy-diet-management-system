<template>
  <view class="forgot-page">
    <view class="header">
      <text class="title">找回密码</text>
      <text class="subtitle">重置你的登录密码</text>
    </view>

    <view class="form">
      <!-- 邮箱 -->
      <view class="input-group">
        <text class="label">邮箱</text>
        <input
          class="inp"
          :value="email"
          placeholder="请输入邮箱"
          @input="onEmailInput"
        />
      </view>

      <!-- 验证码 -->
      <view class="input-group">
        <text class="label">验证码</text>
        <view class="input-row">
          <input
            class="inp"
            :value="code"
            placeholder="请输入验证码"
            @input="onCodeInput"
          />

          <button
            class="get-code-btn"
            @click="sendCode"
            :class="{ disabled: codeCountdown > 0 }"
            plain
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>

      <!-- 新密码 -->
      <view class="input-group">
        <text class="label">新密码</text>

        <view class="input-row">
          <input
            class="inp"
            :value="newPassword"
            :password="!showPassword"
            placeholder="请输入新密码"
            @input="onPasswordInput"
          />

          <button
            class="eye-btn"
            @click="toggleShowPassword"
            plain
          >
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </view>
      </view>

      <button
        class="btn-primary"
        type="primary"
        @click="submit"
      >
        确认重置
      </button>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../../utils/config.js'

export default {
  data() {
    return {
      email: '',
      code: '',
      newPassword: '',
      showPassword: false,
      codeCountdown: 0,
      timer: null
    }
  },

  methods: {
    onEmailInput(e) { this.email = e.detail.value },
    onCodeInput(e) { this.code = e.detail.value },
    onPasswordInput(e) { this.newPassword = e.detail.value },

    toggleShowPassword() { this.showPassword = !this.showPassword },

    resetCountdown() {
      this.codeCountdown = 0
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    },

    async sendCode() {
      if (this.codeCountdown > 0) return
      const emailReg = /^[^\s]+@[^\s]+\.[^\s]+$/
      if (!emailReg.test(this.email)) { uni.showToast({ title: '请输入正确邮箱', icon: 'none' }); return }

      try {
        // 检查邮箱是否注册
        const [checkErr, checkRes] = await uni.request({
          url: API_BASE + '/api/email/check-email-exists',
          method: 'POST',
          header: { 'content-type': 'application/json' },
          data: { email: this.email }
        })

        if (checkErr) { uni.showToast({ title: '网络异常', icon: 'none' }); return }
        if (checkRes.data.code === 0 && checkRes.data.data === false) {
          uni.showToast({ title: '该邮箱尚未注册', icon: 'none' })
          return
        }

        // 倒计时
        this.codeCountdown = 60
        this.timer = setInterval(() => {
          this.codeCountdown--
          if (this.codeCountdown <= 0) this.resetCountdown()
        }, 1000)

        uni.request({
          url: API_BASE + '/api/email/send-code',
          method: 'POST',
          header: { 'content-type': 'application/x-www-form-urlencoded' },
          data: { email: this.email },
          success: (res) => {
            if (res.data.code === 0) {}
            else { uni.showToast({ title: res.data.message || '发送失败', icon: 'none' }); this.resetCountdown() }
          },
          fail: () => { uni.showToast({ title: '发送失败', icon: 'none' }); this.resetCountdown() }
        })

      } catch (e) { uni.showToast({ title: '网络异常', icon: 'none' }) }
    },

    async submit() {
      const emailReg = /^[^\s]+@[^\s]+\.[^\s]+$/
      if (!emailReg.test(this.email)) { uni.showToast({ title: '请输入正确邮箱', icon: 'none' }); return }
      if (!this.code) { uni.showToast({ title: '请输入验证码', icon: 'none' }); return }
      if (!this.newPassword) { uni.showToast({ title: '请输入新密码', icon: 'none' }); return }
      if (this.newPassword.length < 6) { uni.showToast({ title: '密码至少6位', icon: 'none' }); return }

      uni.showLoading({ title: '提交中...' })

      try {
        const [err, res] = await uni.request({
          url: API_BASE + '/api/auth/email-reset-pwd',
          method: 'POST',
          header: { 'content-type': 'application/json', token: '' }, // ⚠️ 不带 token
          data: {
            email: this.email,
            code: this.code,
            newPassword: this.newPassword
          }
        })

        uni.hideLoading()
        if (err) { uni.showToast({ title: '网络异常', icon: 'none' }); return }

        if (res.data.code === 0) {
          uni.showToast({ title: '密码重置成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 1500)
        } else { uni.showToast({ title: res.data.message || '重置失败', icon: 'none' }) }

      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '网络异常', icon: 'none' })
      }
    }
  },

  onUnload() { if (this.timer) { clearInterval(this.timer); this.timer = null } }
}
</script>

<style scoped>
.forgot-page {
  background-color: #fff;
  min-height: 100vh;
  padding: 100rpx 40rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 60rpx;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 60rpx;
  font-weight: bold;
  color: #000;
  margin-bottom: 12rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #666;
}

.form {
  width: 100%;
}

.input-group {
  margin-bottom: 40rpx;
}

.label {
  font-size: 35rpx;
  color: #333;
  margin-bottom: 10rpx;
  display: block;
}

.inp {
  width: 100%;
  height: 88rpx;
  background: #f7f7f7;
  border-radius: 50rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  border: none;
}

.input-row {
  position: relative;
}

.get-code-btn {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 26rpx;
  color: #000 !important;
  background: transparent !important;
  border: none !important;
  padding: 0;
  z-index: 999;
}

.get-code-btn.disabled {
  color: #999 !important;
}

.get-code-btn::after {
  border: none !important;
}

.eye-btn {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 26rpx;
  color: #000 !important;
  background: transparent !important;
  border: none !important;
  padding: 0;
  z-index: 999;
}

.eye-btn::after {
  border: none !important;
}

.btn-primary {
  width: 100% !important;
  height: 88rpx !important;
  line-height: 88rpx !important;
  background: #000 !important;
  border-radius: 50rpx !important;
  font-size: 28rpx !important;
  margin-top: 40rpx !important;
  border: none !important;
  color: #fff !important;
}
</style>