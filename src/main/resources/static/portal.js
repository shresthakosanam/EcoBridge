document.head.insertAdjacentHTML('beforeend','<link rel="stylesheet" href="/portal-restored.css">');
const page = document.body.dataset.page;
const title = document.body.dataset.title;
const $ = (selector, context = document) => context.querySelector(selector);
const $$ = (selector, context = document) => [...context.querySelectorAll(selector)];
const icon = (name, label = '') => `<i data-lucide="${name}"${label ? ` aria-label="${label}"` : ''}></i>`;
const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
const initials = name => (name || 'EcoBridge Member').trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();

const nav = [
    ['home', '/', 'globe-2', 'Home'],
    ['dashboard', '/dashboard.html', 'home', 'Dashboard'],
    ['pickup', '/pickup.html', 'recycle', 'Eco Pickup'], ['events', '/events.html', 'calendar-days', 'Eco Events'],
    ['feed', '/feed.html', 'message-square', 'Eco Feed'], ['about', '/about.html', 'info', 'About Us'],
    ['collector', '/collector.html', 'truck', 'Collector Portal']
];

function logo() {
    return `<a class="brand" href="/" aria-label="EcoBridge home"><span class="brand-icon">${icon('leaf')}</span><span><span class="brand-eco">Eco</span><span class="brand-bridge">Bridge</span></span></a>`;
}

function shell() {
    $('#app').innerHTML = `<div class="mobile-top">${logo()}<button class="icon-btn" data-menu aria-label="Open navigation">${icon('menu')}</button></div><div class="mobile-drawer-backdrop"></div><div class="shell"><aside class="sidebar">${logo()}<div class="nav-label">YOUR ECOBRIDGE</div><nav class="side-nav">${nav.map(([key, href, ico, text]) => `<a href="${href}" class="${key === page ? 'active' : ''}">${icon(ico)}${text}</a>`).join('')}</nav><div class="profile"><span class="avatar">--</span><div><b>Loading profile</b><small>Checking session</small></div><button class="logout" aria-label="Sign out" title="Sign out">${icon('log-out')}</button></div></aside><main class="content"><header class="topbar"><div class="crumb">EcoBridge / <b>${escapeHtml(title)}</b></div><div class="top-actions"><div class="search-wrap">${icon('search')}<input class="search" aria-label="Search ${escapeHtml(title)}" placeholder="Search EcoBridge..."></div><button class="icon-btn" data-theme-toggle aria-label="Switch to dark mode">${icon('moon')}</button>${page === 'dashboard' ? `<a class="primary" href="/pickup.html">${icon('plus')}New pickup</a>` : ''}</div></header><div class="page" id="pageContent"></div></main></div><div class="toast" role="status" aria-live="polite"></div>`;
    lucide.createIcons();
}

function toast(message, error = false) {
    const element = $('.toast');
    element.textContent = message;
    element.classList.toggle('error', error);
    element.classList.add('show');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => element.classList.remove('show'), 3500);
}

async function api(url, options = {}) {
    if (url === '/api/pickups' && options.method === 'POST') {
        options.headers = {...options.headers, 'Idempotency-Key': crypto.randomUUID()};
    }
    const response = await fetch(url, {credentials: 'same-origin', ...options});
    if (response.status === 401) {
        if (!['about', 'events'].includes(page)) location.replace('/login.html');
        throw new Error('Please sign in to continue');
    }
    const data = response.status === 204 ? null : await response.json().catch(() => ({}));
    if (!response.ok) {
        const fallback = response.status === 403
            ? 'Your session cannot complete this action. Sign out, sign in again, and retry.'
            : `Request failed (${response.status})`;
        throw new Error(data.message || data.detail || fallback);
    }
    return data;
}

async function uploadImage(file, purpose) {
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) throw new Error('Only JPG, PNG and WebP images are supported');
    if (file.size > 5 * 1024 * 1024) throw new Error('Image must be 5 MB or smaller');
    const form = new FormData(); form.append('file', file); form.append('purpose', purpose);
    return api('/api/uploads', {method: 'POST', body: form});
}

function applyUser(user) {
    const avatar = $('.profile .avatar');
    avatar.innerHTML = user.avatarUrl ? `<img src="${escapeHtml(user.avatarUrl)}" alt="">` : initials(user.name);
    $('.profile b').textContent = user.name;
    $('.profile small').textContent = 'Eco Member';
    if (user.role === 'ROLE_ADMIN' && !$('.side-nav a[href="/admin.html"]')) {
        $('.side-nav').insertAdjacentHTML('beforeend', `<a href="/admin.html">${icon('shield-check')}Collector approvals</a>`);
        lucide.createIcons();
    }
    const composer = $('.composer .avatar'); if (composer) composer.textContent = initials(user.name);
}

function bindShell() {
    const root = document.documentElement;
    const preference = localStorage.getItem('ecobridge-theme') || 'light';
    root.dataset.theme = preference;
    const themeButton = $('[data-theme-toggle]');
    const updateTheme = () => {
        const dark = root.dataset.theme === 'dark';
        themeButton.innerHTML = icon(dark ? 'sun' : 'moon');
        themeButton.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
        lucide.createIcons();
    };
    themeButton.onclick = () => { root.dataset.theme = root.dataset.theme === 'dark' ? 'light' : 'dark'; localStorage.setItem('ecobridge-theme', root.dataset.theme); updateTheme(); };
    updateTheme();
    $('[data-menu]').onclick = () => { $('.sidebar').classList.add('open'); $('.mobile-drawer-backdrop').classList.add('open'); };
    $('.mobile-drawer-backdrop').onclick = () => { $('.sidebar').classList.remove('open'); $('.mobile-drawer-backdrop').classList.remove('open'); };
    $('.logout').onclick = async () => { await fetch('/logout', {method:'POST', credentials:'same-origin'}); location.href='/'; };
}

async function loadUser() {
    try { const user = await api('/api/auth/me'); applyUser(user); return user; }
    catch (error) { if (['about', 'events'].includes(page)) { $('.profile b').textContent='EcoBridge visitor'; $('.profile small').textContent='Sign in to participate'; const authButton=$('.logout'); authButton.innerHTML=icon('log-in'); authButton.setAttribute('aria-label','Sign in'); authButton.title='Sign in'; authButton.onclick=()=>location.href='/login.html'; lucide.createIcons(); return null; } throw error; }
}

const empty = (title, text) => `<div class="empty"><b>${escapeHtml(title)}</b>${escapeHtml(text)}</div>`;

function dashboardView() {
    $('#pageContent').innerHTML = `<section class="welcome"><div><div class="eyebrow" data-date></div><h1>Green morning.</h1><p>Here’s the difference your actions are making.</p></div><div class="date-chip">${icon('leaf')}Your live impact</div></section><section class="metric-grid" data-metrics></section><section class="dashboard-grid"><div><article class="panel"><header class="panel-head"><h2>Upcoming collection</h2><a href="/pickup.html">View all ${icon('arrow-right')}</a></header><div data-upcoming>${empty('Loading', 'Fetching your pickup requests...')}</div></article><article class="panel impact-panel" style="margin-top:16px"><header class="panel-head"><h2>Your impact trend</h2><span class="live-label">LIVE DATA</span></header><div class="impact-chart" aria-label="Your environmental impact"><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="chart-labels"><span>Start</span><span>Reuse</span><span>Recycle</span><span>Join</span><span>Share</span><span>Grow</span></div></article></div><aside><article class="panel"><header class="panel-head"><h2>Your events</h2><a href="/events.html">Explore</a></header><div data-events></div></article><article class="panel" style="margin-top:16px"><header class="panel-head"><h2>Recent activity</h2></header><div data-activity></div></article></aside></section>`;
    lucide.createIcons();
}

async function loadDashboard(user) {
    const data = await api('/api/dashboard');
    $('.welcome h1').textContent = `Green morning, ${user.name.split(/\s+/)[0]}.`;
    $('[data-date]').textContent = new Intl.DateTimeFormat('en-IN',{weekday:'long',day:'2-digit',month:'long'}).format(new Date()).toUpperCase();
    const cards = [['package-check',data.stats.pickupCount,'Collection requests'],['recycle',`${data.stats.wasteDivertedKg} kg`,'Waste diverted'],['calendar-check',data.stats.eventsJoined,'Events joined'],['award',data.stats.ecoPoints,'Eco points earned']];
    $('[data-metrics]').innerHTML = cards.map(([ico,value,label]) => `<article class="metric"><span class="metric-icon">${icon(ico)}</span><strong>${escapeHtml(value)}</strong><small>${label}</small></article>`).join('');
    const p=data.latestPickup; $('[data-upcoming]').innerHTML=p?`<div class="pickup-card"><span class="status-badge">${escapeHtml(p.status)}</span><h3>${escapeHtml(p.wasteType)}</h3><p>Request #ECO-${p.id}</p><div class="pickup-info"><div><small>PICKUP DATE</small><b>${escapeHtml(p.preferredDate)}</b></div><div><small>TIME WINDOW</small><b>${escapeHtml(p.preferredTime)}</b></div><div><small>WEIGHT</small><b>${p.quantity} kg</b></div></div></div>`:empty('No pickup requests yet','Your impact starts with your first action.');
    $('[data-events]').innerHTML=data.upcomingEvents.length?data.upcomingEvents.map(e=>`<div class="pulse-row"><span>${escapeHtml(e.name)}</span><b>${escapeHtml(e.date)}</b></div>`).join(''):empty('Join your first eco event','Registered events will appear here.');
    $('[data-activity]').innerHTML=data.recentActivity.length?data.recentActivity.map(a=>`<div class="pulse-row"><span>${escapeHtml(a.text)}</span></div>`).join(''):empty('No recent activity','Complete an action to begin your timeline.');
    lucide.createIcons();
}

function pickupView() {
    const types=[['Plastic','recycle'],['Paper','file-text'],['Metal','package'],['Glass','wine'],['E-Waste','laptop']];
    $('#pageContent').innerHTML=`<header class="page-head"><div><div class="eyebrow">DOORSTEP RECYCLING</div><h1>Schedule an Eco Pickup</h1><p>Choose your material and a convenient time. We’ll handle the rest responsibly.</p></div></header><section class="form-panel"><article class="form-card"><h2>Pickup details</h2><p class="sub">Required information is used to match a local collector.</p><form id="pickupForm"><div class="waste-types">${types.map(([name,ico],i)=>`<label><input type="radio" name="wasteType" value="${name}" ${i===0?'checked':''}><span>${icon(ico)}<b>${name}</b></span></label>`).join('')}</div><div class="fields"><label>Approximate quantity (kg)<input type="number" name="quantity" min="0.1" step="0.1" required></label><label>Waste photo<input type="file" name="photo" accept="image/jpeg,image/png,image/webp"><small>JPG, PNG or WebP · max 5 MB</small></label><div class="image-preview full" hidden><img alt="Waste preview"><button type="button" class="icon-btn" data-remove-image aria-label="Remove image">${icon('x')}</button></div><label class="full">Pickup address<input name="address" required maxlength="500"></label><label>Preferred date<input name="preferredDate" type="date" required></label><label>Preferred time<select name="preferredTime" required><option value="">Select time window</option><option>08:00 - 10:00</option><option>10:00 - 12:00</option><option>14:00 - 16:00</option><option>16:00 - 18:00</option></select></label><label class="full">Additional notes<textarea name="notes" rows="3" maxlength="1000"></textarea></label></div><p class="field-error" hidden></p><button class="primary submit">Confirm pickup request ${icon('arrow-right')}</button></form></article><aside class="side-card"><h2>Pickup progress</h2><p class="sub">Track every stage from request to recovery.</p>${['Requested','Accepted','Collector assigned','On the way','Collected','Completed'].map((label,i)=>`<div class="timeline-row" data-step="${i}"><i>${i+1}</i><div><h4>${label}</h4><p>Waiting for this stage.</p></div></div>`).join('')}<h2 style="margin-top:24px">Pickup history</h2><div class="history-list" data-history></div></aside></section>`;
    const date=$('[name=preferredDate]'); date.min=new Date().toISOString().slice(0,10);
    lucide.createIcons();
}

function renderPickupProgress(history){
    const latest=history[0];
    const rank=latest?({'REQUESTED':0,'ACCEPTED':2,'COLLECTOR_ASSIGNED':2,'ON_THE_WAY':3,'COLLECTED':4,'COMPLETED':5}[latest.status]??0):0;
    const descriptions=[latest?'Your pickup request was submitted.':'Submit a pickup request to begin.',rank>=1?'A collector accepted your request.':'Waiting for a collector to accept.',latest?.collectorName?`${latest.collectorName} is assigned to this pickup.`:'Collector details will appear here.',rank>=3?'Your collector is travelling to the pickup.':'Waiting for the collector to start the trip.',rank>=4?'Your recyclable material was collected.':'Waiting for collection.',rank>=5?'Pickup completed and impact recorded.':'Completion will update your impact.'];
    $$('.timeline-row').forEach((row,index)=>{row.classList.toggle('done',Boolean(latest)&&index<rank);row.classList.toggle('active',Boolean(latest)&&index===rank);$('i',row).innerHTML=index<rank?icon('check'):String(index+1);$('p',row).textContent=descriptions[index]});
    $('[data-history]').innerHTML=history.length?history.map(p=>`<article class="history-item">${p.imageUrl?`<img src="${escapeHtml(p.imageUrl)}" alt="">`:`<span class="metric-icon">${icon('recycle')}</span>`}<div><h4>${escapeHtml(p.wasteType)} · ${p.quantity} kg</h4><p>${escapeHtml(p.preferredDate)} · ${escapeHtml(p.preferredTime)}${p.collectorName?` · Collector: ${escapeHtml(p.collectorName)}`:''}</p></div><span class="status-badge">${escapeHtml(p.status)}</span></article>`).join(''):empty('No pickup requests yet','Your requests will appear here.');
    lucide.createIcons();
}

async function loadPickup() {
    const history=await api('/api/pickups');
    renderPickupProgress(history);
    const form=$('#pickupForm'), file=$('[name=photo]',form), preview=$('.image-preview',form), error=$('.field-error',form), submit=$('.submit',form);
    const showError=message=>{error.textContent=message;error.hidden=false};
    file.onchange=()=>{error.hidden=true;const selected=file.files[0];if(!selected){preview.hidden=true;return}try{if(!['image/jpeg','image/png','image/webp'].includes(selected.type))throw Error('Only JPG, PNG and WebP images are supported');if(selected.size>5*1024*1024)throw Error('Image must be 5 MB or smaller');$('img',preview).src=URL.createObjectURL(selected);preview.hidden=false}catch(e){file.value='';preview.hidden=true;showError(e.message)}};
    $('[data-remove-image]',form).onclick=()=>{file.value='';preview.hidden=true};
    form.onsubmit=async event=>{event.preventDefault();error.hidden=true;const values=new FormData(form);if(Number(values.get('quantity'))<=0)return showError('Quantity must be greater than zero');if(values.get('preferredDate')<new Date().toISOString().slice(0,10))return showError('Choose today or a future date');submit.disabled=true;submit.textContent='Submitting pickup request…';try{let imageUrl='';if(file.files[0])imageUrl=(await uploadImage(file.files[0],'pickups')).url;await api('/api/pickups',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({wasteType:values.get('wasteType'),quantity:Number(values.get('quantity')),imageUrl,address:values.get('address'),preferredDate:values.get('preferredDate'),preferredTime:values.get('preferredTime'),notes:values.get('notes')})});toast('Pickup requested successfully');form.reset();preview.hidden=true;await loadPickup()}catch(e){showError(e.message);toast(e.message,true)}finally{submit.disabled=false;submit.innerHTML=`Confirm pickup request ${icon('arrow-right')}`;lucide.createIcons()}};
    lucide.createIcons();
}

function feedView() {
    const activities=['Recycling','Tree Planting','Cleanup Drive','Water Conservation','Energy Saving','Sustainable Travel','Community Awareness','Other'];
    $('#pageContent').innerHTML=`<header class="page-head"><div><div class="eyebrow">COMMUNITY IN ACTION</div><h1>Eco Feed</h1><p>Real environmental work from people and communities around you.</p></div></header><div class="feed-layout"><section><form class="composer" id="feedComposer"><div class="composer-top"><span class="avatar">--</span><textarea rows="3" maxlength="4000" placeholder="What did you do for the planet today?"></textarea></div><div class="image-preview" hidden><img alt="Post image preview"><button type="button" class="icon-btn" data-remove-image aria-label="Remove image">${icon('x')}</button></div><div data-selected-activity></div><p class="field-error" hidden></p><div class="composer-actions"><div class="composer-tools"><input type="file" accept="image/jpeg,image/png,image/webp" hidden><button type="button" class="ghost" data-photo>${icon('image-plus')}Photo</button><div class="composer-tool-wrap"><button type="button" class="ghost" data-activity>${icon('tag')}Activity</button><div class="activity-menu">${activities.map(a=>`<button type="button" data-value="${a}">${a}</button>`).join('')}</div></div></div><button class="primary" disabled>Share action ${icon('arrow-right')}</button></div></form><div data-feed>${empty('Loading Eco Feed','Fetching community actions...')}</div></section><aside><article class="panel"><header class="panel-head"><h2>Community pulse</h2></header><div data-pulse></div></article><article class="panel" style="margin-top:14px"><header class="panel-head"><h2>Trending activities</h2></header><div class="pulse-list" data-trending></div></article></aside></div>`;
    lucide.createIcons();
}

function timeAgo(value){const seconds=Math.max(1,Math.floor((Date.now()-new Date(value))/1000));if(seconds<60)return 'Just now';if(seconds<3600)return `${Math.floor(seconds/60)}m ago`;if(seconds<86400)return `${Math.floor(seconds/3600)}h ago`;return new Intl.DateTimeFormat('en-IN',{day:'numeric',month:'short'}).format(new Date(value))}

function feedCard(post,user){const own=user&&post.userId===user.id;return `<article class="feed-card" data-post="${post.id}"><div class="feed-user"><span class="avatar">${initials(post.author)}</span><div><b>${escapeHtml(post.author)}</b><small>${timeAgo(post.createdAt)}</small></div>${own?`<button class="icon-btn" data-delete aria-label="Delete post" style="margin-left:auto">${icon('trash-2')}</button>`:''}</div>${post.activity?`<span class="activity-chip">${icon('tag')}${escapeHtml(post.activity)}</span>`:''}${post.caption?`<p>${escapeHtml(post.caption)}</p>`:''}${post.imageUrl?`<img class="feed-photo" src="${escapeHtml(post.imageUrl)}" alt="Shared environmental action">`:''}<div class="feed-actions"><button data-like class="${post.liked?'liked':''}">${icon('heart')}<span>${post.likes}</span></button><button data-comments>${icon('message-circle')}<span>${post.comments}</span></button></div><div class="comments" hidden><div data-comment-list></div><form class="comment-form"><input maxlength="1000" required placeholder="Write a comment"><button class="primary">Post</button></form></div></article>`}

async function loadFeed(user){
    const posts=await api('/api/posts'), container=$('[data-feed]');
    container.innerHTML=posts.length?posts.map(post=>feedCard(post,user)).join(''):empty('No posts yet','Share the first real eco action.');
    $('[data-pulse]').innerHTML=`<div class="metric" style="border:0"><span class="metric-icon">${icon('message-square')}</span><strong>${posts.length}</strong><small>stored community actions</small></div>`;
    const counts={};posts.forEach(post=>{if(post.activity)counts[post.activity]=(counts[post.activity]||0)+1});
    $('[data-trending]').innerHTML=Object.entries(counts).sort((a,b)=>b[1]-a[1]).slice(0,5).map(([name,count])=>`<div class="pulse-row"><span>${escapeHtml(name)}</span><b>${count}</b></div>`).join('')||empty('No trends yet','Choose an activity when you post.');
    $$('[data-post]').forEach(card=>{
        const id=card.dataset.post;
        $('[data-like]',card).onclick=async()=>{try{const result=await api(`/api/posts/${id}/like`,{method:'POST'});$('[data-like]',card).classList.toggle('liked',result.liked);$('[data-like] span',card).textContent=result.likes}catch(e){toast(e.message,true)}};
        $('[data-comments]',card).onclick=async()=>{const box=$('.comments',card);box.hidden=!box.hidden;if(!box.hidden){try{const data=await api(`/api/posts/${id}/comments`);$('[data-comment-list]',card).innerHTML=data.map(c=>`<div class="comment"><b>${escapeHtml(c.author)}</b>${escapeHtml(c.body)}</div>`).join('')||'<div class="sub">No comments yet.</div>'}catch(e){toast(e.message,true)}}};
        $('.comment-form',card).onsubmit=async event=>{event.preventDefault();const input=$('input',event.currentTarget);try{await api(`/api/posts/${id}/comments`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({body:input.value})});input.value='';const button=$('[data-comments] span',card);button.textContent=Number(button.textContent)+1;$('[data-comments]',card).click();$('[data-comments]',card).click()}catch(e){toast(e.message,true)}};
        const deleteButton=$('[data-delete]',card);if(deleteButton)deleteButton.onclick=async()=>{try{await api(`/api/posts/${id}`,{method:'DELETE'});toast('Post deleted');await loadFeed(user)}catch(e){toast(e.message,true)}};
    });
    const form=$('#feedComposer'), text=$('textarea',form), file=$('input[type=file]',form), preview=$('.image-preview',form), error=$('.field-error',form), submit=$('.primary',form);let activity='';
    const validate=()=>{submit.disabled=!text.value.trim()&&!file.files[0]&&!activity};text.oninput=validate;
    $('[data-photo]',form).onclick=()=>file.click();file.onchange=()=>{error.hidden=true;const selected=file.files[0];if(!selected){preview.hidden=true;validate();return}try{if(!['image/jpeg','image/png','image/webp'].includes(selected.type))throw Error('Only JPG, PNG and WebP images are supported');if(selected.size>5*1024*1024)throw Error('Image must be 5 MB or smaller');$('img',preview).src=URL.createObjectURL(selected);preview.hidden=false}catch(e){file.value='';preview.hidden=true;error.textContent=e.message;error.hidden=false}validate()};
    $('[data-remove-image]',form).onclick=()=>{file.value='';preview.hidden=true;validate()};
    $('[data-activity]',form).onclick=()=>$('.activity-menu',form).classList.toggle('open');$$('.activity-menu button',form).forEach(button=>button.onclick=()=>{activity=button.dataset.value;$('[data-selected-activity]',form).innerHTML=`<button type="button" class="activity-chip" data-clear-activity>${icon('tag')}${escapeHtml(activity)} ${icon('x')}</button>`;$('.activity-menu',form).classList.remove('open');$('[data-clear-activity]',form).onclick=()=>{activity='';$('[data-selected-activity]',form).innerHTML='';validate()};validate();lucide.createIcons()});
    form.onsubmit=async event=>{event.preventDefault();if(submit.disabled)return;submit.disabled=true;submit.textContent='Publishing…';error.hidden=true;try{let imageUrl='';if(file.files[0])imageUrl=(await uploadImage(file.files[0],'posts')).url;await api('/api/posts',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({caption:text.value.trim(),activity,imageUrl})});form.reset();preview.hidden=true;activity='';$('[data-selected-activity]',form).innerHTML='';toast('Your post is live in Eco Feed');await loadFeed(user)}catch(e){error.textContent=e.message;error.hidden=false;toast(e.message,true)}finally{submit.innerHTML=`Share action ${icon('arrow-right')}`;validate();lucide.createIcons()}};
    lucide.createIcons();
}

function eventsView(){ $('#pageContent').innerHTML=`<header class="page-head"><div><div class="eyebrow">ACT LOCALLY</div><h1>Events around you</h1><p>Meet good people and leave your city better than you found it.</p></div><button class="primary" data-host-toggle>${icon('plus')}Host an event</button></header><section class="host-event-panel" data-host-panel hidden><div class="panel-head"><div><h2>Host a community event</h2><p class="sub">Publish it here so EcoBridge members can discover and join it.</p></div><button class="icon-btn" type="button" data-host-close aria-label="Close event form">${icon('x')}</button></div><form id="eventHostForm"><div class="fields"><label>Event name<input name="name" maxlength="120" required placeholder="River cleanup"></label><label>Location<input name="location" maxlength="255" required placeholder="Mula River, Pune"></label><label>Date<input name="date" type="date" required></label><label>Time<input name="time" type="time" required></label><label>Maximum participants<input name="capacity" type="number" min="1" max="10000" value="25" required></label><label>Cover image URL <small>(optional)</small><input name="imageUrl" type="url" placeholder="https://..."></label><label class="full">Description<textarea name="description" rows="4" maxlength="2000" required placeholder="Tell members what you will do and what they should bring."></textarea></label></div><p class="field-error" hidden></p><div class="host-actions"><button type="button" class="ghost" data-host-cancel>Cancel</button><button class="primary" type="submit">Publish event ${icon('arrow-right')}</button></div></form></section><div class="filters">${['All events','Cleanups','Plantation','Workshops','My events'].map((x,i)=>`<button class="filter ${i===0?'active':''}" data-filter="${i===0?'all':x.split(' ')[0].toLowerCase()}">${x}</button>`).join('')}</div><section class="listing-grid"></section>`; const date=$('#eventHostForm [name=date]');date.min=new Date().toISOString().slice(0,10);lucide.createIcons(); }
async function loadEvents(user){const data=await api('/api/events'),grid=$('.listing-grid');const render=filter=>{const shown=data.filter(e=>filter==='all'||filter==='my'&&(e.joined||e.createdByCurrentUser)||filter==='cleanups'&&/clean|river|lake/i.test(e.name+e.description)||filter==='plantation'&&/tree|plant/i.test(e.name+e.description)||filter==='workshops'&&/workshop|awareness|compost|recycl/i.test(e.name+e.description));grid.innerHTML=shown.length?shown.map(e=>`<article class="listing"><div class="listing-img" style="background-image:url('${escapeHtml(e.imageUrl||'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=800&q=80')}')"><span>${escapeHtml(e.date)} · ${escapeHtml(e.time)}</span></div><div class="listing-body"><h3>${escapeHtml(e.name)}</h3><p>${escapeHtml(e.description)}</p><small>Hosted by ${escapeHtml(e.organizer)} · ${e.registeredCount}/${e.capacity} joined</small><div class="listing-meta"><span>${icon('map-pin')}${escapeHtml(e.location)}</span><button class="primary" data-event="${e.id}">${e.joined?'Leave event':'Join event'}</button></div></div></article>`).join(''):empty('No events yet','Host the first event and invite the EcoBridge community.');$$('[data-event]').forEach(button=>button.onclick=async()=>{if(!user)return location.href='/login.html';const e=data.find(item=>String(item.id)===button.dataset.event);try{await api(`/api/events/${e.id}/join`,{method:e.joined?'DELETE':'POST'});toast(e.joined?'Registration cancelled':'You joined the event');await loadEvents(user)}catch(error){toast(error.message,true)}});lucide.createIcons()};render('all');$$('.filter').forEach(button=>button.onclick=()=>{$$('.filter').forEach(b=>b.classList.remove('active'));button.classList.add('active');render(button.dataset.filter)});$('.search').oninput=()=>{const q=$('.search').value.toLowerCase();$$('.listing').forEach(card=>card.hidden=!card.textContent.toLowerCase().includes(q))};const panel=$('[data-host-panel]'),form=$('#eventHostForm');const setOpen=open=>{panel.hidden=!open;if(open)form.elements.name.focus()};$('[data-host-toggle]').onclick=()=>setOpen(true);$('[data-host-close]').onclick=()=>setOpen(false);$('[data-host-cancel]').onclick=()=>setOpen(false);form.onsubmit=async event=>{event.preventDefault();const error=$('.field-error',form),submit=$('[type=submit]',form),values=new FormData(form);error.hidden=true;submit.disabled=true;submit.textContent='Publishing…';try{await api('/api/events',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:values.get('name').trim(),description:values.get('description').trim(),date:values.get('date'),time:values.get('time'),location:values.get('location').trim(),capacity:Number(values.get('capacity')),imageUrl:values.get('imageUrl').trim()})});toast('Event published — members can now join');form.reset();form.elements.capacity.value=25;setOpen(false);await loadEvents(user)}catch(e){error.textContent=e.message;error.hidden=false}finally{submit.disabled=false;submit.innerHTML=`Publish event ${icon('arrow-right')}`;lucide.createIcons()}};}

function aboutView(){$('#pageContent').innerHTML=`<section class="about-hero"><div><div class="eyebrow">WHY WE EXIST</div><h1>We make environmental action <em>easier.</em></h1><p>EcoBridge connects everyday recycling, real-world participation and community storytelling—so good intentions become visible, measurable action.</p></div></section><section class="values"><article class="value">${icon('recycle')}<h3>Recycle responsibly</h3><p>Doorstep collection gives recoverable materials a responsible route.</p></article><article class="value">${icon('calendar-days')}<h3>Participate locally</h3><p>Events turn environmental concern into practical community work.</p></article><article class="value">${icon('message-square')}<h3>Inspire authentically</h3><p>Eco Feed celebrates environmental work people actually completed.</p></article></section>`;lucide.createIcons()}

async function init(){shell();bindShell();if(page==='dashboard')dashboardView();if(page==='pickup')pickupView();if(page==='feed')feedView();if(page==='events')eventsView();if(page==='about')aboutView();const user=await loadUser();if(page==='dashboard')await loadDashboard(user);if(page==='pickup'){await loadPickup();setInterval(async()=>{try{renderPickupProgress(await api('/api/pickups'))}catch(error){console.debug('Pickup refresh paused',error)}},5000)}if(page==='feed')await loadFeed(user);if(page==='events')await loadEvents(user)}
init().catch(error=>{toast(error.message,true);console.error(error)});
