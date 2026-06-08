document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("form-producto");
    const modalTitulo = document.getElementById("modal-titulo-producto");

    // Elementos del formulario - Datos Generales
    const idInput = document.getElementById("producto-id");
    const nombreInput = document.getElementById("producto-nombre");
    const descripcionInput = document.getElementById("producto-descripcion");
    const categoriaSelect = document.getElementById("producto-categoria");
    const capacidadInput = document.getElementById("producto-capacidad");
    const unidadMedidaSelect = document.getElementById("producto-unidadMedida");
    const contenedorCapacidad = document.getElementById("contenedor-capacidad");
    const contenedorUnidad = document.getElementById("contenedor-unidad");
    const labelCapacidad = document.getElementById("label-capacidad");
    const precioCompraInput = document.getElementById("producto-precioCompra");
    const precioVentaInput = document.getElementById("producto-precioVenta");
    const gananciaProductoInput = document.getElementById("producto-gananciaProducto");
    const infoGananciaPor = document.getElementById("info-ganancia-por");
    const infoStockMinimo = document.getElementById("info-stock-minimo");
    const stockLlenosInput = document.getElementById("producto-stockLlenos");
    const stockVaciosInput = document.getElementById("producto-stockVacios");
    const stockMinimoInput = document.getElementById("producto-stockMinimo");
    const requiereEnvaseCheckbox = document.getElementById("producto-requiereEnvase");
    const contenedorStockVacios = document.getElementById("contenedor-stockVacios");

    // IDs de categorías
    const ID_GAS = 1;
    const ID_ACCESORIOS = 2;
    const ID_AGUA = 3;

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

    // Mensaje dinámico bajo Ganancia según categoría y unidad
    function actualizarMensajeGanancia() {
        if (!infoGananciaPor) return;
        const categoriaId = parseInt(categoriaSelect.value);
        const unidad = unidadMedidaSelect.value;

        if (categoriaId === ID_ACCESORIOS && unidad === "M") {
            infoGananciaPor.innerText = "Precio de ganancia por 1 metro";
        } else {
            infoGananciaPor.innerText = "Precio de ganancia por unidad";
        }
    }

    function actualizarMensajeStockMinimo() {
        if (!infoStockMinimo) return;
        const categoriaId = parseInt(categoriaSelect.value);
        const unidad = unidadMedidaSelect.value;

        if (categoriaId === ID_ACCESORIOS && unidad === "M") {
            infoStockMinimo.innerText = "Alerta de stock bajo por metros";
        } else {
            infoStockMinimo.innerText = "Alerta de stock bajo por unidad";
        }
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
        if (categoriaId === ID_ACCESORIOS) {
            contenedorStockVacios.style.display = "none";
            stockVaciosInput.value = 0;
            stockVaciosInput.required = false;
        } else {
            contenedorStockVacios.style.display = "block";
            stockVaciosInput.required = true;
        }
    }

    function actualizarVisibilidadCampos() {
        const categoriaId = parseInt(categoriaSelect.value);
        const infoUnidadBloqueadas = document.getElementById("info-unidad-bloqueadas");

        if (!categoriaId) {
            contenedorCapacidad.style.display = "none";
            unidadMedidaSelect.disabled = false;
            infoUnidadBloqueadas.style.display = "none";
            habilitarTodasOpciones();
            return;
        }

        // GAS DOMÉSTICO: Solo KG
        if (categoriaId === ID_GAS) {
            unidadMedidaSelect.value = "KG";
            contenedorCapacidad.style.display = "block";
            labelCapacidad.innerText = "Capacidad (kg)";
            infoUnidadBloqueadas.style.display = "block";
            bloquearOpcionesGas();
        }
        // AGUA: Solo L
        else if (categoriaId === ID_AGUA) {
            unidadMedidaSelect.value = "L";
            contenedorCapacidad.style.display = "block";
            labelCapacidad.innerText = "Capacidad (litros)";
            infoUnidadBloqueadas.style.display = "block";
            bloquearOpcionesAgua();
        }
        // ACCESORIOS: Solo M y NO_APLICA
        else if (categoriaId === ID_ACCESORIOS) {
            bloquearOpcionesAccesorios();
            if (unidadMedidaSelect.value !== "M" && unidadMedidaSelect.value !== "NO_APLICA") {
                unidadMedidaSelect.value = "NO_APLICA";
            }
            infoUnidadBloqueadas.style.display = "block";
            controlarCapacidadPorUnidad();
        }
        else {
            unidadMedidaSelect.disabled = false;
            contenedorCapacidad.style.display = "none";
            infoUnidadBloqueadas.style.display = "none";
            habilitarTodasOpciones();
        }
    }

    // Bloquear solo KG para Gas
    function bloquearOpcionesGas() {
        [...unidadMedidaSelect.options].forEach(op => {
            op.disabled = (op.value !== "" && op.value !== "KG");
        });
    }

    // Bloquear solo L para Agua
    function bloquearOpcionesAgua() {
        [...unidadMedidaSelect.options].forEach(op => {
            op.disabled = (op.value !== "" && op.value !== "L");
        });
    }

    // Bloquear KG y L para Accesorios
    function bloquearOpcionesAccesorios() {
        [...unidadMedidaSelect.options].forEach(op => {
            op.disabled = (op.value === "KG" || op.value === "L");
        });
    }

    // Habilitar todas las opciones
    function habilitarTodasOpciones() {
        [...unidadMedidaSelect.options].forEach(op => {
            op.disabled = false;
        });
    }

    // Controlar visibilidad de capacidad según unidad seleccionada
    function controlarCapacidadPorUnidad() {
        const categoriaTexto = categoriaSelect.options[categoriaSelect.selectedIndex]?.text?.trim();
        
        // Para Gas y Agua, siempre mostrar capacidad
        if (categoriaTexto === "Gas Domestico" || categoriaTexto === "Bidones de Agua") {
            contenedorCapacidad.style.display = "block";
            return;
        }

        // Para Accesorios, mostrar capacidad solo si es Metros
        if (categoriaTexto === "Accesorios") {
            if (unidadMedidaSelect.value === "M") {
                contenedorCapacidad.style.display = "block";
                labelCapacidad.innerText = "Medida (metros)";
            } else if (unidadMedidaSelect.value === "NO_APLICA") {
                contenedorCapacidad.style.display = "none";
                capacidadInput.value = "";
            }
        }
    }
    categoriaSelect.addEventListener("change", () => {

        actualizarVisibilidadStockVacios();

        actualizarVisibilidadCampos();
        actualizarMensajeGanancia();
        actualizarMensajeStockMinimo();
    });

    unidadMedidaSelect.addEventListener("change", function () {
        controlarCapacidadPorUnidad();
        actualizarMensajeGanancia();
        actualizarMensajeStockMinimo();
    });

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

            alert("La imagen se cargo correctamente");
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
                    unidadMedidaSelect.value = data.unidadMedida || "";
                    capacidadInput.value = data.capacidad != null ? data.capacidad : "";
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
                    actualizarVisibilidadCampos();
                    actualizarMensajeGanancia();
                    actualizarMensajeStockMinimo();
                    // Abrir modal usando la instancia jQuery de Bootstrap 4
                    $("#modal-producto").modal("show");
                })
                .catch(err => alert("Error: " + err.message));
        }

        // 📝️ BOTÓN VER DESCRIPCIÓN
        const btnVerDescripcion = e.target.closest(".btn-ver-descripcion");
        if (btnVerDescripcion) {
            const nombreProducto = btnVerDescripcion.dataset.nombre;
            const descripcion = btnVerDescripcion.dataset.descripcion || "";
            const ganancia = btnVerDescripcion.dataset.ganancia;

            document.getElementById("modal-descripcion-titulo").innerText = "Detalle - " + nombreProducto;

            // Mostrar ganancia con formato S/ 0.00
            const gananciaSpan = document.getElementById("modal-ganancia-contenido");
            const g = parseFloat(ganancia);
            if (!isNaN(g)) {
                gananciaSpan.innerText = "S/ " + g.toFixed(2);
            } else {
                gananciaSpan.innerText = "S/ 0.00";
            }

            // Mostrar descripción (vacío si no existe)
            document.getElementById("modal-descripcion-contenido").innerText = descripcion;
            $("#modal-descripcion").modal("show");
        }

        //BOTÓN CAMBIAR ESTADO (Activar / Inhabilitar)
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
        actualizarMensajeGanancia();
        actualizarMensajeStockMinimo();
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