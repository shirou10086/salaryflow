package com.salaryflow.app.update

import android.content.Intent
import java.io.File

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

interface UpdateManager {
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult

    suspend fun download(
        release: UpdateRelease,
        onProgress: (Int) -> Unit,
    ): File

    fun needsInstallPermission(): Boolean

    fun installPermissionIntent(): Intent

    fun install(file: File): InstallResult
}
