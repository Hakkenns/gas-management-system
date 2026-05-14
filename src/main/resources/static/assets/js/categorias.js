// assets/js/categorias.js
// Lógica para el módulo de Gestión de Categorías

$(document).ready(function() {

    // 1. MANEJO DE MODAL Y BOTONES DE CREAR/EDITAR
    
    // Evento Delegado para clics
    $(document).on("click", function (e) {

        // CREAR CATEGORÍA (#btn-crear-categoria)
        if (e.target.closest("#btn-crear-categoria")) {
            $("#form-categoria")[0].reset();
            $("#categoria-id").val('');
            $("#modal-titulo-categoria").text("Crear Categoría");
            $("#modal-categoria").modal("show");
        }

        // EDITAR CATEGORÍA (.btn-editar-categoria)
        if (e.target.closest(".btn-editar-categoria")) {
            const id = e.target.closest(".btn-editar-categoria").dataset.id;

            fetch(`views/categorias.php?action=obtener&id=${id}`)
                .then(r => r.json())
                .then(data => {

                    $("#modal-titulo-categoria").text("Editar Categoría");

                    $("#categoria-id").val(data.id);
                    $("#nombre-categoria").val(data.nombre);
                    $("#descripcion-categoria").val(data.descripcion);

                    $("#modal-categoria").modal("show");
                });
        }
    });

    // --------------------------------------------------------
    // 2. ENVÍO DE FORMULARIO (AJAX)
    // --------------------------------------------------------

    // Previene el doble envío y maneja la inserción/actualización
    $("#form-categoria").off("submit").on("submit", function(e) {
        e.preventDefault();

        let formData = new FormData(this);

        // Deshabilita el botón de guardar
        const btnGuardar = $(this).find('button[type="submit"]');
        btnGuardar.prop('disabled', true).text('Guardando...');

        $.ajax({
            url: "views/categorias.php?action=guardar",
            method: "POST",
            data: formData,
            contentType: false,
            processData: false,
            success: function(r) {
                if (r.trim() === "OK") {
                    $("#modal-categoria").modal('hide');
                    AbrirPagina("categorias"); // Recarga la vista de categorías
                } else {
                    console.error("Error al guardar categoría:", r);
                }
            },
            error: function() {
                alert("Ocurrió un error en la comunicación con el servidor.");
            },
            complete: function() {
                // Vuelve a habilitar el botón
                btnGuardar.prop('disabled', false).text('Guardar');
            }
        });
    });
});