
# BizAgent - AI企业低代码开发平台

基于AI驱动的企业级低代码开发平台，支持通过自然语言描述自动生成业务功能模块。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.x |
| ORM框架 | MyBatis Plus | 3.5.x |
| 数据库 | MySQL | 8.0+ |
| 前端框架 | Vue | 3.x |
| UI组件 | Element Plus | 2.x |
| 构建工具 | Vite | 6.x |

## 项目结构

```
BizAgent/
├── server/                    # Spring Boot后端
│   ├── src/main/java/com/example/bizagent/
│   │   ├── BizAgentApplication.java    # 启动类
│   │   ├── common/                     # 通用组件
│   │   │   └── auth/                   # 认证相关
│   │   ├── config/                     # 配置类
│   │   └── modules/                    # 业务模块
│   │       ├── aiengine/               # AI引擎模块
│   │       ├── modulecontainer/        # 模块容器
│   │       └── system/                 # 系统管理
│   ├── src/main/resources/
│   │   ├── application.example.yml     # 应用配置模板
│   │   └── application.yml             # 本地应用配置（不提交）
│   └── pom.xml                         # Maven配置
├── src/                        # Vue前端
│   ├── views/                         # 页面视图
│   ├── router/                        # 路由配置
│   ├── api/                           # API接口
│   ├── modules/                       # 业务模块
│   ├── App.vue                        # 根组件
│   ├── main.js                        # 入口文件
│   └── package.json                   # 依赖配置
├── init.sql                    # 数据库初始化脚本
└── README.md                   # 项目说明文档
```

## 核心功能

### 1. AI引擎模块
- 需求分析：自动识别业务需求
- 需求完善：智能追问补充需求
- 页面生成：自动生成Vue页面
- 接口生成：自动生成RESTful API
- SQL生成：自动生成数据库表结构
- 权限生成：自动生成权限配置

### 2. 系统管理模块
- 用户管理：用户增删改查
- 角色管理：角色权限配置
- 菜单管理：系统菜单管理
- 权限管理：细粒度权限控制
- 项目管理：多项目隔离
- 模块管理：业务模块管理

### 3. 模块运行容器
- 动态加载：运行时动态加载模块
- 热更新：无需重启更新模块
- 权限挂载：自动挂载权限点
- 菜单挂载：自动注册菜单
- 路由挂载：自动注册路由

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

### 后端启动

```bash
cd server
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

### 前端启动

```bash
cd src
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动

### 数据库配置

复制配置模板并修改本地连接信息：

```bash
copy server\src\main\resources\application.example.yml server\src\main\resources\application.yml
```

也可以通过环境变量覆盖数据库连接：

```yaml
DB_URL=jdbc:mysql://localhost:3306/bizagent?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
DB_USERNAME=your_username
DB_PASSWORD=your_password
BIZAGENT_AUTH_SECRET=replace-with-a-long-random-secret
```

## API文档

启动后端服务后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

## 模块规范

每个业务模块必须包含以下文件：
- `module.json` - 模块配置
- `menus.json` - 菜单配置
- `permissions.json` - 权限配置
- `routes.json` - 路由配置

## 权限规则

权限格式：`模块:功能:操作`

示例：
- `inspection:list` - 设备巡检列表
- `inspection:add` - 新增巡检记录
- `inspection:edit` - 编辑巡检记录
- `inspection:delete` - 删除巡检记录

## 数据库规范

### 表名前缀
业务表统一使用 `biz_` 前缀

### 通用字段
所有业务表必须包含：
- `id` - 主键
- `create_by` - 创建人
- `create_time` - 创建时间
- `update_by` - 更新人
- `update_time` - 更新时间
- `del_flag` - 删除标记
- `project_id` - 项目ID

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。
