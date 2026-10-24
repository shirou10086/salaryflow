package com.salaryflow.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val RELEASE_API_URL =
    "https://api.github.com/repos/shirou10086/salaryflow/releases/latest"
private const val MAX_APK_BYTES = 100L * 1024L * 1024L

class AppVersion private constructor(
    private val parts: List<Int>,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        repeat(size) { index ->
            val comparison = (parts.getOrElse(index) { 0 })
                .compareTo(other.parts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    override fun equals(other: Any?): Boolean =
        other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int =
        parts.dropLastWhile { it == 0 }.ifEmpty { listOf(0) }.hashCode()

    companion object {
        private val versionPattern = Regex("""^v?(\d+(?:\.\d+){0,3})(?:[-+].*)?$""")

        fun parse(raw: String): AppVersion? {
            val match = versionPattern.matchEntire(raw.trim()) ?: return null
            val parts = match.groupValues[1]
                .split('.')
                .map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}

data class UpdateRelease(
    val version: AppVersion,
    val tagName: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long,
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val release: UpdateRelease) : UpdateCheckResult
}

sealed interface InstallResult {
    data object Started : InstallResult
    data object PermissionRequired : InstallResult
    data object Failed : InstallResult
}

class UpdateManager(private val context: Context) {

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val current = AppVersion.parse(currentVersion)
            ?: throw IOException("Unsupported current version")
        val release = fetchLatestRelease()
        if (release.version > current) {
            UpdateCheckResult.Available(release)
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    suspend fun download(
        release: UpdateRelease,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (release.sizeBytes > MAX_APK_BYTES) {
            throw IOException("Update package is too large")
        }
        val directory = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(directory, "SalaryFlow-v${release.version}.apk")
        val partial = File(directory, "${target.name}.part")
        partial.delete()

        try {
            val connection = openConnection(release.apkUrl, "application/octet-stream")
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("Update download failed with HTTP $responseCode")
                }
                val totalBytes = connection.contentLengthLong
                    .takeIf { it > 0L }
                    ?: release.sizeBytes.takeIf { it > 0L }
                    ?: -1L
                var downloadedBytes = 0L
                var lastProgress = -1

                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count <= 0) break
                            downloadedBytes += count
                            if (downloadedBytes > MAX_APK_BYTES) {
                                throw IOException("Update package is too large")
                            }
                            output.write(buffer, 0, count)
                            if (totalBytes > 0L) {
                                val progress = ((downloadedBytes * 100L) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            verifyApk(partial, release.version)
            target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            onProgress(100)
            target
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }
    }

    fun needsInstallPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    fun install(file: File): InstallResult {
        if (needsInstallPermission()) return InstallResult.PermissionRequired
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updates",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.Started
        } catch (_: Throwable) {
            InstallResult.Failed
        }
    }

    private fun fetchLatestRelease(): UpdateRelease {
        val connection = openConnection(RELEASE_API_URL, "application/vnd.github+json")
        val body = try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Update check failed with HTTP $responseCode")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
        return parseRelease(JSONObject(body))
    }

    private fun parseRelease(json: JSONObject): UpdateRelease {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) {
            throw IOException("Latest release is not stable")
        }
        val tagName = json.optString("tag_name").trim()
        val version = AppVersion.parse(tagName)
            ?: throw IOException("Release has an invalid version")
        val assets = json.optJSONArray("assets")
            ?: throw IOException("Release has no assets")
        val apkAsset = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { asset ->
                val name = asset.optString("name")
                name.endsWith(".apk", ignoreCase = true) &&
                    !name.contains("unsigned", ignoreCase = true) &&
                    !name.contains("debug", ignoreCase = true)
            }
            ?: throw IOException("Release has no installable APK")
        val apkUrl = apkAsset.optString("browser_download_url")
        if (!apkUrl.startsWith("https://")) {
            throw IOException("Release APK URL is not secure")
        }
        return UpdateRelease(
            version = version,
            tagName = tagName,
            title = json.optString("name").ifBlank { tagName },
            notes = cleanReleaseNotes(json.optString("body")),
            apkUrl = apkUrl,
            apkName = apkAsset.optString("name"),
            sizeBytes = apkAsset.optLong("size"),
        )
    }

    private fun openConnection(url: String, accept: String): HttpURLConnection {
        val parsedUrl = URL(url)
        if (parsedUrl.protocol != "https") {
            throw IOException("Only HTTPS update URLs are allowed")
        }
        return (parsedUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "SalaryFlow-Android")
        }
    }

    private fun verifyApk(file: File, expectedVersion: AppVersion) {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("Downloaded APK cannot be read")
        if (archive.packageName != context.packageName) {
            throw SecurityException("Downloaded APK package does not match")
        }
        val archiveVersion = AppVersion.parse(archive.versionName.orEmpty())
            ?: throw IOException("Downloaded APK has an invalid version")
        if (archiveVersion != expectedVersion) {
            throw SecurityException("Downloaded APK version does not match release")
        }
        val installed = packageManager.getPackageInfo(context.packageName, flags)
        val archiveDigests = signingDigests(archive, flags)
        if (archiveDigests.isEmpty() || archiveDigests != signingDigests(installed, flags)) {
            throw SecurityException("Downloaded APK signature does not match")
        }
        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }
        val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installed.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            installed.versionCode.toLong()
        }
        if (archiveCode < installedCode) {
            throw SecurityException("Downloaded APK is older than the installed app")
        }
    }

    private fun signingDigests(packageInfo: PackageInfo, flags: Int): Set<String> {
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            packageInfo.signatures
        } ?: return emptySet()
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }
        }
    }

    private fun cleanReleaseNotes(raw: String): String =
        raw.lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("#")
                    .trim()
                    .removePrefix("-")
                    .trim()
                    .removePrefix("*")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .joinToString("\n")

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
