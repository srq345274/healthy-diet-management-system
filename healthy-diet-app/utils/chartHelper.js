/**
 * 轻量 Canvas 图表绘制（适配 uni-app 微信小程序）
 */

function getCanvasSize() {
  const sys = uni.getSystemInfoSync()
  const width = sys.windowWidth - 48
  const height = Math.round(width * 0.55)
  return { width, height, dpr: sys.pixelRatio || 1 }
}

function niceRange(min, max, padding = 0.08) {
  if (min === max) {
    const delta = min === 0 ? 1 : Math.abs(min) * 0.1
    return { min: min - delta, max: max + delta }
  }
  const span = max - min
  const pad = span * padding
  return { min: min - pad, max: max + pad }
}

function drawGrid(ctx, padding, chartW, chartH, yMin, yMax, steps = 4) {
  ctx.setStrokeStyle('#eee')
  ctx.setLineWidth(1)
  ctx.setFontSize(10)
  ctx.setFillStyle('#999')

  for (let i = 0; i <= steps; i++) {
    const ratio = i / steps
    const y = padding.top + chartH * ratio
    const val = yMax - (yMax - yMin) * ratio

    ctx.beginPath()
    ctx.moveTo(padding.left, y)
    ctx.lineTo(padding.left + chartW, y)
    ctx.stroke()

    ctx.fillText(val.toFixed(1), 4, y + 4)
  }
}

function mapPoints(values, padding, chartW, chartH, yMin, yMax) {
  const count = values.length
  if (count === 0) return []

  return values.map((val, i) => {
    const x = count === 1
      ? padding.left + chartW / 2
      : padding.left + (chartW * i) / (count - 1)
    const ratio = (val - yMin) / (yMax - yMin || 1)
    const y = padding.top + chartH * (1 - ratio)
    return { x, y, val }
  })
}

function drawXLabels(ctx, labels, points, padding, chartH) {
  ctx.setFontSize(9)
  ctx.setFillStyle('#999')
  const step = Math.max(1, Math.ceil(labels.length / 5))

  points.forEach((pt, i) => {
    if (i % step !== 0 && i !== labels.length - 1) return
    const text = labels[i].length > 5 ? labels[i].slice(5) : labels[i]
    ctx.fillText(text, pt.x - 16, padding.top + chartH + 18)
  })
}

export function drawLineChart(canvasId, vm, options = {}) {
  const { width, height } = getCanvasSize()
  const padding = { top: 24, right: 16, bottom: 36, left: 40 }
  const chartW = width - padding.left - padding.right
  const chartH = height - padding.top - padding.bottom

  const labels = options.labels || []
  const values = (options.values || []).map(Number)
  if (!values.length) return

  const { min: yMin, max: yMax } = niceRange(
    Math.min(...values),
    Math.max(...values)
  )

  const ctx = uni.createCanvasContext(canvasId, vm)
  ctx.clearRect(0, 0, width, height)

  drawGrid(ctx, padding, chartW, chartH, yMin, yMax)
  const points = mapPoints(values, padding, chartW, chartH, yMin, yMax)

  const lineColor = options.color || '#333'

  ctx.setStrokeStyle(lineColor)
  ctx.setLineWidth(2)
  ctx.beginPath()
  points.forEach((pt, i) => {
    if (i === 0) ctx.moveTo(pt.x, pt.y)
    else ctx.lineTo(pt.x, pt.y)
  })
  ctx.stroke()

  ctx.setFillStyle(lineColor)
  points.forEach(pt => {
    ctx.beginPath()
    ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2)
    ctx.fill()
  })

  drawXLabels(ctx, labels, points, padding, chartH)
  ctx.draw()
}

export function drawBarChart(canvasId, vm, options = {}) {
  const { width, height } = getCanvasSize()
  const padding = { top: 24, right: 16, bottom: 40, left: 40 }
  const chartW = width - padding.left - padding.right
  const chartH = height - padding.top - padding.bottom

  const labels = options.labels || []
  const values = (options.values || []).map(Number)
  if (!values.length) return

  const maxVal = Math.max(...values.map(v => Math.abs(v)), 1)
  const yMax = maxVal * 1.15
  const yMin = options.signed ? -yMax : 0

  const ctx = uni.createCanvasContext(canvasId, vm)
  ctx.clearRect(0, 0, width, height)

  drawGrid(ctx, padding, chartW, chartH, yMin, yMax)

  const barCount = values.length
  const gap = 8
  const barW = Math.max(12, (chartW - gap * (barCount + 1)) / barCount)
  const zeroY = padding.top + chartH * (yMax / (yMax - yMin))

  values.forEach((val, i) => {
    const x = padding.left + gap + i * (barW + gap)
    const barH = (Math.abs(val) / (yMax - yMin)) * chartH
    const y = val >= 0 ? zeroY - barH : zeroY

    ctx.setFillStyle(val >= 0 ? (options.positiveColor || '#333') : '#ccc')
    ctx.fillRect(x, y, barW, barH)

    ctx.setFontSize(9)
    ctx.setFillStyle('#666')
    ctx.fillText(val.toFixed(1), x + 2, y - 4)

    const label = labels[i] || ''
    ctx.setFillStyle('#999')
    ctx.fillText(label, x, padding.top + chartH + 18)
  })

  ctx.draw()
}

export function getChartSize() {
  return getCanvasSize()
}
