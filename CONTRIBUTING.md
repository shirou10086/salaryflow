# 贡献指南

感谢你愿意为薪流做点事。下面是几条约定：

## 开发环境

- JDK 17
- Android Studio Ladybug 及以上（或命令行 SDK 35）
- `./gradlew :app:testDebugUnitTest` 必须通过

## 提交前检查

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## 代码约定

- 计薪 / 结算等纯逻辑写在 `core` 与 `data` 层，保持可在 JVM 上单测；
- 新增计薪规则时，请在 `SalaryClockTest` 补对应用例；
- UI 文案使用中文，金额展示统一走 `Money` 工具，避免各页面格式不一致；
- 不要引入网络依赖：本项目的核心承诺是「不联网」。

## Issue / PR

- Bug 请附上系统版本、机型与复现步骤；
- 功能建议请说明「它解决了打工人什么真实场景」；
- PR 请保持单一主题，方便 review。
