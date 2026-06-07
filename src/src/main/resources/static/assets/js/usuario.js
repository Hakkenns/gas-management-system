// ─── MOSTRAR / OCULTAR PASSWORD ───────────────────────────────────────────────
function togglePassword() {
    var input = document.getElementById('input-password');
    var icono = document.getElementById('icono-password');

    if (input.type === 'password') {
        input.type = 'text';
        icono.classList.remove('fa-eye');
        icono.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icono.classList.remove('fa-eye-slash');
        icono.classList.add('fa-eye');
    }
}


// ─── ABRIR MODAL NUEVO ────────────────────────────────────────────────────────
function abrirModalNuevo() {

    document.getElementById('form-usuario').reset();
    document.getElementById('modal-titulo').textContent = 'Nuevo Usuario';
    document.getElementById('form-usuario').action     = '/usuarios';
    document.getElementById('form-usuario').method     = 'post';

    // Resetear ojito
    document.getElementById('input-password').type     = 'password';
    document.getElementById('icono-password').className = 'fas fa-eye';

    $('#modal-usuario').modal('show');
}


// ─── ABRIR MODAL EDITAR ───────────────────────────────────────────────────────
function abrirModalEditar(id) {

    fetch('/usuarios/' + id)
        .then(function(response) {
            if (!response.ok) {
                alert('No se pudo cargar el usuario.');
                return null;
            }
            return response.json();
        })
        .then(function(usuario) {

            if (!usuario) return;

            document.getElementById('modal-titulo').textContent = 'Editar Usuario';

            document.getElementById('input-nombre').value   = usuario.nombre    || '';
            document.getElementById('input-username').value = usuario.userName   || '';
            document.getElementById('input-correo').value   = usuario.correo     || '';
            document.getElementById('input-perfil').value   = usuario.idPerfil   || '';

            // Password vacío — si no se toca se conserva la actual
            document.getElementById('input-password').value = '';

            // Resetear ojito
            document.getElementById('input-password').type   = 'password';
            document.getElementById('icono-password').className = 'fas fa-eye';

            document.getElementById('form-usuario').action = '/usuarios/' + id + '/editar';
            document.getElementById('form-usuario').method = 'post';

            $('#modal-usuario').modal('show');
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('Error al cargar los datos del usuario.');
        });
}


function reloadUsuariosTable() {
    fetch('/usuarios/tabla')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('No se pudo cargar la tabla de usuarios.');
            }
            return response.text();
        })
        .then(function(html) {
            var tbody = document.getElementById('tabla-usuarios');
            if (tbody) {
                tbody.outerHTML = html;
            }
        })
        .catch(function(error) {
            console.error('Error recargando tabla de usuarios:', error);
            alert('No se pudo recargar la tabla de usuarios.');
        });
}


var formUsuario = document.getElementById('form-usuario');
if (formUsuario) {
    formUsuario.addEventListener('submit', function(event) {
        event.preventDefault();

        var url = formUsuario.action;
        var data = new URLSearchParams(new FormData(formUsuario)).toString();

        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: data
        })
        .then(function(response) {
            return response.json().then(function(body) {
                if (!response.ok) {
                    throw new Error(body.message || 'Error guardando usuario.');
                }
                return body;
            });
        })
        .then(function(body) {
            if (body.status === 'OK') {
                $('#modal-usuario').modal('hide');
                reloadUsuariosTable();
            } else {
                throw new Error(body.message || 'Error guardando usuario.');
            }
        })
        .catch(function(error) {
            console.error('Error guardando usuario:', error);
            alert(error.message || 'Error guardando usuario.');
        });
    });
}


// ─── CAMBIAR ESTADO (ACTIVAR / DESACTIVAR) ────────────────────────────────────
function cambiarEstado(id, nuevoEstado) {

    var mensaje = nuevoEstado === 1
        ? '¿Deseas activar este usuario?'
        : '¿Deseas desactivar este usuario?';

    if (!confirm(mensaje)) return;

    fetch('/usuarios/' + id + '/estado', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'estado=' + encodeURIComponent(nuevoEstado)
    })
    .then(function(response) {
        return response.json().then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Error cambiando estado.');
            }
            return body;
        });
    })
    .then(function(body) {
        if (body.status === 'OK') {
            reloadUsuariosTable();
        } else {
            throw new Error(body.message || 'Error cambiando estado.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert(error.message || 'Error al cambiar el estado.');
    });
}


// ─── ELIMINAR USUARIO ─────────────────────────────────────────────────────────
var idUsuarioAEliminar = null;

function eliminarUsuario(id) {
    idUsuarioAEliminar = id;
    $('#modal-eliminar').modal('show');
}

// Confirmar eliminar desde el modal
document.getElementById('btn-confirmar-eliminar').addEventListener('click', function() {

    if (!idUsuarioAEliminar) return;

    fetch('/usuarios/' + idUsuarioAEliminar + '/eliminar', {
        method: 'POST'
    })
    .then(function(response) {
        return response.json().then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Error eliminando usuario.');
            }
            return body;
        });
    })
    .then(function(body) {
        if (body.status === 'OK') {
            $('#modal-eliminar').modal('hide');
            idUsuarioAEliminar = null;
            reloadUsuariosTable();
        } else {
            throw new Error(body.message || 'Error eliminando usuario.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert(error.message || 'Error al eliminar el usuario.');
    });
});