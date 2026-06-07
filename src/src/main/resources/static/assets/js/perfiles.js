document.addEventListener("click", function (e) {

    // modal de crear perfil
    if (e.target.closest("#btn-crear-perfil")) {
        document.getElementById("form-perfil").reset();
        document.getElementById("id-perfil").value = "";
        document.getElementById("modal-titulo-perfil").textContent = "Crear Perfil";
        $("#modal-perfil").modal("show");
    }

    if (e.target.closest(".close") || e.target.closest("#btn-cancelar-perfil")) {
        $("#modal-perfil").modal("hide");
    }

    // editar perfil
    if (e.target.closest(".btn-editar-perfil")) {
        const id = e.target.closest(".btn-editar-perfil").dataset.id;

        fetch(`/perfiles/${id}`)
            .then(r => r.json())
            .then(data => {
                document.getElementById("modal-titulo-perfil").textContent = "Editar Perfil";
                document.getElementById("id-perfil").value = data.id;
                document.getElementById("nombre-perfil").value = data.nombrePerfil;
                document.getElementById("descripcion-perfil").value = data.descripcion;
                $("#modal-perfil").modal("show");
            })
            .catch(err => console.log("ERROR JSON PERFIL:", err));
    }

    // abrir el modal de permisos
    if (e.target.closest(".btn-permisos-perfil")) {
        const btn = e.target.closest(".btn-permisos-perfil");
        const id = btn.dataset.id;
        const nombre = btn.dataset.nombre;

        document.getElementById("perm-nombre-perfil").textContent = nombre;
        document.getElementById("perm-id").value = id;

        fetch(`/perfiles/${id}/permisos`)
            .then(r => r.json())
            .then(data => {
                let rows = "";

                data.opciones.forEach(op => {
                    const checked = data.permisosActuales.includes(op.id) ? "checked" : "";
                    rows += `<tr><td><label class="d-flex align-items-center" style="gap:1px; cursor:pointer;">
                              <input type="checkbox" name="idOpciones" value="${op.id}" ${checked}> <span>${op.nombre}</span>
                             </label></td></tr>`;
                });

                document.getElementById("perm-lista-opciones").innerHTML = rows;
                $("#modal-permisos").modal("show");
            })
            .catch(err => {
                console.log("ERROR JSON:", err);
            });
    }

    if (e.target.closest(".close-permisos")) {
        $("#modal-permisos").modal("hide");
    }

    if (e.target.closest(".btn-cambiar-estado")) {
        const btn = e.target.closest(".btn-cambiar-estado");
        const id = btn.dataset.id;
        const estadoActual = parseInt(btn.dataset.estado, 10);
        PerfilCambiarEstado(id, estadoActual);
    }

    if (e.target.closest(".btn-eliminar-perfil")) {
        const id = e.target.closest(".btn-eliminar-perfil").dataset.id;
        PerfilEliminar(id);
    }
});

function reloadPerfilesTable() {
    fetch("/perfiles/tabla")
        .then(r => {
            if (!r.ok) {
                throw new Error("Error cargando tabla de perfiles");
            }
            return r.text();
        })
        .then(html => {
            const tbody = document.getElementById("tabla-perfiles");
            if (tbody) {
                tbody.outerHTML = html;
            }
        })
        .catch(err => {
            console.log("ERROR recargando tabla de perfiles:", err);
        });
}

// guardar perfil ajax
$("#form-perfil").on("submit", function (e) {
    e.preventDefault();

    const id = document.getElementById("id-perfil").value;
    const url = id ? `/perfiles/${id}/editar` : "/perfiles";

    $.ajax({
        url: url,
        type: "POST",
        data: $(this).serialize(),
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                $("#modal-perfil").modal('hide');
                reloadPerfilesTable();
            } else {
                const message = resp.message || "Error guardando perfil";
                alert(message);
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.log("Error guardando perfil:", msg);
        }
    });
});

// guardar permisos ajax
$(document).on("submit", "#form-permisos-ajax", function (e) {
    e.preventDefault();

    const id = document.getElementById("perm-id").value;

    $.ajax({
        url: `/perfiles/${id}/permisos`,
        type: "POST",
        data: $(this).serialize(),
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                $("#modal-permisos").modal("hide");
                reloadPerfilesTable();
            } else {
                const message = resp.message || "Error guardando permisos";
                alert(message);
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.log("Error guardando permisos:", msg);
        }
    });
});

function PerfilCambiarEstado(id, estadoActual) {
    const nuevoEstado = estadoActual === 1 ? 0 : 1;

    $.ajax({
        url: `/perfiles/${id}/estado`,
        type: "POST",
        data: { estado: nuevoEstado },
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadPerfilesTable();
            } else {
                const message = resp.message || "Error cambiando estado";
                alert(message);
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.log("Error cambiando estado:", msg);
        }
    });
}

function PerfilEliminar(id) {
    if (!confirm("¿Eliminar el perfil?")) return;

    $.ajax({
        url: `/perfiles/${id}/eliminar`,
        type: "POST",
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadPerfilesTable();
            } else {
                const message = resp.message || "ERROR ELIMINANDO PERFIL";
                alert(message);
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.log("ERROR ELIMINANDO PERFIL:", msg);
        }
    });
}
