window.AppModules = window.AppModules || {};

var estadoUsuarios = {
    initialized: false,
    formUsuario: null,
    submitHandler: null,
    botonEliminar: null,
    eliminarHandler: null,
    table: null,
    dataTable: null,
    resizeHandler: null,
    lengthSelect: null,
    lengthHandler: null,
    searchInput: null,
    searchHandler: null,
    drawHandler: null,
    swipeCleanup: null,
    idUsuarioAEliminar: null
};

// ─── MOSTRAR / OCULTAR PASSWORD ───────────────────────────────────────────────
function togglePassword() {
    var input = document.getElementById('input-password');
    var icono = document.getElementById('icono-password');

    if (input.type === 'password') {
        input.type = 'text';
        icono.classList.remove('fa-eye');
        icono.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icono.classList.remove('fa-eye-slash');
        icono.classList.add('fa-eye');
    }
}

// ─── ABRIR MODAL NUEVO ────────────────────────────────────────────────────────
function abrirModalNuevoUsuario() {
    document.getElementById('form-usuario').reset();
    document.getElementById('modal-titulo').textContent = 'Nuevo Usuario';
    document.getElementById('form-usuario').action     = '/usuarios';
    document.getElementById('form-usuario').method     = 'post';

    // MOSTRAR SELECTOR: El usuario nuevo requiere asociar obligatoriamente un empleado
    document.getElementById('wrapper-select-empleado').style.display = 'block';
    document.getElementById('select-empleado').required = true;

    // Resetear ojito
    document.getElementById('input-password').type     = 'password';
    document.getElementById('icono-password').className = 'fas fa-eye';

    $('#modal-usuario').modal('show');
}

// ─── ABRIR MODAL EDITAR ───────────────────────────────────────────────────────
function abrirModalEditarUsuario(id) {
    fetch('/usuarios/' + id)
        .then(function(response) {
            if (!response.ok) {
                alert('No se pudo cargar el usuario.');
                return null;
            }
            return response.json();
        })
        .then(function(usuario) {
            if (!usuario) return;

            document.getElementById('modal-titulo').textContent = 'Editar Usuario';

            // OCULTAR SELECTOR: Al editar, el empleado ya está fijo y no debe cambiarse
            document.getElementById('wrapper-select-empleado').style.display = 'none';
            document.getElementById('select-empleado').required = false;

            // Se remueve 'input-nombre' ya que se quitó del HTML de usuarios
            document.getElementById('input-username').value = usuario.userName   || '';
            document.getElementById('input-correo').value   = usuario.correo     || '';
            document.getElementById('input-perfil').value   = usuario.idPerfil   || '';

            // Password vacío — si no se toca se conserva la actual
            document.getElementById('input-password').value = '';

            // Resetear ojito
            document.getElementById('input-password').type   = 'password';
            document.getElementById('icono-password').className = 'fas fa-eye';

            document.getElementById('form-usuario').action = '/usuarios/' + id + '/editar';
            document.getElementById('form-usuario').method = 'post';

            $('#modal-usuario').modal('show');
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('Error al cargar los datos del usuario.');
        });
}

function reloadUsuariosTable() {
    fetch('/usuarios/tabla')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('No se pudo cargar la tabla de usuarios.');
            }
            return response.text();
        })
        .then(function(html) {
            var lifecycleActivo = estadoUsuarios.initialized;
            if (lifecycleActivo) {
                destruirTablaUsuarios();
            }

            var tbody = document.getElementById('tabla-usuarios-body');
            if (tbody) {
                tbody.outerHTML = html;
                if (lifecycleActivo) initTablaUsuarios();
            }
        })
        .catch(function(error) {
            console.error('Error recargando tabla de usuarios:', error);
            alert('No se pudo recargar la tabla de usuarios.');
        });
}

function manejarSubmitUsuario(event) {
    event.preventDefault();

    var formUsuario = estadoUsuarios.formUsuario;
    var url = formUsuario.action;
    var data = new URLSearchParams(new FormData(formUsuario)).toString();

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(function(response) {
        return response.json().then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Error guardando usuario.');
            }
            return body;
        });
    })
    .then(function(body) {
        if (body.status === 'OK') {
            $('#modal-usuario').modal('hide');
            reloadUsuariosTable();
        } else {
            throw new Error(body.message || 'Error guardando usuario.');
        }
    })
    .catch(function(error) {
        console.error('Error guardando usuario:', error);
        alert(error.message || 'Error guardando usuario.');
    });
}

// ─── CAMBIAR ESTADO (ACTIVAR / DESACTIVAR) ────────────────────────────────────
function cambiarEstado(id, nuevoEstado) {
    var mensaje = nuevoEstado === 1
        ? '¿Deseas activar este usuario?'
        : '¿Deseas desactivar este usuario?';

    if (!confirm(mensaje)) return;

    fetch('/usuarios/' + id + '/estado', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'estado=' + encodeURIComponent(nuevoEstado)
    })
    .then(function(response) {
        return response.json().then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Error cambiando estado.');
            }
            return body;
        });
    })
    .then(function(body) {
        if (body.status === 'OK') {
            reloadUsuariosTable();
        } else {
            throw new Error(body.message || 'Error cambiando estado.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert(error.message || 'Error al cambiar el estado.');
    });
}

// ─── ELIMINAR USUARIO ─────────────────────────────────────────────────────────
function eliminarUsuario(id) {
    estadoUsuarios.idUsuarioAEliminar = id;
    $('#modal-eliminar').modal('show');
}

// Confirmar eliminar desde el modal
function manejarConfirmacionEliminar() {
    if (!estadoUsuarios.idUsuarioAEliminar) return;

    fetch('/usuarios/' + estadoUsuarios.idUsuarioAEliminar + '/eliminar', {
        method: 'POST'
    })
    .then(function(response) {
        return response.json().then(function(body) {
            if (!response.ok) {
                throw new Error(body.message || 'Error eliminando usuario.');
            }
            return body;
        });
    })
    .then(function(body) {
        if (body.status === 'OK') {
            $('#modal-eliminar').modal('hide');
            estadoUsuarios.idUsuarioAEliminar = null;
            reloadUsuariosTable();
        } else {
            throw new Error(body.message || 'Error eliminando usuario.');
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        alert(error.message || 'Error al eliminar el usuario.');
    });
}

// Inicialización de DataTables para Usuarios
function initTablaUsuarios() {
    if (!window.jQuery || !$.fn.DataTable) return;

    const table = $('#tabla-usuarios');
    if (!table.length) return;
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

    estadoUsuarios.table = table[0];
    estadoUsuarios.dataTable = dataTable;

    // Forzar reajuste de columnas al cambiar el tamaño de la ventana o zoom
    estadoUsuarios.resizeHandler = function () {
        dataTable.columns.adjust().draw();
    };
    $(window).on('resize.usuarios', estadoUsuarios.resizeHandler);

    // Conectar controles personalizados
    estadoUsuarios.lengthSelect = document.getElementById('usuarios-length');
    estadoUsuarios.searchInput = document.getElementById('usuarios-search');

    // Cambiar número de registros por página
    if (estadoUsuarios.lengthSelect) {
        estadoUsuarios.lengthHandler = function () {
            dataTable.page.len(parseInt(estadoUsuarios.lengthSelect.value, 10)).draw();
        };
        estadoUsuarios.lengthSelect.addEventListener('change', estadoUsuarios.lengthHandler);
    }

    // Búsqueda personalizada
    if (estadoUsuarios.searchInput) {
        estadoUsuarios.searchHandler = function () {
            dataTable.search(estadoUsuarios.searchInput.value).draw();
        };
        estadoUsuarios.searchInput.addEventListener('keyup', estadoUsuarios.searchHandler);
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
            customPager.setAttribute('aria-label', 'Paginación de usuarios');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStylesUsuarios();
            renderCustomInfoUsuarios(dataTable, infoBar);
            renderCustomPaginationUsuarios(dataTable, customPager);
            estadoUsuarios.swipeCleanup = attachSwipePaginationUsuarios(wrapperEl, dataTable);

            estadoUsuarios.drawHandler = function () {
                renderCustomInfoUsuarios(dataTable, infoBar);
                renderCustomPaginationUsuarios(dataTable, customPager);
            };
            dataTable.on('draw.dt.usuarios', estadoUsuarios.drawHandler);
    }
}

function destruirTablaUsuarios() {
    if (estadoUsuarios.lengthSelect && estadoUsuarios.lengthHandler) {
        estadoUsuarios.lengthSelect.removeEventListener('change', estadoUsuarios.lengthHandler);
    }
    if (estadoUsuarios.searchInput && estadoUsuarios.searchHandler) {
        estadoUsuarios.searchInput.removeEventListener('keyup', estadoUsuarios.searchHandler);
    }
    if (estadoUsuarios.resizeHandler && window.jQuery) {
        $(window).off('resize.usuarios', estadoUsuarios.resizeHandler);
    }
    if (estadoUsuarios.swipeCleanup) estadoUsuarios.swipeCleanup();
    if (estadoUsuarios.dataTable) {
        if (estadoUsuarios.drawHandler) {
            estadoUsuarios.dataTable.off('draw.dt.usuarios', estadoUsuarios.drawHandler);
        }
        estadoUsuarios.dataTable.destroy();
    }
    estadoUsuarios.table = null;
    estadoUsuarios.dataTable = null;
    estadoUsuarios.resizeHandler = null;
    estadoUsuarios.lengthSelect = null;
    estadoUsuarios.lengthHandler = null;
    estadoUsuarios.searchInput = null;
    estadoUsuarios.searchHandler = null;
    estadoUsuarios.drawHandler = null;
    estadoUsuarios.swipeCleanup = null;
}

function initUsuarios() {
    if (estadoUsuarios.initialized) return;

    estadoUsuarios.formUsuario = document.getElementById('form-usuario');
    estadoUsuarios.submitHandler = manejarSubmitUsuario;
    if (estadoUsuarios.formUsuario) {
        estadoUsuarios.formUsuario.addEventListener('submit', estadoUsuarios.submitHandler);
    }

    estadoUsuarios.botonEliminar = document.getElementById('btn-confirmar-eliminar');
    estadoUsuarios.eliminarHandler = manejarConfirmacionEliminar;
    if (estadoUsuarios.botonEliminar) {
        estadoUsuarios.botonEliminar.addEventListener('click', estadoUsuarios.eliminarHandler);
    }

    estadoUsuarios.initialized = true;
    initTablaUsuarios();
}

function destruirModalesUsuarios() {
    if (window.jQuery && $.fn.modal) {
        $('#modal-usuario, #modal-eliminar').modal('hide');
    }
    estadoUsuarios.idUsuarioAEliminar = null;
}

function destroyUsuarios() {
    if (!estadoUsuarios.initialized) return;
    if (estadoUsuarios.formUsuario && estadoUsuarios.submitHandler) {
        estadoUsuarios.formUsuario.removeEventListener('submit', estadoUsuarios.submitHandler);
    }
    if (estadoUsuarios.botonEliminar && estadoUsuarios.eliminarHandler) {
        estadoUsuarios.botonEliminar.removeEventListener('click', estadoUsuarios.eliminarHandler);
    }
    destruirTablaUsuarios();
    destruirModalesUsuarios();
    estadoUsuarios.formUsuario = null;
    estadoUsuarios.submitHandler = null;
    estadoUsuarios.botonEliminar = null;
    estadoUsuarios.eliminarHandler = null;
    estadoUsuarios.idUsuarioAEliminar = null;
    estadoUsuarios.initialized = false;
}

window.AppModules.usuarios = {
    init: initUsuarios,
    destroy: destroyUsuarios,
    abrirModalNuevo: abrirModalNuevoUsuario,
    abrirModalEditar: abrirModalEditarUsuario,
    togglePassword: togglePassword,
    cambiarEstado: cambiarEstado,
    eliminarUsuario: eliminarUsuario,
    reloadTable: reloadUsuariosTable
};

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStylesUsuarios() {
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

function renderCustomInfoUsuarios(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPaginationUsuarios(dataTable, pagerElement) {
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

function attachSwipePaginationUsuarios(wrapperElement, dataTable) {
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
