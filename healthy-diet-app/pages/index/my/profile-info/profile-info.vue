<template>
  <view class="profile-page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-title">个人资料</view>
    </view>

    <!-- 列表区域 -->
    <view class="list-wrap">
      <!-- 头像 -->
      <view class="item" @click="showAvatarAction">
        <view class="left">
          <text class="label">头像</text>
        </view>
        <view class="right">
          <image
            :key="avatarKey"
            :src="avatarUrl"
            class="avatar-preview"
            mode="aspectFill"
          />
          <text class="arrow">></text>
        </view>
      </view>

      <!-- 昵称 -->
      <view class="item" @click="editNickname">
        <view class="left">
          <text class="label">昵称</text>
        </view>
        <view class="right">
          <text class="value">{{ nickname }}</text>
          <text class="arrow">></text>
        </view>
      </view>

      <!-- 性别 -->
      <view class="item" @click="editGender">
        <view class="left">
          <text class="label">性别</text>
        </view>
        <view class="right">
          <text class="value">{{ gender }}</text>
          <text class="arrow">></text>
        </view>
      </view>

      <!-- 邮箱 -->
      <view class="item no-border" @click="changeEmail">
        <view class="left">
          <text class="label">邮箱</text>
        </view>
        <view class="right">
          <text class="value">{{ email }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { API_BASE } from '../../../../utils/config.js'

export default {
  data() {
    return {
      avatarUrl: '/static/default-avatar.png', // 当前头像
      nickname: '未设置',
      gender: '未设置',
      email: '',
      avatarModified: false // 是否修改过头像
    }
  },

  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      this.nickname = ''
      this.avatarUrl = '/static/default-avatar.png'
      return
    }

    // 优先用本地缓存的头像（仅在修改过头像后）
    const cacheAvatar = uni.getStorageSync('avatar_' + token)
    if (cacheAvatar && this.avatarModified) {
      this.avatarUrl = cacheAvatar
    } else {
      // 第一次打开或未修改头像，从数据库获取
      this.loadUserInfo()
      this.avatarModified = false
    }
  },

  methods: {
    goBack() {
      uni.navigateBack()
    },

    // =====================
    // 获取用户资料
    // =====================
    loadUserInfo() {
      const token = uni.getStorageSync('token')
      if (!token) return

      uni.request({
        url: API_BASE + '/api/user/info',
        method: 'GET',
        header: { token },
        success: (res) => {
          console.log('用户信息：', res.data)
          if (res.data.code === 0) {
            const user = res.data.data

            // 昵称
            this.nickname = user.nickname || '未设置'

            // 性别
            const genderNum = Number(user.gender)
            this.gender =
              genderNum === 1 ? '男' :
              genderNum === 2 ? '女' : '未设置'

            // 邮箱
            this.email = user.email || user.qqEmail || '未设置'

            // 始终以数据库头像为准
            this.avatarUrl = user.avatar || '/static/default-avatar.png'

            // 缓存数据库头像，方便下次快速显示
            if (user.avatar) {
              uni.setStorageSync('avatar_' + token, user.avatar)
            }
          }
        },
        fail: (err) => {
          console.log(err)
          uni.showToast({ title: '获取资料失败', icon: 'none' })
        }
      })
    },

    // =====================
    // 保存资料
    // =====================
    saveToDB(field, value, callback) {
      const token = uni.getStorageSync('token')

      uni.request({
        url: API_BASE + '/api/user/update-profile',
        method: 'POST',
        header: { token, 'content-type': 'application/json' },
        data: { field, value },
        success: (res) => {
          console.log('更新结果：', res.data)
          if (res.data.code === 0) {
            uni.showToast({ title: '保存成功', icon: 'success' })
            if (callback) callback()
          } else {
            uni.showToast({ title: res.data.message || '保存失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络异常', icon: 'none' })
        }
      })
    },

    // =====================
    // 头像
    // =====================
    showAvatarAction() {
      uni.showActionSheet({
        itemList: ['查看头像', '拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) this.previewAvatar()
          else if (res.tapIndex === 1) this.changeAvatar('camera')
          else if (res.tapIndex === 2) this.changeAvatar('album')
        }
      })
    },

    previewAvatar() {
      if (!this.avatarUrl) return
      uni.previewImage({ urls: [this.avatarUrl] })
    },

    changeAvatar(sourceType) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: [sourceType],
        success: (res) => {
          const path = res.tempFilePaths[0]
          this.avatarUrl = path
          this.avatarModified = true

          const token = uni.getStorageSync('token')
          // 保存本地缓存，区分用户
          uni.setStorageSync('avatar_' + token, path)

          // 保存数据库
          this.saveToDB('avatar', path)

          uni.showToast({ title: '头像已更新', icon: 'success' })
        }
      })
    },

    // =====================
    // 昵称
    // =====================
    editNickname() {
      uni.showModal({
        title: '修改昵称',
        editable: true,
        placeholderText: '请输入新昵称',
        success: (res) => {
          if (res.confirm && res.content && res.content.trim()) {
            this.nickname = res.content.trim()
            this.saveToDB('nickname', this.nickname)
          }
        }
      })
    },

    // =====================
    // 性别
    // =====================
    editGender() {
      uni.showActionSheet({
        itemList: ['男', '女'],
        success: (res) => {
          const genderValue = res.tapIndex === 0 ? 1 : 2
          this.gender = genderValue === 1 ? '男' : '女'
          this.saveToDB('gender', String(genderValue))
        }
      })
    },

    // =====================
    // 邮箱
    // =====================
    changeEmail() {
      uni.showToast({ title: '换绑邮箱功能待开发', icon: 'none' })
    }
  }
}
</script>

<style scoped>
/* 你的样式 100% 原样没动 */
.profile-page {
  background-color: #ffffff;
  min-height: 100vh;
}
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
  left: 40rpx;
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
.list-wrap {
  padding-top: 180rpx;
  background-color: #ffffff;
}
.item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 140rpx;
  padding: 0 40rpx;
  background-color: #f7f7f7;
  border-bottom: 1rpx solid #e8e8e8;
}
.item.no-border {
  border-bottom: none;
}
.left .label {
  font-size: 32rpx;
  color: #000;
}
.right {
  display: flex;
  align-items: center;
}
.avatar-preview {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}
.value {
  font-size: 32rpx;
  color: #999;
  margin-right: 10rpx;
}
.arrow {
  font-size: 36rpx;
  color: #999;
}
</style>