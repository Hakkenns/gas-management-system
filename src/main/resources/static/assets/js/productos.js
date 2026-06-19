document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("form-producto");
    const modalTitulo = document.getElementById("modal-titulo-producto");

    // Elementos del formulario - Datos Generales
    const idInput = document.getElementById("producto-id");
    const nombreInput = document.getElementById("producto-nombre");
    const descripcionInput = document.getElementById("producto-descripcion");
    const categoriaDisplay = document.getElementById("producto-categoria-display");
    const categoriaSelect = document.getElementById("producto-categoria");
    const capacidadInput = document.getElementById("producto-capacidad");
    const unidadMedidaSelect = document.getElementById("producto-unidadMedida");
    const contenedorCapacidad = document.getElementById("contenedor-capacidad");
    const contenedorRequiereEnvase = document.getElementById("contenedor-requiereEnvase");
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

    // Elementos del formulario - Imagen
    const archivoImagenInput = document.getElementById("producto-archivo-imagen");
    const btnAgregarImagen = document.getElementById("btn-agregar-imagen");
    const btnLimpiarImagen = document.getElementById("btn-limpiar-imagen");
    const previewImagen = document.getElementById("preview-imagen");
    const textoSinImagen = document.getElementById("texto-sin-imagen");
    const quitarImagenInput = document.getElementById("producto-quitarImagen");
    const imagenBase64Hidden = document.getElementById("producto-imagenBase64");

    // Elementos del modal de búsqueda de categorías
    const btnBuscarCategoria = document.getElementById("btn-buscar-categoria");
    const modalBuscarCategoria = document.getElementById("modal-buscar-categoria");
    const inputBuscarCategoriaNombre = document.getElementById("input-buscar-categoria-nombre");
    const selectFiltroUnidadMedida = document.getElementById("select-filtro-unidad-medida");
    const btnFiltrarCategorias = document.getElementById("btn-filtrar-categorias");
    const tablaBusquedaCategorias = document.getElementById("tabla-busqueda-categorias");
    const contenedorTablaProductos = document.getElementById("contenedor-tabla-productos");

    const requiredNodes = [
        { name: 'form-producto', node: form },
        { name: 'modal-titulo-producto', node: modalTitulo },
        { name: 'producto-id', node: idInput },
        { name: 'producto-nombre', node: nombreInput },
        { name: 'producto-descripcion', node: descripcionInput },
        { name: 'producto-categoria', node: categoriaSelect },
        { name: 'producto-precioCompra', node: precioCompraInput },
        { name: 'producto-precioVenta', node: precioVentaInput },
        { name: 'producto-gananciaProducto', node: gananciaProductoInput },
        { name: 'producto-stockLlenos', node: stockLlenosInput },
        { name: 'producto-stockVacios', node: stockVaciosInput },
        { name: 'producto-stockMinimo', node: stockMinimoInput },
        { name: 'producto-requiereEnvase', node: requiereEnvaseCheckbox },
        { name: 'contenedor-tabla-productos', node: contenedorTablaProductos }
    ];

    const missingNodes = requiredNodes.filter(item => !item.node).map(item => item.name);
    if (missingNodes.length > 0) {
        console.warn('productos.js: faltan elementos DOM obligatorios:', missingNodes);
    }

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
        const unidad = unidadMedidaSelect.value;
        infoGananciaPor.innerText = unidad === "M" ? "Precio de ganancia por 1 metro" : "Precio de ganancia por unidad";
    }

    function actualizarMensajeStockMinimo() {
        if (!infoStockMinimo) return;
        const unidad = unidadMedidaSelect.value;
        infoStockMinimo.innerText = unidad === "M" ? "Alerta de stock bajo por metros" : "Alerta de stock bajo por unidad";
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

    function actualizarVisibilidadCampos() {
        // Obtener la fila de la categoría seleccionada de la tabla
        const categoriaId = categoriaSelect.value;
        if (!categoriaId) {
            contenedorCapacidad.style.display = "none";
            contenedorStockVacios.style.display = "none";
            unidadMedidaSelect.value = "";
            if (contenedorRequiereEnvase) {
                contenedorRequiereEnvase.style.display = "none";
                requiereEnvaseCheckbox.checked = false;
            }
            return;
        }

        // Buscar la fila en la tabla del modal con el ID seleccionado
        const filaCategoria = tablaBusquedaCategorias.querySelector(`tr[data-id="${categoriaId}"]`);
        if (!filaCategoria) {
            console.warn("Categoría no encontrada en la tabla");
            return;
        }

        // 1. Extraemos las banderas configuradas desde los data attributes
        const requiereCapacidad = filaCategoria.dataset.requierecapacidad === "true";
        const etiquetaCapacidad = filaCategoria.dataset.etiquetacapacidad || "Capacidad / Medida";
        const unidadMedida = filaCategoria.dataset.unidadmedida || "UND";
        const manejaEnvase = filaCategoria.dataset.manejaenvase === "true";

        // 2. Control del campo Unidad de Medida (Automático y Bloqueado)
        unidadMedidaSelect.value = unidadMedida;

        // 3. Control de Capacidad Dinámica
        if (requiereCapacidad) {
            contenedorCapacidad.style.display = "block";
            labelCapacidad.innerText = etiquetaCapacidad;
            capacidadInput.required = true;
        } else {
            contenedorCapacidad.style.display = "none";
            capacidadInput.value = "";
            capacidadInput.required = false;
        }

        // 4. Control de Envases Vacíos Dinámico
        if (manejaEnvase) {
            contenedorStockVacios.style.display = "block";
            stockVaciosInput.required = true;
        } else {
            contenedorStockVacios.style.display = "none";
            stockVaciosInput.value = 0;
            stockVaciosInput.required = false;
        }

        // 5. Actualizar mensajes informativos de la interfaz
        if (infoGananciaPor) {
            infoGananciaPor.innerText = unidadMedida === "M" ? "Precio de ganancia por 1 metro" : "Precio de ganancia por unidad";
        }
        if (infoStockMinimo) {
            infoStockMinimo.innerText = unidadMedida === "M" ? "Alerta de stock bajo por metros" : "Alerta de stock bajo por unidad";
        }

        // 6. Mostrar / ocultar el control de envase según el manejo de envases por categoría
        if (contenedorRequiereEnvase) {
            if (manejaEnvase) {
                contenedorRequiereEnvase.style.display = "block";
            } else {
                contenedorRequiereEnvase.style.display = "none";
                requiereEnvaseCheckbox.checked = false;
            }
        }
    }

    function actualizarRestriccionesCapacidad() {
        actualizarVisibilidadCampos();
    }

    function validarProducto() {
        if (!categoriaSelect.value) {
            alert("Por favor, seleccione una categoría.");
            return false;
        }
        if (!nombreInput.value.trim()) {
            alert("Por favor, ingrese el nombre del producto.");
            return false;
        }

        // Validación estricta de capacidad según unidad de medida
        const unidadActual = unidadMedidaSelect.value;
        if (unidadActual === "UND") {
            capacidadInput.value = "";
        } else {
            const capVal = parseFloat(capacidadInput.value) || 0;
            if (unidadActual === "KG" && capVal < 10) {
                alert("KG debe ser >= 10");
                return false;
            }
            if (unidadActual === "L" && capVal < 20) {
                alert("L debe ser >= 20");
                return false;
            }
            if (unidadActual === "M" && capVal < 50) {
                alert("M debe ser >= 50");
                return false;
            }
        }

        return true;
    }

    function actualizarGananciaEditable(esEditable) {
        if (!gananciaProductoInput) return;
        gananciaProductoInput.readOnly = !esEditable;
        gananciaProductoInput.style.backgroundColor = esEditable ? "#ffffff" : "#e9ecef";
    }

    // ============ MANEJADORES DEL MODAL DE BÚSQUEDA DE CATEGORÍAS ============

    // Abrir modal al hacer clic en el botón de búsqueda
    btnBuscarCategoria.addEventListener("click", function () {
        // Limpiar filtros
        inputBuscarCategoriaNombre.value = "";
        selectFiltroUnidadMedida.value = "";
        // Mostrar todas las filas
        tablaBusquedaCategorias.querySelectorAll("tbody tr").forEach(row => {
            row.style.display = "";
        });
        // Abrir modal
        if (window.jQuery) {
            jQuery("#modal-buscar-categoria").modal("show");
        }
    });

    // Filtrar categorías
    function filtrarCategorias() {
        const nombreFiltro = inputBuscarCategoriaNombre.value.toLowerCase();
        const unidadFiltro = selectFiltroUnidadMedida.value;

        tablaBusquedaCategorias.querySelectorAll("tbody tr").forEach(row => {
            const nombre = row.dataset.nombre.toLowerCase();
            const unidad = row.dataset.unidadmedida;

            const coincideNombre = nombre.includes(nombreFiltro);
            const coincideUnidad = !unidadFiltro || unidad === unidadFiltro;

            row.style.display = coincideNombre && coincideUnidad ? "" : "none";
        });
    }

    // Eventos para filtrar
    inputBuscarCategoriaNombre.addEventListener("keyup", filtrarCategorias);
    inputBuscarCategoriaNombre.addEventListener("change", filtrarCategorias);
    selectFiltroUnidadMedida.addEventListener("change", filtrarCategorias);
    // El botón de filtrar puede haber sido eliminado del DOM; añadir handler sólo si existe
    if (btnFiltrarCategorias) {
        btnFiltrarCategorias.addEventListener("click", filtrarCategorias);
    }

    // Seleccionar categoría desde el modal
    tablaBusquedaCategorias.addEventListener("click", function (e) {
        const btnSeleccionar = e.target.closest(".btn-seleccionar-categoria");
        if (btnSeleccionar) {
            const fila = btnSeleccionar.closest("tr");
            const categoriaId = fila.dataset.id;
            const categoriaNombre = fila.dataset.nombre;

            // Llenar el campo de texto y el hidden
            categoriaDisplay.value = categoriaNombre;
            categoriaSelect.value = categoriaId;

            // Disparar cambios
            actualizarVisibilidadCampos();
            actualizarMensajeGanancia();
            actualizarMensajeStockMinimo();

            // Cerrar modal
            if (window.jQuery) {
                jQuery("#modal-buscar-categoria").modal("hide");
            }
        }
    });

    // Eventos de cambio en unidad de medida
    unidadMedidaSelect.addEventListener("change", function () {
        actualizarRestriccionesCapacidad();
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
                document.querySelector(".custom-file-label").innerText = archivo.name;
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
            imagenBase64Hidden.value = evento.target.result;
            quitarImagenInput.value = "false";
            btnAgregarImagen.innerText = "Imagen agregada";
            btnAgregarImagen.classList.remove("btn-secondary");
            btnAgregarImagen.classList.add("btn-success");

            const productoId = idInput.value;
            guardarEstadoImagenStaged(productoId);

            alert("La imagen se cargó correctamente");
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
        quitarImagenInput.value = "true";
        imagenBase64Hidden.value = "";
        btnAgregarImagen.innerText = "Agregar Imagen";
        btnAgregarImagen.classList.remove("btn-success");
        btnAgregarImagen.classList.add("btn-secondary");

        const productoId = idInput.value;
        guardarEstadoImagenStaged(productoId);
    });

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        if (!validarProducto()) {
            return;
        }

        const formData = new FormData(form);
        formData.set("requiereEnvase", requiereEnvaseCheckbox.checked ? "true" : "false");

        fetch("/productos", {
            method: "POST",
            body: formData
        })
            .then(response => response.json())
            .then(res => {
                if (res.status === "OK") {
                    const productoId = idInput.value;
                    limpiarEstadoImagenStaged(productoId);
                    if (window.jQuery) window.jQuery("#modal-producto").modal("hide");
                    recargarTabla();
                } else {
                    alert("Atención: " + res.message);
                }
            })
            .catch(err => alert("Error al procesar la solicitud."));
    });

    document.addEventListener("click", function (e) {
        const editButton = e.target.closest(".btn-editar-producto");

        if (editButton) {
            modalTitulo.textContent = "Editar Producto";

            idInput.value = editButton.dataset.id;
            nombreInput.value = editButton.dataset.nombre;
            categoriaSelect.value = editButton.dataset.idcategoria;

            // Llenar el display con el nombre de la categoría
            const filaCategoria = tablaBusquedaCategorias.querySelector(`tr[data-id="${editButton.dataset.idcategoria}"]`);
            if (filaCategoria) {
                categoriaDisplay.value = filaCategoria.dataset.nombre;
            }

            precioCompraInput.value = editButton.dataset.preciocompra;
            precioVentaInput.value = editButton.dataset.precioventa;
            stockMinimoInput.value = editButton.dataset.stockminimo;

            // =====================================================================
            // 🌟 CORRECCIÓN VISUAL: Inyecta la Ganancia Base real en el modal
            // =====================================================================
            if (gananciaProductoInput) {
                // Lee el datasetganancia que le agregamos al botón amarillo en el HTML
                const gananciaDato = editButton.dataset.ganancia || "1.00";
                gananciaProductoInput.value = parseFloat(gananciaDato).toFixed(2);
            }
            // =====================================================================

            actualizarVisibilidadCampos();

            if (capacidadInput) capacidadInput.value = editButton.dataset.capacidad || "";
            if (stockVaciosInput) stockVaciosInput.value = editButton.dataset.stockvacios || 0;
            requiereEnvaseCheckbox.checked = editButton.dataset.requiereenvase === "true";

            calcularPrecioVenta();
            cargarEstadoImagenStaged(editButton.dataset.id);

            fetch(`/inventario-lotes/producto/${editButton.dataset.id}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error("No se pudo verificar el historial de lotes.");
                    }
                    return response.json();
                })
                .then(lotes => {
                    const tieneLotes = Array.isArray(lotes) && lotes.length > 0;
                    actualizarGananciaEditable(!tieneLotes);
                    if (window.jQuery) window.jQuery("#modal-producto").modal("show");
                })
                .catch(err => {
                    console.error(err);
                    alert("No se pudo verificar el historial de compras del producto. Por seguridad, intente nuevamente.");
                });
        }
    });


    document.getElementById("contenedor-tabla-productos").addEventListener("click", function (e) {
        const btnVerDescripcion = e.target.closest(".btn-ver-descripcion");
        if (btnVerDescripcion) {
            const nombreProducto = btnVerDescripcion.dataset.nombre;
            const descripcion = btnVerDescripcion.dataset.descripcion || "";
            const ganancia = btnVerDescripcion.dataset.ganancia;

            document.getElementById("modal-descripcion-titulo").innerText = "Detalle - " + nombreProducto;

            const gananciaSpan = document.getElementById("modal-ganancia-contenido");
            const g = parseFloat(ganancia);
            if (!isNaN(g)) {
                gananciaSpan.innerText = "S/ " + g.toFixed(2);
            } else {
                gananciaSpan.innerText = "S/ 0.00";
            }

            document.getElementById("modal-descripcion-contenido").innerText = descripcion;
            $("#modal-descripcion").modal("show");
        }

        const btnEstado = e.target.closest(".btn-estado-producto");
        if (btnEstado) {
            if (btnEstado.disabled || btnEstado.classList.contains("disabled")) {
                e.preventDefault();
                return false;
            }

            const id = btnEstado.dataset.id;
            const nuevoEstado = btnEstado.dataset.estado;

            fetch(`/productos/${id}/estado?estado=${nuevoEstado}`, { method: "POST" })
                .then(response => response.json())
                .then(res => {
                    if (res.status === "OK") {
                        const toastMensaje = document.getElementById("toast-mensaje");
                        if (toastMensaje) {
                            toastMensaje.innerText = nuevoEstado === "1"
                                ? "El producto ha sido activado correctamente."
                                : "El producto ha sido inactivado correctamente.";
                        }
                        if (window.jQuery) {
                            window.jQuery("#toast-estado").toast("show");
                        }
                        recargarTabla();
                    } else {
                        alert("Error al cambiar de estado: " + res.message);
                    }
                })
                .catch(err => alert("Error en el servidor al cambiar estado"));
        }

        const btnEliminar = e.target.closest(".btn-eliminar-producto, .btn-eliminar");
        // Si el botón está deshabilitado visualmente (gris por historial), cancelamos en seco
        if (!btnEliminar || btnEliminar.classList.contains("disabled") || btnEliminar.hasAttribute("disabled")) {
            return;
        }

        const idProducto = btnEliminar.dataset.id;
        const nombreProducto = btnEliminar.dataset.nombre;

        if (confirm("¿Está seguro de que desea eliminar el producto: " + nombreProducto + "?")) {
            fetch(`/productos/${idProducto}/eliminar`, {
                method: "POST"
            })
            .then(r => r.json())
            .then(res => {
                if (res.status === "OK") {
                    console.log("Producto eliminado correctamente en la base de datos.");
                    if (typeof reloadProductosTable === "function") {
                        reloadProductosTable();
                    } else {
                        window.location.reload();
                    }
                } else {
                    alert("Atención: " + res.message);
                }
            })
            .catch(err => {
                console.error("Error al eliminar el producto:", err);
                alert("No se pudo procesar la eliminación del producto.");
            });
        }
    });

    document.getElementById("btn-crear-producto").addEventListener("click", function () {
        modalTitulo.innerText = "Nuevo Producto";
        form.reset();
        categoriaDisplay.value = "";
        categoriaSelect.value = "";
        idInput.value = "";
        requiereEnvaseCheckbox.checked = false;
        precioCompraInput.value = "0.00";
        precioVentaInput.value = "0.00";
        stockLlenosInput.value = 0;
        quitarImagenInput.value = "false";
        imagenBase64Hidden.value = "";
        btnAgregarImagen.innerText = "Agregar Imagen";
        btnAgregarImagen.classList.remove("btn-success");
        btnAgregarImagen.classList.add("btn-secondary");
        btnLimpiarImagen.click();
        actualizarVisibilidadCampos();
        actualizarRestriccionesCapacidad();
        actualizarMensajeGanancia();
        actualizarMensajeStockMinimo();
        actualizarGananciaEditable(true);
        $("#modal-producto").modal("show");
    });

    function recargarTabla() {
        fetch("/productos/tabla")
            .then(response => response.text())
            .then(html => {
                document.getElementById("contenedor-tabla-productos").innerHTML = html;
            })
            .catch(err => console.error("Error al refrescar la tabla:", err));
    }

    // =========================================================================
    // 🌟 MÓDULO REACTIVO: GESTIÓN DE CATÁLOGO DE PROVEEDORES (NUEVO)
    // =========================================================================
    const modalCatalogo = document.getElementById("modal-catalogo-proveedores");
    const catalogoProdNombre = document.getElementById("catalogo-producto-nombre");
    const catalogoProdId = document.getElementById("catalogo-producto-id");
    const inputBuscarProv = document.getElementById("input-buscar-proveedor-catalogo");
    const checkTodosProv = document.getElementById("check-seleccionar-todos-proveedores");
    const tablaProvBody = document.querySelector("#tabla-proveedores-catalogo tbody");

    // 1. ESCUCHADOR DE CLICS GLOBAL (Detecta el botón de la lupa de Proveedores)
    document.addEventListener("click", function (e) {
        const btnCatalogo = e.target.closest(".btn-catalogo-proveedores");
        if (btnCatalogo) {
            const productoId = btnCatalogo.dataset.id;
            const productoNombre = btnCatalogo.dataset.nombre;

            // Seteamos los datos informativos en el modal
            if (catalogoProdId) catalogoProdId.value = productoId;
            if (catalogoProdNombre) catalogoProdNombre.textContent = productoNombre;

            // Reseteamos el buscador y desmarcamos el "Seleccionar Todos" visualmente
            if (inputBuscarProv) inputBuscarProv.value = "";
            if (checkTodosProv) checkTodosProv.checked = false;

            // Ponemos todos los checkboxes de la pantalla en falso antes de consultar
            document.querySelectorAll(".check-proveedor-item").forEach(chk => chk.checked = false);

            // LLAMADA AJAX: Consultamos al backend qué proveedores ya venden este producto
            fetch("/catalogo-proveedores/listarTodo")
                .then(r => r.json())
                .then(data => {
                    // Filtramos las asociaciones que correspondan exactamente a nuestro productoId
                    // Nota: Tu SimpleResponse envía idProducto numerico
                    data.forEach(item => {
                        if (item.idProducto == productoId) {
                            const checkbox = document.getElementById(`check-prov-${item.idProveedor}`);
                            if (checkbox) checkbox.checked = true;
                        }
                    });
                    actualizarEstadoCheckTodos();
                    ejecutarFiltradoProveedores(); // Muestra la lista limpia

                    // Abrimos el modal por código usando jQuery nativo de tu sistema
                    if (window.jQuery) {
                        window.jQuery("#modal-catalogo-proveedores").modal("show");
                    }
                })
                .catch(err => {
                    console.error("Error cargando catálogo:", err);
                    alert("No se pudo cargar el catálogo de proveedores actual.");
                });
        }
    });

    // 2. BUSCADOR EN TIEMPO REAL (Filtra proveedores al escribir sin usar botones)
    if (inputBuscarProv) {
        inputBuscarProv.addEventListener("input", ejecutarFiltradoProveedores);
    }

    function ejecutarFiltradoProveedores() {
        const query = inputBuscarProv.value.toLowerCase().trim();
        const filas = tablaProvBody.querySelectorAll("tr");

        filas.forEach(fila => {
            if (fila.cells.length < 3) return; // Salta la fila vacía
            const nombreProv = fila.cells[1].textContent.toLowerCase();
            const rucProv = fila.cells[2].textContent.toLowerCase();

            if (nombreProv.includes(query) || rucProv.includes(query)) {
                fila.style.display = "";
            } else {
                fila.style.display = "none";
            }
        });
    }

    // 3. SINCRONIZACIÓN REACTIVA EN TIEMPO REAL (Manejo de Checkboxes individuales)
    document.addEventListener("change", function (e) {
        const checkbox = e.target.closest(".check-proveedor-item");
        if (checkbox) {
            if (checkbox.dataset.bloqueado === "true") {
                return;
            }

            const idProducto = catalogoProdId.value;
            const idProveedor = checkbox.value;
            const intentandoMarcar = checkbox.checked;
            const estadoAnterior = !intentandoMarcar;

            checkbox.dataset.bloqueado = "true";
            checkbox.disabled = true;

            if (intentandoMarcar) {
                const formData = new FormData();
                formData.append("idProducto", idProducto);
                formData.append("idProveedor", idProveedor);

                fetch("/catalogo-proveedores/asociar", {
                    method: "POST",
                    body: formData
                })
                    .then(r => r.json())
                    .then(res => {
                        if (res.status !== "OK") {
                            checkbox.checked = estadoAnterior;
                            alert(res.message);
                        }
                    })
                    .catch(() => {
                        checkbox.checked = estadoAnterior;
                        alert("Error al intentar comunicar con el servidor.");
                    })
                    .finally(() => {
                        checkbox.disabled = false;
                        delete checkbox.dataset.bloqueado;
                    });

            } else {
                fetch(`/catalogo-proveedores/desasociar?idProveedor=${idProveedor}&idProducto=${idProducto}`, {
                    method: "POST"
                })
                    .then(r => r.json())
                    .then(res => {
                        if (res.status !== "OK") {
                            checkbox.checked = estadoAnterior;
                            alert(res.message);
                        }
                        actualizarEstadoCheckTodos();
                    })
                    .catch(() => {
                        checkbox.checked = estadoAnterior;
                        alert("Error de red al desasociar producto.");
                    })
                    .finally(() => {
                        checkbox.disabled = false;
                        delete checkbox.dataset.bloqueado;
                    });
            }
        }
    });

    // 4. CONTROL DEL CHECKBOX MAESTRO (Seleccionar / Deseleccionar todos de golpe)
    if (checkTodosProv) {
        checkTodosProv.addEventListener("change", function () {
            const estadoMaestro = checkTodosProv.checked;
            const checkboxesVisibles = Array.from(tablaProvBody.querySelectorAll("tr"))
                .filter(tr => tr.style.display !== "none") // Solo actúa sobre los que están filtrados en pantalla
                .map(tr => tr.querySelector(".check-proveedor-item"))
                .filter(chk => chk !== null);

            checkboxesVisibles.forEach(chk => {
                if (chk.checked !== estadoMaestro) {
                    chk.checked = estadoMaestro;
                    // Disparamos el evento change programáticamente para que se guarde en la BD solo
                    chk.dispatchEvent(new Event("change", { bubbles: true }));
                }
            });
        });
    }

    // Función auxiliar para mantener equilibrado el botón maestro de arriba
    function actualizarEstadoCheckTodos() {
        if (!checkTodosProv) return;
        const checks = Array.from(document.querySelectorAll(".check-proveedor-item"));
        if (checks.length === 0) { checkTodosProv.checked = false; return; }
        const todosMarcados = checks.every(chk => chk.checked);
        checkTodosProv.checked = todosMarcados;
    }

    // =========================================================================
    // 📦 MÓDULO REACTIVO: HISTORIAL DE LOTES Y CAMBIO DE PRECIOS (NUEVO)
    // =========================================================================
    const modalDesgloseLotes = document.getElementById("modal-desglose-lotes");
    const loteProdNombre = document.getElementById("lote-producto-nombre");
    const loteProdId = document.getElementById("lote-producto-id");
    const cuerpoTablaLotes = document.getElementById("cuerpo-tabla-lotes");
    const cuerpoTablaRollos = document.getElementById("cuerpo-tabla-rollos");

    document.addEventListener("click", function (e) {
        const btnVerLotes = e.target.closest(".btn-ver-lotes");
        if (btnVerLotes) {
            const productoId = btnVerLotes.dataset.id;
            const productoNombre = btnVerLotes.dataset.nombre;
            const unidadProducto = btnVerLotes.dataset.unidadmedida || "";

            if (loteProdId) loteProdId.value = productoId;
            if (loteProdNombre) loteProdNombre.textContent = productoNombre;

            if (cuerpoTablaLotes) {
                cuerpoTablaLotes.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center text-muted">
                            <i class="fas fa-spinner fa-spin"></i> Cargando historial de lotes...
                        </td>
                    </tr>`;
            }

            fetch(`/inventario-lotes/producto/${productoId}`)
                .then(r => {
                    if (!r.ok) throw new Error("Error de servidor");
                    return r.json();
                })
                .then(lotes => {
                    cuerpoTablaLotes.innerHTML = "";

                    if (lotes.length === 0) {
                        cuerpoTablaLotes.innerHTML = `
                            <tr>
                                <td colspan="9" class="text-center text-danger">
                                    Este producto no registra lotes de inventario (Sin compras).
                                </td>
                            </tr>`;
                        return;
                    }

                    lotes.forEach(l => {
                        const fila = document.createElement("tr");
                        const fechaIngreso = l.createdAt ? new Date(l.createdAt).toLocaleDateString() : "-";
                        const fechaAjuste = l.updatedAt ? new Date(l.updatedAt).toLocaleDateString() : "-";
                        const cantidadActual = Number(l.cantidadActual ?? 0);
                        const cantidadInicial = Number(l.cantidadInicial ?? 0);
                        let estadoLote = "-";
                        let acciones = "-";
                        const capacidadRollo = Number(l.metrosPorRollo ?? 60.00);

                        if (unidadProducto === "M") {
                            estadoLote = cantidadActual < cantidadInicial
                                ? '<span class="text-warning"><i class="fas fa-lock-open"></i> Abierto</span>'
                                : '<span class="text-muted"><i class="fas fa-lock"></i> Sellado</span>';

                            acciones = `<button type="button" class="btn btn-info btn-xs btn-ver-rollos" data-inicial="${l.cantidadInicial}" data-actual="${l.cantidadActual}" data-capacidad="${capacidadRollo}" title="Ver Desglose de Rollos"><i class="fas fa-scroll"></i></button>`;
                        }

                        fila.innerHTML = `
                            <td>${l.nombreProveedor}</td>
                            <td>${fechaIngreso}</td>
                            <td>S/ ${parseFloat(l.precioCompra).toFixed(2)}</td>
                            <td>
                                <div class="input-group input-group-sm" style="max-width: 120px;">
                                    <input type="number" step="0.10" class="form-control input-precio-lote" value="${parseFloat(l.precioVenta).toFixed(2)}">
                                    <div class="input-group-append">
                                        <button class="btn btn-success btn-guardar-precio-lote" data-id="${l.id}" type="button"><i class="fas fa-save"></i></button>
                                    </div>
                                </div>
                            </td>
                            <td><span class="badge badge-secondary">${l.cantidadInicial}</span></td>
                            <td><span class="badge ${cantidadActual > 0 ? 'badge-success' : 'badge-danger'}">${l.cantidadActual}</span></td>
                            <td class="font-weight-bold text-secondary">${unidadProducto}</td>
                            <td class="text-muted text-xs">${fechaAjuste}</td>
                            <td>${acciones}</td>
                        `;
                        cuerpoTablaLotes.appendChild(fila);
                    });

                    if (window.jQuery) {
                        window.jQuery("#modal-desglose-lotes").modal("show");
                    }
                })
                .catch(err => {
                    console.error(err);
                    alert("No se pudo cargar el desglose de lotes del producto.");
                });
        }

        const btnVerRollos = e.target.closest(".btn-ver-rollos");
        if (btnVerRollos) {
            const totalInicial = Number(btnVerRollos.dataset.inicial || 0);
            const totalActual = Number(btnVerRollos.dataset.actual || 0);
            const capacidad = Number(btnVerRollos.dataset.capacidad || 60);
            const cantidadRollos = Math.max(1, Math.ceil(totalInicial / capacidad));

            // 🧠 FÓRMULA DE RESTA INVERSA: Calculamos cuántos metros se vendieron en total
            let metrosVendidosTotales = totalInicial - totalActual; // Ej: 120 - 90 = 30 metros vendidos
            const filas = [];

            for (let index = 1; index <= cantidadRollos; index++) {
                const inicialRollo = index < cantidadRollos
                    ? capacidad
                    : totalInicial - capacidad * (cantidadRollos - 1);

                let actualRollo = inicialRollo; // Cada rollo empieza idealmente lleno

                if (metrosVendidosTotales > 0) {
                    if (metrosVendidosTotales >= inicialRollo) {
                        // Caso A: Los metros vendidos consumieron por completo este rollo (se agota a 0)
                        actualRollo = 0;
                        metrosVendidosTotales -= inicialRollo;
                    } else {
                        // Caso B: Los metros vendidos cortan una parte de este rollo (este es el rollo 'Abierto')
                        actualRollo = inicialRollo - metrosVendidosTotales;
                        metrosVendidosTotales = 0;
                    }
                } else {
                    // Caso C: No hay más metros vendidos, el rollo se queda intacto
                    actualRollo = inicialRollo;
                }

                let estadoRollo = "-";
                if (actualRollo === 0) {
                    estadoRollo = '<span class="text-danger"><i class="fas fa-times-circle"></i> Agotado</span>';
                } else if (actualRollo < inicialRollo) {
                    estadoRollo = '<span class="text-warning"><i class="fas fa-lock-open"></i> Abierto</span>';
                } else {
                    estadoRollo = '<span class="text-muted"><i class="fas fa-lock"></i> Sellado</span>';
                }

                filas.push(`
                    <tr>
                        <td>Rollo ${index}</td>
                        <td>${inicialRollo.toFixed(2)}</td>
                        <td>${actualRollo.toFixed(2)}</td>
                        <td>${estadoRollo}</td>
                    </tr>`);
            }

            if (cuerpoTablaRollos) {
                cuerpoTablaRollos.innerHTML = filas.join("");
            }

            if (window.jQuery) {
                window.jQuery("#modal-desglose-rollos").modal("show");
            }
            return;
        }

    
        const btnGuardarPrecio = e.target.closest(".btn-guardar-precio-lote");
        if (btnGuardarPrecio) {
            const idLote = btnGuardarPrecio.dataset.id;
            const inputPrecio = document.getElementById(`precio-input-${idLote}`);
            const nuevoPrecio = inputPrecio ? inputPrecio.value : "";

            if (!nuevoPrecio || parseFloat(nuevoPrecio) < 0) {
                alert("Por favor, ingrese un precio de venta válido.");
                return;
            }

            btnGuardarPrecio.disabled = true;

            const formData = new FormData();
            formData.append("precioVenta", nuevoPrecio);

            fetch(`/inventario-lotes/${idLote}/actualizar-precio`, {
                method: "POST",
                body: formData
            })
                .then(r => r.json())
                .then(res => {
                    if (res.status === "OK") {
                        btnGuardarPrecio.classList.remove("btn-success");
                        btnGuardarPrecio.classList.add("btn-primary");
                        setTimeout(() => {
                            btnGuardarPrecio.classList.remove("btn-primary");
                            btnGuardarPrecio.classList.add("btn-success");
                        }, 1000);

                        const productoId = document.getElementById("lote-producto-id")?.value;
                        if (productoId) {
                            const filaLote = btnGuardarPrecio.closest("tr");
                            const precioCompraCelda = filaLote ? filaLote.querySelector("td:nth-child(3)") : null;
                            if (precioCompraCelda) {
                                const textoPrecioCompra = precioCompraCelda.textContent.replace(/S\/?\s*/g, "").trim();
                                const precioCompra = parseFloat(textoPrecioCompra) || 0;
                                const precioVenta = parseFloat(nuevoPrecio) || 0;
                                const nuevaGanancia = precioVenta - precioCompra;
                                const detalleBtn = document.querySelector(`.btn-ver-descripcion[data-id="${productoId}"]`);
                                if (detalleBtn) {
                                    detalleBtn.dataset.ganancia = nuevaGanancia.toFixed(2);
                                }
                            }

                            const filaProducto = document.querySelector(`tr[data-id="${productoId}"]`);
                            if (filaProducto) {
                                const precioVentaCelda = filaProducto.querySelector("td.precio-venta");
                                if (precioVentaCelda) {
                                    precioVentaCelda.textContent = `S/ ${parseFloat(nuevoPrecio).toFixed(2)}`;
                                }
                            }
                        }
                    } else {
                        alert("Atención: " + res.message);
                    }
                })
                .catch(() => alert("Error de red al actualizar el precio del lote."))
                .finally(() => btnGuardarPrecio.disabled = false);
        }
    });
});