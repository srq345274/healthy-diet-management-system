<template>
  <view class="register-page">
    <view class="header">
      <text class="title">注册账号</text>
      <text class="subtitle">创建你的健康饮食账户</text>
    </view>

    <view class="form">
      <view class="input-group">
        <text class="label">昵称</text>
        <input
          class="inp"
          :value="nickname"
          placeholder="请输入昵称"
          @input="onNickInput"
        />
      </view>

      <view class="input-group">
        <text class="label">生日</text>
        <picker mode="date" :value="birthday" @change="onBirthdayChange">
          <view class="picker-input">
            <text class="picker-text">
              {{ birthday || '请选择生日' }}
            </text>
            <text class="picker-arrow">></text>
          </view>
        </picker>
      </view>

      <view class="input-group">
        <text class="label">邮箱</text>
        <input
          class="inp"
          :value="email"
          placeholder="请输入邮箱"
          @input="onEmailInput"
        />
      </view>

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

      <view class="input-group">
        <text class="label">密码</text>

        <input
          class="inp"
          :value="password"
          password
          placeholder="请设置密码"
          @input="onPwdInput"
        />

        <text
          class="tip-red"
          v-if="passwordError"
        >
          {{ passwordError }}
        </text>
      </view>

      <button
        class="btn-primary"
        type="primary"
        @click="submit"
      >
        注册
      </button>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../utils/config.js'

export default {
  data() {
    return {
      nickname: '',
      birthday: '',
      email: '',
      code: '',
      password: '',
      passwordError: '',
      codeCountdown: 0,
      timer: null
    }
  },

  watch: {
    password(val) {
      this.passwordError = val.length < 6 ? '密码至少6位' : ''
    }
  },

  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },

  methods: {
    onNickInput(e) { this.nickname = e.detail.value },
    onEmailInput(e) { this.email = e.detail.value },
    onCodeInput(e) { this.code = e.detail.value },
    onPwdInput(e) { this.password = e.detail.value },
    onBirthdayChange(e) { this.birthday = e.detail.value },

    resetCountdown() {
      this.codeCountdown = 0
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    },

    // 发送验证码
    async sendCode() {
      if (this.codeCountdown > 0) return

      const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
      if (!emailReg.test(this.email)) {
        uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
        return
      }

      try {
        // 检查邮箱是否已注册
        const [checkErr, checkRes] = await uni.request({
          url: API_BASE + '/api/email/check-email-exists',
          method: 'POST',
          header: { 'content-type': 'application/json' }, // 不带 token
          data: { email: this.email }
        })

        if (checkErr) {
          uni.showToast({ title: '网络异常', icon: 'none' })
          return
        }

        if (checkRes.data.code === 0 && checkRes.data.data === true) {
          uni.showToast({ title: '该邮箱已注册', icon: 'none' })
          return
        }

        // 开始倒计时
        this.codeCountdown = 60
        this.timer = setInterval(() => {
          this.codeCountdown--
          if (this.codeCountdown <= 0) this.resetCountdown()
        }, 1000)

        // 发送验证码
        uni.request({
          url: API_BASE + '/api/email/send-code',
          method: 'POST',
          header: {
            'content-type': 'application/json'
          },
          data: {
            email: this.email
          }
        })

      } catch (e) {
        uni.showToast({ title: '网络异常', icon: 'none' })
      }
    },

    // 注册
    async submit() {
      if (!this.nickname) { uni.showToast({ title: '请输入昵称', icon: 'none' }); return }
      if (!this.email) { uni.showToast({ title: '请输入邮箱', icon: 'none' }); return }
      if (!this.code) { uni.showToast({ title: '请输入验证码', icon: 'none' }); return }
      if (!this.password || this.password.length < 6) { this.passwordError = '密码至少6位'; return }

      uni.showLoading({ title: '注册中...' })

      try {
        const [err, res] = await uni.request({
          url: API_BASE + '/api/auth/email-register',
          method: 'POST',
          header: { 'content-type': 'application/json' }, // ⚠️ 不带 token
          data: {
            email: this.email,
            code: this.code,
            nickname: this.nickname,
            password: this.password,
            birthday: this.birthday
          }
        })

        uni.hideLoading()

        if (res.data.code === 0 && res.data.data) {
          const user = res.data.data
          uni.setStorageSync('token', user.token)
          uni.setStorageSync('userId', user.userId)
          uni.setStorageSync('nickname', user.nickname)

          uni.showToast({ title: '注册成功', icon: 'success' })
          setTimeout(() => uni.reLaunch({ url: '/pages/index/my/my' }), 1000)

        } else {
          uni.showToast({ title: res.data.message || '注册失败', icon: 'none' })
        }

      } catch (e) {
        uni.hideLoading()
        console.error(e)
        uni.showToast({ title: '网络异常', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.register-page {
  background-color: #fff;
  min-height: 100vh;
  padding: 100rpx 40rpx;
  box-sizing: border-box;
}

.header {
  margin-bottom: 60rpx;
}

.title {
  font-size: 60rpx;
  font-weight: bold;
  color: #000;
  margin-bottom: 20rpx;
  display: block;
}

.subtitle {
  font-size: 26rpx;
  color: #666;
  display: block;
}

.form {
  width: 100%;
}

.input-group {
  margin-bottom: 40rpx;
}

.label {
  font-size: 30rpx;
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

.picker-input {
  width: 100%;
  height: 88rpx;
  background: #f7f7f7;
  border-radius: 50rpx;
  padding: 0 30rpx;
  box-sizing: border-box;
  border: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.picker-text {
  font-size: 28rpx;
  color: #333;
}

.picker-arrow {
  font-size: 28rpx;
  color: #999;
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

.tip-red {
  font-size: 24rpx;
  color: red;
  text-align: right;
  display: block;
  margin-top: 8rpx;
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