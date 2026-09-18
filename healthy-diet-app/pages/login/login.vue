<template>
  <view class="login-page">
    <view class="header">
      <text class="title">欢迎回来</text>
      <text class="subtitle">登录你的健康饮食账户</text>
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

      <!-- 密码 / 验证码 切换 -->
      <view class="input-group">
        <text class="label">{{ loginType === 'password' ? '密码' : '验证码' }}</text>
        <view class="input-row">
          <input 
            class="inp" 
            :value="loginType === 'password' ? password : code"
            :password="loginType === 'password'"
            :placeholder="loginType === 'password' ? '请输入密码' : '请输入验证码'"
            @input="onCodeOrPasswordInput"
          />
          
          <button 
            v-if="loginType === 'password'" 
            class="forgot-btn" 
            @click="goForgot"
            plain
          >
            忘记密码？
          </button>

          <button 
            v-else 
            class="get-code-btn" 
            @click="sendCode"
            :class="{ disabled: codeCountdown > 0 }"
            plain
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>

      <button class="btn-primary" type="primary" @click="submit">登录</button>

      <view class="bottom-row">
        <text class="switch-btn" @click="toggleLoginType">
          {{ loginType === 'password' ? '验证码登录' : '账号密码登录' }}
        </text>
        <text class="reg-btn" @click="goReg">注册</text>
      </view>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../utils/config.js'
export default {
  data() {
    return {
      email: '',
      password: '',
      code: '',
      loginType: 'password',
      codeCountdown: 0
    }
  },

  methods: {
    onEmailInput(e) {
      this.email = e.detail.value
    },

    onCodeOrPasswordInput(e) {
      if (this.loginType === 'password') {
        this.password = e.detail.value
      } else {
        this.code = e.detail.value
      }
    },

    toggleLoginType() {
      this.loginType = this.loginType === 'password' ? 'code' : 'password'
    },

    async sendCode() {
      if (this.codeCountdown > 0) return
      const emailReg = /^[^\s]+@[^\s]+\.[^\s]+$/
      if (!emailReg.test(this.email)) {
        uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
        return
      }

      // 检查邮箱是否存在
      try {
        const checkRes = await uni.request({
          url: API_BASE + "/api/email/check-email-exists",
          method: "POST",
          data: { email: this.email },
          header: { "Content-Type": "application/json" }
        });

        if (checkRes[1].data.data === false) {
          uni.showToast({ title: "该邮箱未注册", icon: "none" });
          return;
        }
      } catch (e) {}

      // 发送验证码
      uni.request({
        url: API_BASE + "/api/email/send-code",
        method: "POST",
        data: {
          email: this.email
        },
        header: {
          "Content-Type": "application/json"
        },
        success: (res) => {
          if (res.data.code === 0) {
      
            // 直接开始倒计时，不弹任何提示
            this.codeCountdown = 60
      
            const timer = setInterval(() => {
              this.codeCountdown--
      
              if (this.codeCountdown <= 0) {
                clearInterval(timer)
              }
            }, 1000)
      
          } else {
            uni.showToast({
              title: res.data.message || "发送失败",
              icon: "none"
            })
          }
        },
        fail: () => {
          uni.showToast({
            title: "网络异常",
            icon: "none"
          })
        }
      })
    },

    goForgot() {
      uni.navigateTo({ url: '/pages/login/forgot/forgot' })
    },

    // ✅ 修复：正确传 qqEmail，不再报用户不存在
    async submit() {
      if (!this.email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return
      }
      if (this.loginType === 'password' && !this.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      if (this.loginType === 'code' && !this.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }
    
      uni.showLoading({ title: '登录中...' })
    
      try {
        let res;
        if (this.loginType === 'password') {
          // ✅ 正确：后端要的是 email，不是 qqEmail！
          res = await uni.request({
            url: API_BASE + "/api/auth/email-pwd-login",
            method: "POST",
            data: {
              email: this.email,    // 👈 👈 👈 改这里！！！
              password: this.password
            },
            header: { "Content-Type": "application/json" }
          });
        } else {
          // ✅ 正确：后端要的是 email，不是 qqEmail！
          res = await uni.request({
            url: API_BASE + "/api/auth/email-code-login",
            method: "POST",
            data: {
              email: this.email,   // 👈 👈 👈 改这里！！！
              code: this.code
            },
            header: { "Content-Type": "application/json" }
          });
        }
    
        const data = res[1].data;
        if (data.code === 0 && data.data) {
          uni.setStorageSync('token', data.data.token);
          uni.showToast({ title: '登录成功', icon: 'success' });
          setTimeout(() => {
            uni.reLaunch({ url: "/pages/index/index" });
          }, 800);
        } else {
          uni.showToast({ title: data.message || '登录失败', icon: 'none' });
        }
      } catch (err) {
        uni.showToast({ title: '网络异常', icon: 'none' });
      } finally {
        uni.hideLoading();
      }
    },

    goReg() {
      uni.navigateTo({ url: '/pages/register/register' })
    }
  }
}
</script>

<style>
.login-page { background-color: #fff; min-height: 100vh; padding: 100rpx 40rpx; box-sizing: border-box }
.header { margin-bottom: 60rpx }
.title { font-size: 60rpx; font-weight: bold; color: #000; margin-bottom: 20rpx; display: block }
.subtitle { font-size: 26rpx; color: #666; display: block }
.form { width: 100% }
.input-group { margin-bottom: 40rpx }
.label { font-size: 35rpx; color: #333; margin-bottom: 10rpx; display: block }
.inp { width: 100%; height: 88rpx; background: #f7f7f7; border-radius: 50rpx !important; padding: 0 30rpx; font-size: 28rpx; box-sizing: border-box; border: none }
.input-row { position: relative }
.forgot-btn { position: absolute; right: 24rpx; top: 50%; transform: translateY(-50%); font-size: 26rpx; color: #000 !important; border: none !important; background: transparent !important; padding: 0; margin: 0; z-index: 999; line-height: 1 }
.forgot-btn::after { border: none !important }
.get-code-btn { position: absolute; right: 24rpx; top: 50%; transform: translateY(-50%); font-size: 26rpx; color: #000; z-index: 999; background: transparent !important; border: none !important; padding: 0; margin: 0; line-height: 1 }
.get-code-btn::after { border: none !important }
.get-code-btn.disabled { color: #999 }
.btn-primary { width: 100% !important; height: 88rpx !important; line-height: 88rpx !important; background: #000 !important; border-radius: 50rpx !important; font-size: 28rpx !important; margin-top: 40rpx !important; border: none !important; color: #fff !important }
.bottom-row { display: flex; justify-content: space-between; margin-top: 40rpx; font-size: 26rpx; color: #666 }
.switch-btn, .reg-btn { color: #000 }
</style>