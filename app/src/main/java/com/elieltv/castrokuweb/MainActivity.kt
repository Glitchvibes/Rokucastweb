package com.elieltv.castrokuweb

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabVideos: FloatingActionButton
    private lateinit var badgeCount: TextView
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Guarda os videos detectados na pagina atual, sem duplicatas (por URL).
    private val detectedVideos = LinkedHashMap<String, DetectedVideo>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val homeUrl = "https://www.google.com"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        fabVideos = findViewById(R.id.fabVideos)
        badgeCount = findViewById(R.id.badgeCount)

        setupWebView()
        setupToolbar()

        if (!RokuPrefs.isConfigured(this)) {
            Toast.makeText(
                this,
                "Configure o IP da Roku na engrenagem antes de transmitir.",
                Toast.LENGTH_LONG
            ).show()
        }

        webView.loadUrl(homeUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.userAgentString = settings.userAgentString +
            " CastRokuWeb/1.0"

        webView.addJavascriptInterface(
            VideoDetector { url, title -> addDetectedVideo(url, title) },
            VideoDetector.JS_INTERFACE_NAME
        )

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                if (customView != null) { callback?.onCustomViewHidden(); return }
                customView = view
                customViewCallback = callback
                (window.decorView as android.widget.FrameLayout).addView(view,
                    android.widget.FrameLayout.LayoutParams(-1, -1))
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onHideCustomView() {
                val view = customView ?: return
                (window.decorView as android.widget.FrameLayout).removeView(view)
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                progressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                // nada por enquanto; titulo e lido direto do webView quando precisa
            }
        }

        webView.webViewClient = object : WebViewClient() {

            // Tecnica principal de deteccao: olha TODA requisicao de rede que a
            // WebView faz (inclusive as de dentro de iframes/players embutidos
            // de outro dominio, que o JS da pagina principal nao consegue ver).
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()
                if (url != null && AdBlockPrefs.isEnabled(this@MainActivity) && AdBlocker.shouldBlock(url)) {
                    return AdBlocker.emptyResponse()
                }
                if (url != null && VideoDetector.looksLikeVideo(url)) {
                    mainHandler.post { addDetectedVideo(url, webView.title ?: "") }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null) urlBar.setText(url)
                detectedVideos.clear()
                updateBadge()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                // segunda tecnica de deteccao: varre o DOM da pagina principal
                webView.evaluateJavascript(VideoDetector.SCAN_SCRIPT, null)
                webView.evaluateJavascript(PlaybackResume.SCRIPT, null)
            }
        }

        swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btnForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btnReload).setOnClickListener {
            webView.reload()
        }
        findViewById<ImageButton>(R.id.btnRemote).setOnClickListener {
            if (!RokuPrefs.isConfigured(this)) {
                Toast.makeText(this, "Configure a TV Roku primeiro.", Toast.LENGTH_SHORT).show()
            } else {
                RokuRemoteDialog(this).show()
            }
        }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }

        urlBar.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                navigateTo(urlBar.text.toString().trim())
                true
            } else {
                false
            }
        }

        fabVideos.setOnClickListener {
            val dialog = DetectedVideosDialog(
                this,
                detectedVideos.values.toList(),
                webView.url ?: "",
                webView.title ?: ""
            )
            dialog.show()
        }
    }

    private fun navigateTo(input: String) {
        if (input.isBlank()) return
        val looksLikeUrl = input.contains(".") && !input.contains(" ")
        val target = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            looksLikeUrl -> "https://$input"
            else -> "https://www.google.com/search?q=" + android.net.Uri.encode(input)
        }
        webView.loadUrl(target)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
        urlBar.clearFocus()
    }

    private fun addDetectedVideo(url: String, title: String) {
        if (detectedVideos.containsKey(url)) return
        detectedVideos[url] = DetectedVideo(url, title, webView.url ?: "")
        updateBadge()
    }

    private fun updateBadge() {
        val count = detectedVideos.size
        if (count > 0) {
            badgeCount.text = count.toString()
            badgeCount.visibility = View.VISIBLE
        } else {
            badgeCount.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            (webView.webChromeClient as? WebChromeClient)?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
