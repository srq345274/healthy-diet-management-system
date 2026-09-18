import { API_BASE } from './config.js'

function getToken() {
  try {
    return uni.getStorageSync('token') || ''
  } catch (e) {
    return ''
  }
}

export function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    needAuth = true,
    silent = false,
    timeout = 60000
  } = options

  const header = {
    'Content-Type': 'application/json'
  }

  if (needAuth) {
    const t = getToken()

    console.log('当前token=', t)

    if (t) {
      // 新后端需要
      header.token = t

      // 兼容旧接口
      header.Authorization = 'Bearer ' + t
    }
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: API_BASE.replace(/\/$/, '') + url,
      method,
      data,
      header,
      timeout,

      success(res) {
        console.log('请求地址=', url)
        console.log('响应=', res)
      
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.showToast({
            title: '请重新登录',
            icon: 'none'
          })
          reject(new Error('401'))
          return
        }
      
        const body = res.data
      
        // 直接返回数组
        if (Array.isArray(body)) {
          resolve(body)
          return
        }
      
        // 直接返回对象（没有code字段）
        if (body && body.code === undefined) {
          resolve(body)
          return
        }
      
        // ApiResponse格式
        if (body && body.code === 0) {
          resolve(body.data)
          return
        }
      
        const msg = (body && body.message) || '请求失败'
      
        if (!silent) {
          uni.showToast({
            title: msg,
            icon: 'none'
          })
        }
      
        reject(new Error(msg))
      },

      fail(err) {
        console.error('请求失败=', err)

        const msg = '网络错误，请检查后端是否启动、IP是否正确'

        if (!silent) {
          uni.showToast({
            title: msg,
            icon: 'none'
          })
        }

        reject(new Error(msg))
      }
    })
  })
}

export const api = {
  login(payload) {
    return request({
      url: '/auth/login',
      method: 'POST',
      data: payload,
      needAuth: false
    })
  },
  
  saveDietRecord(payload) {
    return request({
      url: '/diet-records/save',
      method: 'POST',
      data: payload
    })
  },

  register(payload) {
    return request({
      url: '/auth/register',
      method: 'POST',
      data: payload,
      needAuth: false
    })
  },

  getProfile() {
    return request({
      url: '/profile',
      method: 'GET'
    })
  },

  saveProfile(payload) {
    return request({
      url: '/profile',
      method: 'PUT',
      data: payload
    })
  },

  addDietRecord(payload) {
    return request({
      url: '/diet-records',
      method: 'POST',
      data: payload
    })
  },

  dietDay(date) {
    return request({
      url: '/diet-records/day?date=' + date,
      method: 'GET'
    })
  },

  dietSummary(date) {
    return request({
      url: '/diet-records/summary-day?date=' + date,
      method: 'GET'
    })
  },

  recognizePreview(imageBase64) {
    return request({
      url: '/diet-records/recognize-preview',
      method: 'POST',
      data: { imageBase64 }
    })
  },

  processFood(imageBase64) {
    return request({
      url: '/diet-records/process-food',
      method: 'POST',
      data: { imageBase64 },
      silent: true,
      timeout: 120000
    })
  },

  dietFromPhoto(payload) {
    return request({
      url: '/diet-records/from-photo',
      method: 'POST',
      data: payload
    })
  },

  listRecipes(keyword) {
    const q = keyword
      ? ('?keyword=' + encodeURIComponent(keyword))
      : ''

    return request({
      url: '/recipes' + q,
      method: 'GET',
      needAuth: false
    })
  },

  getRecipe(id) {
    return request({
      url: '/recipes/' + id,
      method: 'GET',
      needAuth: false
    })
  },

  listMealPlans() {
    return request({
      url: '/meal-plans',
      method: 'GET'
    })
  },

  createMealPlan(payload) {
    return request({
      url: '/meal-plans',
      method: 'POST',
      data: payload
    })
  },

  aiMealDraft(requirement, excludedIngredients) {
    return request({
      url: '/ai/meal-plan-draft',
      method: 'POST',
      data: {
        requirement,
        excludedIngredients: excludedIngredients || ''
      }
    })
  },

  seasonalAdvice(payload) {
    return request({
      url: '/ai/seasonal-advice',
      method: 'POST',
      data: payload || {}
    })
  },

  updateMealPlan(id, payload) {
    return request({
      url: '/meal-plans/' + id,
      method: 'PUT',
      data: payload
    })
  },

  aiAdvice(question, useProfile) {
    return request({
      url: '/ai/nutrition-advice',
      method: 'POST',
      data: {
        question,
        useProfile
      }
    })
  },

  listShopping() {
    return request({
      url: '/shopping-lists',
      method: 'GET'
    })
  },

  createShopping(payload) {
    return request({
      url: '/shopping-lists',
      method: 'POST',
      data: payload
    })
  },

  shoppingFromMealPlan(mealPlanId) {
    return request({
      url: '/shopping-lists/from-meal-plan/' + mealPlanId,
      method: 'POST',
      data: {}
    })
  },

  checkIn(payload) {
    return request({
      url: '/check-ins',
      method: 'POST',
      data: payload
    })
  },

  streak() {
    return request({
      url: '/check-ins/streak',
      method: 'GET'
    })
  },

  badgeCatalog() {
    return request({
      url: '/check-ins/badges/catalog',
      method: 'GET'
    })
  },

  weightTrend(from, to) {
    return request({
      url: '/statistics/weight-trend?from=' + from + '&to=' + to,
      method: 'GET'
    })
  },

  nutritionByDay(from, to) {
    return request({
      url: '/statistics/nutrition-by-day?from=' + from + '&to=' + to,
      method: 'GET'
    })
  },

  weeklyReport(weekStart) {
    return request({
      url: '/statistics/weekly-report?weekStart=' + weekStart,
      method: 'GET'
    })
  },

  monthlyReport(year, month) {
    return request({
      url: '/statistics/monthly-report?year=' + year + '&month=' + month,
      method: 'GET'
    })
  },

  nutritionRiskAlerts(from, to) {
    return request({
      url: '/statistics/nutrition-risk-alerts?from=' + from + '&to=' + to,
      method: 'GET'
    })
  },

  dietPattern(from, to) {
    return request({
      url: '/statistics/diet-pattern?from=' + from + '&to=' + to,
      method: 'GET'
    })
  },

  articles(category) {
    const q = category
      ? ('?category=' + encodeURIComponent(category))
      : ''

    return request({
      url: '/articles' + q,
      method: 'GET',
      needAuth: false
    })
  },

  articleDetail(id) {
    return request({
      url: '/articles/' + id,
      method: 'GET',
      needAuth: false
    })
  },

  searchIngredients(keyword) {
    return request({
      url: '/ingredients/search?keyword=' + encodeURIComponent(keyword),
      method: 'GET',
      needAuth: false
    })
  },

  addMetric(payload) {
    return request({
      url: '/health-metrics',
      method: 'POST',
      data: payload
    })
  },
    weightTrend(from, to) {
      return request({
        url: '/statistics/weight-trend?from=' + from + '&to=' + to,
        method: 'GET'
      })
    },
  
    nutritionByDay(from, to) {
      return request({
        url: '/statistics/nutrition-by-day?from=' + from + '&to=' + to,
        method: 'GET'
      })
    },
  
    // ===== 新增 =====
    nutritionSummary(from, to) {
      return request({
        url: '/statistics/nutrition-summary?from=' + from + '&to=' + to,
        method: 'GET'
      })
    },
  
    // ===== 新增 =====
    latestWeight() {
      return request({
        url: '/statistics/latest-weight',
        method: 'GET'
      })
    },
  
    weeklyReport(weekStart) {
      return request({
        url: '/statistics/weekly-report?weekStart=' + weekStart,
        method: 'GET'
      })
    },
    
	weekCalorie() {
	  return request({
	    url: '/food-list/week-calorie',
	    method: 'GET'
	  })
	},
	
    monthlyReport(year, month) {
      return request({
        url: '/statistics/monthly-report?year=' + year + '&month=' + month,
        method: 'GET'
      })
    },
	getWeekPlan(startDate, endDate) {
	  return request({
	    url: '/plan/week',
	    method: 'GET',
	    data: { startDate, endDate }
	  })
	},
	
	getIngredients(recipeIds) {
	  return request({
	    url: '/ingredient/list',
	    method: 'GET',
	    data: {
	      recipeIds
	    }
	  })
	},
	
	// 获取当日食谱计划
	getDayPlan(date) {
	  const token = getToken()
	  
	
	  return new Promise((resolve, reject) => {
	    uni.request({
	      url: API_BASE.replace(/\/$/, '') + '/api/shop/day-plan',
	      method: 'GET',
	      data: { date },
	
	      header: {
	        'Content-Type': 'application/json',
	        token: token
	      },
	
	      success(res) {
	
	        console.log('day-plan=', res)
	
	        if (res.statusCode === 401) {
	          uni.removeStorageSync('token')
	
	          uni.showToast({
	            title: '请重新登录',
	            icon: 'none'
	          })
	
	          reject(new Error('401'))
	          return
	        }
	
	        resolve(res.data)
	      },
	
	      fail(err) {
	        reject(err)
	      }
	    })
	  })
	},
	
	// 根据菜品ID批量获取食材
	getIngredientsByRecipeIds(recipeIds) {
	  return request({
	    url: '/api/shop/ingredients',
	    method: 'POST',
	    data: recipeIds
	  })
	},
	
	refreshPlan(date) {
	  return request({
	    url:'/plan/refresh',
	    method:'POST',
	    data:{ date }
	  })
	},
	getAllRecipes() {
	  return request({
	    url: '/recipe/all',
	    method: 'GET'
	  })
	},
	updateMeal({ date, mealType, recipeIds }, token) {
	  return request({
	    url: '/plan/update',
	    method: 'POST',
	    header: { token },
	    data: { date, mealType, recipeIds }
	  })
	}
	
}