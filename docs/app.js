/* Storage Inventory — web version (works on iPhone, Android and desktop browsers).
 * Everything is stored on the device (IndexedDB). No account, no server. */
'use strict';

const APP_VERSION = '1.4.2.1';

// ===========================================================================
// Small helpers
// ===========================================================================
const $ = (sel, root = document) => root.querySelector(sel);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const uid = () => (crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36) + Math.random().toString(36).slice(2));
const pad = (n) => String(n).padStart(2, '0');
const fmtDate = (ms) => {
  if (!ms) return 'Unknown';
  const d = new Date(ms);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};
const typeLabel = (n) => n.type === 'STORAGE' ? (n.qr ? 'Storage' : 'Temporary storage') : n.type === 'CONTAINER' ? 'Container' : 'Item';
const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 's'}`;
const countText = ({ items, containers }) =>
  containers ? `${plural(items, 'item')} · ${plural(containers, 'container')}` : plural(items, 'item');
const canHold = (n) => n.type !== 'ITEM';
const photosOf = (n) => (n && n.photoIds) || [];
const mainPhoto = (n) => photosOf(n)[0] || null;
const photoCount = (n) => photosOf(n).length;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const ICON = {
  back: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20z"/></svg>',
  search: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14"/></svg>',
  plus: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg>',
  gear: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.47.47 0 0 0-.12-.61zM12 15.6A3.6 3.6 0 1 1 15.6 12 3.6 3.6 0 0 1 12 15.6"/></svg>',
  more: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 8a2 2 0 1 0-2-2 2 2 0 0 0 2 2m0 2a2 2 0 1 0 2 2 2 2 0 0 0-2-2m0 6a2 2 0 1 0 2 2 2 2 0 0 0-2-2"/></svg>',
  close: '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>',
};

// ===========================================================================
// Database (IndexedDB)
// ===========================================================================
const DB = {
  db: null,
  open() {
    return new Promise((resolve, reject) => {
      const req = indexedDB.open('storage-inventory', 2);
      req.onupgradeneeded = () => {
        const db = req.result;
        const have = db.objectStoreNames;
        if (!have.contains('nodes')) db.createObjectStore('nodes', { keyPath: 'id' });
        if (!have.contains('photos')) db.createObjectStore('photos', { keyPath: 'id' });
        if (!have.contains('features')) db.createObjectStore('features', { keyPath: 'id' });
        if (!have.contains('log')) db.createObjectStore('log', { keyPath: 'n', autoIncrement: true });
      };
      req.onsuccess = () => { this.db = req.result; resolve(); };
      req.onerror = () => reject(req.error);
    });
  },
  request(store, mode, fn) {
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction(store, mode);
      const req = fn(tx.objectStore(store));
      tx.oncomplete = () => resolve(req ? req.result : undefined);
      tx.onerror = () => reject(tx.error);
      tx.onabort = () => reject(tx.error);
    });
  },
  all: (s) => DB.request(s, 'readonly', (st) => st.getAll()),
  get: (s, id) => DB.request(s, 'readonly', (st) => st.get(id)),
  put: (s, v) => DB.request(s, 'readwrite', (st) => st.put(v)),
  del: (s, id) => DB.request(s, 'readwrite', (st) => st.delete(id)),
  clear: (s) => DB.request(s, 'readwrite', (st) => st.clear()),
  count: (s) => DB.request(s, 'readonly', (st) => st.count()),
};

// ===========================================================================
// Activity log: every interaction with date and time, for troubleshooting
// ===========================================================================
const Log = {
  MAX: 5000,
  screen: 'Home',
  add(event, details = '') {
    if (!DB.db) return;
    DB.put('log', { t: Date.now(), e: event, d: String(details), s: this.screen, v: APP_VERSION }).catch(() => {});
  },
  async trim() {
    const n = await DB.count('log');
    if (n <= this.MAX) return;
    let extra = n - this.MAX;
    await new Promise((resolve) => {
      const tx = DB.db.transaction('log', 'readwrite');
      tx.objectStore('log').openCursor().onsuccess = (e) => {
        const cur = e.target.result;
        if (cur && extra-- > 0) { cur.delete(); cur.continue(); }
      };
      tx.oncomplete = resolve; tx.onerror = resolve;
    });
  },
};
window.addEventListener('error', (e) => Log.add('Error', `${e.message} @ ${e.filename}:${e.lineno}`));
window.addEventListener('unhandledrejection', (e) => Log.add('Error', String(e.reason && (e.reason.stack || e.reason.message) || e.reason)));

// ===========================================================================
// Inventory store (nodes kept in memory, saved to IndexedDB)
// ===========================================================================
const Store = {
  nodes: [],
  async load() {
    this.nodes = await DB.all('nodes');
    // Versions before 1.4.2.1 kept a single photoId; convert to a list.
    for (const n of this.nodes) {
      if (!Array.isArray(n.photoIds)) {
        n.photoIds = n.photoId ? [n.photoId] : [];
        delete n.photoId;
        await DB.put('nodes', n);
      }
    }
  },
  get(id) { return this.nodes.find((n) => n.id === id) || null; },
  storages() { return this.nodes.filter((n) => !n.parentId).sort((a, b) => a.name.localeCompare(b.name)); },
  children(id) {
    return this.nodes.filter((n) => n.parentId === id)
      .sort((a, b) => (a.type === 'ITEM') - (b.type === 'ITEM') || a.name.localeCompare(b.name));
  },
  findByQr(qr) { return qr ? this.nodes.find((n) => n.qr === qr) || null : null; },
  path(id) {
    const out = [];
    let n = this.get(id), guard = 0;
    while (n && guard++ < 100) { out.unshift(n); n = n.parentId ? this.get(n.parentId) : null; }
    return out;
  },
  location(id) { return this.path(id).slice(0, -1).map((n) => n.name).join(' › '); },
  countInside(id) {
    let items = 0, containers = 0;
    const queue = [id];
    while (queue.length) {
      const p = queue.shift();
      for (const n of this.nodes) if (n.parentId === p) {
        if (n.type === 'ITEM') items++; else { containers++; queue.push(n.id); }
      }
    }
    return { items, containers };
  },
  async add({ qr = '', name, type, parentId = null, photoIds = [] }) {
    const node = { id: uid(), qr, name: name.trim(), type, parentId, createdAt: Date.now(), photoIds: [...photoIds] };
    this.nodes.push(node);
    await DB.put('nodes', node);
    Log.add(`Added ${type.toLowerCase()}`, `“${node.name}”` + (qr ? ` · QR ${qr}` : ' · no QR') +
      (parentId ? ` · in ${this.path(parentId).map((p) => p.name).join(' › ')}` : '') + (photoIds.length ? ` · ${photoIds.length} photo(s)` : ' · no photo'));
    return node;
  },
  async update(id, patch) {
    const n = this.get(id);
    if (!n) return;
    const before = { ...n };
    Object.assign(n, patch);
    await DB.put('nodes', n);
    if ('name' in patch) Log.add('Renamed', `“${before.name}” → “${n.name}”`);
    if ('qr' in patch) Log.add('QR code assigned', `“${n.name}” · QR ${n.qr}`);
  },
  async addPhoto(id, photoId) {
    const n = this.get(id);
    if (!n) return;
    n.photoIds = [...photosOf(n), photoId];
    await DB.put('nodes', n);
    Log.add('Photo added', `“${n.name}” · now ${n.photoIds.length} photo(s)`);
  },
  async removePhoto(id, photoId) {
    const n = this.get(id);
    if (!n) return;
    n.photoIds = photosOf(n).filter((p) => p !== photoId);
    await DB.put('nodes', n);
    await Photos.remove(photoId);
    Log.add('Photo deleted', `“${n.name}” · ${n.photoIds.length} photo(s) left`);
  },
  async remove(id) {
    const target = this.get(id);
    if (target) {
      const c = this.countInside(id);
      Log.add(`Deleted ${target.type.toLowerCase()}`, `“${target.name}” · with ${c.items} item(s), ${c.containers} container(s) inside`);
    }
    const remove = new Set();
    const queue = [id];
    while (queue.length) {
      const cur = queue.shift();
      if (remove.has(cur)) continue;
      remove.add(cur);
      this.nodes.filter((n) => n.parentId === cur).forEach((n) => queue.push(n.id));
    }
    const gone = this.nodes.filter((n) => remove.has(n.id));
    this.nodes = this.nodes.filter((n) => !remove.has(n.id));
    for (const n of gone) {
      await DB.del('nodes', n.id);
      for (const p of photosOf(n)) await Photos.remove(p);
    }
  },
};

// ===========================================================================
// Photos (full size + thumbnail, kept as JPEG blobs)
// ===========================================================================
function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Could not load image'));
    img.src = src;
  });
}
function scaleToBlob(source, w, h, maxSide, quality = 0.85) {
  const scale = Math.min(1, maxSide / Math.max(w, h));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(w * scale);
  canvas.height = Math.round(h * scale);
  canvas.getContext('2d').drawImage(source, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality));
}

const Photos = {
  urls: new Map(),
  /** Saves an image (canvas, video frame, or image element) and returns its id. */
  async save(source, w, h) {
    const full = await scaleToBlob(source, w, h, 1600);
    const thumb = await scaleToBlob(source, w, h, 260, 0.8);
    const id = uid();
    await DB.put('photos', { id, full, thumb });
    return id;
  },
  async saveBlob(blob) {
    const url = URL.createObjectURL(blob);
    try {
      const img = await loadImage(url);
      return await this.save(img, img.naturalWidth, img.naturalHeight);
    } finally { URL.revokeObjectURL(url); }
  },
  async url(id, kind = 'thumb') {
    if (!id) return null;
    const key = id + ':' + kind;
    if (this.urls.has(key)) return this.urls.get(key);
    const rec = await DB.get('photos', id);
    if (!rec) return null;
    const url = URL.createObjectURL(rec[kind] || rec.full);
    this.urls.set(key, url);
    return url;
  },
  async remove(id) {
    for (const k of ['thumb', 'full']) {
      const key = id + ':' + k;
      if (this.urls.has(key)) { URL.revokeObjectURL(this.urls.get(key)); this.urls.delete(key); }
    }
    await DB.del('photos', id);
    await DB.del('features', id);
  },
};

/** Fills every <img data-photo="id" data-kind="thumb|full"> with its photo. */
async function hydratePhotos(root = document) {
  for (const img of root.querySelectorAll('img[data-photo]')) {
    if (img.dataset.loaded) continue;
    img.dataset.loaded = '1';
    const url = await Photos.url(img.dataset.photo, img.dataset.kind || 'thumb');
    if (url) img.src = url;
  }
}

// ===========================================================================
// Camera: QR scanning and photo capture
// ===========================================================================
const Camera = {
  el: null, video: null, finder: null, stream: null, mode: null,
  resolve: null, loop: 0,

  init() {
    this.el = $('#camera');
    this.video = $('#camera-video');
    this.finder = $('#camera-finder');
    $('#camera-close').innerHTML = ICON.close;
    $('#camera-close').onclick = () => this.finish(null, true);
    $('#camera-skip').onclick = () => this.finish(null, false);
    $('#camera-shutter').onclick = () => this.capture();
  },

  async ensureStream() {
    if (this.stream) return true;
    if (!navigator.mediaDevices?.getUserMedia) return false;
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 } },
      });
      this.video.srcObject = this.stream;
      await this.video.play().catch(() => {});
      return true;
    } catch (e) {
      console.warn('Camera unavailable', e);
      return false;
    }
  },

  show(title, subtitle) {
    $('#camera-title').textContent = title;
    $('#camera-subtitle').textContent = subtitle || '';
    $('#camera-error').hidden = true;
    this.el.hidden = false;
  },

  close() {
    cancelAnimationFrame(this.loop);
    this.loop = 0;
    if (this.stream) this.stream.getTracks().forEach((t) => t.stop());
    this.stream = null;
    this.video.srcObject = null;
    this.el.hidden = true;
    this.mode = null;
  },

  /** cancelled=true means the user tapped X (stop the whole flow). */
  finish(value, cancelled) {
    const r = this.resolve;
    this.resolve = null;
    cancelAnimationFrame(this.loop);
    this.loop = 0;
    if (cancelled || !this.keepOpen) this.close();
    if (r) r({ value, cancelled });
  },

  drawFinder(scanMode) {
    const c = this.finder, dpr = window.devicePixelRatio || 1;
    c.width = c.clientWidth * dpr; c.height = c.clientHeight * dpr;
    const g = c.getContext('2d');
    g.clearRect(0, 0, c.width, c.height);
    if (!scanMode) return;
    const side = Math.min(c.width, c.height) * 0.68;
    const x = (c.width - side) / 2, y = (c.height - side) / 2, r = 24 * dpr;
    g.fillStyle = 'rgba(0,0,0,0.5)';
    g.beginPath();
    g.rect(0, 0, c.width, c.height);
    g.roundRect ? g.roundRect(x, y, side, side, r) : g.rect(x, y, side, side);
    g.fill('evenodd');
    g.strokeStyle = '#F2B35B'; g.lineWidth = 5 * dpr;
    const len = side * 0.16;
    [[x, y, 1, 1], [x + side, y, -1, 1], [x, y + side, 1, -1], [x + side, y + side, -1, -1]].forEach(([px, py, dx, dy]) => {
      g.beginPath(); g.moveTo(px + dx * len, py); g.lineTo(px, py); g.lineTo(px, py + dy * len); g.stroke();
    });
  },

  /** Opens the camera and resolves with the text of the first QR code seen (or null). */
  async scan({ title, subtitle, keepOpen = false }) {
    Log.add('Scanner opened', title);
    this.keepOpen = keepOpen;
    this.mode = 'scan';
    this.show(title, subtitle);
    $('#camera-bottom').hidden = true;
    requestAnimationFrame(() => this.drawFinder(true));
    const ok = await this.ensureStream();
    if (!ok) {
      $('#camera-error').hidden = false;
      $('#camera-error').textContent = 'Camera access is needed to scan QR codes. Allow camera access for this site in Settings › Safari › Camera, then try again.';
    }
    return new Promise((resolve) => {
      this.resolve = resolve;
      if (!ok) return;
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d', { willReadFrequently: true });
      let last = 0;
      const tick = (t) => {
        this.loop = requestAnimationFrame(tick);
        if (t - last < 120 || this.video.readyState < 2) return;
        last = t;
        const vw = this.video.videoWidth, vh = this.video.videoHeight;
        if (!vw) return;
        const scale = Math.min(1, 720 / Math.max(vw, vh));
        canvas.width = Math.round(vw * scale); canvas.height = Math.round(vh * scale);
        ctx.drawImage(this.video, 0, 0, canvas.width, canvas.height);
        const data = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = window.jsQR(data.data, data.width, data.height, { inversionAttempts: 'attemptBoth' });
        if (code && code.data && code.data.trim()) this.finish(code.data.trim(), false);
      };
      this.loop = requestAnimationFrame(tick);
    }).then((r) => {
      Log.add(r.value ? 'QR scanned' : 'Scan cancelled', r.value ? `${r.value} · ${title}` : title);
      return r.value;
    });
  },

  /**
   * Opens the camera with a shutter button. Resolves with
   *  { photoId }            photo saved
   *  { blob }               when save=false
   *  { skipped: true }      user tapped "Skip photo"
   *  null                   user cancelled (X)
   */
  async photo({ title, subtitle, allowSkip = true, save = true }) {
    this.keepOpen = false;
    this.mode = 'photo';
    this.save = save;
    this.show(title, subtitle);
    $('#camera-bottom').hidden = false;
    $('#camera-skip').style.visibility = allowSkip ? 'visible' : 'hidden';
    this.drawFinder(false);
    const ok = await this.ensureStream();
    if (!ok) {
      Log.add('Live camera unavailable', 'Using the system camera instead');
      // No live camera: fall back to the system camera via a file input (needs a tap).
      this.close();
      return pickPhotoWithSystemCamera({ title, allowSkip, save });
    }
    const r = await new Promise((resolve) => { this.resolve = resolve; });
    if (r.cancelled) { Log.add('Photo cancelled', title); return null; }
    if (r.value == null) { Log.add('Photo skipped', title); return { skipped: true }; }
    Log.add('Photo taken', title);
    return r.value;
  },

  async capture() {
    const v = this.video;
    if (!v.videoWidth) return;
    const canvas = document.createElement('canvas');
    canvas.width = v.videoWidth; canvas.height = v.videoHeight;
    canvas.getContext('2d').drawImage(v, 0, 0);
    $('#camera-shutter').disabled = true;
    try {
      let value;
      if (this.save) value = { photoId: await Photos.save(canvas, canvas.width, canvas.height) };
      else value = { blob: await scaleToBlob(canvas, canvas.width, canvas.height, 1200) };
      this.finish(value, false);
    } finally {
      $('#camera-shutter').disabled = false;
    }
  },
};

/** Fallback when the live camera isn't available: the phone's own camera via a file picker. */
function pickPhotoWithSystemCamera({ title, allowSkip, save }) {
  return new Promise((resolve) => {
    const input = $('#file-photo');
    const close = dialog({
      title,
      body: '<p>Tap “Open camera” to take the photo.</p>',
      actions: [
        ...(allowSkip ? [{ label: 'Skip photo', kind: 'text', onClick: () => { close(); resolve({ skipped: true }); } }] : []),
        { label: 'Cancel', kind: 'text', onClick: () => { close(); resolve(null); } },
        {
          label: 'Open camera', kind: 'text', onClick: () => {
            input.value = '';
            input.onchange = async () => {
              const file = input.files && input.files[0];
              close();
              if (!file) return resolve(null);
              if (save) resolve({ photoId: await Photos.saveBlob(file) });
              else resolve({ blob: file });
            };
            input.click();
          },
        },
      ],
    });
  });
}

// ===========================================================================
// Dialogs, menus, toasts
// ===========================================================================
function dialog({ title, body = '', actions = [], onOpen, dismissable = true, onDismiss }) {
  const scrim = document.createElement('div');
  scrim.className = 'scrim';
  scrim.innerHTML = `<div class="dialog" role="dialog" aria-modal="true">
      ${title ? `<h3>${esc(title)}</h3>` : ''}
      <div class="body">${body}</div>
      <div class="actions"></div>
    </div>`;
  const box = $('.actions', scrim);
  for (const a of actions) {
    const b = document.createElement('button');
    b.className = 'btn text' + (a.kind === 'danger' ? ' danger' : '');
    b.textContent = a.label;
    if (a.id) b.id = a.id;
    b.onclick = a.onClick;
    box.appendChild(b);
  }
  const close = () => scrim.remove();
  if (dismissable) scrim.addEventListener('click', (e) => { if (e.target === scrim) { close(); onDismiss && onDismiss(); } });
  $('#layer').appendChild(scrim);
  hydratePhotos(scrim);
  onOpen && onOpen(scrim);
  return close;
}

function alertDialog(text, title = '') {
  Log.add('Notice shown', (title ? title + ': ' : '') + text);
  return new Promise((resolve) => {
    const close = dialog({
      title, body: `<p>${esc(text)}</p>`, onDismiss: resolve,
      actions: [{ label: 'OK', onClick: () => { close(); resolve(); } }],
    });
  });
}

function confirmDialog({ title, text, ok = 'OK', cancel = 'Cancel', danger = false }) {
  return new Promise((resolve) => {
    const close = dialog({
      title, body: `<p>${esc(text)}</p>`, onDismiss: () => resolve(false),
      actions: [
        { label: cancel, onClick: () => { close(); resolve(false); } },
        { label: ok, kind: danger ? 'danger' : '', onClick: () => { close(); resolve(true); } },
      ],
    });
  });
}

function showMenu(anchor, items) {
  const layer = document.createElement('div');
  layer.className = 'menu-layer';
  const menu = document.createElement('div');
  menu.className = 'menu';
  for (const it of items) {
    if (!it) continue;
    const b = document.createElement('button');
    b.textContent = it.label;
    if (it.danger) b.className = 'danger';
    b.disabled = !!it.disabled;
    b.onclick = (e) => { e.stopPropagation(); layer.remove(); it.onClick(); };
    menu.appendChild(b);
  }
  layer.appendChild(menu);
  layer.onclick = () => layer.remove();
  document.body.appendChild(layer);
  const r = anchor.getBoundingClientRect();
  const mw = menu.offsetWidth, mh = menu.offsetHeight;
  let left = Math.min(window.innerWidth - mw - 8, Math.max(8, r.right - mw));
  let top = r.bottom + 4;
  if (top + mh > window.innerHeight - 8) top = Math.max(8, r.top - mh - 4);
  menu.style.left = left + 'px';
  menu.style.top = top + 'px';
}

function toast(text) {
  const t = document.createElement('div');
  t.className = 'toast';
  t.textContent = text;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 2600);
}

/** Full-screen photos: swipe left/right; Delete is offered for photos of a saved entry. */
function photoViewer(ids, start = 0, nodeId = null) {
  if (!Array.isArray(ids)) ids = [ids];
  if (!ids.length) return;
  const v = document.createElement('div');
  v.className = 'viewer';
  v.innerHTML = `<div class="pages">${ids.map((id) =>
      `<div class="page" data-pid="${esc(id)}"><img data-photo="${esc(id)}" data-kind="full" alt=""></div>`).join('')}</div>
    <div class="viewer-bar">
      ${nodeId ? '<button class="btn text danger" data-v="delete">Delete</button>' : '<span style="width:72px"></span>'}
      <span class="viewer-count"></span>
      <button class="btn text light" data-v="close">Close</button>
    </div>`;
  document.body.appendChild(v);
  hydratePhotos(v);
  const pages = $('.pages', v);
  const current = () => Math.round(pages.scrollLeft / Math.max(1, pages.clientWidth));
  const count = () => { $('.viewer-count', v).textContent = ids.length > 1 ? `${current() + 1} / ${ids.length} · swipe` : 'Tap photo to close'; };
  requestAnimationFrame(() => { pages.scrollLeft = start * pages.clientWidth; count(); });
  pages.addEventListener('scroll', count, { passive: true });
  pages.addEventListener('click', () => v.remove());
  $('[data-v="close"]', v).onclick = () => v.remove();
  const del = $('[data-v="delete"]', v);
  if (del) del.onclick = async () => {
    const pid = ids[current()];
    const idx = current();
    const ok = await confirmDialog({ title: 'Delete this photo?', text: "This can't be undone.", ok: 'Delete', danger: true });
    if (!ok) return;
    await Store.removePhoto(nodeId, pid);
    v.remove();
    render();
    const left = photosOf(Store.get(nodeId));
    if (left.length) photoViewer(left, Math.min(idx, left.length - 1), nodeId);
  };
}

/** Thumbnail strip used in dialogs and headers. */
function photoStrip(ids, { removable = false, big = false } = {}) {
  return `<div class="strip${big ? ' big' : ''}">${ids.map((id, i) =>
    `<div class="strip-item"><img data-photo="${esc(id)}" data-kind="${big ? 'full' : 'thumb'}" data-strip="${i}" alt="">
      ${removable ? `<button class="strip-x" data-remove="${esc(id)}" aria-label="Remove photo">${ICON.close}</button>` : ''}</div>`).join('')}</div>`;
}

/**
 * Name (and optionally type) dialog for a new entry, with photo preview and retake.
 * Resolves { name, type, photoId } or null when cancelled (the photo is then deleted).
 */
function entryDialog({ title, message, label = 'Name', confirm = 'Done', photoIds = [], needType = false, photoTitle }) {
  return new Promise((resolve) => {
    let photos = [...photoIds];
    let type = null;
    const photoHtml = () => photos.length
      ? photoStrip(photos, { removable: true }) +
        `<div class="no-photo" style="padding:0"><span>${photos.length === 1 ? '1 photo' : photos.length + ' photos'}</span><button class="btn text" id="d-retake">Add another photo</button></div>`
      : `<div class="no-photo" style="padding-left:0"><span>No photo</span><button class="btn text" id="d-retake">Take photo</button></div>`;
    const body = `
      <div id="d-photo">${photoHtml()}</div>
      ${message ? `<p class="small-muted">${esc(message)}</p>` : ''}
      ${needType ? `
        <p style="margin:12px 0 0;font-weight:600">What is this?</p>
        <label class="choice"><input type="radio" name="d-type" value="ITEM"><span>Item<small>A single thing you're storing</small></span></label>
        <label class="choice"><input type="radio" name="d-type" value="CONTAINER"><span>Smaller container<small>A box or bin that holds more items</small></span></label>` : ''}
      <label class="field"><span id="d-label">${esc(label)}</span><input id="d-name" autocomplete="off" autocapitalize="sentences" enterkeyhint="done"></label>`;
    let close;
    const done = (value) => { close(); resolve(value); };
    const update = (scrim) => {
      const name = $('#d-name', scrim).value.trim();
      $('#d-ok', scrim).disabled = !name || (needType && !type);
    };
    close = dialog({
      title, body, dismissable: false,
      actions: [
        { label: 'Cancel', onClick: () => { photos.forEach((p) => Photos.remove(p)); done(null); } },
        { label: confirm, id: 'd-ok', onClick: () => done({ name: $('#d-name').value.trim(), type, photoIds: photos }) },
      ],
      onOpen: (scrim) => {
        const refresh = () => { $('#d-photo', scrim).innerHTML = photoHtml(); hydratePhotos(scrim); wire(); };
        const wire = () => {
          $('#d-retake', scrim).onclick = async () => {
            const r = await Camera.photo({ title: photoTitle || 'Take another photo', subtitle: 'Another angle of the same thing.', allowSkip: false });
            if (r && r.photoId) { photos.push(r.photoId); refresh(); }
          };
          scrim.querySelectorAll('[data-remove]').forEach((b) => b.onclick = async (e) => {
            e.stopPropagation();
            const id = b.dataset.remove;
            photos = photos.filter((p) => p !== id);
            await Photos.remove(id);
            refresh();
          });
          scrim.querySelectorAll('#d-photo [data-strip]').forEach((img) => img.onclick = () => photoViewer(photos, Number(img.dataset.strip)));
        };
        wire();
        scrim.querySelectorAll('input[name="d-type"]').forEach((r) => r.onchange = () => {
          type = r.value;
          $('#d-label', scrim).textContent = type === 'CONTAINER' ? 'Container name' : 'Item name';
          update(scrim);
        });
        const input = $('#d-name', scrim);
        input.oninput = () => update(scrim);
        input.onkeydown = (e) => { if (e.key === 'Enter' && !$('#d-ok', scrim).disabled) $('#d-ok', scrim).click(); };
        update(scrim);
        if (!needType) setTimeout(() => input.focus(), 50);
      },
    });
  });
}

function textDialog({ title, label, initial = '', confirm = 'Save' }) {
  return new Promise((resolve) => {
    let close;
    close = dialog({
      title, dismissable: false,
      body: `<label class="field"><span>${esc(label)}</span><input id="t-in" autocomplete="off" value="${esc(initial)}"></label>`,
      actions: [
        { label: 'Cancel', onClick: () => { close(); resolve(null); } },
        { label: confirm, id: 't-ok', onClick: () => { const v = $('#t-in').value.trim(); close(); resolve(v); } },
      ],
      onOpen: (scrim) => {
        const input = $('#t-in', scrim);
        const upd = () => { $('#t-ok', scrim).disabled = !input.value.trim(); };
        input.oninput = upd;
        input.onkeydown = (e) => { if (e.key === 'Enter' && input.value.trim()) $('#t-ok', scrim).click(); };
        upd();
        setTimeout(() => { input.focus(); input.select(); }, 50);
      },
    });
  });
}

// ===========================================================================
// Navigation state
// ===========================================================================
const state = {
  screen: 'home',        // home | detail | search | settings
  stack: [],             // ids of opened storages/containers
  search: { query: '', photoBlob: null, photoUrl: null, results: null, progress: null, error: null, note: null },
  update: null,          // { version, notes } when a newer version is published
};

function go(screen) { state.screen = screen; render(); window.scrollTo(0, 0); }
function openNode(node) {
  const target = canHold(node) ? node : (node.parentId ? Store.get(node.parentId) : null);
  state.stack = target ? Store.path(target.id).map((n) => n.id) : [];
  state.screen = state.stack.length ? 'detail' : 'home';
  render();
  window.scrollTo(0, 0);
  if (!canHold(node)) itemDialog(node);
}
function back() {
  if (state.screen === 'search' || state.screen === 'settings') {
    if (state.screen === 'search') clearSearchPhoto();
    state.screen = state.stack.length ? 'detail' : 'home';
  } else if (state.stack.length) {
    state.stack.pop();
    state.screen = state.stack.length ? 'detail' : 'home';
  }
  render();
}

// ===========================================================================
// Rendering
// ===========================================================================
function topbar({ title, crumbs, backBtn = false, actions = '' }) {
  return `<header class="topbar">
    ${backBtn ? `<button class="icon-btn" data-act="back" aria-label="Back">${ICON.back}</button>` : '<span style="width:6px"></span>'}
    <div class="titles"><h1>${esc(title)}</h1>${crumbs ? `<div class="crumbs">${esc(crumbs)}</div>` : ''}</div>
    ${actions}
  </header>`;
}

function badge(node, small = false) {
  const letter = node.type === 'STORAGE' ? 'S' : node.type === 'CONTAINER' ? 'C' : 'I';
  return `<div class="badge ${letter}${small ? ' small' : ''}">${letter}</div>`;
}

function nodeRow(node, detail, { score } = {}) {
  const thumb = mainPhoto(node)
    ? `<div class="thumb-wrap"><img class="thumb" data-photo="${esc(mainPhoto(node))}" alt="">${badge(node, true)}</div>`
    : `<div class="thumb-wrap">${badge(node)}</div>`;
  return `<div class="card row clickable" data-open="${esc(node.id)}">
    ${thumb}
    <div class="main"><div class="name">${esc(node.name)}</div><div class="detail">${esc(detail)}</div></div>
    ${score != null ? `<span class="score">${Math.round(score * 100)}%</span>`
      : `<button class="icon-btn" data-menu="${esc(node.id)}" aria-label="More options">${ICON.more}</button>`}
  </div>`;
}

function render() {
  const app = $('#app');
  // Drop navigation entries that were deleted.
  while (state.stack.length && !Store.get(state.stack[state.stack.length - 1])) state.stack.pop();
  if (state.screen === 'detail' && !state.stack.length) state.screen = 'home';

  const top = state.stack.length ? Store.get(state.stack[state.stack.length - 1]) : null;
  const screenName = state.screen === 'search' ? 'Search' : state.screen === 'settings' ? 'Settings'
    : top ? `${typeLabel(top)}: ${top.name}` : 'Home';
  if (screenName !== Log.screen) { Log.screen = screenName; Log.add('Opened screen', screenName); }

  let html = '';
  if (state.screen === 'home') html = renderHome();
  else if (state.screen === 'detail') html = renderDetail();
  else if (state.screen === 'search') html = renderSearch();
  else if (state.screen === 'settings') html = renderSettings();
  app.innerHTML = html;
  hydratePhotos(app);
  const lc = $('#log-count');
  if (lc) DB.count('log').then((n) => { lc.textContent = n; }).catch(() => {});
  if (state.screen === 'search') wireSearch();
}

function updateBanner() {
  if (!state.update) return '';
  return `<div class="banner"><span>Version ${esc(state.update.version)} is available.</span>
    <button class="btn text" data-act="apply-update">Update</button></div>`;
}

function installBanner() {
  const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent);
  const standalone = navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  let dismissed = false;
  try { dismissed = localStorage.getItem('installTipDismissed') === '1'; } catch (e) { /* ignore */ }
  if (!isIos || standalone || dismissed) return '';
  return `<div class="banner"><span>Install: tap Share <b>⎙</b> then “Add to Home Screen”. It then opens like an app and keeps your data safe.</span>
    <button class="btn text" data-act="dismiss-install">OK</button></div>`;
}

function renderHome() {
  const storages = Store.storages();
  const actions = `
    ${Store.nodes.length ? `<button class="icon-btn" data-act="search" aria-label="Search">${ICON.search}</button>` : ''}
    <button class="icon-btn" data-act="add-menu" aria-label="Add">${ICON.plus}</button>
    <button class="icon-btn" data-act="settings" aria-label="Settings and updates">${ICON.gear}</button>`;
  let body;
  if (!storages.length) {
    body = `<div class="empty"><h2>No Storage Data Available</h2><p>Tap + in the top right corner to add a storage.</p></div>`;
  } else {
    body = `<div class="list">${storages.map((s) =>
      nodeRow(s, (s.qr ? '' : 'Temporary · ') + countText(Store.countInside(s.id)))).join('')}</div>`;
  }
  return topbar({ title: 'Storage Inventory', actions }) + updateBanner() + installBanner() + `<main class="content">${body}</main>`;
}

function renderDetail() {
  const node = Store.get(state.stack[state.stack.length - 1]);
  const crumbs = state.stack.length > 1 ? Store.location(node.id) : typeLabel(node);
  const kids = Store.children(node.id);
  const ph = photosOf(node);
  const header = ph.length
    ? (ph.length === 1
        ? `<img class="photo-header" data-photo="${esc(ph[0])}" data-kind="full" data-view-node="${esc(node.id)}" data-index="0" alt="">`
        : `<div class="strip big">${ph.map((id, i) => `<div class="strip-item"><img data-photo="${esc(id)}" data-kind="full" data-view-node="${esc(node.id)}" data-index="${i}" alt=""></div>`).join('')}</div>`) +
      `<div class="no-photo" style="padding:0 0 0 4px"><span>${ph.length === 1 ? '1 photo' : ph.length + ' photos · swipe to see all'}</span>
         <button class="btn text" data-act="photo-node" data-id="${esc(node.id)}">Add photo</button></div>`
    : `<div class="card no-photo"><span>No photo of this ${esc(typeLabel(node).toLowerCase())} yet</span>
         <button class="btn text" data-act="photo-node" data-id="${esc(node.id)}">Take photo</button></div>`;
  let list;
  if (!kids.length) {
    list = `<p class="hint" style="text-align:center;padding:32px 16px">No items yet. Press + to start adding.</p>`;
  } else {
    list = `<div class="label">Total inside: ${esc(countText(Store.countInside(node.id)))}</div>` +
      kids.map((c) => nodeRow(c, (canHold(c) ? `Container · ${countText(Store.countInside(c.id))}` : 'Item') +
        (photoCount(c) > 1 ? ` · ${photoCount(c)} photos` : ''))).join('');
  }
  return topbar({
    title: node.name, crumbs, backBtn: true,
    actions: `<button class="icon-btn" data-menu="${esc(node.id)}" aria-label="More options">${ICON.more}</button>`,
  }) + `<main class="content"><div class="list">${header}${list}</div></main>
    <button class="fab" data-act="detail-add">${ICON.plus}Add</button>`;
}

function searchResultsHtml() {
  const s = state.search;
  const q = s.query.trim().toLowerCase();
  let results = '';
  if (q) {
    const hits = Store.nodes.filter((n) => n.name.toLowerCase().includes(q) || (n.qr && n.qr.toLowerCase().includes(q)))
      .sort((a, b) => a.name.localeCompare(b.name));
    results = `<div class="label">${hits.length} result${hits.length === 1 ? '' : 's'} for “${esc(s.query.trim())}”</div>` +
      hits.map((n) => nodeRow(n, `${typeLabel(n)} · ${Store.location(n.id) || 'Top level'}`)).join('');
  } else if (s.photoUrl) {
    results = `<div class="row" style="padding-left:0"><img class="thumb" src="${s.photoUrl}" alt="" style="width:72px;height:72px">
      <div class="main"><div class="name">Your photo</div><button class="btn text" data-act="clear-photo" style="padding-left:0">Clear</button></div></div>`;
    if (s.error) results += `<p style="color:var(--error)">${esc(s.error)}</p>`;
    else if (!s.results) {
      const p = s.progress;
      const pct = p && p.total ? Math.round((p.done / p.total) * 100) : 0;
      results += `<p class="hint">${p && p.total ? `Comparing with your stored photos… ${p.done}/${p.total}` : esc(p && p.text || 'Analysing your photo…')}</p>
        <div class="progress${p && p.total ? ' det' : ''}"><div style="${p && p.total ? `width:${pct}%` : ''}"></div></div>`;
    } else if (!s.results.length) {
      results += `<p class="hint">No similar items found. Try a closer photo with the object filling most of the frame.</p>`;
    } else {
      results += `<div class="label">Most similar</div>` +
        s.results.map(({ node, score }) => nodeRow(node, `${typeLabel(node)} · ${Store.location(node.id) || 'Top level'}`, { score })).join('');
    }
    if (s.note) results += `<p class="small-muted">${esc(s.note)}</p>`;
  } else {
    results = `<p class="hint">Type a name, take a photo to find similar-looking things you've stored, or scan a QR code.</p>`;
  }
  return results;
}

function renderSearch() {
  const s = state.search;
  const results = searchResultsHtml();
  return topbar({ title: 'Search', backBtn: true }) + `<main class="content"><div class="list">
      <label class="field" style="margin-top:0"><input id="search-input" type="search" placeholder="Search by name" value="${esc(s.query)}" autocomplete="off"></label>
      <div class="btn-row"><button class="btn" data-act="search-photo">Search by photo</button>
        <button class="btn outline" data-act="search-qr">Scan QR code</button></div>
      <div class="list" id="search-results">${results}</div>
    </div></main>`;
}

function wireSearch() {
  const input = $('#search-input');
  if (!input) return;
  input.oninput = () => {
    state.search.query = input.value;
    clearTimeout(wireSearch.timer);
    const q = input.value.trim();
    if (q) wireSearch.timer = setTimeout(() => {
      const n = Store.nodes.filter((x) => x.name.toLowerCase().includes(q.toLowerCase()) || (x.qr && x.qr.toLowerCase().includes(q.toLowerCase()))).length;
      Log.add('Searched by name', `“${q}” · ${n} result(s)`);
    }, 1500);
    if (input.value.trim()) clearSearchPhoto();
    const box = $('#search-results');
    box.innerHTML = searchResultsHtml();
    hydratePhotos(box);
  };
}

function renderSettings() {
  const counts = { S: 0, C: 0, I: 0 };
  Store.nodes.forEach((n) => { counts[n.type[0]]++; });
  const standalone = navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  const u = state.updateCheck || {};
  let updateHtml = '';
  if (u.status === 'checking') updateHtml = `<p class="hint">Checking for updates…</p><div class="progress"><div></div></div>`;
  else if (u.status === 'latest') updateHtml = `<p style="color:var(--primary)">You're up to date. Latest published version is ${esc(u.version)}.</p>`;
  else if (u.status === 'available') updateHtml = `<p style="font-weight:700;margin-bottom:4px">Version ${esc(u.version)} is available</p>
      ${u.notes ? `<p class="hint">${esc(u.notes)}</p>` : ''}<button class="btn" data-act="apply-update">Update now</button>`;
  else if (u.status === 'error') updateHtml = `<p style="color:var(--error)">${esc(u.message)}</p>`;
  return topbar({ title: 'Settings', backBtn: true }) + `<main class="content"><div class="list">
    <div class="card" style="padding:16px">
      <div class="name" style="font-weight:600;font-size:17px">App updates</div>
      <p>Installed version: ${APP_VERSION} (web)</p>
      ${updateHtml}
      ${u.status !== 'available' ? `<button class="btn outline" data-act="check-update" ${u.status === 'checking' ? 'disabled' : ''}>Check for updates</button>` : ''}
    </div>
    ${standalone ? '' : `<div class="card" style="padding:16px">
      <div style="font-weight:600;font-size:17px">Install on your Home Screen</div>
      <p class="hint">In Safari, tap the Share button, then “Add to Home Screen”. The app then opens full screen, works offline, and iOS keeps its data safe.</p></div>`}
    <div class="card" style="padding:16px">
      <div style="font-weight:600;font-size:17px">Backup</div>
      <p class="hint">Your inventory and photos are saved on this device only. Save a backup file now and then, or to move to another phone.</p>
      <div class="btn-row"><button class="btn outline" data-act="export">Save backup</button>
        <button class="btn outline" data-act="import">Restore backup</button></div>
    </div>
    <div class="card" style="padding:16px">
      <div style="font-weight:600;font-size:17px">Troubleshooting</div>
      <p class="hint">Every action in the app is recorded with its date and time (<span id="log-count">…</span> entries kept on this device).</p>
      <button class="btn outline" data-act="export-log">Export activity log (Excel)</button>
    </div>
    <div class="card" style="padding:16px">
      <div style="font-weight:600;font-size:17px">Inventory</div>
      <p>${plural(counts.S, 'storage')} · ${plural(counts.C, 'container')} · ${plural(counts.I, 'item')}</p>
    </div>
  </div></main>`;
}

// ===========================================================================
// Item & info dialogs
// ===========================================================================
function itemDialog(item) {
  Log.add('Opened item', `“${item.name}”`);
  const ph = photosOf(item);
  const photo = ph.length
    ? (ph.length === 1
        ? `<img class="dialog-photo" style="height:220px" data-photo="${esc(ph[0])}" data-kind="full" data-strip="0" alt="">`
        : photoStrip(ph, { big: true })) +
      `<p class="small-muted" style="margin-top:6px">${ph.length === 1 ? '1 photo' : ph.length + ' photos · swipe to see all'}</p>`
    : `<p class="small-muted">No photo yet</p>`;
  const close = dialog({
    title: item.name,
    body: `${photo}<p style="margin-top:10px">Stored in: ${esc(Store.location(item.id) || 'Top level')}</p>`,
    actions: [
      { label: 'Info', onClick: () => { close(); infoDialog(item); } },
      { label: 'Add photo', onClick: () => { close(); flowAddPhoto(item); } },
      { label: 'Rename', onClick: () => { close(); flowRename(item); } },
      { label: 'Delete', kind: 'danger', onClick: () => { close(); flowDelete(item); } },
      { label: 'Close', onClick: () => close() },
    ],
    onOpen: (scrim) => {
      scrim.querySelectorAll('[data-strip]').forEach((img) => img.onclick = () => { close(); photoViewer(photosOf(item), Number(img.dataset.strip), item.id); });
    },
  });
}

function infoDialog(node) {
  Log.add('Viewed info', `“${node.name}”`);
  const row = (k, v) => `<div class="info-row"><div class="k">${esc(k)}</div><div class="v">${esc(v)}</div></div>`;
  const close = dialog({
    title: 'Info',
    body: row('Name', node.name) + row('Type', typeLabel(node)) + row('Added', fmtDate(node.createdAt)) +
      row('Stored in', Store.location(node.id) || 'Top level') + row('Photos', String(photoCount(node))) +
      (canHold(node) ? row('Contains', countText(Store.countInside(node.id))) : '') +
      row('QR code', node.qr || 'None yet (use “Assign QR code” in the ⋮ menu)'),
    actions: [{ label: 'Close', onClick: () => close() }],
  });
}

function nodeMenu(anchor, node) {
  showMenu(anchor, [
    { label: 'Info', onClick: () => infoDialog(node) },
    !node.qr ? { label: 'Assign QR code', onClick: () => flowAssignQr(node) } : null,
    { label: 'Add photo', onClick: () => flowAddPhoto(node) },
    canHold(node) ? { label: 'Export to Excel', onClick: () => exportStorageExcel(node) } : null,
    { label: 'Rename', onClick: () => flowRename(node) },
    { label: 'Delete', danger: true, onClick: () => flowDelete(node) },
  ]);
}

// ===========================================================================
// Flows
// ===========================================================================
async function showDuplicate(node) {
  Log.add('Duplicate QR code', `Belongs to “${node.name}” · QR ${node.qr}`);
  const where = Store.location(node.id);
  const open = await confirmDialog({
    title: 'QR code already registered',
    text: `This code belongs to the ${typeLabel(node).toLowerCase()} “${node.name}”${where ? ` in ${where}` : ''}. Each QR code can only be used once.`,
    ok: 'Open it', cancel: 'OK',
  });
  if (open) openNode(node);
}

async function flowNewStorage() {
  const qr = await Camera.scan({ title: 'Scan the Storage QR code', subtitle: 'Point the camera at the QR code on the storage unit.', keepOpen: true });
  if (!qr) return Camera.close();
  const dup = Store.findByQr(qr);
  if (dup) { Camera.close(); return showDuplicate(dup); }
  const shot = await Camera.photo({ title: 'Take a photo of the storage unit', subtitle: 'Or tap “Skip photo”.' });
  if (!shot) return;
  const res = await entryDialog({
    title: 'Name this storage', message: `QR code: ${qr}`, label: 'Storage name', confirm: 'Done',
    photoIds: shot.photoId ? [shot.photoId] : [], photoTitle: 'Another photo of the storage unit',
  });
  if (!res) return;
  const storage = await Store.add({ qr, name: res.name, type: 'STORAGE', photoIds: res.photoIds });
  openNode(storage);
}

async function flowTemporaryStorage() {
  const res = await entryDialog({
    title: 'Create temporary storage',
    message: 'No QR code needed. You can attach one later with “Assign QR code” in its ⋮ menu.',
    label: 'Storage name', confirm: 'Create', photoTitle: 'Take a photo of the storage',
  });
  if (!res) return;
  const storage = await Store.add({ qr: '', name: res.name, type: 'STORAGE', photoIds: res.photoIds });
  openNode(storage);
}

async function flowAddChildByQr(parentId) {
  for (;;) {
    const parent = Store.get(parentId);
    if (!parent) return;
    const qr = await Camera.scan({
      title: 'Scan the next QR code',
      subtitle: `Adding to “${parent.name}”. Scan the code on the item or smaller container.`, keepOpen: true,
    });
    if (!qr) return Camera.close();
    const dup = Store.findByQr(qr);
    if (dup) { Camera.close(); return showDuplicate(dup); }
    const shot = await Camera.photo({ title: 'Take a photo of the item(s)', subtitle: 'Or tap “Skip photo”.' });
    if (!shot) return;
    const res = await entryDialog({
      title: `Add to “${parent.name}”`, message: `QR code: ${qr}`, needType: true,
      photoIds: shot.photoId ? [shot.photoId] : [], photoTitle: 'Another photo of the item(s)',
    });
    if (!res) return;
    await Store.add({ qr, name: res.name, type: res.type, parentId, photoIds: res.photoIds });
    render();
    const again = await confirmDialog({
      title: `Added “${res.name}”`, text: `Scan the next QR code to keep adding to “${parent.name}”?`,
      ok: 'Scan next', cancel: 'Done',
    });
    if (!again) return;
  }
}

function chooseTarget(photoId) {
  return new Promise((resolve) => {
    const targets = Store.nodes.filter(canHold)
      .map((n) => ({ n, label: Store.path(n.id).map((p) => p.name).join(' › ') }))
      .sort((a, b) => a.label.localeCompare(b.label));
    const close = dialog({
      title: 'Add to which storage?', dismissable: false,
      body: `<img class="dialog-photo" style="height:120px" data-photo="${esc(photoId)}" alt="">
        <div class="pick-list">${targets.map(({ n, label }) =>
          `<button class="pick" data-pick="${esc(n.id)}">${badge(n, true)}<span>${esc(label)}</span></button>`).join('')}</div>`,
      actions: [
        { label: 'Scan its QR code instead', onClick: async () => {
          close();
          const qr = await Camera.scan({ title: "Scan the storage's QR code", subtitle: 'Scan the QR code of the storage or container to put this item in.' });
          if (!qr) return resolve(await chooseTarget(photoId));
          const node = Store.findByQr(qr);
          if (!node) { await alertDialog("That QR code isn't in your inventory yet. Pick a storage from the list instead."); return resolve(await chooseTarget(photoId)); }
          if (!canHold(node)) { await alertDialog(`That code belongs to the item “${node.name}”. Scan a storage or container instead.`); return resolve(await chooseTarget(photoId)); }
          resolve(node);
        } },
        { label: 'Cancel', onClick: () => { close(); resolve(null); } },
      ],
      onOpen: (scrim) => scrim.querySelectorAll('[data-pick]').forEach((b) => b.onclick = () => { close(); resolve(Store.get(b.dataset.pick)); }),
    });
  });
}

async function flowAddByPhoto(targetId = null) {
  if (!Store.nodes.some(canHold)) return alertDialog('Add a storage first, then you can add items to it by photo.');
  const shot = await Camera.photo({ title: 'Take a photo of the item', allowSkip: false });
  if (!shot || !shot.photoId) return;
  let target = targetId ? Store.get(targetId) : null;
  if (!target) target = await chooseTarget(shot.photoId);
  if (!target) { await Photos.remove(shot.photoId); return; }
  const res = await entryDialog({
    title: `Add to “${target.name}”`, message: 'No QR code (added by photo)', needType: true,
    photoIds: [shot.photoId], photoTitle: 'Another photo of the item',
  });
  if (!res) return;
  await Store.add({ qr: '', name: res.name, type: res.type, parentId: target.id, photoIds: res.photoIds });
  openNode(target);
  toast(`Added “${res.name}”`);
}

async function flowAssignQr(node) {
  const qr = await Camera.scan({ title: 'Scan a QR code', subtitle: `The code will be attached to “${node.name}”.` });
  if (!qr) return;
  const dup = Store.findByQr(qr);
  if (dup) return showDuplicate(dup);
  await Store.update(node.id, { qr });
  render();
  toast(`QR code attached to “${node.name}”`);
}

async function flowAddPhoto(node) {
  const shot = await Camera.photo({ title: `Photo of “${node.name}”`, subtitle: photoCount(node) ? 'Adds another photo.' : '', allowSkip: false });
  if (!shot || !shot.photoId) return;
  await Store.addPhoto(node.id, shot.photoId);
  render();
}

async function flowRename(node) {
  const name = await textDialog({ title: `Rename ${typeLabel(node).toLowerCase()}`, label: 'Name', initial: node.name });
  if (!name) return;
  await Store.update(node.id, { name });
  render();
}

async function flowDelete(node) {
  const inside = Store.countInside(node.id);
  const extra = inside.items + inside.containers ? ` Everything inside it will be removed too (${countText(inside)}).` : '';
  const ok = await confirmDialog({ title: `Delete “${node.name}”?`, text: `This can't be undone.${extra}`, ok: 'Delete', danger: true });
  if (!ok) return;
  await Store.remove(node.id);
  render();
}

async function flowFindByQr() {
  const qr = await Camera.scan({ title: 'Find by QR code', subtitle: 'Scan any code to jump to where it is stored.' });
  if (!qr) return;
  const node = Store.findByQr(qr);
  if (!node) return alertDialog(`No storage, container or item uses this QR code:\n${qr}`, 'Not in your inventory');
  clearSearchPhoto();
  openNode(node);
}

// ===========================================================================
// Search by photo (on-device AI + colour matching)
// ===========================================================================
function loadScript(src) {
  return new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = src; s.onload = resolve; s.onerror = () => reject(new Error('Could not load ' + src));
    document.head.appendChild(s);
  });
}

const Similar = {
  model: null, loading: null, failed: false,
  async loadModel() {
    if (this.model) return this.model;
    if (this.failed) return null;
    if (!this.loading) {
      this.loading = (async () => {
        try {
          if (!window.tf) await loadScript('vendor/tf.min.js');
          if (!window.mobilenet) await loadScript('vendor/mobilenet.min.js');
          this.model = await window.mobilenet.load({ version: 2, alpha: 1.0 });
          return this.model;
        } catch (e) {
          console.warn('AI model unavailable', e);
          this.failed = true;
          return null;
        }
      })();
    }
    return this.loading;
  },
  async embed(img) {
    const model = await this.loadModel();
    if (!model) return null;
    const t = model.infer(img, true);
    const data = await t.data();
    t.dispose();
    let norm = 0;
    for (const v of data) norm += v * v;
    norm = Math.sqrt(norm) || 1;
    return Array.from(data, (v) => v / norm);
  },
  hist(img) {
    const c = document.createElement('canvas');
    c.width = 48; c.height = 48;
    const g = c.getContext('2d', { willReadFrequently: true });
    g.drawImage(img, 0, 0, 48, 48);
    const d = g.getImageData(0, 0, 48, 48).data;
    const H = 12, S = 3, G = 4, bins = new Float32Array(H * S + G);
    for (let i = 0; i < d.length; i += 4) {
      const r = d[i] / 255, gr = d[i + 1] / 255, b = d[i + 2] / 255;
      const max = Math.max(r, gr, b), min = Math.min(r, gr, b), v = max, delta = max - min;
      const s = max === 0 ? 0 : delta / max;
      let idx;
      if (s < 0.18 || v < 0.15) idx = H * S + Math.min(G - 1, Math.floor(v * G));
      else {
        let h = 0;
        if (max === r) h = ((gr - b) / delta) % 6; else if (max === gr) h = (b - r) / delta + 2; else h = (r - gr) / delta + 4;
        h = (h * 60 + 360) % 360;
        idx = Math.min(H - 1, Math.floor(h / 360 * H)) * S + Math.min(S - 1, Math.floor((s - 0.18) / 0.82 * S));
      }
      bins[idx]++;
    }
    const total = 48 * 48;
    return Array.from(bins, (v) => v / total);
  },
  async features(img) { return { emb: await this.embed(img), hist: this.hist(img) }; },
  async featuresForPhoto(photoId) {
    const cached = await DB.get('features', photoId);
    if (cached && (cached.emb || this.failed)) return cached;
    const url = await Photos.url(photoId, 'full');
    if (!url) return null;
    const img = await loadImage(url);
    const f = { id: photoId, ...(await this.features(img)) };
    await DB.put('features', f);
    return f;
  },
  score(a, b) {
    let colour = 0;
    for (let i = 0; i < a.hist.length; i++) colour += Math.min(a.hist[i], b.hist[i]);
    if (a.emb && b.emb) {
      let dot = 0;
      for (let i = 0; i < a.emb.length; i++) dot += a.emb[i] * b.emb[i];
      return 0.8 * Math.max(0, dot) + 0.2 * colour;
    }
    return 0.6 * colour;
  },
};

function clearSearchPhoto() {
  const s = state.search;
  if (s.photoUrl) URL.revokeObjectURL(s.photoUrl);
  Object.assign(s, { photoBlob: null, photoUrl: null, results: null, progress: null, error: null, note: null });
}

async function flowSearchByPhoto() {
  const shot = await Camera.photo({ title: "Photo of what you're looking for", subtitle: 'Fill the frame with the object.', allowSkip: false, save: false });
  if (!shot || !shot.blob) return;
  clearSearchPhoto();
  const s = state.search;
  s.query = '';
  s.photoBlob = shot.blob;
  s.photoUrl = URL.createObjectURL(shot.blob);
  s.progress = { text: 'Loading the on-device AI… (first time needs internet)' };
  render();
  const token = s.photoUrl;
  try {
    const img = await loadImage(s.photoUrl);
    const query = await Similar.features(img);
    if (Similar.failed) s.note = "The AI model couldn't load (no internet?), so this search used colour matching only.";
    const withPhotos = Store.nodes.filter((n) => photoCount(n));
    const total = withPhotos.reduce((a, n) => a + photoCount(n), 0);
    const scored = [];
    let done = 0;
    for (const node of withPhotos) {
      // A node matches as well as its best-matching photo.
      let best = -1;
      for (const pid of photosOf(node)) {
        if (state.search.photoUrl !== token) return; // search was cleared
        s.progress = { done, total };
        if (state.screen === 'search') render();
        const f = await Similar.featuresForPhoto(pid);
        if (f) best = Math.max(best, Similar.score(query, f));
        done++;
        await sleep(0);
      }
      if (best >= 0) scored.push({ node, score: best });
    }
    s.results = scored.filter((r) => r.score >= 0.3).sort((a, b) => b.score - a.score).slice(0, 15);
    const best = s.results[0];
    Log.add('Photo search results', `${s.results.length} match(es)` + (best ? ` · best: “${best.node.name}” ${Math.round(best.score * 100)}%` : '') + (Similar.failed ? ' · colour only' : ''));
  } catch (e) {
    s.error = 'Photo search failed: ' + (e.message || e);
    Log.add('Photo search failed', e.message || String(e));
  }
  if (state.screen === 'search') render();
}

// ===========================================================================
// Backup
// ===========================================================================
function blobToDataUrl(blob) {
  return new Promise((resolve) => { const r = new FileReader(); r.onload = () => resolve(r.result); r.readAsDataURL(blob); });
}

async function exportBackup() {
  toast('Preparing backup…');
  const photos = [];
  for (const n of Store.nodes) {
    for (const pid of photosOf(n)) {
      const rec = await DB.get('photos', pid);
      if (rec) photos.push({ id: rec.id, full: await blobToDataUrl(rec.full) });
    }
  }
  const data = { app: 'StorageInventory', format: 1, version: APP_VERSION, exportedAt: Date.now(), nodes: Store.nodes, photos };
  const d = new Date();
  const name = `StorageInventory-backup-${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}.json`;
  const file = new File([JSON.stringify(data)], name, { type: 'application/json' });
  Log.add('Backup saved', `${data.nodes.length} entries · ${photos.length} photos · ${name}`);
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try { await navigator.share({ files: [file], title: 'Storage Inventory backup' }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(file);
  a.download = name;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 2000);
}

function importBackup() {
  const input = $('#file-backup');
  input.value = '';
  input.onchange = async () => {
    const file = input.files && input.files[0];
    if (!file) return;
    let data;
    try { data = JSON.parse(await file.text()); } catch (e) { return alertDialog("That file isn't a Storage Inventory backup."); }
    if (data.app !== 'StorageInventory' || !Array.isArray(data.nodes)) return alertDialog("That file isn't a Storage Inventory backup.");
    const ok = await confirmDialog({
      title: 'Restore backup?',
      text: `This replaces everything on this device with the backup from ${fmtDate(data.exportedAt)} (${plural(data.nodes.length, 'entry')}).`,
      ok: 'Restore', danger: true,
    });
    if (!ok) return;
    toast('Restoring…');
    await DB.clear('nodes'); await DB.clear('photos'); await DB.clear('features');
    Photos.urls.forEach((u) => URL.revokeObjectURL(u)); Photos.urls.clear();
    const idMap = new Map();
    for (const p of data.photos || []) {
      try {
        const blob = await (await fetch(p.full)).blob();
        idMap.set(p.id, await Photos.saveBlob(blob));
      } catch (e) { console.warn('Photo skipped', e); }
    }
    for (const n of data.nodes) {
      const ids = Array.isArray(n.photoIds) ? n.photoIds : (n.photoId ? [n.photoId] : []);
      const { photoId, ...rest } = n;
      await DB.put('nodes', { ...rest, photoIds: ids.map((id) => idMap.get(id)).filter(Boolean) });
    }
    await Store.load();
    Log.add('Backup restored', `${data.nodes.length} entries from ${fmtDate(data.exportedAt)}`);
    state.stack = []; state.screen = 'home';
    render();
    toast('Backup restored');
  };
  input.click();
}

// ===========================================================================
// Updates (service worker + version.json)
// ===========================================================================
let swReg = null;
const newer = (a, b) => {
  const pa = a.split('.').map(Number), pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const x = pa[i] || 0, y = pb[i] || 0;
    if (x !== y) return x > y;
  }
  return false;
};

async function fetchLatest() {
  const res = await fetch('version.json?t=' + Date.now(), { cache: 'no-store' });
  if (!res.ok) throw new Error('Server answered ' + res.status);
  return res.json();
}

async function checkForUpdates(silent = false) {
  if (!silent) { state.updateCheck = { status: 'checking' }; render(); }
  try {
    const info = await fetchLatest();
    Log.add(silent ? 'Automatic update check' : 'Checking for updates', `Installed ${APP_VERSION} · latest ${info.version}`);
    if (newer(info.version, APP_VERSION)) {
      state.update = info;
      state.updateCheck = { status: 'available', version: info.version, notes: info.notes };
    } else if (!silent) {
      state.updateCheck = { status: 'latest', version: info.version };
    }
  } catch (e) {
    Log.add('Update check failed', e.message || String(e));
    if (!silent) state.updateCheck = { status: 'error', message: "Couldn't check for updates. Are you online?" };
  }
  render();
}

async function applyUpdate() {
  Log.add('Installing update', state.update ? state.update.version : '');
  toast('Updating…');
  try {
    if (swReg) {
      await swReg.update();
      const waiting = swReg.waiting || swReg.installing;
      if (waiting) {
        navigator.serviceWorker.addEventListener('controllerchange', () => location.reload(), { once: true });
        const send = (w) => w.postMessage('skipWaiting');
        if (waiting.state === 'installed') send(waiting);
        else waiting.addEventListener('statechange', () => { if (waiting.state === 'installed') send(waiting); });
        setTimeout(() => location.reload(), 8000);
        return;
      }
    }
  } catch (e) { console.warn(e); }
  location.reload();
}

// ===========================================================================
// Excel export (.xlsx written directly: zip + spreadsheet XML)
// ===========================================================================
const Xlsx = (() => {
  const CRC = (() => {
    const t = new Uint32Array(256);
    for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; }
    return t;
  })();
  const crc32 = (bytes) => { let c = 0xFFFFFFFF; for (let i = 0; i < bytes.length; i++) c = CRC[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8); return (c ^ 0xFFFFFFFF) >>> 0; };

  /** Stored (uncompressed) zip archive from [{name, data: Uint8Array}]. */
  function zip(files) {
    const enc = new TextEncoder();
    const chunks = [], central = [];
    let offset = 0;
    const d = new Date();
    const time = (d.getHours() << 11) | (d.getMinutes() << 5) | (d.getSeconds() >> 1);
    const date = ((d.getFullYear() - 1980) << 9) | ((d.getMonth() + 1) << 5) | d.getDate();
    for (const f of files) {
      const name = enc.encode(f.name), data = f.data, crc = crc32(data);
      const local = new DataView(new ArrayBuffer(30));
      local.setUint32(0, 0x04034b50, true); local.setUint16(4, 20, true); local.setUint16(6, 0x0800, true);
      local.setUint16(8, 0, true); local.setUint16(10, time, true); local.setUint16(12, date, true);
      local.setUint32(14, crc, true); local.setUint32(18, data.length, true); local.setUint32(22, data.length, true);
      local.setUint16(26, name.length, true); local.setUint16(28, 0, true);
      chunks.push(new Uint8Array(local.buffer), name, data);
      const cen = new DataView(new ArrayBuffer(46));
      cen.setUint32(0, 0x02014b50, true); cen.setUint16(4, 20, true); cen.setUint16(6, 20, true); cen.setUint16(8, 0x0800, true);
      cen.setUint16(10, 0, true); cen.setUint16(12, time, true); cen.setUint16(14, date, true);
      cen.setUint32(16, crc, true); cen.setUint32(20, data.length, true); cen.setUint32(24, data.length, true);
      cen.setUint16(28, name.length, true); cen.setUint32(42, offset, true);
      central.push(new Uint8Array(cen.buffer), name);
      offset += 30 + name.length + data.length;
    }
    const cenSize = central.reduce((a, c) => a + c.length, 0);
    const end = new DataView(new ArrayBuffer(22));
    end.setUint32(0, 0x06054b50, true); end.setUint16(8, files.length, true); end.setUint16(10, files.length, true);
    end.setUint32(12, cenSize, true); end.setUint32(16, offset, true);
    return new Blob([...chunks, ...central, new Uint8Array(end.buffer)], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  }

  const x = (s) => String(s ?? '').replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  const col = (i) => { let n = i + 1, s = ''; while (n > 0) { const m = (n - 1) % 26; s = String.fromCharCode(65 + m) + s; n = Math.floor((n - 1) / 26); } return s; };
  const MAIN = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main';
  const REL = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships';
  const HEAD = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';

  function sheetXml(s) {
    const cols = Math.max(1, s.header.length, ...s.rows.map((r) => r.length));
    const row = (r, cells, header) => `<row r="${r}">` + cells.map((v, c) => {
      if (v == null || v === '') return '';
      const ref = col(c) + r, st = header ? ' s="1"' : '';
      if (typeof v === 'number') return `<c r="${ref}"${st}><v>${v}</v></c>`;
      return `<c r="${ref}"${st} t="inlineStr"><is><t xml:space="preserve">${x(v)}</t></is></c>`;
    }).join('') + '</row>';
    return HEAD + `<worksheet xmlns="${MAIN}" xmlns:r="${REL}">` +
      '<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>' +
      '<cols>' + Array.from({ length: cols }, (_, c) => `<col min="${c + 1}" max="${c + 1}" width="${s.widths[c] || 14}" customWidth="1"/>`).join('') + '</cols>' +
      '<sheetData>' + row(1, s.header, true) + s.rows.map((r, i) => row(i + 2, r, false)).join('') + '</sheetData>' +
      `<autoFilter ref="A1:${col(cols - 1)}${s.rows.length + 1}"/></worksheet>`;
  }

  const STYLES = HEAD + `<styleSheet xmlns="${MAIN}">` +
    '<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts>' +
    '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>' +
    '<fill><patternFill patternType="solid"><fgColor rgb="FF1F4E5F"/><bgColor indexed="64"/></patternFill></fill></fills>' +
    '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>' +
    '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>' +
    '<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>' +
    '<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs>' +
    '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>';

  /** sheets: [{ name, header: [...], rows: [[...]], widths: [...] }] → Blob */
  function build(sheets) {
    const enc = new TextEncoder();
    const n = sheets.length;
    const used = new Set();
    const names = sheets.map((s, i) => {
      let nm = (s.name || '').replace(/[\[\]:*?\/\\]/g, ' ').trim().slice(0, 31) || `Sheet${i + 1}`;
      while (used.has(nm.toLowerCase())) nm = nm.slice(0, 28) + '_' + (i + 1);
      used.add(nm.toLowerCase());
      return nm;
    });
    const files = [
      { name: '[Content_Types].xml', text: HEAD + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>' +
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
        sheets.map((_, i) => `<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`).join('') +
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>' },
      { name: '_rels/.rels', text: HEAD + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        `<Relationship Id="rId1" Type="${REL}/officeDocument" Target="xl/workbook.xml"/></Relationships>` },
      { name: 'xl/workbook.xml', text: HEAD + `<workbook xmlns="${MAIN}" xmlns:r="${REL}"><sheets>` +
        names.map((nm, i) => `<sheet name="${x(nm)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>`).join('') + '</sheets></workbook>' },
      { name: 'xl/_rels/workbook.xml.rels', text: HEAD + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        sheets.map((_, i) => `<Relationship Id="rId${i + 1}" Type="${REL}/worksheet" Target="worksheets/sheet${i + 1}.xml"/>`).join('') +
        `<Relationship Id="rId${n + 1}" Type="${REL}/styles" Target="styles.xml"/></Relationships>` },
      { name: 'xl/styles.xml', text: STYLES },
      ...sheets.map((s, i) => ({ name: `xl/worksheets/sheet${i + 1}.xml`, text: sheetXml(s) })),
    ];
    return zip(files.map((f) => ({ name: f.name, data: enc.encode(f.text) })));
  }
  return { build };
})();

function excelFileName(base) {
  const d = new Date();
  const clean = (base || 'Export').replace(/[^\p{L}\p{N} _.-]/gu, '').trim().replace(/ /g, '_').slice(0, 40) || 'Export';
  return `${clean}_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}.xlsx`;
}

async function saveOrShare(blob, name, title) {
  const file = new File([blob], name, { type: blob.type });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try { await navigator.share({ files: [file], title }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(file);
  a.download = name;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 3000);
}

function storageWorkbook(root) {
  const rows = [];
  const walk = (parent, level) => {
    for (const child of Store.children(parent.id)) {
      rows.push([
        rows.length + 1, child.name, typeLabel(child), Store.location(child.id), parent.name, level,
        child.qr || '(none)', fmtDate(child.createdAt), photoCount(child),
        canHold(child) ? countText(Store.countInside(child.id)) : '',
      ]);
      if (canHold(child)) walk(child, level + 1);
    }
  };
  walk(root, 1);
  const c = Store.countInside(root.id);
  return [
    { name: 'Contents', header: ['#', 'Name', 'Type', 'Location', 'Directly inside', 'Level', 'QR code', 'Added', 'Photos', 'Contents'],
      rows, widths: [6, 30, 18, 40, 24, 7, 22, 21, 8, 26] },
    { name: 'Summary', header: ['Field', 'Value'], widths: [20, 50], rows: [
      ['Name', root.name], ['Type', typeLabel(root)], ['Location', Store.location(root.id) || 'Top level'],
      ['QR code', root.qr || '(none)'], ['Added', fmtDate(root.createdAt)],
      ['Items inside', c.items], ['Containers inside', c.containers],
      ['Exported', fmtDate(Date.now())], ['App version', `${APP_VERSION} (web)`],
    ] },
  ];
}

async function exportStorageExcel(node) {
  Log.add('Export to Excel started', `“${node.name}”`);
  try {
    const sheets = storageWorkbook(node);
    const name = excelFileName(node.name);
    Log.add('Exported to Excel', `“${node.name}” · ${sheets[0].rows.length} rows · ${name}`);
    await saveOrShare(Xlsx.build(sheets), name, `Storage Inventory – ${node.name}`);
  } catch (e) {
    Log.add('Export failed', `“${node.name}” · ${e.message || e}`);
    alertDialog("Couldn't create the Excel file: " + (e.message || e));
  }
}

async function exportLogExcel() {
  Log.add('Activity log export started');
  try {
    await sleep(50);
    const entries = await DB.all('log');
    const sheets = [
      { name: 'Activity log', header: ['#', 'Date & time', 'Event', 'Details', 'Screen', 'App version'], widths: [7, 21, 26, 70, 26, 12],
        rows: entries.map((e, i) => [i + 1, fmtDate(e.t), e.e, e.d, e.s, e.v]) },
      { name: 'Device', header: ['Field', 'Value'], widths: [16, 60], rows: [
        ['App version', `${APP_VERSION} (web)`],
        ['Mode', (navigator.standalone || matchMedia('(display-mode: standalone)').matches) ? 'Home Screen app' : 'Browser'],
        ['Browser', navigator.userAgent],
        ['Entries', entries.length],
        ['First entry', entries[0] ? fmtDate(entries[0].t) : '-'],
        ['Last entry', entries.length ? fmtDate(entries[entries.length - 1].t) : '-'],
        ['Exported', fmtDate(Date.now())],
      ] },
    ];
    const name = excelFileName('StorageInventory_log');
    await saveOrShare(Xlsx.build(sheets), name, 'Storage Inventory activity log');
    Log.add('Activity log exported', `${entries.length} entries · ${name}`);
  } catch (e) {
    Log.add('Activity log export failed', e.message || String(e));
    alertDialog("Couldn't create the log file: " + (e.message || e));
  }
}

// ===========================================================================
// Events
// ===========================================================================
document.addEventListener('click', (e) => {
  const t = e.target;
  const menuBtn = t.closest('[data-menu]');
  if (menuBtn) {
    e.stopPropagation();
    const node = Store.get(menuBtn.dataset.menu);
    if (node) nodeMenu(menuBtn, node);
    return;
  }
  const viewNode = t.closest('#app [data-view-node]');
  if (viewNode) {
    const n = Store.get(viewNode.dataset.viewNode);
    if (n) photoViewer(photosOf(n), Number(viewNode.dataset.index || 0), n.id);
    return;
  }
  const open = t.closest('#app [data-open]');
  if (open) {
    const node = Store.get(open.dataset.open);
    if (!node) return;
    if (state.screen === 'search') {
      Log.add('Opened search result', `“${node.name}” (${state.search.photoUrl ? 'photo' : 'name'} search)`);
      clearSearchPhoto();
      return openNode(node);
    }
    if (canHold(node)) { state.stack.push(node.id); go('detail'); } else itemDialog(node);
    return;
  }
  const act = t.closest('[data-act]');
  if (!act || !act.closest('#app')) return;
  const a = act.dataset.act;
  if (a === 'back') back();
  else if (a === 'search') go('search');
  else if (a === 'settings') { state.updateCheck = null; go('settings'); }
  else if (a === 'add-menu') {
    showMenu(act, [
      { label: 'Take photo to add to existing storage', disabled: !Store.nodes.some(canHold), onClick: () => flowAddByPhoto() },
      { label: 'Add new storage (scan QR code)', onClick: () => flowNewStorage() },
      { label: 'Create temporary storage', onClick: () => flowTemporaryStorage() },
    ]);
  } else if (a === 'detail-add') {
    const id = state.stack[state.stack.length - 1];
    showMenu(act, [
      { label: 'Add by photo', onClick: () => flowAddByPhoto(id) },
      { label: 'Add with QR code + photo', onClick: () => flowAddChildByQr(id) },
    ]);
  } else if (a === 'photo-node') { const n = Store.get(act.dataset.id); if (n) flowAddPhoto(n); }
  else if (a === 'search-photo') flowSearchByPhoto();
  else if (a === 'search-qr') flowFindByQr();
  else if (a === 'clear-photo') { clearSearchPhoto(); render(); }
  else if (a === 'check-update') checkForUpdates(false);
  else if (a === 'apply-update') applyUpdate();
  else if (a === 'export') exportBackup();
  else if (a === 'export-log') exportLogExcel();
  else if (a === 'import') importBackup();
  else if (a === 'dismiss-install') { try { localStorage.setItem('installTipDismissed', '1'); } catch (err) { /* ignore */ } render(); }
});

window.addEventListener('resize', () => { if (!Camera.el.hidden) Camera.drawFinder(Camera.mode === 'scan'); });

// ===========================================================================
// Start
// ===========================================================================
(async function start() {
  Camera.init();
  try {
    await DB.open();
    await Store.load();
  } catch (e) {
    $('#app').innerHTML = `<div class="empty"><h2>Can't open storage</h2><p>${esc(e.message || e)}. If you're in a Private tab, open the app in a normal Safari tab.</p></div>`;
    return;
  }
  const standalone = navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  Log.add('App opened', `Version ${APP_VERSION} (web) · ${standalone ? 'Home Screen app' : 'browser'} · ${navigator.userAgent}`);
  Log.trim().catch(() => {});
  render();
  if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
  if ('serviceWorker' in navigator) {
    try { swReg = await navigator.serviceWorker.register('sw.js'); } catch (e) { console.warn('SW failed', e); }
  }
  checkForUpdates(true);
})();
