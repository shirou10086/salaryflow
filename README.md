# 薪流 SalaryFlow

> 把「这个月还有多少工资没到手」变成通知栏里一个一直在涨的数字。

薪流是一个纯本地记账 Android 应用：

- 通知栏常驻显示今日已赚、当前存款、今日支出和发薪倒计时；
- 工作时段内实时计算工资，发薪日前显示为虚线预计收入；
- 到发薪日幂等写入完整工资收入，虚线收入切换为实线并播放到账动画；
- 首页提供独立的 `− 记支出`、`+ 记收入`，也可直接从通知栏快捷记账；
- 存款按收入和支出自动推算，每月提醒输入一次真实余额进行可追溯校准；
- 所有金额和流水只保存在本机，不申请网络权限，也没有账号和埋点。

![首页](docs/screenshots/home.png)

## 下载安装

前往 [GitHub Releases](https://github.com/shirou10086/salaryflow/releases/latest) 下载已签名的
[`SalaryFlow-v0.1.1.apk`](https://github.com/shirou10086/salaryflow/releases/latest/download/SalaryFlow-v0.1.1.apk)。

- 最低支持 Android 8.0（API 26）；
- 首次安装时，系统可能要求允许当前浏览器或文件管理器「安装未知应用」；
- APK 使用独立发布证书签名，SHA-256 校验值会附在对应 Release 中。

## 首次设置

首次打开会进入本地初始化，不需要注册云账号：

1. 输入税后月薪、常用计薪天数和实际发薪日；
2. 选择常规上下班时间；
3. 可选填写当前实际存款，留空时从 0 开始；
4. 选择是否开启通知栏实时工资和每月余额校准提醒。

设置完成后会直接进入首页，后续可随时在「设置」中修改。

## 功能

### 通知栏实时工资

![通知栏](docs/screenshots/notification.png)

- 前台服务按 1 / 5 / 10 / 60 秒刷新，默认每秒更新；
- 通知显示今日已赚、当前存款、今日支出、下班倒计时和发薪倒计时；
- 可直接执行「记支出」「记收入」「暂停 / 继续」；
- 支持隐藏金额，锁屏时不展示具体数字；
- 重启手机后可按设置恢复，Android 12+ 以后台系统限制为准。

### 发薪日到账

![发薪日动画](docs/screenshots/payday.png)

- 结算周期为上月发薪日次日到本月发薪日；
- 到账后按完整月薪写入一条工资收入，避免按比例转存造成重复计算；
- 使用 `settlementKey` 幂等去重，服务、WorkManager 和打开 App 多入口触发也只入账一次；
- 漏掉多个周期时可自动补记，最多 24 期；
- 到账后首页预计收入卡从虚线和进度轨道切换为实线、已到账状态。

![工资已到账](docs/screenshots/home-landed.png)

### 记账与统计

![账本](docs/screenshots/ledger.png)

- 首页独立提供 `− 记支出`、`+ 记收入`；
- 10 个内置支出分类、5 个收入分类，可填写金额、分类和备注；
- 按月浏览、按天分组、支持删除和隐藏金额；
- 自动记账与手动记账在同一本流水中展示，并标记是否自动生成。

![统计](docs/screenshots/stats.png)

- 本月支出、收入与结余；
- 近 6 个月支出柱状图；
- 本月支出分类环形图；
- 按日计算的存款余额走势。

### 存款与余额校准

当前存款使用同一套账本口径计算：

```text
当前存款 = 手动/自动收入 - 支出 + 历次实际余额校准差额
```

支付宝、微信个人零钱及普通网银目前没有面向普通开发者的公开余额读取接口。薪流不会监听通知、抓取短信或使用无障碍权限读取其他应用。

因此应用提供手动校准：

- 首次使用时可填写当前真实存款；
- 后续收入和支出会自动增减存款；
- 每月 1 日可收到一次余额校准提醒；
- 输入实际余额后只新增一条「余额校准」流水，不改写历史记录，也可在账本中查看或删除。

### 桌面小组件

2×2 小组件显示今日已赚、当前存款与服务状态；前台服务运行时会顺带刷新。

## 计算规则

| 指标 | 公式 |
| --- | --- |
| 日薪 | `月薪 ÷ 每月计薪天数`（默认 21.75 天） |
| 每秒工资 | `日薪 ÷ 每个工作日的计薪秒数` |
| 今日已赚 | `今天从上班到现在的计薪秒数 × 每秒工资` |
| 计薪秒数 | `(下班时间 - 上班时间) - 不计薪的午休` |
| 发薪日入账 | `完整月薪` |

午休是否计薪、工作日、计薪天数和发薪日均可在设置中修改。当前版本未内置法定节假日与调休日历。

## 构建

环境要求：JDK 17、Android SDK（`compileSdk 35`、Build Tools 35.0.0）。

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

安装 Debug 包：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

发布版需要在仓库外使用自己的密钥签名，仓库不包含签名文件或密码。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Room 流水账本 + DataStore 设置
- Foreground Service specialUse + NotificationCompat
- WorkManager 发薪日结算兜底和月度余额提醒
- AppWidgetProvider 桌面小组件
- JUnit 覆盖金额、计薪、发薪周期与结算规则

```text
app/src/main/java/com/salaryflow/app/
├── core/        薪资时钟、金额与时间格式
├── data/        Room、DataStore、余额校准、发薪日结算
├── service/     前台通知、开机恢复、结算通知
├── work/        WorkManager 调度
├── widget/      桌面小组件
└── ui/          Compose 页面、主题和到账动画
```

## 隐私

- 不声明 `INTERNET` 权限，代码中不存在网络请求；
- 不读取支付宝、微信、短信或其他应用的数据；
- 所有金额、设置和流水保存在应用私有目录；
- Android 系统备份可能包含本机数据库和设置，可在系统设置中关闭；
- 应用只使用通知、前台服务和开机恢复所需权限。

## 当前限制

- 无法自动读取第三方支付和银行账户余额，需首次填写并每月手动校准；
- 未接入法定节假日与调休日历，工作日规则由用户设置；
- 不提供金融建议，也不代表真实银行账户流水。

欢迎提交 Issue / PR。

## License

[MIT](LICENSE)
