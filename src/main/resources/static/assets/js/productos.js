document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("form-producto");
    const modalTitulo = document.getElementById("modal-titulo-producto");

    // Elementos del formulario - Datos Generales
    const idInput = document.getElementById("producto-id");
    const nombreInput = document.getElementById("producto-nombre");
    const descripcionInput = document.getElementById("producto-descripcion");
    const categoriaSelect = document.getElementById("producto-categoria");
    const precioCompraInput = document.getElementById("producto-precioCompra");
    const precioVentaInput = document.getElementById("producto-precioVenta");
    const gananciaProductoInput = document.getElementById("producto-gananciaProducto");
    const stockLlenosInput = document.getElementById("producto-stockLlenos");
    const stockVaciosInput = document.getElementById("producto-stockVacios");
    const stockMinimoInput = document.getElementById("producto-stockMinimo");
    const requiereEnvaseCheckbox = document.getElementById("producto-requiereEnvase");
    const contenedorStockVacios = document.getElementById("contenedor-stockVacios");

    // IDs de categorías
    const ID_ACCESORIOS_GAS = 2;

    // Elementos del formulario - Imagen
    const archivoImagenInput = document.getElementById("producto-archivo-imagen");
    const btnAgregarImagen = document.getElementById("btn-agregar-imagen");
    const btnLimpiarImagen = document.getElementById("btn-limpiar-imagen");
    const previewImagen = document.getElementById("preview-imagen");
    const textoSinImagen = document.getElementById("texto-sin-imagen");
    const quitarImagenInput = document.getElementById("producto-quitarImagen");
    const imagenBase64Hidden = document.getElementById("producto-imagenBase64");

    // ============ ALMACENAMIENTO EN MEMORIA DE IMÁGENES STAGED ============
    // Estructura: { productoId: { imagenBase64: "...", quitarImagen: true/false } }
    // Usamos localStorage para persistir entre recargas de página
    const STORAGE_KEY = "productosImagenesStaged";
    
    function cargarImagenesStagedDesdeStorage() {
        const stored = localStorage.getItem(STORAGE_KEY);
        return stored ? JSON.parse(stored) : {};
    }
    
    let imagenesStaged = cargarImagenesStagedDesdeStorage();

    function cargarEstadoImagenStaged(productoId) {
        if (productoId && imagenesStaged[productoId]) {
            const staged = imagenesStaged[productoId];
            quitarImagenInput.value = staged.quitarImagen ? "true" : "false";
            imagenBase64Hidden.value = staged.imagenBase64 || "";
            
            if (staged.imagenBase64) {
                previewImagen.src = staged.imagenBase64;
                previewImagen.style.display = "block";
                textoSinImagen.style.display = "none";
                btnAgregarImagen.innerText = "Imagen agregada";
                btnAgregarImagen.classList.remove("btn-secondary");
                btnAgregarImagen.classList.add("btn-success");
            } else if (staged.quitarImagen) {
                previewImagen.src = "";
                previewImagen.style.display = "none";
                textoSinImagen.style.display = "block";
                btnAgregarImagen.innerText = "Agregar Imagen";
                btnAgregarImagen.classList.remove("btn-success");
                btnAgregarImagen.classList.add("btn-secondary");
            }
            return true;
        }
        return false;
    }

    function guardarEstadoImagenStaged(productoId) {
        if (productoId) {
            imagenesStaged[productoId] = {
                imagenBase64: imagenBase64Hidden.value || "",
                quitarImagen: quitarImagenInput.value === "true"
            };
            // Persistir en localStorage
            localStorage.setItem(STORAGE_KEY, JSON.stringify(imagenesStaged));
        }
    }

    function limpiarEstadoImagenStaged(productoId) {
        if (productoId && imagenesStaged[productoId]) {
            delete imagenesStaged[productoId];
            // Persistir cambios en localStorage
            localStorage.setItem(STORAGE_KEY, JSON.stringify(imagenesStaged));
        }
    }

    // ============ MANEJO DE STOCK VACÍOS ============
    function actualizarVisibilidadStockVacios() {
        const categoriaId = parseInt(categoriaSelect.value);
        if (categoriaId === ID_ACCESORIOS_GAS) {
            contenedorStockVacios.style.display = "none";
            stockVaciosInput.value = 0;
            stockVaciosInput.required = false;
        } else {
            contenedorStockVacios.style.display = "block";
            stockVaciosInput.required = true;
        }
    }

    categoriaSelect.addEventListener("change", actualizarVisibilidadStockVacios);

    // ============ MANEJO DE CÁLCULO DE PRECIO VENTA ============
    function calcularPrecioVenta() {
        const precioCompra = parseFloat(precioCompraInput.value) || 0;
        const ganancia = parseFloat(gananciaProductoInput.value) || 0;
        if (precioCompra > 0) {
            const precioVenta = precioCompra + ganancia;
            precioVentaInput.value = precioVenta.toFixed(2);
        } else {
            precioVentaInput.value = "0.00";
        }
    }

    gananciaProductoInput.addEventListener("change", calcularPrecioVenta);
    gananciaProductoInput.addEventListener("input", calcularPrecioVenta);

    // MANEJO DE VISTA PREVIA DE IMAGEN
    archivoImagenInput.addEventListener("change", function (e) {
        const archivo = e.target.files[0];
        if (archivo) {
            const reader = new FileReader();
            reader.onload = function (evento) {
                previewImagen.src = evento.target.result;
                previewImagen.style.display = "block";
                textoSinImagen.style.display = "none";
                // Actualizar nombre del label
                document.querySelector(".custom-file-label").innerText = archivo.name;
                // Al seleccionar un archivo, no lo marcamos como agregado hasta que el usuario presione "Agregar Imagen"
                quitarImagenInput.value = "false";
            };
            reader.readAsDataURL(archivo);
        } else {
            previewImagen.src = "";
            previewImagen.style.display = "none";
            textoSinImagen.style.display = "block";
            document.querySelector(".custom-file-label").innerText = "Seleccionar archivo...";
        }
    });

    // BOTON AGREGAR IMAGEN
    btnAgregarImagen.addEventListener("click", function () {
        if (archivoImagenInput.files.length === 0) {
            alert("Por favor selecciona una imagen primero");
            return;
        }
        const archivo = archivoImagenInput.files[0];
        const reader = new FileReader();
        reader.onload = function (evento) {
            // Guardamos la imagen en base64 en el campo oculto para que quede "staged"
            imagenBase64Hidden.value = evento.target.result;
            quitarImagenInput.value = "false";
            btnAgregarImagen.innerText = "Imagen agregada";
            btnAgregarImagen.classList.remove("btn-secondary");
            btnAgregarImagen.classList.add("btn-success");
            
            // Guardar estado en memoria
            const productoId = idInput.value;
            guardarEstadoImagenStaged(productoId);
            
            alert("La imagen se preparó correctamente y quedará lista para guardar.");
        };
        reader.readAsDataURL(archivo);
    });

    // BOTON LIMPIAR IMAGEN
    btnLimpiarImagen.addEventListener("click", function () {
        archivoImagenInput.value = "";
        previewImagen.src = "";
        previewImagen.style.display = "none";
        textoSinImagen.style.display = "block";
        document.querySelector(".custom-file-label").innerText = "Seleccionar archivo...";
        // Marcamos que se eliminará la imagen actual al guardar
        quitarImagenInput.value = "true";
        // Limpiamos cualquier imagen previamente staged
        imagenBase64Hidden.value = "";
        btnAgregarImagen.innerText = "Agregar Imagen";
        btnAgregarImagen.classList.remove("btn-success");
        btnAgregarImagen.classList.add("btn-secondary");
        
        // Guardar estado en memoria
        const productoId = idInput.value;
        guardarEstadoImagenStaged(productoId);
    });

    // 1. ESCUCHAR CLICS EN LA TABLA ASÍNCRONA
    document.getElementById("contenedor-tabla-productos").addEventListener("click", function (e) {
        
        // ✏️ BOTÓN EDITAR
        const btnEditar = e.target.closest(".btn-editar-producto");
        if (btnEditar) {
            const id = btnEditar.dataset.id;
            modalTitulo.innerText = "Editar Producto";
            form.reset();
            // NO llamamos a btnLimpiarImagen.click() aquí para no borrar el estado staged
            // En su lugar, reseteamos solo los campos visuales
            archivoImagenInput.value = "";
            previewImagen.src = "";
            previewImagen.style.display = "none";
            textoSinImagen.style.display = "block";
            document.querySelector(".custom-file-label").innerText = "Seleccionar archivo...";
            btnAgregarImagen.innerText = "Agregar Imagen";
            btnAgregarImagen.classList.remove("btn-success");
            btnAgregarImagen.classList.add("btn-secondary");

            fetch(`/productos/${id}`)
                .then(response => {
                    if (!response.ok) throw new Error("No se pudieron cargar los datos");
                    return response.json();
                })
                .then(data => {
                    idInput.value = data.id;
                    nombreInput.value = data.nombre;
                    descripcionInput.value = data.descripcion || "";
                    precioCompraInput.value = data.precioCompra != null ? data.precioCompra : "0.00";
                    gananciaProductoInput.value = data.gananciaProducto || 0;
                    precioVentaInput.value = data.precioVenta != null ? data.precioVenta : "0.00";
                    stockLlenosInput.value = data.stockLlenos != null ? data.stockLlenos : 0;
                    stockVaciosInput.value = data.stockVacios != null ? data.stockVacios : 0;
                    stockMinimoInput.value = data.stockMinimo != null ? data.stockMinimo : 0;
                    categoriaSelect.value = data.idCategoria || "";
                    requiereEnvaseCheckbox.checked = data.requiereEnvase === true;

                    // Verificar si hay un estado staged para esta imagen
                    const tieneEstadoStaged = cargarEstadoImagenStaged(data.id);

                    if (!tieneEstadoStaged) {
                        // Reset flags de imagen solo si no hay estado staged
                        quitarImagenInput.value = "false";
                        imagenBase64Hidden.value = "";

                        // Mostrar imagen existente si la hay
                        if (data.urlImagen) {
                            previewImagen.src = data.urlImagen;
                            previewImagen.style.display = "block";
                            textoSinImagen.style.display = "none";
                        }
                    }

                    actualizarVisibilidadStockVacios();
                    // Abrir modal usando la instancia jQuery de Bootstrap 4
                    $("#modal-producto").modal("show");
                })
                .catch(err => alert("Error: " + err.message));
        }

        // 📝️ BOTÓN VER DESCRIPCIÓN
        const btnVerDescripcion = e.target.closest(".btn-ver-descripcion");
        if (btnVerDescripcion) {
            const nombreProducto = btnVerDescripcion.dataset.nombre;
            const descripcion = btnVerDescripcion.dataset.descripcion;

            document.getElementById("modal-descripcion-titulo").innerText = "Descripción - " + nombreProducto;
            document.getElementById("modal-descripcion-contenido").innerText = descripcion || "Sin descripción";
            $("#modal-descripcion").modal("show");
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

    // 2. BOTON NUEVO PRODUCTO
    document.getElementById("btn-crear-producto").addEventListener("click", function () {
        modalTitulo.innerText = "Nuevo Producto";
        form.reset();
        idInput.value = "";
        requiereEnvaseCheckbox.checked = false;
        precioCompraInput.value = "0.00";
        precioVentaInput.value = "0.00";
        stockLlenosInput.value = 0;
        // Inicializamos flags de imagen
        quitarImagenInput.value = "false";
        imagenBase64Hidden.value = "";
        btnAgregarImagen.innerText = "Agregar Imagen";
        btnAgregarImagen.classList.remove("btn-success");
        btnAgregarImagen.classList.add("btn-secondary");
        btnLimpiarImagen.click(); // Limpiar imagen
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
                // Limpiar estado staged después de guardar exitosamente
                const productoId = idInput.value;
                limpiarEstadoImagenStaged(productoId);
                
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