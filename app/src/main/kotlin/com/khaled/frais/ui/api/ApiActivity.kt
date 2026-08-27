package com.khaled.frais.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khaled.frais.R
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.AppManager
import com.khaled.frais.app.FraisApi
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.theme.AppTheme
import com.khaled.frais.utils.HPackages
import com.khaled.frais.utils.HTarget
import com.khaled.frais.utils.HUI

class ApiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            if (handleAction(intent.action)) finish()
        }.onFailure(::setErrorDialog)
    }

    private fun handleAction(action: String?): Boolean {
        when (action) {
            Intent.ACTION_SHOW_APP_INFO -> {
                setContent { AppTheme { RedirectBottomSheet(requirePackage) } }
                return false
            }

            Intent.ACTION_VIEW -> return handleSchema(intent.data)

            FraisApi.ACTION_LAUNCH -> launchApp(requirePackage)
            FraisApi.ACTION_FREEZE -> setAppFrozen(requirePackage, true)
            FraisApi.ACTION_UNFREEZE -> setAppFrozen(requirePackage, false)

            FraisApi.ACTION_FREEZE_ALL -> setListFrozen(true)
            FraisApi.ACTION_UNFREEZE_ALL -> setListFrozen(false)
            FraisApi.ACTION_FREEZE_NON_WHITELISTED -> setListFrozen(true, skipWhitelisted = true)
            FraisApi.ACTION_LOCK -> lockScreen(false)
            FraisApi.ACTION_LOCK_FREEZE -> lockScreen(true)
            else -> throw IllegalArgumentException("Unknown action:\n$action")
        }
        return true
    }

    private fun handleSchema(uri: Uri?): Boolean {
        if (uri?.scheme != "frais") throw IllegalArgumentException("Unknown scheme:\n${uri?.scheme}")
        return handleAction(
            when (uri.host) {
                "launch" -> FraisApi.ACTION_LAUNCH
                "freeze" -> FraisApi.ACTION_FREEZE
                "unfreeze" -> FraisApi.ACTION_UNFREEZE
                "freeze_all" -> FraisApi.ACTION_FREEZE_ALL
                "unfreeze_all" -> FraisApi.ACTION_UNFREEZE_ALL
                "freeze_non_whitelisted" -> FraisApi.ACTION_FREEZE_NON_WHITELISTED
                "lock" -> FraisApi.ACTION_LOCK
                "lock_freeze" -> FraisApi.ACTION_LOCK_FREEZE
                else -> throw IllegalArgumentException("Unknown host:\n${uri.host}")
            }
        )
    }

    private fun setErrorDialog(t: Throwable) = setContent { AppTheme { ErrorDialog(t) } }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RedirectBottomSheet(pkg: String) = ModalBottomSheet(
        onDismissRequest = ::finish, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column {
            Text(
                text = HPackages.getApplicationInfoOrNull(pkg)?.let { AppInfo(it).name } ?: pkg,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            ClickableItem(
                icon = Icons.AutoMirrored.Outlined.Launch, title = R.string.action_launch
            ) { launchApp(pkg) }
            ClickableItem(
                icon = Icons.Rounded.AcUnit, title = R.string.action_freeze
            ) { setAppFrozen(pkg, true) }
            ClickableItem(
                icon = Icons.Rounded.BrightnessLow, title = R.string.action_unfreeze
            ) { setAppFrozen(pkg, false) }
        }
    }

    @Composable
    private fun ClickableItem(icon: ImageVector, @StringRes title: Int, onClick: () -> Unit) = Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = {
            runCatching {
                onClick()
                finish()
            }.onFailure(::setErrorDialog)
        }), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(16.dp)
        )
        Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
    }

    @Composable
    private fun ErrorDialog(t: Throwable) = AlertDialog(
        text = { Text(text = t.message ?: t.stackTraceToString()) },
        onDismissRequest = ::finish,
        confirmButton = {
            TextButton(onClick = ::finish) {
                Text(text = stringResource(android.R.string.ok))
            }
        })

    private val requirePackage: String
        get() = intent.run {
            if (action == Intent.ACTION_VIEW) data?.getQueryParameter(FraisData.KEY_PACKAGE)
            else getStringExtra(
                if (action != Intent.ACTION_SHOW_APP_INFO) FraisData.KEY_PACKAGE
                else if (HTarget.N) Intent.EXTRA_PACKAGE_NAME
                else "android.intent.extra.PACKAGE_NAME"
            )
        }?.also {
            HPackages.getApplicationInfoOrNull(it) ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("Package must not be null")

    private fun launchApp(pkg: String) {
        if (AppManager.isAppFrozen(pkg)) {
            AppManager.setAppFrozen(pkg, false)
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it)
        } ?: throw ActivityNotFoundException(getString(R.string.activity_not_found))
    }

    private fun setAppFrozen(pkg: String, frozen: Boolean) = when {
        AppManager.isAppFrozen(pkg) != frozen && !AppManager.setAppFrozen(
            pkg, frozen
        ) -> throw IllegalStateException(getString(R.string.permission_denied))

        else -> {
            HUI.showToast(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                HPackages.getApplicationInfoOrNull(pkg)?.let { AppInfo(it).name } ?: pkg
            )
        }
    }

    private fun setListFrozen(
        frozen: Boolean, list: List<AppInfo>? = null, skipWhitelisted: Boolean = false
    ) {
        val actualList = list ?: HPackages.getInstalledApplications().map { AppInfo(it.packageName) }
        val filtered =
            actualList.filter { AppManager.isAppFrozen(it.packageName) != frozen && !(skipWhitelisted && it.whitelisted) }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> throw IllegalStateException(getString(R.string.permission_denied))
            else -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, result
                )
            }
        }
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setListFrozen(true)
        if (AppManager.lockScreen.not()) throw IllegalStateException(getString(R.string.permission_denied))
    }
}
