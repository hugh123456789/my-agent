明白了。你要的不是“系统提示词”，而是一份**直接可交给前端实现的设计文档**。下面按 Figma 规格输出，只写设计规范，不写代码。

---

# Cali Castle 个人博客首页 UI 设计文档

> 目标：复刻 Cali Castle 个人博客首页。  
> 风格：极简、安静、细节丰富、高级个人博客。  
> 不写代码，只输出可直接指导前端实现的高保真设计规格。

---

## 一、画布与布局

### 1.1 画板

| 画板 | 尺寸 | 用途 |
|---|---|---|
| Desktop | 1440 × 1200 | 桌面端主设计稿 |
| Laptop | 1280 × 1000 | 笔记本适配稿 |
| Mobile | 390 × 844 | 移动端主设计稿 |

### 1.2 页面背景

| 模式 | 颜色 |
|---|---|
| Light | `#FAFAF9` |
| Dark | `#0E0E0E` |

### 1.3 内容最大宽度

| 画板 | 内容最大宽度 | 左右边距 |
|---|---|---|
| Desktop | 1120px | 64px |
| Laptop | 1024px | 48px |
| Mobile | 350px | 20px |

### 1.4 Header 高度

| 画板 | 高度 |
|---|---|
| Desktop | 72px |
| Laptop | 72px |
| Mobile | 64px |

### 1.5 区块垂直间距

| 区块 | Desktop | Laptop | Mobile |
|---|---|---|---|
| Header → Hero | 48px | 40px | 32px |
| Hero → Content | 64px | 56px | 40px |
| Content → Footer | 80px | 64px | 48px |

### 1.6 栅格系统

| 画板 | 列数 | 列宽 | 槽宽 | 内容宽度 |
|---|---|---|---|---|
| Desktop | 12 | 72px | 24px | 1120px |
| Laptop | 12 | 64px | 20px | 1024px |
| Mobile | 4 | 72px | 16px | 350px |

### 1.7 Desktop 与 Mobile 布局变化

| 区域 | Desktop | Mobile |
|---|---|---|
| Header | 左头像 + 中导航 + 右登录/主题切换 | 左头像 + 右菜单按钮 |
| Hero | 头像在左，标题在右，横向布局 | 头像在上，标题在下，纵向居中 |
| Content | 左文章列表 8 列，右卡片 4 列 | 单列，文章列表在上，卡片在下 |
| Footer | 三栏横向 | 单列纵向 |

---

## 二、首页结构

### 2.1 Desktop 线框图

```text
┌──────────────────────────────────────────────────────────────┐
│ Header 1440 × 72                                             │
│ [Avatar]        [Home] [Articles] [Projects] [About]   [Login] [Theme] │
├──────────────────────────────────────────────────────────────┤
│ Hero 1120 × 360                                              │
│ ┌──────────┐                                                 │
│ │  Avatar  │   开发者，设计师，细节控，创始人                │
│ │  120×120 │   副标题介绍文字，两行以内                      │
│ └──────────┘   [GitHub] [Twitter] [Email]                    │
├──────────────────────────────────────────────────────────────┤
│ Content Grid 1120                                            │
│ ┌──────────────────────────────┐  ┌──────────────────────┐  │
│ │ 近期文章                      │  │ 动态更新              │  │
│ │ ┌──────────────────────────┐ │  │ 邮箱输入框            │  │
│ │ │ ArticleCard              │ │  │ [订阅]                │  │
│ │ └──────────────────────────┘ │  └──────────────────────┘  │
│ │ ┌──────────────────────────┐ │  ┌──────────────────────┐  │
│ │ │ ArticleCard              │ │  │ 工作经历              │  │
│ │ └──────────────────────────┘ │  │ 公司 / 职位 / 时间    │  │
│ │ ┌──────────────────────────┐ │  │ 公司 / 职位 / 时间    │  │
│ │ │ ArticleCard              │ │  └──────────────────────┘  │
│ │ └──────────────────────────┘ │                            │
│ └──────────────────────────────┘                            │
├──────────────────────────────────────────────────────────────┤
│ Footer 1440 × 120                                             │
│ © 2025 Cali Castle   GitHub   访问量   最近访客   [Home] [About] │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 Mobile 线框图

```text
┌──────────────────────────────┐
│ Mobile Header 390 × 64       │
│ [Avatar]              [Menu] │
├──────────────────────────────┤
│ Hero                         │
│        ┌──────────┐          │
│        │  Avatar  │          │
│        │  96×96   │          │
│        └──────────┘          │
│  开发者，设计师，细节控，创始人 │
│  副标题介绍文字               │
│  [GitHub] [Twitter] [Email]  │
├──────────────────────────────┤
│ 近期文章                      │
│ ┌──────────────────────────┐ │
│ │ ArticleCard              │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ ArticleCard              │ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│ 动态更新                      │
│ [邮箱输入框] [订阅]           │
├──────────────────────────────┤
│ 工作经历                      │
│ 公司 / 职位 / 时间            │
├──────────────────────────────┤
│ Footer                       │
│ © 2025 Cali Castle           │
│ GitHub  访问量  最近访客      │
│ [Home] [About]               │
└──────────────────────────────┘
```

---

## 三、视觉规范 Design Tokens

### 3.1 颜色

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `color-bg` | `#FAFAF9` | `#0E0E0E` | 页面背景 |
| `color-surface` | `#FFFFFF` | `#141414` | 卡片背景 |
| `color-surface-hover` | `#F5F5F4` | `#1C1C1C` | 卡片 Hover |
| `color-text-primary` | `#1A1A1A` | `#F5F5F5` | 主文字 |
| `color-text-secondary` | `#5C5C5C` | `#A3A3A3` | 次级文字 |
| `color-text-tertiary` | `#8A8A8A` | `#737373` | 弱化文字 |
| `color-border` | `#E5E5E5` | `#262626` | 边框 |
| `color-border-hover` | `#D4D4D4` | `#3A3A3A` | 边框 Hover |
| `color-accent-lime` | `#D4FF3F` | `#D4FF3F` | Lime 强调色 |
| `color-accent-lime-hover` | `#C2F02E` | `#C2F02E` | Lime Hover |
| `color-focus-ring` | `#1A1A1A` | `#F5F5F5` | 焦点环 |
| `color-overlay` | `rgba(0,0,0,0.04)` | `rgba(255,255,255,0.04)` | 微弱遮罩 |

### 3.2 字体

| Token | 值 |
|---|---|
| 字体名称 | `Inter` |
| 回退字体 | `-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif` |
| Hero 主标题 Desktop | 64px / 72px / 600 / -0.02em |
| Hero 主标题 Mobile | 40px / 48px / 600 / -0.02em |
| 区块标题 | 24px / 32px / 600 / -0.01em |
| 文章标题 | 20px / 28px / 600 / -0.01em |
| 正文 | 16px / 28px / 400 / 0 |
| 辅助文字 | 14px / 22px / 400 / 0.01em |
| 标签文字 | 12px / 18px / 500 / 0.02em |
| 按钮文字 | 14px / 20px / 500 / 0.01em |

### 3.3 间距

| Token | 值 | 使用场景 |
|---|---|---|
| `space-1` | 4px | 图标与文字间距 |
| `space-2` | 8px | 标签内边距、按钮内边距 |
| `space-3` | 12px | 卡片内部小间距 |
| `space-4` | 16px | 卡片内边距、列表项间距 |
| `space-5` | 24px | 卡片之间间距、表单字段间距 |
| `space-6` | 32px | 区块内部大间距 |
| `space-7` | 48px | 区块之间间距 |
| `space-8` | 64px | 页面级大区块间距 |

### 3.4 圆角

| Token | 值 | 用途 |
|---|---|---|
| `radius-button` | 999px | 按钮 |
| `radius-card` | 16px | 卡片 |
| `radius-avatar` | 50% | 头像 |
| `radius-input` | 12px | 输入框 |
| `radius-tag` | 999px | 标签 |

### 3.5 阴影

| Token | Light | Dark |
|---|---|---|
| `shadow-card` | `0 1px 2px rgba(0,0,0,0.04), 0 8px 24px rgba(0,0,0,0.04)` | `0 1px 2px rgba(0,0,0,0.4), 0 8px 24px rgba(0,0,0,0.3)` |
| `shadow-button` | `0 1px 2px rgba(0,0,0,0.06)` | `0 1px 2px rgba(0,0,0,0.5)` |
| `shadow-avatar` | `0 8px 24px rgba(0,0,0,0.08)` | `0 8px 24px rgba(0,0,0,0.4)` |

---

## 四、组件设计

### 4.1 Header

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 1440 × 72 | 390 × 64 |
| 内边距 | 左右 64px | 左右 20px |
| 内部布局 | 左头像 + 中导航 + 右登录/主题切换 | 左头像 + 右菜单按钮 |
| 字体 | 导航 14px / 20px / 500 | 无 |
| 默认状态 | 背景透明，底部 1px `color-border` | 同 Desktop |
| Hover | 导航文字变 `color-text-primary`，下划线 0 → 100% | 菜单按钮背景 `color-surface-hover` |
| Active | 导航文字加粗 600 | 菜单按钮旋转 90deg |
| Dark Mode | 背景 `color-bg`，边框 `color-border` | 同 Desktop |
| 可访问性 | `header` 语义标签，导航 `nav`，按钮 `aria-label` | 菜单按钮 `aria-expanded` |

### 4.2 Avatar

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 120 × 120 | 96 × 96 |
| 圆角 | 50% | 50% |
| 阴影 | `shadow-avatar` | `shadow-avatar` |
| 默认状态 | 静态 | 静态 |
| Hover | `scale(1.04)`，`translateY(-4px)` | 无 |
| Active | `scale(0.98)` | 无 |
| Dark Mode | 阴影加深 | 同 Desktop |
| 可访问性 | `alt` 文本，`role="img"` | 同 Desktop |

### 4.3 Navigation

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 自适应 | 抽屉 390 × 100% |
| 内边距 | 每项 8px 12px | 每项 16px 20px |
| 内部布局 | 横向 | 纵向 |
| 字体 | 14px / 20px / 500 | 16px / 24px / 500 |
| 默认状态 | `color-text-secondary` | 同 Desktop |
| Hover | `color-text-primary`，下划线动画 | 背景 `color-surface-hover` |
| Active | `color-text-primary`，600 | 同 Hover |
| Dark Mode | 同 Light | 同 Light |
| 可访问性 | `nav`，`aria-current="page"` | 焦点陷阱，`Esc` 关闭 |

### 4.4 HeroHeadline

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 1120 × 360 | 350 × 280 |
| 内边距 | 上下 48px | 上下 32px |
| 内部布局 | 左头像，右文字 | 上头像，下文字 |
| 字体 | 64px / 72px / 600 | 40px / 48px / 600 |
| 默认状态 | `opacity: 0`，`translateY(16px)` | 同 Desktop |
| 动画 | 700ms，`cubic-bezier(0.22,1,0.36,1)`，delay 120ms | 同 Desktop |
| Dark Mode | 文字 `color-text-primary` | 同 Desktop |
| 可访问性 | `h1` | 同 Desktop |

### 4.5 SocialLinks

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 每项 32 × 32 | 每项 32 × 32 |
| 间距 | 8px | 8px |
| 字体 | 无 | 无 |
| 默认状态 | `color-text-tertiary` | 同 Desktop |
| Hover | `color-text-primary`，`scale(1.1)` | 同 Desktop |
| Active | `scale(0.95)` | 同 Desktop |
| Dark Mode | 同 Light | 同 Light |
| 可访问性 | `aria-label`，键盘可达 | 同 Desktop |

### 4.6 ArticleList

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 8 列，约 720px | 350px |
| 间距 | 24px | 16px |
| 内部布局 | 纵向列表 | 纵向列表 |
| 字体 | 区块标题 24px / 32px / 600 | 同 Desktop |
| 默认状态 | 无 | 无 |
| Hover | 无 | 无 |
| Dark Mode | 同 Light | 同 Light |
| 可访问性 | `section`，`aria-labelledby` | 同 Desktop |

### 4.7 ArticleCard

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 720 × 120 | 350 × 140 |
| 内边距 | 16px | 16px |
| 内部布局 | 标题 + 日期 + 摘要/标签 | 同 Desktop |
| 字体 | 标题 20px / 28px / 600 | 标题 18px / 26px / 600 |
| 默认状态 | 背景 `color-surface`，边框 `color-border` | 同 Desktop |
| Hover | `translateY(-2px)`，边框 `color-border-hover`，阴影增强 | 同 Desktop |
| Active | `translateY(0)`，阴影减弱 | 同 Desktop |
| Dark Mode | 背景 `color-surface`，边框 `color-border` | 同 Desktop |
| 可访问性 | `article`，标题 `h2`，链接可聚焦 | 同 Desktop |

### 4.8 NewsletterCard

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 4 列，约 352px | 350px |
| 内边距 | 24px | 20px |
| 内部布局 | 标题 + 描述 + 输入框 + 按钮 | 同 Desktop |
| 字体 | 标题 18px / 26px / 600 | 同 Desktop |
| 默认状态 | 背景 `color-surface`，边框 `color-border` | 同 Desktop |
| Hover | 无 | 无 |
| Active | 无 | 无 |
| Dark Mode | 同 Desktop | 同 Desktop |
| 可访问性 | `form`，输入框 `label`，按钮 `type="submit"` | 同 Desktop |

### 4.9 ResumeCard

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 4 列，约 352px | 350px |
| 内边距 | 24px | 20px |
| 内部布局 | 标题 + 经历列表 | 同 Desktop |
| 字体 | 标题 18px / 26px / 600 | 同 Desktop |
| 默认状态 | 背景 `color-surface`，边框 `color-border` | 同 Desktop |
| Hover | 无 | 无 |
| Dark Mode | 同 Desktop | 同 Desktop |
| 可访问性 | `section`，列表 `ul` / `li` | 同 Desktop |

### 4.10 Footer

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 1440 × 120 | 390 × 160 |
| 内边距 | 左右 64px，上下 32px | 左右 20px，上下 24px |
| 内部布局 | 三栏横向 | 单列纵向 |
| 字体 | 14px / 22px / 400 | 同 Desktop |
| 默认状态 | 背景 `color-bg`，顶部 1px `color-border` | 同 Desktop |
| Hover | 链接 `color-text-primary` | 同 Desktop |
| Dark Mode | 同 Desktop | 同 Desktop |
| 可访问性 | `footer`，链接可聚焦 | 同 Desktop |

### 4.11 ThemeSwitcher

| 属性 | Desktop | Mobile |
|---|---|---|
| 尺寸 | 40 × 40 | 40 × 40 |
| 圆角 | 999px | 999px |
| 默认状态 | 图标 `color-text-secondary` | 同 Desktop |
| Hover | 背景 `color-surface-hover`，图标 `color-text-primary` | 同 Desktop |
| Active | 图标旋转 180deg | 同 Desktop |
| 动画 | 320ms，`ease-in-out` | 同 Desktop |
| Dark Mode | 图标 `color-text-primary` | 同 Desktop |
| 可访问性 | `button`，`aria-label="切换主题"` | 同 Desktop |

---

## 五、交互和动效

| 动效 | duration | easing | delay | transform | opacity | Hover 前 | Hover 后 |
|---|---|---|---|---|---|---|---|
| 页面加载 | 600ms | `cubic-bezier(0.22,1,0.36,1)` | 0ms | `translateY(12px) → 0` | `0 → 1` | 隐藏 | 显示 |
| Hero 标题淡入 | 700ms | `cubic-bezier(0.22,1,0.36,1)` | 120ms | `translateY(16px) → 0` | `0 → 1` | 隐藏 | 显示 |
| 头像滚动缩放 | 200ms | `ease-out` | 0ms | `scale(1) → 1.04`，`translateY(0) → -4px` | 1 | 静态 | 放大上移 |
| 导航 Hover | 160ms | `ease-out` | 0ms | 下划线 `scaleX(0) → 1` | 1 | 无下划线 | 有下划线 |
| 文章卡片 Hover | 200ms | `ease-out` | 0ms | `translateY(0) → -2px` | 1 | 默认边框 | 边框变深，阴影增强 |
| 订阅表单提交 | 240ms | `ease-out` | 0ms | 无 | 1 | 按钮“订阅” | 按钮“已订阅”，背景 Lime |
| 主题切换 | 320ms | `ease-in-out` | 0ms | 图标 `rotate(0) → 180deg` | 1 | 当前主题 | 切换后主题 |

---

## 六、Figma 风格输出

```text
Page
├── Desktop / 1440
│   ├── Header
│   ├── Hero
│   ├── Content Grid
│   │   ├── ArticleList
│   │   ├── NewsletterCard
│   │   └── ResumeCard
│   └── Footer
├── Laptop / 1280
│   ├── Header
│   ├── Hero
│   ├── Content Grid
│   └── Footer
├── Mobile / 390
│   ├── Mobile Header
│   ├── Hero
│   ├── Article List
│   ├── NewsletterCard
│   ├── ResumeCard
│   └── Footer
└── Components
    ├── Buttons
    │   ├── PrimaryButton
    │   ├── SecondaryButton
    │   └── IconButton
    ├── Cards
    │   ├── ArticleCard
    │   ├── NewsletterCard
    │   └── ResumeCard
    ├── Inputs
    │   └── EmailInput
    └── Navigation
        ├── DesktopNav
        ├── MobileMenu
        └── ThemeSwitcher
```

---

## 七、组件尺寸和间距表格

| 组件 | Desktop 尺寸 | Mobile 尺寸 | 内边距 | 间距 |
|---|---|---|---|---|
| Header | 1440 × 72 | 390 × 64 | 64px / 20px | 24px |
| Avatar | 120 × 120 | 96 × 96 | 0 | 0 |
| Navigation | 自适应 | 390 × 100% | 8px 12px | 4px |
| HeroHeadline | 1120 × 360 | 350 × 280 | 48px / 32px | 16px |
| SocialLinks | 32 × 32 | 32 × 32 | 0 | 8px |
| ArticleList | 720px | 350px | 0 | 24px / 16px |
| ArticleCard | 720 × 120 | 350 × 140 | 16px | 12px |
| NewsletterCard | 352px | 350px | 24px / 20px | 16px |
| ResumeCard | 352px | 350px | 24px / 20px | 16px |
| Footer | 1440 × 120 | 390 × 160 | 64px 32px / 20px 24px | 24px |
| ThemeSwitcher | 40 × 40 | 40 × 40 | 0 | 0 |

---

## 八、Desktop 与 Mobile 差异说明

| 差异点 | Desktop | Mobile |
|---|---|---|
| Header | 左头像 + 中导航 + 右登录/主题切换 | 左头像 + 右菜单按钮 |
| Hero | 头像在左，标题在右 | 头像在上，标题在下 |
| Content | 左文章列表 8 列，右卡片 4 列 | 单列，文章列表在上，卡片在下 |
| Footer | 三栏横向 | 单列纵向 |
| 导航 | 横向展开 | 抽屉式，点击菜单按钮展开 |
| 字体 | Hero 64px | Hero 40px |
| 间距 | 大间距，64px 为主 | 小间距，32px 为主 |
| 头像 | 120 × 120 | 96 × 96 |

---

## 九、最终前端实现检查清单

- [ ] 页面背景 Light `#FAFAF9`，Dark `#0E0E0E`
- [ ] 内容最大宽度 Desktop 1120px，Laptop 1024px，Mobile 350px
- [ ] Header 高度 Desktop 72px，Mobile 64px
- [ ] 栅格 Desktop 12 列，Mobile 4 列
- [ ] Hero 主标题 Desktop 64px / 72px / 600，Mobile 40px / 48px / 600
- [ ] 正文字体 16px / 28px / 400
- [ ] 辅助文字 14px / 22px / 400
- [ ] 卡片圆角 16px，按钮圆角 999px，输入框圆角 12px
- [ ] 卡片阴影 Light `0 1px 2px rgba(0,0,0,0.04), 0 8px 24px rgba(0,0,0,0.04)`
- [ ] Dark Mode 阴影 `0 1px 2px rgba(0,0,0,0.4), 0 8px 24px rgba(0,0,0,0.3)`
- [ ] Lime 强调色 `#D4FF3F`
- [ ] Header 包含头像、导航、登录按钮、主题切换、移动端菜单按钮
- [ ] Hero 包含头像、主标题、副标题、社交链接
- [ ] Content 包含近期文章、文章卡片、动态更新订阅卡片、工作经历卡片
- [ ] Footer 包含版权、GitHub、访问量、最近访客、底部导航
- [ ] 页面加载动画 600ms，`cubic-bezier(0.22,1,0.36,1)`
- [ ] Hero 标题淡入 700ms，delay 120ms
- [ ] 头像 Hover `scale(1.04)`，`translateY(-4px)`，200ms
- [ ] 导航 Hover 下划线 `scaleX(0) → 1`，160ms
- [ ] 文章卡片 Hover `translateY(-2px)`，200ms
- [ ] 订阅表单提交后按钮变“已订阅”，背景 Lime
- [ ] 主题切换图标旋转 180deg，320ms
- [ ] 所有交互元素键盘可达
- [ ] 所有图片有 `alt`
- [ ] 所有按钮有 `aria-label`
- [ ] 移动端菜单支持 `Esc` 关闭
- [ ] 焦点态使用 `color-focus-ring`
- [ ] Dark Mode 所有颜色 token 正确切换