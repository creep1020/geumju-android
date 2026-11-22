package com.geumju.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String HOME = "https://creep1020.github.io/grumjoo/";
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 41;
    private static final int NOTIFICATION_REQUEST = 42;
    private final class Bridge {
        @JavascriptInterface public boolean isEnabled() { return ReminderReceiver.enabled(MainActivity.this); }
        @JavascriptInterface public void toggle() { runOnUiThread(() -> {
            boolean turnOn = !ReminderReceiver.enabled(MainActivity.this);
            getSharedPreferences(ReminderReceiver.PREFS, 0).edit().putBoolean("enabled", turnOn).apply();
            ReminderReceiver.schedule(MainActivity.this);
            if (turnOn && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
            refreshButton();
        }); }
    }
    private void refreshButton() { if (web != null) web.evaluateJavascript("window.refreshNativeReminder && window.refreshNativeReminder()", null); }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(), "NativeReminder");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri url = request.getUrl();
                if ("https".equals(url.getScheme()) && "creep1020.github.io".equals(url.getHost()) && url.getPath().startsWith("/grumjoo/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, url)); } catch (Exception ignored) {}
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (!url.startsWith(HOME)) return;
                String js = "(function(){" +
                  "if(window.nativeReminderInstalled)return;window.nativeReminderInstalled=true;" +
                  "var b=document.getElementById('pushToggle'),s=document.getElementById('pushStatus');if(!b||!s)return;" +
                  "window.refreshNativeReminder=function(){var on=NativeReminder.isEnabled();b.disabled=false;b.textContent=on?'저녁 7시 알림 끄기':'저녁 7시 알림 켜기';s.textContent=on?'APK 알림이 켜져 있어요. 매일 저녁 7시 전후에 휴대폰 알림이 도착합니다.':'APK 자체 알림을 사용할 수 있어요. 버튼을 눌러 켜 주세요.';};" +
                  "b.addEventListener('click',function(e){e.stopImmediatePropagation();e.preventDefault();NativeReminder.toggle();},true);" +
                  "new MutationObserver(function(){window.refreshNativeReminder();}).observe(b,{attributes:true,attributeFilter:['disabled']});" +
                  "window.refreshNativeReminder();setTimeout(window.refreshNativeReminder,1500);" +
                  "})();";
                view.evaluateJavascript(js, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_REQUEST); return true; }
                catch (Exception ex) { fileCallback = null; return false; }
            }
        });
        ReminderReceiver.schedule(this);
        web.loadUrl(HOME);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATION_REQUEST && (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED)) {
            getSharedPreferences(ReminderReceiver.PREFS, 0).edit().putBoolean("enabled", false).apply();
            ReminderReceiver.schedule(this);
        }
        refreshButton();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
