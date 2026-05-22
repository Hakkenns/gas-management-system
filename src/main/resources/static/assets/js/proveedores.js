document.addEventListener("click", function (e) {
    if (e.target.closest("#btn-crear-proveedor")) {
        document.getElementById("form-proveedor").reset();
        document.getElementById("input-id").value = "";
        document.getElementById("modal-titulo-proveedor").textContent = "Crear Proveedor";
        $("#modal-proveedor").modal("show");
    }

    if (e.target.closest(".btn-editar-proveedor")) {
        const btn = e.target.closest(".btn-editar-proveedor");
        const id = btn.dataset.id;

        fetch(`/proveedores/${id}`)
            .then(r => r.json())
            .then(data => {
                document.getElementById("modal-titulo-proveedor").textContent = "Editar Proveedor";
                document.getElementById("input-id").value = data.id;
                document.getElementById("input-ruc").value = data.ruc;
                document.getElementById("input-nombre").value = data.nombre;
                document.getElementById("input-telefono").value = data.telefono;
                document.getElementById("input-correo").value = data.correo;
                document.getElementById("input-rubro").value = data.idRubro;
                $("#modal-proveedor").modal("show");
            })
            .catch(err => console.error("ERROR JSON PROVEEDOR:", err));
    }

    if (e.target.closest(".btn-cambiar-estado")) {
        const btn = e.target.closest(".btn-cambiar-estado");
        const id = btn.dataset.id;
        const estadoActual = parseInt(btn.dataset.estado, 10);
        const nuevoEstado = estadoActual === 1 ? 0 : 1;
        ProveedorCambiarEstado(id, nuevoEstado);
        e.preventDefault();
    }

    if (e.target.closest(".btn-eliminar-proveedor")) {
        const id = e.target.closest(".btn-eliminar-proveedor").dataset.id;
        ProveedorEliminar(id);
        e.preventDefault();
    }
});

function initTablaProveedores() {
    if ($.fn.DataTable) {
        const table = $('#tabla-proveedores');
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().destroy();
        }
        table.DataTable({
            paging: true,
            lengthChange: true,
            searching: true,
            ordering: true,
            info: true,
            autoWidth: false,
            responsive: true,
        });
    }
}

function reloadProveedoresTable() {
    fetch("/proveedores/tabla")
        .then(r => {
            if (!r.ok) throw new Error("Error cargando tabla de proveedores");
            return r.text();
        })
        .then(html => {
            const container = document.getElementById("contenedor-tabla");
            if (container) {
                container.innerHTML = html;
                initTablaProveedores();
            }
        })
        .catch(err => console.error("ERROR recargando tabla de proveedores:", err));
}

initTablaProveedores();

$("#form-proveedor").on("submit", function (e) {
    e.preventDefault();

    const btnGuardar = $(this).find('button[type="submit"]');
    btnGuardar.prop('disabled', true).text('Guardando...');

    $.ajax({
        url: "/proveedores",
        type: "POST",
        data: $(this).serialize(),
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                $("#modal-proveedor").modal('hide');
                reloadProveedoresTable();
            } else {
                const message = resp.message || "Error guardando proveedor";
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
            console.error("Error guardando proveedor:", msg);
        },
        complete: function () {
            btnGuardar.prop('disabled', false).text('Guardar');
        }
    });
});

function ProveedorCambiarEstado(id, estado) {
    $.ajax({
        url: `/proveedores/${id}/estado`,
        type: "POST",
        data: { estado: estado },
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadProveedoresTable();
            } else {
                alert(resp.message || "Error cambiando estado");
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.error("Error cambiando estado:", msg);
        }
    });
}

function ProveedorEliminar(id) {
    if (!confirm("¿Eliminar el proveedor?")) return;

    $.ajax({
        url: `/proveedores/${id}/eliminar`,
        type: "POST",
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadProveedoresTable();
            } else {
                alert(resp.message || "ERROR ELIMINANDO PROVEEDOR");
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.error("ERROR ELIMINANDO PROVEEDOR:", msg);
        }
    });
}
