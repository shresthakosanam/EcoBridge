const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
const icon = name => `<i data-lucide="${name}"></i>`;
const pending = document.querySelector('#pending');
const reviewed = document.querySelector('#reviewed');
const message = document.querySelector('#message');
let csrfToken;

function notice(text, error = false) {
    message.textContent = text;
    message.classList.toggle('error', error);
}

function label(value) {
    return String(value ?? '').replace(/^ROLE_/, '').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, char => char.toUpperCase());
}

function date(value) {
    return value ? new Date(value).toLocaleString([], {dateStyle: 'medium', timeStyle: 'short'}) : '—';
}

function empty(text) {
    return `<div class="empty">${escapeHtml(text)}</div>`;
}

async function api(url, options = {}) {
    const response = await fetch(url, {credentials: 'same-origin', ...options});
    const body = await response.json().catch(() => ({}));
    if (response.status === 401) {
        location.href = '/login.html';
        throw new Error('Please sign in again.');
    }
    if (!response.ok) throw new Error(body.message || body.detail || `Request failed (${response.status})`);
    return body;
}

async function csrf() {
    if (!csrfToken) csrfToken = (await api('/api/admin/csrf')).token;
    return csrfToken;
}

function applicationCard(application) {
    const isPending = application.status === 'PENDING';
    const vehicle = [application.vehicleType, application.vehicleNumber].filter(Boolean).join(' · ');
    return `<article class="application"><div><h3>${escapeHtml(application.applicantName)}</h3>
        <p>${escapeHtml(application.email)}</p><div class="application-meta"><p>Phone: ${escapeHtml(application.phone)}</p>
        <p>Area: ${escapeHtml(application.serviceArea)}</p>${vehicle ? `<p>Vehicle: ${escapeHtml(vehicle)}</p>` : ''}<p>Applied: ${escapeHtml(date(application.submittedAt))}</p></div></div>
        <div class="application-actions">${isPending ? `<button class="primary" data-id="${application.id}" data-action="approve">Approve</button>
        <button class="ghost reject" data-id="${application.id}" data-action="reject">Reject</button>` :
        `<span class="status-badge">${escapeHtml(label(application.status))}</span>`}</div></article>`;
}

function renderMetrics(metrics) {
    const cards = [
        ['users', 'Users', 'users'], ['collectors', 'Approved collectors', 'badge-check'],
        ['pendingCollectors', 'Pending approvals', 'clock-3'], ['pickups', 'Pickup requests', 'truck'],
        ['openPickups', 'Open pickups', 'package-open'], ['events', 'Events', 'calendar-days'], ['posts', 'Feed posts', 'messages-square']
    ];
    document.querySelector('#metrics').innerHTML = cards.map(([key, title, glyph]) =>
        `<article class="admin-metric"><span>${icon(glyph)}</span><strong>${Number(metrics[key] || 0)}</strong><small>${title}</small></article>`).join('');
}

function renderPickups(items) {
    document.querySelector('#pickupCount').textContent = `${items.length} requests`;
    document.querySelector('#pickups').innerHTML = items.length ? `<table class="admin-table"><thead><tr><th>Request</th><th>Member</th><th>Material</th><th>Schedule</th><th>Collector</th><th>Status</th></tr></thead><tbody>${items.map(item => `<tr>
        <td><b>#ECO-${item.id}</b><small>${escapeHtml(item.address)}</small></td>
        <td>${escapeHtml(item.requester)}</td><td>${escapeHtml(item.wasteType)} · ${item.quantity} kg</td>
        <td>${escapeHtml(item.preferredDate)}<small>${escapeHtml(item.preferredTime || 'Any time')}</small></td>
        <td>${escapeHtml(item.collector || 'Waiting for collector')}</td><td><span class="status-badge">${escapeHtml(label(item.status))}</span></td>
        </tr>`).join('')}</tbody></table>` : empty('No pickup requests have been submitted yet.');
}

function renderUsers(items) {
    document.querySelector('#userCount').textContent = `${items.length} accounts`;
    document.querySelector('#users').innerHTML = items.length ? `<table class="admin-table"><thead><tr><th>Account</th><th>Email</th><th>Access</th><th>Status</th><th>Joined</th></tr></thead><tbody>${items.map(item => `<tr>
        <td><b>${escapeHtml(item.name)}</b><small>User #${item.id}</small></td><td>${escapeHtml(item.email)}</td>
        <td>${escapeHtml(label(item.role))}</td><td><span class="status-badge">${escapeHtml(label(item.status))}</span></td><td>${escapeHtml(date(item.joinedAt))}</td>
        </tr>`).join('')}</tbody></table>` : empty('No accounts found.');
}

function renderActivity(overview) {
    document.querySelector('#events').innerHTML = overview.events.length ? overview.events.map(item =>
        `<article class="activity-card"><span>${icon('calendar-days')}</span><div><h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.date)} · ${escapeHtml(item.time || 'Time not set')} · ${escapeHtml(item.location)}</p><small>Hosted by ${escapeHtml(item.organizer)}</small></div></article>`).join('') : empty('No events published yet.');
    document.querySelector('#posts').innerHTML = overview.posts.length ? overview.posts.map(item =>
        `<article class="activity-card"><span>${icon('message-square')}</span><div><h3>${escapeHtml(item.author)}</h3><p>${escapeHtml(item.caption || item.activity || 'Shared an environmental action')}</p><small>${escapeHtml(date(item.createdAt))}</small></div></article>`).join('') : empty('No feed posts shared yet.');
}

async function load() {
    try {
        notice('');
        const user = await api('/api/auth/me');
        if (user.role !== 'ROLE_ADMIN') throw new Error('Admin access is required. Sign out and sign in again if your role was just changed.');
        const [overview, applications] = await Promise.all([api('/api/admin/overview'), api('/api/admin/collectors')]);
        const waiting = applications.filter(item => item.status === 'PENDING');
        const completed = applications.filter(item => item.status !== 'PENDING');
        pending.innerHTML = waiting.length ? waiting.map(applicationCard).join('') : empty('No applications waiting for approval. A member must open Collector Portal and apply first.');
        reviewed.innerHTML = completed.length ? completed.map(applicationCard).join('') : empty('No applications have been reviewed yet.');
        document.querySelector('#pendingCount').textContent = `${waiting.length} pending`;
        renderMetrics(overview.metrics);
        renderPickups(overview.pickups);
        renderUsers(overview.users);
        renderActivity(overview);
        lucide.createIcons();
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
        notice(`Application ${action === 'approve' ? 'approved' : 'rejected'}. The applicant must sign out and sign in again.`);
        await load();
    } catch (error) {
        notice(error.message, true);
        button.disabled = false;
    }
});

lucide.createIcons();
load();
