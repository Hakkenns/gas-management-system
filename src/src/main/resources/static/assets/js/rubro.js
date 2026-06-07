$(document).ready(function () {

    // Listener global para capturar clics en la página
    document.addEventListener("click", function (e) {

        // Abrir modal vacío para crear
        if (e.target.closest("#btn-crear-rubro")) {
            document.getElementById("form-rubro").reset();
            document.getElementById("id-rubro").value = "";
            document.getElementById("modal-titulo-rubro").textContent = "Crear Rubro";
            $("#modal-rubro").modal("show");
        }

        // Botones de salida manual del modal
        if (e.target.closest(".close") || e.target.closest("#btn-cancelar-rubro")) {
            $("#modal-rubro").modal("hide");
        }

        // Cargar data por fetch para editar
        if (e.target.closest(".btn-editar-rubro")) {
            const id = e.target.closest(".btn-editar-rubro").dataset.id;

            fetch(`/rubros/${id}`)
                .then(r => r.json())
                .then(data => {
                    document.getElementById("modal-titulo-rubro").textContent = "Editar Rubro";
                    document.getElementById("id-rubro").value = data.id;
                    document.getElementById("nombre-rubro").value = data.nombre;
                    document.getElementById("descripcion-rubro").value = data.descripcion;
                    $("#modal-rubro").modal("show");
                })
                .catch(err => console.log("ERROR AL CARGAR DATOS DEL RUBRO:", err));
        }

        // 🟢 NUEVO: Detectar clic en los botones de Activar / Inhabilitar Estado
        if (e.target.closest(".btn-estado-rubro")) {
            const boton = e.target.closest(".btn-estado-rubro");
            const id = boton.dataset.id;
            const nuevoEstado = boton.dataset.estado;
            RubroCambiarEstado(id, nuevoEstado);
        }

        // Acción eliminar (Lógico -> Pasa a estado 0)
        if (e.target.closest(".btn-eliminar-rubro")) {
            const id = e.target.closest(".btn-eliminar-rubro").dataset.id;
            RubroEliminar(id);
        }
    });

    // Refrescar el fragmento HTML de la tabla sin recargar la página
    function reloadRubrosTable() {
        fetch("/rubros/tabla")
            .then(r => {
                if (!r.ok) throw new Error("Error cargando tabla de rubros");
                return r.text();
            })
            .then(html => {
                const container = document.getElementById("contenedor-tabla");
                if (container) {
                    container.innerHTML = html;
                }
            })
            .catch(err => console.log("ERROR RECARGANDO TABLA:", err));
    }

    // Envío del Formulario vía AJAX
    $("#form-rubro").on("submit", function (e) {
        e.preventDefault();

        const id = document.getElementById("id-rubro").value;
        const url = id ? `/rubros/${id}/editar` : "/rubros";

        $.ajax({
            url: url,
            type: "POST",
            data: $(this).serialize(),
            dataType: "json",
            success: function (resp) {
                if (resp.status === "OK") {
                    $("#modal-rubro").modal('hide');
                    reloadRubrosTable();
                } else {
                    alert(resp.message || "Error al procesar el rubro");
                }
            },
            error: function (xhr) {
                console.log("XHR completo de error:", xhr);
                alert("Error interno del servidor al guardar.");
            }
        });
    });

    // Función para cambiar a estado 0 (Inactivo / Eliminado según tu lógica unificada)
    function RubroEliminar(id) {
        if (!confirm("¿Deseas eliminar este rubro?")) return;

        $.ajax({
            url: `/rubros/${id}/eliminar`,
            type: "POST",
            dataType: "json",
            success: function (resp) {
                if (resp.status === "OK") {
                    reloadRubrosTable();
                } else {
                    alert(resp.message || "Error al eliminar");
                }
            },
            error: function (xhr) {
                alert("No se pudo eliminar el rubro.");
            }
        });
    }

    // 🟢 NUEVA FUNCIÓN: Cambiar Estado de forma asíncrona (AJAX)
    function RubroCambiarEstado(id, estado) {
        const mensaje = estado == 1 ? "¿Deseas activar este rubro?" : "¿Deseas inhabilitar este rubro?";
        if (!confirm(mensaje)) return;

        $.ajax({
            url: `/rubros/${id}/estado`,
            type: "POST",
            data: { estado: estado },
            dataType: "json",
            success: function (resp) {
                if (resp.status === "OK") {
                    reloadRubrosTable(); // Refresca dinámicamente la tabla
                } else {
                    alert(resp.message || "Error al cambiar el estado");
                }
            },
            error: function (xhr) {
                alert("No se pudo cambiar el estado del rubro.");
            }
        });
    }

});