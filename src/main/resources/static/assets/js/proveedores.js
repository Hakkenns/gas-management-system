window.AppModules = window.AppModules || {};

var estadoProveedores = {
    initialized: false,
    root: null,
    clickHandler: null,
    formProveedor: null,
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

function manejarClickProveedor(e) {
    if (e.target.closest("#btn-crear-proveedor")) {
        document.getElementById("form-proveedor").reset();
        document.getElementById("input-id").value = "";
        document.getElementById("modal-titulo-proveedor").textContent = "Crear Proveedor";
        $("#modal-proveedor").modal("show");
    }

    if (e.target.closest(".btn-editar-proveedor")) {
        const btn = e.target.closest(".btn-editar-proveedor");
        const id = btn.dataset.id;

        if (estadoProveedores.editAbortController) estadoProveedores.editAbortController.abort();
        const controller = new AbortController();
        const requestId = ++estadoProveedores.editRequestId;
        estadoProveedores.editAbortController = controller;

        fetch(`/proveedores/${id}`, { signal: controller.signal })
            .then(r => r.json())
            .then(data => {
                if (!estadoProveedores.initialized || requestId !== estadoProveedores.editRequestId) return;
                const modalTitulo = document.getElementById("modal-titulo-proveedor");
                const inputId = document.getElementById("input-id");
                const inputRuc = document.getElementById("input-ruc");
                const inputNombre = document.getElementById("input-nombre");
                const inputTelefono = document.getElementById("input-telefono");
                const inputCorreo = document.getElementById("input-correo");
                const inputRubro = document.getElementById("input-rubro");
                if (!modalTitulo || !inputId || !inputRuc || !inputNombre || !inputTelefono || !inputCorreo || !inputRubro) return;
                modalTitulo.textContent = "Editar Proveedor";
                inputId.value = data.id;
                inputRuc.value = data.ruc;
                inputNombre.value = data.nombre;
                inputTelefono.value = data.telefono;
                inputCorreo.value = data.correo;
                inputRubro.value = data.idRubro;
                $("#modal-proveedor").modal("show");
            })
            .catch(err => {
                if (err.name !== 'AbortError' && estadoProveedores.initialized && requestId === estadoProveedores.editRequestId) {
                    console.error("ERROR JSON PROVEEDOR:", err);
                }
            })
            .finally(() => {
                if (requestId === estadoProveedores.editRequestId) estadoProveedores.editAbortController = null;
            });
    }

    if (e.target.closest(".btn-cambiar-estado")) {
        const btn = e.target.closest(".btn-cambiar-estado");
        const id = btn.dataset.id;
        const estadoActual = parseInt(btn.dataset.estado, 10);
        const nuevoEstado = estadoActual === 1 ? 0 : 1;
        cambiarEstadoProveedor(id, nuevoEstado);
        e.preventDefault();
    }

    if (e.target.closest(".btn-eliminar-proveedor")) {
        const id = e.target.closest(".btn-eliminar-proveedor").dataset.id;
        eliminarProveedor(id);
        e.preventDefault();
    }
}

function initTablaProveedores() {
    if (!window.jQuery || !$.fn.DataTable) return;
    const table = $('#tabla-proveedores');
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
    estadoProveedores.table = table[0];
    estadoProveedores.dataTable = dataTable;

    estadoProveedores.resizeHandler = function () { dataTable.columns.adjust().draw(); };
    $(window).on('resize.proveedores', estadoProveedores.resizeHandler);

    estadoProveedores.lengthSelect = document.getElementById('proveedores-length');
    estadoProveedores.searchInput = document.getElementById('proveedores-search');

    if (estadoProveedores.lengthSelect) {
        estadoProveedores.lengthHandler = function () {
            dataTable.page.len(parseInt(estadoProveedores.lengthSelect.value, 10)).draw();
        };
        estadoProveedores.lengthSelect.addEventListener('change', estadoProveedores.lengthHandler);
    }

    if (estadoProveedores.searchInput) {
        estadoProveedores.searchHandler = function () {
            dataTable.search(estadoProveedores.searchInput.value).draw();
        };
        estadoProveedores.searchInput.addEventListener('keyup', estadoProveedores.searchHandler);
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
            customPager.setAttribute('aria-label', 'Paginación de proveedores');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesProveedores();
            renderCustomInfoProveedores(dataTable, infoBar);
            renderCustomPaginationProveedores(dataTable, customPager);
            estadoProveedores.swipeCleanup = attachSwipePaginationProveedores(wrapperEl, dataTable);

            estadoProveedores.drawHandler = function () {
                renderCustomInfoProveedores(dataTable, infoBar);
                renderCustomPaginationProveedores(dataTable, customPager);
            };
            dataTable.on('draw.dt.proveedores', estadoProveedores.drawHandler);
    }
}

function reloadProveedoresTable() {
    fetch("/proveedores/tabla")
        .then(r => {
            if (!r.ok) throw new Error("Error cargando tabla de proveedores");
            return r.text();
        })
        .then(html => {
            const lifecycleActivo = estadoProveedores.initialized;
            if (lifecycleActivo) destruirTablaProveedores();

            const tbody = document.getElementById('tabla-proveedores-body');
            if (tbody) {
                tbody.outerHTML = html;
                if (lifecycleActivo) initTablaProveedores();
            }
        })
        .catch(err => console.error("ERROR recargando tabla de proveedores:", err));
}

function manejarSubmitProveedor(e) {
    e.preventDefault();

    const btnGuardar = $(estadoProveedores.formProveedor).find('button[type="submit"]');
    btnGuardar.prop('disabled', true).text('Guardando...');

    $.ajax({
        url: "/proveedores",
        type: "POST",
        data: $(estadoProveedores.formProveedor).serialize(),
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
}

function destruirTablaProveedores() {
    if (estadoProveedores.lengthSelect && estadoProveedores.lengthHandler) {
        estadoProveedores.lengthSelect.removeEventListener('change', estadoProveedores.lengthHandler);
    }
    if (estadoProveedores.searchInput && estadoProveedores.searchHandler) {
        estadoProveedores.searchInput.removeEventListener('keyup', estadoProveedores.searchHandler);
    }
    if (estadoProveedores.resizeHandler && window.jQuery) {
        $(window).off('resize.proveedores', estadoProveedores.resizeHandler);
    }
    if (estadoProveedores.swipeCleanup) estadoProveedores.swipeCleanup();
    if (estadoProveedores.dataTable) {
        if (estadoProveedores.drawHandler) {
            estadoProveedores.dataTable.off('draw.dt.proveedores', estadoProveedores.drawHandler);
        }
        estadoProveedores.dataTable.destroy();
    }
    estadoProveedores.table = null;
    estadoProveedores.dataTable = null;
    estadoProveedores.resizeHandler = null;
    estadoProveedores.lengthSelect = null;
    estadoProveedores.lengthHandler = null;
    estadoProveedores.searchInput = null;
    estadoProveedores.searchHandler = null;
    estadoProveedores.drawHandler = null;
    estadoProveedores.swipeCleanup = null;
}

function initProveedores() {
    if (estadoProveedores.initialized) return;
    estadoProveedores.root = document.querySelector('[data-modulo="proveedores"]');
    if (!estadoProveedores.root) return;
    estadoProveedores.clickHandler = manejarClickProveedor;
    estadoProveedores.root.addEventListener('click', estadoProveedores.clickHandler);
    estadoProveedores.formProveedor = document.getElementById('form-proveedor');
    estadoProveedores.submitHandler = manejarSubmitProveedor;
    if (estadoProveedores.formProveedor) {
        estadoProveedores.formProveedor.addEventListener('submit', estadoProveedores.submitHandler);
    }
    estadoProveedores.initialized = true;
    initTablaProveedores();
}

function destroyProveedores() {
    if (!estadoProveedores.initialized) return;
    if (estadoProveedores.root && estadoProveedores.clickHandler) {
        estadoProveedores.root.removeEventListener('click', estadoProveedores.clickHandler);
    }
    if (estadoProveedores.formProveedor && estadoProveedores.submitHandler) {
        estadoProveedores.formProveedor.removeEventListener('submit', estadoProveedores.submitHandler);
    }
    destruirTablaProveedores();
    if (estadoProveedores.editAbortController) estadoProveedores.editAbortController.abort();
    estadoProveedores.editAbortController = null;
    estadoProveedores.editRequestId += 1;
    if (window.jQuery && $.fn.modal) $('#modal-proveedor').modal('hide');
    estadoProveedores.root = null;
    estadoProveedores.clickHandler = null;
    estadoProveedores.formProveedor = null;
    estadoProveedores.submitHandler = null;
    estadoProveedores.initialized = false;
}

window.AppModules.proveedores = {
    init: initProveedores,
    destroy: destroyProveedores,
    reloadTable: reloadProveedoresTable
};

function cambiarEstadoProveedor(id, estado) {
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

function eliminarProveedor(id) {
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

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesProveedores() {
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

function renderCustomInfoProveedores(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationProveedores(dataTable, pagerElement) {
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

function attachSwipePaginationProveedores(wrapperElement, dataTable) {
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
