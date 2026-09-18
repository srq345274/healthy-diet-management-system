<template>
  <view class="choose-page">
    <!-- 顶部标题 -->
    <view class="header">
      <text class="title">小饭卡</text>
      <text class="subtitle">请选择登录方式</text>
    </view>

    <!-- 登录按钮 -->
    <view class="btn-box">
      <button class="btn-primary" @click="goToAccountLogin">
        账号登录 / 注册
      </button>

      <!-- 纯获取手机号按钮，不嵌套任何逻辑 -->
      <button 
        class="btn-primary" 
        open-type="getPhoneNumber" 
        @getphonenumber="getWxPhoneNumber"
      >
        微信授权登录
      </button>
    </view>

    <!-- 昵称设置弹窗 -->
    <view class="popup" v-if="showNickPopup">
      <view class="popup-content">
        <text class="popup-title">设置昵称</text>
        <input class="nick-input" v-model="newNickname" placeholder="请输入昵称" />
        <view class="popup-btn-box">
          <button class="popup-btn secondary" @click="useWxNick">使用微信昵称</button>
          <button class="popup-btn primary" @click="saveNick">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../../utils/config.js'
export default {
  data() {
    return {
      showNickPopup: false,
      newNickname: "",
      wxUserInfo: {},
      token: ""
    };
  },
  methods: {
    // 账号密码登录
    goToAccountLogin() {
      uni.navigateTo({
        url: "/pages/login/login"
      });
    },

    // ====================== 【正确】微信获取手机号 ======================
    async getWxPhoneNumber(e) {
      try {
        // 1. 用户取消
        if (e.detail.errMsg !== "getPhoneNumber:ok") {
          uni.showToast({ title: "已取消授权", icon: "none" });
          return;
        }

        // 2. 先拿 wx.login code
        const loginRes = await uni.login();
        const code = loginRes.code;
        if (!code) throw new Error("登录失败");

        // 3. 调用后端【微信快捷登录】接口
        const { data } = await uni.request({
          url: API_BASE + "/api/user/wx/phone-login",
          method: "POST",
          header: {
            "content-type": "application/json"
          },
          data: {
            code: code,
            encryptedData: e.detail.encryptedData,
            iv: e.detail.iv
          }
        });

        console.log("微信登录返回：", data);
        if (data.code !== 0) {
          uni.showToast({ title: data.message || "登录失败", icon: "none" });
          return;
        }

        // 4. 登录成功 → 拿到 token
        this.token = data.data.token;

        // 5. 获取微信头像昵称
        const userProfile = await uni.getUserProfile({ desc: "用于完善资料" });
        this.wxUserInfo = userProfile.userInfo;
        this.newNickname = userProfile.userInfo.nickName;

        // 6. 弹出昵称框
        this.showNickPopup = true;

      } catch (err) {
        console.error(err);
        uni.showToast({ title: "授权失败", icon: "none" });
      }
    },

    // 使用微信昵称
    useWxNick() {
      this.newNickname = this.wxUserInfo.nickName;
    },

    // 保存昵称
    async saveNick() {
      if (!this.newNickname.trim()) {
        uni.showToast({ title: "请输入昵称", icon: "none" });
        return;
      }

      try {
        await uni.request({
          url: API_BASE + "/api/user/update-nickname",
          method: "POST",
          header: {
            "token": this.token,
            "content-type": "application/json"
          },
          data: { nickname: this.newNickname.trim() }
        });

        // 保存本地
        uni.setStorageSync("token", this.token);
        uni.setStorageSync("nickname", this.newNickname);
        uni.setStorageSync("avatar", this.wxUserInfo.avatarUrl);

        uni.showToast({ title: "登录成功", icon: "success" });
        this.showNickPopup = false;

        setTimeout(() => {
          uni.navigateBack({ delta: 1 });
        }, 1000);

      } catch (e) {
        uni.showToast({ title: "保存失败", icon: "none" });
      }
    }
  }
};
</script>

<style scoped>
.choose-page {
  background-color: #fff;
  min-height: 100vh;
  padding: 100rpx 40rpx;
  box-sizing: border-box;
}
.header {
  margin-bottom: 100rpx;
  text-align: center;
}
.title {
  font-size: 60rpx;
  font-weight: bold;
  color: #000;
  display: block;
  margin-bottom: 20rpx;
}
.subtitle {
  font-size: 26rpx;
  color: #666;
}
.btn-box {
  width: 100%;
}
.btn-primary {
  width: 100% !important;
  height: 88rpx !important;
  line-height: 88rpx !important;
  background: #000 !important;
  border-radius: 50rpx !important;
  font-size: 28rpx !important;
  margin-bottom: 30rpx !important;
  border: none !important;
  padding: 0 !important;
  color: #fff !important;
}

.popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.popup-content {
  background: #fff;
  border-radius: 20rpx;
  width: 80%;
  padding: 40rpx;
  box-sizing: border-box;
}
.popup-title {
  font-size: 32rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30rpx;
  display: block;
}
.nick-input {
  width: 100%;
  height: 88rpx;
  background: #f7f7f7;
  border-radius: 50rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  border: none;
  margin-bottom: 30rpx;
}
.popup-btn-box {
  display: flex;
  gap: 20rpx;
}
.popup-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 50rpx;
  font-size: 26rpx;
  border: none;
}
.secondary {
  background: #f2f2f2;
  color: #333;
}
.primary {
  background: #000;
  color: #fff;
}
</style>