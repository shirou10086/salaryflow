# 贡献指南

感谢你愿意为存钱罐做点事。下面是几条约定：

## 开发环境

- JDK 17
- Android Studio（支持 API 36）及以上，或命令行 Android SDK 36
- GitHub 与 Play flavor 的单元测试都必须通过

## 提交前检查

```bash
./gradlew :app:testGithubDebugUnitTest :app:lintGithubDebug \
  :app:assembleGithubDebug
./gradlew :app:testPlayDebugUnitTest :app:lintPlayDebug \
  :app:bundlePlayDebug
```

## 代码约定

- 计薪 / 结算等纯逻辑写在 `core` 与 `data` 层，保持可在 JVM 上单测；
- 新增计薪规则时，请在 `SalaryClockTest` 补对应用例；
- UI 文案使用中文，金额展示统一走 `Money` 工具，避免各页面格式不一致；
- 账本和设置保持本地存储；除 GitHub flavor 的版本检查与 APK 下载外，不要引入业务网络依赖；
- Play flavor 不应引入自更新、安装未知应用或敏感权限。

## Issue / PR

- Bug 请附上系统版本、机型与复现步骤；
- 功能建议请说明「它解决了打工人什么真实场景」；
- PR 请保持单一主题，方便 review。
