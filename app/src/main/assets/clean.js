(function () {
  if (window.__clearShop) { window.__clearShop.sweep(); return; }

  // ---------- стилі ----------
  var css = [
    '*{-webkit-tap-highlight-color:transparent}',
    'body{padding-bottom:96px!important}',
    // банери "завантажте додаток" та подібне
    '[class*="download-app" i],[class*="downloadApp" i],[class*="open-app" i],[class*="openApp" i],',
    '[class*="smart-banner" i],[class*="app-banner" i],[class*="appBanner" i],[id*="download-app" i]',
    '{display:none!important}'
  ].join('');
  var st = document.createElement('style');
  st.id = 'clearshop-style';
  st.textContent = css;
  (document.head || document.documentElement).appendChild(st);

  // ---------- що вважаємо рекламним вікном ----------
  var PROMO = /(купон|coupon|колес|wheel|spin|крутн|прокрут|подарун|gift|отримати|claim|забрати|collect|знижк|% off|off\b|розпрод|лише сьогодні|only today|limited|обмежен|щаслив|lucky|бонус|bonus|кредит|credit|додаток|the app|download|завантаж|сповіщен|notification|підпис|subscribe|запроси|invite|free|безкоштов)/i;
  // такі вікна НЕ чіпаємо
  var SAFE = /(увійти|вхід|sign in|log in|login|парол|password|оплат|payment|checkout|оформ|адрес|address|в кошик|add to cart|розмір|size|колір|color|кількість|quantity|видалити|delete|підтвер|confirm)/i;

  var removed = 0;

  function hide(el) {
    el.style.setProperty('display', 'none', 'important');
    el.setAttribute('data-clearshop', 'hidden');
    removed++;
  }

  function isOverlay(el) {
    var s = getComputedStyle(el);
    if (s.position !== 'fixed') return false;
    if (s.display === 'none' || s.visibility === 'hidden') return false;
    var r = el.getBoundingClientRect();
    var big = r.width * r.height > innerWidth * innerHeight * 0.12;
    var z = parseInt(s.zIndex, 10) || 0;
    return big || z >= 999;
  }

  function candidates() {
    var set = new Set();
    var body = document.body;
    if (!body) return [];
    for (var i = 0; i < body.children.length; i++) set.add(body.children[i]);
    document.querySelectorAll(
      '[role="dialog"],[aria-modal="true"],[class*="modal" i],[class*="popup" i],' +
      '[class*="dialog" i],[class*="mask" i],[class*="overlay" i],[class*="layer" i]'
    ).forEach(function (e) { set.add(e); });
    return Array.from(set);
  }

  function sweep() {
    var hit = false, masks = [];
    candidates().forEach(function (el) {
      if (el.id === 'clearshop-style' || el.getAttribute('data-clearshop')) return;
      if (el.tagName === 'SCRIPT' || el.tagName === 'STYLE') return;
      if (!isOverlay(el)) return;
      var txt = (el.innerText || '').slice(0, 3000);
      if (el.querySelector('input[type=password],input[type=tel],input[type=email]')) return;
      if (!txt.trim()) { masks.push(el); return; }          // порожнє затемнення
      if (SAFE.test(txt)) return;
      if (PROMO.test(txt)) { hide(el); hit = true; }
    });
    // затемнення ховаємо, лише якщо щойно прибрали рекламне вікно
    if (hit) masks.forEach(hide);
    if (hit) unlockScroll();
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
  var mo = new MutationObserver(function () {
    clearTimeout(t);
    t = setTimeout(sweep, 120);
  });
  function start() {
    if (!document.body) return setTimeout(start, 50);
    mo.observe(document.body, { childList: true, subtree: true });
    sweep();
  }
  start();
  // підстраховка: перші 40 секунд перевіряємо частіше
  var n = 0, iv = setInterval(function () { sweep(); if (++n > 20) clearInterval(iv); }, 2000);

  // блокуємо window.open (нові вкладки з рекламою)
  window.open = function (u) { if (u) location.href = u; return null; };

  window.__clearShop = { sweep: sweep, count: function () { return removed; } };
})();
