<template>
  <view class="box">
    <view v-if="recipe" class="card">
      <text class="title">{{ recipe.name }}</text>
      <text class="meta"
        >每份约 {{ recipe.caloriesPerServing != null ? recipe.caloriesPerServing : '-' }} kcal · 蛋白
        {{ recipe.proteinG != null ? recipe.proteinG : '-' }}g · 脂肪 {{ recipe.fatG != null ? recipe.fatG : '-' }}g · 碳水
        {{ recipe.carbG != null ? recipe.carbG : '-' }}g</text
      >
      <view class="block" v-if="recipe.description">
        <text class="h">简介</text>
        <text class="body">{{ recipe.description }}</text>
      </view>
      <view class="block" v-if="recipe.steps">
        <text class="h">步骤</text>
        <text class="body steps">{{ recipe.steps }}</text>
      </view>
    </view>
    <view v-else class="tip">加载中或菜谱不存在</view>
  </view>
</template>

<script>
import { api } from '../../utils/request.js'

export default {
  data() {
    return { id: null, recipe: null }
  },
  onLoad(options) {
    this.id = options.id ? Number(options.id) : null
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      if (!this.id) return
      try {
        this.recipe = await api.getRecipe(this.id)
      } catch (e) {
        this.recipe = null
      }
    }
  }
}
</script>

<style scoped>
.box {
  padding: 24rpx;
}
.card {
  background: #fff;
  padding: 24rpx;
  border-radius: 12rpx;
}
.title {
  font-size: 34rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 12rpx;
}
.meta {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 20rpx;
}
.block {
  margin-top: 20rpx;
}
.h {
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}
.body {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}
.steps {
  white-space: pre-wrap;
}
.tip {
  color: #888;
  text-align: center;
  padding: 40rpx;
}
</style>
