const params = new URLSearchParams(location.search);
if (params.has('error')) {
    const box = document.querySelector('.auth-error');
    box.hidden = false;
    box.textContent = 'Google sign-in could not be completed. Please try again.';
}
lucide.createIcons();
