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


// ─── CAMBIAR ESTADO (ACTIVAR / DESACTIVAR) ────────────────────────────────────
function cambiarEstado(id, nuevoEstado) {

    var mensaje = nuevoEstado === 1
        ? '¿Deseas activar este usuario?'
        : '¿Deseas desactivar este usuario?';

    if (!confirm(mensaje)) return;

    fetch('/usuarios/' + id + '/estado', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ estado: nuevoEstado })
    })
    .then(function(response) {
        if (response.ok) {
            // Recargar la página para reflejar el nuevo estado
            window.location.reload();
        } else {
            alert('No se pudo cambiar el estado del usuario.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert('Error al cambiar el estado.');
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
        if (response.ok) {
            $('#modal-eliminar').modal('hide');
            window.location.reload();
        } else {
            alert('No se pudo eliminar el usuario.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert('Error al eliminar el usuario.');
    });
});