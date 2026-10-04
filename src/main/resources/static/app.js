const state = {me: null, books: [], book: null, authMode: 'login'};

const el = id => document.getElementById(id);

async function api(path, options = {}) {
    const method = options.method || 'GET';
    if (method !== 'GET') {
        const csrf = await fetch('/api/auth/csrf').then(r => r.json());
        options.headers = Object.assign({'X-CSRF-TOKEN': csrf.token}, options.headers || {});
    }
    const response = await fetch(path, options);
    if (path === '/api/auth/me' && response.status === 401) {
        return null;
    }
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'Ошибка запроса (' + response.status + ')');
    }
    return response.json().catch(() => null);
}

function sizeText(bytes) {
    return bytes > 1048576 ? (bytes / 1048576).toFixed(1) + ' МБ' : Math.ceil(bytes / 1024) + ' КБ';
}

function renderSession() {
    el('who').textContent = state.me ? state.me.username : 'Гость';
    el('login-open').hidden = !!state.me;
    el('logout').hidden = !state.me;
    el('upload-open').hidden = !state.me;
}

function renderBooks() {
    el('empty').hidden = state.books.length > 0;
    el('books').innerHTML = '';
    state.books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `<div><div class="title"></div><div class="meta"></div></div><div class="likes"></div>`;
        card.querySelector('.title').textContent = book.title;
        card.querySelector('.meta').textContent = `${book.author} · ${book.format.toUpperCase()} · ${sizeText(book.size)}`;
        card.querySelector('.likes').textContent = `Нравится: ${book.likes}`;
        card.onclick = () => openBook(book.id);
        el('books').appendChild(card);
    });
}

function query() {
    const title = el('search-input').value.trim();
    return title ? '?title=' + encodeURIComponent(title) : '';
}

function renderBook() {
    const book = state.book;
    el('list-view').hidden = true;
    el('book-view').hidden = false;
    el('book-title').textContent = book.title;
    el('book-author').textContent = book.author;
    el('book-description').textContent = book.description || '';
    el('book-description').hidden = !book.description;
    el('book-meta').textContent = `Формат: ${book.format.toUpperCase()}, размер: ${sizeText(book.size)}`;
    el('like-count').textContent = book.likes;
    el('like').classList.toggle('liked', book.liked);
    el('like').disabled = !state.me;
    el('delete').hidden = !state.me || !state.me.moderator;
}

async function loadBooks() {
    state.books = await api('/api/books' + query()) || [];
    el('list-view').hidden = false;
    el('book-view').hidden = true;
    renderBooks();
}

async function refreshBooks() {
    state.books = await api('/api/books' + query()) || [];
    renderBooks();
}

async function openBook(id) {
    state.book = await api('/api/books/' + id);
    renderBook();
}

async function loadSession() {
    state.me = await api('/api/auth/me');
    renderSession();
}

function json(method, body) {
    return {method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)};
}

el('search-form').onsubmit = event => {
    event.preventDefault();
    loadBooks();
};

el('back').onclick = () => loadBooks();

el('download').onclick = () => {
    window.location = '/api/books/' + state.book.id + '/download';
};

el('like').onclick = async () => {
    try {
        state.book = await api('/api/books/' + state.book.id + '/like', {method: 'PUT'});
        renderBook();
        refreshBooks();
    } catch (e) {
        alert(e.message);
    }
};

el('delete').onclick = async () => {
    if (!confirm('Удалить книгу из общего доступа?')) {
        return;
    }
    try {
        await api('/api/moderation/books/' + state.book.id, {method: 'DELETE'});
        state.book = null;
        await loadBooks();
    } catch (e) {
        alert(e.message);
    }
};

el('login-open').onclick = () => openAuth('login');

el('auth-toggle').onclick = () => openAuth(state.authMode === 'login' ? 'register' : 'login');

el('auth-cancel').onclick = () => el('auth-dialog').close();

el('logout').onclick = async () => {
    await api('/api/auth/logout', {method: 'POST'});
    state.me = null;
    state.book = null;
    renderSession();
    await loadBooks();
};

el('auth-form').onsubmit = async event => {
    event.preventDefault();
    el('auth-error').textContent = '';
    try {
        const body = {username: el('auth-username').value.trim(), password: el('auth-password').value};
        state.me = await api('/api/auth/' + state.authMode, json('POST', body));
        el('auth-dialog').close();
        renderSession();
    } catch (e) {
        el('auth-error').textContent = e.message;
    }
};

function openAuth(mode) {
    state.authMode = mode;
    el('auth-title').textContent = mode === 'login' ? 'Вход' : 'Регистрация';
    el('auth-submit').textContent = mode === 'login' ? 'Войти' : 'Создать аккаунт';
    el('auth-toggle').textContent = mode === 'login' ? 'Регистрация' : 'Вход';
    el('auth-hint').hidden = mode === 'login';
    el('auth-error').textContent = '';
    el('auth-dialog').showModal();
}

el('upload-open').onclick = () => {
    el('upload-error').textContent = '';
    el('upload-dialog').showModal();
};

el('upload-cancel').onclick = () => el('upload-dialog').close();

el('upload-form').onsubmit = async event => {
    event.preventDefault();
    el('upload-error').textContent = '';
    const form = new FormData();
    form.append('title', el('upload-title').value.trim());
    form.append('author', el('upload-author').value.trim());
    form.append('description', el('upload-description').value.trim());
    form.append('file', el('upload-file').files[0]);
    try {
        await api('/api/books', {method: 'POST', body: form});
        el('upload-dialog').close();
        el('upload-form').reset();
        el('search-input').value = '';
        await loadBooks();
    } catch (e) {
        el('upload-error').textContent = e.message;
    }
};

loadSession().then(() => loadBooks());
