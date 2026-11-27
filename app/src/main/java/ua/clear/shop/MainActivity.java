package ua.clear.shop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    // ===== Налаштування =====
    static final String SHOP = "https://www.temu.com/ua/";
    static final String CART = "https://www.temu.com/shopping_cart.html";
    static final String SEARCH = "https://www.temu.com/ua/search_result.html?search_key=";
    static final String HOME_PAGE = "file:///android_asset/home.html";
    static final int ACCENT = 0xFFFF6A2B;
    static final int INK = 0xFF2B2530;

    static final String[] BLOCK = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "googletagmanager.com", "google-analytics.com", "facebook.net",
            "connect.facebook.com", "analytics.tiktok.com", "bat.bing.com",
            "criteo.com", "criteo.net", "adsrvr.org", "taboola.com", "outbrain.com",
            "ct.pinterest.com", "snap.licdn.com", "sc-static.net", "scorecardresearch.com",
            "hotjar.com", "appsflyer.com", "onelink.me", "app.link", "adjust.com",
            "branch.io", "clarity.ms", "mc.yandex"
    };

    WebView home, shop;
    LinearLayout shopBox;
    ProgressBar bar;
    TextView title;
    ImageView heart;
    SharedPreferences prefs;
    String cleanJs = "";
    final View[] tabs = new View[5];
    final AtomicInteger blocked = new AtomicInteger();
    int popups = 0, pagePopups = 0;
    boolean shopShown = false;

    // ===================== Життєвий цикл =====================

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("clearshop", MODE_PRIVATE);
        blocked.set(prefs.getInt("statAds", 0));
        popups = prefs.getInt("statPopups", 0);
        cleanJs = readAsset("clean.js");

        FrameLayout root = new FrameLayout(this);
        root.setFitsSystemWindows(true);

        FrameLayout content = new FrameLayout(this);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -1);
        cp.bottomMargin = dp(84);
        root.addView(content, cp);

        // ---- власний головний екран ----
        home = new WebView(this);
        setupCommon(home);
        home.setBackgroundColor(Color.TRANSPARENT);
        home.addJavascriptInterface(new Bridge(), "Android");
        home.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                openShop(r.getUrl().toString());
                return true;
            }
        });
        home.setWebChromeClient(new WebChromeClient());
        content.addView(home, new FrameLayout.LayoutParams(-1, -1));

        // ---- магазин у скляній картці ----
        shopBox = new LinearLayout(this);
        shopBox.setOrientation(LinearLayout.VERTICAL);
        shopBox.setVisibility(View.GONE);
        content.addView(shopBox, new FrameLayout.LayoutParams(-1, -1));

        shopBox.addView(buildTopBar(), topParams());

        FrameLayout card = new FrameLayout(this);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(26));
        cardBg.setColor(0xFFFFFFFF);
        cardBg.setStroke(dp(1), 0xCCFFFFFF);
        card.setBackground(cardBg);
        card.setClipToOutline(true);
        card.setElevation(dp(6));
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, 0, 1f);
        cardP.setMargins(dp(8), dp(4), dp(8), dp(6));
        shopBox.addView(card, cardP);

        shop = new WebView(this);
        setupCommon(shop);
        WebSettings s = shop.getSettings();
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptThirdPartyCookies(shop, true);
        shop.setWebViewClient(new ShopClient());
        shop.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
                if (p > 30) inject();
                if (p >= 100) countPopups();
            }

            @Override
            public void onReceivedTitle(WebView v, String t) { setTitle(t); }
        });
        card.addView(shop, new FrameLayout.LayoutParams(-1, -1));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, dp(3));
        bp.gravity = Gravity.TOP;
        card.addView(bar, bp);

        // ---- нижня склопанель ----
        root.addView(buildBottomBar(), bottomParams());

        setContentView(root);
        CookieManager.getInstance().setAcceptCookie(true);
        home.loadUrl(HOME_PAGE);
        if (state != null && state.getBoolean("shop", false)) {
            shop.restoreState(state);
            showShop(true);
        }
    }

    void setupCommon(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    // ===================== Клієнт магазину =====================

    class ShopClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            Uri u = r.getUrl();
            String sc = u.getScheme() == null ? "" : u.getScheme();
            if (sc.equals("http") || sc.equals("https")) return false;
            if (sc.equals("tel") || sc.equals("mailto")) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
            }
            return true; // intent://, market://, deep links — блокуємо
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            if (!opt("ads")) return null;
            String url = r.getUrl().toString();
            for (String b : BLOCK) {
                if (url.contains(b)) {
                    blocked.incrementAndGet();
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
            }
            return null;
        }

        @Override
        public void onPageStarted(WebView v, String url, android.graphics.Bitmap f) {
            pagePopups = 0;
            updateHeart(url);
        }

        @Override
        public void onPageCommitVisible(WebView v, String url) { inject(); }

        @Override
        public void onPageFinished(WebView v, String url) {
            inject();
            setTitle(v.getTitle());
            updateHeart(url);
            if (isProduct(url)) addHistory(url, cleanTitle(v.getTitle()));
            if (url != null && url.contains("shopping_cart")) selectTab(2);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
            if (r.isForMainFrame()) showOffline();
        }
    }

    void inject() {
        String o = "window.__csOpts={popups:" + opt("popups") + ",skin:" + opt("skin") + "};";
        shop.evaluateJavascript(o + cleanJs, null);
    }

    void countPopups() {
        shop.evaluateJavascript("window.__clearShop?window.__clearShop.count():0", r -> {
            try {
                int n = Integer.parseInt(r.trim());
                if (n > pagePopups) { popups += n - pagePopups; pagePopups = n; }
            } catch (Exception ignored) {}
        });
    }

    // ===================== Навігація =====================

    void openShop(String url) {
        showShop(true);
        shop.loadUrl(url);
    }

    void showShop(boolean on) {
        shopShown = on;
        shopBox.setVisibility(on ? View.VISIBLE : View.GONE);
        home.setVisibility(on ? View.GONE : View.VISIBLE);
        if (on) {
            shopBox.setAlpha(0f);
            shopBox.setTranslationY(dp(24));
            shopBox.animate().alpha(1f).translationY(0).setDuration(260).start();
        } else {
            saveStats();
            home.evaluateJavascript("window.refresh&&refresh()", null);
        }
    }

    void goRoute(String r) {
        showShop(false);
        home.evaluateJavascript("window.route&&route('" + r + "')", null);
    }

    void onTab(int i) {
        switch (i) {
            case 0: goRoute("home"); break;
            case 1: goRoute("catalog"); break;
            case 2: openShop(CART); break;
            case 3: goRoute("favs"); break;
            case 4: goRoute("settings"); break;
        }
        selectTab(i);
    }

    @Override
    public void onBackPressed() {
        if (shopShown) {
            if (shop.canGoBack()) shop.goBack();
            else { showShop(false); selectTab(0); }
            return;
        }
        home.evaluateJavascript("window.__route||'home'", r -> {
            if (r != null && !r.contains("home")) { goRoute("home"); selectTab(0); }
            else finish();
        });
    }

    // ===================== Верхня панель =====================

    View buildTopBar() {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.HORIZONTAL);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(6), 0, dp(6), 0);
        t.setBackground(glass(dp(24)));
        t.setElevation(dp(8));

        t.addView(iconBtn("ic_back", v -> onBackPressed()), new LinearLayout.LayoutParams(dp(44), dp(44)));

        title = new TextView(this);
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(dp(6), 0, dp(6), 0);
        t.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        t.addView(iconBtn("ic_search", v -> { goRoute("home"); selectTab(0);
            home.evaluateJavascript("window.focusSearch&&focusSearch()", null); }),
                new LinearLayout.LayoutParams(dp(44), dp(44)));
        t.addView(iconBtn("ic_refresh", v -> shop.reload()), new LinearLayout.LayoutParams(dp(44), dp(44)));
        heart = iconBtn("ic_heart", v -> toggleFav());
        t.addView(heart, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return t;
    }

    LinearLayout.LayoutParams topParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(dp(10), dp(6), dp(10), dp(4));
        return p;
    }

    ImageView iconBtn(String icon, View.OnClickListener l) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res(icon));
        iv.setColorFilter(INK);
        iv.setPadding(dp(10), dp(10), dp(10), dp(10));
        iv.setOnClickListener(v -> { bounce(v); l.onClick(v); });
        return iv;
    }

    void setTitle(String t) {
        if (title != null) title.setText(cleanTitle(t));
    }

    // ===================== Нижня панель =====================

    View buildBottomBar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(6), dp(6), dp(6));
        nav.setBackground(glass(dp(34)));
        nav.setElevation(dp(14));
        nav.setOutlineSpotShadowColor(0x55FF6A2B);

        String[][] items = {
                {"ic_home", "Головна"}, {"ic_grid", "Каталог"}, {"ic_cart", "Кошик"},
                {"ic_heart", "Обране"}, {"ic_tune", "Ще"}
        };
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            tabs[i] = tab(nav, res(items[i][0]), items[i][1], v -> onTab(idx));
        }
        selectTab(0);
        return nav;
    }

    FrameLayout.LayoutParams bottomParams() {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(68));
        p.gravity = Gravity.BOTTOM;
        p.setMargins(dp(14), 0, dp(14), dp(10));
        return p;
    }

    Drawable glass(int radius) {
        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(radius);
        base.setColor(0xD9FFFFFF);
        base.setStroke(dp(1), 0xB3FFFFFF);
        GradientDrawable tint = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0x1AFF6A2B, 0x00FF6A2B});
        tint.setCornerRadius(radius);
        GradientDrawable shine = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x99FFFFFF, 0x00FFFFFF});
        shine.setCornerRadius(radius);
        return new LayerDrawable(new Drawable[]{base, tint, shine});
    }

    View tab(LinearLayout parent, int icon, String label, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setTag("icon");
        t.addView(iv, new LinearLayout.LayoutParams(dp(23), dp(23)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTag("label");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(2), 0, 0);
        t.addView(tv);

        t.setOnClickListener(v -> { bounce(v); click.onClick(v); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(t, lp);
        return t;
    }

    void selectTab(int idx) {
        for (int i = 0; i < tabs.length; i++) {
            View t = tabs[i];
            if (t == null) continue;
            boolean on = i == idx;
            ImageView iv = t.findViewWithTag("icon");
            TextView tv = t.findViewWithTag("label");
            iv.setColorFilter(on ? ACCENT : INK);
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

    void bounce(View v) {
        v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(90).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(170).start()).start();
    }

    // ===================== Обране й історія =====================

    boolean isProduct(String url) {
        return url != null && (url.matches(".*-g-\\d+.*") || url.contains("goods.html") || url.contains("goods_id="));
    }

    String cleanTitle(String t) {
        if (t == null) return "";
        t = t.replaceAll("(?i)\\s*[-|–]\\s*temu.*$", "").trim();
        return t.isEmpty() ? "Магазин" : t;
    }

    JSONArray list(String key) {
        try { return new JSONArray(prefs.getString(key, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    void putList(String key, JSONArray a) { prefs.edit().putString(key, a.toString()).apply(); }

    int indexOf(JSONArray a, String url) {
        for (int i = 0; i < a.length(); i++) if (url.equals(a.optJSONObject(i).optString("u"))) return i;
        return -1;
    }

    JSONArray without(JSONArray a, String url) {
        JSONArray b = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (!url.equals(o.optString("u"))) b.put(o);
        }
        return b;
    }

    void addHistory(String url, String t) {
        try {
            JSONArray old = without(list("history"), url);
            JSONArray a = new JSONArray();
            a.put(new JSONObject().put("u", url).put("t", t).put("d", today()));
            for (int i = 0; i < old.length() && i < 49; i++) a.put(old.get(i));
            putList("history", a);
        } catch (Exception ignored) {}
    }

    void toggleFav() {
        String url = shop.getUrl();
        if (url == null || url.startsWith("data:")) return;
        JSONArray a = list("favs");
        try {
            if (indexOf(a, url) >= 0) a = without(a, url);
            else {
                JSONArray b = new JSONArray();
                b.put(new JSONObject().put("u", url).put("t", cleanTitle(shop.getTitle())).put("d", "Збережено " + today()));
                for (int i = 0; i < a.length(); i++) b.put(a.get(i));
                a = b;
            }
        } catch (Exception ignored) {}
        putList("favs", a);
        updateHeart(url);
    }

    void updateHeart(String url) {
        if (heart == null) return;
        boolean fav = url != null && indexOf(list("favs"), url) >= 0;
        heart.setImageResource(res(fav ? "ic_heart_fill" : "ic_heart"));
        heart.setColorFilter(fav ? 0xFFFF3D77 : INK);
    }

    String today() {
        return new java.text.SimpleDateFormat("d.MM HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    // ===================== Міст для головного екрана =====================

    class Bridge {
        @JavascriptInterface public void search(String q) {
            runOnUiThread(() -> { openShop(SEARCH + Uri.encode(q)); selectTab(-1); });
        }
        @JavascriptInterface public void openUrl(String u) {
            if (u == null || !u.startsWith("https://")) return;
            runOnUiThread(() -> { openShop(u); selectTab(-1); });
        }
        @JavascriptInterface public void cart() { runOnUiThread(() -> onTab(2)); }
        @JavascriptInterface public void home() { runOnUiThread(() -> { openShop(SHOP); selectTab(-1); }); }
        @JavascriptInterface public String favs() { return list("favs").toString(); }
        @JavascriptInterface public String history() { return list("history").toString(); }
        @JavascriptInterface public void removeFav(String u) { putList("favs", without(list("favs"), u)); }
        @JavascriptInterface public void removeHistory(String u) { putList("history", without(list("history"), u)); }
        @JavascriptInterface public void clearHistory() { putList("history", new JSONArray()); }
        @JavascriptInterface public void logout() {
            runOnUiThread(() -> {
                CookieManager.getInstance().removeAllCookies(null);
                shop.clearCache(true);
                shop.loadUrl("about:blank");
            });
        }
        @JavascriptInterface public String stats() {
            return "{\"ads\":" + blocked.get() + ",\"popups\":" + popups + "}";
        }
        @JavascriptInterface public boolean getOpt(String k) { return opt(k); }
        @JavascriptInterface public void setOpt(String k, boolean v) { prefs.edit().putBoolean("opt_" + k, v).apply(); }
        @JavascriptInterface public void tab(int i) { runOnUiThread(() -> selectTab(i)); }
    }

    boolean opt(String k) { return prefs.getBoolean("opt_" + k, true); }

    // ===================== Різне =====================

    void showOffline() {
        String html = "<html><body style=\"margin:0;height:100vh;display:flex;align-items:center;"
                + "justify-content:center;font-family:sans-serif;background:linear-gradient(#F7F4F9,#FDEFE8)\">"
                + "<div style=\"text-align:center;padding:32px 28px;border-radius:28px;margin:20px;"
                + "background:rgba(255,255,255,.7);border:1px solid #fff;box-shadow:0 10px 40px rgba(255,106,43,.18)\">"
                + "<div style=\"font-size:44px\">📡</div><h3 style=\"color:#2B2530\">Немає з'єднання</h3>"
                + "<p style=\"color:#6E6675\">Перевір інтернет і спробуй ще раз</p>"
                + "<button onclick=\"history.back()\" style=\"border:0;border-radius:20px;padding:12px 26px;"
                + "background:linear-gradient(135deg,#FF6A2B,#FF3D77);color:#fff;font-size:16px\">Спробувати</button>"
                + "</div></body></html>";
        shop.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    String readAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    int res(String name) { return getResources().getIdentifier(name, "drawable", getPackageName()); }

    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    void saveStats() {
        prefs.edit().putInt("statAds", blocked.get()).putInt("statPopups", popups).apply();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("shop", shopShown);
        if (shopShown) shop.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveStats();
        CookieManager.getInstance().flush();
    }
}
