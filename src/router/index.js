
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/ai/generate'
  },
  {
    path: '/login',
    name: 'Login',
    meta: { public: true },
    component: () => import('../views/Login.vue')
  },
  {
    path: '/ai/generate',
    name: 'AiGenerate',
    component: () => import('../views/AiGenerate.vue')
  },
  {
    path: '/system/user',
    name: 'SysUser',
    component: () => import('../views/system/UserList.vue')
  },
  {
    path: '/system/role',
    name: 'SysRole',
    component: () => import('../views/system/RoleList.vue')
  },
  {
    path: '/system/menu',
    name: 'SysMenu',
    component: () => import('../views/system/MenuList.vue')
  },
  {
    path: '/system/permission',
    name: 'SysPermission',
    component: () => import('../views/system/PermissionList.vue')
  },
  {
    path: '/system/project',
    name: 'SysProject',
    component: () => import('../views/system/ProjectList.vue')
  },
  {
    path: '/system/module',
    name: 'SysModule',
    component: () => import('../views/system/ModuleList.vue')
  },
  {
    path: '/system/model-config',
    name: 'SysModelConfig',
    component: () => import('../views/system/ModelConfigList.vue')
  },
  {
    path: '/module/preview/:id',
    name: 'ModulePreview',
    component: () => import('../views/ModulePreview.vue')
  },
  {
    path: '/module-runtime/:moduleCode',
    name: 'ModuleRuntime',
    component: () => import('../views/ModuleRuntime.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  const token = localStorage.getItem('bizagent_token')
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
