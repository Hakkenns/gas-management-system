// assets/js/ventas.js
// Maneja ventas, crédito, gestión de cuotas y pagos

$(function() {
    // Estado
    let detalle = [];
    let cuotasCalculadas = [];
    let creditoConfigured = false;
    let currentGestionVentaId = null;

    // Helpers
    function parseNumber(v) {
        const n = Number(String(v).replace(/[^\d.-]+/g, '').replace(',', '.'));
        return isNaN(n) ? 0 : n;
    }

    function formatoMoneda(n) {
        return parseNumber(n).toFixed(2);
    }

    function actualizarTotales() {
        const total = detalle.reduce((s, d) => s + parseNumber(d.subtotal), 0);
        $('#venta-total').text(formatoMoneda(total));
    }

    function sanitizeDetalle() {
        return detalle.map(d => ({
            id_producto: d.id_producto,
            cantidad: parseInt(d.cantidad || 0, 10),
            precio_unitario: parseFloat(parseNumber(d.precio_unitario)),
            descuento: parseFloat(parseNumber(d.descuento || 0)),
            subtotal: parseFloat(parseNumber(d.subtotal || 0))
        }));
    }

    function renderTablaProductos() {
        const $tbody = $('#tabla-productos tbody').empty();
        if (detalle.length === 0) {
            $tbody.append('<tr><td colspan="8" class="text-center">Sin productos</td></tr>');
        } else {
            detalle.forEach((d, idx) => {
                const $tr = $('<tr>');
                $tr.append(`<td>${d.id_producto}</td>`);
                $tr.append(`<td>${d.nombre}</td>`);
                $tr.append(`<td class="text-center">${d.stock}</td>`);
                $tr.append(`<td class="text-center"><input type="number" min="1" max="${d.stock}" value="${d.cantidad}" class="form-control input-cant" data-idx="${idx}"></td>`);
                $tr.append(`<td class="text-right">S/ ${formatoMoneda(d.precio_unitario)}</td>`);
                $tr.append(`<td class="text-right">S/ ${formatoMoneda(d.descuento || 0)}</td>`);
                $tr.append(`<td class="text-right">S/ <span class="subtotal" data-idx="${idx}">${formatoMoneda(d.subtotal)}</span></td>`);
                $tr.append(`<td class="text-center"><button class="btn btn-danger btn-sm btn-eliminar" data-idx="${idx}"><i class="fas fa-trash"></i></button></td>`);
                $tbody.append($tr);
            });
        }
        actualizarTotales();
    }

    // =========================
    // LÓGICA DE CRÉDITO
    // =========================

    function configurarEventoCambioFormaPago() {
        $(document).off('change', '#venta-forma-pago').on('change', '#venta-forma-pago', function() {
            const formaPago = $(this).val();
            if (formaPago === 'Credito') {
                $('#btn-abrir-credito').show();
                creditoConfigured = false;
                $('#div-resumen-credito').hide();
            } else {
                $('#btn-abrir-credito').hide();
                $('#div-resumen-credito').hide();
                creditoConfigured = false;
                cuotasCalculadas = [];
                $('#tabla-cuotas-credito tbody').empty();
            }
        });
    }

    $('#btn-abrir-credito').on('click', function() {
        const total = parseNumber($('#venta-total').text());
        if (total <= 0) {
            alert('Agrega productos antes de configurar el crédito');
            return;
        }
        $('#credito-deuda-total').text(formatoMoneda(total));
        $('#modal-credito').modal('show');
    });

    function actualizarDeudaCredito() {
        const total = parseNumber($('#venta-total').text());
        const pagoInicial = parseNumber($('#credito-pago-inicial').val() || 0);
        const deuda = Math.max(0, total - pagoInicial);
        $('#credito-deuda-total').text(formatoMoneda(deuda));
        calcularCuotasCredito();
    }

    $(document).off('change', '#credito-pago-inicial').on('change', '#credito-pago-inicial', function() {
        actualizarDeudaCredito();
    });

    function calcularCuotasCredito() {
        const numCuotas = parseInt($('#credito-num-cuotas').val() || 0, 10);
        const intervalo = $('#credito-intervalo').val();
        const pagoInicial = parseNumber($('#credito-pago-inicial').val() || 0);
        const totalVenta = parseNumber($('#venta-total').text() || 0);

        if (numCuotas <= 0 || !intervalo || totalVenta <= 0) {
            $('#div-tabla-cuotas-credito').hide();
            cuotasCalculadas = [];
            return;
        }

        if (pagoInicial > totalVenta) {
            alert('El pago inicial no puede ser mayor al total');
            $('#credito-pago-inicial').val('0.00');
            actualizarDeudaCredito();
            return;
        }

        const deuda = Math.max(0, totalVenta - pagoInicial);
        let montoPorCuota = parseFloat((deuda / numCuotas).toFixed(2));

        cuotasCalculadas = [];
        const diasIntervalo = {
            'QUINCENAL': 15,
            'MENSUAL': 30,
            'BIMESTRAL': 60,
            'TRIMESTRAL': 90
        }[intervalo] || 30;

        const hoy = new Date();
        for (let i = 1; i <= numCuotas; i++) {
            const fecha = new Date(hoy);
            fecha.setDate(fecha.getDate() + (diasIntervalo * i));
            cuotasCalculadas.push({
                numero: i,
                monto: montoPorCuota,
                fecha: fecha.toISOString().split('T')[0],
                intervalo: intervalo
            });
        }

        // Ajuste por redondeo en la última cuota
        const suma = cuotasCalculadas.reduce((s, c) => s + parseNumber(c.monto), 0);
        const diff = parseFloat((deuda - suma).toFixed(2));
        if (Math.abs(diff) >= 0.01 && cuotasCalculadas.length > 0) {
            cuotasCalculadas[cuotasCalculadas.length - 1].monto = parseFloat((cuotasCalculadas[cuotasCalculadas.length - 1].monto + diff).toFixed(2));
        }

        renderTablaCuotasCredito();
        $('#div-tabla-cuotas-credito').show();
    }

    function renderTablaCuotasCredito() {
        const $tbody = $('#tabla-cuotas-credito tbody').empty();
        if (cuotasCalculadas.length === 0) {
            $tbody.append('<tr><td colspan="3" class="text-center">Sin cuotas</td></tr>');
            return;
        }
        cuotasCalculadas.forEach(c => {
            const $tr = $('<tr>');
            $tr.append(`<td class="text-center"><strong>${c.numero}</strong></td>`);
            $tr.append(`<td class="text-center"><strong>S/ ${formatoMoneda(c.monto)}</strong></td>`);
            $tr.append(`<td class="text-center">${c.fecha}</td>`);
            $tbody.append($tr);
        });
    }

    $(document).off('change', '#credito-num-cuotas').on('change', '#credito-num-cuotas', function() {
        calcularCuotasCredito();
    });

    $(document).off('change', '#credito-intervalo').on('change', '#credito-intervalo', function() {
        calcularCuotasCredito();
    });

    $('#btn-guardar-credito').on('click', function() {
        const numCuotas = parseInt($('#credito-num-cuotas').val() || 0, 10);
        const intervalo = $('#credito-intervalo').val();

        if (numCuotas <= 0 || !intervalo) {
            alert('Debes seleccionar N° de cuotas e intervalo de pago');
            return;
        }

        if (cuotasCalculadas.length === 0) {
            alert('Las cuotas no se calcularon. Intenta de nuevo.' );
            return;
        }

        const pagoInicial = parseNumber($('#credito-pago-inicial').val() || 0);
        const deuda = parseNumber($('#credito-deuda-total').text() || 0);

        $('#resumen-pago-inicial').text(formatoMoneda(pagoInicial));
        $('#resumen-num-cuotas').text(numCuotas);
        $('#resumen-intervalo').text(intervalo);
        $('#resumen-deuda').text(formatoMoneda(deuda));
        $('#div-resumen-credito').show();

        // Guardar el pago_inicial en un atributo de datos
        $('body').attr('data-pago-inicial', pagoInicial);

        creditoConfigured = true;
    });

    $('#modal-credito').on('show.bs.modal', function() {
        const total = parseNumber($('#venta-total').text() || 0);
        $('#credito-deuda-total').text(formatoMoneda(total));
    });

    // =========================
    // CARGA Y BÚSQUEDAS
    // =========================

    function cargarCategorias() {
        $.getJSON('views/Ventas.php?action=obtener_categorias')
            .done(function(resp) {
                const $select = $('#filtro-categoria').empty().append('<option value="">-- Todas las categorías --</option>');
                if (resp && resp.data && Array.isArray(resp.data)) {
                    resp.data.forEach(cat => {
                        $select.append(`<option value="${cat.id}">${cat.nombre}</option>`);
                    });
                }
            })
            .fail(function() {
                console.error('Error cargando categorías');
            });
    }

    function cargarProductosModal(id_categoria = null) {
        const $tbody = $('#tabla-buscar-productos tbody').empty();
        let url = 'views/Ventas.php?action=obtener_productos';
        if (id_categoria && id_categoria > 0) url += '&categoria=' + encodeURIComponent(id_categoria);

        $.getJSON(url)
            .done(function(resp) {
                const data = resp.data || [];
                if (!data.length) {
                    $tbody.append('<tr><td colspan="5" class="text-center">No hay productos disponibles</td></tr>');
                    return;
                }
                data.forEach(p => {
                    const precio = parseNumber(p.precio_venta || p.precio || 0).toFixed(2);
                    const $tr = $('<tr>');
                    $tr.append(`<td>${p.id}</td>`);
                    $tr.append(`<td>${p.nombre || p.descripcion || ''}</td>`);
                    $tr.append(`<td class="text-center">${p.stock || 0}</td>`);
                    $tr.append(`<td class="text-right">S/ ${precio}</td>`);
                    $tr.append(`<td class="text-center"><button class="btn btn-primary btn-sm btn-add-prod" data-id="${p.id}" data-nombre="${p.nombre || p.descripcion || ''}" data-stock="${p.stock || 0}" data-precio="${precio}"><i class="fas fa-plus"></i></button></td>`);
                    $tbody.append($tr);
                });
            })
            .fail(function(xhr, status, error) {
                console.error('Error cargando productos:', error);
                $tbody.append('<tr><td colspan="5" class="text-danger text-center">Error cargando productos</td></tr>');
            });
    }

    $('#btn-productos').on('click', function() {
        $('#filtro-categoria').empty().append('<option value="">-- Todas las categorías --</option>');
        cargarCategorias();
        cargarProductosModal();
        $('#modal-productos').modal('show');
    });

    $(document).on('change', '#filtro-categoria', function() {
        const id_categoria = $(this).val();
        cargarProductosModal(id_categoria || null);
    });

    $(document).on('keyup', '#buscarProductoVenta', function() {
        const termino = $(this).val().toLowerCase();
        $('#tabla-buscar-productos tbody tr').each(function() {
            const nombre = $(this).find('td:eq(1)').text().toLowerCase();
            $(this).toggle(nombre.indexOf(termino) !== -1);
        });
    });

    // =========================
    // DETALLE DE PRODUCTOS
    // =========================

    $(document).on('click', '.btn-add-prod', function() {
        const $b = $(this);
        const p = {
            id_producto: $b.data('id'),
            nombre: $b.data('nombre'),
            stock: parseInt($b.data('stock') || 0, 10),
            cantidad: 1,
            precio_unitario: parseFloat(parseNumber($b.data('precio') || 0)),
            descuento: 0,
            subtotal: parseFloat(parseNumber($b.data('precio') || 0))
        };
        const idx = detalle.findIndex(x => x.id_producto == p.id_producto);
        if (idx >= 0) {
            const nuevo = Math.min(detalle[idx].cantidad + 1, p.stock);
            detalle[idx].cantidad = nuevo;
            detalle[idx].subtotal = parseFloat((nuevo * detalle[idx].precio_unitario - (detalle[idx].descuento || 0)).toFixed(2));
        } else {
            detalle.push(p);
        }
        renderTablaProductos();
    });

    $(document).on('change', '.input-cant', function() {
        const idx = $(this).data('idx');
        let val = parseInt($(this).val() || 1, 10);
        if (isNaN(val) || val < 1) val = 1;
        const max = parseInt($(this).attr('max') || 999999, 10);
        if (val > max) val = max;
        $(this).val(val);
        if (detalle[idx]) {
            detalle[idx].cantidad = val;
            detalle[idx].subtotal = parseFloat((detalle[idx].cantidad * detalle[idx].precio_unitario - (detalle[idx].descuento || 0)).toFixed(2));
            $(`.subtotal[data-idx="${idx}"]`).text(formatoMoneda(detalle[idx].subtotal));
            actualizarTotales();
        }
    });

    $(document).on('click', '.btn-eliminar', function() {
        const idx = $(this).data('idx');
        detalle.splice(idx, 1);
        renderTablaProductos();
    });

    // =========================
    // CLIENTE / DNI
    // =========================

    $('#btn-buscar-dni').on('click', function() {
        const doc = $('#venta-dni').val().trim();
        $('#venta-nombre').val('');
        $('#venta-correo').val('');
        $('#venta-id-cliente').val('');

        if (!doc || !/^\d+$/.test(doc)) {
            alert('Ingrese sólo números (DNI 8 / RUC 11)');
            return;
        }

        $.getJSON('views/Ventas.php?action=buscarClienteBD&documento=' + encodeURIComponent(doc))
            .done(function(resp) {
                if (!resp) return alert('Respuesta inválida');
                if (resp.success && resp.data) {
                    $('#venta-id-cliente').val(resp.data.id || '');
                    $('#venta-nombre').val(resp.data.nombre || '');
                    $('#venta-correo').val(resp.data.correo || '');
                } else if (resp.found_in === 'API' && resp.data) {
                    $('#venta-nombre').val(resp.data.nombre || '');
                    $('#venta-correo').val('');
                } else {
                    alert(resp.message || 'Cliente no encontrado');
                    $('#venta-correo').val('');
                }
            })
            .fail(function() {
                alert('Error conectando al servidor para buscar cliente');
            });
    });

    $('#btn-sin-cliente').on('click', function() {
        $('#venta-dni').val('');
        $('#venta-id-cliente').val('');
        $('#venta-nombre').val('CLIENTE FINAL');
        $('#venta-correo').val('');
    });

    // =========================
    // CORRELATIVO
    // =========================

    $('#venta-comprobante').on('change', function() {
        const tipo = $(this).val();
        if (!tipo) return;
        $.getJSON('views/Ventas.php?action=correlativo&tipo=' + encodeURIComponent(tipo))
            .done(function(resp) {
                if (resp && resp.success) {
                    $('#venta-nro-doc').val(resp.n_venta);
                } else {
                    alert('Error obteniendo correlativo');
                }
            })
            .fail(function() {
                alert('Error al solicitar correlativo');
            });
    });

    // =========================
    // GUARDAR VENTA
    // =========================

    $('#btn-guardar-venta').on('click', function() {
        if (detalle.length === 0) {
            alert('Agrega al menos un producto');
            return;
        }

        const tipo_comprobante = $('#venta-comprobante').val();
        if (!tipo_comprobante) {
            alert('Selecciona comprobante');
            return;
        }

        const formaPagoVenta = $('#venta-forma-pago').val() || 'Contado';
        if (formaPagoVenta === 'Credito') {
            if (!creditoConfigured || cuotasCalculadas.length === 0) {
                alert('Para crédito: Debes configurar el crédito correctamente');
                return;
            }
        }

        // Construir valores numéricos seguros
        const total = parseNumber($('#venta-total').text() || 0);
        const pagoInicial = formaPagoVenta === 'Credito' ? parseFloat($('body').attr('data-pago-inicial') || $('#credito-pago-inicial').val() || 0) : 0;
        const deuda = Math.max(0, total - pagoInicial);

        // Prepara payload
        const payload = {
            tipo_comprobante: tipo_comprobante,
            doc_cliente: $('#venta-dni').val().trim(),
            nombre_cliente: $('#venta-nombre').val().trim() || 'CLIENTE ANÓNIMO',
            correo_cliente: $('#venta-correo').val().trim() || '',
            tipo_doc_cliente: ($('#venta-dni').val().trim().length === 11) ? 'RUC' : 'DNI',
            id_cliente: $('#venta-id-cliente').val() || null,
            forma_pago: $('#venta-forma').val() || 'Efectivo',
            tipo_pago: $('#venta-forma').val() || 'Efectivo',
            total: parseFloat(total),
            total_general: parseFloat(total),
            forma_pago_venta: formaPagoVenta,
            pago_inicial: parseFloat(pagoInicial),
            deuda_total: parseFloat(deuda),
            num_cuotas: formaPagoVenta === 'Credito' ? parseInt($('#credito-num-cuotas').val() || 0, 10) : 0,
            intervalo_pago: formaPagoVenta === 'Credito' ? ($('#credito-intervalo').val() || '') : '',
            cuotas: formaPagoVenta === 'Credito' ? cuotasCalculadas.map(c => ({
                numero: parseInt(c.numero, 10),
                monto: parseFloat(parseNumber(c.monto)),
                fecha: c.fecha,
                intervalo: c.intervalo || ''
            })) : [],
            detalle: sanitizeDetalle()
        };

        console.log('Payload a enviar:', payload);

        const $btn = $(this).prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Guardando...');
        $.ajax({
            url: 'views/Ventas.php?action=guardar',
            method: 'POST',
            data: JSON.stringify(payload),
            contentType: 'application/json',
            dataType: 'json',
            timeout: 30000,
            success: function(resp) {
                console.log('Respuesta servidor (success):', resp);
                if (resp && resp.success) {
                    alert('Venta registrada correctamente');
                    $('#modal-venta').modal('hide');
                    location.reload();
                } else {
                    alert('Error: ' + (resp && resp.message ? resp.message : 'Respuesta inválida'));
                    console.error('Respuesta servidor (success=false):', resp);
                }
            },
            error: function(xhr, status, err) {
                var texto = xhr.responseText || err || status;
                try {
                    var obj = JSON.parse(texto);
                    texto = obj.message || JSON.stringify(obj);
                } catch (e) {
                    // no es JSON
                }
                alert('Error en servidor: ' + texto);
                console.error('AJAX error', status, err, xhr);
            },
            complete: function() {
                $btn.prop('disabled', false).html('<i class="fas fa-save"></i> Guardar Venta');
            }
        });
    });

    // =========================
    // Ver productos de una venta
    // =========================

    $(document).on('click', '.btn-ver-productos', function() {
        const id_venta = $(this).data('id');
        const $tbody = $('#tabla-detalle-productos tbody').empty();

        $.getJSON('views/Ventas.php?action=detalle&id=' + encodeURIComponent(id_venta))
            .done(function(resp) {
                if (resp && resp.success && resp.data) {
                    const productos = resp.data;
                    if (productos.length === 0) {
                        $tbody.append('<tr><td colspan="6" class="text-center">Sin productos</td></tr>');
                    } else {
                        productos.forEach(p => {
                            const $tr = $('<tr>');
                            $tr.append(`<td>${p.id_producto}</td>`);
                            $tr.append(`<td>${p.producto || 'N/A'}</td>`);
                            $tr.append(`<td class="text-center">${p.cantidad}</td>`);
                            $tr.append(`<td class="text-right">S/ ${parseFloat(p.precio_unitario).toFixed(2)}</td>`);
                            $tr.append(`<td class="text-right">S/ ${parseFloat(p.descuento || 0).toFixed(2)}</td>`);
                            $tr.append(`<td class="text-right">S/ ${parseFloat(p.subtotal).toFixed(2)}</td>`);
                            $tbody.append($tr);
                        });
                    }
                    $('.modal-backdrop').remove();
                    $('#modal-ver-productos').modal('show');
                } else {
                    alert('Error cargando detalles de la venta');
                }
            })
            .fail(function() {
                alert('Error conectando al servidor');
            });
    });

    // =========================
    // GESTIÓN DE CUOTAS Y PAGOS
    // =========================

    function cargarGestionCuotas(id_venta) {
        const $tbody = $('#tabla-gestion-cuotas tbody').empty();
        $.getJSON('views/Ventas.php?action=obtener_cuotas&id=' + encodeURIComponent(id_venta))
            .done(function(resp) {
                if (!resp || !resp.success) {
                    $tbody.append('<tr><td colspan="5" class="text-center text-danger">Error cargando cuotas</td></tr>');
                    return;
                }
                const cuotas = resp.data || [];
                if (!cuotas.length) {
                    $tbody.append('<tr><td colspan="5" class="text-center">Sin cuotas</td></tr>');
                    return;
                }

                // VALIDACIÓN SECUENCIAL: determinar si se puede pagar cada cuota
                let allPrevPaid = true;
                cuotas.forEach(c => {
                    const estadoRaw = (c.estado_cuota || c.estado || '').toString().toLowerCase();
                    const estado = (estadoRaw === 'pagada' || estadoRaw === 'paid') ? 'Pagada' : 'Pendiente';
                    const numero = c.numero_cuota || c.numero || '-';
                    const monto = parseNumber(c.monto_cuota || c.monto || 0);
                    const fechaProg = c.fecha_programada || c.fecha || '-';

                    const $tr = $('<tr>');
                    $tr.append(`<td class="text-center"><strong>${numero}</strong></td>`);
                    $tr.append(`<td class="text-center">S/ ${formatoMoneda(monto)}</td>`);
                    $tr.append(`<td class="text-center">${fechaProg}</td>`);
                    $tr.append(`<td class="text-center">${estado === 'Pagada' ? '<span class="badge badge-success">Pagada</span>' : '<span class="badge badge-warning">Pendiente</span>'}</td>`);

                    const $accionTd = $('<td class="text-center">');
                    if (estado === 'Pagada') {
                        // Si está pagada, mostrar botón deshabilitado
                        $accionTd.append('<button class="btn btn-secondary btn-sm" disabled><i class="fas fa-check"></i> Pagada</button>');
                    } else {
                        // Si está pendiente, verificar si se puede pagar (validar secuencia)
                        const id_cuota = c.id_cuota || c.id || 0;
                        if (allPrevPaid) {
                            // Todas las anteriores están pagadas, permitir pago
                            $accionTd.append(`<button class="btn btn-success btn-sm btn-pagar-cuota-row" data-id="${id_cuota}" data-num="${numero}" data-monto="${monto}"><i class="fas fa-money-bill-wave"></i> Pagar</button>`);
                        } else {
                            // Hay cuotas anteriores pendientes, deshabilitar
                            $accionTd.append(`<button class="btn btn-warning btn-sm" disabled title="Debe pagar las cuotas anteriores primero"><i class="fas fa-lock"></i> Bloqueada</button>`);
                        }
                        // Marcar esta cuota como pendiente para las siguientes
                        allPrevPaid = false;
                    }
                    $tr.append($accionTd);
                    $tbody.append($tr);
                });
            })
            .fail(function() {
                $tbody.append('<tr><td colspan="5" class="text-center text-danger">Error conectando al servidor</td></tr>');
            });
    }

    // Abrir modal gestión de cuotas desde el botón "Pagar" de la fila de venta
    $(document).on('click', '.btn-pagar-venta', function() {
        const id_venta = $(this).data('id');
        if (!id_venta) {
            alert('ID de venta inválido');
            return;
        }
        currentGestionVentaId = id_venta;
        $('#cuota-venta-id').text(id_venta);
        cargarGestionCuotas(id_venta);
        $('#modal-gestion-cuotas').modal('show');
    });

    // Abrir modal pago de cuota (desde la tabla de gestión de cuotas)
    $(document).on('click', '.btn-pagar-cuota-row', function() {
        const id_cuota = $(this).data('id');
        const numero = $(this).data('num');
        const monto = parseNumber($(this).data('monto') || 0);

        $('#pago-id-cuota').val(id_cuota);
        $('#pago-numero-cuota').text(numero || '-');
        $('#pago-monto-cuota').text('S/ ' + formatoMoneda(monto));
        $('#pago-fecha').val(new Date().toISOString().split('T')[0]);
        $('#pago-metodo').val('');
        $('#modal-pago-cuota').modal('show');
    });

    // Guardar pago de cuota
$('#btn-guardar-pago').on('click', function() {
    const id_cuota = parseInt($('#pago-id-cuota').val() || 0, 10);
    const metodo = $('#pago-metodo').val() || '';
    const fecha_pago = $('#pago-fecha').val() || '';
    const montoTxt = $('#pago-monto-cuota').text().replace(/[^\d.,-]+/g, '').replace(',', '.');
    const monto = parseFloat(montoTxt || 0);

    if (!id_cuota || !metodo || monto <= 0 || !fecha_pago) {
        alert('Complete método, fecha y asegúrese del monto');
        return;
    }

    const $btn = $(this).prop('disabled', true).text('Guardando...');
    const payload = {
        id_cuota: id_cuota,
        metodo_pago: metodo,
        monto: monto,
        fecha_pago: fecha_pago
    };

    $.ajax({
        url: 'views/Ventas.php?action=guardar_pago',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(payload),
        dataType: 'json',
        success: function(resp) {
            if (resp && resp.success) {
                $('#modal-pago-cuota').modal('hide');
                $('#modal-gestion-cuotas').modal('hide');
                if (currentGestionVentaId) {
                    cargarGestionCuotas(currentGestionVentaId);
                }
                console.log('✓ Pago registrado correctamente');
                // Recarga la página de inmediato
                location.reload();
            } else {
                alert('Error al registrar pago: ' + (resp && resp.message ? resp.message : 'respuesta inválida'));
            }
        },
        error: function(xhr) {
            var texto = xhr.responseText || 'Error en servidor';
            try {
                var obj = JSON.parse(texto);
                texto = obj.message || texto;
            } catch (e) {}
            alert('Error: ' + texto);
        },
        complete: function() {
            $btn.prop('disabled', false).text('Guardar Pago');
        }
    });
});

    // =========================
    // LIMPIEZA Y ESTADO INICIAL
    // =========================

    $('#modal-venta').on('hidden.bs.modal', function() {
        detalle = [];
        cuotasCalculadas = [];
        creditoConfigured = false;
        $('#venta-dni').val('');
        $('#venta-nombre').val('');
        $('#venta-correo').val('');
        $('#venta-id-cliente').val('');
        $('#venta-comprobante').val('');
        $('#venta-nro-doc').val('');
        $('#venta-forma').val('Efectivo');
        $('#venta-forma-pago').val('Contado');
        $('#btn-abrir-credito').hide();
        $('#div-resumen-credito').hide();
        $('#tabla-productos tbody').empty();
        $('#venta-total').text('0.00');
        $('body').removeAttr('data-pago-inicial');
    });

    $('#modal-credito').on('hidden.bs.modal', function() {
        $('#credito-pago-inicial').val('0.00');
        $('#credito-num-cuotas').val('');
        $('#credito-intervalo').val('');
        $('#credito-deuda-total').text('0.00');
        $('#tabla-cuotas-credito tbody').empty();
        $('#div-tabla-cuotas-credito').hide();
    });

    // Inicial
    configurarEventoCambioFormaPago();
    renderTablaProductos();
    console.log('✓ Ventas.js cargado correctamente');
});