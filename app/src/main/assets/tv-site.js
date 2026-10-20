/* Reads public presentation only. Never reads input values, cookies, storage,
 * authentication tokens or media source URLs. */
(function () {
  'use strict';
  if (location.origin !== 'https://bingebang.st' || window.__projectorTV) return;
  function visible(el) {
    if (!el || !el.getClientRects().length || el.closest('[hidden],[inert],[aria-hidden="true"]')) return false;
    var s = getComputedStyle(el);
    return s.visibility !== 'hidden' && s.display !== 'none';
  }
  function challenge() {
    var h = document.querySelector('h1');
    return /Quick check/i.test(document.title) || !!(h && /Quick check before you watch/i.test(h.textContent));
  }
  function titleUrl(value) {
    try {
      var u = new URL(value, location.href);
      return u.origin === location.origin && !u.username && !u.password &&
        /^\/(movie|tv)\/watch\/[a-zA-Z0-9_-]+\/?$/.test(u.pathname) ? u.origin + u.pathname : null;
    } catch (_) { return null; }
  }
  function snapshot() {
    if (challenge()) return JSON.stringify({url:location.href,blocked:true,titles:[]});
    var found = [], seen = {};
    var links = document.querySelectorAll('a[href]');
    for (var i = 0; i < links.length && found.length < 160; i++) {
      var a = links[i], url = titleUrl(a.href);
      if (!url) continue;
      var img = a.querySelector('img');
      var title = (a.getAttribute('aria-label') || a.getAttribute('title') ||
        (img && img.alt) || a.innerText || '').trim().slice(0,150);
      if (!title) continue;
      var section = a.closest('section,[role="region"]');
      var heading = section && section.querySelector('h2,h3');
      var group = heading ? heading.textContent.trim().replace(/\s+/g,' ').slice(0,100) : '';
      var key = group + '|' + url;
      if (seen[key]) continue;
      seen[key] = true;
      var poster = img ? (img.currentSrc || img.getAttribute('data-src') || img.src || '') : '';
      try {
        var p = new URL(poster,location.href);
        if (!poster || p.protocol !== 'https:' || p.username || p.password ||
          (p.port && p.port !== '443') || !['image.tmdb.org','bingebang.st'].includes(p.hostname)) poster = '';
        else poster = p.href;
      } catch (_) { poster = ''; }
      found.push({title:title,url:url,poster:poster,group:group,
        kind:url.indexOf('/movie/watch/') >= 0 ? 'Film' : 'Serie'});
    }
    return JSON.stringify({url:location.href,blocked:false,titles:found});
  }
  function openSearch() {
    if (challenge()) return 'blocked';
    var query = 'input[placeholder*="Search"],input[placeholder*="search"]';
    var input = Array.from(document.querySelectorAll(query)).find(visible);
    if (!input) {
      var trigger = Array.from(document.querySelectorAll('button,[role="button"]')).find(function (e) {
        return visible(e) && /^search$/i.test((e.getAttribute('aria-label') || e.textContent || '').trim());
      });
      if (trigger) trigger.click();
      input = Array.from(document.querySelectorAll(query)).find(visible);
    }
    if (!input) return 'unavailable';
    input.focus(); return 'focused';
  }
  function playLink() {
    if (challenge()) return '';
    var a = Array.from(document.querySelectorAll('a[href]')).find(function (e) {
      try {
        var u = new URL(e.href);
        return visible(e) && u.origin === location.origin && !u.username && !u.password &&
          /^\/(movie|tv)\/play\//.test(u.pathname);
      } catch (_) { return false; }
    });
    return a ? a.href : '';
  }
  function controls() {
    return Array.from(document.querySelectorAll('a[href],button,input:not([type="hidden"]),select,textarea,[role="button"],[tabindex]'))
      .filter(function (e) {return visible(e) && !e.disabled && e.tabIndex >= 0;});
  }
  function focusContent() {
    if (challenge()) return false;
    var candidates = controls();
    var e = candidates.find(function (el) {return !el.closest('nav,header,[role="navigation"]');}) || candidates[0];
    if (!e) return false;
    e.focus(); e.scrollIntoView({block:'center',inline:'nearest'}); return true;
  }
  function installFocus() {
    if (challenge()) return;
    var s = document.createElement('style');
    s.textContent = 'a:focus,button:focus,input:focus,select:focus,textarea:focus,[role="button"]:focus,[tabindex]:focus{outline:4px solid #5be9f7!important;outline-offset:3px!important}';
    document.head.appendChild(s);
    document.addEventListener('keydown',function (event) {
      if (event.defaultPrevented || challenge() || document.fullscreenElement) return;
      if (!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(event.key)) return;
      var current = document.activeElement;
      if (current && (current.isContentEditable || /^(INPUT|TEXTAREA|SELECT|VIDEO|IFRAME)$/.test(current.tagName) ||
          current.closest('[role="slider"],.plyr,.jwplayer,.video-js'))) return;
      var list = controls();
      if (!current || list.indexOf(current) < 0) {if (focusContent()) event.preventDefault(); return;}
      var r = current.getBoundingClientRect(), horizontal = /Left|Right/.test(event.key);
      var sign = /Right|Down/.test(event.key) ? 1 : -1, best = null, score = Infinity;
      list.forEach(function (e) {
        if (e === current) return;
        var b = e.getBoundingClientRect();
        var dx = (b.left+b.right-r.left-r.right)/2, dy = (b.top+b.bottom-r.top-r.bottom)/2;
        var primary = (horizontal ? dx : dy)*sign, secondary = Math.abs(horizontal ? dy : dx);
        if (primary <= 2) return;
        var overlap = horizontal ? b.top < r.bottom && b.bottom > r.top : b.left < r.right && b.right > r.left;
        var value = primary + secondary*3 + (overlap ? 0 : 1000);
        if (value < score) {score=value;best=e;}
      });
      if (best) {best.focus();best.scrollIntoView({block:'nearest',inline:'nearest'});event.preventDefault();}
    });
  }
  window.__projectorTV = {snapshot:snapshot,openSearch:openSearch,playLink:playLink,focusContent:focusContent};
  installFocus();
})();
