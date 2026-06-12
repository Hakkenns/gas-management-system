document.addEventListener("DOMContentLoaded", function () {
    const modalTitle = document.getElementById("modal-titulo-categoria");
    const form = document.getElementById("form-categoria");
    const idField = document.getElementById("categoria-id");
    const nombreField = document.getElementById("nombre-categoria");
    const descripcionField = document.getElementById("descripcion-categoria");
    // NUEVO: Capturamos el ComboBox del tipo de unidad
    const tipoUnidadField = document.getElementById("tipoUnidad");
    const btnCreate = document.getElementById("btn-crear-categoria");

    // Función reutilizable para abrir el modal (ACTUALIZADA con tipoUnidad)
    const openModal = function (title, id = "", nombre = "", descripcion = "", tipoUnidad = "") {
        modalTitle.textContent = title;
        idField.value = id;
        nombreField.value = nombre;
        descripcionField.value = descripcion;

        // Asignamos el valor al ComboBox. Si viene vacío (nueva categoría), vuelve a la opción por defecto
        if (tipoUnidadField) {
            tipoUnidadField.value = tipoUnidad;
        }

        if (window.jQuery && typeof window.jQuery === "function") {
            window.jQuery("#modal-categoria").modal("show");
        }
    };

    // Al hacer clic en "Nueva Categoría"
    if (btnCreate) {
        btnCreate.addEventListener("click", function () {
            openModal("Nueva Categoría");
        });
    }

    // LISTENER GLOBAL DE CLICS (Para capturar Editar, Eliminar y Estado)
    document.addEventListener("click", function (e) {

        // 1. Cargar datos para Editar
        const editButton = e.target.closest(".btn-editar-categoria");
        if (editButton) {
            const id = editButton.dataset.id || "";
            const nombre = editButton.dataset.nombre || "";
            const descripcion = editButton.dataset.descripcion || "";
            // NUEVO: Capturamos el data-tipounidad inyectado desde Thymeleaf
            const tipoUnidad = editButton.dataset.tipounidad || "";

            openModal("Editar Categoría", id, nombre, descripcion, tipoUnidad);
        }

        // 2. Cambiar Estado (Activar / Inhabilitar)
        const statusButton = e.target.closest(".btn-cambiar-estado-categoria");
        if (statusButton) {
            const id = statusButton.dataset.id;
            const nuevoEstado = statusButton.dataset.estado;
            CategoriaCambiarEstado(id, nuevoEstado);
        }

        // 3. Eliminar Categoría
        const deleteButton = e.target.closest(".btn-eliminar-categoria");
        if (deleteButton) {
            const id = deleteButton.dataset.id;
            CategoriaEliminar(id);
        }
    });

    // Función para refrescar únicamente el fragmento HTML de la tabla
    function reloadCategoriasTable() {
        fetch("/categorias/tabla")
            .then(r => {
                if (!r.ok) throw new Error("Error cargando tabla de categorías");
                return r.text();
            })
            .then(html => {
                const container = document.getElementById("contenedor-tabla-categoria");
                if (container) {
                    container.innerHTML = html;
                }
            })
            .catch(err => console.error("ERROR RECARGANDO TABLA:", err));
    }

    // Envío del Formulario (Guardar / Editar) vía AJAX
    if (form) {
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            const submitButton = form.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.textContent = "Guardando...";
            }

            const formData = new FormData(form);

            fetch("/categorias/ajax", {
                method: "POST",
                body: formData,
            })
                .then(response => response.json())
                .then(data => {
                    if (data.status === "OK") {
                        if (window.jQuery && typeof window.jQuery === "function") {
                            window.jQuery("#modal-categoria").modal("hide");
                        }
                        reloadCategoriasTable();
                    } else {
                        alert(data.message || "Error guardando categoría.");
                    }
                })
                .catch(error => {
                    console.error("Error guardando categoría:", error);
                    alert("Ocurrió un error al guardar la categoría.");
                })
                .finally(() => {
                    if (submitButton) {
                        submitButton.disabled = false;
                        submitButton.textContent = "Guardar";
                    }
                });
        });
    }

    // Función: Cambiar Estado por Fetch asíncrono
    function CategoriaCambiarEstado(id, estado) {
        const mensaje = estado == 1 ? "¿Deseas activar esta categoría?" : "¿Deseas inhabilitar esta categoría?";
        if (!confirm(mensaje)) return;

        fetch(`/categorias/${id}/estado?estado=${estado}`, {
            method: "POST"
        })
            .then(r => r.json())
            .then(data => {
                if (data.status === "OK") {
                    reloadCategoriasTable();
                } else {
                    alert(data.message || "Error al cambiar el estado");
                }
            })
            .catch(err => console.error("Error:", err));
    }

    // Función: Eliminar Categoría por Fetch asíncrono
    function CategoriaEliminar(id) {
        if (!confirm("¿Deseas eliminar esta categoría?")) return;

        fetch(`/categorias/${id}/eliminar`, {
            method: "POST"
        })
            .then(r => r.json())
            .then(data => {
                if (data.status === "OK") {
                    reloadCategoriasTable();
                } else {
                    alert(data.message || "Error al eliminar");
                }
            })
            .catch(err => console.error("Error:", err));
    }
});
