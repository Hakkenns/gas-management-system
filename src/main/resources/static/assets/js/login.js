// ─── LOGIN.JS: Manejo del formulario de login ───────────────────────────────────

document.addEventListener('DOMContentLoaded', function() {
    
    const formLogin = document.getElementById('form-login');
    const btnLogin = document.getElementById('btn-login');
    const inputUsername = document.getElementById('username');
    const inputPassword = document.getElementById('password');

    // Validación en tiempo real
    if (inputUsername) {
        inputUsername.addEventListener('blur', validateUsername);
    }

    if (inputPassword) {
        inputPassword.addEventListener('blur', validatePassword);
    }

    // Permitir login con Enter
    if (formLogin) {
        formLogin.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performLogin();
            }
        });
    }

    // Click en botón login
    if (btnLogin) {
        btnLogin.addEventListener('click', function(e) {
            e.preventDefault();
            performLogin();
        });
    }

    // Restaurar estado en caso de volver con el botón atrás
    window.addEventListener('pageshow', function(event) {
        resetLoginButton();
    });

    resetLoginButton();
});

/**
 * Restaurar el estado del botón de login
 */
function resetLoginButton() {
    const btnLogin = document.getElementById('btn-login');
    if (!btnLogin) return;
    btnLogin.disabled = false;
    btnLogin.textContent = 'Ingresar';
}

/**
 * Valida que el username no esté vacío
 */
function validateUsername() {
    const username = document.getElementById('username').value.trim();
    const div = document.getElementById('username').closest('.input-group-custom');
    
    if (username === '') {
        div.classList.add('error');
        return false;
    }
    
    div.classList.remove('error');
    return true;
}

/**
 * Valida que la contraseña no esté vacía
 */
function validatePassword() {
    const password = document.getElementById('password').value.trim();
    const div = document.getElementById('password').closest('.input-group-custom');
    
    if (password === '') {
        div.classList.add('error');
        return false;
    }
    
    div.classList.remove('error');
    return true;
}

/**
 * Realiza el login enviando credenciales al servidor
 */
function performLogin() {
    
    // Validar campos
    if (!validateUsername() || !validatePassword()) {
        showError('Por favor completa todos los campos');
        return;
    }

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    // Deshabilitar botón durante petición
    const btnLogin = document.getElementById('btn-login');
    const originalText = btnLogin.textContent;
    btnLogin.disabled = true;
    btnLogin.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Ingresando...';

    // Obtener CSRF token del formulario (puede no existir si no hay Spring Security)
    const csrfTokenElement = document.querySelector('input[name="_csrf"]');
    const csrfToken = csrfTokenElement ? csrfTokenElement.value : '';
    const csrfHeader = 'X-CSRF-TOKEN';

    // Headers base
    const headers = {
        'Content-Type': 'application/json'
    };
    
    // Agregar CSRF token si existe
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    // Enviar credenciales al servidor
    fetch('/login', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            username: username,
            password: password
        })
    })
    .then(response => {
        if (response.status === 401) {
            return response.json().then(data => {
                throw new Error(data.message || data.detail || 'Usuario o contraseña incorrectos');
            });
        }

        if (response.status === 403) {
            return response.json().then(data => {
                throw new Error(data.message || data.detail || 'El usuario está desactivado');
            });
        }

        if (response.status === 423) {
            return response.json().then(data => {
                throw new Error(data.message || data.detail || 'Cuenta bloqueada por 15 minutos');
            });
        }

        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || data.detail || 'Error en la autenticación');
            }).catch(() => {
                throw new Error('Error en la autenticación');
            });
        }

        return response.json();
    })
    .then(usuarioData => {
        // Login exitoso
        console.log('Usuario logueado:', usuarioData);

        // Si el perfil es motorizado, forzar su vista asignados
        if (usuarioData.idPerfil === 4 || (usuarioData.nombrePerfil && usuarioData.nombrePerfil.toUpperCase() === 'MOTORIZADO')) {
            window.location.href = '/motorizado/asignados';
            return;
        }

        // Solicitar ruta de landing según el perfil en sesión
        fetch('/api/landing')
            .then(r => r.json())
            .then(data => {
                const path = data.path || '/';
                window.location.href = path;
            })
            .catch(err => {
                console.error('Error obteniendo landing:', err);
                window.location.href = '/';
            });
    })
    .catch(error => {
        console.error('Error:', error);
        showError(error.message || 'Error al intentar conectar');
        
        // Restaurar botón
        btnLogin.disabled = false;
        btnLogin.textContent = originalText;
    });
}

/**
 * Muestra un mensaje de error en la pantalla
 */
function showError(mensaje) {
    
    // Buscar si ya existe un div de error
    let alertDiv = document.querySelector('.alert-danger');
    
    if (!alertDiv) {
        // Crear el div si no existe
        alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger';
        alertDiv.role = 'alert';
        
        // Insertar antes del form
        const loginBox = document.querySelector('.login-box');
        loginBox.insertBefore(alertDiv, loginBox.querySelector('form'));
    }
    
    // Establecer mensaje y mostrar
    alertDiv.textContent = mensaje;
    alertDiv.style.display = 'block';
    
    // Auto-ocultar después de 5 segundos
    setTimeout(() => {
        alertDiv.style.display = 'none';
    }, 5000);
}

/**
 * Función para mostrar/ocultar contraseña
 */
function togglePassword() {
    const passwordInput = document.getElementById('password');
    const passwordIcon = document.querySelector('[onclick="togglePassword()"]');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        if (passwordIcon) {
            passwordIcon.classList.remove('fa-eye');
            passwordIcon.classList.add('fa-eye-slash');
        }
    } else {
        passwordInput.type = 'password';
        if (passwordIcon) {
            passwordIcon.classList.remove('fa-eye-slash');
            passwordIcon.classList.add('fa-eye');
        }
    }
}
