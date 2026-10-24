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
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.AppRoot
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.theme.SalaryFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SalaryFlowViewModel by viewModels()
    private var quickAddRequest: TxType? = null
    private var celebrationRequest = false
    private var balanceUpdateRequest = false

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
                val balanceUpdate by balanceUpdateState
                AppRoot(
                    viewModel = viewModel,
                    quickAddRequest = quickAdd ?: quickAddRequest,
                    celebrationRequest = celebration || celebrationRequest,
                    balanceUpdateRequest = balanceUpdate || balanceUpdateRequest,
                    onQuickAddConsumed = {
                        quickAddRequest = null
                        quickAddState.value = null
                    },
                    onCelebrationConsumed = {
                        celebrationRequest = false
                        celebrationState.value = false
                    },
                    onBalanceUpdateConsumed = {
                        balanceUpdateRequest = false
                        balanceUpdateState.value = false
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
        balanceUpdateState.value = balanceUpdateRequest
    }

    private val quickAddState = androidx.compose.runtime.mutableStateOf<TxType?>(null)
    private val celebrationState = androidx.compose.runtime.mutableStateOf(false)
    private val balanceUpdateState = androidx.compose.runtime.mutableStateOf(false)

    private fun readRequest(intent: Intent?) {
        quickAddRequest = intent
            ?.getStringExtra(EXTRA_QUICK_ADD_TYPE)
            ?.let { value -> runCatching { TxType.valueOf(value) }.getOrNull() }
            ?: quickAddRequest
        if (intent?.getBooleanExtra(EXTRA_CELEBRATION, false) == true) {
            celebrationRequest = true
            intent.removeExtra(EXTRA_CELEBRATION)
        }
        if (intent?.getBooleanExtra(EXTRA_BALANCE_UPDATE, false) == true) {
            balanceUpdateRequest = true
            intent.removeExtra(EXTRA_BALANCE_UPDATE)
        }
        intent?.removeExtra(EXTRA_QUICK_ADD_TYPE)
    }

    companion object {
        private const val EXTRA_QUICK_ADD_TYPE = "extra_quick_add_type"
        private const val EXTRA_CELEBRATION = "extra_celebration"
        private const val EXTRA_BALANCE_UPDATE = "extra_balance_update"

        fun intent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        fun quickAddIntent(context: Context, type: TxType): Intent =
            intent(context).putExtra(EXTRA_QUICK_ADD_TYPE, type.name)

        fun celebrationIntent(context: Context): Intent =
            intent(context).putExtra(EXTRA_CELEBRATION, true)

        fun balanceUpdateIntent(context: Context): Intent =
            intent(context).putExtra(EXTRA_BALANCE_UPDATE, true)
    }
}
