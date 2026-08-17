window.AppModules = window.AppModules || {};

var estadoEmpleados = {
    initialized: false,
    formEmpleado: null,
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
    dniAbortController: null,
    dniRequestId: 0
};

function manejarSubmitEmpleado(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var formEmpleado = estadoEmpleados.formEmpleado;
            var url = formEmpleado.action;
            var data = new URLSearchParams(new FormData(formEmpleado)).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando empleado.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-empleado').modal('hide');
                    reloadEmpleadosTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos del empleado se procesaron con éxito.',
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
function abrirModalNuevoEmpleado() {
    var form = document.getElementById('form-empleado');
    if (form) form.reset();

    // Habilitar la edición del nombre por si acaso quedó bloqueado de una búsqueda previa
    var inputNombre = document.getElementById('input-nombre');
    if (inputNombre) inputNombre.readOnly = false;

    document.getElementById('modal-titulo').textContent = 'Nuevo Empleado';
    if (form) form.action = '/empleados';
    $('#modal-empleado').modal('show');
}

function abrirModalEditarEmpleado(id) {
    fetch('/empleados/' + id)
        .then(response => response.json())
        .then(empleado => {
            document.getElementById('modal-titulo').textContent = 'Editar Empleado';
            document.getElementById('input-nombre').value   = empleado.nombre || '';
            document.getElementById('input-nombre').readOnly = false; // Permitir editar en edición
            document.getElementById('input-dni').value      = empleado.dni || '';
            document.getElementById('input-sueldo').value   = empleado.sueldoBase || '';
            document.getElementById('input-telefono').value = empleado.telefono || '';

            var form = document.getElementById('form-empleado');
            if (form) form.action = '/empleados/' + id + '/editar';

            $('#modal-empleado').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

function reloadEmpleadosTable() {
    fetch('/empleados/tabla')
        .then(response => response.text())
        .then(html => {
            var lifecycleActivo = estadoEmpleados.initialized;
            if (lifecycleActivo) destruirTablaEmpleados();

            var tbody = document.getElementById('tabla-empleados-body');
            if (tbody) {
                tbody.outerHTML = html;
                if (lifecycleActivo) initTablaEmpleados();
            }
        });
}

// ─── ELIMINAR CON SWEETALERT2 DIRECTO ────────────────────────────────────────
function eliminarEmpleado(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "El empleado se desactivará en el sistema.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/empleados/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadEmpleadosTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'El empleado ha sido dado de baja.',
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

// ─── CONSULTA API DNI (EXTRAÍDA AL SCOPE GLOBAL) ──────────────────────────────
function buscarDniApiEmpleado() {
    const inputDni = document.getElementById('input-dni');
    if (!inputDni) return;
    const dni = inputDni.value;

    if (dni.length !== 8 || isNaN(dni)) {
        Swal.fire({
            title: 'DNI Inválido',
            text: 'Por favor, ingrese un número de DNI de 8 dígitos.',
            icon: 'warning'
        });
        return;
    }

    const btnBuscar = document.getElementById('btn-buscar-dni');
    if (!btnBuscar || !estadoEmpleados.initialized) return;
    if (estadoEmpleados.dniAbortController) estadoEmpleados.dniAbortController.abort();
    const controller = new AbortController();
    const requestId = ++estadoEmpleados.dniRequestId;
    estadoEmpleados.dniAbortController = controller;
    const iconoOriginal = btnBuscar.innerHTML;
    btnBuscar.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    btnBuscar.disabled = true;

    // Endpoint proxy local
    fetch('/empleados/api/consultar-dni/' + dni, { signal: controller.signal })
        .then(response => {
            if (!response.ok) throw new Error('No se pudo establecer conexión con el servidor.');
            return response.json();
        })
        .then(res => {
            if (!estadoEmpleados.initialized || requestId !== estadoEmpleados.dniRequestId) return;
            // Nota: Se valida res.success tal como lo estructuraste
            if (res && res.datos) {
                const info = res.datos;

                // Sanitización y formateo
                const listaNombres = info.nombres.trim().split(/\s+/);
                const primerNombre = listaNombres[0];
                const nombreFormateado = `${primerNombre} ${info.ape_paterno} ${info.ape_materno}`;

                const inputNombre = document.getElementById('input-nombre');
                if (!inputNombre) return;
                inputNombre.value = nombreFormateado;
                inputNombre.readOnly = true;
            } else {
                throw new Error('El DNI ingresado no existe en el padrón o la respuesta no es válida.');
            }
        })
        .catch(error => {
            if (error.name === 'AbortError') return;
            if (!estadoEmpleados.initialized || requestId !== estadoEmpleados.dniRequestId) return;
            Swal.fire({
                title: 'Error de Búsqueda',
                text: error.message,
                icon: 'error'
            });
            const inputNombre = document.getElementById('input-nombre');
            if (inputNombre) inputNombre.readOnly = false;
        })
        .finally(() => {
            if (estadoEmpleados.initialized && requestId === estadoEmpleados.dniRequestId && btnBuscar.isConnected) {
                btnBuscar.innerHTML = iconoOriginal;
                btnBuscar.disabled = false;
                estadoEmpleados.dniAbortController = null;
            }
        });
}

// Inicialización de DataTables para Empleados
function initTablaEmpleados() {
    if (!window.jQuery || !$.fn.DataTable) return;
    const table = $('#tabla-empleados');
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
    estadoEmpleados.table = table[0];
    estadoEmpleados.dataTable = dataTable;

    // Forzar reajuste de columnas al cambiar el tamaño de la ventana o zoom
    estadoEmpleados.resizeHandler = function () { dataTable.columns.adjust().draw(); };
    $(window).on('resize.empleados', estadoEmpleados.resizeHandler);

    // Conectar controles personalizados
    estadoEmpleados.lengthSelect = document.getElementById('empleados-length');
    estadoEmpleados.searchInput = document.getElementById('empleados-search');

    if (estadoEmpleados.lengthSelect) {
        estadoEmpleados.lengthHandler = function () {
            dataTable.page.len(parseInt(estadoEmpleados.lengthSelect.value, 10)).draw();
        };
        estadoEmpleados.lengthSelect.addEventListener('change', estadoEmpleados.lengthHandler);
    }

    if (estadoEmpleados.searchInput) {
        estadoEmpleados.searchHandler = function () {
            dataTable.search(estadoEmpleados.searchInput.value).draw();
        };
        estadoEmpleados.searchInput.addEventListener('keyup', estadoEmpleados.searchHandler);
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
            customPager.setAttribute('aria-label', 'Paginación de empleados');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesEmpleados();
            renderCustomInfoEmpleados(dataTable, infoBar);
            renderCustomPaginationEmpleados(dataTable, customPager);
            estadoEmpleados.swipeCleanup = attachSwipePaginationEmpleados(wrapperEl, dataTable);

            estadoEmpleados.drawHandler = function () {
                renderCustomInfoEmpleados(dataTable, infoBar);
                renderCustomPaginationEmpleados(dataTable, customPager);
            };
            dataTable.on('draw.dt.empleados', estadoEmpleados.drawHandler);
    }
}

function destruirTablaEmpleados() {
    if (estadoEmpleados.lengthSelect && estadoEmpleados.lengthHandler) {
        estadoEmpleados.lengthSelect.removeEventListener('change', estadoEmpleados.lengthHandler);
    }
    if (estadoEmpleados.searchInput && estadoEmpleados.searchHandler) {
        estadoEmpleados.searchInput.removeEventListener('keyup', estadoEmpleados.searchHandler);
    }
    if (estadoEmpleados.resizeHandler && window.jQuery) {
        $(window).off('resize.empleados', estadoEmpleados.resizeHandler);
    }
    if (estadoEmpleados.swipeCleanup) estadoEmpleados.swipeCleanup();
    if (estadoEmpleados.dataTable) {
        if (estadoEmpleados.drawHandler) {
            estadoEmpleados.dataTable.off('draw.dt.empleados', estadoEmpleados.drawHandler);
        }
        estadoEmpleados.dataTable.destroy();
    }
    estadoEmpleados.table = null;
    estadoEmpleados.dataTable = null;
    estadoEmpleados.resizeHandler = null;
    estadoEmpleados.lengthSelect = null;
    estadoEmpleados.lengthHandler = null;
    estadoEmpleados.searchInput = null;
    estadoEmpleados.searchHandler = null;
    estadoEmpleados.drawHandler = null;
    estadoEmpleados.swipeCleanup = null;
}

function initEmpleados() {
    if (estadoEmpleados.initialized) return;
    estadoEmpleados.formEmpleado = document.getElementById('form-empleado');
    estadoEmpleados.submitHandler = manejarSubmitEmpleado;
    if (estadoEmpleados.formEmpleado) {
        estadoEmpleados.formEmpleado.addEventListener('submit', estadoEmpleados.submitHandler);
    }
    estadoEmpleados.initialized = true;
    initTablaEmpleados();
}

function destroyEmpleados() {
    if (!estadoEmpleados.initialized) return;
    if (estadoEmpleados.formEmpleado && estadoEmpleados.submitHandler) {
        estadoEmpleados.formEmpleado.removeEventListener('submit', estadoEmpleados.submitHandler);
    }
    destruirTablaEmpleados();
    if (estadoEmpleados.dniAbortController) estadoEmpleados.dniAbortController.abort();
    estadoEmpleados.dniAbortController = null;
    estadoEmpleados.dniRequestId += 1;
    if (window.jQuery && $.fn.modal) {
        $('#modal-empleado, #modal-eliminar-empleado').modal('hide');
    }
    const inputNombre = document.getElementById('input-nombre');
    if (inputNombre) inputNombre.readOnly = false;
    estadoEmpleados.formEmpleado = null;
    estadoEmpleados.submitHandler = null;
    estadoEmpleados.initialized = false;
}

window.AppModules.empleados = {
    init: initEmpleados,
    destroy: destroyEmpleados,
    abrirModalNuevo: abrirModalNuevoEmpleado,
    abrirModalEditar: abrirModalEditarEmpleado,
    eliminarEmpleado: eliminarEmpleado,
    buscarDniApi: buscarDniApiEmpleado,
    reloadTable: reloadEmpleadosTable
};

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesEmpleados() {
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

function renderCustomInfoEmpleados(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationEmpleados(dataTable, pagerElement) {
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

function attachSwipePaginationEmpleados(wrapperElement, dataTable) {
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
