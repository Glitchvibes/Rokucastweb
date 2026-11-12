package com.elieltv.castrokuweb

/**
 * Persiste a posição do elemento HTML5 video no próprio armazenamento do site.
 * A recuperação é feita após o player carregar; sites com players proprietários
 * podem não expor um elemento <video> acessível.
 */
object PlaybackResume {
    val SCRIPT = """
        (function() {
          if (window.__castRokuResumeInstalled) return;
          window.__castRokuResumeInstalled = true;
          function key(v) {
            var src = v.currentSrc || v.src || location.href;
            return 'castroku_resume_' + location.origin + location.pathname + '_' + src;
          }
          function save(v) {
            try {
              if (v.duration && isFinite(v.duration) && v.currentTime > 1) {
                localStorage.setItem(key(v), JSON.stringify({
                  time: v.currentTime, duration: v.duration, saved: Date.now()
                }));
              }
            } catch(e) {}
          }
          function attach(v) {
            if (v.__castRokuResume) return;
            v.__castRokuResume = true;
            var restored = false;
            v.addEventListener('loadedmetadata', function() {
              if (restored) return;
              restored = true;
              try {
                var raw = localStorage.getItem(key(v));
                if (!raw) return;
                var data = JSON.parse(raw);
                if (data.time > 5 && data.time < v.duration - 3 &&
                    Date.now() - data.saved < 30 * 24 * 60 * 60 * 1000) {
                  v.currentTime = data.time;
                }
              } catch(e) {}
            });
            v.addEventListener('timeupdate', function() {
              if (!v.paused) save(v);
            });
            v.addEventListener('pause', function() { save(v); });
            v.addEventListener('ended', function() {
              try { localStorage.removeItem(key(v)); } catch(e) {}
            });
          }
          function scan() {
            document.querySelectorAll('video').forEach(attach);
          }
          scan();
          new MutationObserver(scan).observe(document.documentElement, {
            childList: true, subtree: true
          });
          window.addEventListener('pagehide', function() {
            document.querySelectorAll('video').forEach(save);
          });
        })();
    """.trimIndent()
}
