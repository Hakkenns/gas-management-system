window.AppModules = window.AppModules || {};

(function () {
    var estadoCategorias = {
        initialized: false, root: null, lifecycleId: 0, clickHandler: null,
        formCategoria: null, submitHandler: null, tipoUnidadField: null,
        tipoUnidadChangeHandler: null, inputNuevaCapacidad: null,
        capacidadKeypressHandler: null, btnAgregarCapacidad: null,
        capacidadClickHandler: null, modalCategoria: null, modalVerCapacidades: null,
        modalHiddenHandler: null, table: null, dataTable: null, resizeHandler: null,
        lengthSelect: null, lengthHandler: null, searchInput: null, searchHandler: null,
        drawHandler: null, swipeCleanup: null, editAbortController: null,
        editRequestId: 0, viewAbortController: null, viewRequestId: 0,
        validationTimeoutId: null, capacidadesActuales: [], unidadActual: ''
    };

    function activo(id) {
        return estadoCategorias.initialized && estadoCategorias.lifecycleId === id
            && estadoCategorias.root && estadoCategorias.root.isConnected;
    }

    function limites(unidad) {
        if (unidad === 'KG') return { min: 3, max: 45, decimales: 0, label: 'kg (3 a 45)' };
        if (unidad === 'L') return { min: 3, max: 21, decimales: 2, label: 'litros (3 a 21)' };
        if (unidad === 'M') return { min: 20, max: 100, decimales: 2, label: 'metros (20 a 100)' };
        return null;
    }

    function codigoPorLabel(label) {
        if (label === 'POR KILOS (KG)') return 'KG';
        if (label === 'POR LITROS (L)') return 'L';
        if (label === 'POR METROS (M)') return 'M';
        return '';
    }

    function labelPorCodigo(codigo) {
        if (codigo === 'KG') return 'POR KILOS (KG)';
        if (codigo === 'L') return 'POR LITROS (L)';
        if (codigo === 'M') return 'POR METROS (M)';
        return 'POR UNIDADES / PIEZAS';
    }

    function mostrarMensaje(texto) {
        var mensaje = estadoCategorias.root.querySelector('#mensaje-validacion-capacidad');
        if (!mensaje) return;
        if (estadoCategorias.validationTimeoutId) clearTimeout(estadoCategorias.validationTimeoutId);
        mensaje.textContent = texto;
        mensaje.style.display = 'block';
        var lifecycleId = estadoCategorias.lifecycleId;
        estadoCategorias.validationTimeoutId = setTimeout(function () {
            if (activo(lifecycleId)) mensaje.style.display = 'none';
        }, 3000);
    }

    function renderizarCapacidades() {
        var lista = estadoCategorias.root.querySelector('#lista-capacidades-categoria');
        var hidden = estadoCategorias.root.querySelector('#capacidades-json');
        if (!lista || !hidden) return;
        lista.innerHTML = '';
        var reglas = limites(estadoCategorias.unidadActual);
        estadoCategorias.capacidadesActuales.forEach(function (item, index) {
            var valor = Number(item.valor);
            var texto = reglas && reglas.decimales === 0 ? valor.toFixed(0) : valor.toFixed(2);
            var badge = document.createElement('span');
            badge.className = item.enUso
                ? 'badge badge-danger p-2 mr-1 mb-1'
                : 'badge badge-info p-2 mr-1 mb-1 d-inline-flex align-items-center';
            badge.style.fontSize = '14px';
            badge.textContent = texto;
            if (item.enUso) {
                badge.title = 'Capacidad en uso (no se puede eliminar)';
            } else {
                var remove = document.createElement('i');
                remove.className = 'fas fa-times ml-1 remove-capacity';
                remove.style.cssText = 'cursor: pointer; font-size: 14px;';
                remove.dataset.index = String(index);
                badge.appendChild(remove);
            }
            lista.appendChild(badge);
        });
        hidden.value = estadoCategorias.capacidadesActuales.map(function (item) {
            return Number(item.valor).toFixed(reglas && reglas.decimales === 0 ? 0 : 2);
        }).join(',');
    }

    function actualizarControlesUnidad() {
        var input = estadoCategorias.inputNuevaCapacidad;
        var contenedor = estadoCategorias.root.querySelector('#contenedor-capacidades-categoria');
        var mensaje = estadoCategorias.root.querySelector('#mensaje-validacion-capacidad');
        if (!input || !contenedor) return;
        var reglas = limites(estadoCategorias.unidadActual);
        var requiere = Boolean(reglas);
        contenedor.style.display = requiere ? 'block' : 'none';
        if (requiere) {
            input.placeholder = 'Ej: ' + reglas.min + ' (' + reglas.label + ')';
            input.step = reglas.decimales === 0 ? '1' : '0.01';
            input.min = reglas.min;
            input.max = reglas.max;
        } else {
            input.placeholder = '';
            input.step = '0.01';
            input.min = '0';
            input.max = '';
        }
        if (mensaje) mensaje.style.display = 'none';
    }

    function cambiarTipoUnidad() {
        var valor = estadoCategorias.tipoUnidadField.value;
        estadoCategorias.unidadActual = valor === 'POR KILOS (KG)' ? 'KG'
            : valor === 'POR LITROS (L)' ? 'L'
                : valor === 'POR METROS (M)' ? 'M' : '';
        var hidden = estadoCategorias.root.querySelector('#tipoUnidad-hidden');
        if (hidden) hidden.value = valor;
        estadoCategorias.capacidadesActuales = [];
        estadoCategorias.inputNuevaCapacidad.value = '';
        actualizarControlesUnidad();
        renderizarCapacidades();
    }

    function agregarCapacidad() {
        var reglas = limites(estadoCategorias.unidadActual);
        if (!reglas) return;
        var raw = estadoCategorias.inputNuevaCapacidad.value.trim();
        if (raw === '') {
            window.Swal.fire({ icon: 'warning', title: 'Campo vacío', text: 'Ingrese un valor entre ' + reglas.min + ' y ' + reglas.max + ' ' + reglas.label + '.', confirmButtonColor: '#28a745' });
            return;
        }
        var valor = parseFloat(raw);
        if (isNaN(valor) || valor < reglas.min || valor > reglas.max) {
            window.Swal.fire({ icon: 'error', title: 'Fuera de rango', text: 'El valor debe estar entre ' + reglas.min + ' y ' + reglas.max + ' ' + reglas.label + '.', confirmButtonColor: '#28a745' });
            return;
        }
        var redondeado = parseFloat(valor.toFixed(reglas.decimales));
        if (estadoCategorias.capacidadesActuales.some(function (item) { return Number(item.valor) === redondeado; })) {
            window.Swal.fire({ icon: 'error', title: 'Capacidad duplicada', text: 'Esa capacidad ya está registrada.', confirmButtonColor: '#28a745' });
            return;
        }
        estadoCategorias.capacidadesActuales.push({ valor: redondeado, enUso: false });
        estadoCategorias.capacidadesActuales.sort(function (a, b) { return a.valor - b.valor; });
        estadoCategorias.inputNuevaCapacidad.value = '';
        renderizarCapacidades();
        estadoCategorias.inputNuevaCapacidad.focus();
    }

    function abrirModal(titulo, id, nombre, descripcion, tipoUnidad, capacidades, edicion) {
        var root = estadoCategorias.root;
        root.querySelector('#modal-titulo-categoria').textContent = titulo;
        root.querySelector('#categoria-id').value = id || '';
        root.querySelector('#nombre-categoria').value = nombre || '';
        root.querySelector('#descripcion-categoria').value = descripcion || '';
        estadoCategorias.tipoUnidadField.value = tipoUnidad || '';
        estadoCategorias.tipoUnidadField.disabled = Boolean(edicion);
        var hidden = root.querySelector('#tipoUnidad-hidden');
        if (hidden) hidden.value = tipoUnidad || '';
        estadoCategorias.unidadActual = codigoPorLabel(tipoUnidad || '');
        estadoCategorias.capacidadesActuales = Array.isArray(capacidades) ? capacidades.map(function (item) {
            return typeof item === 'object' ? { valor: item.valor, enUso: item.enUso } : { valor: item, enUso: false };
        }) : [];
        estadoCategorias.capacidadesActuales.sort(function (a, b) { return a.valor - b.valor; });
        actualizarControlesUnidad();
        renderizarCapacidades();
        if (window.jQuery && window.jQuery.fn.modal) window.jQuery(estadoCategorias.modalCategoria).modal('show');
    }

    function abrirNuevaCategoria() {
        estadoCategorias.formCategoria.reset();
        var hidden = estadoCategorias.root.querySelector('#tipoUnidad-hidden');
        estadoCategorias.tipoUnidadField.disabled = false;
        estadoCategorias.tipoUnidadField.value = '';
        if (hidden) { hidden.value = ''; hidden.removeAttribute('name'); }
        estadoCategorias.capacidadesActuales = [];
        estadoCategorias.unidadActual = '';
        estadoCategorias.root.querySelector('#input-nueva-capacidad').value = '';
        actualizarControlesUnidad();
        renderizarCapacidades();
        abrirModal('Nueva Categoría', '', '', '', '', [], false);
    }

    function verCapacidades(id, nombre, lifecycleId) {
        if (estadoCategorias.viewAbortController) estadoCategorias.viewAbortController.abort();
        var requestId = ++estadoCategorias.viewRequestId;
        var controller = new AbortController();
        estadoCategorias.viewAbortController = controller;
        var contenedor = estadoCategorias.root.querySelector('#contenedor-lista-capacidades');
        estadoCategorias.root.querySelector('#modal-cap-nombre').textContent = nombre;
        contenedor.innerHTML = '<p class="text-muted mb-0">Cargando...</p>';
        if (window.jQuery && window.jQuery.fn.modal) window.jQuery(estadoCategorias.modalVerCapacidades).modal('show');
        fetch('/categorias/' + id, { signal: controller.signal })
            .then(function (response) { return response.json(); })
            .then(function (data) {
                if (!activo(lifecycleId) || requestId !== estadoCategorias.viewRequestId) return;
                var caps = data.capacidades || [];
                if (!caps.length) {
                    contenedor.innerHTML = '<p class="text-muted mb-0">No hay capacidades registradas.</p>';
                    return;
                }
                var lista = document.createElement('ul');
                lista.className = 'list-unstyled text-left';
                lista.style.cssText = 'max-width: 250px; margin: 0 auto;';
                caps.forEach(function (item) {
                    var valor = typeof item === 'object' ? item.valor : item;
                    var enUso = typeof item === 'object' && item.enUso;
                    var li = document.createElement('li');
                    li.className = 'mb-1';
                    li.innerHTML = '<i class="fas ' + (enUso ? 'fa-lock text-danger' : 'fa-check-circle text-success') + ' mr-1"></i> <strong>' + Number(valor).toFixed(2) + '</strong>';
                    lista.appendChild(li);
                });
                contenedor.innerHTML = '';
                contenedor.appendChild(lista);
            })
            .catch(function (error) {
                if (error.name !== 'AbortError' && activo(lifecycleId) && requestId === estadoCategorias.viewRequestId) {
                    contenedor.innerHTML = '<p class="text-danger mb-0">Error al cargar capacidades.</p>';
                }
            })
            .finally(function () {
                if (requestId === estadoCategorias.viewRequestId) estadoCategorias.viewAbortController = null;
            });
    }

    function editarCategoria(boton, lifecycleId) {
        if (estadoCategorias.editAbortController) estadoCategorias.editAbortController.abort();
        var requestId = ++estadoCategorias.editRequestId;
        var controller = new AbortController();
        estadoCategorias.editAbortController = controller;
        var id = boton.dataset.id || '';
        var nombre = boton.dataset.nombre || '';
        var descripcion = boton.dataset.descripcion || '';
        var unidad = boton.dataset.unidadmedida || '';
        var tipo = unidad ? labelPorCodigo(unidad) : (boton.dataset.tipounidad || '');
        fetch('/categorias/' + id, { signal: controller.signal })
            .then(function (response) { return response.json(); })
            .then(function (data) {
                if (activo(lifecycleId) && requestId === estadoCategorias.editRequestId) {
                    abrirModal('Editar Categoría', id, nombre, descripcion, tipo, data.capacidades || [], true);
                }
            })
            .catch(function (error) {
                if (error.name !== 'AbortError' && activo(lifecycleId) && requestId === estadoCategorias.editRequestId) {
                    abrirModal('Editar Categoría', id, nombre, descripcion, tipo, [], true);
                }
            })
            .finally(function () {
                if (requestId === estadoCategorias.editRequestId) estadoCategorias.editAbortController = null;
            });
    }

    function cambiarEstado(id, estado, lifecycleId) {
        var mensaje = estado == 1 ? '¿Deseas activar esta categoría?' : '¿Deseas inhabilitar esta categoría?';
        if (!window.confirm(mensaje)) return;
        fetch('/categorias/' + id + '/estado?estado=' + estado, { method: 'POST' })
            .then(function (response) { return response.json(); })
            .then(function (data) {
                if (!activo(lifecycleId)) return;
                if (data.status === 'OK') reloadCategoriasTable();
                else window.alert(data.message || 'Error al cambiar el estado');
            })
            .catch(function (error) { if (activo(lifecycleId)) console.error('Error:', error); });
    }

    function eliminarCategoria(id, lifecycleId) {
        window.Swal.fire({
            title: '¿Estás seguro?', text: 'La categoría se eliminará del sistema.', icon: 'warning',
            showCancelButton: true, confirmButtonColor: '#dc3545', cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, eliminar', cancelButtonText: 'Cancelar', reverseButtons: true
        }).then(function (result) {
            if (!result.isConfirmed || !activo(lifecycleId)) return;
            return fetch('/categorias/' + id + '/eliminar', { method: 'POST' })
                .then(function (response) { return response.json(); })
                .then(function (data) {
                    if (!activo(lifecycleId)) return;
                    if (data.status === 'OK') {
                        reloadCategoriasTable();
                        window.Swal.fire({ icon: 'success', title: '¡Eliminado!', text: 'La categoría ha sido eliminada.', timer: 1500, showConfirmButton: false });
                    } else {
                        window.Swal.fire({ icon: 'error', title: 'No se puede eliminar', text: data.message || 'Error al eliminar la categoría.', confirmButtonColor: '#28a745' });
                    }
                });
        }).catch(function (error) {
            console.error('Error:', error);
            if (activo(lifecycleId)) {
                window.Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'Ocurrió un error al eliminar la categoría.',
                    confirmButtonColor: '#28a745'
                });
            }
        });
    }

    function manejarClick(event) {
        var target = event.target;
        if (target.closest('#btn-crear-categoria')) { event.preventDefault(); abrirNuevaCategoria(); return; }
        var remove = target.closest('.remove-capacity');
        if (remove) { estadoCategorias.capacidadesActuales.splice(parseInt(remove.dataset.index, 10), 1); renderizarCapacidades(); return; }
        var ver = target.closest('.btn-ver-capacidades');
        if (ver) { verCapacidades(ver.dataset.id, ver.dataset.nombre, estadoCategorias.lifecycleId); return; }
        var editar = target.closest('.btn-editar-categoria');
        if (editar) { editarCategoria(editar, estadoCategorias.lifecycleId); return; }
        var estado = target.closest('.btn-cambiar-estado-categoria');
        if (estado) { cambiarEstado(estado.dataset.id, estado.dataset.estado, estadoCategorias.lifecycleId); return; }
        var eliminar = target.closest('.btn-eliminar-categoria');
        if (eliminar) eliminarCategoria(eliminar.dataset.id, estadoCategorias.lifecycleId);
    }

    function manejarSubmit(event) {
        event.preventDefault();
        var lifecycleId = estadoCategorias.lifecycleId;
        var form = estadoCategorias.formCategoria;
        var submit = form.querySelector('button[type="submit"]');
        if (submit) { submit.disabled = true; submit.textContent = 'Guardando...'; }
        var data = new FormData(form);
        var hidden = estadoCategorias.root.querySelector('#tipoUnidad-hidden');
        if (hidden && hidden.value) data.set('tipoUnidad', hidden.value);
        else data.set('tipoUnidad', estadoCategorias.tipoUnidadField.value);
        fetch('/categorias/ajax', { method: 'POST', body: data })
            .then(function (response) { return response.json(); })
            .then(function (result) {
                if (!activo(lifecycleId)) return;
                if (result.status === 'OK') {
                    if (window.jQuery && window.jQuery.fn.modal) window.jQuery(estadoCategorias.modalCategoria).modal('hide');
                    reloadCategoriasTable();
                    window.Swal.fire({ icon: 'success', title: '¡Guardado!', text: 'La categoría se ha guardado correctamente.', timer: 2000, showConfirmButton: false });
                } else {
                    window.Swal.fire({ icon: 'error', title: 'Error', text: result.message || 'Error guardando categoría.', confirmButtonColor: '#28a745' });
                }
            })
            .catch(function () {
                if (activo(lifecycleId)) window.Swal.fire({ icon: 'error', title: 'Error', text: 'Ocurrió un error al guardar la categoría.', confirmButtonColor: '#28a745' });
            })
            .finally(function () { if (submit && activo(lifecycleId)) { submit.disabled = false; submit.textContent = 'Guardar'; } });
    }

    function initTablaCategorias() {
        if (!window.jQuery || !window.jQuery.fn.DataTable || !estadoCategorias.root) return;
        var $table = window.jQuery(estadoCategorias.root.querySelector('#tabla-categorias'));
        if (!$table.length) return;
        if (window.jQuery.fn.dataTable.isDataTable($table)) $table.DataTable().destroy();
        var dt = $table.DataTable({
            dom: 'rt<"bottom"ip><"clear">', paging: true, pageLength: 10,
            lengthMenu: [[10, 20, 30, 40, 50], [10, 20, 30, 40, 50]], lengthChange: true,
            searching: true, ordering: true, info: true, autoWidth: false, responsive: true,
            pagingType: 'simple', scrollY: '400px', scrollCollapse: true,
            language: { search: 'Buscar:', lengthMenu: 'Mostrar _MENU_ registros', info: 'Mostrando _START_ a _END_ de _TOTAL_ registros', infoEmpty: 'No hay registros', infoFiltered: '(filtrado de _MAX_ registros)', zeroRecords: 'No se encontraron resultados', paginate: { previous: 'Anterior', next: 'Siguiente' } }
        });
        estadoCategorias.table = $table[0]; estadoCategorias.dataTable = dt;
        estadoCategorias.resizeHandler = function () { dt.columns.adjust().draw(); };
        window.jQuery(window).on('resize.categorias', estadoCategorias.resizeHandler);
        estadoCategorias.lengthSelect = estadoCategorias.root.querySelector('#categorias-length');
        estadoCategorias.searchInput = estadoCategorias.root.querySelector('#categorias-search');
        if (estadoCategorias.lengthSelect) {
            estadoCategorias.lengthHandler = function () { dt.page.len(parseInt(estadoCategorias.lengthSelect.value, 10)).draw(); };
            estadoCategorias.lengthSelect.addEventListener('change', estadoCategorias.lengthHandler);
        }
        if (estadoCategorias.searchInput) {
            estadoCategorias.searchHandler = function () { dt.search(estadoCategorias.searchInput.value).draw(); };
            estadoCategorias.searchInput.addEventListener('keyup', estadoCategorias.searchHandler);
        }
        var wrapper = $table.closest('.dataTables_wrapper')[0];
        if (wrapper) {
            var nativePager = wrapper.querySelector('.dataTables_paginate'); if (nativePager) nativePager.style.display = 'none';
            var nativeInfo = wrapper.querySelector('.dataTables_info'); if (nativeInfo) nativeInfo.style.display = 'none';
            var pagerRow = document.createElement('div'); pagerRow.className = 'categorias-pager-row';
            var info = document.createElement('div'); info.className = 'categorias-info-bar';
            var pager = document.createElement('div'); pager.className = 'categorias-custom-pagination'; pager.setAttribute('aria-label', 'Paginación de categorías');
            pagerRow.appendChild(info); pagerRow.appendChild(pager); wrapper.appendChild(pagerRow);
            injectCustomPaginationStylesCategorias(); renderCustomInfoCategorias(dt, info); renderCustomPaginationCategorias(dt, pager);
            estadoCategorias.swipeCleanup = attachSwipePaginationCategorias(wrapper, dt);
            estadoCategorias.drawHandler = function () { renderCustomInfoCategorias(dt, info); renderCustomPaginationCategorias(dt, pager); };
            dt.on('draw.dt.categorias', estadoCategorias.drawHandler);
        }
    }

    function destruirTablaCategorias() {
        if (estadoCategorias.lengthSelect && estadoCategorias.lengthHandler) estadoCategorias.lengthSelect.removeEventListener('change', estadoCategorias.lengthHandler);
        if (estadoCategorias.searchInput && estadoCategorias.searchHandler) estadoCategorias.searchInput.removeEventListener('keyup', estadoCategorias.searchHandler);
        if (window.jQuery && estadoCategorias.resizeHandler) window.jQuery(window).off('resize.categorias', estadoCategorias.resizeHandler);
        if (estadoCategorias.swipeCleanup) estadoCategorias.swipeCleanup();
        if (estadoCategorias.table) { var wrapper = estadoCategorias.table.closest('.dataTables_wrapper'); if (wrapper) wrapper.querySelectorAll('.categorias-pager-row').forEach(function (row) { row.remove(); }); }
        if (estadoCategorias.dataTable) { if (estadoCategorias.drawHandler) estadoCategorias.dataTable.off('draw.dt.categorias', estadoCategorias.drawHandler); estadoCategorias.dataTable.destroy(); }
        estadoCategorias.table = null; estadoCategorias.dataTable = null; estadoCategorias.resizeHandler = null;
        estadoCategorias.lengthSelect = null; estadoCategorias.lengthHandler = null; estadoCategorias.searchInput = null;
        estadoCategorias.searchHandler = null; estadoCategorias.drawHandler = null; estadoCategorias.swipeCleanup = null;
    }

    function reloadCategoriasTable() {
        var lifecycleId = estadoCategorias.lifecycleId;
        return fetch('/categorias/tabla', { credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch', 'Accept': 'text/html' } })
            .then(function (response) { if (!response.ok) throw new Error('Error cargando tabla de categorías'); return response.text(); })
            .then(function (html) {
                if (!activo(lifecycleId)) return;
                destruirTablaCategorias();
                var tbody = estadoCategorias.root.querySelector('#tabla-categorias-body');
                if (!tbody) throw new Error('No se encontró el cuerpo de la tabla de categorías.');
                tbody.outerHTML = html;
                initTablaCategorias();
            })
            .catch(function (error) { if (activo(lifecycleId)) console.error('ERROR RECARGANDO TABLA:', error); });
    }

    function injectCustomPaginationStylesCategorias() {
        if (document.getElementById('categorias-custom-pagination-style')) return;
        var style = document.createElement('style'); style.id = 'categorias-custom-pagination-style';
        style.textContent = '.categorias-info-bar{display:flex;justify-content:flex-start;align-items:center;margin:10px 0 6px;font-size:.95rem;color:#495057;font-weight:600;flex:1}.categorias-pager-row{display:flex;justify-content:space-between;align-items:center;width:100%;margin-top:4px;gap:12px}.categorias-custom-pagination{display:flex;justify-content:flex-end;align-items:center;gap:4px;padding:6px 0;flex-wrap:nowrap;white-space:nowrap;overflow:hidden}.categorias-custom-pagination .page-btn{min-width:38px;height:38px;padding:0 10px;border:1px solid #ced4da;border-radius:6px;background:#fff;color:#343a40;display:inline-flex;align-items:center;justify-content:center;font-weight:600;cursor:pointer;line-height:1}.categorias-custom-pagination .page-btn.active{background:#0d6efd;color:#fff;border-color:#0d6efd}.categorias-custom-pagination .page-btn:disabled{opacity:.65;cursor:not-allowed}.categorias-custom-pagination .page-btn:hover:not(:disabled){background:#e9ecef}';
        document.head.appendChild(style);
    }

    function renderCustomInfoCategorias(dt, infoElement) {
        var info = dt.page.info(); var total = info.recordsTotal; var start = total === 0 ? 0 : info.start + 1; var end = total === 0 ? 0 : info.end;
        infoElement.textContent = total === 0 ? 'No hay registros para mostrar' : 'Mostrando ' + start + ' a ' + end + ' de ' + total + ' registros';
    }

    function renderCustomPaginationCategorias(dt, pager) {
        var info = dt.page.info(); pager.innerHTML = '';
        if (info.pages <= 1) { pager.style.display = 'none'; return; }
        pager.style.display = 'flex';
        function button(label, page, current, disabled) {
            var el = document.createElement('button'); el.type = 'button'; el.textContent = label; el.className = 'page-btn' + (current ? ' active' : ''); el.disabled = disabled; el.setAttribute('aria-current', current ? 'page' : 'false');
            if (!disabled) el.addEventListener('click', function (event) { event.preventDefault(); dt.page(page).draw(false); });
            return el;
        }
        pager.appendChild(button('‹', Math.max(0, info.page - 1), false, info.page === 0));
        var start = Math.max(0, Math.min(info.page - 1, info.pages - 3)); var end = Math.min(info.pages - 1, start + 2);
        for (var page = start; page <= end; page += 1) pager.appendChild(button(String(page + 1), page, page === info.page, false));
        pager.appendChild(button('›', Math.min(info.pages - 1, info.page + 1), false, info.page >= info.pages - 1));
    }

    function attachSwipePaginationCategorias(wrapper, dt) {
        var startX = 0;
        function touchStart(event) { startX = event.touches[0].clientX; }
        function touchEnd(event) { if (!event.changedTouches.length) return; var delta = event.changedTouches[0].clientX - startX; if (Math.abs(delta) >= 50) dt.page(delta < 0 ? 'next' : 'previous').draw(false); }
        wrapper.addEventListener('touchstart', touchStart, { passive: true }); wrapper.addEventListener('touchend', touchEnd, { passive: true });
        return function () { wrapper.removeEventListener('touchstart', touchStart); wrapper.removeEventListener('touchend', touchEnd); };
    }

    function initCategorias() {
        if (estadoCategorias.initialized) return;
        estadoCategorias.root = document.querySelector('[data-modulo="categorias"]'); if (!estadoCategorias.root) return;
        estadoCategorias.lifecycleId += 1; estadoCategorias.initialized = true;
        estadoCategorias.modalCategoria = estadoCategorias.root.querySelector('#modal-categoria');
        estadoCategorias.modalVerCapacidades = estadoCategorias.root.querySelector('#modal-ver-capacidades');
        estadoCategorias.formCategoria = estadoCategorias.root.querySelector('#form-categoria');
        estadoCategorias.tipoUnidadField = estadoCategorias.root.querySelector('#tipoUnidad');
        estadoCategorias.inputNuevaCapacidad = estadoCategorias.root.querySelector('#input-nueva-capacidad');
        estadoCategorias.btnAgregarCapacidad = estadoCategorias.root.querySelector('#btn-agregar-capacidad');
        var input = estadoCategorias.inputNuevaCapacidad.parentNode.parentNode; var mensaje = document.createElement('small'); mensaje.className = 'form-text text-danger mt-1'; mensaje.id = 'mensaje-validacion-capacidad'; mensaje.style.display = 'none'; input.appendChild(mensaje);
        estadoCategorias.clickHandler = manejarClick; estadoCategorias.root.addEventListener('click', estadoCategorias.clickHandler);
        estadoCategorias.submitHandler = manejarSubmit; if (estadoCategorias.formCategoria) estadoCategorias.formCategoria.addEventListener('submit', estadoCategorias.submitHandler);
        estadoCategorias.tipoUnidadChangeHandler = cambiarTipoUnidad; estadoCategorias.tipoUnidadField.addEventListener('change', estadoCategorias.tipoUnidadChangeHandler);
        estadoCategorias.capacidadClickHandler = agregarCapacidad; estadoCategorias.btnAgregarCapacidad.addEventListener('click', estadoCategorias.capacidadClickHandler);
        estadoCategorias.capacidadKeypressHandler = function (event) { if (event.key === 'Enter') { event.preventDefault(); agregarCapacidad(); } };
        estadoCategorias.inputNuevaCapacidad.addEventListener('keypress', estadoCategorias.capacidadKeypressHandler);
        estadoCategorias.modalHiddenHandler = function () { estadoCategorias.inputNuevaCapacidad.value = ''; estadoCategorias.capacidadesActuales = []; estadoCategorias.unidadActual = ''; renderizarCapacidades(); estadoCategorias.tipoUnidadField.disabled = false; estadoCategorias.tipoUnidadField.value = ''; var hidden = estadoCategorias.root.querySelector('#tipoUnidad-hidden'); if (hidden) { hidden.removeAttribute('name'); hidden.value = ''; } actualizarControlesUnidad(); };
        if (window.jQuery && window.jQuery.fn.on) window.jQuery(estadoCategorias.modalCategoria).on('hidden.bs.modal.categorias', estadoCategorias.modalHiddenHandler);
        initTablaCategorias();
    }

    function destroyCategorias() {
        if (!estadoCategorias.initialized) return;
        estadoCategorias.root.removeEventListener('click', estadoCategorias.clickHandler);
        if (estadoCategorias.formCategoria) estadoCategorias.formCategoria.removeEventListener('submit', estadoCategorias.submitHandler);
        estadoCategorias.tipoUnidadField.removeEventListener('change', estadoCategorias.tipoUnidadChangeHandler);
        estadoCategorias.inputNuevaCapacidad.removeEventListener('keypress', estadoCategorias.capacidadKeypressHandler);
        estadoCategorias.btnAgregarCapacidad.removeEventListener('click', estadoCategorias.capacidadClickHandler);
        if (window.jQuery && window.jQuery.fn.off) window.jQuery(estadoCategorias.modalCategoria).off('hidden.bs.modal.categorias', estadoCategorias.modalHiddenHandler);
        destruirTablaCategorias();
        if (estadoCategorias.editAbortController) estadoCategorias.editAbortController.abort(); if (estadoCategorias.viewAbortController) estadoCategorias.viewAbortController.abort();
        estadoCategorias.editAbortController = null; estadoCategorias.viewAbortController = null; estadoCategorias.editRequestId += 1; estadoCategorias.viewRequestId += 1;
        if (estadoCategorias.validationTimeoutId) clearTimeout(estadoCategorias.validationTimeoutId); estadoCategorias.validationTimeoutId = null;
        if (window.jQuery && window.jQuery.fn.modal) { window.jQuery(estadoCategorias.modalCategoria).modal('hide'); window.jQuery(estadoCategorias.modalVerCapacidades).modal('hide'); }
        estadoCategorias.lifecycleId += 1;
        estadoCategorias.root = null;
        estadoCategorias.clickHandler = null;
        estadoCategorias.formCategoria = null;
        estadoCategorias.submitHandler = null;
        estadoCategorias.tipoUnidadField = null;
        estadoCategorias.tipoUnidadChangeHandler = null;
        estadoCategorias.inputNuevaCapacidad = null;
        estadoCategorias.capacidadKeypressHandler = null;
        estadoCategorias.btnAgregarCapacidad = null;
        estadoCategorias.capacidadClickHandler = null;
        estadoCategorias.modalCategoria = null;
        estadoCategorias.modalVerCapacidades = null;
        estadoCategorias.modalHiddenHandler = null;
        estadoCategorias.capacidadesActuales = [];
        estadoCategorias.unidadActual = '';
        estadoCategorias.initialized = false;
    }

    window.AppModules.categorias = { init: initCategorias, destroy: destroyCategorias, reloadTable: reloadCategoriasTable };
}());
