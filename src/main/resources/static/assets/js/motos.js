window.AppModules = window.AppModules || {};

var estadoMotos = {
    initialized: false,
    formMoto: null,
    submitHandler: null,
    table: null,
    dataTable: null,
    resizeHandler: null,
    lengthSelect: null,
    lengthHandler: null,
    searchInput: null,
    searchHandler: null,
    drawHandler: null,
    swipeCleanup: null
};

function manejarSubmitMoto(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var formMoto = estadoMotos.formMoto;
            var url = formMoto.action;
            var data = new URLSearchParams(new FormData(formMoto)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando moto.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-moto').modal('hide');
                    reloadMotosTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos de la moto se procesaron con éxito.',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });
                }
            })
            .catch(function(error) {
                Swal.fire({
                    title: 'Advertencia',
                    text: error.message,
                    icon: 'warning',
                    confirmButtonColor: '#ffc107'
                });
            });
}

// ─── ACCIONES DEL MODAL ──────────────────────────────────────────────────────
function abrirModalNuevoMoto() {
    var form = document.getElementById('form-moto');
    if (form) form.reset();
    document.getElementById('modal-titulo').textContent = 'Nueva Moto';
    if (form) form.action = '/motos';
    $('#modal-moto').modal('show');
}

function abrirModalEditarMoto(id) {
    fetch('/motos/' + id)
        .then(response => response.json())
        .then(moto => {
            document.getElementById('modal-titulo').textContent = 'Editar Moto';
            document.getElementById('input-placa').value  = moto.placa || '';
            document.getElementById('input-marca').value  = moto.marca || '';
            document.getElementById('input-modelo').value = moto.modelo || '';
            document.getElementById('input-anio').value   = moto.anio || '';

            var form = document.getElementById('form-moto');
            if (form) form.action = '/motos/' + id + '/editar';

            $('#modal-moto').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

function reloadMotosTable() {
    fetch('/motos/tabla')
        .then(response => response.text())
        .then(html => {
            var lifecycleActivo = estadoMotos.initialized;
            if (lifecycleActivo) destruirTablaMotos();

            var tbody = document.getElementById('tabla-motos-body');
            if (tbody) {
                tbody.outerHTML = html;
                if (lifecycleActivo) initTablaMotos();
            }
        });
}

// ─── ELIMINAR CON SWEETALERT2 DIRECTO ────────────────────────────────────────
function eliminarMoto(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "La moto se desactivará en el sistema.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/motos/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadMotosTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'La moto ha sido dada de baja.',
                        icon: 'success',
                        timer: 1500,
                        showConfirmButton: false
                    });
                }
            })
            .catch(error => {
                Swal.fire({ title: 'Error', text: 'No se pudo eliminar.', icon: 'error' });
            });
        }
    });
}

// Inicialización de DataTables para Motos
function initTablaMotos() {
    if (!window.jQuery || !$.fn.DataTable) return;
    const table = $('#tabla-motos');
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
    estadoMotos.table = table[0];
    estadoMotos.dataTable = dataTable;

    estadoMotos.resizeHandler = function () { dataTable.columns.adjust().draw(); };
    $(window).on('resize.motos', estadoMotos.resizeHandler);

    estadoMotos.lengthSelect = document.getElementById('motos-length');
    estadoMotos.searchInput = document.getElementById('motos-search');

    if (estadoMotos.lengthSelect) {
        estadoMotos.lengthHandler = function () {
            dataTable.page.len(parseInt(estadoMotos.lengthSelect.value, 10)).draw();
        };
        estadoMotos.lengthSelect.addEventListener('change', estadoMotos.lengthHandler);
    }

    if (estadoMotos.searchInput) {
        estadoMotos.searchHandler = function () {
            dataTable.search(estadoMotos.searchInput.value).draw();
        };
        estadoMotos.searchInput.addEventListener('keyup', estadoMotos.searchHandler);
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
            customPager.setAttribute('aria-label', 'Paginación de motos');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesMotos();
            renderCustomInfoMotos(dataTable, infoBar);
            renderCustomPaginationMotos(dataTable, customPager);
            estadoMotos.swipeCleanup = attachSwipePaginationMotos(wrapperEl, dataTable);

            estadoMotos.drawHandler = function () {
                renderCustomInfoMotos(dataTable, infoBar);
                renderCustomPaginationMotos(dataTable, customPager);
            };
            dataTable.on('draw.dt.motos', estadoMotos.drawHandler);
    }
}

function destruirTablaMotos() {
    if (estadoMotos.lengthSelect && estadoMotos.lengthHandler) {
        estadoMotos.lengthSelect.removeEventListener('change', estadoMotos.lengthHandler);
    }
    if (estadoMotos.searchInput && estadoMotos.searchHandler) {
        estadoMotos.searchInput.removeEventListener('keyup', estadoMotos.searchHandler);
    }
    if (estadoMotos.resizeHandler && window.jQuery) {
        $(window).off('resize.motos', estadoMotos.resizeHandler);
    }
    if (estadoMotos.swipeCleanup) estadoMotos.swipeCleanup();
    if (estadoMotos.dataTable) {
        if (estadoMotos.drawHandler) {
            estadoMotos.dataTable.off('draw.dt.motos', estadoMotos.drawHandler);
        }
        estadoMotos.dataTable.destroy();
    }
    estadoMotos.table = null;
    estadoMotos.dataTable = null;
    estadoMotos.resizeHandler = null;
    estadoMotos.lengthSelect = null;
    estadoMotos.lengthHandler = null;
    estadoMotos.searchInput = null;
    estadoMotos.searchHandler = null;
    estadoMotos.drawHandler = null;
    estadoMotos.swipeCleanup = null;
}

function initMotos() {
    if (estadoMotos.initialized) return;
    estadoMotos.formMoto = document.getElementById('form-moto');
    estadoMotos.submitHandler = manejarSubmitMoto;
    if (estadoMotos.formMoto) {
        estadoMotos.formMoto.addEventListener('submit', estadoMotos.submitHandler);
    }
    estadoMotos.initialized = true;
    initTablaMotos();
}

function destroyMotos() {
    if (!estadoMotos.initialized) return;
    if (estadoMotos.formMoto && estadoMotos.submitHandler) {
        estadoMotos.formMoto.removeEventListener('submit', estadoMotos.submitHandler);
    }
    destruirTablaMotos();
    if (window.jQuery && $.fn.modal) {
        $('#modal-moto, #modal-eliminar-moto').modal('hide');
    }
    estadoMotos.formMoto = null;
    estadoMotos.submitHandler = null;
    estadoMotos.initialized = false;
}

window.AppModules.motos = {
    init: initMotos,
    destroy: destroyMotos,
    abrirModalNuevo: abrirModalNuevoMoto,
    abrirModalEditar: abrirModalEditarMoto,
    eliminarMoto: eliminarMoto,
    reloadTable: reloadMotosTable
};

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesMotos() {
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

function renderCustomInfoMotos(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationMotos(dataTable, pagerElement) {
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

function attachSwipePaginationMotos(wrapperElement, dataTable) {
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
