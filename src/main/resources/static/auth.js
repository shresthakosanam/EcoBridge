const mode = document.body.dataset.auth;
const form = document.querySelector('.auth-form');
const card = document.querySelector('.auth-card');
const success = document.querySelector('.success');
const errorBox = document.querySelector('.auth-error');

function showError(message) {
  errorBox.textContent = message;
  errorBox.hidden = false;
}

document.querySelector('[data-eye]')?.addEventListener('click', event => {
  const input = event.currentTarget.closest('.input-wrap').querySelector('input');
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  event.currentTarget.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
});

form?.addEventListener('submit', async event => {
  event.preventDefault();
  errorBox.hidden = true;
  const submit = form.querySelector('.submit');
  const originalLabel = submit.textContent;
  const values = Object.fromEntries(new FormData(form));
  submit.disabled = true;
  submit.textContent = mode === 'signup' ? 'Creating account…' : 'Logging in…';

  try {
    const response = await fetch(`/api/auth/${mode}`, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(values)
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.message || (response.status >= 500
        ? 'The server could not complete your request. Please try again.'
        : 'Please check your details and try again.'));
    }
    card.style.display = 'none';
    success.classList.add('show');
    setTimeout(() => location.replace('/dashboard.html'), 500);
  } catch (error) {
    showError(error.message || 'Could not connect to EcoBridge. Please try again.');
    submit.disabled = false;
    submit.textContent = originalLabel;
  }
});
