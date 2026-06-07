// OBLIGATORIO: Interceptar el formulario cuando se envía
document.addEventListener("DOMContentLoaded", function() {
    console.log("Motos script listo.");

    var formMoto = document.getElementById('form-moto');
    if (formMoto) {
        formMoto.addEventListener('submit', function(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var url = formMoto.action;
            var data = new URLSearchParams(new FormData(formMoto)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando moto.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-moto').modal('hide');
                    reloadMotosTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos de la moto se procesaron con éxito.',
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
    var form = document.getElementById('form-moto');
    if (form) form.reset();
    document.getElementById('modal-titulo').textContent = 'Nueva Moto';
    if (form) form.action = '/motos';
    $('#modal-moto').modal('show');
}

function abrirModalEditar(id) {
    fetch('/motos/' + id)
        .then(response => response.json())
        .then(moto => {
            document.getElementById('modal-titulo').textContent = 'Editar Moto';
            document.getElementById('input-placa').value  = moto.placa || '';
            document.getElementById('input-marca').value  = moto.marca || '';
            document.getElementById('input-modelo').value = moto.modelo || '';
            document.getElementById('input-anio').value   = moto.anio || '';

            var form = document.getElementById('form-moto');
            if (form) form.action = '/motos/' + id + '/editar';

            $('#modal-moto').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

function reloadMotosTable() {
    fetch('/motos/tabla')
        .then(response => response.text())
        .then(html => {
            var tbody = document.getElementById('tabla-motos');
            if (tbody) tbody.outerHTML = html;
        });
}

// ─── ELIMINAR CON SWEETALERT2 DIRECTO ────────────────────────────────────────
function eliminarMoto(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "La moto se desactivará en el sistema.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/motos/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadMotosTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'La moto ha sido dada de baja.',
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
