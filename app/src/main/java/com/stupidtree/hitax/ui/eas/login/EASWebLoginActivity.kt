package com.stupidtree.hitax.ui.eas.login

import android.annotation.SuppressLint
import android.os.Message
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.databinding.ActivityEasWebLoginBinding
import com.stupidtree.style.base.BaseActivity
import java.util.Locale

class EASWebLoginActivity : BaseActivity<WebLoginEASViewModel, ActivityEasWebLoginBinding>() {
    private var completionRequested = false
    private var hasRetriedWithDirectCas = false

    override fun initViewBinding(): ActivityEasWebLoginBinding {
        return ActivityEasWebLoginBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<WebLoginEASViewModel> {
        return WebLoginEASViewModel::class.java
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initViews() {
        setToolbarActionBack(binding.toolbar)
        binding.toolbar.title = "教务登录"

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webview, true)
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()

        binding.webview.clearCache(true)
        binding.webview.clearHistory()

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                Log.d(TAG, "onCreateWindow triggered, using host WebView")
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val popupWebView = WebView(this@EASWebLoginActivity)
                popupWebView.settings.javaScriptEnabled = true
                popupWebView.settings.domStorageEnabled = true
                popupWebView.settings.javaScriptCanOpenWindowsAutomatically = true
                popupWebView.settings.setSupportMultipleWindows(true)
                popupWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        binding.webview.loadUrl(request.url.toString())
                        return true
                    }
                }
                transport.webView = popupWebView
                resultMsg.sendToTarget()
                return true
            }
        }
        binding.webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString.replace("; wv", "")
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "onPageStarted: $url")
                if (!completionRequested) {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                detectLoginCompleted(request.url.toString())
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: $url, contentHeight=${view.contentHeight}")
                if (!completionRequested) {
                    binding.progressBar.visibility = View.GONE
                }
                if (!completionRequested && url == "about:blank") {
                    binding.progressBar.visibility = View.VISIBLE
                    val nextUrl = if (hasRetriedWithDirectCas) {
                        EAS_DIRECT_CAS_LOGIN_URL
                    } else {
                        hasRetriedWithDirectCas = true
                        EAS_COMBINED_LOGIN_URL
                    }
                    binding.webview.loadUrl(nextUrl)
                    return
                }
                if (!completionRequested && !hasRetriedWithDirectCas &&
                    url.startsWith(EAS_DIRECT_CAS_LOGIN_URL) && view.contentHeight == 0
                ) {
                    hasRetriedWithDirectCas = true
                    binding.progressBar.visibility = View.VISIBLE
                    binding.webview.loadUrl(EAS_COMBINED_LOGIN_URL)
                    return
                }
                detectLoginCompleted(url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError: ${request.url}, code=${error.errorCode}, desc=${error.description}")
                if (request.isForMainFrame) {
                    completionRequested = false
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EASWebLoginActivity, "页面加载失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "onReceivedHttpError: ${request.url}, status=${errorResponse.statusCode}")
                if (request.isForMainFrame && errorResponse.statusCode >= 400 && !completionRequested) {
                    binding.progressBar.visibility = View.VISIBLE
                    val nextUrl = if (hasRetriedWithDirectCas) {
                        EAS_DIRECT_CAS_LOGIN_URL
                    } else {
                        hasRetriedWithDirectCas = true
                        EAS_COMBINED_LOGIN_URL
                    }
                    binding.webview.loadUrl(nextUrl)
                }
            }
        }

        viewModel.webLoginResultLiveData.observe(this) {
            binding.progressBar.visibility = View.GONE
            if (it.state == DataState.STATE.SUCCESS && it.data == true) {
                setResult(RESULT_OK)
                finish()
            } else if (it.state != DataState.STATE.NOTHING) {
                completionRequested = false
            }
        }

        binding.webview.loadUrl(EAS_DIRECT_CAS_LOGIN_URL)
    }

    private fun detectLoginCompleted(url: String) {
        if (completionRequested) return
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        val host = uri?.host?.lowercase(Locale.ROOT)
        val path = uri?.path ?: ""
        val hitTarget = host == "jw.hitsz.edu.cn" &&
            (path.startsWith("/casLogin") || path.startsWith("/cas"))
        Log.d(TAG, "detectLoginCompleted url=$url, hitTarget=$hitTarget")
        if (!hitTarget) return

        completionRequested = true
        binding.progressBar.visibility = View.VISIBLE

        val repo = EASRepository.getInstance(application)
        val oldToken = repo.getEasToken()
        val cookieMap = collectCookieMap()
        if (cookieMap.isEmpty()) {
            completionRequested = false
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "未获取到登录Cookie，请继续完成认证", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.completeLoginByCookies(cookieMap, oldToken.username, oldToken.password)
    }

    private fun collectCookieMap(): HashMap<String, String> {
        val cookieManager = CookieManager.getInstance()
        val map = hashMapOf<String, String>()
        parseCookieString(cookieManager.getCookie("http://jw.hitsz.edu.cn"), map)
        parseCookieString(cookieManager.getCookie("https://jw.hitsz.edu.cn"), map)
        parseCookieString(cookieManager.getCookie("https://ids.hit.edu.cn"), map)
        return HashMap(map)
    }

    private fun parseCookieString(raw: String?, target: MutableMap<String, String>) {
        if (raw.isNullOrBlank()) return
        raw.split(";").forEach { pair ->
            val kv = pair.trim().split("=", limit = 2)
            if (kv.size == 2 && kv[0].isNotBlank()) {
                target[kv[0].trim()] = kv[1].trim()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        binding.webview.stopLoading()
        binding.webview.webChromeClient = null
        binding.webview.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "EASWebLogin"
        private const val EAS_COMBINED_LOGIN_URL =
            "https://ids.hit.edu.cn/authserver/combinedLogin.do?type=IDSUnion&appId=ff2dfca3a2a2448e9026a8c6e38fa52b&success=http%3A%2F%2Fjw.hitsz.edu.cn%2FcasLogin"
        private const val EAS_DIRECT_CAS_LOGIN_URL = "https://jw.hitsz.edu.cn/casLogin"
    }
}
