# Google Play 发布与封闭测试

本文对应 WageJar / 存钱罐 `0.4.5 (12)`，`applicationId` 为 `com.salaryflow.app`。
当前 `compileSdk` 和 `targetSdk` 均为 Android 16（API 36）。

## 两个发布渠道

- `github` flavor：内置 GitHub 自动更新，申请 `REQUEST_INSTALL_PACKAGES`，发布 APK。
- `play` flavor：不申请“安装未知应用”权限，不内置自更新，上传 AAB，由 Google Play
  管理更新。

两个 flavor 使用相同包名，不能同时安装在同一台设备上。

## 生成 Play AAB

首次构建可先设置以下环境变量，密码不要写入仓库：

```bash
export SALARYFLOW_KEYSTORE_FILE="$HOME/.android/salaryflow-release.jks"
export SALARYFLOW_KEYSTORE_PASSWORD="<keystore password>"
export SALARYFLOW_KEY_ALIAS="salaryflow"
export SALARYFLOW_KEY_PASSWORD="<key password>"
```

构建并检查：

```bash
./gradlew :app:testPlayDebugUnitTest :app:lintPlayDebug :app:bundlePlayRelease

curl -fsSL -o /tmp/bundletool-all-1.18.0.jar \
  https://github.com/google/bundletool/releases/download/1.18.0/bundletool-all-1.18.0.jar
export BUNDLETOOL=/tmp/bundletool-all-1.18.0.jar
java -jar "$BUNDLETOOL" validate \
  --bundle=app/build/outputs/bundle/playRelease/app-play-release.aab

jarsigner -verify -verbose -certs \
  app/build/outputs/bundle/playRelease/app-play-release.aab
```

上传文件是：

```text
app/build/outputs/bundle/playRelease/app-play-release.aab
```

建议启用 Play App Signing。Play 会使用应用签名密钥重新签名分发给用户的版本，开发者保存的
密钥作为上传密钥。上线后不要丢失上传密钥，也不要让 GitHub APK 和 Play 版本互相覆盖安装。

## Play Console 首次配置

1. 在 Play Console 创建应用，默认语言可按目标市场选择，应用名可使用 `WageJar` 或
   `存钱罐`。
2. 完成开发者账号验证。组织账号不受下面的 12 人规则限制；2023-11-13 之后创建的个人账号
   需要在申请生产权限前完成封闭测试。
3. 完成 Dashboard 和“App content”必填项，包括隐私政策、Data safety、内容分级、
   广告声明、目标受众、新闻应用和金融功能声明。
4. 如果填写金融功能，必须如实描述本地记账、工资估算和存款记录能力，不要声明应用提供
   银行、贷款、投资或支付服务。
5. Data safety 应按实际实现填写。当前版本把账本和设置保存在本机，不上传账本，也没有广告
   SDK；但提交前仍需按当时接入的 SDK 和控制台问题重新核对。
6. 在“Foreground service permissions”声明 `specialUse`。用途是用户主动开启的实时工资和
   存款通知，停止服务后通知也会停止，并提供收入和支出快捷入口。
7. 完成商店资料、应用图标、手机截图、隐私政策 URL、联系方式和目标国家或地区。

## 封闭测试 14 天怎么做

规则以 [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
为准：至少 12 个测试者必须在申请生产权限时处于已加入状态，并连续加入封闭测试至少
14 天。内部测试轨道不计入这项要求。

1. 在“Test and release > Testing > Closed testing”创建封闭测试轨道。
2. 上传 `versionCode 12` 的 AAB，选择需要测试的国家或地区，确认没有阻断性错误后发布。
3. 在 Testers 页面按 Google 账号邮箱逐个添加测试者，或添加一个包含测试者的 Google Group。
4. 把该轨道的 opt-in URL 发给测试者。测试者必须登录自己的真实 Google 账号，打开链接，
   点击加入测试，再从 Google Play 安装应用。
5. 只“收到邀请”或只被加入列表通常不算成功。以 Play Console 显示的已加入人数为准，
   建议准备 15 到 20 人，防止中途退出导致不满足 12 人。
6. 测试者退出后再次加入，14 天会从重新加入时重新计算。申请生产权限当天，至少 12 人仍要
   保持加入并能看到测试版本。
7. 不要只让测试者安装。请让他们实际使用通知、记账、发薪日、统计和语言切换，并记录问题、
   修复版本和反馈结论。
8. 连续 14 天后，在 Play Console Dashboard 查看 `Apply for production`。填写“About your
   closed test”，说明测试人数、测试设备、核心场景、发现的问题、修复情况和剩余风险。
9. Google 审核生产权限通常不超过 7 天，但不是保证时限。被要求补充测试时，应保持测试者
   继续加入，修复问题后重新申请。

注意：14 天的起算应以实际操作时 Play Console 显示的测试状态为准，Google 可能根据应用
复杂度和测试质量要求更多测试。政策可能调整，提交前应重新阅读上面链接的最新官方说明。

## 申请生产版

1. 收到生产权限后，在 Production 创建 release。
2. 使用不低于封闭测试版本的 `versionCode` 上传 AAB。每次上传都必须递增 `versionCode`。
3. 先完成内部或封闭轨道回归，再选择国家或地区并提交审核。
4. 建议先使用分阶段发布，例如 5% 或 20%，观察崩溃、ANR、评论和核心功能表现后再扩大。
5. 生产版本发布后，后续更新继续使用 Play 渠道 AAB，不要切换到 GitHub APK 覆盖安装。

## 发布前检查

- `targetSdk` 为 36，AAB 已签名且 `bundletool validate` 通过。
- Play 版本清单不含 `REQUEST_INSTALL_PACKAGES`。
- 应用内更新入口和 GitHub 检查逻辑不在 Play 版本出现。
- 隐私政策、Data safety、金融功能和前台服务声明与代码实际行为一致。
- 12 人连续加入封闭测试满 14 天，且测试反馈和修复情况已整理。
- 版本号已递增，商店截图、说明、联系人和国家或地区配置完整。

官方参考：

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334
- https://support.google.com/googleplay/android-developer/answer/11926878
