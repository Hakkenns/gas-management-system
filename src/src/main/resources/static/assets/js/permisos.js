// assets/js/permisos.js — versión SPA corregida

document.addEventListener("click", function (e) {

    // BOTÓN PERMISOS
    if (e.target.closest(".btn-permisos-perfil")) {

        const btn = e.target.closest(".btn-permisos-perfil");
        const id = btn.dataset.id;
        const nombre = btn.dataset.nombre;

        const modal = $("#modal-permisos");

        // Título
        $("#perfil-nombre-permisos").text(nombre);

        // Contenido mientras carga
        $("#contenido-permisos").html(`
            <div class='text-center p-3'>
                <i class="fas fa-spinner fa-spin fa-2x"></i>
                <br>Cargando permisos...
            </div>
        `);

        // Mostrar modal
        modal.modal("show");

        // Cargar matriz de permisos
        fetch(`admin.php?page=perfiles&action=obtener_permisos&id=${id}`)
            .then(r => r.text())
            .then(html => {
                $("#contenido-permisos").html(html);
            })
            .catch(err => {
                $("#contenido-permisos").html(`
                    <div class="alert alert-danger">
                        Error al cargar permisos<br>${err.message}
                    </div>
                `);
            });
    }


    // CERRAR MODAL con botón personalizado
    if (e.target.closest(".close-permisos")) {
        $("#modal-permisos").modal("hide");
    }

});


// CERRAR MODAL haciendo click fuera
window.addEventListener("click", function (e) {
    const modal = document.getElementById("modal-permisos");
    if (e.target === modal) {
        $("#modal-permisos").modal("hide");
    }
});


$(document).on("submit", "#form-permisos-ajax", function(e) {
    e.preventDefault();

    $.ajax({
        url: "views/perfiles.php?action=guardar_permisos",
        method: "POST",
        data: $(this).serialize(),
        success: function(r) {
            if (r.trim() === "OK") {
                $("#modal-permisos").modal("hide");
                AbrirPagina("perfiles");
            } else {
                console.log("Error permisos:", r);
            }
        }
    });
});

