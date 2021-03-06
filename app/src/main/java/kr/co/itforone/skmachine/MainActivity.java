package kr.co.itforone.skmachine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import kr.co.itforone.skmachine.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding biding;

    public WebView webView;
    public SwipeRefreshLayout refreshLayout = null;
    public CookieManager cookieManager;

    private long backPrssedTime = 0;
    public int flg_refresh = 1;

    final int FILECHOOSER_NORMAL_REQ_CODE = 1200, FILECHOOSER_LOLLIPOP_REQ_CODE = 1300;
    public ValueCallback<Uri> filePathCallbackNormal;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public Uri mCapturedImageURI;
    public String loadUrl = "";
    private int vsocde = 0;

    public static String TOKEN = ""; // ????????????
    public ImageView imgGif;    // ?????????

    public SharedPreferences preferences;  // ????????????????????????
    public SharedPreferences.Editor pEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ??????????????????
        biding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        biding.setMainData(this);

        webView = biding.webview;
        refreshLayout = biding.refreshlayout;

        imgGif = biding.imgGif; // ???????????????
        Glide.with(this).asGif()    // GIF ??????
                .load( R.raw.loading_img )
                .override(200, 200)
                .diskCacheStrategy( DiskCacheStrategy.RESOURCE )    // Glide?????? ????????? ???????????? ????????? ???????????? ????????? ????????? ????????? ??????
                .into( imgGif );


        setTOKEN(this);

        // ???????????????
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        setCookieAllow(cookieManager, webView);

        // ????????????????????????
        preferences = getSharedPreferences("member", Activity.MODE_PRIVATE);
        if(preferences!=null) {
            pEditor = preferences.edit();
        }

        // ????????????
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            vsocde = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {

        }


        Intent splash = new Intent(kr.co.itforone.skmachine.MainActivity.this, SplashActivity.class);
        startActivity(splash);

        webView.addJavascriptInterface(new WebviewJavainterface(this), "Android");
        webView.setWebViewClient(new kr.co.itforone.skmachine.ClientManager(this));
        webView.setWebChromeClient(new kr.co.itforone.skmachine.ChoromeManager(this, this));
        webView.setWebContentsDebuggingEnabled(true); // ???????????????
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(settings.getUserAgentString() + "INAPP/APP_VER="+vsocde);
        settings.setTextZoom(100);
        settings.setJavaScriptEnabled(true);    // ??????????????????
        // ???????????????????????? ????????????
//        settings.setDomStorageEnabled(true);    //  ?????????????????? ??????
//        settings.setJavaScriptCanOpenWindowsAutomatically(true); // window.open()??????

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {    // ???????????? net::ERR_CACHE_MISS
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        settings.setAppCacheMaxSize(1024 * 1024 * 8); //8mb
        File dir = getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        settings.setAppCachePath(dir.getPath());
        settings.setAllowFileAccess(true);
        settings.setAppCacheEnabled(true);  // ???????????????????????????

//        webView = new WebView(this) {
//            @Override
//            public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//                outAttrs.privateImeOptions="defaultInputmode=korean";
//                return super.onCreateInputConnection(outAttrs);
//            }
//        };

        EditorInfo ei = new EditorInfo();
        ei.privateImeOptions = "defaultInputmode=korean";
        webView.onCreateInputConnection(ei);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.clearCache(true);
                webView.reload();
                refreshLayout.setRefreshing(false);
            }
        });

        refreshLayout.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (webView.getScrollY() == 0 && flg_refresh == 1) {
                    refreshLayout.setEnabled(true);
                } else {
                    refreshLayout.setEnabled(false);
                }
            }
        });


        // push&???????????? url??????
        loadUrl = getString(R.string.index);
        try {
            Intent intent = getIntent();

            if (intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uriData = intent.getData();
                Log.d("??????:uriData", uriData.toString());
                if (uriData != null) {
                    String idx = uriData.getQueryParameter("idx");
                    if (!idx.equals("")) {
                        loadUrl = uriData.getQueryParameter("url").toString() + "?idx=" + uriData.getQueryParameter("idx").toString();
                    }
                }
            } else if (!intent.getExtras().getString("goUrl").equals("")) {
                loadUrl = intent.getExtras().getString("goUrl");
            }

        } catch (Exception e) {
            Log.d("??????:uriData_exc", e.toString());
        }

        // ?????????????????? ??????..
        loadUrl += (loadUrl.contains("?"))? "&" : "?";
        loadUrl += "app_mb_id=" + preferences.getString("appLoginId", "");
        Log.d("??????:onCreate", loadUrl);

        webView.loadUrl(loadUrl);
        webView.clearCache(true);
    }




    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync();
        }
    }

    //?????????????????????
    @Override
    public void onBackPressed() {
        WebBackForwardList historyList = webView.copyBackForwardList();
        String currentUrl = webView.getUrl();

        if (currentUrl.equals(getString(R.string.index))) {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPrssedTime;

            if (0 <= intervalTime && 2000 >= intervalTime) {
                finish();
            } else {
                backPrssedTime = tempTime;
                Toast.makeText(getApplicationContext(), "?????? ??? ???????????? ????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (webView.canGoBack()) {
            String backTargetUrl = historyList.getItemAtIndex(historyList.getCurrentIndex() - 1).getUrl();
            Log.d("??????:currentUrl", currentUrl);
            Log.d("??????:backTargetUrl", backTargetUrl);

//            if ((currentUrl.contains("cart.php") && !backTargetUrl.contains("store_view")) || (currentUrl.contains("/bbs/order_result.php") && !backTargetUrl.contains("/bbs/myorder.php"))
//                    || currentUrl.contains("/bbs/order_done.php") || (currentUrl.contains("/bbs/order_list.php") && backTargetUrl.contains("/bbs/order_detail.php"))) {
//                webView.clearHistory();
//                webView.loadUrl(getString(R.string.index));
//            }

            if ( (currentUrl.contains("register_form.php") && backTargetUrl.contains("register_form.php")) ) {
                Log.d("??????:??????????????????", currentUrl);
                webView.clearHistory();
                webView.loadUrl(getString(R.string.index));
                return;
            }

            webView.goBack();

        } else {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPrssedTime;

            if (0 <= intervalTime && 2000 >= intervalTime) {
                finish();
            } else {
                backPrssedTime = tempTime;
                Toast.makeText(getApplicationContext(), "?????? ??? ???????????? ????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
                /*
                if (currentUrl.contains("store_view") || currentUrl.contains("/bbs/order_result.php") || currentUrl.contains("/zeropay/cancel.php")) {
                    // ???????????? ?????????????????? or ???????????? ??? ???????????? ????????? or ???????????????
                    webView.clearHistory();
                    webView.loadUrl(getString(R.string.index));

                } else if (currentUrl.contains("order_detail.php")) {
                    // ?????????????????? ?????????
                    webView.loadUrl(getString(R.string.domain) + "bbs/order_list.php");

                } else {
                    backPrssedTime = tempTime;
                    Toast.makeText(getApplicationContext(), "?????? ??? ???????????? ????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
                }
                 */
            }
        }
    }

    // ????????????????????? ??????
    public void setPageRefresh(boolean flag) {
        refreshLayout.setEnabled(flag);
    }

    public void setCookieAllow(CookieManager cookieManager, WebView webView) {
        try {
            cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            }
        } catch (Exception e) {
        }
    }
//    public void setCookieRegist(String key, String val) {
//        //cookieManager.setCookie();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == FILECHOOSER_NORMAL_REQ_CODE) {
                if (filePathCallbackNormal == null) return;
                Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                filePathCallbackNormal.onReceiveValue(result);
                filePathCallbackNormal = null;

            } else if (requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE) {
                Uri[] result = new Uri[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // ?????????/????????? ??????
                    if (resultCode == RESULT_OK) {
                        result = (data == null) ? new Uri[]{mCapturedImageURI} : WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                    }
                    filePathCallbackLollipop.onReceiveValue(result);
                    filePathCallbackLollipop = null;
                }
            }
        } else {
            try {
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                    //webView.loadUrl("javascript:removeInputFile()");
                }
            } catch (Exception e) {
            }
        }
    }

    // ??????..
    public static void setTOKEN(Activity mActivity){
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }
                // Get new Instance ID token
                TOKEN = task.getResult().getToken();
            }
        });
    }

    public class WebViewInput extends WebView {
        public WebViewInput(Context context) {
            super(context);
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            outAttrs.privateImeOptions="defaultInputmode=korean";
            return super.onCreateInputConnection(outAttrs);
        }
    }

}