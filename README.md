# 朋友圈生成（MomentMock）

一个用来「造」朋友圈截图的 Android 小工具：填好头像、昵称、正文、图片（或公众号文章、视频）、点赞数、评论，一键生成一张朋友圈详情页。

纯 Kotlin + 原生 View 实现，**零第三方依赖**，没有 WebView、没有截图模板。

## 背景

生成朋友圈截图的项目很多，但是几乎全部都是前端项目，生成的截图真看细节的话一眼假。

本仓库通过逆向微信 v8.0.79 安卓客户端，完全复刻微信渲染细节，生成的界面和真实的朋友圈详情页**逐像素点一致**。

示例见 `examples/`。

## 功能

- **发布人**：头像（内置 300 张头像库随机 / 从相册选）、昵称
- **正文**：多行文字、时间、位置
- **内容形式（三选一）**：
  - **图片**：1~9 张；单图按微信的 200dp 盒子规则等比缩放，多图走九宫格
  - **视频**：展示封面 + 播放按钮
  - **公众号卡片**：标题、描述、来源、封面
- **点赞**：显示 N 个头像
- **评论**：头像、昵称、时间、内容；支持「回复某某」
- 生成后直接手机截屏即可

## 编译

环境：JDK 17+、Android SDK（platform 34、build-tools 34.0.0）。

```bash
# Windows
gradlew.bat :app:buildApk

# macOS / Linux
./gradlew :app:buildApk
```

产物在工程根目录：`moment-mock.apk`

构建时会自动查找 Android SDK：

1. 工程根目录的 `local.properties` 里的 `sdk.dir`
2. 环境变量 `ANDROID_HOME`

也可以直接用 Android Studio 打开本目录，Run 即可。

## 目录

```
app/src/main/java/com/example/momentmock/
  MainActivity.kt     入口
  ConfigScreen.kt     配置页
  MomentScreen.kt     详情页
  Config.kt           配置模型 + JSON 持久化
  SnsMoment.kt        展示用数据模型
  Images.kt           图片读取、头像库
  Ui.kt               dp/sp 换算、圆角头像、正方形格子
app/src/main/res/     图标、颜色、尺寸、样式
app/src/main/assets/avatars/   300 张头像
```

## 两个重要的坑

- **密度**：微信用 `MMDensityManager` 把屏幕按 400dp 宽做网格（`density = 短边 / 400`）。
- **分隔符**：位置、公众号来源里的「·」用的是 `U+2006 · U+00B7 · U+2006`（六分之一空格 + 中点），不是普通空格。

## 免责声明

本项目与腾讯、微信官方无关，仅供学习与个人测试使用；

请勿用于伪造他人信息、冒充他人或误导性传播，由此产生的一切后果由使用者自行承担。

## License

代码采用 [MIT](LICENSE) 许可证。
