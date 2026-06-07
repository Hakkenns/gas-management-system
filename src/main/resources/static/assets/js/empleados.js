// OBLIGATORIO: Interceptar el formulario cuando se envía
document.addEventListener("DOMContentLoaded", function() {
    console.log("Empleados script listo.");

    var formEmpleado = document.getElementById('form-empleado');
    if (formEmpleado) {
        formEmpleado.addEventListener('submit', function(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var url = formEmpleado.action;
            var data = new URLSearchParams(new FormData(formEmpleado)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando empleado.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-empleado').modal('hide');
                    reloadEmpleadosTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos del empleado se procesaron con éxito.',
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

// ─── ACCIONES DEL MODAL ──────────────────────────────────────────────────────
function abrirModalNuevo() {
    var form = document.getElementById('form-empleado');
    if (form) form.reset();

    // Habilitar la edición del nombre por si acaso quedó bloqueado de una búsqueda previa
    var inputNombre = document.getElementById('input-nombre');
    if (inputNombre) inputNombre.readOnly = false;

    document.getElementById('modal-titulo').textContent = 'Nuevo Empleado';
    if (form) form.action = '/empleados';
    $('#modal-empleado').modal('show');
}

function abrirModalEditar(id) {
    fetch('/empleados/' + id)
        .then(response => response.json())
        .then(empleado => {
            document.getElementById('modal-titulo').textContent = 'Editar Empleado';
            document.getElementById('input-nombre').value   = empleado.nombre || '';
            document.getElementById('input-nombre').readOnly = false; // Permitir editar en edición
            document.getElementById('input-dni').value      = empleado.dni || '';
            document.getElementById('input-sueldo').value   = empleado.sueldoBase || '';
            document.getElementById('input-telefono').value = empleado.telefono || '';

            var form = document.getElementById('form-empleado');
            if (form) form.action = '/empleados/' + id + '/editar';

            $('#modal-empleado').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

function reloadEmpleadosTable() {
    fetch('/empleados/tabla')
        .then(response => response.text())
        .then(html => {
            var tbody = document.getElementById('tabla-employees'); // Revisa si tu ID es 'tabla-empleados' o 'tabla-employees'
            if (!tbody) tbody = document.getElementById('tabla-empleados');
            if (tbody) tbody.outerHTML = html;
        });
}

// ─── ELIMINAR CON SWEETALERT2 DIRECTO ────────────────────────────────────────
function eliminarEmpleado(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "El empleado se desactivará en el sistema.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/empleados/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadEmpleadosTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'El empleado ha sido dado de baja.',
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

// ─── CONSULTA API DNI (EXTRAÍDA AL SCOPE GLOBAL) ──────────────────────────────
function buscarDniApi() {
    const dni = document.getElementById('input-dni').value;

    if (dni.length !== 8 || isNaN(dni)) {
        Swal.fire({
            title: 'DNI Inválido',
            text: 'Por favor, ingrese un número de DNI de 8 dígitos.',
            icon: 'warning'
        });
        return;
    }

    const btnBuscar = document.getElementById('btn-buscar-dni');
    const iconoOriginal = btnBuscar.innerHTML;
    btnBuscar.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    btnBuscar.disabled = true;

    // Endpoint proxy local
    fetch('/empleados/api/consultar-dni/' + dni)
        .then(response => {
            if (!response.ok) throw new Error('No se pudo establecer conexión con el servidor.');
            return response.json();
        })
        .then(res => {
            // Nota: Se valida res.success tal como lo estructuraste
            if (res && res.datos) {
                const info = res.datos;

                // Sanitización y formateo
                const listaNombres = info.nombres.trim().split(/\s+/);
                const primerNombre = listaNombres[0];
                const nombreFormateado = `${primerNombre} ${info.ape_paterno} ${info.ape_materno}`;

                document.getElementById('input-nombre').value = nombreFormateado;
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
            btnBuscar.innerHTML = iconoOriginal;
            btnBuscar.disabled = false;
        });
}