# 发布新版本

版本号规则：`v1.0.0` → `v1.0.1` → … → `v1.0.9` → `v1.1.0`，即语义化的「主.次.修订」，逐位进位。

## 1. 改版本号（两处必须一致）

`app/build.gradle.kts`：

```kotlin
versionCode = 10001          // major * 10000 + minor * 100 + patch
versionName = "1.0.1"
```

`app/src/main/java/com/yg/scoreanalysis/data/AppVersion.kt`：

```kotlin
const val NAME = "1.0.1"
const val CODE = 10001
val VERSION = Version(1, 0, 1)
```

> `VersionTest` 会校验这两处是否脱节，改漏一处测试会失败。

## 2. 打包

```bat
set JAVA_HOME=D:\Application\jdk-17.0.13+11
set GRADLE_USER_HOME=D:\Application\gradle-home
cd /d D:\analysis\ScoreAnalysis
gradle.bat testDebugUnitTest assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

## 3. 建 release

1. 仓库 → Releases → **Draft a new release**
2. **Choose a tag** 填 `v1.0.1`（必须带 `v` 前缀，版本号必须比上一个高），选 *Create new tag*
3. Title 随意，可写 `v1.0.1`
4. **Describe this release** 写更新说明——这段正文会原样显示在 App 的更新弹窗里，支持 Markdown
5. 附件上传 **apk 文件**（文件名以 `.apk` 结尾；有多个 apk 时优先选名字不含 debug 的）
6. Publish release

App 通过 `releases/latest` 接口读取，只会看到「最新的正式 release」。Draft 与 Pre-release 不会被读到。

## 4. 校验

打开旧版 App，设置页点「检查更新」，应弹出新版本说明；点「下载更新」应能下载并拉起安装器。

## 注意

- 更新用的是 `applicationId` + 签名一致的 APK，换签名会导致安装失败
- 若 App 里显示「服务器返回 HTTP 404」，说明还没有任何正式 release
- 若显示「无法连接更新服务器」，多为网络问题；镜像失效时可在 `GitHubRepo.MIRRORS` 增删前缀
