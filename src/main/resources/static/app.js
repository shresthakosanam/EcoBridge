const $=(selector,context=document)=>context.querySelector(selector);
const $$=(selector,context=document)=>[...context.querySelectorAll(selector)];
const esc=value=>String(value??'').replace(/[&<>'"]/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
const ico=name=>`<i data-lucide="${name}"></i>`;

async function call(url,options={}){
    const response=await fetch(url,{credentials:'same-origin',headers:{'Content-Type':'application/json'},...options});
    const data=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(data.message||data.detail||`Request failed (${response.status})`);
    return data;
}

function renderEvents(events){
    $('#eventList').innerHTML=events.length?events.slice(0,3).map(event=>`<article class="event-card" style="background-image:url('${esc(event.imageUrl||'https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=1200&q=80')}')"><div class="event-info"><small>${new Date(event.date+'T12:00').toLocaleDateString('en-IN',{day:'numeric',month:'short'}).toUpperCase()} · ${esc(event.time)} · ${event.registeredCount}/${event.capacity} JOINED</small><h3>${esc(event.name)}</h3><p>${ico('map-pin')} ${esc(event.location)} · by ${esc(event.organizer)}</p></div><a class="join" href="/events.html">View event</a></article>`).join(''):'<p>No community events have been published yet.</p>';
}

function renderPosts(posts){
    $('#postList').innerHTML=posts.length?posts.slice(0,3).map(post=>`<article class="post"><div class="post-img" style="background-image:url('${esc(post.imageUrl||'https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=1200&q=80')}')"></div><div class="post-body"><div class="post-user"><b>${esc(post.author)}</b><span>${esc(post.activity||'Eco action')}</span></div><p>${esc(post.caption)}</p><div class="post-actions"><span>${ico('heart')} ${post.likes}</span><span>${ico('message-circle')} ${post.comments}</span><a href="/feed.html">${ico('arrow-up-right')} View</a></div></div></article>`).join(''):'<p>No community actions have been shared yet.</p>';
    $('.counter').textContent=posts.length.toLocaleString('en-IN');
    $('.hero aside>span').textContent='community actions shared';
}

$$('.open-pickup').forEach(button=>button.onclick=()=>location.href='/pickup.html');
$$('[data-open="eventModal"]').forEach(button=>button.onclick=()=>location.href='/events.html');
$$('[data-open="postModal"]').forEach(button=>button.onclick=()=>location.href='/feed.html');
$('.menu').onclick=()=>{const nav=$('.nav nav'),open=nav.classList.toggle('open');$('.menu').setAttribute('aria-expanded',open);$('.menu').innerHTML=ico(open?'x':'menu');lucide.createIcons()};

Promise.all([call('/api/events'),call('/api/posts')]).then(([events,posts])=>{renderEvents(events);renderPosts(posts);lucide.createIcons()}).catch(error=>console.error('Could not load landing-page data',error));
lucide.createIcons();
const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.animate([{opacity:0,transform:'translateY(28px)'},{opacity:1,transform:'none'}],{duration:650,easing:'cubic-bezier(.2,.7,.2,1)',fill:'both'});observer.unobserve(entry.target)}}),{threshold:.1});
$$('.section,.journey').forEach(element=>observer.observe(element));
