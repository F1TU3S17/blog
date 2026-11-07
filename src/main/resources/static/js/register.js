const registerForm = document.getElementById('register-form');
const registerError = document.getElementById('register-error');

if (registerForm && registerError) {
    registerForm.addEventListener('input', () => registerError.remove(), { once: true });
}
