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
    const contenedorEnvaseAsociado = document.getElementById("contenedor-envaseAsociado");
    const envaseSelect = document.getElementById("producto-envaseId");

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

    async function cargarCapacidadesCategoria(categoriaId, selectedCapacidad = "") {
        if (!categoriaId || !capacidadInput) {
            return;
        }

        capacidadInput.innerHTML = "";
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Seleccione una capacidad";
        placeholder.disabled = true;
        placeholder.selected = true;
        capacidadInput.appendChild(placeholder);

        try {
            const response = await fetch(`/categorias/${categoriaId}`);
            if (!response.ok) {
                throw new Error("No se pudo cargar la categoría");
            }
            const data = await response.json();
            const unidad = data.unidadMedida || unidadMedidaSelect.value || "";
            const etiqueta = data.etiquetaCapacidad || labelCapacidad.innerText || "Capacidad";
            labelCapacidad.innerText = etiqueta;

            const capacidades = Array.isArray(data.capacidades) ? data.capacidades : [];
            capacidades.sort((a, b) => parseFloat(a.valor) - parseFloat(b.valor));

            capacidades.forEach(cap => {
                const option = document.createElement("option");
                option.value = cap.valor;
                // Mostrar solo el número de capacidad — la unidad se muestra en el campo separado
                option.textContent = String(cap.valor);
                if (String(cap.valor) === String(selectedCapacidad)) {
                    option.selected = true;
                }
                capacidadInput.appendChild(option);
            });

            if (selectedCapacidad && !capacidades.some(cap => String(cap.valor) === String(selectedCapacidad))) {
                const option = document.createElement("option");
                option.value = selectedCapacidad;
                // No añadir sufijos como "(actual)", mostrar sólo el número
                option.textContent = String(selectedCapacidad);
                option.selected = true;
                capacidadInput.appendChild(option);
            }
        } catch (error) {
            console.error("Error cargando capacidades de categoría:", error);
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

    function actualizarVisibilidadCampos() {
        // Obtener la fila de la categoría seleccionada de la tabla
        const categoriaId = categoriaSelect.value;
        if (!categoriaId) {
            contenedorCapacidad.style.display = "none";
            capacidadInput.innerHTML = "";
            contenedorStockVacios.style.display = "none";
            unidadMedidaSelect.value = "";
            if (contenedorRequiereEnvase) {
                contenedorRequiereEnvase.style.display = "none";
                requiereEnvaseCheckbox.checked = false;
            }
            actualizarVisibilidadEnvase();
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
        cargarEnvases();

        // 3. Control de Capacidad Dinámica
        if (requiereCapacidad) {
            contenedorCapacidad.style.display = "block";
            labelCapacidad.innerText = etiquetaCapacidad;
            capacidadInput.required = true;
        } else {
            contenedorCapacidad.style.display = "none";
            capacidadInput.innerHTML = "";
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
        actualizarVisibilidadEnvase();
    }

    function cargarEnvases(envaseSeleccionado = "") {
        if (!envaseSelect) return;

        const unidadProducto = (unidadMedidaSelect.value || '').trim().toUpperCase();
        const seleccionSolicitada = envaseSeleccionado || envaseSelect.value;

        Array.from(envaseSelect.options).forEach(option => {
            // La opción "Sin envase" debe permanecer siempre disponible.
            if (!option.value) {
                option.hidden = false;
                option.disabled = false;
                return;
            }

            const unidadEnvase = (option.dataset.unidadMedida || '').trim().toUpperCase();
            const coincideUnidad = Boolean(unidadProducto) && unidadEnvase === unidadProducto;
            option.hidden = !coincideUnidad;
            option.disabled = !coincideUnidad;
        });

        const opcionSeleccionada = Array.from(envaseSelect.options).find(option =>
            option.value === String(seleccionSolicitada) && !option.hidden
        );
        envaseSelect.value = opcionSeleccionada ? seleccionSolicitada : '';
    }

    function actualizarVisibilidadEnvase(envaseSeleccionado = "") {
        if (!contenedorEnvaseAsociado || !envaseSelect || !requiereEnvaseCheckbox) return;

        const requiereEnvase = requiereEnvaseCheckbox.checked;
        contenedorEnvaseAsociado.style.display = requiereEnvase ? 'block' : 'none';
        envaseSelect.disabled = !requiereEnvase;
        envaseSelect.required = requiereEnvase;

        if (requiereEnvase) {
            cargarEnvases(envaseSeleccionado);
        } else {
            envaseSelect.value = '';
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
        return true;
    }

    function actualizarGananciaEditable(esEditable) {
        if (!gananciaProductoInput) return;
        gananciaProductoInput.readOnly = !esEditable;
        gananciaProductoInput.style.backgroundColor = esEditable ? "#ffffff" : "#e9ecef";
    }

    // Bloquear la mayoría de campos cuando se entra en modo 'Editar producto'
    function aplicarRestriccionesEdicionProducto() {
        // Campos que se permiten editar: stockVacios, stockMinimo, descripcion
        // Dejar imagenes y pestaña de imagen intacta

        // Campos a bloquear (usar readonly en lugar de disabled para que FormData los lea)
        const camposBloquear = [
            nombreInput,
            categoriaSelect,
            capacidadInput,
            unidadMedidaSelect,
            precioCompraInput,
            precioVentaInput,
            gananciaProductoInput,
            requiereEnvaseCheckbox,
            document.getElementById('btn-buscar-categoria')
        ];

        camposBloquear.forEach(node => {
            if (!node) return;
            try {
                node.readOnly = true;
                node.style.backgroundColor = '#e9ecef';
            } catch (e) {}
        });

        // Asegurar que los campos permitidos estén habilitados
        if (stockVaciosInput) { stockVaciosInput.readOnly = false; stockVaciosInput.style.backgroundColor = ''; }
        if (stockMinimoInput) { stockMinimoInput.readOnly = false; stockMinimoInput.style.backgroundColor = ''; }
        if (descripcionInput) descripcionInput.readOnly = false;
    }

    function habilitarCamposPorDefecto() {
        // Restaurar el estado esperado para CREAR (como estaba en la plantilla):
        // - permitir editar: nombre, categoría, capacidad, ganancia, stockVacios, stockMinimo, descripcion, requiereEnvase
        // - mantener readonly: precioCompra, precioVenta, unidadMedida

        if (nombreInput) { nombreInput.readOnly = false; nombreInput.style.backgroundColor = ''; }
        if (categoriaSelect) { categoriaSelect.readOnly = false; categoriaSelect.style.backgroundColor = ''; }
        if (capacidadInput) { capacidadInput.readOnly = false; capacidadInput.style.backgroundColor = ''; }
        if (gananciaProductoInput) { gananciaProductoInput.readOnly = false; gananciaProductoInput.style.backgroundColor = ''; }
        if (stockVaciosInput) { stockVaciosInput.readOnly = false; stockVaciosInput.style.backgroundColor = ''; }
        if (stockMinimoInput) { stockMinimoInput.readOnly = false; stockMinimoInput.style.backgroundColor = ''; }
        if (descripcionInput) { descripcionInput.readOnly = false; descripcionInput.style.backgroundColor = ''; }
        if (requiereEnvaseCheckbox) { requiereEnvaseCheckbox.readOnly = false; }

        // Mantener precio y unidad readonly (como en la plantilla)
        if (precioCompraInput) { precioCompraInput.readOnly = true; precioCompraInput.style.backgroundColor = '#e9ecef'; }
        if (precioVentaInput) { precioVentaInput.readOnly = true; precioVentaInput.style.backgroundColor = '#e9ecef'; }
        if (unidadMedidaSelect) { unidadMedidaSelect.readOnly = true; unidadMedidaSelect.style.backgroundColor = '#e9ecef'; }

        // Rehabilitar el botón de buscar categoría
        const btnBuscar = document.getElementById('btn-buscar-categoria');
        if (btnBuscar) btnBuscar.disabled = false;
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
            cargarCapacidadesCategoria(categoriaId);
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
        cargarEnvases();
        actualizarRestriccionesCapacidad();
        actualizarMensajeGanancia();
        actualizarMensajeStockMinimo();
    });

    requiereEnvaseCheckbox.addEventListener("change", function () {
        actualizarVisibilidadEnvase();
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
                    // Recargar tabla preservando la página actual
                    recargarTabla(true);
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
            descripcionInput.value = editButton.dataset.descripcion || "";
            categoriaSelect.value = editButton.dataset.idcategoria;
            const categoriaId = editButton.dataset.idcategoria;

            // Llenar el display con el nombre de la categoría
            const filaCategoria = tablaBusquedaCategorias.querySelector(`tr[data-id="${categoriaId}"]`);
            if (filaCategoria) {
                categoriaDisplay.value = filaCategoria.dataset.nombre;
            }

            precioCompraInput.value = editButton.dataset.preciocompra;
            precioVentaInput.value = editButton.dataset.precioventa;
            stockMinimoInput.value = editButton.dataset.stockminimo;
            // Asegurar que el campo de stock llenos muestre el valor real (el template no lo incluía antes)
            if (stockLlenosInput) stockLlenosInput.value = editButton.dataset.stockllenos || 0;

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

            if (capacidadInput) {
                cargarCapacidadesCategoria(categoriaId, editButton.dataset.capacidad || "");
            }
            if (stockVaciosInput) stockVaciosInput.value = editButton.dataset.stockvacios || 0;
            requiereEnvaseCheckbox.checked = editButton.dataset.requiereenvase === "true";
            if (requiereEnvaseCheckbox.checked) {
                fetch(`/productos/${editButton.dataset.id}/envase`)
                    .then(response => response.ok ? response.json() : { envaseId: null })
                    .then(data => actualizarVisibilidadEnvase(data.envaseId || ""))
                    .catch(() => actualizarVisibilidadEnvase());
            } else {
                actualizarVisibilidadEnvase();
            }

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
                    // Aplicar restricciones de edición: bloquear campos que no deben modificarse
                    aplicarRestriccionesEdicionProducto();
                    // Forzar carga de capacidad seleccionada después de cargar las opciones
                    if (capacidadInput && editButton.dataset.capacidad) {
                        setTimeout(() => {
                            capacidadInput.value = editButton.dataset.capacidad;
                        }, 50);
                    }
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
                        // Recargar tabla preservando página actual
                        recargarTabla(true);
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
        actualizarVisibilidadEnvase();
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
        // Asegurar que los campos estén habilitados en creación
        habilitarCamposPorDefecto();
        $("#modal-producto").modal("show");
    });

    function obtenerFilaProducto(productoId) {
        return document.querySelector(`#tabla-productos tbody tr[data-id="${productoId}"]`);
    }

    function mapUnidadMedida(unidad) {
        return unidad === 'KG' ? 'KG'
            : unidad === 'L' ? 'Litros'
            : unidad === 'M' ? 'Metros'
            : unidad === 'UND' ? 'UND'
            : '-';
    }

    function formatMoney(valor) {
        const numero = parseFloat(valor);
        return Number.isFinite(numero) ? `S/ ${numero.toFixed(2)}` : 'S/ 0.00';
    }

    function renderEnvaseBadge(requiereEnvase) {
        return requiereEnvase
            ? '<span class="badge badge-info">SÍ</span>'
            : '<span class="badge badge-secondary">NO</span>';
    }

    function renderEstadoBadge(estado) {
        return estado === '1' || estado === 1
            ? '<span class="badge badge-success">ACTIVO</span>'
            : '<span class="badge badge-danger">INACTIVO</span>';
    }

    function redrawRow(row) {
        if (window.jQuery && $.fn.DataTable && $.fn.dataTable.isDataTable('#tabla-productos')) {
            const dataTable = $('#tabla-productos').DataTable();
            dataTable.row(row).invalidate('dom').draw(false);
        }
    }

    function actualizarFilaProducto(productoId, datos) {
        const row = obtenerFilaProducto(productoId);
        if (!row) {
            return;
        }

        const cells = row.cells;

        if (datos.nombre !== undefined) {
            cells[2].textContent = datos.nombre;
        }
        if (datos.nombreCategoria !== undefined) {
            cells[3].textContent = datos.nombreCategoria;
        }
        if (datos.capacidad !== undefined) {
            cells[4].textContent = datos.capacidad || '-';
        }
        if (datos.unidadMedida !== undefined) {
            cells[5].textContent = mapUnidadMedida(datos.unidadMedida);
        }
        if (datos.precioCompra !== undefined) {
            cells[6].textContent = formatMoney(datos.precioCompra);
        }
        if (datos.precioVenta !== undefined) {
            cells[7].textContent = formatMoney(datos.precioVenta);
        }
        if (datos.stockLlenos !== undefined) {
            cells[8].textContent = datos.stockLlenos;
        }
        if (datos.stockVacios !== undefined) {
            cells[9].textContent = datos.stockVacios;
        }
        if (datos.stockMinimo !== undefined) {
            cells[10].textContent = datos.stockMinimo;
        }
        if (datos.requiereEnvase !== undefined) {
            cells[11].innerHTML = renderEnvaseBadge(datos.requiereEnvase);
        }

        const editButton = row.querySelector('.btn-editar-producto');
        if (editButton) {
            if (datos.nombre !== undefined) editButton.dataset.nombre = datos.nombre;
            if (datos.idCategoria !== undefined) editButton.dataset.idcategoria = datos.idCategoria;
            if (datos.capacidad !== undefined) editButton.dataset.capacidad = datos.capacidad;
            if (datos.unidadMedida !== undefined) editButton.dataset.unidadmedida = datos.unidadMedida;
            if (datos.precioCompra !== undefined) editButton.dataset.preciocompra = datos.precioCompra;
            if (datos.precioVenta !== undefined) editButton.dataset.precioventa = datos.precioVenta;
            if (datos.stockLlenos !== undefined) editButton.dataset.stockllenos = datos.stockLlenos;
            if (datos.stockMinimo !== undefined) editButton.dataset.stockminimo = datos.stockMinimo;
            if (datos.stockVacios !== undefined) editButton.dataset.stockvacios = datos.stockVacios;
            if (datos.requiereEnvase !== undefined) editButton.dataset.requiereenvase = datos.requiereEnvase;
            if (datos.ganancia !== undefined) editButton.dataset.ganancia = datos.ganancia;
        }

        const descButton = row.querySelector('.btn-ver-descripcion');
        if (descButton) {
            if (datos.nombre !== undefined) descButton.dataset.nombre = datos.nombre;
            if (datos.descripcion !== undefined) descButton.dataset.descripcion = datos.descripcion;
            if (datos.ganancia !== undefined) descButton.dataset.ganancia = datos.ganancia;
        }

        const catalogoButton = row.querySelector('.btn-catalogo-proveedores');
        if (catalogoButton && datos.nombre !== undefined) {
            catalogoButton.dataset.nombre = datos.nombre;
        }

        const verLotesButton = row.querySelector('.btn-ver-lotes');
        if (verLotesButton && datos.nombre !== undefined) {
            verLotesButton.dataset.nombre = datos.nombre;
        }

        const stateCell = cells[12];
        const currentEstado = datos.estado !== undefined ? datos.estado : stateCell.textContent.includes('ACTIVO') ? '1' : '0';
        stateCell.innerHTML = renderEstadoBadge(currentEstado);

        const btnEliminar = row.querySelector('.btn-eliminar-producto');
        if (btnEliminar && datos.stockLlenos !== undefined) {
            const hasStock = parseInt(datos.stockLlenos, 10) > 0;
            btnEliminar.disabled = hasStock;
            btnEliminar.classList.toggle('disabled', hasStock);
            btnEliminar.classList.toggle('btn-secondary', hasStock);
            btnEliminar.classList.toggle('btn-danger', !hasStock);
            btnEliminar.title = hasStock
                ? 'No se puede eliminar por historial de inventario'
                : 'Eliminar';
        }

        const btnEstado = row.querySelector('.btn-estado-producto');
        if (btnEstado) {
            const estado = datos.estado !== undefined ? datos.estado.toString() : currentEstado;
            if (estado === '1') {
                btnEstado.dataset.estado = '0';
                btnEstado.className = 'btn btn-secondary btn-xs btn-estado-producto';
                const hasStock = datos.stockLlenos !== undefined ? parseInt(datos.stockLlenos, 10) > 0 : false;
                btnEstado.disabled = hasStock;
                btnEstado.classList.toggle('disabled', hasStock);
                btnEstado.title = hasStock
                    ? 'No se puede inactivar: Aún cuenta con stock disponible en el inventario'
                    : 'Inhabilitar';
                btnEstado.innerHTML = '<i class="fas fa-ban"></i>';
            } else {
                btnEstado.dataset.estado = '1';
                btnEstado.className = 'btn btn-success btn-xs btn-estado-producto';
                btnEstado.disabled = false;
                btnEstado.title = 'Activar';
                btnEstado.innerHTML = '<i class="fas fa-check"></i>';
            }
        }

        redrawRow(row);
    }

    function actualizarEstadoFila(productoId, nuevoEstado) {
        const row = obtenerFilaProducto(productoId);
        if (!row) {
            return;
        }

        const cells = row.cells;
        cells[12].innerHTML = renderEstadoBadge(nuevoEstado);

        const btnEstado = row.querySelector('.btn-estado-producto');
        if (btnEstado) {
            if (nuevoEstado === '1') {
                btnEstado.dataset.estado = '0';
                btnEstado.className = 'btn btn-secondary btn-xs btn-estado-producto';
                const stockLlenos = parseInt(cells[8].textContent, 10) || 0;
                const disabled = stockLlenos > 0;
                btnEstado.disabled = disabled;
                btnEstado.classList.toggle('disabled', disabled);
                btnEstado.title = disabled
                    ? 'No se puede inactivar: Aún cuenta con stock disponible en el inventario'
                    : 'Inhabilitar';
                btnEstado.innerHTML = '<i class="fas fa-ban"></i>';
            } else {
                btnEstado.dataset.estado = '1';
                btnEstado.className = 'btn btn-success btn-xs btn-estado-producto';
                btnEstado.disabled = false;
                btnEstado.title = 'Activar';
                btnEstado.innerHTML = '<i class="fas fa-check"></i>';
            }
        }

        redrawRow(row);
    }

    function recargarTabla(preservarPagina = false) {
        const contenedor = document.getElementById("contenedor-tabla-productos");
        if (!contenedor) return;

        // Capturar página actual antes de destruir
        let paginaActual = 0;
        if (preservarPagina && window.jQuery && $.fn.DataTable && $.fn.dataTable.isDataTable('#tabla-productos')) {
            paginaActual = $('#tabla-productos').DataTable().page.info().page;
        }

        const oldWrapper = contenedor.querySelector('.dataTables_wrapper');
        if (oldWrapper) {
            oldWrapper.remove();
        }

        const oldTable = contenedor.querySelector('#tabla-productos');
        if (oldTable && window.jQuery && $.fn.DataTable && $.fn.dataTable.isDataTable(oldTable)) {
            $(oldTable).DataTable().clear().destroy();
        }

        fetch("/productos/tabla")
            .then(response => response.text())
            .then(html => {
                contenedor.innerHTML = html;

                if (window.jQuery && $.fn.DataTable) {
                    initTablaProductos();
                    // Restaurar página si se solicitó
                    if (preservarPagina && paginaActual > 0) {
                        setTimeout(() => {
                            if ($.fn.dataTable.isDataTable('#tabla-productos')) {
                                $('#tabla-productos').DataTable().page(paginaActual).draw(false);
                            }
                        }, 100);
                    }
                }
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

// Inicialización de DataTables para Productos
function initTablaProductos() {
    if ($.fn.DataTable) {
        const table = $('#tabla-productos');
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().clear().destroy();
            table.removeClass('dataTable');
            table.removeAttr('style');
        }

        table.css('width', '100%');

        const dataTable = table.DataTable({
            dom: 'rt<"bottom"ip><"clear">',
            paging: true,
            pageLength: 10,
            lengthMenu: [[10, 20, 30, 40, 50], [10, 20, 30, 40, 50]],
            lengthChange: true,
            searching: true,
            ordering: true,
            info: true,
            autoWidth: false,
            responsive: true,
            pagingType: 'simple',
            language: {
                search: 'Buscar:',
                lengthMenu: 'Mostrar _MENU_ registros',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ registros',
                infoEmpty: 'No hay registros',
                infoFiltered: '(filtrado de _MAX_ registros)',
                zeroRecords: 'No se encontraron resultados',
                paginate: {
                    previous: 'Anterior',
                    next: 'Siguiente'
                }
            }
        });

        // Forzar reajuste de columnas al cambiar el tamaño de la ventana o zoom
        $(window).off('resize.productosTable');
        $(window).on('resize.productosTable', function () {
            dataTable.columns.adjust().draw();
        });

        // Asegurar que el ancho de la tabla se recalcula correctamente al inicializar
        dataTable.columns.adjust().draw();

        // Conectar controles personalizados
        const $lengthSelect = $('#productos-length');
        const $searchInput = $('#productos-search');

        // Cambiar número de registros por página
        if ($lengthSelect.length) {
            $lengthSelect.on('change', function () {
                const pageLength = parseInt($(this).val(), 10);
                dataTable.page.len(pageLength).draw();
            });
        }

        // Búsqueda personalizada
        if ($searchInput.length) {
            $searchInput.on('keyup', function () {
                dataTable.search(this.value).draw();
            });
        }

        // Ocultar controles nativos duplicados de DataTables
        const $wrapper = table.closest('.dataTables_wrapper');
        if ($wrapper.length) {
            const wrapperEl = $wrapper[0];
            const paginateContainer = wrapperEl.querySelector('.dataTables_paginate');
            if (paginateContainer) {
                paginateContainer.style.display = 'none';
            }

            const defaultInfo = wrapperEl.querySelector('.dataTables_info');
            if (defaultInfo) {
                defaultInfo.style.display = 'none';
            }

            const pagerRow = document.createElement('div');
            pagerRow.className = 'compras-pager-row';
            wrapperEl.appendChild(pagerRow);

            const infoBar = document.createElement('div');
            infoBar.className = 'compras-info-bar';
            pagerRow.appendChild(infoBar);

            const customPager = document.createElement('div');
            customPager.className = 'compras-custom-pagination';
            customPager.setAttribute('aria-label', 'Paginación de productos');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStyles();
            renderCustomInfo(dataTable, infoBar);
            renderCustomPagination(dataTable, customPager);
            attachSwipePagination(wrapperEl, dataTable);

            dataTable.on('draw.dt', () => {
                renderCustomInfo(dataTable, infoBar);
                renderCustomPagination(dataTable, customPager);
            });
        }
    }
}

// Inicializar tabla cuando se carga la página
document.addEventListener('DOMContentLoaded', function() {
    if ($.fn.DataTable) {
        initTablaProductos();
    }
});

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStyles() {
    if (document.getElementById('compras-custom-pagination-style')) {
        return;
    }

    const style = document.createElement('style');
    style.id = 'compras-custom-pagination-style';
    style.textContent = `
        .compras-info-bar {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            margin: 10px 0 6px;
            font-size: 0.95rem;
            color: #495057;
            font-weight: 600;
            visibility: visible !important;
            opacity: 1 !important;
            flex: 1;
        }
        .compras-pager-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            margin-top: 4px;
            gap: 12px;
        }
        .compras-custom-pagination {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 4px;
            padding: 6px 0;
            flex-wrap: nowrap;
            white-space: nowrap;
            overflow: hidden;
            visibility: visible !important;
            opacity: 1 !important;
        }
        .compras-custom-pagination .page-btn {
            min-width: 38px;
            height: 38px;
            padding: 0 10px;
            border: 1px solid #ced4da;
            border-radius: 6px;
            background: #fff;
            color: #343a40;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            cursor: pointer;
            line-height: 1;
        }
        .compras-custom-pagination .page-btn.active {
            background: #0d6efd;
            color: #fff;
            border-color: #0d6efd;
        }
        .compras-custom-pagination .page-btn:disabled {
            opacity: 0.65;
            cursor: not-allowed;
        }
        .compras-custom-pagination .page-btn:hover:not(:disabled) {
            background: #e9ecef;
        }
        /* Ocultar controles nativos duplicados de DataTables */
        .dataTables_wrapper .dataTables_length:not(:first-of-type),
        .dataTables_wrapper .dataTables_filter:not(:first-of-type) {
            display: none !important;
        }
    `;
    document.head.appendChild(style);
}

function renderCustomInfo(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPagination(dataTable, pagerElement) {
    const info = dataTable.page.info();
    const totalPages = info.pages;
    const currentPage = info.page;

    pagerElement.innerHTML = '';

    if (totalPages <= 1) {
        pagerElement.style.display = 'none';
        return;
    }

    pagerElement.style.display = 'flex';
    pagerElement.style.visibility = 'visible';
    pagerElement.style.opacity = '1';

    const createPageButton = (label, pageIndex, isActive = false, disabled = false) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = label;
        button.className = `page-btn${isActive ? ' active' : ''}`;
        button.setAttribute('aria-current', isActive ? 'page' : 'false');

        if (disabled) {
            button.disabled = true;
        } else {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                if (document.activeElement instanceof HTMLElement) {
                    document.activeElement.blur();
                }
                dataTable.page(pageIndex).draw(false);
            });
        }

        return button;
    };

    const prevButton = createPageButton('‹', Math.max(0, currentPage - 1), false, currentPage === 0);
    prevButton.setAttribute('aria-label', 'Página anterior');
    pagerElement.appendChild(prevButton);

    const startPage = Math.max(0, Math.min(currentPage - 1, totalPages - 3));
    const endPage = Math.min(totalPages - 1, startPage + 2);

    for (let pageIndex = startPage; pageIndex <= endPage; pageIndex += 1) {
        const pageButton = createPageButton(String(pageIndex + 1), pageIndex, pageIndex === currentPage);
        pagerElement.appendChild(pageButton);
    }

    const nextButton = createPageButton('›', Math.min(totalPages - 1, currentPage + 1), false, currentPage >= totalPages - 1);
    nextButton.setAttribute('aria-label', 'Página siguiente');
    pagerElement.appendChild(nextButton);
}

function attachSwipePagination(wrapperElement, dataTable) {
    let touchStartX = 0;

    wrapperElement.addEventListener('touchstart', (event) => {
        touchStartX = event.touches[0].clientX;
    }, { passive: true });

    wrapperElement.addEventListener('touchend', (event) => {
        const touchEndX = event.changedTouches[0].clientX;
        const deltaX = touchEndX - touchStartX;

        if (Math.abs(deltaX) < 50) {
            return;
        }

        if (deltaX < 0) {
            dataTable.page('next').draw(false);
        } else {
            dataTable.page('previous').draw(false);
        }
    }, { passive: true });
}
