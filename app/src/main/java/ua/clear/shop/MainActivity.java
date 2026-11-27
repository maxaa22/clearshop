package ua.clear.shop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    // ===== Налаштування (легко змінити) =====
    static final String HOME = "https://www.temu.com/ua/";
    static final String CART = "https://www.temu.com/shopping_cart.html";
    static final int ACCENT = 0xFFFF6A2B;
    static final int INK = 0xFF2B2530;

    // домени реклами/трекерів, які не вантажимо взагалі
    static final String[] BLOCK = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "googletagmanager.com", "google-analytics.com", "facebook.net",
            "connect.facebook.com", "analytics.tiktok.com", "bat.bing.com",
            "criteo.com", "criteo.net", "adsrvr.org", "taboola.com", "outbrain.com",
            "ct.pinterest.com", "snap.licdn.com", "sc-static.net", "scorecardresearch.com",
            "hotjar.com", "appsflyer.com", "onelink.me", "app.link", "adjust.com",
            "branch.io", "clarity.ms", "yandex.ru/metrika", "mc.yandex"
    };

    WebView web;
    ProgressBar bar;
    String cleanJs = "";
    final View[] tabs = new View[4];

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        cleanJs = readAsset("clean.js");

        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);

        // ---- WebView ----
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // прибираємо "; wv", щоб сайт не нав'язував "відкрийте в додатку"
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String sc = u.getScheme() == null ? "" : u.getScheme();
                if (sc.equals("http") || sc.equals("https")) return false;
                if (sc.equals("tel") || sc.equals("mailto")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                }
                // intent://, market://, deep-link у сторонній додаток — блокуємо
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                for (String b : BLOCK) {
                    if (url.contains(b)) {
                        return new WebResourceResponse("text/plain", "utf-8",
                                new ByteArrayInputStream(new byte[0]));
                    }
                }
                return null;
            }

            @Override
            public void onPageCommitVisible(WebView v, String url) { inject(); }

            @Override
            public void onPageFinished(WebView v, String url) {
                inject();
                markTab(url);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showOffline();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
                if (p > 30) inject();
            }
        });

        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        // ---- тонкий прогрес-бар зверху ----
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, dp(3));
        bp.gravity = Gravity.TOP;
        root.addView(bar, bp);

        // ---- Liquid Glass нижня панель ----
        root.addView(buildGlassBar(), glassParams());

        setContentView(root);

        if (state != null) web.restoreState(state);
        else web.loadUrl(HOME);
    }

    // ===================== Скляна панель =====================

    View buildGlassBar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));

        // шар 1: напівпрозоре «скло»
        GradientDrawable glass = new GradientDrawable();
        glass.setCornerRadius(dp(34));
        glass.setColor(0xC8FFFFFF);
        glass.setStroke(dp(1), 0x99FFFFFF);
        // шар 2: верхній відблиск
        GradientDrawable shine = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x88FFFFFF, 0x00FFFFFF});
        shine.setCornerRadius(dp(34));
        // шар 3: ледь помітний теплий відтінок знизу
        GradientDrawable tint = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0x14FF6A2B, 0x00FF6A2B});
        tint.setCornerRadius(dp(34));
        nav.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{glass, tint, shine}));
        nav.setElevation(dp(14));
        nav.setOutlineSpotShadowColor(0x55FF6A2B);
        nav.setOutlineAmbientShadowColor(0x33000000);

        tabs[0] = tab(nav, res("ic_home"), "Головна", v -> { web.loadUrl(HOME); select(0); });
        tabs[1] = tab(nav, res("ic_search"), "Пошук", v -> { focusSearch(); select(1); });
        tabs[2] = tab(nav, res("ic_cart"), "Кошик", v -> { web.loadUrl(CART); select(2); });
        tabs[3] = tab(nav, res("ic_refresh"), "Оновити", v -> { web.reload(); bounce(v); });
        select(0);
        return nav;
    }

    FrameLayout.LayoutParams glassParams() {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(68));
        p.gravity = Gravity.BOTTOM;
        p.setMargins(dp(16), 0, dp(16), dp(14));
        return p;
    }

    View tab(LinearLayout parent, int icon, String label, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(4), 0, dp(4));

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setTag("icon");
        t.addView(iv, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTag("label");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(2), 0, 0);
        t.addView(tv);

        t.setOnClickListener(v -> { bounce(v); click.onClick(v); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(t, lp);
        return t;
    }

    void select(int idx) {
        for (int i = 0; i < tabs.length; i++) {
            View t = tabs[i];
            boolean on = i == idx;
            ImageView iv = t.findViewWithTag("icon");
            TextView tv = t.findViewWithTag("label");
            int c = on ? ACCENT : INK;
            iv.setColorFilter(c);
            tv.setTextColor(on ? ACCENT : 0xFF6E6675);
            tv.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            if (on) {
                GradientDrawable pill = new GradientDrawable();
                pill.setCornerRadius(dp(26));
                pill.setColor(0x24FF6A2B);
                pill.setStroke(dp(1), 0x40FF6A2B);
                t.setBackground(pill);
            } else {
                t.setBackground(null);
            }
        }
    }

    void markTab(String url) {
        if (url == null) return;
        if (url.contains("shopping_cart")) select(2);
        else if (url.contains("search")) select(1);
        else if (url.replace("www.", "").matches("https://temu\\.com/(ua/?)?(\\?.*)?$")) select(0);
    }

    void bounce(View v) {
        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
    }

    // ===================== Логіка =====================

    void inject() {
        if (!cleanJs.isEmpty()) web.evaluateJavascript(cleanJs, null);
    }

    void focusSearch() {
        String js = "(function(){var i=document.querySelector('input[type=search],input[placeholder]');"
                + "if(i){i.scrollIntoView({block:'center'});i.focus();i.click();return 1}"
                + "var b=document.querySelector('[class*=search i],[aria-label*=earch i],[aria-label*=ошук i]');"
                + "if(b){b.click();return 1}return 0})()";
        web.evaluateJavascript(js, r -> {
            if (!"1".equals(r)) web.loadUrl(HOME);
        });
    }

    void showOffline() {
        String html = "<html><body style=\"margin:0;height:100vh;display:flex;align-items:center;"
                + "justify-content:center;font-family:sans-serif;background:linear-gradient(#F7F4F9,#FDEFE8)\">"
                + "<div style=\"text-align:center;padding:32px 28px;border-radius:28px;"
                + "background:rgba(255,255,255,.7);border:1px solid rgba(255,255,255,.9);"
                + "box-shadow:0 10px 40px rgba(255,106,43,.18)\">"
                + "<div style=\"font-size:44px\">📡</div><h3 style=\"color:#2B2530\">Немає з'єднання</h3>"
                + "<p style=\"color:#6E6675\">Перевір інтернет і спробуй ще раз</p>"
                + "<button onclick=\"location.href='" + HOME + "'\" style=\"border:0;border-radius:20px;"
                + "padding:12px 26px;background:#FF6A2B;color:#fff;font-size:16px\">Спробувати</button>"
                + "</div></body></html>";
        web.loadDataWithBaseURL(HOME, html, "text/html", "utf-8", null);
    }

    String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            byte[] b = new byte[in.available()];
            int n = in.read(b);
            return new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    int res(String name) {
        return getResources().getIdentifier(name, "drawable", getPackageName());
    }

    int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}
