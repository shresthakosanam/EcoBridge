const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
const pending = document.querySelector('#pending');
const reviewed = document.querySelector('#reviewed');
const message = document.querySelector('#message');
let csrfToken;

function notice(text, error = false) {
    message.textContent = text;
    message.classList.toggle('error', error);
}

async function api(url, options = {}) {
    const response = await fetch(url, {credentials: 'same-origin', ...options});
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.message || body.detail || `Request failed (${response.status})`);
    return body;
}

async function csrf() {
    if (!csrfToken) csrfToken = (await api('/api/admin/csrf')).token;
    return csrfToken;
}

function card(application) {
    const isPending = application.status === 'PENDING';
    return `<article class="application"><div><h3>${escapeHtml(application.applicantName)}</h3>
        <p>${escapeHtml(application.email)}</p><div class="application-meta"><p>Phone: ${escapeHtml(application.phone)}</p>
        <p>Area: ${escapeHtml(application.serviceArea)}</p><p>Applied: ${escapeHtml(new Date(application.submittedAt).toLocaleDateString())}</p></div></div>
        <div class="application-actions">${isPending ? `<button class="primary" data-id="${application.id}" data-action="approve">Approve</button>
        <button class="ghost reject" data-id="${application.id}" data-action="reject">Reject</button>` :
        `<span class="status-badge">${escapeHtml(application.status)}</span>`}</div></article>`;
}

async function load() {
    try {
        const user = await api('/api/auth/me');
        if (user.role !== 'ROLE_ADMIN') throw new Error('Admin access is required. Sign out and sign in again if your role was just changed.');
        const applications = await api('/api/admin/collectors');
        const waiting = applications.filter(item => item.status === 'PENDING');
        pending.innerHTML = waiting.length ? waiting.map(card).join('') : '<div class="empty">No applications waiting for approval.</div>';
        reviewed.innerHTML = applications.some(item => item.status !== 'PENDING') ? applications.filter(item => item.status !== 'PENDING').map(card).join('') : '<div class="empty">No reviewed applications yet.</div>';
        document.querySelector('#pendingCount').textContent = `${waiting.length} pending`;
    } catch (error) {
        notice(error.message, true);
    }
}

document.querySelector('#refresh').addEventListener('click', load);
document.addEventListener('click', async event => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const action = button.dataset.action;
    if (!confirm(`${action === 'approve' ? 'Approve' : 'Reject'} this collector application?`)) return;
    button.disabled = true;
    try {
        await api(`/api/admin/collectors/${button.dataset.id}/${action}`, {method: 'POST', headers: {'X-CSRF-TOKEN': await csrf()}});
        notice(`Application ${action === 'approve' ? 'approved' : 'rejected'}. Ask the applicant to sign out and sign in again.`);
        await load();
    } catch (error) {
        notice(error.message, true);
        button.disabled = false;
    }
});
lucide.createIcons();
load();
