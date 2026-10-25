/* ============================================================================
 * markdown.js — a tiny, SAFE, zero-dependency Markdown renderer (~4 KB).
 * No regex exponential backtracking, no innerHTML of raw user HTML:
 * everything is escaped first, then reconstructed into known-safe markup.
 *
 * Supports: ATX headings, paragraphs, bold/italic/strikethrough/inline code,
 * links & autolinks, images (http/https only), blockquotes, unordered /
 * ordered / task lists (nested), fenced code blocks, horizontal rules,
 * line breaks (two trailing spaces or single-newline inside paragraph).
 * ==========================================================================*/
(function (global) {
  'use strict';

  var ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  function esc(s) { return s.replace(/[&<>"']/g, function (c) { return ESC[c]; }); }

  /* Only allow benign URL schemes. */
  function safeUrl(u) {
    var t = String(u).trim();
    if (/^(?:java|vb)script\s*:/i.test(t.replace(/[\s\x00-\x1f&#;]+/g, '')) && !/^https?\s*:/i.test(t)) return '#';
    if (/^data:(?!image\/(png|jpe?g|gif|webp))/i.test(t)) return '#';
    return esc(t);
  }

  /* ---- inline pass: operates on already-escaped text ------------------- */
  function inline(text) {
    var out = '';
    var i = 0;
    var n = text.length;
    while (i < n) {
      var c = text.charAt(i);

      /* escaped char */
      if (c === '\\' && i + 1 < n && '\\`*_{}[]()#+-.!~|<>'.indexOf(text.charAt(i + 1)) !== -1) {
        out += text.charAt(i + 1); i += 2; continue;
      }
      /* inline code `…` */
      if (c === '`') {
        var end = text.indexOf('`', i + 1);
        if (end !== -1) {
          out += '<code>' + text.slice(i + 1, end) + '</code>';
          i = end + 1; continue;
        }
      }
      /* image ![alt](url) */
      if (c === '!' && text.charAt(i + 1) === '[') {
        var m = /^\!\[([^\]]*)\]\(([^()\s]*(?:\([^()]*\)[^()\s]*)*)(?:\s+&quot;([^&]*)&quot;)?\)/.exec(text.slice(i));
        if (m) {
          out += '<img src="' + safeUrl(m[2]) + '" alt="' + m[1] + '"' +
                 (m[3] ? ' title="' + m[3] + '"' : '') + ' loading="lazy">';
          i += m[0].length; continue;
        }
      }
      /* link [text](url) */
      if (c === '[') {
        var ml = /^\[([^\]]*)\]\(([^()\s]*(?:\([^()]*\)[^()\s]*)*)(?:\s+&quot;([^&]*)&quot;)?\)/.exec(text.slice(i));
        if (ml) {
          out += '<a href="' + safeUrl(ml[2]) + '"' + (ml[3] ? ' title="' + ml[3] + '"' : '') +
                 ' target="_blank" rel="noopener noreferrer">' + emStrong(ml[1]) + '</a>';
          i += ml[0].length; continue;
        }
      }
      /* autolink <https://…> (already escaped to &lt;…&gt;) */
      if (text.substr(i, 5) === '&lt;h' || text.substr(i, 5) === '&lt;w') {
        var ma = /^&lt;((?:https?|ftp):\/\/[^\s&]+(?:&(?:amp;|#39;|#quot;)?[^\s&]*)*)&gt;/
          .exec(text.slice(i));
        if (ma) {
          out += '<a href="' + safeUrl(ma[1]) + '" target="_blank" rel="noopener noreferrer">' + ma[1] + '</a>';
          i += ma[0].length; continue;
        }
      }
      /* emphasis / strikethrough */
      if (c === '*' || c === '_' || c === '~') {
        var run = 1;
        while (i + run < n && text.charAt(i + run) === c) run++;

        if (c === '~') {                                   /* ~~strike~~ */
          var closeS = findClose(text, i + run, '~', run);
          if (closeS !== -1) {
            out += '<del>' + emStrong(text.slice(i + run, closeS)) + '</del>';
            i = closeS + run; continue;
          }
          out += text.substr(i, run); i += run; continue;
        }

        if (run >= 3) {
          var close3 = findClose(text, i + 3, c, 3);
          if (close3 !== -1) {
            out += '<strong><em>' + emStrong(text.slice(i + 3, close3)) + '</em></strong>';
            i = close3 + 3; continue;
          }
        }
        if (run >= 2) {
          var close2 = findClose(text, i + 2, c, 2);
          if (close2 !== -1) {
            out += '<strong>' + emStrong(text.slice(i + 2, close2)) + '</strong>';
            i = close2 + 2; continue;
          }
        }
        var close1 = findClose(text, i + 1, c, 1);
        if (close1 !== -1 && (c !== '_' || !isWordChar(text.charAt(i - 1)))) {
          out += '<em>' + emStrong(text.slice(i + 1, close1)) + '</em>';
          i = close1 + 1; continue;
        }
        out += text.substr(i, run); i += run; continue;
      }
      out += c; i++;
    }
    return out;

    function isWordChar(ch) { return /[A-Za-z0-9]/.test(ch); }
  }

  /* find closing delimiter run of exactly `len` chars, not inside code spans */
  function findClose(text, from, ch, len) {
    var idx = from;
    while (true) {
      idx = text.indexOf(repeat(ch, len), idx);
      if (idx === -1) return -1;
      /* skip if it's part of a longer run boundary mismatch */
      var before = idx > 0 ? text.charAt(idx - 1) : '';
      var after = text.charAt(idx + len) || '';
      if (before === ch || after === ch) { idx += 1; continue; }
      /* don't match across an unbalanced backtick (code span) */
      var between = text.slice(from, idx);
      if ((between.split('`').length - 1) % 2 === 1) { idx += 1; continue; }
      return idx;
    }
  }
  function repeat(s, k) { var r = ''; for (var j = 0; j < k; j++) r += s; return r; }

  /* second-level pass for content inside links (avoid nested anchors) */
  function emStrong(t) { return inline(t); }

  /* ---- block pass ------------------------------------------------------- */
  function render(src) {
    var lines = String(src).replace(/\r\n?/g, '\n').split('\n');
    var html = [];
    var i = 0;

    while (i < lines.length) {
      var line = lines[i];

      /* blank */
      if (!line.trim()) { i++; continue; }

      /* fenced code ``` or ~~~ */
      var fence = /^( {0,3})(`{3,}|~{3,})\s*([\w+-]*)\s*$/.exec(line);
      if (fence) {
        var mark = fence[2].charAt(0), mlen = fence[2].length, lang = fence[3];
        var buf = [];
        i++;
        while (i < lines.length) {
          var cf = new RegExp('^\\s{' + fence[1].length + ',}?' + repeat(mark === '`' ? '`' : '~', mlen) + '+\\s*$').test(lines[i]);
          if (cf && lines[i].trim().charAt(0) === mark) break;
          buf.push(esc(lines[i])); i++;
        }
        i++; /* skip closing fence (or EOF) */
        html.push('<pre' + (lang ? ' data-lang="' + esc(lang) + '"' : '') + '><code>' +
                  buf.join('\n') + '</code></pre>');
        continue;
      }

      /* heading */
      var h = /^ {0,3}(#{1,6})\s+(.*?)\s*#*\s*$/.exec(line);
      if (h) {
        var lvl = h[1].length;
        html.push('<h' + lvl + '>' + inline(esc(h[2])) + '</h' + lvl + '>');
        i++; continue;
      }

      /* hr */
      if (/^ {0,3}((\* *){3,}|(- *){3,}|(_ *){3,})$/.test(line)) {
        html.push('<hr>'); i++; continue;
      }

      /* blockquote */
      if (/^ {0,3}>/.test(line)) {
        var qb = [];
        while (i < lines.length && (/^ {0,3}>/.test(lines[i]) || (lines[i].trim() && qb.length && !/^ {0,3}(#{1,6}\s|\|)/.test(lines[i])))) {
          qb.push(lines[i].replace(/^ {0,3}>\s?/, ''));
          i++;
        }
        html.push('<blockquote>' + render(qb.join('\n')) + '</blockquote>');
        continue;
      }

      /* table | a | b | */
      if (line.indexOf('|') !== -1 && i + 1 < lines.length &&
          /^ *\|?[ :\-|]+\|[ :\-|]*$/.test(lines[i + 1]) && /-/.test(lines[i + 1])) {
        var rows = [];
        while (i < lines.length && lines[i].indexOf('|') !== -1 && lines[i].trim()) { rows.push(lines[i]); i++; }
        html.push(table(rows));
        continue;
      }

      /* list (ul / ol / task) */
      var lm = /^(\s*)([-+*]|\d+[.)])\s+/.exec(line);
      if (lm) {
        var res = parseList(lines, i);
        html.push(res.html); i = res.next; continue;
      }

      /* paragraph */
      var pb = [];
      while (i < lines.length && lines[i].trim() &&
             !/^( {0,3}(#{1,6}\s|>|```|~~~|([-+*]|\d+[.)])\s))/ .test(lines[i]) &&
             !/^ {0,3}((\* *){3,}|(- *){3,}|(_ *){3,})$/.test(lines[i])) {
        pb.push(lines[i]); i++;
      }
      html.push('<p>' + inline(esc(pb.join('\n'))).replace(/( {2,}|\\)\n/g, '<br>') + '</p>');
    }
    return html.join('\n');
  }

  /* ---- lists ------------------------------------------------------------ */
  function parseList(lines, start) {
    var i = start;
    var first = /^(\s*)([-+*]|\d+[.)])\s+(.*)$/.exec(lines[i]);
    var baseIndent = first[1].length;
    var ordered = /\d/.test(first[2]);
    var items = [];   /* {text, children(html|null)} */

    while (i < lines.length) {
      var line = lines[i];
      if (!line.trim()) {
        /* blank line: list continues if next non-blank is still in list */
        var j = i + 1;
        while (j < lines.length && !lines[j].trim()) j++;
        if (j < lines.length && (isItem(lines[j]) || isCont(lines[j]))) { i = j; continue; }
        break;
      }
      var m = /^(\s*)([-+*]|\d+[.)])\s+(.*)$/.exec(line);
      if (m && m[1].length >= baseIndent && m[1].length < baseIndent + 4) {
        items.push({ text: m[3], children: null });
        i++;
        /* gather continuation / nested lines */
        var cont = [];
        while (i < lines.length) {
          var l2 = lines[i];
          if (!l2.trim()) break;
          var m2 = /^(\s*)([-+*]|\d+[.)])\s+/.exec(l2);
          if (m2 && m2[1].length >= baseIndent + 2) { /* nested list */
            var sub = parseList(lines, i);
            if (items.length) items[items.length - 1].children = sub.html;
            i = sub.next;
            /* any further same-level nested items were consumed by parseList */
            continue;
          }
          if (m2) break;               /* sibling item */
          if (/^ {0,3}>|^ {0,3}```|^ {0,3}#{1,6}\s/.test(l2)) break;
          cont.push(l2.replace(/^\s{1,}/, '')); /* lazy continuation */
          i++;
        }
        if (cont.length && items.length) {
          var last = items[items.length - 1];
          last.text += (last.text ? '\n' : '') + cont.join('\n');
        }
        continue;
      }
      if (!m && line.length > baseIndent + 1 && items.length) { /* loose para under list */
        i++; continue;
      }
      break;
    }

    var tag = ordered ? 'ol' : 'ul';
    var out = '<' + tag + ' class="md-list">';
    for (var k = 0; k < items.length; k++) {
      var it = items[k];
      var task = /^\[([ xX])\]\s+([\s\S]*)$/.exec(it.text);
      if (task) {
        var done = task[1].toLowerCase() === 'x';
        out += '<li class="task"><span class="cb' + (done ? ' on' : '') + '" aria-hidden="true">' +
               (done ? '✓' : '') + '</span>' + inline(esc(task[2])) + '</li>';
      } else {
        out += '<li>' + inline(esc(it.text)).replace(/\n/g, '<br>') + '</li>';
      }
      if (it.children) out += it.children;
    }
    out += '</' + tag + '>';
    return { html: out, next: i };

    function isItem(s) { var mm = /^(\s*)([-+*]|\d+[.)])\s+/.exec(s); return !!mm && mm[1].length >= baseIndent && mm[1].length < baseIndent + 4; }
    function isCont(s) { return s.length > baseIndent + 1 && !/^( {0,3}#{1,6}\s| {0,3}>)/.test(s); }
  }

  /* ---- tables ----------------------------------------------------------- */
  function table(rows) {
    function cells(r) {
      return r.replace(/^\s*\|/, '').replace(/\|\s*$/, '').split(/(?<!\\)\|/)
              .map(function (c) { return c.trim(); });
    }
    var head = cells(rows[0]);
    var aligns = cells(rows[1]).map(function (a) {
      if (/^:-+:$/.test(a)) return 'center';
      if (/^-+:$/.test(a)) return 'right';
      if (/^:-+$/.test(a)) return 'left';
      return '';
    });
    var st = function (idx) { return aligns[idx] ? ' style="text-align:' + aligns[idx] + '"' : ''; };
    var out = '<table><thead><tr>';
    for (var c = 0; c < head.length; c++) out += '<th' + st(c) + '>' + inline(esc(head[c])) + '</th>';
    out += '</tr></thead><tbody>';
    for (var r = 2; r < rows.length; r++) {
      var cs = cells(rows[r]);
      out += '<tr>';
      for (var k = 0; k < head.length; k++) out += '<td' + st(k) + '>' + inline(esc(cs[k] || '')) + '</td>';
      out += '</tr>';
    }
    return out + '</tbody></table>';
  }

  global.MD = { render: render, escapeHtml: esc };
})(window);
