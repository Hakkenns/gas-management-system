document.addEventListener("DOMContentLoaded", function () {
    const modalTitle = document.getElementById("modal-titulo-categoria");
    const form = document.getElementById("form-categoria");
    const idField = document.getElementById("categoria-id");
    const nombreField = document.getElementById("nombre-categoria");
    const descripcionField = document.getElementById("descripcion-categoria");
    // NUEVO: Capturamos el ComboBox del tipo de unidad
    const tipoUnidadField = document.getElementById("tipoUnidad");
    const tipoUnidadHiddenField = document.getElementById("tipoUnidad-hidden");
    const btnCreate = document.getElementById("btn-crear-categoria");

    // Elementos de capacidades
    const contenedorCapacidades = document.getElementById("contenedor-capacidades-categoria");
    const inputNuevaCapacidad = document.getElementById("input-nueva-capacidad");
    const btnAgregarCapacidad = document.getElementById("btn-agregar-capacidad");
    const listaCapacidades = document.getElementById("lista-capacidades-categoria");
    const capacidadesJsonInput = document.getElementById("capacidades-json");

    let capacidadesActuales = [];
    let unidadActual = ""; // "KG", "L", "M" o ""

    // Elemento para mensaje de validación
    const mensajeValidacion = document.createElement("small");
    mensajeValidacion.className = "form-text text-danger mt-1";
    mensajeValidacion.id = "mensaje-validacion-capacidad";
    mensajeValidacion.style.display = "none";
    inputNuevaCapacidad.parentNode.parentNode.appendChild(mensajeValidacion);

    function obtenerLimites(unidad) {
        switch (unidad) {
            case "KG": return { min: 3, max: 45, decimales: 0, label: "kg (3 a 45)" };
            case "L":  return { min: 3, max: 21, decimales: 2, label: "litros (3 a 21)" };
            case "M":  return { min: 20, max: 100, decimales: 2, label: "metros (20 a 100)" };
            default:   return null;
        }
    }

    function tipoUnidadLabelPorCodigo(unidad) {
        switch (unidad) {
            case "KG": return "POR KILOS (KG)";
            case "L":  return "POR LITROS (L)";
            case "M":  return "POR METROS (M)";
            default:    return "POR UNIDADES / PIEZAS";
        }
    }

    function tipoUnidadCodigoPorLabel(label) {
        switch (label) {
            case "POR KILOS (KG)": return "KG";
            case "POR LITROS (L)": return "L";
            case "POR METROS (M)": return "M";
            default: return "";
        }
    }

    function renderizarCapacidades() {
        listaCapacidades.innerHTML = "";
        const limites = obtenerLimites(unidadActual);
        capacidadesActuales.forEach((item, index) => {
            const valor = item.valor;
            const enUso = item.enUso;
            const texto = limites && limites.decimales === 0 ? valor.toFixed(0) : valor.toFixed(2);
            const badge = document.createElement("span");
            if (enUso) {
                badge.className = "badge badge-danger p-2 mr-1 mb-1";
                badge.style.fontSize = "14px";
                badge.textContent = texto;
                badge.title = "Capacidad en uso (no se puede eliminar)";
            } else {
                badge.className = "badge badge-info p-2 mr-1 mb-1 d-inline-flex align-items-center";
                badge.style.fontSize = "14px";
                badge.innerHTML = `${texto} <i class="fas fa-times ml-1 remove-capacity" style="cursor: pointer; font-size: 14px;" data-index="${index}"></i>`;
                badge.querySelector(".remove-capacity").addEventListener("click", function() {
                    capacidadesActuales.splice(parseInt(this.dataset.index), 1);
                    renderizarCapacidades();
                });
            }
            listaCapacidades.appendChild(badge);
        });
        const valores = capacidadesActuales.map(item => item.valor.toFixed(limites && limites.decimales === 0 ? 0 : 2));
        capacidadesJsonInput.value = valores.join(",");
    }

    function mostrarMensaje(texto) {
        mensajeValidacion.textContent = texto;
        mensajeValidacion.style.display = "block";
        setTimeout(() => { mensajeValidacion.style.display = "none"; }, 3000);
    }

    function agregarCapacidad() {
        const limites = obtenerLimites(unidadActual);
        if (!limites) return;

        const valorRaw = inputNuevaCapacidad.value.trim();
        if (valorRaw === "") {
            Swal.fire({ icon: "warning", title: "Campo vacío", text: `Ingrese un valor entre ${limites.min} y ${limites.max} ${limites.label}.`, confirmButtonColor: "#28a745" });
            return;
        }

        const valor = parseFloat(valorRaw);
        if (!limites) {
            Swal.fire({ icon: "error", title: "Tipo de unidad inválido", text: "Seleccione primero una forma de venta válida para agregar capacidades.", confirmButtonColor: "#28a745" });
            return;
        }
        if (isNaN(valor) || valor < limites.min || valor > limites.max) {
            Swal.fire({ icon: "error", title: "Fuera de rango", text: `El valor debe estar entre ${limites.min} y ${limites.max} ${limites.label}.`, confirmButtonColor: "#28a745" });
            return;
        }

        // Redondear según decimales permitidos
        const valorRedondeado = parseFloat(valor.toFixed(limites.decimales));

        // Verificar duplicados comparando solo el valor numérico
        const existeDuplicado = capacidadesActuales.some(item => item.valor === valorRedondeado);
        if (existeDuplicado) {
            Swal.fire({ icon: "error", title: "Capacidad duplicada", text: "Esa capacidad ya está registrada.", confirmButtonColor: "#28a745" });
            return;
        }

        // Agregar como objeto {valor, enUso: false}
        capacidadesActuales.push({ valor: valorRedondeado, enUso: false });
        capacidadesActuales.sort((a, b) => a.valor - b.valor);
        inputNuevaCapacidad.value = "";
        renderizarCapacidades();
        inputNuevaCapacidad.focus();
    }

    btnAgregarCapacidad.addEventListener("click", agregarCapacidad);
    inputNuevaCapacidad.addEventListener("keypress", function(e) {
        if (e.key === "Enter") {
            e.preventDefault();
            agregarCapacidad();
        }
    });

    // Mostrar/ocultar sección de capacidades según el tipo de unidad seleccionado
    tipoUnidadField.addEventListener("change", function() {
        const valor = this.value;
        const nuevaUnidad = valor === "POR KILOS (KG)" ? "KG"
                          : valor === "POR LITROS (L)" ? "L"
                          : valor === "POR METROS (M)" ? "M"
                          : "";
        const requiereCap = nuevaUnidad !== "";

        // LIMPIAR SIEMPRE al cambiar de unidad (incluso entre KG/L/M)
        capacidadesActuales = [];
        inputNuevaCapacidad.value = "";
        mensajeValidacion.style.display = "none";
        renderizarCapacidades();

        unidadActual = nuevaUnidad;
        if (tipoUnidadHiddenField) {
            tipoUnidadHiddenField.value = this.value;
        }
        contenedorCapacidades.style.display = requiereCap ? "block" : "none";

        // Actualizar placeholder según unidad
        if (requiereCap) {
            const limites = obtenerLimites(nuevaUnidad);
            inputNuevaCapacidad.placeholder = `Ej: ${limites.min} (${limites.label})`;
            inputNuevaCapacidad.step = limites.decimales === 0 ? "1" : "0.01";
            inputNuevaCapacidad.min = limites.min;
            inputNuevaCapacidad.max = limites.max;
        } else {
            inputNuevaCapacidad.placeholder = "";
            inputNuevaCapacidad.step = "0.01";
            inputNuevaCapacidad.min = "0";
            inputNuevaCapacidad.max = "";
        }
    });

    // Función reutilizable para abrir el modal (ACTUALIZADA con tipoUnidad, capacidades y modo edición)
    const openModal = function (title, id = "", nombre = "", descripcion = "", tipoUnidad = "", capacidades = [], esEdicion = false) {
        modalTitle.textContent = title;
        idField.value = id;
        nombreField.value = nombre;
        descripcionField.value = descripcion;

        // Asignamos el valor al ComboBox
        if (tipoUnidadField) {
            tipoUnidadField.value = tipoUnidad;
            if (tipoUnidadHiddenField) {
                tipoUnidadHiddenField.value = tipoUnidad;
            }
            unidadActual = tipoUnidadCodigoPorLabel(tipoUnidad);
            contenedorCapacidades.style.display = unidadActual ? "block" : "none";
            const limites = obtenerLimites(unidadActual);
            inputNuevaCapacidad.placeholder = limites ? `Ej: ${limites.min} (${limites.label})` : "";
            inputNuevaCapacidad.step = limites && limites.decimales === 0 ? "1" : "0.01";
            inputNuevaCapacidad.min = limites ? limites.min : "0";
            inputNuevaCapacidad.max = limites ? limites.max : "";
            if (window.jQuery && typeof window.jQuery === "function") {
                window.jQuery(tipoUnidadField).trigger("change");
            }
            // Bloquear select en modo edición
            tipoUnidadField.disabled = esEdicion;
        }

        // Cargar capacidades existentes (pueden venir como {valor, enUso} o como números simples)
        if (capacidades.length > 0 && typeof capacidades[0] === "object" && capacidades[0].valor !== undefined) {
            capacidadesActuales = capacidades.map(item => ({ valor: item.valor, enUso: item.enUso }));
        } else {
            capacidadesActuales = (Array.isArray(capacidades) ? capacidades : []).map(valor => ({ valor: valor, enUso: false }));
        }
        capacidadesActuales.sort((a, b) => a.valor - b.valor);
        renderizarCapacidades();

        // Mostrar/ocultar contenedor según el tipo
        const requiereCap = tipoUnidad === "POR KILOS (KG)" || tipoUnidad === "POR LITROS (L)" || tipoUnidad === "POR METROS (M)";
        contenedorCapacidades.style.display = requiereCap ? "block" : "none";

        if (window.jQuery && typeof window.jQuery === "function") {
            window.jQuery("#modal-categoria").modal("show");
        }
    };

    // Al hacer clic en "Nueva Categoría"
    if (btnCreate) {
        btnCreate.addEventListener("click", function () {
            // Asegurar que el select esté habilitado para nueva categoría
            if (tipoUnidadField) {
                tipoUnidadField.disabled = false;
                tipoUnidadField.value = "";
            }
            if (tipoUnidadHiddenField) {
                tipoUnidadHiddenField.value = "";
                tipoUnidadHiddenField.removeAttribute("name");
            }
            unidadActual = "";
            inputNuevaCapacidad.value = "";
            contenedorCapacidades.style.display = "none";
            openModal("Nueva Categoría");
        });
    }

    // Reset completo al cerrar el modal
    if (window.jQuery && typeof window.jQuery === "function") {
        window.jQuery("#modal-categoria").on("hidden.bs.modal", function () {
            // Limpiar input, array de capacidades y tipo de unidad
            inputNuevaCapacidad.value = "";
            capacidadesActuales = [];
            unidadActual = "";
            renderizarCapacidades();
            if (tipoUnidadField) {
                tipoUnidadField.disabled = false;
                tipoUnidadField.value = "";
            }
            if (tipoUnidadHiddenField) {
                tipoUnidadHiddenField.removeAttribute("name");
                tipoUnidadHiddenField.value = "";
            }
            contenedorCapacidades.style.display = "none";
        });
    }

    // LISTENER GLOBAL DE CLICS (Para capturar Ver Capacidades, Editar, Eliminar y Estado)
    document.addEventListener("click", function (e) {

        // 0. Ver Capacidades
        const verCapButton = e.target.closest(".btn-ver-capacidades");
        if (verCapButton) {
            const id = verCapButton.dataset.id;
            const nombre = verCapButton.dataset.nombre;
            document.getElementById("modal-cap-nombre").textContent = nombre;
            const contenedor = document.getElementById("contenedor-lista-capacidades");
            contenedor.innerHTML = '<p class="text-muted mb-0">Cargando...</p>';

            fetch(`/categorias/${id}`)
                .then(response => response.json())
                .then(data => {
                    const caps = data.capacidades || [];
                    if (caps.length === 0) {
                        contenedor.innerHTML = '<p class="text-muted mb-0">No hay capacidades registradas.</p>';
                    } else {
                        // Mostrar como lista de viñetas (solo lectura) con icono según enUso
                        let html = '<ul class="list-unstyled text-left" style="max-width: 250px; margin: 0 auto;">';
                        caps.forEach(item => {
                            const valor = typeof item === "object" ? item.valor : item;
                            const enUso = typeof item === "object" ? item.enUso : false;
                            const icono = enUso ? "fa-lock text-danger" : "fa-check-circle text-success";
                            html += `<li class="mb-1"><i class="fas ${icono} mr-1"></i> <strong>${valor.toFixed(2)}</strong></li>`;
                        });
                        html += '</ul>';
                        contenedor.innerHTML = html;
                    }
                })
                .catch(() => {
                    contenedor.innerHTML = '<p class="text-danger mb-0">Error al cargar capacidades.</p>';
                });

            if (window.jQuery) {
                window.jQuery("#modal-ver-capacidades").modal("show");
            }
        }

        // 1. Cargar datos para Editar
        const editButton = e.target.closest(".btn-editar-categoria");
        if (editButton) {
            const id = editButton.dataset.id || "";
            const nombre = editButton.dataset.nombre || "";
            const descripcion = editButton.dataset.descripcion || "";
            const unidadMedida = editButton.dataset.unidadmedida || "";
            const tipoUnidad = unidadMedida ? tipoUnidadLabelPorCodigo(unidadMedida) : (editButton.dataset.tipounidad || "");

            // Obtener capacidades desde el backend
            fetch(`/categorias/${id}`)
                .then(response => response.json())
                .then(data => {
                    const capacidades = data.capacidades || [];
                    // Pasar esEdicion = true para bloquear el select de unidad
                    openModal("Editar Categoría", id, nombre, descripcion, tipoUnidad, capacidades, true);
                })
                .catch(() => {
                    openModal("Editar Categoría", id, nombre, descripcion, tipoUnidad, [], true);
                });
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
            if (tipoUnidadHiddenField && tipoUnidadHiddenField.value) {
                formData.set("tipoUnidad", tipoUnidadHiddenField.value);
            } else if (tipoUnidadField) {
                formData.set("tipoUnidad", tipoUnidadField.value);
            }

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
                        Swal.fire({
                            icon: "success",
                            title: "¡Guardado!",
                            text: "La categoría se ha guardado correctamente.",
                            timer: 2000,
                            showConfirmButton: false
                        });
                    } else {
                        Swal.fire({ icon: "error", title: "Error", text: data.message || "Error guardando categoría.", confirmButtonColor: "#28a745" });
                    }
                })
                .catch(error => {
                    console.error("Error guardando categoría:", error);
                    Swal.fire({ icon: "error", title: "Error", text: "Ocurrió un error al guardar la categoría.", confirmButtonColor: "#28a745" });
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

    // Función: Eliminar Categoría por Fetch asíncrono con SweetAlert2
    function CategoriaEliminar(id) {
        Swal.fire({
            title: "¿Estás seguro?",
            text: "La categoría se eliminará del sistema.",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#dc3545",
            cancelButtonColor: "#6c757d",
            confirmButtonText: "Sí, eliminar",
            cancelButtonText: "Cancelar",
            reverseButtons: true
        }).then((result) => {
            if (!result.isConfirmed) return;

            fetch(`/categorias/${id}/eliminar`, {
                method: "POST"
            })
                .then(r => r.json())
                .then(data => {
                    if (data.status === "OK") {
                        reloadCategoriasTable();
                        Swal.fire({
                            icon: "success",
                            title: "¡Eliminado!",
                            text: "La categoría ha sido eliminada.",
                            timer: 1500,
                            showConfirmButton: false
                        });
                    } else {
                        // Mostrar el mensaje exacto del backend (incluye validación de productos asociados)
                        Swal.fire({
                            icon: "error",
                            title: "No se puede eliminar",
                            text: data.message || "Error al eliminar la categoría.",
                            confirmButtonColor: "#28a745"
                        });
                    }
                })
                .catch(err => {
                    console.error("Error:", err);
                    Swal.fire({
                        icon: "error",
                        title: "Error",
                        text: "Ocurrió un error al eliminar la categoría.",
                        confirmButtonColor: "#28a745"
                    });
                });
        });
    }
});

// Inicialización de DataTables para Categorías
function initTablaCategorias() {
    if ($.fn.DataTable) {
        const table = $('#tabla-categorias');
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().destroy();
        }

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
            scrollY: "400px",
            scrollCollapse: true,
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
        $(window).on('resize', function () {
            dataTable.columns.adjust().draw();
        });

        // Conectar controles personalizados
        const $lengthSelect = $('#categorias-length');
        const $searchInput = $('#categorias-search');

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
            customPager.setAttribute('aria-label', 'Paginación de categorías');
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
        initTablaCategorias();
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
