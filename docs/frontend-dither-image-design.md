# 前端设计稿：Dither 图片与悬浮还原效果

> 状态：设计稿
>
> 日期：2026-09-29
>
> 参考方向：Editorial / print design；参考 `awesome-design-md` 的 Wired editorial 语言，并结合 Cali.so 的公开实现进行抽象。

## 1. 设计目标

图片默认以“纸张上的像素印刷品”呈现，降低照片在写作列表中的视觉喧宾感；用户把鼠标移到图片上时，印刷层淡出，显示原始图片，让交互像“拿起一张照片”一样自然。

这个效果的重点不是普通的 CSS 模糊、马赛克或滤镜，而是：

1. 原图始终存在并负责尺寸、加载和可访问性。
2. Canvas 在原图上方生成一层低分辨率、有序抖动的纸墨图案。
3. 悬浮或键盘聚焦时只改变遮罩层的透明度，不重新加载或替换原图。
4. 触摸设备没有 hover，因此保留稳定的印刷状态，并可通过点击扩展为显式切换。

## 2. 视觉语言

### 2.1 质感

- 画面像纸张上的单色印刷，不使用渐变和强烈彩色滤镜。
- 默认图片由纸色和深色墨组成，避免主题切换时产生不一致的照片色彩。
- 点阵应保留图片的明暗轮廓，而不是把照片处理成不可识别的纯噪声。
- 悬浮状态显示真实照片，但只做透明度过渡，不改变图片尺寸或裁剪比例。

### 2.2 推荐颜色

```css
:root {
  --print-paper: oklch(0.98 0.004 95);
  --print-ink: oklch(0.28 0.012 95);
  --print-duration: 300ms;
  --print-ease: cubic-bezier(0.2, 0.8, 0.2, 1);
}
```

颜色属于“印刷层”而不是主题文本色。暗色主题可以继续使用相同的纸墨组合，保持图片作为物理印刷品的独立性；若产品视觉要求更强的暗色适配，再单独定义 dark token，不直接复用正文前景色。

## 3. 组件结构

推荐拆成两个组件：

```tsx
type DitheredImageProps = Omit<ImageProps, 'src'> & {
  src: string;
  mode?: 'dither' | 'collage';
};

function DitheredImage({ src, mode = 'dither', ...props }: DitheredImageProps) {
  const imageRef = useRef<HTMLImageElement>(null);

  return (
    <>
      <Image ref={imageRef} src={src} {...props} />
      <DitherVeil imageRef={imageRef} src={src} mode={mode} />
    </>
  );
}
```

### 3.1 `DitheredImage`

职责：

- 渲染 Next/Image 或普通 `<img>` 原图。
- 保持原图的 `alt`、尺寸、加载策略和布局占位。
- 创建并传递图片引用给 `DitherVeil`。
- 不在自身内部处理像素算法。

### 3.2 `DitherVeil`

职责：

- 创建覆盖在原图上的全尺寸 Canvas。
- 等待图片可采样后生成点阵。
- 监听容器尺寸变化并重绘。
- 在 reduced motion 或 Canvas 不可用时提供静态回退。
- 仅在明确支持触摸切换的模式下监听 click，避免把 hover 逻辑和链接行为混在一起。

推荐 DOM：

```html
<span class="print-image group">
  <img class="print-image-source" alt="..." />
  <canvas class="dither-veil" aria-hidden="true"></canvas>
</span>
```

Canvas 是纯装饰层，必须 `aria-hidden="true"`，不能重复朗读图片内容。

## 4. 图像处理算法

### 4.1 采样尺寸

默认点阵单元：`2.5 CSS px`。

```ts
const PIXEL_CELL = 2.5;
const cols = Math.max(1, Math.round(rect.width / PIXEL_CELL));
const rows = Math.max(1, Math.round(rect.height / PIXEL_CELL));
```

Canvas 的实际像素尺寸乘以 `devicePixelRatio`，但绘制坐标仍使用 CSS 像素，确保 Retina 屏幕上的边缘清晰：

```ts
const dpr = window.devicePixelRatio || 1;
canvas.width = Math.round(rect.width * dpr);
canvas.height = Math.round(rect.height * dpr);
ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
```

### 4.2 亮度计算与归一化

先将原图缩小绘制到离屏 Canvas，再使用 `getImageData` 读取采样像素。亮度使用感知加权：

```ts
const luminance =
  (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255;
```

为了避免一张过亮或过暗的图片全部变成单色，使用第 5 和第 95 百分位作为明暗边界：

```ts
const normalized = clamp((luminance - p05) / Math.max(0.05, p95 - p05));
```

### 4.3 4×4 Bayer ordered dithering

使用固定的 Bayer 阈值矩阵，使同一张图在 SSR、客户端重绘和不同设备上保持稳定：

```ts
const BAYER_ORDER = [
  [0, 8, 2, 10],
  [12, 4, 14, 6],
  [3, 11, 1, 9],
  [15, 7, 13, 5],
];

const BAYER = BAYER_ORDER.map((row) =>
  row.map((value) => (value + 0.5) / 16),
);
```

每个单元只绘制纸色或墨色：

```ts
const ink = 1 - normalized;
if (ink > BAYER[row % 4][col % 4]) {
  ctx.fillStyle = PRINT_INK;
  ctx.fillRect(x, y, cellWidth, cellHeight);
}
```

绘制前先填满纸色背景，防止透明 Canvas 与页面底色混合导致不同主题下的结果漂移。

## 5. 状态与交互

### 5.1 默认状态

- 原图正常加载。
- Canvas 遮罩 `opacity: 1`。
- 图片容器不改变尺寸。
- Canvas 不接收指针事件。

```css
.dither-veil {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  opacity: 1;
  pointer-events: none;
  transition: opacity var(--print-duration) var(--print-ease);
}
```

### 5.2 桌面悬浮

只有明确支持 hover 的设备才启用悬浮还原，避免触摸设备出现“粘住 hover”的状态：

```css
@media (hover: hover) and (pointer: fine) {
  .group:hover .dither-veil {
    opacity: 0;
  }
}
```

实现上是让 Canvas 淡出，而不是让原图淡入。这样原图一直参与布局，不会造成布局偏移，也不会出现图片重新解码带来的闪烁。

### 5.3 键盘聚焦

如果图片位于链接或按钮内部，键盘聚焦应显示原图：

```css
.group:focus-visible .dither-veil {
  opacity: 0;
}
```

焦点环必须由外层可交互元素提供，Canvas 不承担焦点职责。

### 5.4 触摸设备

写作列表缩略图默认保持 dither 状态；链接第一次点击仍应执行导航，不应因为图片效果阻止默认行为。

如果产品希望在文章封面上支持触摸切换，可在 `mode="collage"` 或显式 `interactive` 模式中监听容器 click：

- 点击一次：按 Bayer 阈值逐步生成完整 dither 图。
- 再点击一次：按反向顺序清除 Canvas，回到原图。
- 动画过程中再次点击：目标状态反转，当前进度继续向新目标移动。
- 该模式不应默认用于普通写作列表，避免影响链接导航。

### 5.5 Reduced motion

```css
@media (prefers-reduced-motion: reduce) {
  .dither-veil {
    transition: none;
  }
}
```

JavaScript 侧也应检查 `prefers-reduced-motion`：

- 禁止 glitch、逐步 dissolve 和声音反馈。
- hover/focus 可以立即切换到最终状态。
- 不隐藏内容，不依赖动画才能看见原图。

## 6. 生命周期与回退

### 6.1 图片加载

图片的 `load` 事件只代表资源完成加载，不一定代表像素已经可被 Canvas 解码。实现应：

1. `img.complete && img.naturalWidth > 0` 时尝试渲染。
2. `load` 时尝试渲染。
3. 如果采样结果完全透明，延迟重试有限次数。
4. `img.decode()` 只作为额外重试信号，不能作为唯一门槛。

### 6.2 Canvas 不可用

以下情况直接显示原图：

- `getContext('2d')` 返回空。
- 图片跨域导致像素读取失败。
- 图片在有限次重试后仍没有有效像素。
- 容器宽高小于最小绘制尺寸。

原图必须是可靠的 graceful fallback，不能因为视觉增强失败而出现空白图片。

### 6.3 尺寸变化

使用 `ResizeObserver` 监听 Canvas 或图片容器：

- 尺寸改变时重新设置 Canvas 分辨率。
- 重新采样并重绘。
- 不修改外层布局尺寸。
- 组件卸载时断开 observer、清理计时器和事件监听。

## 7. 性能约束

- 不为同一张图片创建多个 Canvas。
- 不在 pointermove 中实时重算整张图片。
- 默认只在加载完成、尺寸变化和显式状态切换时绘制。
- 使用低分辨率离屏 Canvas 做采样，避免读取原始大图的全部像素。
- `getImageData` 的 Canvas 使用 `{ willReadFrequently: true }`。
- 进入视口前不强制高成本处理；必要时配合懒加载或 IntersectionObserver。
- 清理所有 `setTimeout`，避免路由切换后继续绘制已卸载节点。

## 8. 响应式建议

| 场景 | 点阵单元 | 行为 |
| --- | ---: | --- |
| 写作列表缩略图 | 2.5px | 默认 dither，桌面 hover 显示原图 |
| 文章封面 | 2.5px | 可选 collage/glitch，点击可切换 |
| 极小图片（宽度 < 80px） | 2–3px | 减少复杂动画，保持静态印刷 |
| 触摸设备 | 2.5px | 不依赖 hover；保持普通链接行为 |
| Reduced motion | 2.5px | 取消过渡和逐步动画 |

图片容器使用固定的宽高比或显式尺寸，避免 Canvas 初始化前后发生布局偏移。

## 9. 可访问性要求

- 原图保留真实 `alt` 文本。
- Canvas 设置 `aria-hidden="true"`。
- 装饰层设置 `pointer-events: none`，不能遮挡链接或按钮。
- 悬浮不是唯一信息来源；用户不悬浮时也必须知道图片是什么。
- 键盘用户通过 `:focus-visible` 获得与鼠标悬浮等价的清晰状态。
- 不以颜色变化作为唯一状态提示。
- reduced motion 下仍然保留图片和交互功能。
- 不使用 `cursor: pointer` 暗示 Canvas 本身可点击；只有真正可切换的封面容器才设置指针样式。

## 10. 验收标准

### 视觉

- 默认状态能识别图片主体，但明显呈现纸墨点阵效果。
- 桌面鼠标进入图片后，约 300ms 内平滑显示原图。
- 鼠标离开后，Canvas 恢复显示，过渡不改变图片尺寸。
- 不同图片、不同主题和不同 DPR 下不会出现明显的空白、拉伸或错位。

### 交互

- 触摸设备不依赖 hover，也不会出现 hover 卡死。
- 链接图片首次点击仍正常导航。
- 可交互封面支持中途反向切换，不会出现两个计时器同时绘制。
- 键盘聚焦时图片清晰可见。

### 工程

- 图片加载失败时保留稳定的布局占位和原图 fallback。
- Canvas 不可用时页面仍可用。
- reduced motion 下没有过渡、glitch 或声音反馈。
- 组件卸载后无 observer、timer 或 DOM 事件泄漏。
- 核心逻辑可通过亮度采样、Bayer 判定、尺寸重绘和回退路径测试。

## 11. 参考实现

该设计稿抽象自 Cali.so 的公开实现：

- `components/dither-veil.tsx`：Canvas 采样、亮度归一化、Bayer dither、可选 dissolve。
- `app/globals.css`：`.dither-veil` 的定位、透明度过渡、hover/focus 行为。
- `docs/design-language.md`：写作列表缩略图、印刷层和 reduced-motion 的视觉约束。

本设计稿只借鉴技术方案和交互原则，不复制 Cali.so 的品牌文案、个人内容、图片资源或私有服务配置。
