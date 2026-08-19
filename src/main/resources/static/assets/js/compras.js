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
            catalogo: 0
        },
        listeners: [],
        jqueryHandlers: [],
        dataTable: null,
        dateFilter: null,
        swipeHandlers: [],
        timers: new Set()
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

    function iniciarVistaCompras() {
    const root = window.document.querySelector('[data-modulo="compras"]');
    if (!root) return;
    estadoCompras.root = root;
    const lifecycleId = estadoCompras.lifecycleId;
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
            fetch(`/compras/detalle/${id}`, { signal: detalleController.signal })
                .then(r => r.json())
                .then(data => {
                    if (!activo(lifecycleId) || detalleRequestId !== estadoCompras.requestIds.detalle) return;
                    const tbody = document.getElementById("filas-ver-detalle");
                    tbody.innerHTML = "";
                    data.forEach(item => {
                        const tr = document.createElement("tr");
                        const cantidadText = item.unidad === 'M' ? `${item.cantidad} rollos` : `${item.cantidad} unidades`;
                        tr.innerHTML = `
                            <td>${item.producto}</td>
                            <td>${cantidadText}</td>
                            <td>S/ ${parseFloat(item.precio).toFixed(2)}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                    if (window.jQuery) window.jQuery(document.getElementById("modal-detalle-ver")).modal("show");
                })
                .catch(err => {
                    if (err.name !== 'AbortError' && activo(lifecycleId)) console.error("ERROR CARGANDO DETALLES:", err);
                })
                .finally(() => estadoCompras.controllers.delete(detalleController));
        }

        if (e.target.closest('#btn-buscar-proveedor')) {
            if (window.jQuery) {
                window.jQuery("#modal-buscar-proveedor").modal("show");
            }
        }

        if (e.target.closest('.btn-seleccionar-proveedor')) {
            const btn = e.target.closest('.btn-seleccionar-proveedor');
            const row = btn.closest('tr');
            if (!row) return;

            const id = row.dataset.id || '';
            const nombre = row.dataset.nombre || '';
            const ruc = row.dataset.ruc || '';
            const inputProveedor = document.getElementById('input-proveedor');
            const displayProveedor = document.getElementById('display-proveedor');
            estadoCompras.requestIds.costo += 1;
            if (estadoCompras.costoController) estadoCompras.costoController.abort();
            if (inputProveedor) inputProveedor.value = id;
            if (displayProveedor) displayProveedor.value = `${ruc ? ruc + ' - ' : ''}${nombre}`;

            if (window.jQuery) {
                window.jQuery("#modal-buscar-proveedor").modal("hide");
            }
        }

            if (e.target.closest('.btn-seleccionar-producto')) {
                const btn = e.target.closest('.btn-seleccionar-producto');
                const row = btn.closest('tr');
                if (!row) return;

                const idProducto = row.dataset.id || '';
                const nombreProd = row.dataset.nombre || '';
                const unidad = row.dataset.unidad || '';
                const capacidad = row.dataset.capacidad || '';
                const categoria = row.dataset.categoria || '';

                const inputProveedor = document.getElementById('input-proveedor');
                const idProveedor = inputProveedor ? inputProveedor.value : '';

                // Rellenar campos del formulario con la info del producto seleccionado
                const selectProd = document.getElementById('select-producto');
                const inputNombreProd = document.getElementById('input-producto-nombre');
                const inputUnidad = document.getElementById('select-unidad');
                const inputCapacidad = document.getElementById('select-capacidad');
                const inputCategoria = document.getElementById('select-categoria');

                if (selectProd) selectProd.value = idProducto;
                if (inputNombreProd) inputNombreProd.value = nombreProd + (capacidad ? (' - ' + capacidad + (unidad === 'KG' ? ' kg' : unidad === 'L' ? ' L' : unidad === 'M' ? ' m' : '')) : '');
                if (inputUnidad) inputUnidad.value = unidad;
                if (inputCapacidad) inputCapacidad.value = capacidad;
                if (inputCategoria) inputCategoria.value = categoria;

                // Pedir el último precio de costo para este producto y proveedor
                if (idProducto && idProveedor) {
                    const costoRequestId = ++estadoCompras.requestIds.costo;
                    if (estadoCompras.costoController) estadoCompras.costoController.abort();
                    const costoController = registrarController(new AbortController());
                    estadoCompras.costoController = costoController;
                    fetch(`/compras/ultimo-costo?idProducto=${encodeURIComponent(idProducto)}&idProveedor=${encodeURIComponent(idProveedor)}`, { signal: costoController.signal })
                        .then(r => r.json())
                        .then(data => {
                            if (!activo(lifecycleId) || costoRequestId !== estadoCompras.requestIds.costo) return;
                            const precio = parseFloat(data.precioCosto) || 0;
                            const inputPrecio = document.getElementById('select-precio');
                            const inputCant = document.getElementById('select-cantidad');
                            if (inputPrecio) inputPrecio.value = precio.toFixed(2);
                            if (inputCant) inputCant.value = '1';
                        })
                        .catch(err => {
                            if (err.name !== 'AbortError' && activo(lifecycleId)) console.error('ERROR OBTENIENDO ULTIMO COSTO:', err);
                        })
                        .finally(() => estadoCompras.controllers.delete(costoController));
                }

                if (window.jQuery) {
                    window.jQuery('#modal-buscar-producto').modal('hide');
                }
            }

        if (e.target.closest('.btn-anular-compra')) {
            const btn = e.target.closest('.btn-anular-compra');
            if (btn.disabled) return; // No hacer nada si está deshabilitado

            const id = btn.dataset.id;
            if (!confirm('¿Confirma anular este documento de compra?')) return;

            fetch(`/compras/${id}/anular`, { method: 'POST' })
                .then(r => {
                    if (!r.ok) throw new Error('Error anulando');
                    return r.json().catch(() => ({ status: 'OK' }));
                })
                .then(resp => {
                    if (!activo(lifecycleId)) return;
                    if (resp.status === 'OK') {
                        reloadComprasTable();
                    } else {
                        alert(resp.message || 'No se pudo anular la compra.');
                    }
                })
                .catch(err => {
                    if (!activo(lifecycleId)) return;
                    console.error('ERROR ANULANDO COMPRA:', err);
                    alert('No se pudo anular la compra.');
                });
        }
    });

    // Añadir artículo al listado interno del modal
    registrarListener(document.getElementById("btn-agregar-lista"), "click", () => {
        const selectProd = document.getElementById("select-producto");
        const inputNombreProd = document.getElementById("input-producto-nombre");
        const inputUnidad = document.getElementById("select-unidad");
        const inputCapacidad = document.getElementById("select-capacidad");
        const inputCategoria = document.getElementById("select-categoria");
        const inputCant = document.getElementById("select-cantidad");
        const inputPrecio = document.getElementById("select-precio");

        const idProducto = selectProd ? selectProd.value : '';
        const nombreProducto = inputNombreProd && inputNombreProd.value ? inputNombreProd.value : (selectProd && selectProd.options ? selectProd.options[selectProd.selectedIndex].text : '');
        const unidad = inputUnidad ? inputUnidad.value : '';
        const capacidad = inputCapacidad ? parseFloat(inputCapacidad.value) : 0;
        const categoria = inputCategoria ? parseInt(inputCategoria.value) : null;
        const cantidadRaw = inputCant.value;
        let cantidad = parseFloat(cantidadRaw);
        let precio = parseFloat(inputPrecio.value);

        if (!idProducto || isNaN(cantidad) || cantidad < 1 || !Number.isInteger(cantidad) || isNaN(precio) || precio <= 0) {
            alert("Seleccione un artículo e ingrese una cantidad entera y un precio válido.");
            return;
        }

        // Verificar que el precio se ingrese en incrementos de S/0.10
        const precioCentimos = Math.round(precio * 100);
        if (precioCentimos % 10 !== 0) {
            alert("El precio debe incrementarse de S/0.10 en S/0.10.");
            return;
        }

        // Validar precio mínimo según categoría
        const preciosMinimos = {
            1: 50,    // Gas Doméstico: mínimo S/50
            2: 200,   // Accesorios: mínimo S/200 (especialmente rollos)
            3: 8      // Agua: mínimo S/8
        };
        const precioMinimo = preciosMinimos[categoria] || 0.10;
        if (precio < precioMinimo) {
            alert(`El precio mínimo para esta categoría es S/${precioMinimo.toFixed(2)}.`);
            return;
        }
        // Conversión automática si es unidad 'M' (metros/rollos)
        if (unidad === 'M' && capacidad > 0) {
            // rollos × metros/rollo = cantidad total en metros
            cantidad = cantidad * capacidad;
            // precio/rollo ÷ metros/rollo = precio unitario por metro
            precio = precio / capacidad;
        }

        const duplicado = arrayDetalles.find(item => item.idProducto === idProducto);
        if (duplicado) {
            duplicado.cantidad += cantidad;
        } else {
            arrayDetalles.push({ idProducto, nombreProducto, cantidad, precioCostoUnitario: precio });
        }

        renderizarFilas();

        // Limpiar campos del formulario después de agregar el producto
        if (selectProd) selectProd.value = '';
        if (inputNombreProd) inputNombreProd.value = '';
        if (inputUnidad) inputUnidad.value = '';
        if (inputCapacidad) inputCapacidad.value = '';
        if (inputCategoria) inputCategoria.value = '';
        if (inputCant) inputCant.value = '1';
        if (inputPrecio) inputPrecio.value = '';
    });

    function renderizarFilas() {
        const tbody = document.getElementById("tabla-filas-compras");
        tbody.innerHTML = "";
        let totalGeneral = 0;

        arrayDetalles.forEach((item, index) => {
            const subtotal = item.cantidad * item.precioCostoUnitario;
            totalGeneral += subtotal;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${item.nombreProducto}</td>
                <td>${item.cantidad}</td>
                <td>S/ ${item.precioCostoUnitario.toFixed(2)}</td>
                <td>S/ ${subtotal.toFixed(2)}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-danger btn-sm btn-remover" data-index="${index}">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        document.getElementById("txt-total-general").innerText = totalGeneral.toFixed(2);

        document.querySelectorAll(".btn-remover").forEach(btn => {
            registrarListener(btn, "click", (e) => {
                const idx = e.currentTarget.dataset.index;
                arrayDetalles.splice(idx, 1);
                renderizarFilas();
            });
        });

        // Bloquear/desbloquear el selector de proveedor según si hay productos en el detalle
        actualizarEstadoProveedor();
    }

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

        const payload = {
            idProveedor: idProveedorSeleccionado,
            numDocumento: document.getElementById("input-documento").value,
            fechaCompra: fechaCompraValue,
            montoTotal: arrayDetalles.reduce((acc, item) => acc + (item.cantidad * item.precioCostoUnitario), 0),
            detalles: arrayDetalles
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

    const modalBuscarProducto = document.getElementById("modal-buscar-producto");
    const cuerpoTablaProductos = document.getElementById("cuerpo-busqueda-productos");

    if (window.jQuery && modalBuscarProducto) {
        registrarJQueryHandler(modalBuscarProducto, "show.bs.modal.compras", function (e) {
            const selectorProveedor = document.getElementById("input-proveedor");
            const idProveedor = selectorProveedor ? selectorProveedor.value : "";

            if (!idProveedor) {
                alert("Atención: Por favor, seleccione primero un proveedor para filtrar su catálogo autorizado.");
                e.preventDefault();
                return false;
            }

            if (cuerpoTablaProductos) {
                cuerpoTablaProductos.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-muted">
                            <i class="fas fa-spinner fa-spin"></i> Cargando catálogo autorizado del proveedor...
                        </td>
                    </tr>`;
            }

            const catalogoRequestId = ++estadoCompras.requestIds.catalogo;
            if (estadoCompras.catalogoController) estadoCompras.catalogoController.abort();
            const catalogoController = registrarController(new AbortController());
            estadoCompras.catalogoController = catalogoController;
            fetch(`/catalogo-proveedores/proveedor/${idProveedor}/productos`, { signal: catalogoController.signal })
                .then(response => {
                    if (!response.ok) throw new Error("Error en el servidor");
                    return response.json();
                })
                .then(productosAutorizados => {
                    if (!activo(lifecycleId) || catalogoRequestId !== estadoCompras.requestIds.catalogo) return;
                    if (!cuerpoTablaProductos) return;
                    cuerpoTablaProductos.innerHTML = "";

                    if (productosAutorizados.length === 0) {
                        cuerpoTablaProductos.innerHTML = `
                            <tr>
                                <td colspan="4" class="text-center text-danger">
                                    El proveedor seleccionado no tiene productos asignados en su catálogo.
                                </td>
                            </tr>`;
                        return;
                    }

                    productosAutorizados.forEach(p => {
                        const fila = document.createElement("tr");
                        fila.className = "fila-producto-busqueda";
                        fila.dataset.id = p.id;
                        fila.dataset.nombre = p.nombre;
                        fila.dataset.precio = p.precioVenta;
                        fila.dataset.ganancia = p.gananciaProducto || 0;
                        fila.dataset.categoria = p.idCategoria || "";
                        fila.dataset.capacidad = p.capacidad || "";
                        fila.dataset.unidad = p.unidadMedida || "";

                        let nombreFormateado = p.nombre || "";
                        if (p.capacidad && p.unidadMedida) {
                            const sufijoUnidad = p.unidadMedida === "KG" ? " kg" : p.unidadMedida === "L" ? " L" : p.unidadMedida === "M" ? " m" : "";
                            nombreFormateado += ` - ${p.capacidad}${sufijoUnidad}`;
                        }

                        fila.innerHTML = `
                            <td>${nombreFormateado}</td>
                            <td>${p.nombreCategoria || "-"}</td>
                            <td class="text-right">S/ ${parseFloat(p.precioVenta).toFixed(2)}</td>
                            <td>
                                <button type="button" class="btn btn-sm btn-success btn-seleccionar-producto">Seleccionar</button>
                            </td>
                        `;
                        cuerpoTablaProductos.appendChild(fila);
                    });
                })
                .catch(err => {
                    if (err.name === 'AbortError' || !activo(lifecycleId) || catalogoRequestId !== estadoCompras.requestIds.catalogo) return;
                    console.error(err);
                    if (cuerpoTablaProductos) {
                        cuerpoTablaProductos.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Error al cargar el catálogo.</td></tr>`;
                    }
                })
                .finally(() => estadoCompras.controllers.delete(catalogoController));
        });
    }

    const filtroBuscarProveedor = document.getElementById('filtro-buscar-proveedor');
    if (filtroBuscarProveedor) {
        registrarListener(filtroBuscarProveedor, 'input', () => {
            const textoFiltro = filtroBuscarProveedor.value.trim().toLowerCase();
            document.querySelectorAll('#tabla-busqueda-proveedores tbody tr').forEach(row => {
                const textoFila = `${row.dataset.ruc || ''} ${row.dataset.nombre || ''}`.toLowerCase();
                row.style.display = textoFila.includes(textoFiltro) ? '' : 'none';
            });
        });
    }

    const inputFiltro = document.getElementById('buscar-producto-filtro');
    const selectCategoria = document.getElementById('select-buscar-categoria');

    function filtrarProductosModal() {
        const textoBusqueda = (inputFiltro ? inputFiltro.value : '').toLowerCase().trim();
        const categoriaId = selectCategoria ? selectCategoria.value : '';
        const filasProductos = document.querySelectorAll('#tabla-busqueda-productos tbody tr');

        filasProductos.forEach(fila => {
            const nombreProducto = fila.cells[0].textContent.toLowerCase();
            const categoriaIdAttr = fila.dataset.categoria || '';

            const coincideTexto = nombreProducto.includes(textoBusqueda);
            const coincideCategoria = categoriaId === '' || categoriaIdAttr === categoriaId;

            if (coincideTexto && coincideCategoria) {
                fila.style.display = 'table-row';
            } else {
                fila.style.display = 'none';
            }
        });
    }

    if (inputFiltro) {
        registrarListener(inputFiltro, 'input', filtrarProductosModal);
    }

    if (selectCategoria) {
        registrarListener(selectCategoria, 'change', filtrarProductosModal);
    }

    // Abrir modal de búsqueda de productos
    const btnBuscarProducto = document.getElementById('btn-buscar-producto');
    if (btnBuscarProducto) {
        registrarListener(btnBuscarProducto, 'click', () => {
            if (window.jQuery) {
                window.jQuery('#modal-buscar-producto').modal('show');
            }
        });
    }

    const inputProductoNombre = document.getElementById('input-producto-nombre');
    if (inputProductoNombre) {
        registrarListener(inputProductoNombre, 'click', () => {
            if (window.jQuery) {
                window.jQuery('#modal-buscar-producto').modal('show');
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
            dataTable.columns.adjust().draw();
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
