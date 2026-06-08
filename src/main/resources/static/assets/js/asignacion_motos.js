document.addEventListener("DOMContentLoaded", function() {
    console.log("Asignación de motos script listo.");

    // NUEVO: Al cargar la página, verificamos si venimos de un redireccionamiento de éxito
    var urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('guardado') === 'true') {
        Swal.fire({
            title: '¡Guardado!',
            text: 'La moto se asignó de manera exitosa.',
            icon: 'success',
            timer: 2000,
            showConfirmButton: false
        });
        // Limpiamos la URL para que no vuelva a salir la alerta si el usuario da F5 manual
        window.history.replaceState({}, document.title, window.location.pathname);
    } else if (urlParams.get('finalizado') === 'true') {
        Swal.fire({
            title: '¡Recibida!',
            text: 'La unidad está libre para volver a ser asignada.',
            icon: 'success',
            timer: 1500,
            showConfirmButton: false
        });
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    var formAsignacion = document.getElementById('form-asignacion');
    if (formAsignacion) {
        formAsignacion.addEventListener('submit', function(event) {
            event.preventDefault();

            var url = formAsignacion.action;
            var data = new URLSearchParams(new FormData(formAsignacion)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando asignación.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-asignacion').modal('hide');
                    // Redirige inmediatamente con el parámetro. La página carga y la alerta sale encima.
                    window.location.href = window.location.pathname + '?guardado=true';
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

function abrirModalNuevo() {
    var form = document.getElementById('form-asignacion');
    if (form) form.reset();
    document.getElementById('modal-titulo').textContent = 'Nueva Asignación';
    $('#modal-asignacion').modal('show');
}

function finalizarAsignacion(id) {
    Swal.fire({
        title: '¿Finalizar Asignación?',
        text: "Se registrará la devolución de la moto en el sistema.",
        icon: 'info',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, recibir unidad',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/asignacion_motos/' + id + '/finalizar', {
                method: 'POST'
            })
            .then(response => response.json().then(body => {
                if (!response.ok) throw new Error(body.message || 'No se pudo procesar.');
                return body;
            }))
            .then(body => {
                if (body.status === 'OK') {
                    // Redirige inmediatamente liberando los componentes
                    window.location.href = window.location.pathname + '?finalizado=true';
                }
            })
            .catch(error => {
                Swal.fire({ title: 'Error', text: error.message, icon: 'error' });
            });
        }
    });
}

function cambiarFiltro() {
    var estado = document.getElementById('filtro-estado').value;
    // Redirecciona manteniendo el filtro seleccionado en la página 0
    window.location.href = '/asignacion_motos?estado=' + estado + '&page=0';
}