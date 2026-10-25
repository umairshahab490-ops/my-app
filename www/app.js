/* ============================================================================
 * app.js — OneNote Lite: notebooks → sections → pages, markdown editing,
 * instant search, localStorage persistence, keyboard-first navigation.
 * Zero dependencies, tiny memory footprint, no timers running when idle.
 * ==========================================================================*/
(function () {
  'use strict';

  /* ---------------------------- constants ------------------------------ */
  var LS_DATA = 'onenote-lite:data:v1';
  var LS_UI   = 'onenote-lite:ui:v1';
  var SECTION_COLORS = ['#7712AA', '#038387', '#107C10', '#CA5010', '#0078D4',
                        '#DA3B01', '#8764B8', '#E3008C'];
  var PALETTE = [
    { name: 'Welcome to OneNote Lite', color: '#7712AA', open: true,
      sections: ['Getting Started', 'Study Notes'] },
    { name: 'Work',       color: '#0078D4', open: false, sections: ['Meetings', 'Projects'] },
    { name: 'Personal',   color: '#107C10', open: false, sections: ['Ideas', 'Journal'] },
    { name: 'Reference',  color: '#CA5010', open: false, sections: ['Snippets'] },
    { name: 'Archive',    color: '#605E5C', open: false, sections: [] }
  ];

  /* ---------------------------- helpers -------------------------------- */
  function $(id) { return document.getElementById(id); }
  function uid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 8); }
  function clamp(v, a, b) { return v < a ? a : (v > b ? b : v); }

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function timeAgo(ts) {
    var d = Math.floor((Date.now() - ts) / 1000);
    if (d < 45) return 'just now';
    if (d < 3600) return Math.floor(d / 60) + ' min ago';
    if (d < 86400) return Math.floor(d / 3600) + ' h ago';
    var days = Math.floor(d / 86400);
    if (days === 1) return 'yesterday';
    if (days < 7) return days + ' days ago';
    var dt = new Date(ts);
    var opts = { month: 'short', day: 'numeric' };
    if (dt.getFullYear() !== new Date().getFullYear()) opts.year = 'numeric';
    return dt.toLocaleDateString(undefined, opts);
  }

  function fullTime(ts) {
    var dt = new Date(ts);
    return dt.toLocaleString(undefined,
      { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric',
        hour: '2-digit', minute: '2-digit' });
  }

  /* ---------------------------- state ---------------------------------- */
  var data = { notebooks: [], notes: [] };           /* notes belong to sectionId */
  var ui = { currentSection: null, currentNote: null, sidebarOpen: true, previewOn: false };
  var dirty = false, saveTimer = 0, metaTimer = 0, renderQueued = false;
  var searchTerm = '';

  function defaultData() {
    var nb = { id: uid(), name: PALETTE[0].name, color: PALETTE[0].color, open: true, sections: [] };
    var s1 = { id: uid(), notebookId: nb.id, name: 'Getting Started' };
    var s2 = { id: uid(), notebookId: nb.id, name: 'Study Notes' };
    nb.sections = [s1, s2];
    var now = Date.now();
    var n1 = {
      id: uid(), sectionId: s1.id, title: 'Welcome to OneNote Lite 📝',
      created: now, modified: now, content: SAMPLE_MD
    };
    var n2 = {
      id: uid(), sectionId: s2.id, title: 'Markdown Cheat Sheet',
      created: now - 60000, modified: now - 30000, content: CHEAT_MD
    };
    return { notebooks: [nb], sections: [s1, s2], notes: [n1, n2] };
  }

  var SAMPLE_MD = [
    '# Welcome 👋',
    '',
    'This is **OneNote Lite** — a featherweight notes workspace in pure',
    'HTML, CSS and JavaScript. No frameworks, no build step, no network.',
    '',
    '> Everything you type is saved to your browser\'s **localStorage**',
    '> automatically, about half a second after you stop typing.',
    '',
    '## What it can do',
    '',
    '- 📒 Notebooks → Sections → Pages, just like OneNote',
    '- ✍️ Markdown editing with an instant **preview toggle** (`Ctrl P`)',
    '- 🔎 Instant fuzzy search across titles & content (`Ctrl F`)',
    '- ⌨️ Full keyboard navigation (`Alt ↑ / ↓`, `Esc`, `Ctrl B`)',
    '- 🕒 Last-edited timestamps on every page',
    '',
    '## Try a task list',
    '',
    '- [x] Open the app',
    '- [ ] Create a new note with `Ctrl Alt N`',
    '- [ ] Toggle the preview with `Ctrl P`',
    '',
    '```js',
    '// code blocks are rendered too',
    'console.log("Happy writing!");',
    '```',
    '',
    'Delete this page any time with the 🗑 button — happy note-taking!'
  ].join('\n');

  var CHEAT_MD = [
    '# Markdown Cheat Sheet',
    '',
    '| Syntax | Result |',
    '| --- | --- |',
    '| `**bold**` | **bold** |',
    '| `*italic*` | *italic* |',
    '| `~~strike~~` | ~~strike~~ |',
    '| `` `code` `` | `code` |',
    '| `[link](https://example.com)` | [link](https://example.com) |',
    '',
    '## Lists',
    '',
    '1. Ordered items',
    '2. Just number them',
    '   - Nested bullets work',
    '   - Indent by two spaces',
    '',
    '---',
    '',
    'Blockquotes:',
    '',
    '> Simplicity is the ultimate sophistication.',
    '',
    'Headings: `# H1` through `###### H6`.'
  ].join('\n');

  /* --------------------------- persistence ----------------------------- */
  function load() {
    try {
      var raw = localStorage.getItem(LS_DATA);
      if (raw) {
        var parsed = JSON.parse(raw);
        if (parsed && Array.isArray(parsed.notebooks) && Array.isArray(parsed.notes)) {
          data = parsed;
          if (!Array.isArray(data.sections)) data.sections = [];
          normalizeData();
          return;
        }
      }
    } catch (e) { /* corrupted store → start fresh */ }
    data = defaultData();
    persistNow();
  }

  /* Repair any structural gaps so rendering can never crash: notebooks
     always carry a sections array, every section links to a real notebook,
     and every note links to a real section (orphans are re-homed). */
  function normalizeData() {
    if (!Array.isArray(data.notebooks)) data.notebooks = [];
    if (!Array.isArray(data.sections)) data.sections = [];
    if (!Array.isArray(data.notes)) data.notes = [];

    var nbIds = {};
    for (var i = 0; i < data.notebooks.length; i++) {
      var nb = data.notebooks[i];
      if (!nb || typeof nb.id !== 'string') { data.notebooks.splice(i--, 1); continue; }
      if (!Array.isArray(nb.sections)) nb.sections = [];
      nbIds[nb.id] = true;
    }

    var secIds = {};
    for (var j = 0; j < data.sections.length; j++) {
      var s = data.sections[j];
      if (!s || typeof s.id !== 'string' || !nbIds[s.notebookId]) {
        data.sections.splice(j--, 1); continue;
      }
      secIds[s.id] = true;
    }

    // Rebuild each notebook's section list from the flat sections array.
    for (var k = 0; k < data.notebooks.length; k++) data.notebooks[k].sections = [];
    for (var m = 0; m < data.sections.length; m++) {
      var sec = data.sections[m];
      for (var n2 = 0; n2 < data.notebooks.length; n2++) {
        if (data.notebooks[n2].id === sec.notebookId) {
          data.notebooks[n2].sections.push(sec); break;
        }
      }
    }

    // Re-home orphan notes into the first available section (or create one).
    var fallback = data.sections.length ? data.sections[0].id : null;
    if (!fallback && data.notebooks.length) {
      var nb0 = data.notebooks[0];
      var s0 = { id: uid(), notebookId: nb0.id, name: 'Notes' };
      data.sections.push(s0);
      nb0.sections.push(s0);
      fallback = s0.id;
    }
    for (var p = 0; p < data.notes.length; p++) {
      var note = data.notes[p];
      if (!note || typeof note.id !== 'string') { data.notes.splice(p--, 1); continue; }
      if (!secIds[note.sectionId]) {
        if (!fallback) { data.notes.splice(p--, 1); continue; }
        note.sectionId = fallback;
      }
      if (typeof note.content !== 'string') note.content = '';
      if (typeof note.title !== 'string') note.title = '';
      if (typeof note.created !== 'number') note.created = Date.now();
      if (typeof note.modified !== 'number') note.modified = note.created;
    }
  }

  function loadUI() {
    try {
      var u = JSON.parse(localStorage.getItem(LS_UI) || '{}');
      if (u && typeof u === 'object') {
        ui.sidebarOpen = u.sidebarOpen !== false;
        ui.previewOn = !!u.previewOn;
        ui.currentSection = u.currentSection || null;
        ui.currentNote = u.currentNote || null;
      }
    } catch (e) {}
  }

  function persistNow() {
    try {
      localStorage.setItem(LS_DATA, JSON.stringify(data));
      setSaveState('Saved · ' + timeAgo(Date.now()));
    } catch (e) {
      setSaveState('⚠ Storage full — export your notes!');
    }
    updateStorageInfo();
  }

  function schedulePersist() {
    dirty = true;
    setSaveState('Saving…');
    clearTimeout(saveTimer);
    saveTimer = setTimeout(function () {
      dirty = false;
      persistNow();
    }, 450);
  }

  function persistUI() {
    try { localStorage.setItem(LS_UI, JSON.stringify(ui)); } catch (e) {}
  }

  function updateStorageInfo() {
    var bytes = 0;
    try { bytes = (localStorage.getItem(LS_DATA) || '').length; } catch (e) {}
    var kb = (bytes / 1024).toFixed(1);
    $('storage-info').textContent = kb + ' KB stored locally';
  }

  /* ----------------------------- lookups ------------------------------- */
  function sectionById(id) {
    for (var i = 0; i < data.sections.length; i++) if (data.sections[i].id === id) return data.sections[i];
    return null;
  }
  function notebookById(id) {
    for (var i = 0; i < data.notebooks.length; i++) if (data.notebooks[i].id === id) return data.notebooks[i];
    return null;
  }
  function noteById(id) {
    for (var i = 0; i < data.notes.length; i++) if (data.notes[i].id === id) return data.notes[i];
    return null;
  }
  function notesOfSection(sectionId) {
    return data.notes.filter(function (n) { return n.sectionId === sectionId; })
                     .sort(function (a, b) { return b.modified - a.modified; });
  }

  /* ------------------------- visible-note model ------------------------ */
  function visibleNotes() {
    var list = ui.currentSection ? notesOfSection(ui.currentSection) : [];
    if (searchTerm) {
      var q = searchTerm.toLowerCase();
      list = data.notes.slice()
        .filter(function (n) {
          return n.title.toLowerCase().indexOf(q) !== -1 ||
                 n.content.toLowerCase().indexOf(q) !== -1;
        })
        .sort(function (a, b) { return b.modified - a.modified; });
    }
    return list;
  }

  /* ------------------------------ sidebar ------------------------------ */
  function renderSidebar() {
    var tree = $('tree');
    var frag = document.createDocumentFragment();

    for (var nb_i = 0; nb_i < data.notebooks.length; nb_i++) {
      var nb = data.notebooks[nb_i];
      var group = el('div', 'notebook' + (nb.open ? ' open' : ''));

      var tab = el('button', 'nb-tab');
      tab.style.setProperty('--nb-color', nb.color);
      tab.setAttribute('role', 'treeitem');
      tab.setAttribute('aria-expanded', nb.open ? 'true' : 'false');
      tab.dataset.nb = nb.id;
      tab.appendChild(el('span', 'nb-caret', '▸'));
      tab.appendChild(el('span', 'nb-name', nb.name));
      var count = data.notes.filter(function (n) {
        return nb.sections.some(function (s) { return s.id === n.sectionId; });
      }).length;
      tab.appendChild(el('span', 'nb-count', String(count)));
      group.appendChild(tab);

      var secsWrap = el('div', 'nb-sections');
      for (var s_i = 0; s_i < nb.sections.length; s_i++) {
        var sec = nb.sections[s_i];
        var btn = el('button', 'sec-tab' + (sec.id === ui.currentSection ? ' active' : ''));
        btn.setAttribute('role', 'treeitem');
        btn.dataset.sec = sec.id;
        var bar = el('span', 'sec-bar');
        bar.style.background = nb.color;
        btn.appendChild(bar);
        btn.appendChild(el('span', 'sec-name', sec.name));
        btn.appendChild(el('span', 'sec-count', String(notesOfSection(sec.id).length)));
        secsWrap.appendChild(btn);
      }
      var addSec = el('button', 'sec-add', '+ New Section');
      addSec.dataset.addSection = nb.id;
      secsWrap.appendChild(addSec);
      group.appendChild(secsWrap);

      frag.appendChild(group);
    }

    /* page list for the current section (or search results) */
    var pages = el('div', 'pages-panel');
    var heading = el('h3', 'pages-title',
      searchTerm ? ('Search: "' + searchTerm + '"') :
      ((sectionById(ui.currentSection) || {}).name || 'Pages'));
    pages.appendChild(heading);

    var list = visibleNotes();
    if (!list.length) {
      pages.appendChild(el('p', 'pages-empty',
        searchTerm ? 'No notes match your search.' : 'No pages yet — press Ctrl Alt N.'));
    }
    for (var k = 0; k < list.length; k++) {
      var n = list[k];
      var item = el('button', 'page-item' + (n.id === ui.currentNote ? ' active' : ''));
      item.dataset.note = n.id;
      var pt = el('span', 'page-title', n.title || 'Untitled page');
      var pm = el('span', 'page-meta', timeAgo(n.modified));
      var snippet = n.content.replace(/[#>*`\[\]_~-]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 70);
      var ps = el('span', 'page-snippet', snippet || 'Empty page');
      item.appendChild(pt); item.appendChild(pm); item.appendChild(ps);
      pages.appendChild(item);
    }
    frag.appendChild(pages);

    tree.textContent = '';
    tree.appendChild(frag);
  }

  /* ------------------------------ editor ------------------------------- */
  function syncEditorFromNote() {
    var note = noteById(ui.currentNote);
    var hasNote = !!note;
    $('welcome').hidden = hasNote;
    $('editor').hidden = !hasNote || ui.previewOn;
    $('preview').hidden = !hasNote || !ui.previewOn;
    $('btn-delete').disabled = !hasNote;
    $('btn-preview').disabled = !hasNote;
    $('note-title').contentEditable = hasNote ? 'true' : 'false';

    if (!hasNote) {
      $('note-title').textContent = '';
      $('note-modified').textContent = '';
      $('word-count').textContent = '0 words';
      $('char-count').textContent = '0 chars';
      setSaveState('Ready');
      return;
    }
    if ($('note-title').textContent !== note.title) $('note-title').textContent = note.title;
    $('editor').value = note.content;
    $('note-modified').textContent = 'Edited ' + fullTime(note.modified);
    updateCounts(note.content);
    if (ui.previewOn) renderPreview(note.content);
  }

  function updateCounts(text) {
    var words = (text.match(/\S+/g) || []).length;
    $('word-count').textContent = words + (words === 1 ? ' word' : ' words');
    $('char-count').textContent = text.length + ' chars';
  }

  function renderPreview(text) {
    /* Debounced via rAF batching — cheap even on weak CPUs. */
    if (renderQueued) return;
    renderQueued = true;
    requestAnimationFrame(function () {
      renderQueued = false;
      $('preview').innerHTML = MD.render(text);
    });
  }

  function setSaveState(s) { $('save-state').textContent = s; }

  /* ---------------------------- mutations ------------------------------ */
  function commitTitle() {
    var note = noteById(ui.currentNote);
    if (!note) return;
    var t = $('note-title').textContent.replace(/\s+/g, ' ').trim().slice(0, 200);
    if (t !== note.title) {
      note.title = t;
      note.modified = Date.now();
      schedulePersist();
      renderSidebar();
    }
  }

  function createNote(sectionId) {
    var sid = sectionId || ui.currentSection;
    if (!sid) {                                  /* fall back to first section */
      if (!data.sections.length) {
        var nb = data.notebooks[0];
        var sec = { id: uid(), notebookId: nb.id, name: 'Quick Notes' };
        nb.sections.push(sec);
        data.sections.push(sec);
      }
      sid = data.sections[0].id;
    }
    var now = Date.now();
    var note = { id: uid(), sectionId: sid, title: '', created: now, modified: now, content: '' };
    data.notes.push(note);
    ui.currentSection = sid;
    ui.currentNote = note.id;
    clearSearch(false);
    schedulePersist();
    persistUI();
    renderSidebar();
    syncEditorFromNote();
    $('note-title').focus();
    selectAllIn($('note-title'));
    return note;
  }

  function deleteCurrentNote() {
    var note = noteById(ui.currentNote);
    if (!note) return;
    var siblings = notesOfSection(note.sectionId);
    var idx = siblings.indexOf(note);
    data.notes.splice(data.notes.indexOf(note), 1);
    var next = siblings[idx + 1] || siblings[idx - 1] ||
               (visibleNotes()[0] || null);
    ui.currentNote = next ? next.id : null;
    schedulePersist();
    persistUI();
    renderSidebar();
    syncEditorFromNote();
    toast('Note deleted');
    $('editor').focus();
  }

  function selectNote(id) {
    if (ui.currentNote === id) { $('editor').focus(); return; }
    commitTitleQuiet();
    var n = noteById(id);
    if (!n) return;
    ui.currentNote = id;
    ui.currentSection = n.sectionId;
    persistUI();
    renderSidebar();
    syncEditorFromNote();
    $('editor').focus();
  }

  function commitTitleQuiet() {
    var note = noteById(ui.currentNote);
    if (!note) return;
    var t = $('note-title').textContent.replace(/\s+/g, ' ').trim().slice(0, 200);
    if (t !== note.title) { note.title = t; note.modified = Date.now(); schedulePersist(); }
  }

  function switchNote(delta) {
    var list = visibleNotes();
    if (!list.length) return;
    var idx = -1;
    for (var i = 0; i < list.length; i++) if (list[i].id === ui.currentNote) { idx = i; break; }
    var ni = clamp(idx + delta, 0, list.length - 1);
    if (ni === idx && delta < 0 && list.length) ni = list.length - 1; /* wrap */
    if (ni === idx && delta > 0 && list.length) ni = 0;
    selectNote(list[ni].id);
  }

  function selectAllIn(e) {
    if (e.tagName === 'TEXTAREA' || e.tagName === 'INPUT') { e.select(); return; }
    var r = document.createRange();
    r.selectNodeContents(e);
    var sel = window.getSelection();
    sel.removeAllRanges(); sel.addRange(r);
  }

  /* ------------------------------ search ------------------------------- */
  function onSearchInput() {
    searchTerm = $('search').value.trim();
    if (searchTerm) {
      var list = visibleNotes();
      if (list.length && !list.some(function (n) { return n.id === ui.currentNote; })) {
        ui.currentNote = list[0].id;
        ui.currentSection = list[0].sectionId;
        syncEditorFromNote();
      }
    } else {
      /* returning from search: re-open current section's top note */
      var notes = notesOfSection(ui.currentSection);
      if (notes.length && !noteById(ui.currentNote)) { ui.currentNote = notes[0].id; syncEditorFromNote(); }
    }
    renderSidebar();
    syncEditorFromNote();
  }

  function clearSearch(rerender) {
    searchTerm = '';
    $('search').value = '';
    if (rerender) { renderSidebar(); syncEditorFromNote(); }
  }

  /* ------------------------------ dialogs ------------------------------ */
  function confirmDelete() {
    var note = noteById(ui.currentNote);
    if (!note) return;
    var dlg = $('dlg-delete');
    $('dlg-delete-text').textContent =
      '"' + (note.title || 'Untitled page') + '" will be permanently removed.';
    if (typeof dlg.showModal === 'function') {
      dlg.returnValue = 'cancel';
      dlg.showModal();
    } else if (confirm('Delete this note?')) {
      deleteCurrentNote();
    }
  }

  var toastTimer = 0;
  function toast(msg) {
    var t = $('toast');
    t.textContent = msg;
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.hidden = true; }, 2200);
  }

  /* ------------------------- export / import --------------------------- */
  function exportJSON() {
    commitTitleQuiet();
    var blob = new Blob([JSON.stringify(data, null, 1)], { type: 'application/json' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'onenote-lite-backup-' + new Date().toISOString().slice(0, 10) + '.json';
    a.click();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 5000);
    toast('Backup downloaded');
  }

  function importJSON(file) {
    var reader = new FileReader();
    reader.onload = function () {
      try {
        var obj = JSON.parse(reader.result);
        if (!obj || !Array.isArray(obj.notebooks) || !Array.isArray(obj.notes) || !Array.isArray(obj.sections))
          throw new Error('bad shape');
        data = obj;
        normalizeData();
        var firstSec = data.sections[0];
        ui.currentSection = firstSec ? firstSec.id : null;
        var nn = ui.currentSection ? notesOfSection(ui.currentSection) : [];
        ui.currentNote = nn[0] ? nn[0].id : null;
        persistNow();
        persistUI();
        renderSidebar();
        syncEditorFromNote();
        toast('Imported ' + data.notes.length + ' notes');
      } catch (e) {
        toast('⚠ Invalid backup file');
      }
    };
    reader.readAsText(file);
  }

  /* --------------------------- live timestamps ------------------------- */
  function startMetaTicker() {
    /* one lightweight interval; only touches DOM while the tab is visible */
    setInterval(function () {
      if (document.hidden) return;
      var note = noteById(ui.currentNote);
      if (note) $('note-modified').textContent = 'Edited ' + fullTime(note.modified);
      /* refresh relative times in the page list without rebuilding it */
      var items = document.querySelectorAll('.page-item');
      for (var i = 0; i < items.length; i++) {
        var n = noteById(items[i].dataset.note);
        if (n) {
          var m = items[i].querySelector('.page-meta');
          if (m) m.textContent = timeAgo(n.modified);
        }
      }
    }, 30000);
  }

  /* ------------------------------ events ------------------------------- */
  function wire() {
    var editor = $('editor');

    editor.addEventListener('input', function () {
      var note = noteById(ui.currentNote);
      if (!note) return;
      note.content = editor.value;
      note.modified = Date.now();
      updateCounts(editor.value);
      if (ui.previewOn) renderPreview(editor.value);
      schedulePersist();
      /* keep sidebar "edited" stamps fresh without a full rebuild each keystroke */
      clearTimeout(metaTimer);
      metaTimer = setTimeout(function () {
        var cur = document.querySelector('.page-item.active .page-meta');
        if (cur) cur.textContent = timeAgo(note.modified);
      }, 600);
    });

    /* Tab key inserts two spaces instead of leaving the editor */
    editor.addEventListener('keydown', function (e) {
      if (e.key === 'Tab' && !e.ctrlKey && !e.altKey && !e.metaKey) {
        e.preventDefault();
        var s = editor.selectionStart, en = editor.selectionEnd;
        editor.setRangeText('  ', s, en, 'end');
        editor.dispatchEvent(new Event('input'));
      }
    });

    var titleEl = $('note-title');
    titleEl.addEventListener('input', function () {
      /* prevent multi-line paste weirdness quickly */
      var note = noteById(ui.currentNote);
      if (!note) return;
      note.title = titleEl.textContent.replace(/\s+/g, ' ').trim().slice(0, 200);
      note.modified = Date.now();
      schedulePersist();
    });
    titleEl.addEventListener('blur', function () { commitTitle(); renderSidebar(); });
    titleEl.addEventListener('paste', function (e) {
      e.preventDefault();
      var t = (e.clipboardData || window.clipboardData).getData('text');
      document.execCommand('insertText', false, t.replace(/\s+/g, ' '));
    });
    titleEl.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { e.preventDefault(); editor.focus(); }
    });

    $('btn-new').addEventListener('click', function () { createNote(); });
    $('btn-welcome-new').addEventListener('click', function () { createNote(); });
    $('btn-welcome-sample').addEventListener('click', function () {
      var any = data.notes[0];
      if (any) selectNote(any.id); else createNote();
    });
    $('btn-delete').addEventListener('click', confirmDelete);
    $('dlg-delete').addEventListener('close', function () {
      if (this.returnValue === 'ok') deleteCurrentNote();
    });

    $('btn-preview').addEventListener('click', togglePreview);
    $('btn-sidebar').addEventListener('click', toggleSidebar);

    var searchEl = $('search');
    searchEl.addEventListener('input', debounce(onSearchInput, 120));
    searchEl.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { clearSearch(true); editor.focus(); }
      if (e.key === 'Enter') {
        var list = visibleNotes();
        if (list.length) selectNote(list[0].id);
        editor.focus();
      }
    });

    /* delegated clicks inside the tree */
    $('tree').addEventListener('click', function (e) {
      var t = e.target.closest('[data-nb],[data-sec],[data-note],[data-add-section]');
      if (!t) return;
      if (t.dataset.nb) {
        var nb = notebookById(t.dataset.nb);
        if (nb) { nb.open = !nb.open; schedulePersist(); renderSidebar(); }
      } else if (t.dataset.sec) {
        var sec = sectionById(t.dataset.sec);
        if (sec) {
          ui.currentSection = sec.id;
          clearSearch(false);
          var notes = notesOfSection(sec.id);
          ui.currentNote = notes[0] ? notes[0].id : null;
          persistUI();
          renderSidebar();
          syncEditorFromNote();
          if (ui.currentNote) editor.focus();
        }
      } else if (t.dataset.note) {
        selectNote(t.dataset.note);
      } else if (t.dataset.addSection) {
        var nbk = notebookById(t.dataset.addSection);
        var name = prompt('New section name:', 'New Section');
        if (name && name.trim() && nbk) {
          var s = { id: uid(), notebookId: nbk.id, name: name.trim().slice(0, 60) };
          nbk.sections.push(s);
          data.sections.push(s);
          ui.currentSection = s.id;
          schedulePersist(); persistUI();
          renderSidebar(); syncEditorFromNote();
          createNote(s.id);
        }
      }
    });

    $('link-export').addEventListener('click', function (e) { e.preventDefault(); exportJSON(); });
    $('file-import').addEventListener('change', function () {
      if (this.files && this.files[0]) importJSON(this.files[0]);
      this.value = '';
    });

    /* cross-tab sync — free consistency, negligible cost */
    window.addEventListener('storage', function (e) {
      if (e.key === LS_DATA && !dirty) {
        try {
          data = JSON.parse(e.newValue);
          normalizeData();
          renderSidebar(); syncEditorFromNote();
        } catch (err) {}
      }
    });

    /* flush pending edits before unload */
    window.addEventListener('beforeunload', function () {
      if (dirty) { clearTimeout(saveTimer); persistNow(); }
    });

    document.addEventListener('keydown', onGlobalKeys);
  }

  function togglePreview() {
    var hasNote = !!noteById(ui.currentNote);
    if (!hasNote) return;
    ui.previewOn = !ui.previewOn;
    $('btn-preview').setAttribute('aria-pressed', ui.previewOn ? 'true' : 'false');
    $('btn-preview').classList.toggle('on', ui.previewOn);
    $('editor').hidden = ui.previewOn;
    $('preview').hidden = !ui.previewOn;
    if (ui.previewOn) renderPreview($('editor').value);
    persistUI();
    (ui.previewOn ? $('preview') : $('editor')).focus({ preventScroll: true });
  }

  function toggleSidebar() {
    ui.sidebarOpen = !ui.sidebarOpen;
    $('sidebar').classList.toggle('collapsed', !ui.sidebarOpen);
    persistUI();
  }

  function onGlobalKeys(e) {
    var mod = e.ctrlKey || e.metaKey;
    var tag = (document.activeElement || {}).tagName;
    var inField = tag === 'TEXTAREA' || tag === 'INPUT' ||
                  (document.activeElement && document.activeElement.isContentEditable);

    if (mod && e.altKey && (e.key === 'n' || e.key === 'N')) { e.preventDefault(); createNote(); return; }
    if (mod && (e.key === 'f' || e.key === 'F') && !e.shiftKey) {
      e.preventDefault(); $('search').focus(); $('search').select(); return;
    }
    if (mod && (e.key === 'p' || e.key === 'P') && !e.shiftKey) { e.preventDefault(); togglePreview(); return; }
    if (mod && (e.key === 'b' || e.key === 'B') && !e.shiftKey) {
      /* let Ctrl+B bold inside editor? we use it for sidebar — OneNote-like chrome */
      e.preventDefault(); toggleSidebar(); return;
    }
    if (mod && e.shiftKey && (e.key === 'N' || e.key === 'n')) { e.preventDefault(); createNote(); return; }
    if (mod && e.shiftKey && e.key === 'Backspace') { e.preventDefault(); confirmDelete(); return; }
    if (e.altKey && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      e.preventDefault(); switchNote(e.key === 'ArrowDown' ? 1 : -1); return;
    }
    if (mod && e.key === 'PageDown') { e.preventDefault(); switchNote(1); return; }
    if (mod && e.key === 'PageUp') { e.preventDefault(); switchNote(-1); return; }
    if (e.key === 'Escape') {
      var dlg = $('dlg-delete');
      if (dlg.open) return;                    /* dialog handles its own Esc */
      if (searchTerm) { clearSearch(true); $('editor').focus(); return; }
      if (inField) { $('editor').blur(); return; }
    }
    if (e.key === '/' && !inField) { e.preventDefault(); $('search').focus(); return; }
  }

  function debounce(fn, ms) {
    var t = 0;
    return function () {
      var args = arguments, self = this;
      clearTimeout(t);
      t = setTimeout(function () { fn.apply(self, args); }, ms);
    };
  }

  /* ------------------------------- boot -------------------------------- */
  function boot() {
    load();
    loadUI();

    /* validate persisted selection */
    if (!sectionById(ui.currentSection)) {
      ui.currentSection = data.sections.length ? data.sections[0].id : null;
    }
    var cur = noteById(ui.currentNote);
    if (!cur) {
      var notes = ui.currentSection ? notesOfSection(ui.currentSection) : [];
      ui.currentNote = notes[0] ? notes[0].id : null;
    } else {
      ui.currentSection = cur.sectionId;
    }

    $('sidebar').classList.toggle('collapsed', !ui.sidebarOpen);
    $('btn-preview').setAttribute('aria-pressed', ui.previewOn ? 'true' : 'false');
    $('btn-preview').classList.toggle('on', ui.previewOn);

    wire();
    renderSidebar();
    syncEditorFromNote();
    startMetaTicker();

    /* honour prefers-reduced-motion via CSS; nothing needed here */
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
