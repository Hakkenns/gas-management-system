document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("form-producto");
    const modalTitulo = document.getElementById("modal-titulo-producto");

    // Elementos del formulario
    const idInput = document.getElementById("producto-id");
    const nombreInput = document.getElementById("producto-nombre");
    const descripcionInput = document.getElementById("producto-descripcion");
    const urlImagenInput = document.getElementById("producto-urlImagen");
    const categoriaSelect = document.getElementById("producto-categoria");
    const proveedorSelect = document.getElementById("producto-proveedor");
    const precioCompraInput = document.getElementById("producto-precioCompra");
    const precioVentaInput = document.getElementById("producto-precioVenta");
    const stockLlenosInput = document.getElementById("producto-stockLlenos");
    const stockVaciosInput = document.getElementById("producto-stockVacios");
    const stockMinimoInput = document.getElementById("producto-stockMinimo");
    const requiereEnvaseCheckbox = document.getElementById("producto-requiereEnvase");

    // 1. ESCUCHAR CLICS EN LA TABLA ASÍNCRONA
    document.getElementById("contenedor-tabla-productos").addEventListener("click", function (e) {
        
        // ✏️ BOTÓN EDITAR
        const btnEditar = e.target.closest(".btn-editar-producto");
        if (btnEditar) {
            const id = btnEditar.dataset.id;
            modalTitulo.innerText = "Editar Producto";
            form.reset();

            fetch(`/productos/${id}`)
                .then(response => {
                    if (!response.ok) throw new Error("No se pudieron cargar los datos");
                    return response.json();
                })
                .then(data => {
                    idInput.value = data.id;
                    nombreInput.value = data.nombre;
                    descripcionInput.value = data.descripcion || "";
                    urlImagenInput.value = data.urlImagen || "";
                    precioCompraInput.value = data.precioCompra;
                    precioVentaInput.value = data.precioVenta;
                    stockLlenosInput.value = data.stockLlenos;
                    stockVaciosInput.value = data.stockVacios;
                    stockMinimoInput.value = data.stockMinimo;
                    categoriaSelect.value = data.idCategoria || "";
                    proveedorSelect.value = data.idProveedor || "";
                    requiereEnvaseCheckbox.checked = data.requiereEnvase === true;

                    // Abrir modal usando la instancia jQuery de Bootstrap 4
                    $("#modal-producto").modal("show");
                })
                .catch(err => alert("Error: " + err.message));
        }

        // 🟢/🔴 BOTÓN CAMBIAR ESTADO (Activar / Inhabilitar)
        const btnEstado = e.target.closest(".btn-estado-producto");
        if (btnEstado) {
            const id = btnEstado.dataset.id;
            const nuevoEstado = btnEstado.dataset.estado;
            
            fetch(`/productos/${id}/estado?estado=${nuevoEstado}`, { method: "POST" })
                .then(response => response.json())
                .then(res => {
                    if (res.status === "OK") {
                        recargarTabla();
                    } else {
                        alert("Error al cambiar de estado: " + res.message);
                    }
                })
                .catch(err => alert("Error en el servidor al cambiar estado"));
        }

        // 🗑️ BOTÓN ELIMINAR
        const btnEliminar = e.target.closest(".btn-eliminar-producto");
        if (btnEliminar) {
            const id = btnEliminar.dataset.id;
            if (confirm("¿Está seguro de que desea eliminar este producto?")) {
                fetch(`/productos/${id}/eliminar`, { method: "POST" })
                    .then(response => response.json())
                    .then(res => {
                        if (res.status === "OK") {
                            recargarTabla();
                        } else {
                            alert("Error al eliminar: " + res.message);
                        }
                    });
            }
        }
    });

    // 2. BOTÓN NUEVO PRODUCTO
    document.getElementById("btn-crear-producto").addEventListener("click", function () {
        modalTitulo.innerText = "Nuevo Producto";
        form.reset();
        idInput.value = "";
        requiereEnvaseCheckbox.checked = false;
        $("#modal-producto").modal("show");
    });

    // 3. ENVÍO DEL FORMULARIO POR AJAX
    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const formData = new FormData(form);
        if (!requiereEnvaseCheckbox.checked) {
            formData.set("requiereEnvase", "false");
        }

        fetch("/productos", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(res => {
            if (res.status === "OK") {
                $("#modal-producto").modal("hide"); // Cierra la ventana eliminando el fondo gris
                recargarTabla();
            } else {
                alert("Atención: " + res.message);
            }
        })
        .catch(err => alert("Error al procesar la solicitud."));
    });

    // 4. REFRESCAR LA TABLA
    function recargarTabla() {
        fetch("/productos/tabla")
            .then(response => response.text())
            .then(html => {
                document.getElementById("contenedor-tabla-productos").innerHTML = html;
            })
            .catch(err => console.error("Error al refrescar la tabla:", err));
    }
});