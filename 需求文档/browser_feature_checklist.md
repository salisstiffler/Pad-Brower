# SWeb 浏览器功能开发点检项 (参考 Chrome)

## 1. 基础导航 (Navigation)
- [x] 后退 (Back) / 前进 (Forward)
- [x] 刷新 (Reload) / 停止加载 (Stop)
- [x] 主页 (Home) - 导航回 Nexus Core 首页
- [ ] 页面加载进度条 (ProgressBar) - 优化平滑度
- [ ] 错误页面处理 (Custom Error Pages for 404, No Internet)

## 2. 地址栏/多功能框 (Omnibox)
- [x] 网址输入与跳转
- [x] 关键词搜索 (集成 Google/Baidu)
- [x] 自动全选 (Select all on focus)
- [x] 一键清除按钮 (Clear button)
- [ ] 搜索建议 (Search Suggestions) - 优化联想词显示
- [x] 站点安全信息图标 (SSL/Lock Icon)
- [ ] 复制/分享当前网址按钮

## 3. 标签页管理 (Tab Management)
- [x] 多标签页创建 (New Tab)
- [x] 标签页切换器 (Tab Switcher BottomSheet)
- [x] 标签页关闭 (Close Tab)
- [x] 自动保存/恢复标签页状态 (State Persistence)
- [ ] 撤销关闭标签页 (Undo Close Tab)
- [ ] 无痕模式 (Incognito Mode)

## 4. 书签与历史 (Bookmarks & History)
- [x] 添加/删除书签
- [x] 历史记录持久化存储
- [x] 查看历史/书签列表 (Nexus Themed BottomSheet)
- [ ] 搜索历史记录/书签
- [ ] 书签文件夹管理

## 5. 捏持交互系统 (Pinch-Hold System - 核心专利)
- [x] 左右边缘识别区 (Edge Detection)
- [x] B区激活逻辑 (0.5s Hold)
- [x] 三档微动手势滚动算法 (Module 2)
- [x] A/B区优先级协调 (Module 3)
- [x] 捏持状态视觉指示器 (Pinch Active Indicator)
- [ ] 灵敏度自定义设置 (Settings Integration)

## 6. 设置与隐私 (Settings & Privacy)
- [x] 广告拦截 (AdBlocker) 开关
- [x] 桌面模式 (Desktop UA) 切换
- [x] JavaScript 开关
- [x] 清除浏览数据 (History, Cache, Cookies)
- [ ] 默认搜索引擎选择
- [ ] 隐私模式策略 (Do Not Track)

## 7. 页面交互功能 (Page Interaction)
- [x] 页面内搜索 (Find in Page)
- [x] 页面信息查看 (Page Info / SSL Certs)
- [x] 长按菜单 (Open in new tab, Copy link, Download)
- [x] 文件下载管理 (Download Manager)
- [x] 全屏视频播放支持 (Fullscreen Video)
- [ ] 网页翻译集成 (Google Translate API)

## 8. 系统架构与质量 (Architecture & Quality)
- [ ] MVVM 架构重构 (Separation of UI and Logic)
- [ ] 依赖注入实现 (Optional: Manual DI or Hilt)
- [ ] 数据库层封装 (Repository Pattern)
- [ ] 单元测试覆盖 (JUnit/Mockito for business logic)
- [ ] UI 自动化测试 (Espresso for critical paths)
