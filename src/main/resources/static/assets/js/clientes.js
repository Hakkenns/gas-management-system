// ─── INTERCEPTAR Y PROCESAR EL FORMULARIO AL ENVIAR ───────────────────────────
window.AppModules = window.AppModules || {};
var clientesLifecycle = {
    form: null,
    submitHandler: null,
    dataTable: null,
    resizeHandler: null,
    initialized: false
};

function initClientes() {
    if (clientesLifecycle.initialized) return;
    clientesLifecycle.initialized = true;

    var formCliente = document.getElementById('form-cliente');
    if (formCliente) {
        clientesLifecycle.form = formCliente;
        clientesLifecycle.submitHandler = function(event) {
            event.preventDefault(); // Detiene la redirección inmediata del HTML

            var inputDni = document.getElementById('input-dni');
            var inputNombre = document.getElementById('input-nombre');

            // Si el DNI está vacío o solo tiene espacios, lo limpiamos por completo
            if (inputDni && inputDni.value.trim() === '') {
                inputDni.value = '';
            }

            // LÓGICA ANÓNIMA: Si el usuario dejó el nombre vacío, lo autocompletamos
            if (inputNombre && (!inputNombre.value || inputNombre.value.trim() === '')) {
                inputNombre.value = "CLIENTE VARIOS / ANÓNIMO";
            }

            var url = formCliente.action;
            var formData = new FormData(formCliente);
            var inputPrestamoIlimitado = document.getElementById('input-prestamo-ilimitado');
            formData.set('prestamoIlimitado', inputPrestamoIlimitado && inputPrestamoIlimitado.checked ? 'true' : 'false');
            var data = new URLSearchParams(formData).toString();

            fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: data
            })
            .then(function(response) {
                return response.json().then(function(body) {
                    if (!response.ok) {
                        throw new Error(body.message || 'Error guardando cliente.');
                    }
                    return body;
                });
            })
            .then(function(body) {
                if (body.status === 'OK') {
                    $('#modal-cliente').modal('hide');
                    reloadClientesTable();

                    Swal.fire({
                        title: '¡Guardado!',
                        text: 'Los datos del cliente se procesaron con éxito.',
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
        };
        formCliente.addEventListener('submit', clientesLifecycle.submitHandler);
    }
    initTablaClientes();
}

function destroyClientes() {
    if (clientesLifecycle.form && clientesLifecycle.submitHandler) {
        clientesLifecycle.form.removeEventListener('submit', clientesLifecycle.submitHandler);
    }
    if (clientesLifecycle.dataTable && $.fn.dataTable.isDataTable('#tabla-clientes')) {
        clientesLifecycle.dataTable.destroy();
    }
    if (clientesLifecycle.resizeHandler) {
        $(window).off('resize', clientesLifecycle.resizeHandler);
    }
    document.querySelectorAll('#contenido-principal .modal.show').forEach(modal => {
        $('#'+modal.id).modal('hide');
    });
    clientesLifecycle.form = null;
    clientesLifecycle.submitHandler = null;
    clientesLifecycle.dataTable = null;
    clientesLifecycle.resizeHandler = null;
    clientesLifecycle.initialized = false;
}

window.AppModules.clientes = {
    init: initClientes,
    destroy: destroyClientes,
    abrirModalNuevo: abrirModalNuevoCliente,
    abrirModalEditar: abrirModalEditarCliente,
    verCliente: verCliente,
    eliminarCliente: eliminarCliente,
    buscarDniApi: buscarDniApiCliente,
    reloadTable: reloadClientesTable
};

// ─── ACCIONES DEL MODAL (NUEVO / EDITAR) ──────────────────────────────────────
function abrirModalNuevoCliente() {
    var form = document.getElementById('form-cliente');
    if (form) form.reset();

    // Habilitar la edición del nombre por si acaso quedó bloqueado de una búsqueda previa
    var inputNombre = document.getElementById('input-nombre');
    if (inputNombre) inputNombre.readOnly = false;
    var inputPrestamoIlimitado = document.getElementById('input-prestamo-ilimitado');
    if (inputPrestamoIlimitado) inputPrestamoIlimitado.checked = false;

    document.getElementById('modal-titulo').textContent = 'Nuevo Cliente';
    if (form) form.action = '/clientes';
    $('#modal-cliente').modal('show');
}

function abrirModalEditarCliente(id) {
    fetch('/clientes/' + id)
        .then(response => response.json())
        .then(cliente => {
            document.getElementById('modal-titulo').textContent = 'Editar Cliente';
            document.getElementById('input-nombre').value     = cliente.nombre || '';
            document.getElementById('input-nombre').readOnly = false; // Permitir editar en edición
            document.getElementById('input-dni').value        = cliente.dni || '';
            document.getElementById('input-telefono').value   = cliente.telefono || '';
            document.getElementById('input-direccion').value  = cliente.direccion || '';
            document.getElementById('input-referencia').value = cliente.referencia || '';
            document.getElementById('input-correo').value     = cliente.correo || '';
            document.getElementById('input-prestamo-ilimitado').checked = cliente.prestamoIlimitado === true;

            var form = document.getElementById('form-cliente');
            if (form) form.action = '/clientes/' + id + '/editar';

            $('#modal-cliente').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

// ─── VISTA RÁPIDA (SOLO LECTURA - OJITO CELESTE) ──────────────────────────────
function verCliente(id) {
    fetch('/clientes/' + id)
        .then(response => response.json())
        .then(cliente => {
            document.getElementById('view-nombre').textContent = cliente.nombre || '';
            document.getElementById('view-dni').textContent = cliente.dni || '---';
            document.getElementById('view-telefono').textContent = cliente.telefono || '';
            document.getElementById('view-direccion').textContent = cliente.direccion || '';
            document.getElementById('view-referencia').textContent = cliente.referencia || '';
            document.getElementById('view-correo').textContent = cliente.correo || '';
            document.getElementById('view-prestamo-ilimitado').textContent = cliente.prestamoIlimitado === true ? 'Sí' : 'No';

            $('#modal-ver-cliente').modal('show');
        })
        .catch(error => console.error('Error:', error));
}

function reloadClientesTable() {
    fetch('/clientes/tabla')
        .then(response => response.text())
        .then(html => {
            var tbody = document.getElementById('tabla-clientes-body');
            if (tbody) {
                tbody.outerHTML = html;
                if (clientesLifecycle.initialized) initTablaClientes();
            }
        });
}

// ─── ELIMINAR CON SWEETALERT2 ──────────────────────────────────────────────────
function eliminarCliente(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: 'El cliente se desactivará en el sistema.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/clientes/' + id + '/eliminar', {
                method: 'POST'
            })
            .then(response => response.json())
            .then(body => {
                if (body.status === 'OK') {
                    reloadClientesTable();
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'El cliente ha sido dado de baja.',
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
function buscarDniApiCliente() {
    const dni = document.getElementById('input-dni').value;

    if (dni.length !== 8 || isNaN(dni)) {
        Swal.fire({
            title: 'DNI Inválido',
            text: 'Por favor, ingrese un número de DNI de 8 dígitos.',
            icon: 'warning'
        });
        return;
    }

    const btnBuscar = document.getElementById('btn-buscar-dni');
    const contenedorIcono = document.getElementById('icono-buscar');
    const iconoOriginal = '<i class="fas fa-search"></i>';

    if (contenedorIcono) {
        contenedorIcono.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    }
    btnBuscar.disabled = true;

    fetch('/clientes/api/consultar-dni/' + dni)
        .then(response => {
            if (!response.ok) throw new Error('No se pudo establecer conexión con el servidor.');
            return response.json();
        })
        .then(res => {
            if (res && res.datos) {
                const info = res.datos;
                const listaNombres = info.nombres.trim().split(/\s+/);
                const primerNombre = listaNombres[0];
                const nombreFormateado = `${primerNombre} ${info.ape_paterno} ${info.ape_materno || ''}`;

                document.getElementById('input-nombre').value = nombreFormateado.toUpperCase();
                document.getElementById('input-nombre').readOnly = true;
            } else {
                throw new Error('El DNI ingresado no existe en el padrón o la respuesta no es válida.');
            }
        })
        .catch(error => {
            Swal.fire({
                title: 'Error de Búsqueda',
                text: error.message,
                icon: 'error'
            });
            document.getElementById('input-nombre').readOnly = false;
        })
        .finally(() => {
            if (contenedorIcono) {
                contenedorIcono.innerHTML = iconoOriginal;
            }
            btnBuscar.disabled = false;
        });
}

// Inicialización de DataTables para Clientes
function initTablaClientes() {
    if ($.fn.DataTable) {
        const table = $('#tabla-clientes');
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
        if (clientesLifecycle.resizeHandler) {
            $(window).off('resize', clientesLifecycle.resizeHandler);
        }
        clientesLifecycle.dataTable = dataTable;
        clientesLifecycle.resizeHandler = function () {
            dataTable.columns.adjust().draw();
        };
        $(window).on('resize', clientesLifecycle.resizeHandler);

        // Conectar controles personalizados
        const $lengthSelect = $('#clientes-length');
        const $searchInput = $('#clientes-search');

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
            customPager.setAttribute('aria-label', 'Paginación de clientes');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesClientes();
            renderCustomInfoClientes(dataTable, infoBar);
            renderCustomPaginationClientes(dataTable, customPager);
            attachSwipePaginationClientes(wrapperEl, dataTable);

            dataTable.on('draw.dt', () => {
                renderCustomInfoClientes(dataTable, infoBar);
                renderCustomPaginationClientes(dataTable, customPager);
            });
        }
    }
}

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesClientes() {
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

function renderCustomInfoClientes(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationClientes(dataTable, pagerElement) {
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

function attachSwipePaginationClientes(wrapperElement, dataTable) {
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
