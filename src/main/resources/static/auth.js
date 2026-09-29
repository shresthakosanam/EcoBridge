const mode = document.body.dataset.auth;
const form = document.querySelector('.auth-form');
const card = document.querySelector('.auth-card');
const success = document.querySelector('.success');
const errorBox = document.querySelector('.auth-error');
function showError(message) { errorBox.textContent = message; errorBox.hidden = false; }
document.querySelector('[data-eye]')?.addEventListener('click', event => { const input=event.currentTarget.closest('.input-wrap').querySelector('input'); const show=input.type==='password'; input.type=show?'text':'password'; event.currentTarget.setAttribute('aria-label',show?'Hide password':'Show password'); });
fetch('/api/auth/providers').then(r=>r.ok?r.json():{google:false}).then(p=>{if(!p.google)document.querySelectorAll('[data-google]').forEach(e=>e.hidden=true)}).catch(()=>document.querySelectorAll('[data-google]').forEach(e=>e.hidden=true));
if(new URLSearchParams(location.search).has('error'))showError('Google sign-in could not be completed. You can still use email and password.');
form?.addEventListener('submit',async event=>{event.preventDefault();errorBox.hidden=true;const submit=form.querySelector('.submit');const values=Object.fromEntries(new FormData(form));submit.disabled=true;submit.textContent=mode==='signup'?'Creating account…':'Logging in…';try{const response=await fetch(`/api/auth/${mode}`,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json'},body:JSON.stringify(values)});const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(data.message||'Could not complete authentication.');card.style.display='none';success.classList.add('show');setTimeout(()=>location.replace('/dashboard.html'),500)}catch(error){showError(error.message);submit.disabled=false;submit.textContent=mode==='signup'?'Create my account →':'Log in to EcoBridge →'}});
