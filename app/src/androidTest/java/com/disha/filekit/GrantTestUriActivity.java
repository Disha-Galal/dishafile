package com.disha.filekit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

// المكوّن بيشتغل في عملية APK الاختبارات، لذلك لا يعتمد على مكتبات التطبيق.
public final class GrantTestUriActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String value = getIntent().getStringExtra("uri");
        Uri uri = Uri.parse(value == null ? "" : value);
        if ("com.disha.filekit.test.documents".equals(uri.getAuthority())) {
            grantUriPermission("com.disha.filekit", uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        finish();
    }
}
