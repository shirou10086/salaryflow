package com.salaryflow.app.update

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.IOException

fun createUpdateManager(@Suppress("UNUSED_PARAMETER") context: Context): UpdateManager =
    PlayUpdateManager()

private class PlayUpdateManager : UpdateManager {
    override suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult =
        UpdateCheckResult.UpToDate

    override suspend fun download(
        release: UpdateRelease,
        onProgress: (Int) -> Unit,
    ): File = throw IOException("Updates are managed by Google Play")

    override fun needsInstallPermission(): Boolean = false

    override fun installPermissionIntent(): Intent = Intent(Intent.ACTION_VIEW)

    override fun install(file: File): InstallResult = InstallResult.Failed
}
