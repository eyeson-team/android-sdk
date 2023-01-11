package com.eyeson.android.ui.view.scanner

import android.app.Application
import android.net.UrlQuerySanitizer
import androidx.lifecycle.AndroidViewModel

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    fun extractGuestToken(url: String): String {
        val sanitizer = UrlQuerySanitizer().apply {
            allowUnregisteredParamaters = true
            parseUrl(url)
        }
        return sanitizer.getValue("guest") ?: url
    }

}