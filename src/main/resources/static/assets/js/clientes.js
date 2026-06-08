// ─── INTERCEPTAR Y PROCESAR EL FORMULARIO AL ENVIAR ───────────────────────────
document.addEventListener("DOMContentLoaded", function() {
    console.log("Clientes script listo.");

    var formCliente = document.getElementById('form-cliente');
    if (formCliente) {
        formCliente.addEventListener('submit', function(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var inputDni = document.getElementById('input-dni');
            var inputNombre = document.getElementById('input-nombre');

            // Si el DNI está vacío o solo tiene espacios, lo limpiamos por completo
            if (inputDni && inputDni.value.trim() === '') {
                inputDni.value = '';
            }

            // LÓGICA ANÓNIMA: Si el usuario dejó el nombre vacío, lo autocompletamos
            if (inputNombre && (!inputNombre.value || inputNombre.value.trim() === '')) {
                inputNombre.value = "CLIENTE VARIOS / ANÓNIMO";
            }

            var url = formCliente.action;
            var data = new URLSearchParams(new FormData(formCliente)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando cliente.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-cliente').modal('hide');
                    reloadClientesTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos del cliente se procesaron con éxito.',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });
                }
            })
            .catch(function(error) {
                Swal.fire({
                    title: 'Advertencia',
                    text: error.message,
                    icon: 'warning',
                    confirmButtonColor: '#ffc107'
                });
            });
        });
    }
});

// ─── ACCIONES DEL MODAL (NUEVO / EDITAR) ──────────────────────────────────────
function abrirModalNuevo() {
    var form = document.getElementById('form-cliente');
    if (form) form.reset();

    // Habilitar la edición del nombre por si acaso quedó bloqueado de una búsqueda previa
    var inputNombre = document.getElementById('input-nombre');
    if (inputNombre) inputNombre.readOnly = false;

    document.getElementById('modal-titulo').textContent = 'Nuevo Cliente';
    if (form) form.action = '/clientes';
    $('#modal-cliente').modal('show');
}

function abrirModalEditar(id) {
    fetch('/clientes/' + id)
        .then(response => response.json())
        .then(cliente => {
            document.getElementById('modal-titulo').textContent = 'Editar Cliente';
            document.getElementById('input-nombre').value     = cliente.nombre || '';
            document.getElementById('input-nombre').readOnly = false; // Permitir editar en edición
            document.getElementById('input-dni').value        = cliente.dni || '';
            document.getElementById('input-telefono').value   = cliente.telefono || '';
            document.getElementById('input-direccion').value  = cliente.direccion || '';
            document.getElementById('input-referencia').value = cliente.referencia || '';
            document.getElementById('input-correo').value     = cliente.correo || '';

            var form = document.getElementById('form-cliente');
            if (form) form.action = '/clientes/' + id + '/editar';

            $('#modal-cliente').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

// ─── VISTA RÁPIDA (SOLO LECTURA - OJITO CELESTE) ─────────────────────────────
function verCliente(id) {
    fetch('/clientes/' + id)
        .then(response => {
            if (!response.ok) throw new Error('No se pudo obtener la información.');
            return response.json();
        })
        .then(cliente => {
            // Rellenar los elementos del modal plano (sin inputs)
            document.getElementById('view-nombre').textContent    = cliente.nombre || '---';
            document.getElementById('view-dni').textContent       = cliente.dni || '---';
            document.getElementById('view-telefono').textContent  = cliente.telefono || '---';
            document.getElementById('view-direccion').textContent = cliente.direccion || '---';
            document.getElementById('view-referencia').textContent= cliente.referencia || 'Sin referencia registrada.';
            document.getElementById('view-correo').textContent    = cliente.correo || '---';

            // Mostrar el modal informativo
            $('#modal-ver-cliente').modal('show');
        })
        .catch(error => {
            console.error('Error:', error);
            Swal.fire({
                title: 'Error',
                text: 'No se pudo cargar el visor del cliente.',
                icon: 'error'
            });
        });
}

// ─── REFRESCAR TABLA DINÁMICAMENTE ───────────────────────────────────────────
function reloadClientesTable() {
    fetch('/clientes/tabla')
        .then(response => response.text())
        .then(html => {
            var tbody = document.getElementById('tabla-clientes');
            if (tbody) tbody.outerHTML = html;
        });
}

// ─── ELIMINAR REGISTRO (DESACTIVACIÓN LÓGICA) ────────────────────────────────
function eliminarCliente(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "El cliente se desactivará en el sistema.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/clientes/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadClientesTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'El cliente ha sido dado de baja.',
                        icon: 'success',
                        timer: 1500,
                        showConfirmButton: false
                    });
                }
            })
            .catch(error => {
                Swal.fire({ title: 'Error', text: 'No se pudo eliminar.', icon: 'error' });
            });
        }
    });
}

// ─── CONSULTA API DNI (PERMITE VACÍOS AL GUARDAR, VALIDA SOLO AL BUSCAR) ──────
function buscarDniApi() {
    const dniInput = document.getElementById('input-dni');
    const dni = dniInput ? dniInput.value.trim() : '';

    // Si el usuario presiona la lupa pero el campo está totalmente vacío
    if (dni === '') {
        Swal.fire({
            title: 'Campo Vacío',
            text: 'Por favor, ingrese un número de DNI para poder buscarlo.',
            icon: 'info'
        });
        return;
    }

    // Solo exigir los 8 dígitos si se intenta buscar activamente con la lupa
    if (dni.length !== 8 || isNaN(dni)) {
        Swal.fire({
            title: 'DNI Inválido',
            text: 'El DNI debe tener exactamente 8 dígitos numéricos.',
            icon: 'warning'
        });
        return;
    }

    const btnBuscar = document.getElementById('btn-buscar-dni');
    const contenedorIcono = document.getElementById('icono-buscar');
    const iconoOriginal = '<i class="fas fa-search"></i>';

    if (contenedorIcono) {
        contenedorIcono.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    }
    btnBuscar.disabled = true;

    fetch('/clientes/api/consultar-dni/' + dni)
        .then(response => {
            if (!response.ok) throw new Error('No se pudo establecer conexión con el servidor.');
            return response.json();
        })
        .then(res => {
            if (res && res.datos) {
                const info = res.datos;
                const listaNombres = info.nombres.trim().split(/\s+/);
                const primerNombre = listaNombres[0];
                const nombreFormateado = `${primerNombre} ${info.ape_paterno} ${info.ape_materno || ''}`;

                document.getElementById('input-nombre').value = nombreFormateado.toUpperCase();
                document.getElementById('input-nombre').readOnly = true;
            } else {
                throw new Error('El DNI ingresado no existe en el padrón o la respuesta no es válida.');
            }
        })
        .catch(error => {
            Swal.fire({
                title: 'Error de Búsqueda',
                text: error.message,
                icon: 'error'
            });
            document.getElementById('input-nombre').readOnly = false;
        })
        .finally(() => {
            if (contenedorIcono) {
                contenedorIcono.innerHTML = iconoOriginal;
            }
            btnBuscar.disabled = false;
        });
}