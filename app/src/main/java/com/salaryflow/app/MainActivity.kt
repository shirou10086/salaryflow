package com.salaryflow.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.ui.AppRoot
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.theme.SalaryFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SalaryFlowViewModel by viewModels()
    private var quickAddRequest = false
    private var celebrationRequest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readRequest(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            SalaryFlowTheme(darkTheme = darkTheme) {
                val quickAdd by quickAddState
                val celebration by celebrationState
                AppRoot(
                    viewModel = viewModel,
                    quickAddRequest = quickAdd || quickAddRequest,
                    celebrationRequest = celebration || celebrationRequest,
                    onQuickAddConsumed = {
                        quickAddRequest = false
                        quickAddState.value = false
                    },
                    onCelebrationConsumed = {
                        celebrationRequest = false
                        celebrationState.value = false
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readRequest(intent)
        quickAddState.value = quickAddRequest
        celebrationState.value = celebrationRequest
    }

    private val quickAddState = androidx.compose.runtime.mutableStateOf(false)
    private val celebrationState = androidx.compose.runtime.mutableStateOf(false)

    private fun readRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_QUICK_ADD, false) == true) {
            quickAddRequest = true
            intent.removeExtra(EXTRA_QUICK_ADD)
        }
        if (intent?.getBooleanExtra(EXTRA_CELEBRATION, false) == true) {
            celebrationRequest = true
            intent.removeExtra(EXTRA_CELEBRATION)
        }
    }

    companion object {
        private const val EXTRA_QUICK_ADD = "extra_quick_add"
        private const val EXTRA_CELEBRATION = "extra_celebration"

        fun intent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        fun quickAddIntent(context: Context): Intent =
            intent(context).putExtra(EXTRA_QUICK_ADD, true)

        fun celebrationIntent(context: Context): Intent =
            intent(context).putExtra(EXTRA_CELEBRATION, true)
    }
}
