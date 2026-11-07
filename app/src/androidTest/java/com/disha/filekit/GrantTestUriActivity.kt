package com.disha.filekit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class GrantTestUriActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = Uri.parse(intent.getStringExtra("uri") ?: "")
        if (uri.authority == "com.disha.filekit.test.documents") {
            grantUriPermission("com.disha.filekit", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        finish()
    }
}
