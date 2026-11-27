(function () {
  var O = window.__csOpts || { popups: true, skin: true };
  if (window.__clearShop) { window.__clearShop.sweep(); return; }

  // ---------- стилі ----------
  var css = [
    '*{-webkit-tap-highlight-color:transparent}',
    '[class*="download-app" i],[class*="downloadApp" i],[class*="open-app" i],[class*="openApp" i],',
    '[class*="smart-banner" i],[class*="app-banner" i],[class*="appBanner" i],[id*="download-app" i]',
    '{display:none!important}'
  ];
  if (O.skin) css.push(
    // «Liquid Glass»-переоформлення сайту
    'html,body{background:#f7f4f9!important}',
    'body{font-family:system-ui,-apple-system,Roboto,"Segoe UI",sans-serif!important;color:#251f2b}',
    'img{border-radius:12px}',
    'input,textarea,select{border-radius:14px}',
    'button{border-radius:14px}',
    '::selection{background:rgba(255,106,43,.25)}',
    '::-webkit-scrollbar{width:0;height:0}',
    '[data-cs-glass]{background:rgba(255,255,255,.72)!important;-webkit-backdrop-filter:blur(20px) saturate(180%)!important;' +
      'backdrop-filter:blur(20px) saturate(180%)!important;border-bottom:1px solid rgba(255,255,255,.9)!important;' +
      'box-shadow:0 6px 24px rgba(120,60,140,.08)!important}',
    '[data-cs-glassbar]{background:rgba(255,255,255,.78)!important;-webkit-backdrop-filter:blur(20px) saturate(180%)!important;' +
      'backdrop-filter:blur(20px) saturate(180%)!important;border-top:1px solid rgba(255,255,255,.9)!important;' +
      'border-radius:22px 22px 0 0!important;box-shadow:0 -6px 24px rgba(120,60,140,.1)!important}'
  );
  var st = document.createElement('style');
  st.id = 'clearshop-style';
  st.textContent = css.join('');
  (document.head || document.documentElement).appendChild(st);

  var PROMO = /(купон|coupon|колес|wheel|spin|крутн|прокрут|подарун|gift|отримати|claim|забрати|collect|знижк|% off|розпрод|лише сьогодні|only today|limited|обмежен|щаслив|lucky|бонус|bonus|кредит|credit|додаток|the app|download|завантаж|сповіщен|notification|підпис|subscribe|запроси|invite|free|безкоштов)/i;
  var SAFE = /(увійти|вхід|sign in|log in|login|парол|password|оплат|payment|checkout|оформ|адрес|address|в кошик|add to cart|розмір|size|колір|color|кількість|quantity|видалити|delete|підтвер|confirm)/i;
  var NAV_A = /(головна|home)/i, NAV_B = /(кошик|cart)/i, NAV_C = /(ви\b|you|акаунт|account|профіль|категор|categor)/i;

  var removed = 0;
  function hide(el, count) {
    el.style.setProperty('display', 'none', 'important');
    el.setAttribute('data-clearshop', 'hidden');
    if (count) removed++;
  }

  function fixed(el) {
    var s = getComputedStyle(el);
    return (s.position === 'fixed' || s.position === 'sticky') && s.display !== 'none' && s.visibility !== 'hidden' ? s : null;
  }

  function candidates() {
    var set = new Set(), body = document.body;
    if (!body) return [];
    for (var i = 0; i < body.children.length; i++) set.add(body.children[i]);
    document.querySelectorAll(
      '[role="dialog"],[aria-modal="true"],[class*="modal" i],[class*="popup" i],[class*="dialog" i],' +
      '[class*="mask" i],[class*="overlay" i],[class*="layer" i],[class*="header" i],[class*="tab" i],[class*="nav" i],' +
      '[class*="footer" i],[class*="bottom" i],header,nav,footer'
    ).forEach(function (e) { set.add(e); });
    return Array.from(set);
  }

  function sweep() {
    var hit = false, masks = [];
    candidates().forEach(function (el) {
      if (el.id === 'clearshop-style' || el.getAttribute('data-clearshop')) return;
      if (el.tagName === 'SCRIPT' || el.tagName === 'STYLE') return;
      var s = fixed(el);
      if (!s) return;
      var r = el.getBoundingClientRect();
      var txt = (el.innerText || '').slice(0, 3000);

      // рідна нижня панель сайту — у нас своя
      if (O.skin && r.bottom >= innerHeight - 4 && r.height < 120 && r.width > innerWidth * 0.8 &&
          NAV_A.test(txt) && NAV_B.test(txt) && NAV_C.test(txt)) { hide(el, false); return; }

      // скляна шапка й нижні панелі (наприклад «Додати в кошик»)
      if (O.skin && s.position !== 'static' && r.height < 140 && r.width > innerWidth * 0.8) {
        if (r.top <= 2) el.setAttribute('data-cs-glass', '1');
        else if (r.bottom >= innerHeight - 4) el.setAttribute('data-cs-glassbar', '1');
      }

      if (!O.popups || s.position !== 'fixed') return;
      var big = r.width * r.height > innerWidth * innerHeight * 0.12;
      var z = parseInt(s.zIndex, 10) || 0;
      if (!big && z < 999) return;
      if (el.querySelector('input[type=password],input[type=tel],input[type=email]')) return;
      if (!txt.trim()) { if (big) masks.push(el); return; }
      if (SAFE.test(txt)) return;
      if (PROMO.test(txt)) { hide(el, true); hit = true; }
    });
    if (hit) { masks.forEach(function (m) { hide(m, false); }); unlockScroll(); }
  }

  function unlockScroll() {
    [document.documentElement, document.body].forEach(function (e) {
      if (!e) return;
      e.style.removeProperty('overflow');
      e.style.removeProperty('position');
      e.style.removeProperty('height');
      if (getComputedStyle(e).overflow === 'hidden') e.style.setProperty('overflow', 'auto', 'important');
    });
  }

  var t = null;
  var mo = new MutationObserver(function () { clearTimeout(t); t = setTimeout(sweep, 120); });
  function start() {
    if (!document.body) return setTimeout(start, 50);
    mo.observe(document.body, { childList: true, subtree: true });
    sweep();
  }
  start();
  var n = 0, iv = setInterval(function () { sweep(); if (++n > 20) clearInterval(iv); }, 2000);

  window.open = function (u) { if (u) location.href = u; return null; };
  window.__clearShop = { sweep: sweep, count: function () { return removed; } };
})();
