window.AppModules = window.AppModules || {};

(function () {
    var estadoAsignacionMotos = {
        initialized: false,
        root: null,
        clickHandler: null,
        formAsignacion: null,
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
        filtroEstado: 'ACTIVA',
        fragmentAbortController: null,
        fragmentRequestId: 0,
        lifecycleId: 0
    };

    function esLifecycleActivo(lifecycleId) {
        return estadoAsignacionMotos.initialized
            && estadoAsignacionMotos.lifecycleId === lifecycleId
            && estadoAsignacionMotos.root
            && estadoAsignacionMotos.root.isConnected;
    }

    function abrirModalAsignacion() {
        if (window.jQuery && window.jQuery.fn.modal) {
            window.jQuery(estadoAsignacionMotos.root.querySelector('#modal-asignacion')).modal('show');
        }
    }

    function ocultarModalAsignacion() {
        if (window.jQuery && window.jQuery.fn.modal && estadoAsignacionMotos.root) {
            window.jQuery(estadoAsignacionMotos.root.querySelector('#modal-asignacion')).modal('hide');
        }
    }

    function mostrarErrorAsignacion(mensaje) {
        if (window.Swal) window.Swal.fire({ title: 'Error', text: mensaje, icon: 'error' });
    }

    function actualizarBotonesFiltroAsignaciones() {
        if (!estadoAsignacionMotos.root) return;
        estadoAsignacionMotos.root.querySelectorAll('.btn-filtro-asignaciones').forEach(function (boton) {
            var activo = boton.dataset.estado === estadoAsignacionMotos.filtroEstado;
            boton.classList.remove('btn-info', 'btn-secondary', 'btn-outline-secondary');
            boton.classList.toggle('btn-info', activo);
            boton.classList.toggle('btn-secondary', !activo);
            boton.setAttribute('aria-pressed', String(activo));
        });
    }

    function aplicarFiltroEstadoAsignaciones(estado) {
        estadoAsignacionMotos.filtroEstado = estado;
        actualizarBotonesFiltroAsignaciones();
        if (!estadoAsignacionMotos.dataTable) return;

        var busqueda = estado === 'ACTIVA' ? '^Activa$'
            : estado === 'FINALIZADA' ? '^Finalizada$' : '';
        estadoAsignacionMotos.dataTable.column(5).search(busqueda, Boolean(busqueda), false).draw();
    }

    function manejarClickAsignacion(event) {
        var botonNueva = event.target.closest('#btn-nueva-asignacion');
        if (botonNueva) {
            event.preventDefault();
            if (estadoAsignacionMotos.formAsignacion) estadoAsignacionMotos.formAsignacion.reset();
            var titulo = estadoAsignacionMotos.root.querySelector('#modal-titulo');
            if (titulo) titulo.textContent = 'Nueva Asignación';
            abrirModalAsignacion();
            return;
        }

        var botonFiltro = event.target.closest('.btn-filtro-asignaciones');
        if (botonFiltro) {
            event.preventDefault();
            aplicarFiltroEstadoAsignaciones(botonFiltro.dataset.estado);
            return;
        }

        var botonFinalizar = event.target.closest('.btn-finalizar-asignacion');
        if (botonFinalizar) {
            event.preventDefault();
            finalizarAsignacion(botonFinalizar.dataset.id);
        }
    }

    function manejarSubmitAsignacion(event) {
        event.preventDefault();
        var lifecycleId = estadoAsignacionMotos.lifecycleId;
        var form = estadoAsignacionMotos.formAsignacion;
        if (!form) return;

        fetch(form.action, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: serializarFormularioAsignacion(form)
        })
            .then(function (response) {
                return response.json().catch(function () { return {}; }).then(function (body) {
                    if (!response.ok) throw new Error(body.message || 'Error guardando asignación.');
                    return body;
                });
            })
            .then(function (body) {
                if (body.status !== 'OK' || !esLifecycleActivo(lifecycleId)) return false;
                ocultarModalAsignacion();
                return refrescarModuloAsignacionMotos(lifecycleId);
            })
            .then(function (refrescado) {
                if (!refrescado || !estadoAsignacionMotos.initialized || !estadoAsignacionMotos.root.isConnected) return;
                if (window.Swal) {
                    window.Swal.fire({
                        title: '¡Guardado!', text: 'La moto se asignó de manera exitosa.', icon: 'success',
                        timer: 2000, showConfirmButton: false
                    });
                }
            })
            .catch(function (error) {
                if (esLifecycleActivo(lifecycleId) && window.Swal) {
                    window.Swal.fire({
                        title: 'Advertencia',
                        text: error.message,
                        icon: 'warning',
                        confirmButtonColor: '#ffc107'
                    });
                }
            });
    }

    function serializarFormularioAsignacion(form) {
        return Array.from(new FormData(form).entries()).map(function (entrada) {
            return encodeURIComponent(entrada[0]) + '=' + encodeURIComponent(entrada[1]);
        }).join('&');
    }

    function finalizarAsignacion(id) {
        var lifecycleId = estadoAsignacionMotos.lifecycleId;
        if (!id || !window.Swal) return;

        window.Swal.fire({
            title: '¿Finalizar Asignación?', text: 'Se registrará la devolución de la moto en el sistema.', icon: 'info',
            showCancelButton: true, confirmButtonColor: '#28a745', cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, recibir unidad', cancelButtonText: 'Cancelar', reverseButtons: true
        }).then(function (result) {
            if (!result.isConfirmed || !esLifecycleActivo(lifecycleId)) return false;
            return fetch('/asignacion_motos/' + id + '/finalizar', { method: 'POST' })
                .then(function (response) {
                    return response.json().catch(function () { return {}; }).then(function (body) {
                        if (!response.ok) throw new Error(body.message || 'No se pudo procesar.');
                        return body;
                    });
                })
                .then(function (body) {
                    if (body.status !== 'OK' || !esLifecycleActivo(lifecycleId)) return false;
                    return refrescarModuloAsignacionMotos(lifecycleId);
                });
        }).then(function (refrescado) {
            if (!refrescado || !estadoAsignacionMotos.initialized || !estadoAsignacionMotos.root.isConnected) return;
            window.Swal.fire({
                title: '¡Recibida!', text: 'La unidad está libre para volver a ser asignada.', icon: 'success',
                timer: 1500, showConfirmButton: false
            });
        }).catch(function (error) {
            if (esLifecycleActivo(lifecycleId)) mostrarErrorAsignacion(error.message);
        });
    }

    function refrescarModuloAsignacionMotos(lifecycleId) {
        if (!esLifecycleActivo(lifecycleId)) return Promise.resolve(false);
        if (estadoAsignacionMotos.fragmentAbortController) estadoAsignacionMotos.fragmentAbortController.abort();

        var controller = new AbortController();
        var requestId = ++estadoAsignacionMotos.fragmentRequestId;
        estadoAsignacionMotos.fragmentAbortController = controller;

        return fetch('/asignacion_motos/fragment', {
            signal: controller.signal,
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'fetch', 'Accept': 'text/html' }
        })
            .then(function (response) {
                var urlFinal = new URL(response.url, window.location.origin);
                if (response.status === 401 || urlFinal.pathname === '/login') {
                    window.location.assign('/login');
                    return null;
                }
                if (!response.ok) throw new Error('No se pudo actualizar Asignación de Motos.');
                return response.text();
            })
            .then(function (html) {
                if (!html) return false;
                var documento = new DOMParser().parseFromString(html, 'text/html');
                var nuevoRoot = documento.querySelector('[data-modulo="asignacionMotos"]');
                if (!nuevoRoot) throw new Error('La respuesta no contiene Asignación de Motos.');
                nuevoRoot.querySelectorAll('script').forEach(function (script) { script.remove(); });
                if (!esLifecycleActivo(lifecycleId)
                    || requestId !== estadoAsignacionMotos.fragmentRequestId
                    || !estadoAsignacionMotos.root.isConnected) return false;

                estadoAsignacionMotos.fragmentAbortController = null;
                var rootAnterior = estadoAsignacionMotos.root;
                destroyAsignacionMotos();
                rootAnterior.replaceWith(nuevoRoot);
                initAsignacionMotos();
                return true;
            })
            .catch(function (error) {
                if (error.name === 'AbortError') return false;
                throw error;
            })
            .finally(function () {
                if (requestId === estadoAsignacionMotos.fragmentRequestId) {
                    estadoAsignacionMotos.fragmentAbortController = null;
                }
            });
    }

    function initTablaAsignaciones(filtroEstado) {
        if (!window.jQuery || !window.jQuery.fn.DataTable || !estadoAsignacionMotos.root) return;
        var $table = window.jQuery(estadoAsignacionMotos.root.querySelector('#tabla-asignaciones'));
        if (!$table.length) return;
        if (window.jQuery.fn.dataTable.isDataTable($table)) $table.DataTable().destroy();

        var dataTable = $table.DataTable({
            dom: 'rt<"bottom"ip><"clear">', paging: true, pageLength: 10,
            lengthMenu: [[10, 20, 30, 40, 50], [10, 20, 30, 40, 50]], lengthChange: true,
            searching: true, ordering: true, info: true, autoWidth: false, responsive: true,
            pagingType: 'simple', scrollY: '400px', scrollCollapse: true,
            language: {
                search: 'Buscar:', lengthMenu: 'Mostrar _MENU_ registros',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ registros', infoEmpty: 'No hay registros',
                infoFiltered: '(filtrado de _MAX_ registros)', zeroRecords: 'No se encontraron resultados',
                paginate: { previous: 'Anterior', next: 'Siguiente' }
            }
        });

        estadoAsignacionMotos.table = $table[0];
        estadoAsignacionMotos.dataTable = dataTable;
        estadoAsignacionMotos.resizeHandler = function () { dataTable.columns.adjust().draw(); };
        window.jQuery(window).on('resize.asignacionMotos', estadoAsignacionMotos.resizeHandler);
        estadoAsignacionMotos.lengthSelect = estadoAsignacionMotos.root.querySelector('#asignaciones-length');
        estadoAsignacionMotos.searchInput = estadoAsignacionMotos.root.querySelector('#asignaciones-search');

        if (estadoAsignacionMotos.lengthSelect) {
            estadoAsignacionMotos.lengthHandler = function () {
                dataTable.page.len(parseInt(estadoAsignacionMotos.lengthSelect.value, 10)).draw();
            };
            estadoAsignacionMotos.lengthSelect.addEventListener('change', estadoAsignacionMotos.lengthHandler);
        }
        if (estadoAsignacionMotos.searchInput) {
            estadoAsignacionMotos.searchHandler = function () {
                dataTable.search(estadoAsignacionMotos.searchInput.value).draw();
            };
            estadoAsignacionMotos.searchInput.addEventListener('keyup', estadoAsignacionMotos.searchHandler);
        }

        var wrapper = $table.closest('.dataTables_wrapper')[0];
        if (wrapper) {
            var paginadorNativo = wrapper.querySelector('.dataTables_paginate');
            if (paginadorNativo) paginadorNativo.style.display = 'none';
            var infoNativa = wrapper.querySelector('.dataTables_info');
            if (infoNativa) infoNativa.style.display = 'none';
            var pagerRow = document.createElement('div');
            pagerRow.className = 'asignaciones-pager-row';
            var infoBar = document.createElement('div');
            infoBar.className = 'asignaciones-info-bar';
            var paginador = document.createElement('div');
            paginador.className = 'asignaciones-custom-pagination';
            paginador.setAttribute('aria-label', 'Paginación de asignaciones');
            pagerRow.appendChild(infoBar);
            pagerRow.appendChild(paginador);
            wrapper.appendChild(pagerRow);

            injectCustomPaginationStylesAsignaciones();
            renderCustomInfoAsignaciones(dataTable, infoBar);
            renderCustomPaginationAsignaciones(dataTable, paginador);
            estadoAsignacionMotos.swipeCleanup = attachSwipePaginationAsignaciones(wrapper, dataTable);
            estadoAsignacionMotos.drawHandler = function () {
                renderCustomInfoAsignaciones(dataTable, infoBar);
                renderCustomPaginationAsignaciones(dataTable, paginador);
            };
            dataTable.on('draw.dt.asignacionMotos', estadoAsignacionMotos.drawHandler);
        }
        aplicarFiltroEstadoAsignaciones(filtroEstado || estadoAsignacionMotos.filtroEstado);
    }

    function destruirTablaAsignaciones() {
        if (estadoAsignacionMotos.lengthSelect && estadoAsignacionMotos.lengthHandler) {
            estadoAsignacionMotos.lengthSelect.removeEventListener('change', estadoAsignacionMotos.lengthHandler);
        }
        if (estadoAsignacionMotos.searchInput && estadoAsignacionMotos.searchHandler) {
            estadoAsignacionMotos.searchInput.removeEventListener('keyup', estadoAsignacionMotos.searchHandler);
        }
        if (window.jQuery && estadoAsignacionMotos.resizeHandler) {
            window.jQuery(window).off('resize.asignacionMotos', estadoAsignacionMotos.resizeHandler);
        }
        if (estadoAsignacionMotos.swipeCleanup) estadoAsignacionMotos.swipeCleanup();
        if (estadoAsignacionMotos.table) {
            var wrapper = estadoAsignacionMotos.table.closest('.dataTables_wrapper');
            if (wrapper) wrapper.querySelectorAll('.asignaciones-pager-row').forEach(function (pager) { pager.remove(); });
        }
        if (estadoAsignacionMotos.dataTable) {
            if (estadoAsignacionMotos.drawHandler) {
                estadoAsignacionMotos.dataTable.off('draw.dt.asignacionMotos', estadoAsignacionMotos.drawHandler);
            }
            estadoAsignacionMotos.dataTable.destroy();
        }
        estadoAsignacionMotos.table = null;
        estadoAsignacionMotos.dataTable = null;
        estadoAsignacionMotos.resizeHandler = null;
        estadoAsignacionMotos.lengthSelect = null;
        estadoAsignacionMotos.lengthHandler = null;
        estadoAsignacionMotos.searchInput = null;
        estadoAsignacionMotos.searchHandler = null;
        estadoAsignacionMotos.drawHandler = null;
        estadoAsignacionMotos.swipeCleanup = null;
    }

    function reloadAsignacionesTable() {
        var filtroActual = estadoAsignacionMotos.filtroEstado;
        return fetch('/asignacion_motos/tabla', {
            credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch', 'Accept': 'text/html' }
        })
            .then(function (response) {
                if (!response.ok) throw new Error('No se pudo actualizar la tabla de asignaciones.');
                return response.text();
            })
            .then(function (html) {
                if (!estadoAsignacionMotos.initialized || !estadoAsignacionMotos.root.isConnected) return;
                destruirTablaAsignaciones();
                var tbodyActual = estadoAsignacionMotos.root.querySelector('#tabla-asignaciones-body');
                if (!tbodyActual) throw new Error('No se encontró el cuerpo de la tabla de asignaciones.');
                tbodyActual.outerHTML = html;
                initTablaAsignaciones(filtroActual);
            })
            .catch(function (error) {
                if (estadoAsignacionMotos.initialized) mostrarErrorAsignacion(error.message);
            });
    }

    function initAsignacionMotos() {
        if (estadoAsignacionMotos.initialized) return;
        estadoAsignacionMotos.root = document.querySelector('[data-modulo="asignacionMotos"]');
        if (!estadoAsignacionMotos.root) return;
        estadoAsignacionMotos.lifecycleId += 1;
        estadoAsignacionMotos.filtroEstado = 'ACTIVA';
        estadoAsignacionMotos.clickHandler = manejarClickAsignacion;
        estadoAsignacionMotos.root.addEventListener('click', estadoAsignacionMotos.clickHandler);
        estadoAsignacionMotos.formAsignacion = estadoAsignacionMotos.root.querySelector('#form-asignacion');
        estadoAsignacionMotos.submitHandler = manejarSubmitAsignacion;
        if (estadoAsignacionMotos.formAsignacion) {
            estadoAsignacionMotos.formAsignacion.addEventListener('submit', estadoAsignacionMotos.submitHandler);
        }
        estadoAsignacionMotos.initialized = true;
        initTablaAsignaciones('ACTIVA');
        actualizarBotonesFiltroAsignaciones();
    }

    function destroyAsignacionMotos() {
        if (!estadoAsignacionMotos.initialized) return;
        if (estadoAsignacionMotos.root && estadoAsignacionMotos.clickHandler) {
            estadoAsignacionMotos.root.removeEventListener('click', estadoAsignacionMotos.clickHandler);
        }
        if (estadoAsignacionMotos.formAsignacion && estadoAsignacionMotos.submitHandler) {
            estadoAsignacionMotos.formAsignacion.removeEventListener('submit', estadoAsignacionMotos.submitHandler);
        }
        destruirTablaAsignaciones();
        if (estadoAsignacionMotos.fragmentAbortController) estadoAsignacionMotos.fragmentAbortController.abort();
        estadoAsignacionMotos.fragmentAbortController = null;
        estadoAsignacionMotos.fragmentRequestId += 1;
        estadoAsignacionMotos.lifecycleId += 1;
        ocultarModalAsignacion();
        estadoAsignacionMotos.root = null;
        estadoAsignacionMotos.clickHandler = null;
        estadoAsignacionMotos.formAsignacion = null;
        estadoAsignacionMotos.submitHandler = null;
        estadoAsignacionMotos.initialized = false;
    }

    function injectCustomPaginationStylesAsignaciones() {
        if (document.getElementById('asignaciones-custom-pagination-style')) return;
        var style = document.createElement('style');
        style.id = 'asignaciones-custom-pagination-style';
        style.textContent = '\n'
            + '.asignaciones-info-bar { display: flex; justify-content: flex-start; align-items: center; margin: 10px 0 6px; font-size: 0.95rem; color: #495057; font-weight: 600; flex: 1; }\n'
            + '.asignaciones-pager-row { display: flex; justify-content: space-between; align-items: center; width: 100%; margin-top: 4px; gap: 12px; }\n'
            + '.asignaciones-custom-pagination { display: flex; justify-content: flex-end; align-items: center; gap: 4px; padding: 6px 0; flex-wrap: nowrap; white-space: nowrap; overflow: hidden; }\n'
            + '.asignaciones-custom-pagination .page-btn { min-width: 38px; height: 38px; padding: 0 10px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #343a40; display: inline-flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; line-height: 1; }\n'
            + '.asignaciones-custom-pagination .page-btn.active { background: #0d6efd; color: #fff; border-color: #0d6efd; }\n'
            + '.asignaciones-custom-pagination .page-btn:disabled { opacity: 0.65; cursor: not-allowed; }\n'
            + '.asignaciones-custom-pagination .page-btn:hover:not(:disabled) { background: #e9ecef; }';
        document.head.appendChild(style);
    }

    function renderCustomInfoAsignaciones(dataTable, infoElement) {
        var info = dataTable.page.info();
        var totalRecords = info.recordsDisplay;
        var start = totalRecords === 0 ? 0 : info.start + 1;
        var end = totalRecords === 0 ? 0 : info.end;
        infoElement.textContent = totalRecords === 0
            ? 'No hay registros para mostrar'
            : 'Mostrando ' + start + ' a ' + end + ' de ' + totalRecords + ' registros';
    }

    function renderCustomPaginationAsignaciones(dataTable, pagerElement) {
        var info = dataTable.page.info();
        var totalPages = info.pages;
        var currentPage = info.page;
        pagerElement.innerHTML = '';
        if (totalPages <= 1) {
            pagerElement.style.display = 'none';
            return;
        }
        pagerElement.style.display = 'flex';
        var crearBoton = function (label, pageIndex, activo, deshabilitado) {
            var boton = document.createElement('button');
            boton.type = 'button';
            boton.textContent = label;
            boton.className = 'page-btn' + (activo ? ' active' : '');
            boton.setAttribute('aria-current', activo ? 'page' : 'false');
            boton.disabled = deshabilitado;
            if (!deshabilitado) {
                boton.addEventListener('click', function (event) {
                    event.preventDefault();
                    if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
                    dataTable.page(pageIndex).draw(false);
                });
            }
            return boton;
        };
        pagerElement.appendChild(crearBoton('‹', Math.max(0, currentPage - 1), false, currentPage === 0));
        var startPage = Math.max(0, Math.min(currentPage - 1, totalPages - 3));
        var endPage = Math.min(totalPages - 1, startPage + 2);
        for (var pageIndex = startPage; pageIndex <= endPage; pageIndex += 1) {
            pagerElement.appendChild(crearBoton(String(pageIndex + 1), pageIndex, pageIndex === currentPage, false));
        }
        pagerElement.appendChild(crearBoton('›', Math.min(totalPages - 1, currentPage + 1), false, currentPage >= totalPages - 1));
    }

    function attachSwipePaginationAsignaciones(wrapperElement, dataTable) {
        var touchStartX = 0;
        var touchStartHandler = function (event) { touchStartX = event.touches[0].clientX; };
        var touchEndHandler = function (event) {
            if (!event.changedTouches.length) return;
            var deltaX = event.changedTouches[0].clientX - touchStartX;
            if (Math.abs(deltaX) < 50) return;
            dataTable.page(deltaX < 0 ? 'next' : 'previous').draw(false);
        };
        wrapperElement.addEventListener('touchstart', touchStartHandler, { passive: true });
        wrapperElement.addEventListener('touchend', touchEndHandler, { passive: true });
        return function () {
            wrapperElement.removeEventListener('touchstart', touchStartHandler);
            wrapperElement.removeEventListener('touchend', touchEndHandler);
        };
    }

    window.AppModules.asignacionMotos = {
        init: initAsignacionMotos,
        destroy: destroyAsignacionMotos,
        reloadTable: reloadAsignacionesTable
    };
}());
