package name.boyle.chris.sgtpuzzles

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_BACK
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewClientCompat
import name.boyle.chris.sgtpuzzles.NightModeHelper.ActivityWithNightMode
import name.boyle.chris.sgtpuzzles.databinding.ActivityHelpBinding
import java.io.IOException
import java.text.MessageFormat
import java.util.regex.Pattern

class HelpActivity : ActivityWithNightMode() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val topic = intent.getStringExtra(TOPIC) ?: "index"
        if (!ALLOWED_TOPICS.matcher(topic).matches()) {
            finish()
            return
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        webView = ActivityHelpBinding.inflate(layoutInflater).root
        setContentView(webView)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(w: WebView, title: String) {
                supportActionBar?.title = getString(R.string.title_activity_help) + ": " + title
            }

            // onReceivedTitle doesn't happen on back button :-(
            override fun onProgressChanged(w: WebView, progress: Int) {
                if (progress == 100) {
                    supportActionBar?.title =
                        getString(R.string.title_activity_help) + ": " + w.title
                }
            }
        }
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(ASSETS_PATH, AssetsPathHandler(this))
            .build()
        webView.webViewClient = object : WebViewClientCompat() {
            @RequiresApi(21)
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            @Deprecated("Deprecated in Java")  // remove when we reach minSdkVersion 21
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(Uri.parse(url))
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith(ASSETS_URL)) {
                    return false
                }
                // spawn other app
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                applyNightCSSClass()
            }
        }
        with (webView.settings) {
            javaScriptEnabled = true
            // Setting this off for security. Off by default for SDK versions >= 16.
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            // Off by default, deprecated for SDK versions >= 30.
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkImage = true
            builtInZoomControls = true
            blockNetworkLoads = true
            displayZoomControls = false
        }
        // locales[0] needs minSdkVersion 24
        @Suppress("DEPRECATION") val lang = resources.configuration.locale.language
        val haveLocalised = try {
             resources.assets.list(lang)?.contains("$topic.html") ?: false
        } catch (ignored: IOException) {
            false
        }
        webView.loadUrl(ASSETS_URL + helpPath(if (haveLocalised) lang else "en", topic))
    }

    private fun applyNightCSSClass() {
        webView.evaluateJavascript(
            if (NightModeHelper.isNight(resources.configuration)) {
                "document.body.className += ' night';"
            } else {
                "document.body.className = document.body.className.replace(/(?:^|\\s)night(?!\\S)/g, '');"
            },
            null
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        webView.setBackgroundColor(ContextCompat.getColor(this, R.color.webview_background))
        applyNightCSSClass()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == ACTION_DOWN && event.repeatCount == 0 && keyCode == KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TOPIC = "name.boyle.chris.sgtpuzzles.TOPIC"
        private val ALLOWED_TOPICS = Pattern.compile("^[a-z]+$")
        const val ASSETS_PATH = "/assets/"
        const val ASSETS_URL = "https://" + WebViewAssetLoader.DEFAULT_DOMAIN + ASSETS_PATH
        private fun helpPath(lang: String, topic: String?): String {
            return MessageFormat.format("{0}/{1}.html", lang, topic)
        }
    }
}