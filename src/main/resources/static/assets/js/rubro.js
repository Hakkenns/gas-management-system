window.AppModules = window.AppModules || {};

var estadoRubros = {
    initialized: false,
    root: null,
    clickHandler: null,
    formRubro: null,
    submitHandler: null,
    table: null,
    dataTable: null,
    resizeHandler: null,
    lengthSelect: null,
    lengthHandler: null,
    searchInput: null,
    searchHandler: null,
    drawHandler: null,
    swipeCleanup: null,
    editAbortController: null,
    editRequestId: 0
};

function manejarClickRubro(e) {

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

            if (estadoRubros.editAbortController) estadoRubros.editAbortController.abort();
            const controller = new AbortController();
            const requestId = ++estadoRubros.editRequestId;
            estadoRubros.editAbortController = controller;

            fetch(`/rubros/${id}`, { signal: controller.signal })
                .then(r => r.json())
                .then(data => {
                    if (!estadoRubros.initialized || requestId !== estadoRubros.editRequestId) return;
                    const titulo = document.getElementById("modal-titulo-rubro");
                    const idRubro = document.getElementById("id-rubro");
                    const nombre = document.getElementById("nombre-rubro");
                    const descripcion = document.getElementById("descripcion-rubro");
                    if (!titulo || !idRubro || !nombre || !descripcion) return;
                    titulo.textContent = "Editar Rubro";
                    idRubro.value = data.id;
                    nombre.value = data.nombre;
                    descripcion.value = data.descripcion;
                    $("#modal-rubro").modal("show");
                })
                .catch(err => {
                    if (err.name !== 'AbortError' && estadoRubros.initialized && requestId === estadoRubros.editRequestId) {
                        console.log("ERROR AL CARGAR DATOS DEL RUBRO:", err);
                    }
                })
                .finally(() => {
                    if (requestId === estadoRubros.editRequestId) estadoRubros.editAbortController = null;
                });
        }

        // 🟢 NUEVO: Detectar clic en los botones de Activar / Inhabilitar Estado
        if (e.target.closest(".btn-estado-rubro")) {
            const boton = e.target.closest(".btn-estado-rubro");
            const id = boton.dataset.id;
            const nuevoEstado = boton.dataset.estado;
            cambiarEstadoRubro(id, nuevoEstado);
        }

        // Acción eliminar (Lógico -> Pasa a estado 0)
        if (e.target.closest(".btn-eliminar-rubro")) {
            const id = e.target.closest(".btn-eliminar-rubro").dataset.id;
            eliminarRubro(id);
        }
    }

    // Refrescar el fragmento HTML de la tabla sin recargar la página
function reloadRubrosTable() {
        fetch("/rubros/tabla")
            .then(r => {
                if (!r.ok) throw new Error("Error cargando tabla de rubros");
                return r.text();
            })
            .then(html => {
                const lifecycleActivo = estadoRubros.initialized;
                if (lifecycleActivo) destruirTablaRubros();

                const tbody = document.getElementById('tabla-rubros-body');
                if (tbody) {
                    tbody.outerHTML = html;
                    if (lifecycleActivo) initTablaRubros();
                }
            })
            .catch(err => console.log("ERROR RECARGANDO TABLA:", err));
    }

    // Envío del Formulario vía AJAX
    function manejarSubmitRubro(e) {
        e.preventDefault();

        const id = document.getElementById("id-rubro").value;
        const url = id ? `/rubros/${id}/editar` : "/rubros";

        $.ajax({
            url: url,
            type: "POST",
            data: $(estadoRubros.formRubro).serialize(),
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
    }

    // Función para cambiar a estado 0 (Inactivo / Eliminado según tu lógica unificada)
    function eliminarRubro(id) {
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
    function cambiarEstadoRubro(id, estado) {
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

// Inicialización de DataTables para Rubros
function initTablaRubros() {
    if (!window.jQuery || !$.fn.DataTable) return;
    const table = $('#tabla-rubros');
    if (!table.length) return;
    if ($.fn.dataTable.isDataTable(table)) table.DataTable().destroy();

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
    estadoRubros.table = table[0];
    estadoRubros.dataTable = dataTable;

    estadoRubros.resizeHandler = function () { dataTable.columns.adjust().draw(); };
    $(window).on('resize.rubros', estadoRubros.resizeHandler);

    estadoRubros.lengthSelect = document.getElementById('rubros-length');
    estadoRubros.searchInput = document.getElementById('rubros-search');

    if (estadoRubros.lengthSelect) {
        estadoRubros.lengthHandler = function () {
            dataTable.page.len(parseInt(estadoRubros.lengthSelect.value, 10)).draw();
        };
        estadoRubros.lengthSelect.addEventListener('change', estadoRubros.lengthHandler);
    }

    if (estadoRubros.searchInput) {
        estadoRubros.searchHandler = function () {
            dataTable.search(estadoRubros.searchInput.value).draw();
        };
        estadoRubros.searchInput.addEventListener('keyup', estadoRubros.searchHandler);
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
            customPager.setAttribute('aria-label', 'Paginación de rubros');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesRubros();
            renderCustomInfoRubros(dataTable, infoBar);
            renderCustomPaginationRubros(dataTable, customPager);
            estadoRubros.swipeCleanup = attachSwipePaginationRubros(wrapperEl, dataTable);

            estadoRubros.drawHandler = function () {
                renderCustomInfoRubros(dataTable, infoBar);
                renderCustomPaginationRubros(dataTable, customPager);
            };
            dataTable.on('draw.dt.rubros', estadoRubros.drawHandler);
    }
}

function destruirTablaRubros() {
    if (estadoRubros.lengthSelect && estadoRubros.lengthHandler) {
        estadoRubros.lengthSelect.removeEventListener('change', estadoRubros.lengthHandler);
    }
    if (estadoRubros.searchInput && estadoRubros.searchHandler) {
        estadoRubros.searchInput.removeEventListener('keyup', estadoRubros.searchHandler);
    }
    if (estadoRubros.resizeHandler && window.jQuery) {
        $(window).off('resize.rubros', estadoRubros.resizeHandler);
    }
    if (estadoRubros.swipeCleanup) estadoRubros.swipeCleanup();
    if (estadoRubros.dataTable) {
        if (estadoRubros.drawHandler) estadoRubros.dataTable.off('draw.dt.rubros', estadoRubros.drawHandler);
        estadoRubros.dataTable.destroy();
    }
    estadoRubros.table = null;
    estadoRubros.dataTable = null;
    estadoRubros.resizeHandler = null;
    estadoRubros.lengthSelect = null;
    estadoRubros.lengthHandler = null;
    estadoRubros.searchInput = null;
    estadoRubros.searchHandler = null;
    estadoRubros.drawHandler = null;
    estadoRubros.swipeCleanup = null;
}

function initRubros() {
    if (estadoRubros.initialized) return;
    estadoRubros.root = document.querySelector('[data-modulo="rubros"]');
    if (!estadoRubros.root) return;
    estadoRubros.clickHandler = manejarClickRubro;
    estadoRubros.root.addEventListener('click', estadoRubros.clickHandler);
    estadoRubros.formRubro = document.getElementById('form-rubro');
    estadoRubros.submitHandler = manejarSubmitRubro;
    if (estadoRubros.formRubro) estadoRubros.formRubro.addEventListener('submit', estadoRubros.submitHandler);
    estadoRubros.initialized = true;
    initTablaRubros();
}

function destroyRubros() {
    if (!estadoRubros.initialized) return;
    if (estadoRubros.root && estadoRubros.clickHandler) {
        estadoRubros.root.removeEventListener('click', estadoRubros.clickHandler);
    }
    if (estadoRubros.formRubro && estadoRubros.submitHandler) {
        estadoRubros.formRubro.removeEventListener('submit', estadoRubros.submitHandler);
    }
    destruirTablaRubros();
    if (estadoRubros.editAbortController) estadoRubros.editAbortController.abort();
    estadoRubros.editAbortController = null;
    estadoRubros.editRequestId += 1;
    if (window.jQuery && $.fn.modal) $('#modal-rubro').modal('hide');
    estadoRubros.root = null;
    estadoRubros.clickHandler = null;
    estadoRubros.formRubro = null;
    estadoRubros.submitHandler = null;
    estadoRubros.initialized = false;
}

window.AppModules.rubros = {
    init: initRubros,
    destroy: destroyRubros,
    reloadTable: reloadRubrosTable
};

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesRubros() {
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

function renderCustomInfoRubros(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationRubros(dataTable, pagerElement) {
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

function attachSwipePaginationRubros(wrapperElement, dataTable) {
    let touchStartX = 0;

    const touchStartHandler = (event) => {
        touchStartX = event.touches[0].clientX;
    };

    const touchEndHandler = (event) => {
        if (!event.changedTouches.length) return;
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
    };

    wrapperElement.addEventListener('touchstart', touchStartHandler, { passive: true });
    wrapperElement.addEventListener('touchend', touchEndHandler, { passive: true });

    return function () {
        wrapperElement.removeEventListener('touchstart', touchStartHandler);
        wrapperElement.removeEventListener('touchend', touchEndHandler);
    };
}
