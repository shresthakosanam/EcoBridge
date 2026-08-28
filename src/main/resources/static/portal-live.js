const one = (selector, context = document) => context.querySelector(selector);
const all = (selector, context = document) => [...context.querySelectorAll(selector)];
const privatePages = ['/dashboard.html', '/pickup.html', '/feed.html'];
const initials = name => (name || 'EcoBridge Member').trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();

function applyCurrentUser(user) {
    all('.profile').forEach(profile => {
        one('.avatar', profile).textContent = initials(user.name);
        one('b', profile).textContent = user.name;
        one('small', profile).textContent = user.email;
    });
    const composerAvatar = one('.composer .avatar');
    if (composerAvatar) composerAvatar.textContent = initials(user.name);
    const greeting = one('.welcome h1');
    if (greeting) greeting.textContent = `Green morning, ${user.name.split(/\s+/)[0]}.`;
}

async function currentUser() {
    const response = await fetch('/api/auth/me', {credentials: 'same-origin'});
    if (response.status === 401) {
        if (privatePages.includes(location.pathname)) location.replace('/login.html');
        return null;
    }
    if (!response.ok) throw new Error('Could not load your profile.');
    const user = await response.json();
    applyCurrentUser(user);
    return user;
}

function empty(container, title, message) {
    container.innerHTML = `<div class="dashboard-empty"><b>${title}</b><p>${message}</p></div>`;
}

async function dashboard(user) {
    if (location.pathname !== '/dashboard.html' || !user) return;
    const response = await fetch('/api/dashboard');
    if (response.status === 401) return location.replace('/login.html');
    if (!response.ok) return window.ecoToast('Could not load dashboard.', true);
    const data = await response.json();
    const values = [data.stats.pickupCount, `${data.stats.wasteDivertedKg}<small> kg</small>`, data.stats.eventsJoined, data.stats.ecoPoints];
    all('.metric-grid .metric strong').forEach((element, index) => element.innerHTML = values[index]);
    all('.metric-grid .metric .trend').forEach(element => element.textContent = 'Live data');
    const pickup = one('.pickup-card');
    if (!data.latestPickup) empty(pickup, 'No pickup requests yet', 'Your impact starts with your first action.');
    else {
        const p = data.latestPickup;
        pickup.innerHTML = `<span class="status-badge">● ${p.status.toUpperCase()}</span><h3>${p.wasteType}</h3><p>Pickup request #ECO-${p.id}</p><div class="pickup-info"><div><small>PICKUP DATE</small><b>${p.preferredDate}</b></div><div><small>TIME WINDOW</small><b>${p.preferredTime}</b></div><div><small>EST. WEIGHT</small><b>${p.quantity} kilograms</b></div></div>`;
    }
    const panels = all('.dashboard-grid aside .panel');
    if (panels[0]) empty(panels[0], data.upcomingEvents.length ? 'Your upcoming events' : 'Join your first eco event', data.upcomingEvents.length ? data.upcomingEvents.map(e => `${e.name} · ${e.date}`).join('<br>') : 'Community events you join will appear here.');
    if (panels[1]) empty(panels[1], 'Recent activity', data.recentActivity.length ? data.recentActivity.map(a => a.text).join('<br>') : 'Your impact starts with your first action.');
}

currentUser().then(user => { window.ecoUser = user; return dashboard(user); }).catch(error => window.ecoToast?.(error.message, true));
