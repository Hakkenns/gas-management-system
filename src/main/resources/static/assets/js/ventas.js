console.log("ventas.js cargado correctamente en el cliente");

$(function() {
    function actualizarEstadosPedidosEnTabla() {
        fetch('/ventas/estado-actual')
            .then(response => {
                if (!response.ok) {
                    throw new Error('No autorizado');
                }
                return response.json();
            })
            .then(estados => {
                const mapaEstados = new Map(estados.map(estado => [estado.idPedido, estado]));
                $('.tabla-ventas-dinamica tbody tr').each(function() {
                    const idPedido = $(this).find('td').eq(0).text().trim();
                    const estado = mapaEstados.get(Number(idPedido));
                    if (!estado) return;

                    const celdaEstadoPedido = $(this).find('td.estado-pedido');
                    const celdaEstadoPago = $(this).find('td.estado-pago');
                    const celdaMetodoPago = $(this).find('td.metodo-pago');
                    const botonEditar = $(this).find('.btn-editar-venta');

                    if (celdaEstadoPedido.length) {
                        const badge = celdaEstadoPedido.find('.badge');
                        let texto = estado.estadoPedido || 'PENDIENTE';
                        let clase = 'badge-secondary';

                        if (texto === 'ENTREGADO') clase = 'badge-success';
                        else if (texto === 'PENDIENTE') clase = 'badge-warning';
                        else if (texto === 'ANULADO') clase = 'badge-danger';
                        else clase = 'badge-info';

                        if (badge.length) {
                            badge.attr('class', `badge ${clase}`);
                            badge.text(texto);
                        } else {
                            celdaEstadoPedido.html(`<span class="badge ${clase}">${texto}</span>`);
                        }
                    }

                    if (celdaEstadoPago.length) {
                        const badgePago = celdaEstadoPago.find('.badge');
                        let textoPago = estado.estadoPago || 'PENDIENTE';
                        let clasePago = 'badge-secondary';

                        if (textoPago === 'PAGADO') clasePago = 'badge-success';
                        else if (textoPago === 'PENDIENTE') clasePago = 'badge-warning';
                        else if (textoPago === 'CREDITO') clasePago = 'badge-primary';
                        else if (textoPago === 'VENCIDO') clasePago = 'badge-danger';

                        if (badgePago.length) {
                            badgePago.attr('class', `badge ${clasePago}`);
                            badgePago.text(textoPago);
                        } else {
                            celdaEstadoPago.html(`<span class="badge ${clasePago}">${textoPago}</span>`);
                        }
                    }

                    if (celdaMetodoPago.length) {
                        celdaMetodoPago.text(estado.metodoPago || '');
                    }

                    if (botonEditar.length) {
                        const shouldShow = estado.estadoPedido === 'PENDIENTE';
                        botonEditar.toggle(shouldShow);
                    }
                });
            })
            .catch(() => {});
    }

    setInterval(actualizarEstadosPedidosEnTabla, 3000);
    let detallesVenta = [];
    let pagosVenta = [];

    function parseMoney(value) {
        const number = Number(String(value).replace(/[^0-9.-]+/g, '').replace(',', '.'));
        return isNaN(number) ? 0 : number;
    }

    function formatMoney(value) {
        return parseMoney(value).toFixed(2);
    }

    // Toggle fields based on sale type (Local / Domicilio)
    function actualizarCamposPorTipoVenta() {
        const tipo = $('#select-tipo-venta').val();
        if (tipo === 'LOCAL') {
            $('.group-campos-domicilio').hide();
            $('#input-direccion-cliente').val('');
            $('#input-referencia-cliente').val('');
            $('#select-motorizado').val('');

            $('#group-condicion-pago').show();
            $('.group-campos-pago').show();
            $('#group-estado-pedido-parent').show();
            $('#select-estado-pedido').val('ENTREGADO');
            $('#input-estado-pedido-local').hide();
        } else {
            $('.group-campos-domicilio').show();
            $('#group-condicion-pago').show();
            $('.group-campos-pago').hide();
            $('#group-estado-pedido-parent').hide();
            $('#select-estado-pedido').val('PENDIENTE');
            $('#input-estado-pedido-local').hide();
        }
    }

    $('#select-tipo-venta').on('change', actualizarCamposPorTipoVenta);

    function limpiarFormularioVenta() {
        const form = document.getElementById('form-venta');
        if (form) {
            form.reset();
        }
        $('#input-id-pedido').val('');
        $('#input-id-cliente').val('');
        $('#cliente-feedback').text('Ingrese DNI y busque, o complete datos manualmente.');
        $('#tabla-filas-venta').empty();
        $('#tabla-pagos-venta').html('<tr><td colspan="4" class="text-center text-muted">No se han registrado pagos aún.</td></tr>');
        $('#txt-subtotal').text('0.00');
        $('#txt-total-general').text('0.00');
        $('#row-num-operacion').hide();
        $('#input-num-operacion').prop('required', false);
    }

    function abrirModalVenta(tipo) {
        limpiarFormularioVenta();
        $('#select-tipo-venta').val(tipo || 'LOCAL');
        actualizarCamposPorTipoVenta();
        $('#modal-title-venta').text(tipo === 'DOMICILIO' ? 'Registrar nueva venta a domicilio' : 'Registrar nueva venta local');
        $('#modal-venta').modal('show');
    }

    $('.btn-crear-venta').on('click', function() {
        const tipo = $(this).data('tipo');
        abrirModalVenta(tipo);
    });

    // Inicializar campos al cargar
    actualizarCamposPorTipoVenta();

    // Mostrar/ocultar fecha límite según condición de pago
    $('#select-condicion-pago').on('change', function() {
        if ($(this).val() === 'CREDITO') {
            $('#group-fecha-limite').show();
        } else {
            $('#group-fecha-limite').hide();
        }
    });

    // Mostrar/ocultar número de operación según método de pago
    $('#select-metodo').on('change', function() {
        const metodo = $(this).val();
        if (metodo === 'YAPE' || metodo === 'PLIN') {
            $('#row-num-operacion').show();
            $('#input-num-operacion').prop('required', true);
        } else {
            $('#row-num-operacion').hide();
            $('#input-num-operacion').prop('required', false);
        }
    });

    // Buscar cliente por DNI
    $('#btn-buscar-cliente').on('click', function() {
        const dni = $('#input-dni-cliente').val().trim();
        if (!dni || dni.length !== 8) {
            alert('Ingrese un DNI válido de 8 dígitos');
            return;
        }

        $.getJSON(`/clientes/api/consultar-dni/${dni}`)
            .done(function(res) {
                if (res && res.datos) {
                    const info = res.datos;
                    const nombre = `${info.nombres} ${info.ape_paterno} ${info.ape_materno || ''}`;
                    $('#input-nombre-cliente').val(nombre.toUpperCase());
                    $('#input-id-cliente').val(''); // No se asigna ID automáticamente
                    $('#cliente-feedback').text('Cliente encontrado. Complete los datos manualmente si es necesario.');
                } else {
                    alert('Cliente no encontrado. Complete los datos manualmente.');
                }
            })
            .fail(function() {
                alert('Error al buscar cliente. Complete los datos manualmente.');
            });
    });

    // Buscar producto
    $('#btn-buscar-producto').on('click', function() {
        $('#modal-buscar-producto').modal('show');
    });

    // Filtrar productos en el modal
    $('#buscar-producto-filtro, #select-buscar-categoria').on('keyup change', function() {
        const filtro = $('#buscar-producto-filtro').val().toLowerCase();
        const categoria = $('#select-buscar-categoria').val();

        $('.fila-producto-busqueda').each(function() {
            const nombre = $(this).data('nombre') || '';
            const cat = $(this).data('categoria') || '';
            const coincideNombre = nombre.toLowerCase().includes(filtro);
            const coincideCat = !categoria || cat === categoria;
            $(this).toggle(coincideNombre && coincideCat);
        });
    });

    // Seleccionar producto: traer datos verdaderos desde el servidor y fijar cantidad = 1
    $(document).on('click', '.btn-seleccionar-producto', function() {
        const fila = $(this).closest('tr');
        const id = fila.data('id');
        const nombre = fila.data('nombre');

        $('#select-producto').val(id);
        $('#input-producto-nombre').val(nombre);
        // cantidad por defecto
        $('#select-cantidad').val(1);

        // Obtener datos oficiales del producto para asegurar precio unitario verdadero
        fetch(`/productos/${id}`)
            .then(response => {
                if (!response.ok) throw new Error('No se pudo obtener producto');
                return response.json();
            })
            .then(prod => {
                const precioVenta = prod.precioVenta ?? prod.precioVenta; // fallback automático
                if (precioVenta !== undefined && precioVenta !== null) {
                    $('#select-precio').val(precioVenta);
                } else {
                    // fallback a data attribute si existe
                    const precioAttr = fila.data('precio') || '';
                    $('#select-precio').val(precioAttr);
                }
            })
            .catch(() => {
                const precioAttr = fila.data('precio') || '';
                $('#select-precio').val(precioAttr);
            })
            .finally(() => {
                $('#modal-buscar-producto').modal('hide');
                $('#select-cantidad').focus();
            });
    });

    // Agregar detalle a la venta
    $('#btn-agregar-detalle').on('click', function() {
        const productoId = $('#select-producto').val();
        const productoNombre = $('#input-producto-nombre').val();
        const cantidad = parseInt($('#select-cantidad').val()) || 0;
        const precio = parseMoney($('#select-precio').val());
        const prestados = parseInt($('#input-cantidad-prestada').val()) || 0;

        if (!productoId || !productoNombre || cantidad <= 0 || precio <= 0) {
            alert('Complete todos los campos del producto correctamente');
            return;
        }

        const subtotal = (cantidad * precio) + (prestados * precio);
        const existingRow = $(`#tabla-filas-venta tr`).filter(function() {
            return $(this).data('productoId') === productoId;
        }).first();

        if (existingRow.length) {
            const existingCantidad = parseInt(existingRow.data('cantidad')) || 0;
            const existingPrestados = parseInt(existingRow.data('prestados')) || 0;
            const nuevaCantidad = existingCantidad + cantidad;
            const nuevosPrestados = existingPrestados + prestados;
            const nuevoSubtotal = (nuevaCantidad * precio) + (nuevosPrestados * precio);

            existingRow.data('cantidad', nuevaCantidad);
            existingRow.data('prestados', nuevosPrestados);
            existingRow.data('subtotal', nuevoSubtotal);
            existingRow.data('precio', precio);

            existingRow.find('td').eq(1).text(nuevaCantidad);
            existingRow.find('td').eq(2).text(`S/ ${formatMoney(precio)}`);
            existingRow.find('td').eq(3).text(nuevosPrestados > 0 ? nuevosPrestados : '');
            existingRow.find('td').eq(4).text(`S/ ${formatMoney(nuevoSubtotal)}`);
        } else {
            const tr = $('<tr>');
            tr.html(`
                <td>${productoNombre}</td>
                <td class="text-center">${cantidad}</td>
                <td class="text-right">S/ ${formatMoney(precio)}</td>
                <td class="text-center">${prestados > 0 ? prestados : ''}</td>
                <td class="text-right">S/ ${formatMoney(subtotal)}</td>
                <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-quitar-detalle"><i class="fas fa-trash"></i></button></td>
            `);
            tr.attr('data-producto-id', productoId);
            tr.data('productoId', productoId);
            tr.data('cantidad', cantidad);
            tr.data('precio', precio);
            tr.data('prestados', prestados);
            tr.data('subtotal', subtotal);

            $('#tabla-filas-venta').append(tr);
        }

        calcularTotales();
    });

    // Quitar detalle
    $(document).on('click', '.btn-quitar-detalle', function() {
        $(this).closest('tr').remove();
        calcularTotales();
    });

    // Calcular totales
    function calcularTotales() {
        let subtotal = 0;
        // #tabla-filas-venta es el <tbody>, iterar sus filas directamente
        $('#tabla-filas-venta tr').each(function() {
            subtotal += parseFloat($(this).data('subtotal')) || 0;
        });

        $('#txt-subtotal').text(formatMoney(subtotal));
        $('#txt-total-general').text(formatMoney(subtotal));
    }

    // Agregar pago
    $('#btn-agregar-pago').on('click', function() {
        const metodoId = $('#select-metodo').val();
        const metodoNombre = $('#select-metodo option:selected').text();
        const monto = parseMoney($('#input-monto-pago').val());
        const numOperacion = $('#input-num-operacion').val();

        if (!metodoId || monto <= 0) {
            alert('Seleccione método de pago y ingrese un monto válido');
            return;
        }

        if ((metodoNombre === 'YAPE' || metodoNombre === 'PLIN') && !numOperacion) {
            alert('Ingrese el número de operación para Yape/Plin');
            return;
        }

        const tr = $('<tr>');
        tr.html(`
            <td>${metodoNombre}</td>
            <td class="text-right">S/ ${formatMoney(monto)}</td>
            <td>${numOperacion || '-'}</td>
            <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-quitar-pago"><i class="fas fa-trash"></i></button></td>
        `);
        tr.data('idMetodoPago', metodoId);
        tr.data('metodo', metodoNombre);
        tr.data('monto', monto);
        tr.data('numOperacion', numOperacion);

        // Eliminar fila de placeholder si existe
        $('#tabla-pagos-venta').find('tr.placeholder-pago').remove();
        $('#tabla-pagos-venta').append(tr);

        // Limpiar campos
        $('#input-monto-pago').val('');
        $('#input-num-operacion').val('');
    });

    // Quitar pago
    $(document).on('click', '.btn-quitar-pago', function() {
        $(this).closest('tr').remove();
        if ($('#tabla-pagos-venta tr').length === 0) {
            $('#tabla-pagos-venta').html('<tr class="placeholder-pago"><td colspan="4" class="text-center text-muted">No se han registrado pagos aún.</td></tr>');
        }
    });

    // Guardar venta
    $('#form-venta').on('submit', function(e) {
        e.preventDefault();

        // Validar que haya al menos un producto
        if ($('#tabla-filas-venta tr').length === 0) {
            alert('Agregue al menos un producto a la venta');
            return;
        }

        const tipoVenta = $('#select-tipo-venta').val();

        // Validar pagos sólo para venta local
        if (tipoVenta === 'LOCAL' && $('#tabla-pagos-venta tr').filter(function() { return $(this).data('idMetodoPago') !== undefined; }).length === 0) {
            alert('Registre al menos un pago');
            return;
        }

        // Construir JSON de la venta
        const venta = {
            tipoVenta: $('#select-tipo-venta').val(),
            idCliente: $('#input-id-cliente').val() ? Number($('#input-id-cliente').val()) : null,
            nombreCliente: $('#input-nombre-cliente').val(),
            telefonoCliente: $('#input-telefono-cliente').val(),
            direccionCliente: $('#input-direccion-cliente').val(),
            referenciaCliente: $('#input-referencia-cliente').val(),
            condicionPago: $('#select-condicion-pago').val(),
            fechaLimitePago: $('#input-fecha-limite').val() || null,
            idEmpleado: $('#select-motorizado').val() ? Number($('#select-motorizado').val()) : null,
            idMetodoPago: $('#select-metodo').val() ? Number($('#select-metodo').val()) : null,
            numOperacion: $('#input-num-operacion').val(),
            estadoPedido: $('#select-estado-pedido').val(),
            observaciones: $('#input-observaciones').val(),
            detalles: [],
            pagos: []
        };

        // Recopilar detalles
        $('#tabla-filas-venta tr').each(function() {
            venta.detalles.push({
                idProducto: $(this).data('productoId') ? Number($(this).data('productoId')) : null,
                cantidad: $(this).data('cantidad'),
                precioUnitario: $(this).data('precio'),
                cantidadPrestada: $(this).data('prestados')
            });
        });

        // Recopilar pagos
        $('#tabla-pagos-venta tr').filter(function() { return $(this).data('idMetodoPago') !== undefined; }).each(function() {
            venta.pagos.push({
                idMetodoPago: $(this).data('idMetodoPago') ? Number($(this).data('idMetodoPago')) : null,
                monto: $(this).data('monto'),
                numOperacion: $(this).data('numOperacion')
            });
        });

        // Validar IDs antes de enviar
        if (venta.detalles.some(d => d.idProducto == null)) {
            alert('Error: uno de los productos no tiene ID. Seleccione el producto nuevamente.');
            return;
        }
        if (tipoVenta === 'LOCAL' && venta.pagos.some(p => p.idMetodoPago == null)) {
            alert('Error: uno de los pagos no tiene método de pago. Verifique los pagos registrados.');
            return;
        }

        // Enviar al servidor
        $.ajax({
            url: '/ventas',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(venta),
            success: function(response) {
                if (response.status === 'OK') {
                    alert('Venta registrada correctamente');
                    location.reload();
                } else {
                    alert(response.message || 'Error al guardar la venta');
                }
            },
            error: function(xhr) {
                const errorMsg = xhr.responseJSON?.message || xhr.responseText || 'Error al procesar la venta';
                alert('Error al procesar la venta: ' + errorMsg);
            }
        });
    });

    // Ver detalle de venta
    $(document).on('click', '.btn-ver-detalle-venta', function() {
        const ventaId = $(this).data('id');
        $('#detalle-venta-body').empty();
        $('#detalle-venta-sin-items').hide();

        $.getJSON(`/ventas/detalle/${ventaId}`)
            .done(function(data) {
                const detalles = Array.isArray(data) ? data : data.detalles || [];
                if (!Array.isArray(detalles) || detalles.length === 0) {
                    $('#detalle-venta-sin-items').show();
                    $('#modal-detalle-venta').modal('show');
                    return;
                }

                detalles.forEach(det => {
                    const subtotal = parseMoney(det.subtotal || (det.precioUnitario * det.cantidad));
                    const tr = $('<tr>');
                    
                    let productoNombre = det.producto || '';
                    if (det.cantidadPrestada && det.cantidadPrestada > 0) {
                        productoNombre += ` <span class="badge badge-warning">(${det.cantidadPrestada} prestado/s)</span>`;
                    }

                    tr.append(`<td>${productoNombre}</td>`);
                    tr.append(`<td class="text-center">${det.cantidad || 0}</td>`);
                    tr.append(`<td class="text-right">S/ ${formatMoney(det.precioUnitario)}</td>`);
                    tr.append(`<td class="text-right">S/ ${formatMoney(subtotal)}</td>`);
                    $('#detalle-venta-body').append(tr);
                });

                if (Array.isArray(data.pagos) && data.pagos.length > 0) {
                    const pagosHtml = data.pagos.map(pago => `
                        <div><strong>${pago.metodo}:</strong> S/ ${formatMoney(pago.monto)}${pago.numOperacion ? ' (' + pago.numOperacion + ')' : ''}</div>
                    `).join('');
                    $('#detalle-venta-body').append(`<tr><td colspan="4"><strong>Pagos:</strong><br>${pagosHtml}</td></tr>`);
                }

                $('#modal-detalle-venta').modal('show');
            })
            .fail(function() {
                alert('No se pudo cargar el detalle de la venta. Intenta de nuevo.');
            });
    });
});

// Inicialización de DataTables para Ventas
function initTablaVentas() {
    if ($.fn.DataTable) {
        // Inicializar tabla de ventas locales si existe
        const tableLocal = $('#tabla-ventas-local');
        if (tableLocal.length && $.fn.dataTable.isDataTable(tableLocal)) {
            tableLocal.DataTable().destroy();
        }
        if (tableLocal.length) {
            const dataTableLocal = tableLocal.DataTable({
                dom: 'rt',
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
            $(window).on('resize', function () {
                dataTableLocal.columns.adjust().draw();
            });

            // Conectar controles personalizados
            const $lengthSelectLocal = $('#ventas-local-length');
            const $searchInputLocal = $('#ventas-local-search');

            if ($lengthSelectLocal.length) {
                $lengthSelectLocal.on('change', function () {
                    const pageLength = parseInt($(this).val(), 10);
                    dataTableLocal.page.len(pageLength).draw();
                });
            }

            if ($searchInputLocal.length) {
                $searchInputLocal.on('keyup', function () {
                    dataTableLocal.search(this.value).draw();
                });
            }

            // Ocultar controles nativos duplicados de DataTables
            const $wrapperLocal = tableLocal.closest('.dataTables_wrapper');
            if ($wrapperLocal.length) {
                const wrapperElLocal = $wrapperLocal[0];
                const paginateContainerLocal = wrapperElLocal.querySelector('.dataTables_paginate');
                if (paginateContainerLocal) {
                    paginateContainerLocal.style.display = 'none';
                }

                const defaultInfoLocal = wrapperElLocal.querySelector('.dataTables_info');
                if (defaultInfoLocal) {
                    defaultInfoLocal.style.display = 'none';
                }

                const pagerRowLocal = document.createElement('div');
                pagerRowLocal.className = 'compras-pager-row';
                wrapperElLocal.appendChild(pagerRowLocal);

                const infoBarLocal = document.createElement('div');
                infoBarLocal.className = 'compras-info-bar';
                pagerRowLocal.appendChild(infoBarLocal);

                const customPagerLocal = document.createElement('div');
                customPagerLocal.className = 'compras-custom-pagination';
                customPagerLocal.setAttribute('aria-label', 'Paginación de ventas locales');
                pagerRowLocal.appendChild(customPagerLocal);

                injectCustomPaginationStyles();
                renderCustomInfo(dataTableLocal, infoBarLocal);
                renderCustomPagination(dataTableLocal, customPagerLocal);
                attachSwipePagination(wrapperElLocal, dataTableLocal);

                dataTableLocal.on('draw.dt', () => {
                    renderCustomInfo(dataTableLocal, infoBarLocal);
                    renderCustomPagination(dataTableLocal, customPagerLocal);
                });
            }
        }

        // Inicializar tabla de ventas a domicilio si existe
        const tableDomicilio = $('#tabla-ventas-domicilio');
        if (tableDomicilio.length && $.fn.dataTable.isDataTable(tableDomicilio)) {
            tableDomicilio.DataTable().destroy();
        }
        if (tableDomicilio.length) {
            const dataTableDomicilio = tableDomicilio.DataTable({
                dom: 'rt',
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
            $(window).on('resize', function () {
                dataTableDomicilio.columns.adjust().draw();
            });

            // Conectar controles personalizados
            const $lengthSelectDomicilio = $('#ventas-domicilio-length');
            const $searchInputDomicilio = $('#ventas-domicilio-search');

            if ($lengthSelectDomicilio.length) {
                $lengthSelectDomicilio.on('change', function () {
                    const pageLength = parseInt($(this).val(), 10);
                    dataTableDomicilio.page.len(pageLength).draw();
                });
            }

            if ($searchInputDomicilio.length) {
                $searchInputDomicilio.on('keyup', function () {
                    dataTableDomicilio.search(this.value).draw();
                });
            }

            // Ocultar controles nativos duplicados de DataTables
            const $wrapperDomicilio = tableDomicilio.closest('.dataTables_wrapper');
            if ($wrapperDomicilio.length) {
                const wrapperElDomicilio = $wrapperDomicilio[0];
                const paginateContainerDomicilio = wrapperElDomicilio.querySelector('.dataTables_paginate');
                if (paginateContainerDomicilio) {
                    paginateContainerDomicilio.style.display = 'none';
                }

                const defaultInfoDomicilio = wrapperElDomicilio.querySelector('.dataTables_info');
                if (defaultInfoDomicilio) {
                    defaultInfoDomicilio.style.display = 'none';
                }

                const pagerRowDomicilio = document.createElement('div');
                pagerRowDomicilio.className = 'compras-pager-row';
                wrapperElDomicilio.appendChild(pagerRowDomicilio);

                const infoBarDomicilio = document.createElement('div');
                infoBarDomicilio.className = 'compras-info-bar';
                pagerRowDomicilio.appendChild(infoBarDomicilio);

                const customPagerDomicilio = document.createElement('div');
                customPagerDomicilio.className = 'compras-custom-pagination';
                customPagerDomicilio.setAttribute('aria-label', 'Paginación de ventas a domicilio');
                pagerRowDomicilio.appendChild(customPagerDomicilio);

                injectCustomPaginationStyles();
                renderCustomInfo(dataTableDomicilio, infoBarDomicilio);
                renderCustomPagination(dataTableDomicilio, customPagerDomicilio);
                attachSwipePagination(wrapperElDomicilio, dataTableDomicilio);

                dataTableDomicilio.on('draw.dt', () => {
                    renderCustomInfo(dataTableDomicilio, infoBarDomicilio);
                    renderCustomPagination(dataTableDomicilio, customPagerDomicilio);
                });
            }
        }
    }
}

// Inicializar tablas cuando se carga la página
document.addEventListener('DOMContentLoaded', function() {
    if ($.fn.DataTable) {
        initTablaVentas();
    }
});

// Funciones de paginación personalizada (reutilizadas de compras.js)
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
        /* Ocultar controles nativos duplicados de DataTables */
        .dataTables_wrapper .dataTables_length:not(:first-of-type),
        .dataTables_wrapper .dataTables_filter:not(:first-of-type) {
            display: none !important;
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