package com.stupidtree.hitax.ui.eas

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.stupidtree.style.base.BaseActivity
import com.stupidtree.style.ThemeTools
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.ui.eas.login.EASWebLoginActivity

abstract class EASActivity<T : EASViewModel, V : ViewBinding> : BaseActivity<T, V>() {
    private var loginFlowActive = false
    private val easLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            loginFlowActive = false
            if (result.resultCode == Activity.RESULT_OK) {
                onLoginCheckSuccess(true)
            } else {
                onLoginCheckFailed()
            }
        }

    override fun onStart() {
        super.onStart()
        refresh()
        if (consumeSkipLoginCheckOnceFlag()) {
            return
        }
        viewModel.startLoginCheck()
    }

    private fun consumeSkipLoginCheckOnceFlag(): Boolean {
        val currentIntent = intent ?: return false
        val skipLoginCheck = currentIntent.getBooleanExtra(ThemeTools.EXTRA_SKIP_LOGIN_CHECK_ONCE, false)
        if (skipLoginCheck) {
            currentIntent.removeExtra(ThemeTools.EXTRA_SKIP_LOGIN_CHECK_ONCE)
            loginFlowActive = false
        }
        return skipLoginCheck
    }

    abstract fun refresh()

    open fun onLoginCheckSuccess(retry: Boolean) {
        refresh()
    }

    open fun onLoginCheckFailed() {}

    override fun initViews() {
        viewModel.loginCheckResult.observe(this) {
            if (it.state == DataState.STATE.SUCCESS) {
                if (it.data == true) {
                    loginFlowActive = false
                    onLoginCheckSuccess( false)
                } else {
                    if (!loginFlowActive) {
                        loginFlowActive = true
                        easLoginLauncher.launch(Intent(this, EASWebLoginActivity::class.java))
                    }
                }

            }
        }
    }

}
