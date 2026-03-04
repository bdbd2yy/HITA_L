package com.stupidtree.hitax.ui.eas.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.repository.EASRepository

class WebLoginEASViewModel(application: Application) : AndroidViewModel(application) {
    private val easRepository = EASRepository.getInstance(application)
    private val webLoginController = MutableLiveData<WebLoginTrigger>()

    val webLoginResultLiveData: LiveData<DataState<Boolean>>
        get() = webLoginController.switchMap {
            easRepository.loginByWebCookies(it.cookies, it.username, it.password)
        }

    fun completeLoginByCookies(
        cookies: HashMap<String, String>,
        username: String?,
        password: String?
    ) {
        webLoginController.value = WebLoginTrigger(cookies, username, password)
    }
}

data class WebLoginTrigger(
    val cookies: HashMap<String, String>,
    val username: String?,
    val password: String?
)
