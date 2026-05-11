
<template>
  <router-view v-if="isLoginPage" />
  <div v-else class="app-container">
    <el-container>
      <el-aside width="236px" class="sidebar">
        <div class="logo">
          <div class="logo-mark">B</div>
          <div>
            <h2>BizAgent</h2>
            <p>AI低代码平台</p>
          </div>
        </div>
        <el-menu :default-active="activeMenu" class="menu" router>
          <div class="menu-section">工作台</div>
          <el-menu-item index="/ai/generate">
            <el-icon><component :is="icons.Rocket" /></el-icon>
            <span>AI生成</span>
          </el-menu-item>
          <template v-if="moduleMenus.length">
            <div class="menu-section">业务模块</div>
            <el-menu-item v-for="item in moduleMenus" :key="item.id" :index="item.path">
              <el-icon><component :is="icons.Module" /></el-icon>
              <span>{{ item.menuName }}</span>
            </el-menu-item>
          </template>
          <div class="menu-section">系统</div>
          <el-sub-menu index="system">
            <template #title>
              <el-icon><component :is="icons.Setting" /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/user">
              <el-icon><component :is="icons.User" /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/system/role">
              <el-icon><component :is="icons.Role" /></el-icon>
              <span>角色管理</span>
            </el-menu-item>
            <el-menu-item index="/system/menu">
              <el-icon><component :is="icons.Menu" /></el-icon>
              <span>菜单管理</span>
            </el-menu-item>
            <el-menu-item index="/system/permission">
              <el-icon><component :is="icons.Permission" /></el-icon>
              <span>权限管理</span>
            </el-menu-item>
            <el-menu-item index="/system/project">
              <el-icon><component :is="icons.Project" /></el-icon>
              <span>项目管理</span>
            </el-menu-item>
            <el-menu-item index="/system/module">
              <el-icon><component :is="icons.Module" /></el-icon>
              <span>模块管理</span>
            </el-menu-item>
            <el-menu-item index="/system/model-config">
              <el-icon><component :is="icons.Model" /></el-icon>
              <span>模型配置</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>AI企业低代码开发平台</p>
          </div>
          <div class="header-actions">
            <span class="status-dot"></span>
            <span>服务运行中</span>
            <el-divider direction="vertical" />
            <el-select
              v-model="currentProjectId"
              class="project-switcher"
              size="small"
              placeholder="当前项目"
              @change="switchProject"
            >
              <el-option
                v-for="project in projectOptions"
                :key="project.id"
                :label="project.projectName"
                :value="project.id"
              />
            </el-select>
            <el-divider direction="vertical" />
            <span>{{ currentUser?.realName || currentUser?.username || 'admin' }}</span>
            <el-button size="small" text @click="logout">退出登录</el-button>
          </div>
        </el-header>
        <div class="tabs-shell" v-if="openTabs.length">
          <div class="tabs-bar">
            <button
              v-for="tab in openTabs"
              :key="tab.fullPath"
              type="button"
              class="page-tab"
              :class="{ active: tab.fullPath === route.fullPath }"
              @click="switchTab(tab)"
            >
              <span class="tab-dot"></span>
              <span class="tab-title">{{ tab.title }}</span>
              <el-icon v-if="tab.closable" class="tab-close" @click.stop="closeTab(tab.fullPath)">
                <component :is="icons.Close" />
              </el-icon>
            </button>
          </div>
        </div>
        <el-main class="main">
          <router-view v-slot="{ Component, route: viewRoute }">
            <keep-alive :max="30">
              <component :is="Component" :key="getViewKey(viewRoute)" />
            </keep-alive>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Cpu, Folder, Grid, Key, Management, Menu, Promotion, Setting, User } from '@element-plus/icons-vue'
import { authApi, menuApi, projectApi } from './api'

const route = useRoute()
const router = useRouter()
const moduleMenus = ref([])
const isLoginPage = computed(() => route.path === '/login')
const currentUser = ref(null)
const openTabs = ref([])
const tabCacheVersions = ref({})
const projectOptions = ref([])
const currentProjectId = ref(Number(localStorage.getItem('bizagent_project_id') || 1))

const icons = {
  Rocket: Promotion,
  Setting,
  User,
  Role: Management,
  Menu,
  Permission: Key,
  Project: Folder,
  Module: Grid,
  Model: Cpu,
  Close
}

const titleMap = {
  '/ai/generate': 'AI模块生成',
  '/system/user': '用户管理',
  '/system/role': '角色管理',
  '/system/menu': '菜单管理',
  '/system/permission': '权限管理',
  '/system/project': '项目管理',
  '/system/module': '模块管理',
  '/system/model-config': '模型配置'
}

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const moduleMenu = moduleMenus.value.find((item) => item.path === route.path)
  return moduleMenu?.menuName || titleMap[route.path] || '模块预览'
})

const getTabTitle = (targetRoute) => {
  const moduleMenu = moduleMenus.value.find((item) => item.path === targetRoute.path)
  if (moduleMenu?.menuName) {
    return moduleMenu.menuName
  }
  if (titleMap[targetRoute.path]) {
    return titleMap[targetRoute.path]
  }
  if (targetRoute.name === 'ModulePreview') {
    return '模块预览'
  }
  return targetRoute.meta?.title || '功能页面'
}

const syncCurrentTab = () => {
  if (isLoginPage.value) {
    return
  }
  const nextTab = {
    fullPath: route.fullPath,
    path: route.path,
    title: getTabTitle(route),
    closable: route.path !== '/ai/generate'
  }
  const existingIndex = openTabs.value.findIndex((tab) => tab.fullPath === nextTab.fullPath)
  if (existingIndex >= 0) {
    openTabs.value.splice(existingIndex, 1, nextTab)
    return
  }
  openTabs.value.push(nextTab)
}

const refreshTabTitles = () => {
  openTabs.value = openTabs.value.map((tab) => ({
    ...tab,
    title: getTabTitle(tab)
  }))
}

const switchTab = (tab) => {
  if (tab.fullPath !== route.fullPath) {
    router.push(tab.fullPath)
  }
}

const getViewKey = (targetRoute) => {
  return `${targetRoute.fullPath}:${tabCacheVersions.value[targetRoute.fullPath] || 0}`
}

const closeTab = (fullPath) => {
  const closeIndex = openTabs.value.findIndex((tab) => tab.fullPath === fullPath)
  if (closeIndex < 0) {
    return
  }
  const isActiveTab = fullPath === route.fullPath
  openTabs.value.splice(closeIndex, 1)
  tabCacheVersions.value = {
    ...tabCacheVersions.value,
    [fullPath]: (tabCacheVersions.value[fullPath] || 0) + 1
  }
  if (!isActiveTab) {
    return
  }
  const nextTab = openTabs.value[closeIndex] || openTabs.value[closeIndex - 1]
  router.push(nextTab?.fullPath || '/ai/generate')
}

const loadCurrentUser = () => {
  try {
    currentUser.value = JSON.parse(localStorage.getItem('bizagent_user') || 'null')
  } catch (error) {
    currentUser.value = null
  }
}

const refreshCurrentUser = async () => {
  try {
    const response = await authApi.me()
    const user = response.data.data.user
    currentUser.value = user
    localStorage.setItem('bizagent_user', JSON.stringify(user))
  } catch (error) {
    loadCurrentUser()
  }
}

const logout = () => {
  localStorage.removeItem('bizagent_token')
  localStorage.removeItem('bizagent_user')
  localStorage.removeItem('bizagent_permissions')
  moduleMenus.value = []
  openTabs.value = []
  tabCacheVersions.value = {}
  currentUser.value = null
  router.replace('/login')
}

const loadModuleMenus = async () => {
  if (!localStorage.getItem('bizagent_token')) {
    moduleMenus.value = []
    currentUser.value = null
    return
  }
  loadCurrentUser()
  try {
    await refreshCurrentUser()
    const response = await menuApi.list({
      pageNum: 1,
      pageSize: 200,
      projectId: Number(localStorage.getItem('bizagent_project_id') || 1)
    })
    moduleMenus.value = response.data.data.records
      .filter((item) => item.visible !== 0 && item.path?.startsWith('/module-runtime/'))
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    refreshTabTitles()
  } catch (error) {
    console.error('加载业务模块菜单失败:', error)
  }
}

const loadProjects = async () => {
  if (!localStorage.getItem('bizagent_token')) {
    projectOptions.value = []
    return
  }
  try {
    const response = await projectApi.list({ pageNum: 1, pageSize: 100 })
    projectOptions.value = response.data.data.records.filter((item) => item.status === 1)
    if (!projectOptions.value.some((item) => item.id === currentProjectId.value)) {
      currentProjectId.value = projectOptions.value[0]?.id || 1
      localStorage.setItem('bizagent_project_id', String(currentProjectId.value))
    }
  } catch (error) {
    console.error('加载项目列表失败:', error)
  }
}

const switchProject = async (projectId) => {
  localStorage.setItem('bizagent_project_id', String(projectId))
  moduleMenus.value = []
  openTabs.value = openTabs.value.filter((tab) => !tab.path.startsWith('/module-runtime/'))
  await loadModuleMenus()
  if (route.path.startsWith('/module-runtime/')) {
    router.push('/ai/generate')
  }
}

onMounted(async () => {
  await loadProjects()
  await loadModuleMenus()
  syncCurrentTab()
})

watch(
  () => route.fullPath,
  async () => {
    if (route.path !== '/login') {
      await loadModuleMenus()
      syncCurrentTab()
    }
  }
)

const refreshShellData = async () => {
  await loadProjects()
  await loadModuleMenus()
}

window.addEventListener('storage', refreshShellData)
window.addEventListener('bizagent-login', refreshShellData)
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  color: #1f2937;
  background: #eef2f7;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
}

.app-container {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(180deg, #102033 0%, #17253a 52%, #111827 100%);
  color: #dbeafe;
  box-shadow: 10px 0 30px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 86px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.logo-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #0f172a;
  font-weight: 800;
  background: linear-gradient(135deg, #67e8f9 0%, #a7f3d0 100%);
  box-shadow: 0 12px 28px rgba(103, 232, 249, 0.18);
}

.logo h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.1;
  letter-spacing: 0;
  color: #ffffff;
}

.logo p {
  margin: 5px 0 0;
  color: #93a4bd;
  font-size: 12px;
}

.menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #b8c4d6;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-hover-text-color: #ffffff;
  border-right: none;
  background: transparent;
  padding: 16px 12px 22px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.menu::-webkit-scrollbar {
  width: 0;
}

.menu-section {
  margin: 18px 12px 8px;
  color: #70849d;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.menu .el-menu-item,
.menu .el-sub-menu__title {
  height: 44px;
  margin: 5px 0;
  padding: 0 13px !important;
  border-radius: 8px;
  color: #cad5e5;
  font-size: 14px;
  font-weight: 560;
  line-height: 44px;
  background: transparent;
  transition: background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.menu .el-menu-item .el-icon,
.menu .el-sub-menu__title .el-icon {
  width: 20px;
  margin-right: 10px;
  color: #74c7f8;
  font-size: 17px;
}

.menu .el-menu-item:hover,
.menu .el-sub-menu__title:hover {
  background: rgba(148, 163, 184, 0.12);
  color: #ffffff;
  transform: translateX(1px);
}

.menu .el-menu-item.is-active {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.28), rgba(52, 211, 153, 0.18));
  box-shadow: inset 3px 0 0 #22d3ee, 0 14px 30px rgba(8, 145, 178, 0.18);
}

.menu .el-menu-item.is-active .el-icon {
  color: #a7f3d0;
}

.menu .el-menu {
  background: transparent;
  padding: 4px 0 8px 12px;
}

.menu .el-sub-menu.is-opened > .el-sub-menu__title {
  color: #ffffff;
  background: rgba(148, 163, 184, 0.1);
}

.menu .el-sub-menu .el-menu-item {
  height: 40px;
  margin: 4px 0 4px 4px;
  padding-left: 12px !important;
  font-size: 13px;
  color: #a9b8ca;
  background: transparent;
}

.menu .el-sub-menu .el-menu-item::before {
  content: "";
  width: 5px;
  height: 5px;
  margin-right: 10px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.55);
}

.menu .el-sub-menu .el-menu-item:hover {
  color: #f8fafc;
  background: rgba(148, 163, 184, 0.11);
}

.menu .el-sub-menu .el-menu-item.is-active::before {
  background: #67e8f9;
}

.menu .el-sub-menu .el-menu-item.is-active {
  color: #ffffff;
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.24), rgba(20, 184, 166, 0.16));
}

.menu .el-sub-menu__icon-arrow {
  color: #7c8da6;
  right: 14px;
}

.header {
  height: 86px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  backdrop-filter: blur(12px);
}

.header h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  color: #0f766e;
  background: #f0fdfa;
  font-size: 13px;
  white-space: nowrap;
}

.project-switcher {
  width: 132px;
}

.tabs-shell {
  height: 48px;
  padding: 7px 22px 0;
  display: flex;
  align-items: center;
  background: rgba(248, 250, 252, 0.88);
  border-bottom: 1px solid #dfe7f1;
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.72);
}

.tabs-bar {
  width: 100%;
  height: 36px;
  padding: 0 2px;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.tabs-bar::-webkit-scrollbar {
  height: 0;
}

.page-tab {
  position: relative;
  height: 32px;
  min-width: 116px;
  max-width: 210px;
  padding: 0 10px 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 7px;
  flex: 0 0 auto;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #64748b;
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.18s ease, background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.page-tab:hover {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.78);
  border-color: #dbe3ef;
}

.page-tab.active {
  color: #0f172a;
  background: #ffffff;
  border-color: #cbd5e1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.page-tab.active::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: -8px;
  height: 2px;
  border-radius: 999px;
  background: #14b8a6;
}

.tab-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #cbd5e1;
}

.page-tab.active .tab-dot {
  background: #14b8a6;
  box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.14);
}

.tab-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  width: 17px;
  height: 17px;
  margin-left: auto;
  margin-right: -2px;
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  color: #94a3b8;
  font-size: 12px;
  opacity: 0.72;
  transition: color 0.18s ease, background-color 0.18s ease, opacity 0.18s ease;
}

.page-tab:hover .tab-close,
.page-tab.active .tab-close {
  opacity: 1;
}

.tab-close:hover {
  color: #0f172a;
  background: #e2e8f0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.14);
}

.main {
  padding: 28px;
  overflow: auto;
  background:
    radial-gradient(circle at 18% 0%, rgba(34, 211, 238, 0.12), transparent 26%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

@media (max-width: 900px) {
  .sidebar {
    width: 76px !important;
  }

  .logo {
    justify-content: center;
    padding: 16px 10px;
  }

  .logo > div:not(.logo-mark),
  .menu span,
  .menu-section,
  .menu .el-sub-menu__icon-arrow {
    display: none;
  }

  .menu {
    padding: 12px 10px;
  }

  .menu .el-menu-item,
  .menu .el-sub-menu__title {
    justify-content: center;
    padding: 0 !important;
  }

  .menu .el-menu-item .el-icon,
  .menu .el-sub-menu__title .el-icon {
    margin-right: 0;
  }

  .header {
    padding: 0 18px;
  }

  .tabs-shell {
    padding: 8px 12px 0;
  }

  .page-tab {
    min-width: 96px;
    max-width: 152px;
  }

  .header-actions {
    display: none;
  }

  .main {
    padding: 18px;
  }
}
</style>
