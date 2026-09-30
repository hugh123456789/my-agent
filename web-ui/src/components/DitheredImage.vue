<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getDitherInk, normalizeLuminance } from '../dither.js'

const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  width: { type: [String, Number], default: undefined },
  height: { type: [String, Number], default: undefined },
  loading: { type: String, default: 'lazy' },
})

const wrapperRef = ref(null)
const imageRef = ref(null)
const canvasRef = ref(null)

const CELL_SIZE = 2.5
const PRINT_PAPER = '#faf8f2'
const PRINT_INK = '#48443e'
let sampleCanvas
let resizeObserver
let retryTimer

function drawDither() {
  const wrapper = wrapperRef.value
  const image = imageRef.value
  const canvas = canvasRef.value
  if (!wrapper || !image || !canvas || !image.naturalWidth || !image.naturalHeight) return

  const rect = wrapper.getBoundingClientRect()
  if (rect.width < 2 || rect.height < 2) return

  const cols = Math.max(1, Math.round(rect.width / CELL_SIZE))
  const rows = Math.max(1, Math.round(rect.height / CELL_SIZE))
  sampleCanvas ||= document.createElement('canvas')
  sampleCanvas.width = cols
  sampleCanvas.height = rows

  const sampleContext = sampleCanvas.getContext('2d', { willReadFrequently: true })
  const context = canvas.getContext('2d')
  if (!sampleContext || !context) return

  sampleContext.clearRect(0, 0, cols, rows)
  sampleContext.drawImage(image, 0, 0, cols, rows)

  let pixels
  try {
    pixels = sampleContext.getImageData(0, 0, cols, rows).data
  } catch {
    canvas.removeAttribute('data-ready')
    return
  }

  const luminances = []
  for (let index = 0; index < pixels.length; index += 4) {
    luminances.push((0.2126 * pixels[index] + 0.7152 * pixels[index + 1] + 0.0722 * pixels[index + 2]) / 255)
  }
  const sorted = [...luminances].sort((a, b) => a - b)
  const low = sorted[Math.floor(sorted.length * 0.05)] ?? 0
  const high = sorted[Math.floor(sorted.length * 0.95)] ?? 1
  const dpr = window.devicePixelRatio || 1

  canvas.width = Math.round(rect.width * dpr)
  canvas.height = Math.round(rect.height * dpr)
  canvas.style.width = `${rect.width}px`
  canvas.style.height = `${rect.height}px`
  context.setTransform(dpr, 0, 0, dpr, 0, 0)
  context.fillStyle = PRINT_PAPER
  context.fillRect(0, 0, rect.width, rect.height)
  context.fillStyle = PRINT_INK

  for (let row = 0; row < rows; row += 1) {
    for (let column = 0; column < cols; column += 1) {
      const luminance = luminances[row * cols + column]
      const ink = 1 - normalizeLuminance(luminance, low, high)
      if (getDitherInk(ink, column, row)) {
        context.fillRect(column * CELL_SIZE, row * CELL_SIZE, CELL_SIZE + 0.3, CELL_SIZE + 0.3)
      }
    }
  }

  canvas.setAttribute('data-ready', 'true')
}

function scheduleDraw() {
  window.clearTimeout(retryTimer)
  retryTimer = window.setTimeout(drawDither, 0)
}

function handleImageLoad() {
  drawDither()
  imageRef.value?.decode?.().then(drawDither).catch(() => {})
}

onMounted(() => {
  const image = imageRef.value
  if (!image) return

  image.addEventListener('load', handleImageLoad)
  if (image.complete && image.naturalWidth > 0) handleImageLoad()
  else scheduleDraw()

  resizeObserver = new ResizeObserver(drawDither)
  resizeObserver.observe(wrapperRef.value)
})

onBeforeUnmount(() => {
  imageRef.value?.removeEventListener('load', handleImageLoad)
  resizeObserver?.disconnect()
  window.clearTimeout(retryTimer)
})
</script>

<template>
  <span ref="wrapperRef" class="dither-image" :style="{ '--image-width': width, '--image-height': height }">
    <img ref="imageRef" class="dither-image-source" :src="props.src" :alt="props.alt" :width="props.width" :height="props.height" :loading="props.loading" />
    <canvas ref="canvasRef" class="dither-veil" aria-hidden="true"></canvas>
  </span>
</template>
