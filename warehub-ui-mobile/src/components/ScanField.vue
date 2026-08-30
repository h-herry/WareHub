<template>
  <van-field
    :label="label"
    :placeholder="placeholder"
    :required="required"
    :model-value="modelValue"
    @update:model-value="onInput"
  >
    <template #right-icon>
      <van-icon
        name="scan"
        size="22"
        color="#4f5bd5"
        @click="startScan"
      />
    </template>
  </van-field>

  <!-- 摄像头实时取景层 -->
  <van-overlay :show="scanning" @click="stopScan">
    <div class="scan-mask" @click.stop>
      <video ref="videoEl" class="scan-video" autoplay playsinline muted></video>
      <div class="scan-rect"></div>
      <div class="scan-tip">将二维码 / 条形码放入框内，识别后自动填入</div>
      <van-button class="scan-close" icon="cross" round @click="stopScan">关闭</van-button>
    </div>
  </van-overlay>

  <!-- 手动录入兜底 -->
  <van-dialog
    v-model:show="showManual"
    title="手动录入"
    show-cancel-button
    confirm-button-text="确定"
    @confirm="confirmManual"
  >
    <div class="manual-box">
      <p class="manual-tip">
        当前环境无法调用摄像头扫码，请手动录入{{ label }}：
      </p>
      <van-field
        v-model="manualValue"
        :placeholder="`请输入${label}`"
        autofocus
        @keyup.enter="confirmManual"
      />
    </div>
  </van-dialog>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { showToast } from 'vant'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '编码' },
  placeholder: { type: String, default: '请扫码或输入' },
  required: { type: Boolean, default: false },
  // scanType: 'all' | 'qr' | 'bar'
  scanType: { type: String, default: 'all' }
})
const emit = defineEmits(['update:modelValue', 'scan'])

const scanning = ref(false)
const showManual = ref(false)
const manualValue = ref('')
const videoEl = ref(null)

let stream = null
let rafId = null
let detector = null

function formatsFor(type) {
  if (type === 'qr') return ['qr_code']
  if (type === 'bar')
    return ['ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'itf', 'codabar', 'aztec', 'data_matrix', 'pdf417']
  return ['qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'itf', 'codabar', 'aztec', 'data_matrix', 'pdf417']
}

// 清理：trim + 去除常见前缀（如 CODE:, CODE-）
function clean(raw) {
  let v = String(raw || '').trim()
  v = v.replace(/^CODE[:\-]/i, '')
  return v
}

function onInput(val) {
  emit('update:modelValue', clean(val))
}

function startScan() {
  // 1) 微信内置浏览器：调用 JSSDK 扫码
  if (typeof window !== 'undefined' && window.wx && window.wx.scanQRCode) {
    window.wx.scanQRCode({
      needResult: 1,
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        const code = clean(res.resultStr || '')
        finishScan(code)
      },
      fail: () => {
        showManual.value = true
      }
    })
    return
  }
  // 2) 支持 BarcodeDetector 的浏览器：实时摄像头识别
  if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
    startCamera()
    return
  }
  // 3) 兜底：手动录入
  manualValue.value = props.modelValue ? String(props.modelValue) : ''
  showManual.value = true
}

async function startCamera() {
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    scanning.value = true
    await nextTick()
    const video = videoEl.value
    if (!video) {
      stopScan()
      showManual.value = true
      return
    }
    video.srcObject = stream
    await video.play()
    detector = new window.BarcodeDetector({ formats: formatsFor(props.scanType) })
    loop()
  } catch (e) {
    stopScan()
    showManual.value = true
  }
}

async function loop() {
  if (!scanning.value || !videoEl.value) return
  try {
    const codes = await detector.detect(videoEl.value)
    if (codes && codes.length) {
      finishScan(clean(codes[0].rawValue))
      return
    }
  } catch (e) {
    // 继续下一帧
  }
  rafId = requestAnimationFrame(loop)
}

function finishScan(code) {
  stopScan()
  if (props.required && !code) {
    showToast(`请扫描或输入${props.label}`)
    return
  }
  emit('update:modelValue', code)
  emit('scan', code)
  showToast(`已识别：${code}`)
}

function stopScan() {
  scanning.value = false
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
}

function confirmManual() {
  const val = clean(manualValue.value)
  if (props.required && !val) {
    showToast(`请输入${props.label}`)
    return
  }
  emit('update:modelValue', val)
  emit('scan', val)
  showManual.value = false
}

onBeforeUnmount(stopScan)
</script>

<style scoped>
.scan-mask {
  position: relative;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}
.scan-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.scan-rect {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 70%;
  height: 200px;
  transform: translate(-50%, -50%);
  border: 2px solid #4f5bd5;
  border-radius: 8px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.35);
}
.scan-tip {
  position: absolute;
  bottom: 90px;
  left: 0;
  right: 0;
  text-align: center;
  color: #fff;
  font-size: 13px;
  padding: 0 24px;
}
.scan-close {
  position: absolute;
  bottom: 36px;
  left: 50%;
  transform: translateX(-50%);
}
.manual-box {
  padding: 12px 16px 4px;
}
.manual-tip {
  font-size: 13px;
  color: var(--wh-text-3);
  line-height: 1.6;
  margin: 0 0 8px;
}
</style>
