<template>
  <div class="login-page" @mousemove="handleMouseMove">
    <section class="character-panel">
      <div class="panel-brand">
        <div class="brand-mark">B</div>
        <span>BizAgent</span>
      </div>

      <div class="characters" :class="characterState" :style="lookStyle">
        <div class="character purple">
          <div class="face">
            <span class="eye"><i></i></span>
            <span class="eye"><i></i></span>
          </div>
        </div>
        <div class="character black">
          <div class="face">
            <span class="eye"><i></i></span>
            <span class="eye"><i></i></span>
          </div>
        </div>
        <div class="character orange">
          <div class="dot-face">
            <span></span>
            <span></span>
          </div>
        </div>
        <div class="character yellow">
          <div class="dot-face">
            <span></span>
            <span></span>
          </div>
          <div class="mouth"></div>
        </div>
      </div>

      <div class="decor-blur one"></div>
      <div class="decor-blur two"></div>
      <div class="decor-grid"></div>
    </section>

    <section class="form-panel">
      <div class="form-wrapper">
        <p class="panel-tag">WELCOME BACK</p>
        <div class="mobile-brand">
          <div class="brand-mark small">B</div>
          <span>BizAgent</span>
        </div>

        <div class="form-header">
          <h1>进入 BizAgent 控制台</h1>
          <p>登录后生成、发布并运行企业内部业务模块</p>
        </div>

        <el-form class="login-form" label-position="top" @submit.prevent="login">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              size="large"
              autocomplete="username"
              placeholder="请输入用户名"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              size="large"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入密码"
              @focus="passwordFocused = true"
              @blur="passwordFocused = false"
            >
              <template #suffix>
                <button class="eye-toggle" type="button" @mousedown.prevent @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </template>
            </el-input>
          </el-form-item>

          <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

          <el-button class="login-button" type="primary" size="large" :loading="loading" @click="login">
            {{ loading ? '安全验证中...' : '进入工作空间' }}
          </el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const passwordFocused = ref(false)
const errorMessage = ref('')
const look = reactive({ x: 0, y: 0 })

const form = reactive({
  username: 'admin',
  password: '123456'
})

const characterState = computed(() => ({
  typing: isTyping.value,
  guard: passwordFocused.value && form.password.length > 0 && !showPassword.value,
  reveal: passwordFocused.value && form.password.length > 0 && showPassword.value,
  error: Boolean(errorMessage.value)
}))

const lookStyle = computed(() => ({
  '--look-x': `${look.x}px`,
  '--look-y': `${look.y}px`
}))

const handleMouseMove = (event) => {
  const centerX = window.innerWidth * 0.28
  const centerY = window.innerHeight * 0.48
  look.x = Math.max(-8, Math.min(8, (event.clientX - centerX) / 55))
  look.y = Math.max(-7, Math.min(7, (event.clientY - centerY) / 65))
}

const login = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await authApi.login(form)
    localStorage.setItem('bizagent_token', response.data.data.token)
    localStorage.setItem('bizagent_user', JSON.stringify(response.data.data.user))
    localStorage.setItem('bizagent_permissions', JSON.stringify(response.data.data.permissions || []))
    window.dispatchEvent(new Event('bizagent-login'))
    router.replace(route.query.redirect || '/ai/generate')
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '登录失败'
    setTimeout(() => {
      errorMessage.value = ''
    }, 1400)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(420px, 0.95fr);
  overflow: hidden;
  background: #eef4ff;
}

.character-panel {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 28% 18%, rgba(125, 92, 255, 0.42), transparent 24%),
    radial-gradient(circle at 72% 76%, rgba(255, 155, 107, 0.34), transparent 22%),
    linear-gradient(135deg, #172554 0%, #1e3a8a 52%, #0f172a 100%);
}

.panel-brand {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 34px 42px;
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #0f172a;
  background: #8cf0df;
  font-weight: 900;
  box-shadow: 0 18px 44px rgba(12, 74, 110, 0.28);
}

.brand-mark.small {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  font-size: 14px;
}

.characters {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 560px;
  height: 430px;
  transform: translateX(-50%);
  z-index: 2;
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  transition: transform 0.32s ease, height 0.32s ease;
}

.purple {
  left: 72px;
  width: 180px;
  height: 400px;
  border-radius: 12px 12px 0 0;
  background: #6c3ff5;
  z-index: 1;
}

.black {
  left: 242px;
  width: 120px;
  height: 310px;
  border-radius: 10px 10px 0 0;
  background: #2d2d2d;
  z-index: 2;
}

.orange {
  left: 0;
  width: 240px;
  height: 200px;
  border-radius: 120px 120px 0 0;
  background: #ff9b6b;
  z-index: 3;
}

.yellow {
  left: 312px;
  width: 140px;
  height: 230px;
  border-radius: 70px 70px 0 0;
  background: #e8d754;
  z-index: 4;
}

.face,
.dot-face {
  position: absolute;
  display: flex;
  gap: 28px;
  transition: transform 0.28s ease, left 0.28s ease, top 0.28s ease;
}

.purple .face {
  left: 45px;
  top: 42px;
}

.black .face {
  left: 26px;
  top: 32px;
  gap: 24px;
}

.orange .dot-face {
  left: 82px;
  top: 92px;
}

.yellow .dot-face {
  left: 52px;
  top: 42px;
  gap: 24px;
}

.eye {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #ffffff;
  transition: height 0.2s ease;
  animation: blink 4.8s infinite;
}

.black .eye {
  width: 16px;
  height: 16px;
  animation-delay: 1.4s;
}

.eye i,
.dot-face span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #222222;
  transform: translate(var(--look-x), var(--look-y));
  transition: transform 0.16s ease;
}

.dot-face span {
  width: 12px;
  height: 12px;
}

.mouth {
  position: absolute;
  left: 40px;
  top: 88px;
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: transform 0.26s ease, width 0.26s ease;
}

.characters.typing .purple {
  height: 430px;
  transform: translateX(40px) skewX(-10deg);
}

.characters.typing .black {
  transform: translateX(18px) skewX(8deg);
}

.characters.typing .purple .face {
  left: 56px;
  top: 66px;
}

.characters.typing .black .face {
  left: 34px;
  top: 16px;
}

.characters.guard .face,
.characters.guard .dot-face {
  transform: translate(-34px, -18px);
}

.characters.guard .purple .face,
.characters.guard .black .face {
  transform: translate(-26px, -20px);
}

.characters.guard .eye i,
.characters.guard .dot-face span,
.characters.reveal .eye i,
.characters.reveal .dot-face span {
  transform: translate(-6px, -6px);
}

.characters.guard .mouth {
  transform: translate(-16px, -8px);
}

.characters.reveal .purple .face {
  left: 22px;
  top: 36px;
}

.characters.reveal .black .face {
  left: 12px;
  top: 30px;
}

.characters.error {
  animation: shake 0.45s ease;
}

.decor-blur {
  position: absolute;
  border-radius: 999px;
  filter: blur(48px);
  opacity: 0.5;
}

.decor-blur.one {
  width: 220px;
  height: 220px;
  left: 50px;
  top: 120px;
  background: #a78bfa;
}

.decor-blur.two {
  right: 80px;
  bottom: 84px;
  width: 260px;
  height: 260px;
  background: #22d3ee;
}

.decor-grid {
  position: absolute;
  inset: 0;
  opacity: 0.16;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.32) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.32) 1px, transparent 1px);
  background-size: 44px 44px;
}

.form-panel {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 44px;
  background:
    radial-gradient(circle at 18% 8%, rgba(34, 211, 238, 0.16), transparent 28%),
    #f8fafc;
}

.form-wrapper {
  width: min(460px, 100%);
  padding: 42px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 28px 90px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}

.panel-tag {
  margin: 0 0 20px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: #111827;
  font-weight: 800;
}

.form-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.18;
}

.form-header p {
  margin: 12px 0 30px;
  color: #64748b;
  line-height: 1.7;
}

.login-form {
  display: grid;
  gap: 5px;
}

.login-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe3ef inset;
}

.eye-toggle {
  border: 0;
  color: #2563eb;
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.login-button {
  width: 100%;
  height: 46px;
  margin-top: 14px;
  border-radius: 8px;
  font-weight: 800;
}

@keyframes blink {
  0%,
  88%,
  100% {
    transform: scaleY(1);
  }
  92%,
  94% {
    transform: scaleY(0.18);
  }
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(-50%);
  }
  25% {
    transform: translateX(calc(-50% - 10px));
  }
  50% {
    transform: translateX(calc(-50% + 10px));
  }
  75% {
    transform: translateX(calc(-50% - 6px));
  }
}

@media (max-width: 920px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .character-panel {
    display: none;
  }

  .form-panel {
    padding: 24px;
  }

  .form-wrapper {
    padding: 30px;
  }

  .mobile-brand {
    display: flex;
  }
}
</style>
