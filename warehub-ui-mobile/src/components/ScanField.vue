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
        @click="onScan"
      />
    </template>
  </van-field>

  <!-- 模拟扫码：手动录入物料编码/批次号 -->
  <van-dialog
    v-model:show="showManual"
    title="模拟扫码"
    show-cancel-button
    confirm-button-text="确定"
    @confirm="confirmManual"
  >
    <div class="manual-box">
      <p class="manual-tip">
        移动端可调用微信 <code>wx.scanQRCode</code> 或浏览器
        <code>BarcodeDetector</code> 扫码。当前环境未注入扫码能力，请手动录入{{ label }}：
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
import { ref } from 'vue'
import { showToast } from 'vant'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '编码' },
  placeholder: { type: String, default: '请扫码或输入' },
  required: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'scan'])

const showManual = ref(false)
const manualValue = ref('')

// 扫码结果清理：trim + 去除常见前缀（如 CODE:, CODE-）
function cleanScanResult(raw) {
  let v = String(raw || '').trim()
  v = v.replace(/^CODE[:\-]/i, '')
  return v
}

function onInput(val) {
  emit('update:modelValue', cleanScanResult(val))
}

function onScan() {
  // 1) 微信内置浏览器：调用 JSSDK 扫码
  if (typeof window !== 'undefined' && window.wx && window.wx.scanQRCode) {
    window.wx.scanQRCode({
      needResult: 1,
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        const code = cleanScanResult(res.resultStr || '')
        emit('update:modelValue', code)
        emit('scan', code)
      },
      fail: () => {
        showManual.value = true
      }
    })
    return
  }

  // 2) 支持 BarcodeDetector 的浏览器（如 Chrome/Edge 部分版本）
  if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
    try {
      const bd = new window.BarcodeDetector({ formats: ['qr_code', 'ean_13', 'code_128'] })
      navigator.mediaDevices
        .getUserMedia({ video: { facingMode: 'environment' } })
        .then((stream) => {
          const video = document.createElement('video')
          video.srcObject = stream
          video.play()
          bd.detect(video)
            .then((codes) => {
              stream.getTracks().forEach((t) => t.stop())
              if (codes.length) {
                const code = cleanScanResult(codes[0].rawValue)
                emit('update:modelValue', code)
                emit('scan', code)
              } else {
                showToast('未识别到条码，请手动录入')
                showManual.value = true
              }
            })
            .catch(() => {
              stream.getTracks().forEach((t) => t.stop())
              showManual.value = true
            })
        })
        .catch(() => {
          showManual.value = true
        })
      return
    } catch (e) {
      // 忽略，走手动录入
    }
  }

  // 3) 通用 H5 兜底：弹出输入框
  manualValue.value = props.modelValue ? String(props.modelValue) : ''
  showManual.value = true
}

function confirmManual() {
  const val = cleanScanResult(manualValue.value)
  if (props.required && !val) {
    showToast(`请输入${props.label}`)
    return
  }
  emit('update:modelValue', val)
  emit('scan', val)
  showManual.value = false
}
</script>

<style scoped>
.manual-box {
  padding: 12px 16px 4px;
}
.manual-tip {
  font-size: 13px;
  color: var(--wh-text-3);
  line-height: 1.6;
  margin: 0 0 8px;
}
.manual-tip code {
  font-size: 12px;
  background: #f2f3f5;
  padding: 1px 4px;
  border-radius: 4px;
  color: var(--wh-primary);
}
</style>
