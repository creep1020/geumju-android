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
    private static final int BACKUP_REQUEST = 43;
    private String pendingBackup;
    private void backupMessage(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }
    private static final int NOTIFICATION_REQUEST = 42;
    private final class Bridge {
        @JavascriptInterface public void saveBackup(String json) { runOnUiThread(() -> {
            if (web == null || web.getUrl() == null || !web.getUrl().startsWith(HOME)) return;
            if (pendingBackup != null) return;
            try {
                new org.json.JSONObject(json).getJSONObject("records");
                pendingBackup = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "오늘도금주_백업_" + new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA).format(new java.util.Date()) + ".json");
                startActivityForResult(intent, BACKUP_REQUEST);
            } catch (Exception ex) { pendingBackup = null; backupMessage("백업을 시작하지 못했어요."); }
        }); }
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
        if (savedInstanceState != null) pendingBackup = savedInstanceState.getString("pendingBackup");
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
                view.evaluateJavascript("(function(){var b=document.getElementById('exportBtn');if(!b||b.dataset.nativeBackup)return;b.dataset.nativeBackup='1';b.addEventListener('click',function(e){e.preventDefault();e.stopImmediatePropagation();try{var r=JSON.parse(localStorage.getItem('oneuldo_geumju_v1')||'{}');NativeReminder.saveBackup(JSON.stringify({app:'오늘도 금주',version:1,records:r},null,2));}catch(err){alert('백업 데이터를 읽지 못했어요.');}},true);})();", null);
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
        if (requestCode == BACKUP_REQUEST) {
            String json = pendingBackup;
            pendingBackup = null;
            if (resultCode != RESULT_OK) { backupMessage("백업 저장을 취소했어요."); return; }
            if (data == null || data.getData() == null || json == null) { backupMessage("백업 저장 실패. 다시 시도해 주세요."); return; }
            try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
                if (out == null) throw new java.io.IOException();
                out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ex) { backupMessage("백업 저장 실패. 다시 시도해 주세요."); return; }
            backupMessage("백업 파일 저장 완료!");
            return;
        }
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (pendingBackup != null) state.putString("pendingBackup", pendingBackup);
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
