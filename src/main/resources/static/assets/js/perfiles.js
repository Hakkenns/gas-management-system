
document.addEventListener("click", function (e) {

    //modal de crear perfil
    if (e.target.closest("#btn-crear-perfil")) {

        document.getElementById("form-perfil").reset();
        document.getElementById("id-perfil").value = "";

        document.getElementById("modal-titulo-perfil").textContent = "Crear Perfil";

        $("#modal-perfil").modal("show");
    }

    if (e.target.closest(".close") || e.target.closest("#btn-cancelar-perfil")) {
        $("#modal-perfil").modal("hide");
    }


    //editar perfil
    if (e.target.closest(".btn-editar-perfil")) {

        const id = e.target.closest(".btn-editar-perfil").dataset.id;

        fetch(`views/perfiles.php?action=obtener&id=${id}`)
            .then(r => r.json())
            .then(data => {

                document.getElementById("modal-titulo-perfil").textContent = "Editar Perfil";

                document.getElementById("id-perfil").value = data.id_perfil;
                document.getElementById("nombre-perfil").value = data.nombre_perfil;
                document.getElementById("descripcion-perfil").value = data.descripcion;

                $("#modal-perfil").modal("show");
            })
            .catch(err => console.log("ERROR JSON PERFIL:", err));
    }

    //abrir el modal
    if (e.target.closest(".btn-permisos-perfil")) {

        const btn = e.target.closest(".btn-permisos-perfil");
        const id = btn.dataset.id;
        const nombre = btn.dataset.nombre;

        document.getElementById("perm-nombre-perfil").textContent = nombre;

        $("#modal-permisos").modal("show");

        fetch(`views/perfiles.php?action=obtener_permisos&id=${id}`)
            .then(r => r.json())
            .then(data => {

                document.getElementById("perm-id").value = data.idPerfil;

                let rows = "";

                data.opciones.forEach(op => {

                    const checked = data.permisosActuales.includes(op.id_opcion)
                        ? "checked"
                        : "";

                    rows += `<tr><td><label class="d-flex align-items-center" style="gap:1px; cursor:pointer;">
                              <input type="checkbox" name="opciones_seleccionadas[]" value="${op.id_opcion}" ${checked}> <span>${op.nombre}</span>
                             </label></td></tr>`;
                });

                document.getElementById("perm-lista-opciones").innerHTML = rows;
            })
            .catch(err => {
                console.log("ERROR JSON:", err);
            });
    }

    if (e.target.closest(".close-permisos")) {
        $("#modal-permisos").modal("hide");
    }
});



//guardar perfil ajax
$("#form-perfil").on("submit", function (e) {
    e.preventDefault();

    $.ajax({
        url: "views/perfiles.php?action=guardar_perfil",
        type: "POST",
        data: $(this).serialize(),
        success: function (resp) {
            if (resp.trim() === "OK") {
                $("#modal-perfil").modal('hide');
                AbrirPagina("perfiles");
            } else {
                console.log("Error:", resp);
            }
        }
    });
});


//guardar permisos ajax
$(document).on("submit", "#form-permisos-ajax", function (e) {

    e.preventDefault();

    $.ajax({
        url: "views/perfiles.php?action=guardar_permisos",
        type: "POST",
        data: $(this).serialize(),
        success: function (resp) {

            let data = {};

            try {
                data = JSON.parse(resp);
            } catch (e) {
                console.log("Respuesta no válida:", resp);
                return;
            }

            if (data.status === "OK") {

                $("#modal-permisos").modal("hide");

                $(".main-sidebar").replaceWith(data.sidebar);

                AbrirPagina("perfiles");
            }
        }
    });
});



function PerfilCambiarEstado(id, estadoActual) {

    let accion = (estadoActual == 1) ? "anular" : "activar";

    $.get(`views/perfiles.php?action=${accion}&id=${id}`, function (resp) {
        if (resp.trim() === "OK") {
            AbrirPagina("perfiles");
        } else {
            console.log("Error cambiando estado:", resp);
        }
    });
}




function PerfilEliminar(id) {

    if (!confirm("¿Eliminar el perfil?")) return;

    $.get(`views/perfiles.php?action=eliminar&id=${id}`, function (resp) {

        if (resp.trim() === "OK") {
            AbrirPagina("perfiles");
        } else {
            console.log("ERROR ELIMINANDO PERFIL:", resp);
        }
    });
}
