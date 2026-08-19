window.AppModules = window.AppModules || {};

(function () {
    const estadoCompras = {
        initialized: false,
        root: null,
        lifecycleId: 0,
        controllers: new Set(),
        requestIds: {
            tabla: 0,
            correlativo: 0,
            detalle: 0,
            costo: 0,
            catalogo: 0,
            selector: 0
        },
        listeners: [],
        jqueryHandlers: [],
        dataTable: null,
        dateFilter: null,
        swipeHandlers: [],
        sidebarResizeObserver: null,
        sidebarReflowFrame: null,
        timers: new Set(),
        selectorController: null,
        selectorDebounceTimer: null,
        proveedorSelectorId: null,
        productosSelector: [],
        productosFiltradosSelector: [],
        proveedoresSelector: [],
        paginaProveedoresSelector: 1,
        paginaProductosSelector: 1
    };

    function activo(lifecycleId) {
        return estadoCompras.initialized
            && estadoCompras.lifecycleId === lifecycleId
            && estadoCompras.root
            && estadoCompras.root.isConnected;
    }

    function registrarController(controller) {
        estadoCompras.controllers.add(controller);
        return controller;
    }

    function registrarListener(target, type, handler, options) {
        if (!target) return;
        target.addEventListener(type, handler, options);
        estadoCompras.listeners.push({ target, type, handler, options });
    }

    function registrarJQueryHandler(target, events, handler) {
        if (!target || !window.jQuery) return;
        window.jQuery(target).on(events, handler);
        estadoCompras.jqueryHandlers.push({ target, events, handler });
    }

    function registrarTimer(callback, delay) {
        const timer = setTimeout(() => {
            estadoCompras.timers.delete(timer);
            callback();
        }, delay);
        estadoCompras.timers.add(timer);
        return timer;
    }

    function mostrarEstadoSelector(mensaje, clase = 'text-muted') {
        const lista = estadoCompras.root && estadoCompras.root.querySelector('#lista-proveedores-selector');
        const paginador = estadoCompras.root && estadoCompras.root.querySelector('#paginador-proveedores-selector');
        if (lista) lista.innerHTML = `<div class="${clase} text-center py-4">${mensaje}</div>`;
        if (paginador) paginador.innerHTML = '';
    }

    function renderizarProveedoresSelector(proveedores) {
        const lista = estadoCompras.root && estadoCompras.root.querySelector('#lista-proveedores-selector');
        const contador = estadoCompras.root && estadoCompras.root.querySelector('#contador-proveedores-selector');
        const paginador = estadoCompras.root && estadoCompras.root.querySelector('#paginador-proveedores-selector');
        if (!lista) return;
        estadoCompras.proveedoresSelector = proveedores;
        const porPagina = 4;
        const totalPaginas = Math.max(1, Math.ceil(proveedores.length / porPagina));
        estadoCompras.paginaProveedoresSelector = Math.min(estadoCompras.paginaProveedoresSelector, totalPaginas);
        const inicio = (estadoCompras.paginaProveedoresSelector - 1) * porPagina;
        const pagina = proveedores.slice(inicio, inicio + porPagina);
        if (contador) contador.textContent = proveedores.length ? `(${proveedores.length})` : '';
        if (!proveedores.length) { mostrarEstadoSelector('No se encontraron proveedores.'); if (paginador) paginador.innerHTML = ''; return; }
        lista.innerHTML = pagina.map(proveedor => `<button type="button" class="proveedor-selector-item proveedor-selector-boton border rounded mb-2 text-left w-100 bg-white" data-id="${proveedor.idProveedor}" data-nombre="${proveedor.nombreProveedor || ''}" data-ruc="${proveedor.ruc || ''}"><div class="d-flex align-items-center"><div class="flex-grow-1 pr-2"><div class="font-weight-bold text-dark">${proveedor.nombreProveedor || ''}</div><small class="text-muted">RUC: ${proveedor.ruc || '—'}</small></div><span class="badge badge-light border text-primary mr-2">${proveedor.cantidadProductos} productos</span><i class="fas fa-chevron-right text-primary"></i></div></button>`).join('');
        if (paginador) paginador.innerHTML = crearPaginadorSelector('proveedor', estadoCompras.paginaProveedoresSelector, totalPaginas);
        const actual = lista.querySelector(`[data-id="${estadoCompras.proveedorSelectorId || ''}"]`);
        if (actual) {
            actual.classList.add('proveedor-selector-seleccionado');
            const badge = actual.querySelector('.badge');
            if (badge) badge.className = 'badge badge-primary mr-2';
        }
    }

    function renderizarProductosSelector(productos) {
        const lista = estadoCompras.root && estadoCompras.root.querySelector('#lista-productos-selector');
        const contador = estadoCompras.root && estadoCompras.root.querySelector('#contador-productos-selector');
        const paginador = estadoCompras.root && estadoCompras.root.querySelector('#paginador-productos-selector');
        const info = estadoCompras.root && estadoCompras.root.querySelector('#info-productos-selector');
        if (!lista) return;
        const porPagina = 6;
        const totalPaginas = Math.max(1, Math.ceil(productos.length / porPagina));
        estadoCompras.paginaProductosSelector = Math.min(estadoCompras.paginaProductosSelector, totalPaginas);
        const inicio = (estadoCompras.paginaProductosSelector - 1) * porPagina;
        const pagina = productos.slice(inicio, inicio + porPagina);
        if (contador) contador.textContent = productos.length ? `(${productos.length})` : '';
        if (!productos.length) { lista.innerHTML = '<div class="text-muted text-center py-4">Este proveedor no tiene productos activos.</div>'; if (paginador) paginador.innerHTML = ''; if (info) info.textContent = ''; return; }
        lista.innerHTML = pagina.map(producto => { const p = producto.capacidad ? ` - ${producto.capacidad}${producto.unidadMedida === 'KG' ? ' kg' : producto.unidadMedida === 'L' ? ' L' : producto.unidadMedida === 'M' ? ' m' : ''}` : ''; return `<div class="producto-selector-item d-flex align-items-center justify-content-between border-bottom py-2 px-2" data-id="${producto.id}" data-nombre="${producto.nombre || ''}" data-unidad="${producto.unidadMedida || ''}" data-capacidad="${producto.capacidad || ''}" data-categoria="${producto.idCategoria || ''}"><div><i class="fas fa-cube text-primary mr-2"></i><span class="font-weight-bold text-dark">${producto.nombre || ''}${p}</span><small class="d-block text-muted ml-4">${producto.nombreCategoria || ''}</small></div><button type="button" class="btn btn-primary btn-sm btn-seleccionar-producto">Seleccionar</button></div>`; }).join('');
        if (paginador) paginador.innerHTML = crearPaginadorSelector('producto', estadoCompras.paginaProductosSelector, totalPaginas);
        if (info) info.textContent = totalPaginas > 1 ? `Mostrando ${inicio + 1}-${Math.min(inicio + porPagina, productos.length)} de ${productos.length} productos` : '';
    }

    function crearPaginadorSelector(tipo, paginaActual, totalPaginas) {
        if (totalPaginas <= 1) return '';
        let html = `<button type="button" class="btn btn-sm btn-light selector-pagina-${tipo}" data-pagina="${paginaActual - 1}" ${paginaActual === 1 ? 'disabled' : ''}>&lsaquo;</button>`;
        for (let pagina = 1; pagina <= totalPaginas; pagina += 1) html += `<button type="button" class="btn btn-sm ${pagina === paginaActual ? 'btn-primary' : 'btn-light'} selector-pagina-${tipo}" data-pagina="${pagina}">${pagina}</button>`;
        return `${html}<button type="button" class="btn btn-sm btn-light selector-pagina-${tipo}" data-pagina="${paginaActual + 1}" ${paginaActual === totalPaginas ? 'disabled' : ''}>&rsaquo;</button>`;
    }

    function cargarProductosProveedor(idProveedor, nombreProveedor) {
        const lifecycleId = estadoCompras.lifecycleId;
        const requestId = ++estadoCompras.requestIds.catalogo;
        if (estadoCompras.selectorController) estadoCompras.selectorController.abort();
        const controller = registrarController(new AbortController());
        estadoCompras.selectorController = controller;
        const titulo = estadoCompras.root && estadoCompras.root.querySelector('#titulo-productos-selector');
        const filtro = estadoCompras.root && estadoCompras.root.querySelector('#filtro-productos-proveedor');
        const lista = estadoCompras.root && estadoCompras.root.querySelector('#lista-productos-selector');
        if (titulo) titulo.textContent = `Productos que ofrece ${nombreProveedor}`;
        if (filtro) filtro.value = '';
        estadoCompras.paginaProductosSelector = 1;
        if (lista) lista.innerHTML = '<div class="text-muted text-center py-4"><i class="fas fa-spinner fa-spin mr-1"></i>Cargando productos...</div>';
        fetch(`/catalogo-proveedores/proveedor/${encodeURIComponent(idProveedor)}/productos`, { signal: controller.signal }).then(response => { if (!response.ok) throw new Error('catalogo'); return response.json(); }).then(productos => { if (activo(lifecycleId) && requestId === estadoCompras.requestIds.catalogo) { estadoCompras.productosSelector = productos || []; estadoCompras.productosFiltradosSelector = estadoCompras.productosSelector; renderizarProductosSelector(estadoCompras.productosFiltradosSelector); } }).catch(error => { if (error.name !== 'AbortError' && activo(lifecycleId) && requestId === estadoCompras.requestIds.catalogo && lista) lista.innerHTML = '<div class="text-danger text-center py-4">No se pudo cargar el catÃ¡logo.</div>'; }).finally(() => estadoCompras.controllers.delete(controller));
    }

    function buscarProveedoresSelector(texto = '') {
        const lifecycleId = estadoCompras.lifecycleId;
        const requestId = ++estadoCompras.requestIds.selector;
        if (estadoCompras.selectorController) estadoCompras.selectorController.abort();
        const controller = registrarController(new AbortController());
        estadoCompras.selectorController = controller;
        mostrarEstadoSelector('<i class="fas fa-spinner fa-spin mr-1"></i>Buscando proveedores...');
        fetch(`/catalogo-proveedores/buscar?texto=${encodeURIComponent(texto)}&limite=30`, { signal: controller.signal }).then(response => { if (!response.ok) throw new Error('selector'); return response.json(); }).then(proveedores => { if (activo(lifecycleId) && requestId === estadoCompras.requestIds.selector) { estadoCompras.paginaProveedoresSelector = 1; renderizarProveedoresSelector(proveedores || []); } }).catch(error => { if (error.name !== 'AbortError' && activo(lifecycleId) && requestId === estadoCompras.requestIds.selector) mostrarEstadoSelector('No se pudo cargar el catÃ¡logo.', 'text-danger'); }).finally(() => estadoCompras.controllers.delete(controller));
    }

    function detalleElemento(id) {
        return estadoCompras.root && estadoCompras.root.querySelector(`#${id}`);
    }

    function escaparDetalle(valor) {
        return String(valor == null ? '—' : valor)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function textoDetalle(valor) {
        return valor == null || valor === '' ? '—' : String(valor);
    }

    function formatearMontoDetalle(valor) {
        const monto = Number(valor);
        if (!Number.isFinite(monto)) return '—';
        return `S/ ${new Intl.NumberFormat('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(monto)}`;
    }

    function formatearNumeroDetalle(valor) {
        const numero = Number(valor);
        if (!Number.isFinite(numero)) return '—';
        return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(numero);
    }

    function formatearFechaHoraDetalle(fechaCompra) {
        const coincidencia = typeof fechaCompra === 'string'
            && fechaCompra.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/);
        if (coincidencia) {
            return { fecha: `${coincidencia[3]}/${coincidencia[2]}/${coincidencia[1]}`, hora: `${coincidencia[4]}:${coincidencia[5]}:${coincidencia[6]}` };
        }
        const fecha = new Date(fechaCompra);
        if (Number.isNaN(fecha.getTime())) return { fecha: '—', hora: '—' };
        return {
            fecha: new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(fecha),
            hora: new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(fecha)
        };
    }

    function asignarTextoDetalle(id, valor) {
        const elemento = detalleElemento(id);
        if (elemento) elemento.textContent = textoDetalle(valor);
    }

    function prepararDetalleCompra(mensaje) {
        asignarTextoDetalle('detalle-compra-documento', '—');
        asignarTextoDetalle('detalle-compra-fecha', '—');
        asignarTextoDetalle('detalle-compra-proveedor', '—');
        asignarTextoDetalle('detalle-compra-hora', '—');
        asignarTextoDetalle('detalle-compra-usuario', '—');
        asignarTextoDetalle('detalle-compra-total', '—');
        const situacion = detalleElemento('detalle-compra-situacion');
        if (situacion) {
            situacion.className = 'text-muted';
            situacion.textContent = '—';
        }
        const tbody = detalleElemento('filas-ver-detalle');
        if (tbody) tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">${mensaje}</td></tr>`;
    }

    function datosMHistoricos(item) {
        const cantidad = Number(item.cantidad);
        const metrosPorRollo = Number(item.metrosPorRollo);
        return item.unidadMedida === 'M'
            && Number.isFinite(cantidad)
            && Number.isFinite(metrosPorRollo)
            && metrosPorRollo > 0
            ? { cantidad, metrosPorRollo }
            : null;
    }

    function formatearCapacidadDetalle(item, datosM) {
        const capacidad = datosM ? datosM.metrosPorRollo : Number(item.capacidad);
        if (!Number.isFinite(capacidad) || capacidad <= 0) return '—';
        if (item.unidadMedida === 'KG') return `${formatearNumeroDetalle(capacidad)} kg`;
        if (item.unidadMedida === 'L') return `${formatearNumeroDetalle(capacidad)} L`;
        if (item.unidadMedida === 'M' && datosM) return `${formatearNumeroDetalle(capacidad)} m/rollo`;
        return '—';
    }

    function renderizarDetalleCompra(data) {
        const fechaHora = formatearFechaHoraDetalle(data.fechaCompra);
        asignarTextoDetalle('detalle-compra-documento', data.numDocumento);
        asignarTextoDetalle('detalle-compra-fecha', fechaHora.fecha);
        asignarTextoDetalle('detalle-compra-proveedor', data.proveedor);
        asignarTextoDetalle('detalle-compra-hora', fechaHora.hora);
        asignarTextoDetalle('detalle-compra-usuario', data.usuario);
        asignarTextoDetalle('detalle-compra-total', formatearMontoDetalle(data.montoTotal));

        const situacion = detalleElemento('detalle-compra-situacion');
        if (situacion) {
            const anulada = data.situacion === 2;
            situacion.className = `badge ${anulada ? 'badge-danger' : 'badge-success'}`;
            situacion.textContent = anulada ? 'ANULADO' : 'REALIZADO';
        }

        const tbody = detalleElemento('filas-ver-detalle');
        if (!tbody) return;
        const detalles = Array.isArray(data.detalles) ? data.detalles : [];
        if (!detalles.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No hay productos registrados para esta compra.</td></tr>';
            return;
        }
        tbody.innerHTML = detalles.map((item, indice) => {
            const datosM = datosMHistoricos(item);
            const producto = textoDetalle(item.producto);
            const capacidad = formatearCapacidadDetalle(item, datosM);
            const cantidad = datosM
                ? `${formatearNumeroDetalle(datosM.cantidad / datosM.metrosPorRollo)} rollos (${formatearNumeroDetalle(datosM.cantidad)} m)`
                : `${formatearNumeroDetalle(item.cantidad)} ${item.unidadMedida === 'M' ? 'm' : 'und.'}`;
            const costo = datosM
                ? `${formatearMontoDetalle(Number(item.precioCostoUnitario) * datosM.metrosPorRollo)} / rollo`
                : `${formatearMontoDetalle(item.precioCostoUnitario)}${item.unidadMedida === 'M' ? ' / m' : ''}`;
            return `<tr><td>${indice + 1}</td><td><i class="fas fa-box text-info mr-2"></i>${escaparDetalle(producto)}</td><td>${escaparDetalle(capacidad)}</td><td>${escaparDetalle(cantidad)}</td><td>${escaparDetalle(costo)}</td><td>${escaparDetalle(formatearMontoDetalle(item.subtotal))}</td></tr>`;
        }).join('');
    }

    function iniciarVistaCompras() {
    const root = window.document.querySelector('[data-modulo="compras"]');
    if (!root) return;
    estadoCompras.root = root;
    const lifecycleId = estadoCompras.lifecycleId;
    const mensajeConflictoInventario = 'Esta compra no puede anularse porque su inventario ya fue utilizado total o parcialmente.';
    function esConflictoInventarioAnulacion(value) {
        const status = value && Number(value.status);
        const payload = value && value.payload !== undefined ? value.payload : value;
        const message = typeof payload === 'string' ? payload : payload && payload.message;
        return status === 409
            || (typeof message === 'string'
                && /inconsistencia de inventario.*lote.*consumido total o parcialmente/i.test(message));
    }
    function mostrarConflictoInventario() {
        if (!activo(lifecycleId)) return;
        window.Swal.fire({
            title: 'No se puede anular',
            text: mensajeConflictoInventario,
            icon: 'warning',
            confirmButtonText: 'Entendido'
        });
    }
    const document = {
        getElementById: id => root.querySelector('#' + id),
        querySelector: selector => root.querySelector(selector),
        querySelectorAll: selector => root.querySelectorAll(selector),
        createElement: (...args) => window.document.createElement(...args),
        get activeElement() { return window.document.activeElement; }
    };
    let arrayDetalles = []; // Almacena temporalmente los artículos antes de enviar al controller

    // Delegación de eventos para clicks
    registrarListener(root, "click", function (e) {
        if (e.target.closest("#btn-crear-compra")) {
            arrayDetalles = [];
            renderizarFilas();
            const form = document.getElementById("form-compra");
            form.reset();
            delete form.dataset.editId;
            estadoCompras.proveedorSelectorId = null;
            estadoCompras.productosSelector = [];
            estadoCompras.productosFiltradosSelector = [];
            estadoCompras.paginaProductosSelector = 1;
            const tituloProductos = document.getElementById('titulo-productos-selector');
            const listaProductos = document.getElementById('lista-productos-selector');
            const contadorProductos = document.getElementById('contador-productos-selector');
            const infoProductos = document.getElementById('info-productos-selector');
            const paginadorProductos = document.getElementById('paginador-productos-selector');
            if (tituloProductos) tituloProductos.textContent = 'Productos del proveedor';
            if (listaProductos) listaProductos.innerHTML = '<div class="text-muted text-center py-4">Selecciona un proveedor para ver sus productos.</div>';
            if (contadorProductos) contadorProductos.textContent = '';
            if (infoProductos) infoProductos.textContent = '';
            if (paginadorProductos) paginadorProductos.innerHTML = '';
            const filtroCatalogo = document.getElementById('filtro-selector-catalogo');
            if (filtroCatalogo) filtroCatalogo.value = '';
            buscarProveedoresSelector('');

            // Autocompletar fecha local actual y bloquear fechas futuras
            const ahora = new Date();
            ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
            const fechaLocal = ahora.toISOString().slice(0, 16);
            const inputFecha = document.getElementById("input-fecha");
            inputFecha.value = fechaLocal;
            inputFecha.max = fechaLocal;

            // Solicitar siguiente correlativo para compras y prellenar el campo de documento
            const correlativoRequestId = ++estadoCompras.requestIds.correlativo;
            if (estadoCompras.correlativoController) estadoCompras.correlativoController.abort();
            const correlativoController = registrarController(new AbortController());
            estadoCompras.correlativoController = correlativoController;
            fetch('/api/correlativos/next?tipo=COMPRA_NOTA&serie=NC001', { signal: correlativoController.signal })
                .then(r => r.json())
                .then(data => {
                    if (activo(lifecycleId) && correlativoRequestId === estadoCompras.requestIds.correlativo && data && data.codigo) {
                        document.getElementById('input-documento').value = data.codigo;
                    }
                })
                .catch(err => {
                    if (err.name !== 'AbortError' && activo(lifecycleId)) console.warn('No se pudo obtener correlativo:', err);
                })
                .finally(() => {
                    estadoCompras.controllers.delete(correlativoController);
                    if (activo(lifecycleId)
                        && correlativoRequestId === estadoCompras.requestIds.correlativo
                        && window.jQuery) {
                        window.jQuery(document.getElementById("modal-compra")).modal("show");
                    }
                });
        }

        if (e.target.closest(".btn-ver-detalle")) {
            const id = e.target.closest(".btn-ver-detalle").dataset.id;
            const detalleRequestId = ++estadoCompras.requestIds.detalle;
            if (estadoCompras.detalleController) estadoCompras.detalleController.abort();
            const detalleController = registrarController(new AbortController());
            estadoCompras.detalleController = detalleController;
            prepararDetalleCompra('<i class="fas fa-spinner fa-spin mr-1"></i>Cargando detalle de la compra...');
            if (window.jQuery) window.jQuery(document.getElementById("modal-detalle-ver")).modal("show");
            fetch(`/compras/detalle/${id}`, { signal: detalleController.signal })
                .then(r => {
                    if (!r.ok) throw new Error('detalle');
                    return r.json();
                })
                .then(data => {
                    if (!activo(lifecycleId) || detalleRequestId !== estadoCompras.requestIds.detalle) return;
                    renderizarDetalleCompra(data || {});
                })
                .catch(err => {
                    if (err.name !== 'AbortError' && activo(lifecycleId) && detalleRequestId === estadoCompras.requestIds.detalle) {
                        prepararDetalleCompra('No fue posible cargar el detalle de la compra.');
                        console.error("ERROR CARGANDO DETALLES:", err);
                    }
                })
                .finally(() => estadoCompras.controllers.delete(detalleController));
        }

        if (e.target.closest('#btn-buscar-proveedor')) {
            const selector = document.getElementById('selector-catalogo-integrado');
            const filtroSelector = document.getElementById('filtro-selector-catalogo');
            if (selector) selector.scrollIntoView({ behavior: 'smooth', block: 'center' });
            if (filtroSelector) filtroSelector.focus();
            if (!estadoCompras.proveedorSelectorId) buscarProveedoresSelector('');
        }

        if (e.target.closest('.proveedor-selector-item')) {
            const tarjeta = e.target.closest('.proveedor-selector-item');
            const id = tarjeta.dataset.id || '';
            const nombre = tarjeta.dataset.nombre || '';
            const ruc = tarjeta.dataset.ruc || '';
            const inputProveedor = document.getElementById('input-proveedor');
            if (arrayDetalles.length > 0 && inputProveedor && inputProveedor.value !== id) {
                window.Swal.fire({
                    title: 'Acción no disponible',
                    text: 'Retire los productos agregados antes de cambiar de proveedor.',
                    icon: 'warning',
                    confirmButtonText: 'Aceptar',
                    allowOutsideClick: false,
                    allowEscapeKey: false
                });
                return;
            }
            estadoCompras.proveedorSelectorId = id;
            document.querySelectorAll('.proveedor-selector-item').forEach(item => {
                item.classList.remove('proveedor-selector-seleccionado');
                const badge = item.querySelector('.badge');
                if (badge) badge.className = 'badge badge-light border text-primary mr-2';
            });
            tarjeta.classList.add('proveedor-selector-seleccionado');
            const badgeSeleccionado = tarjeta.querySelector('.badge');
            if (badgeSeleccionado) badgeSeleccionado.className = 'badge badge-primary mr-2';
            if (inputProveedor) inputProveedor.value = id;
            const displayProveedor = document.getElementById('display-proveedor');
            if (displayProveedor) displayProveedor.value = `${ruc ? ruc + ' - ' : ''}${nombre}`;
            cargarProductosProveedor(id, nombre);
        }

        if (e.target.closest('.selector-pagina-proveedor')) {
            const pagina = parseInt(e.target.closest('.selector-pagina-proveedor').dataset.pagina, 10);
            if (pagina > 0) { estadoCompras.paginaProveedoresSelector = pagina; renderizarProveedoresSelector(estadoCompras.proveedoresSelector); }
        }

        if (e.target.closest('.selector-pagina-producto')) {
            const pagina = parseInt(e.target.closest('.selector-pagina-producto').dataset.pagina, 10);
            if (pagina > 0) { estadoCompras.paginaProductosSelector = pagina; renderizarProductosSelector(estadoCompras.productosFiltradosSelector); }
        }

        if (e.target.closest('.btn-seleccionar-producto')) {
            const fila = e.target.closest('.producto-selector-item');
            const inputProveedor = document.getElementById('input-proveedor');
            const idProveedor = inputProveedor ? inputProveedor.value : '';
            if (!fila || !idProveedor) return;
            const unidad = fila.dataset.unidad || '';
            const capacidad = parseFloat(fila.dataset.capacidad) || 0;
            const sufijo = capacidad ? ` - ${capacidad}${unidad === 'KG' ? ' kg' : unidad === 'L' ? ' L' : unidad === 'M' ? ' m' : ''}` : '';
            const producto = {
                idProducto: fila.dataset.id || '', nombreProducto: `${fila.dataset.nombre || ''}${sufijo}`,
                unidad, capacidad,
                categoria: parseInt(fila.dataset.categoria, 10) || null
            };
            const costoRequestId = ++estadoCompras.requestIds.costo;
            if (estadoCompras.costoController) estadoCompras.costoController.abort();
            const costoController = registrarController(new AbortController());
            estadoCompras.costoController = costoController;
            fetch(`/compras/ultimo-costo?idProducto=${encodeURIComponent(producto.idProducto)}&idProveedor=${encodeURIComponent(idProveedor)}`, { signal: costoController.signal })
                .then(r => r.json())
                .then(data => {
                    if (!activo(lifecycleId) || costoRequestId !== estadoCompras.requestIds.costo) return;
                    const costoPorMetro = parseFloat(data.precioCosto);
                    const precioVisual = Number.isFinite(costoPorMetro) && costoPorMetro > 0
                        ? (producto.unidad === 'M' && producto.capacidad > 0 ? costoPorMetro * producto.capacidad : costoPorMetro)
                        : null;
                    agregarProductoAlDetalle(producto, precioVisual);
                })
                .catch(err => { if (err.name !== 'AbortError' && activo(lifecycleId)) console.error('ERROR OBTENIENDO ULTIMO COSTO:', err); })
                .finally(() => estadoCompras.controllers.delete(costoController));
        }

        if (e.target.closest('.btn-anular-compra')) {
            const btn = e.target.closest('.btn-anular-compra');
            if (btn.disabled) return; // No hacer nada si está deshabilitado

            const id = btn.dataset.id;
            if (!window.Swal) return;

            window.Swal.fire({
                title: '¿Estás seguro?',
                text: 'Esta compra será anulada y su inventario asociado será revertido.',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, anular compra',
                cancelButtonText: 'Cancelar',
                reverseButtons: true
            }).then(result => {
                if (!result.isConfirmed || !activo(lifecycleId)) return null;
                return fetch(`/compras/${id}/anular`, { method: 'POST' })
                    .then(async response => {
                        const raw = await response.text();
                        let payload = null;
                        if (raw) {
                            try {
                                payload = JSON.parse(raw);
                            } catch (parseError) {
                                payload = { message: raw };
                            }
                        }
                        if (!response.ok) {
                            const error = new Error('Error anulando');
                            error.status = response.status;
                            error.payload = payload;
                            throw error;
                        }
                        return payload || { status: 'OK' };
                    });
            })
                .then(resp => {
                    if (!resp || !activo(lifecycleId)) return;
                    if (resp.status === 'OK') {
                        reloadComprasTable();
                        window.Swal.fire({
                            toast: true,
                            position: 'top-end',
                            icon: 'success',
                            title: 'Compra anulada correctamente',
                            showConfirmButton: false,
                            timer: 3000,
                            timerProgressBar: true,
                            animation: true,
                            didOpen: toast => {
                                toast.addEventListener('mouseenter', window.Swal.stopTimer);
                                toast.addEventListener('mouseleave', window.Swal.resumeTimer);
                            }
                        });
                    } else if (esConflictoInventarioAnulacion(resp)) {
                        mostrarConflictoInventario();
                    } else {
                        window.Swal.fire({
                            title: 'No se pudo anular',
                            text: resp.message || 'No se pudo anular la compra.',
                            icon: 'error'
                        });
                    }
                })
                .catch(err => {
                    if (!activo(lifecycleId)) return;
                    if (esConflictoInventarioAnulacion(err)) {
                        mostrarConflictoInventario();
                        return;
                    }
                    console.error('ERROR ANULANDO COMPRA:', err);
                    window.Swal.fire({
                        title: 'Error',
                        text: 'No se pudo anular la compra.',
                        icon: 'error'
                    });
                });
        }
    });

    function agregarProductoAlDetalle(producto, precioVisual) {
        const existente = arrayDetalles.find(item => item.idProducto === producto.idProducto);
        if (existente) {
            existente.cantidadVisual += 1;
        } else {
            arrayDetalles.push({ ...producto, cantidadVisual: 1, precioVisual });
        }
        renderizarFilas();
    }

    function precioMinimoPorCategoria(categoria) {
        return ({ 1: 50, 2: 200, 3: 8 })[categoria] || 0.10;
    }

    function mostrarValidacionDetalle(mensaje) {
        if (!window.Swal || !activo(lifecycleId)) return;
        window.Swal.fire({
            title: 'Revisa el detalle',
            text: mensaje,
            icon: 'warning',
            confirmButtonText: 'Entendido'
        });
    }

    function detalleEsValido(item, mostrarMensaje) {
        if (!Number.isInteger(item.cantidadVisual) || item.cantidadVisual < 1) {
            if (mostrarMensaje) mostrarValidacionDetalle('La cantidad debe ser un número entero positivo.');
            return false;
        }
        if (!Number.isFinite(item.precioVisual) || item.precioVisual <= 0) {
            if (mostrarMensaje) mostrarValidacionDetalle('Ingrese un precio de costo válido.');
            return false;
        }
        if (Math.round(item.precioVisual * 100) % 10 !== 0) {
            if (mostrarMensaje) mostrarValidacionDetalle('El precio debe incrementarse de S/0.10 en S/0.10.');
            return false;
        }
        if (item.precioVisual < precioMinimoPorCategoria(item.categoria)) {
            if (mostrarMensaje) mostrarValidacionDetalle(`El precio mínimo para esta categoría es S/${precioMinimoPorCategoria(item.categoria).toFixed(2)}.`);
            return false;
        }
        return true;
    }

    function subtotalVisual(item) {
        return Number.isFinite(item.precioVisual) ? item.cantidadVisual * item.precioVisual : 0;
    }

    function renderizarFilas() {
        const tbody = document.getElementById('tabla-filas-compras');
        if (!tbody) return;
        tbody.innerHTML = '';
        let totalGeneral = 0;
        arrayDetalles.forEach((item, index) => {
            const subtotal = subtotalVisual(item);
            totalGeneral += subtotal;
            const presentacion = item.unidad === 'M' ? '<small class="d-block text-muted">Cantidad en rollos · precio por rollo</small>' : '';
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${item.nombreProducto}${presentacion}</td>
                <td><input type="number" class="form-control form-control-sm detalle-cantidad" data-index="${index}" min="1" step="1" value="${item.cantidadVisual}"></td>
                <td><div class="input-group input-group-sm"><div class="input-group-prepend"><span class="input-group-text">S/</span></div><input type="number" class="form-control detalle-precio" data-index="${index}" min="0.10" step="0.10" value="${Number.isFinite(item.precioVisual) ? item.precioVisual.toFixed(2) : ''}" required></div></td>
                <td>S/ ${subtotal.toFixed(2)}</td>
                <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-remover" data-index="${index}"><i class="fas fa-trash"></i></button></td>`;
            tbody.appendChild(tr);
        });
        const total = document.getElementById('txt-total-general');
        if (total) total.innerText = totalGeneral.toFixed(2);
        actualizarEstadoProveedor();
    }

    registrarListener(root, 'change', event => {
        const input = event.target.closest('.detalle-cantidad, .detalle-precio');
        if (!input) return;
        const item = arrayDetalles[parseInt(input.dataset.index, 10)];
        if (!item) return;
        const candidato = { ...item };
        if (input.classList.contains('detalle-cantidad')) candidato.cantidadVisual = parseInt(input.value, 10);
        else candidato.precioVisual = input.value === '' ? null : parseFloat(input.value);
        if (input.classList.contains('detalle-precio') && input.value === '') {
            item.precioVisual = null;
            renderizarFilas();
            return;
        }
        if (!detalleEsValido(candidato, true)) { renderizarFilas(); return; }
        Object.assign(item, candidato);
        renderizarFilas();
    });

    registrarListener(root, 'click', event => {
        const boton = event.target.closest('.btn-remover');
        if (!boton) return;
        arrayDetalles.splice(parseInt(boton.dataset.index, 10), 1);
        renderizarFilas();
    });

    function actualizarEstadoProveedor() {
        const tieneProductos = arrayDetalles.length > 0;
        const inputProveedor = document.getElementById('input-proveedor');
        const displayProveedor = document.getElementById('display-proveedor');
        const btnBuscarProveedor = document.getElementById('btn-buscar-proveedor');

        if (tieneProductos) {
            // Deshabilitar el campo proveedor para evitar cambios
            if (displayProveedor) displayProveedor.setAttribute('disabled', 'disabled');
            if (btnBuscarProveedor) btnBuscarProveedor.setAttribute('disabled', 'disabled');
        } else {
            // Habilitar el campo proveedor
            if (displayProveedor) displayProveedor.removeAttribute('disabled');
            if (btnBuscarProveedor) btnBuscarProveedor.removeAttribute('disabled');
        }
    }

    // Guardar el formulario completo vía AJAX enviando RequestBody
    registrarJQueryHandler(document.getElementById("form-compra"), "submit.compras", function (e) {
        e.preventDefault();

        if (arrayDetalles.length === 0) {
            alert("Debe agregar al menos un artículo antes de registrar el documento.");
            return;
        }

        const btnSubmit = $(this).find('button[type="submit"]');
        btnSubmit.prop('disabled', true).text('Procesando Ingreso...');

        const fechaCompraValue = document.getElementById("input-fecha").value;
        const fechaSeleccionada = fechaCompraValue ? new Date(fechaCompraValue) : null;
        const ahora = new Date();
        if (fechaSeleccionada && fechaSeleccionada > ahora) {
            alert("La fecha de emisión no puede ser futura.");
            btnSubmit.prop('disabled', false).text('Registrar Ingreso');
            return;
        }

        const idProveedorSeleccionado = parseInt(document.getElementById("input-proveedor").value, 10);
        if (isNaN(idProveedorSeleccionado) || idProveedorSeleccionado <= 0) {
            alert('Seleccione un proveedor antes de registrar la compra.');
            btnSubmit.prop('disabled', false).text('Registrar Ingreso');
            return;
        }

        if (!arrayDetalles.every(item => detalleEsValido(item, true))) {
            btnSubmit.prop('disabled', false).text('Registrar Ingreso');
            return;
        }

        const detallesPayload = arrayDetalles.map(item => {
            const esRollo = item.unidad === 'M' && item.capacidad > 0;
            // La UI trabaja rollos/precio por rollo; el contrato de compra recibe metros/costo por metro.
            return {
                idProducto: item.idProducto,
                cantidad: esRollo ? item.cantidadVisual * item.capacidad : item.cantidadVisual,
                precioCostoUnitario: esRollo ? item.precioVisual / item.capacidad : item.precioVisual
            };
        });

        const payload = {
            idProveedor: idProveedorSeleccionado,
            numDocumento: document.getElementById("input-documento").value,
            fechaCompra: fechaCompraValue,
            montoTotal: arrayDetalles.reduce((acc, item) => acc + subtotalVisual(item), 0),
            detalles: detallesPayload
        };
        const form = document.getElementById('form-compra');
        const editId = form.dataset.editId;

        $.ajax({
            url: editId ? `/compras/${editId}` : "/compras",
            type: editId ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(payload),
            dataType: "json",
            success: function (resp) {
                if (!activo(lifecycleId)) return;
                if (resp.status === "OK") {
                    $("#modal-compra").modal('hide');
                    // limpiar modo edición
                    delete form.dataset.editId;
                    reloadComprasTable();

                    // =========================================================================
                    // IMPORTANTE: El registro de lotes ya ocurre en el backend al guardar la compra.
                    // No duplicamos envíos desde la interfaz para evitar lotes repetidos.
                    // =========================================================================

                } else {
                    alert(resp.message || "Error al procesar la compra");
                }
            },
            error: function (xhr) {
                if (!activo(lifecycleId)) return;
                alert("Error crítico en la transacción de almacén.");
                console.error(xhr.responseText);
            },
            complete: function () {
                if (activo(lifecycleId)) btnSubmit.prop('disabled', false).text('Registrar Ingreso');
            }
        });
    });

    const filtroSelector = document.getElementById('filtro-selector-catalogo');
    if (filtroSelector) {
        registrarListener(filtroSelector, 'input', () => {
            if (estadoCompras.selectorDebounceTimer) clearTimeout(estadoCompras.selectorDebounceTimer);
            estadoCompras.selectorDebounceTimer = registrarTimer(() => buscarProveedoresSelector(filtroSelector.value.trim()), 300);
        });
    }

    const filtroProductosProveedor = document.getElementById('filtro-productos-proveedor');
    if (filtroProductosProveedor) {
        registrarListener(filtroProductosProveedor, 'input', () => {
            const texto = filtroProductosProveedor.value.trim().toLowerCase();
            estadoCompras.productosFiltradosSelector = estadoCompras.productosSelector.filter(producto =>
                `${producto.nombre || ''} ${producto.nombreCategoria || ''}`.toLowerCase().includes(texto));
            estadoCompras.paginaProductosSelector = 1;
            renderizarProductosSelector(estadoCompras.productosFiltradosSelector);
        });
    }

    buscarProveedoresSelector('');
    }

function ajustarTablaCompras() {
    if (!estadoCompras.initialized || !estadoCompras.dataTable) return;
    estadoCompras.dataTable.columns.adjust();
    if (estadoCompras.dataTable.responsive && typeof estadoCompras.dataTable.responsive.recalc === 'function') {
        estadoCompras.dataTable.responsive.recalc();
    }
}

function ajustarTablaComprasDuranteReflow() {
    if (!estadoCompras.initialized || !estadoCompras.dataTable) return;
    estadoCompras.dataTable.columns.adjust();
}

function programarAjusteTablaDuranteReflow() {
    if (!estadoCompras.initialized || estadoCompras.sidebarReflowFrame !== null) return;
    const lifecycleId = estadoCompras.lifecycleId;
    estadoCompras.sidebarReflowFrame = window.requestAnimationFrame(() => {
        estadoCompras.sidebarReflowFrame = null;
        if (activo(lifecycleId)) ajustarTablaComprasDuranteReflow();
    });
}

function registrarResizeObserverCompras() {
    const contenedorTabla = estadoCompras.root && estadoCompras.root.querySelector('#contenedor-tabla');
    if (!contenedorTabla || typeof window.ResizeObserver !== 'function') return;

    const lifecycleId = estadoCompras.lifecycleId;
    estadoCompras.sidebarResizeObserver = new window.ResizeObserver(entries => {
        if (entries.some(entry => entry.target === contenedorTabla) && activo(lifecycleId)) {
            programarAjusteTablaDuranteReflow();
        }
    });
    estadoCompras.sidebarResizeObserver.observe(contenedorTabla);
}

function registrarReflowSidebarCompras() {
    const lifecycleId = estadoCompras.lifecycleId;
    const contentWrapper = window.document.querySelector('.content-wrapper');
    if (contentWrapper) {
        registrarListener(contentWrapper, 'transitionend', event => {
            if (event.target === contentWrapper
                && (event.propertyName === 'margin-left' || event.propertyName === 'width')
                && activo(lifecycleId)) {
                ajustarTablaCompras();
            }
        });
    }

    if (window.jQuery) {
        registrarJQueryHandler(window.document, 'collapsed-done.lte.pushmenu.compras shown.lte.pushmenu.compras', () => {
            if (!activo(lifecycleId)) return;
            const transitionDuration = contentWrapper
                ? parseFloat(window.getComputedStyle(contentWrapper).transitionDuration || '0')
                : 0;
            if (!contentWrapper || !Number.isFinite(transitionDuration) || transitionDuration === 0) {
                ajustarTablaCompras();
            }
        });
    }
}

function initTablaCompras() {
    if (!$.fn.DataTable) return;
    const tableNode = estadoCompras.root && estadoCompras.root.querySelector('#tabla-compras');
    if (!tableNode) return;

    try {
        const table = $(tableNode);
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().destroy();
        }

        const dataTable = table.DataTable({
            paging: true,
            pageLength: 10,
            lengthMenu: [[10, 20, 30, 40, 50], [10, 20, 30, 40, 50]],
            lengthChange: true,
            searching: true,
            ordering: true,
            info: false,
            autoWidth: false,
            responsive: true,
            pagingType: 'simple',
            dom: 't',
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
        estadoCompras.dataTable = dataTable;

        // Forzar reajuste de columnas al cambiar el tamaño de la ventana o zoom
        $(window).off('resize.comprasTable').on('resize.comprasTable', function () {
            ajustarTablaCompras();
        });

        // Índice de la columna "Fecha Registro" (columna 4, índice base 0)
        var idxFecha = 4;

        // Función de parseo seguro para formato "DD/MM/YYYY HH:mm"
        function limpiarYParsearFecha(textoCelda) {
            if (!textoCelda) return null;
            
            var limpio = textoCelda.replace(/\s+/g, ' ').trim();
            var fechaParte = limpio.split(' ')[0];
            
            var componentes = fechaParte.split('/');
            if (componentes.length !== 3) return null;
            
            return new Date(parseInt(componentes[2], 10), parseInt(componentes[1], 10) - 1, parseInt(componentes[0], 10));
        }

        // Filtro personalizado de fecha para DataTables
        const filtroFecha = function(settings, data, dataIndex) {
                if (settings.nTable !== tableNode) return true;
                var minInput = estadoCompras.root.querySelector('#minDate')?.value || '';
                var maxInput = estadoCompras.root.querySelector('#maxDate')?.value || '';
                
                var textoCelda = data[idxFecha] || "";
                var fechaCelda = limpiarYParsearFecha(textoCelda);

                if (!fechaCelda) return true;

                var fechaMin = minInput ? new Date(minInput + "T00:00:00") : null;
                var fechaMax = maxInput ? new Date(maxInput + "T23:59:59") : null;

                if ((fechaMin === null && fechaMax === null) ||
                    (fechaMin === null && fechaCelda <= fechaMax) ||
                    (fechaMin <= fechaCelda && fechaMax === null) ||
                    (fechaMin <= fechaCelda && fechaCelda <= fechaMax)) {
                    return true;
                }
                return false;
            };
        estadoCompras.dateFilter = filtroFecha;
        if (!$.fn.dataTable.ext.search.includes(filtroFecha)) {
            $.fn.dataTable.ext.search.push(filtroFecha);
        }

        // Conectar controles personalizados
        const $lengthSelect = $(estadoCompras.root.querySelector('#compras-length'));
        const $searchInput = $(estadoCompras.root.querySelector('#compras-search'));
        const $minDate = $(estadoCompras.root.querySelector('#minDate'));
        const $maxDate = $(estadoCompras.root.querySelector('#maxDate'));

        // Cambiar número de registros por página
        if ($lengthSelect.length) {
            $lengthSelect.off('.compras').on('change.compras', function () {
                const pageLength = parseInt($(this).val(), 10);
                dataTable.page.len(pageLength).draw();
            });
        }

        // Búsqueda personalizada
        if ($searchInput.length) {
            $searchInput.off('.compras').on('keyup.compras', function () {
                dataTable.search(this.value).draw();
            });
        }

        // Filtrado por fecha en tiempo real
        $minDate.add($maxDate).off('.compras').on('change.compras', function () {
            dataTable.draw();
        });

        // Botón Limpiar filtros de fecha
        $(estadoCompras.root.querySelector('#btnLimpiarFechas')).off('.compras').on('click.compras', function(e) {
            e.preventDefault();
            $minDate.val('');
            $maxDate.val('');
            dataTable.draw();
        });

        if ($minDate.val() || $maxDate.val()) {
            dataTable.draw(false);
        }

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
            customPager.setAttribute('aria-label', 'Paginación de compras');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStyles();
            renderCustomInfo(dataTable, infoBar);
            renderCustomPagination(dataTable, customPager);
            attachSwipePagination(wrapperEl, dataTable);

            dataTable.on('draw.dt.compras', () => {
                renderCustomInfo(dataTable, infoBar);
                renderCustomPagination(dataTable, customPager);
            });
        }
    } catch (e) {
        console.warn("Error inicializando DataTable de compras:", e);
    }
}

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

    const touchStartHandler = (event) => {
        touchStartX = event.touches[0].clientX;
    };

    const touchEndHandler = (event) => {
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
    estadoCompras.swipeHandlers.push({ wrapperElement, touchStartHandler, touchEndHandler });
}

function limpiarSwipeHandlers() {
    estadoCompras.swipeHandlers.forEach(({ wrapperElement, touchStartHandler, touchEndHandler }) => {
        wrapperElement.removeEventListener('touchstart', touchStartHandler);
        wrapperElement.removeEventListener('touchend', touchEndHandler);
    });
    estadoCompras.swipeHandlers = [];
}

function destruirTablaCompras() {
    limpiarSwipeHandlers();
    const table = estadoCompras.root && estadoCompras.root.querySelector('#tabla-compras');
    if (table && window.jQuery && $.fn.DataTable && $.fn.dataTable.isDataTable(table)) {
        $(table).DataTable().off('.compras');
        $(table).DataTable().destroy();
    }
    if (window.jQuery) {
        $(window).off('resize.comprasTable');
        $('#compras-length, #compras-search, #minDate, #maxDate, #btnLimpiarFechas').off('.compras');
    }
    if (estadoCompras.dateFilter && window.jQuery && $.fn.dataTable) {
        const filtros = $.fn.dataTable.ext.search;
        const index = filtros.indexOf(estadoCompras.dateFilter);
        if (index !== -1) filtros.splice(index, 1);
    }
    estadoCompras.dateFilter = null;
    estadoCompras.dataTable = null;
}

function reloadComprasTable(preservarPagina = false) {
    const lifecycleId = estadoCompras.lifecycleId;
    if (!activo(lifecycleId)) return;
    const table = estadoCompras.root.querySelector('#tabla-compras');
    if (!table) return;
    let paginaActual = 0;
    if (preservarPagina && window.jQuery && $.fn.DataTable && $.fn.dataTable.isDataTable(table)) {
        paginaActual = $(table).DataTable().page.info().page;
    }
    const requestId = ++estadoCompras.requestIds.tabla;
    if (estadoCompras.tablaController) estadoCompras.tablaController.abort();
    const controller = registrarController(new AbortController());
    estadoCompras.tablaController = controller;
    destruirTablaCompras();

    fetch("/compras/tabla", { signal: controller.signal })
        .then(r => {
            if (!r.ok) throw new Error("Error cargando tabla de compras");
            return r.text();
        })
        .then(html => {
            if (!activo(lifecycleId) || requestId !== estadoCompras.requestIds.tabla) return;
            const currentTable = estadoCompras.root.querySelector('#tabla-compras');
            if (!currentTable || !currentTable.isConnected) return;
            currentTable.outerHTML = html;
            initTablaCompras();
            if (preservarPagina && paginaActual > 0) {
                registrarTimer(() => {
                    const refreshedTable = estadoCompras.root && estadoCompras.root.querySelector('#tabla-compras');
                    if (!activo(lifecycleId) || !refreshedTable || !$.fn.dataTable.isDataTable(refreshedTable)) return;
                    const dataTable = $(refreshedTable).DataTable();
                    dataTable.page(Math.min(paginaActual, Math.max(0, dataTable.page.info().pages - 1))).draw(false);
                }, 0);
            }
        })
        .catch(err => {
            if (err.name !== 'AbortError' && activo(lifecycleId)) console.error("ERROR RECARGANDO TABLA:", err);
        })
        .finally(() => estadoCompras.controllers.delete(controller));
}

function initCompras() {
    if (estadoCompras.initialized) return;
    const root = window.document.querySelector('[data-modulo="compras"]');
    if (!root) return;
    estadoCompras.lifecycleId += 1;
    estadoCompras.initialized = true;
    iniciarVistaCompras();
    registrarResizeObserverCompras();
    registrarReflowSidebarCompras();
    try {
        initTablaCompras();
    } catch (error) {
        console.warn("La tabla de compras no pudo inicializarse con DataTables. Los botones seguirán funcionando.", error);
    }
}

function destroyCompras() {
    if (!estadoCompras.initialized) return;
    estadoCompras.listeners.forEach(({ target, type, handler, options }) => target.removeEventListener(type, handler, options));
    estadoCompras.listeners = [];
    estadoCompras.jqueryHandlers.forEach(({ target, events, handler }) => window.jQuery(target).off(events, handler));
    estadoCompras.jqueryHandlers = [];
    if (estadoCompras.sidebarResizeObserver) {
        estadoCompras.sidebarResizeObserver.disconnect();
        estadoCompras.sidebarResizeObserver = null;
    }
    if (estadoCompras.sidebarReflowFrame !== null) {
        window.cancelAnimationFrame(estadoCompras.sidebarReflowFrame);
        estadoCompras.sidebarReflowFrame = null;
    }
    destruirTablaCompras();
    estadoCompras.controllers.forEach(controller => controller.abort());
    estadoCompras.controllers.clear();
    estadoCompras.requestIds.tabla += 1;
    estadoCompras.requestIds.correlativo += 1;
    estadoCompras.requestIds.detalle += 1;
    estadoCompras.requestIds.costo += 1;
    estadoCompras.requestIds.catalogo += 1;
    estadoCompras.timers.forEach(timer => clearTimeout(timer));
    estadoCompras.timers.clear();
    estadoCompras.selectorDebounceTimer = null;
    estadoCompras.selectorController = null;
    estadoCompras.proveedorSelectorId = null;
    estadoCompras.productosSelector = [];
    estadoCompras.productosFiltradosSelector = [];
    estadoCompras.proveedoresSelector = [];
    estadoCompras.paginaProveedoresSelector = 1;
    estadoCompras.paginaProductosSelector = 1;
    if (estadoCompras.root) {
        estadoCompras.root.querySelectorAll('.modal').forEach(modal => {
            if (window.jQuery && window.jQuery.fn.modal) window.jQuery(modal).modal('hide');
            modal.classList.remove('show');
            modal.style.display = 'none';
        });
        if (!window.document.querySelector('.modal.show')) {
            window.document.querySelectorAll('.modal-backdrop').forEach(backdrop => backdrop.remove());
            if (window.document.body) {
                window.document.body.classList.remove('modal-open');
                window.document.body.style.removeProperty('padding-right');
                window.document.body.style.removeProperty('overflow');
            }
        }
    }
    estadoCompras.lifecycleId += 1;
    estadoCompras.root = null;
    estadoCompras.initialized = false;
}

window.AppModules.compras = {
    init: initCompras,
    destroy: destroyCompras,
    reloadTable: reloadComprasTable
};
}());
