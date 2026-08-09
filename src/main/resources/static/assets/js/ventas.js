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
                        else if (texto === 'RECHAZADO') clase = 'badge-danger';
                        else if (texto === 'EN_REVISION') clase = 'badge-info';
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
                        else if (textoPago === 'CANCELADO') clasePago = 'badge-danger';

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
        $('#tabla-envases-mov').empty();
        $('#tabla-pagos-venta').html('<tr><td colspan="4" class="text-center text-muted">No se han registrado pagos aún.</td></tr>');
        $('#txt-subtotal').text('0.00');
        $('#txt-total-general').text('0.00');
        $('#row-num-operacion').hide();
        $('#input-num-operacion').prop('required', false);
        $('#select-tipo-mov-envase').val('NINGUNO');
        $('#subseccion-mov-envase').hide();
        solicitudEnvaseActual++;
        limpiarEnvaseVinculado();
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

    // Buscar cliente por DNI en la BD local
    $('#btn-buscar-cliente').on('click', function() {
        const dni = $('#input-dni-cliente').val().trim();
        if (!dni || dni.length !== 8) {
            Swal.fire({
                icon: 'warning',
                title: 'DNI inválido',
                text: 'Ingrese un DNI válido de 8 dígitos',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }

        $.getJSON(`/ventas/cliente?dni=${dni}`)
            .done(function(res) {
                if (res && res.id) {
                    // Cliente encontrado en BD local: autocompletar todos los campos
                    $('#input-id-cliente').val(res.id);
                    $('#input-nombre-cliente').val(res.nombre || '');
                    $('#input-telefono-cliente').val(res.telefono || '');
                    $('#input-direccion-cliente').val(res.direccion || '');
                    $('#input-referencia-cliente').val(res.referencia || '');
                    $('#cliente-feedback').text('Cliente encontrado en la base de datos. Datos autocompletados.');
                } else {
                    limpiarCamposCliente();
                    Swal.fire({
                        icon: 'warning',
                        title: 'Cliente no encontrado',
                        text: 'Complete los datos manualmente.',
                        confirmButtonText: 'Entendido',
                        confirmButtonColor: '#3085d6'
                    });
                }
            })
            .fail(function() {
                // Cliente no existe en la BD (respuesta 404)
                limpiarCamposCliente();
                Swal.fire({
                    icon: 'warning',
                    title: 'Cliente no encontrado',
                    text: 'No existe un cliente registrado con ese DNI. Complete los datos manualmente.',
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#3085d6'
                });
            });
    });

    function limpiarCamposCliente() {
        $('#input-id-cliente').val('');
        $('#input-nombre-cliente').val('');
        $('#input-telefono-cliente').val('');
        $('#input-direccion-cliente').val('');
        $('#input-referencia-cliente').val('');
        $('#cliente-feedback').text('Cliente no encontrado. Complete los datos manualmente.');
    }

    // El mismo modal se utiliza para producto principal y envase. El destino
    // se guarda al abrirlo, porque la sección de envases puede estar visible
    // mientras se busca el producto principal.
    let destinoBusquedaProducto = 'principal';

    // Buscar producto principal
    $('#btn-buscar-producto').on('click', function() {
        destinoBusquedaProducto = 'principal';
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

    // =========================================================================
    // FIX SCROLL: Re-aplicar clases de Bootstrap al cerrar modal secundario
    // =========================================================================
    function fixModalScroll() {
        const $body = $('body');
        // Si el modal principal (#modal-venta) sigue abierto, forzar estado correcto
        if ($('#modal-venta').hasClass('show')) {
            $body.addClass('modal-open');
            $body.css('overflow-y', 'auto');
            $body.css('padding-right', '');
        }
    }

    // Al cerrar el modal de búsqueda de productos, restaurar scroll del modal principal
    $('#modal-buscar-producto').on('hidden.bs.modal', function() {
        fixModalScroll();
    });

    // Al hacer clic en btn-seleccionar-producto (también usado por envases)
    $(document).on('click', '.btn-seleccionar-producto', function() {
        const fila = $(this).closest('tr');
        const id = fila.data('id');
        const nombre = fila.data('nombre');
        const precio = fila.data('precio') || 0;

        // Solo llenar el envase cuando el modal fue abierto desde sus controles.
        if (destinoBusquedaProducto === 'envase') {
            solicitudEnvaseActual++;
            envaseVinculadoSeleccionado = null;
            $('#select-envase-producto').val(id);
            $('#input-envase-nombre').val(nombre);
            $('#select-envase-cantidad').val(1);

            const tipo = $('#select-tipo-mov-envase').val();
            if (tipo === 'VENTA') {
                actualizarPrecioVentaEnvase(id, precio);
            }

            // Ocultar modal y fijar scroll
            $('#modal-buscar-producto').modal('hide');
            fixModalScroll();
            $('#select-envase-cantidad').focus();
            return;
        }

        // Selección normal para productos principales
        $('#select-producto').val(id);
        $('#input-producto-nombre').val(nombre);
        $('#select-cantidad').val(1);
        // Al cambiar el producto principal, se limpian de inmediato los
        // campos de envase y solo se vuelven a llenar si hay relación y venta.
        cargarEnvaseVinculado(id);

        // Obtener datos oficiales del producto para asegurar precio unitario verdadero
        fetch(`/productos/${id}`)
            .then(response => {
                if (!response.ok) throw new Error('No se pudo obtener producto');
                return response.json();
            })
            .then(prod => {
                const precioVenta = prod.precioVenta;
                if (precioVenta !== undefined && precioVenta !== null) {
                    $('#select-precio').val(precioVenta);
                } else {
                    const precioAttr = fila.data('precio') || '';
                    $('#select-precio').val(precioAttr);
                }
            })
            .catch(() => {
                const precioAttr = fila.data('precio') || '';
                $('#select-precio').val(precioAttr);
            })
            .finally(() => {
                // Ocultar modal y fijar scroll
                $('#modal-buscar-producto').modal('hide');
                fixModalScroll();
                $('#select-cantidad').focus();
            });
    });

    // =========================================================================
    // VALIDACIÓN: Cantidad y precio no negativos/cero al agregar detalle
    // =========================================================================
    $('#btn-agregar-detalle').on('click', function() {
        const productoId = $('#select-producto').val();
        const productoNombre = $('#input-producto-nombre').val();
        const cantidad = parseInt($('#select-cantidad').val()) || 0;
        const precio = parseMoney($('#select-precio').val());

        // Validaciones con SweetAlert2
        if (!productoId || !productoNombre) {
            Swal.fire({
                icon: 'warning',
                title: 'Producto no seleccionado',
                text: 'Busque y seleccione un producto antes de añadirlo.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }

        if (cantidad <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Cantidad inválida',
                text: 'La cantidad debe ser mayor a 0.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#select-cantidad').focus();
            return;
        }

        if (precio < 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Precio inválido',
                text: 'El precio unitario no puede ser negativo.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#select-precio').focus();
            return;
        }

        const subtotal = cantidad * precio;
        const existingRow = $(`#tabla-filas-venta tr`).filter(function() {
            return $(this).data('productoId') === productoId;
        }).first();

        if (existingRow.length) {
            const existingCantidad = parseInt(existingRow.data('cantidad')) || 0;
            const nuevaCantidad = existingCantidad + cantidad;
            const nuevoSubtotal = nuevaCantidad * precio;

            existingRow.data('cantidad', nuevaCantidad);
            existingRow.data('subtotal', nuevoSubtotal);
            existingRow.data('precio', precio);

            existingRow.find('td').eq(1).text(nuevaCantidad);
            existingRow.find('td').eq(2).text(`S/ ${formatMoney(precio)}`);
            existingRow.find('td').eq(3).text(`S/ ${formatMoney(nuevoSubtotal)}`);
        } else {
            const tr = $('<tr>');
            tr.html(`
                <td>${productoNombre}</td>
                <td class="text-center">${cantidad}</td>
                <td class="text-right">S/ ${formatMoney(precio)}</td>
                <td class="text-right">S/ ${formatMoney(subtotal)}</td>
                <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-quitar-detalle"><i class="fas fa-trash"></i></button></td>
            `);
            tr.attr('data-producto-id', productoId);
            tr.data('productoId', productoId);
            tr.data('cantidad', cantidad);
            tr.data('precio', precio);
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

    // =========================================================================
    // CALCULAR TOTALES (incluye envases en venta)
    // =========================================================================
    function calcularTotales() {
        let subtotal = 0;

        // Sumar productos de gas
        $('#tabla-filas-venta tr').each(function() {
            subtotal += parseFloat($(this).data('subtotal')) || 0;
        });

        // Sumar envases en VENTA (tienen precio)
        $('#tabla-envases-mov tr').each(function() {
            const precio = parseFloat($(this).data('precio')) || 0;
            const cantidad = parseInt($(this).data('cantidad')) || 0;
            if (precio > 0) {
                subtotal += precio * cantidad;
            }
        });

        $('#txt-subtotal').text(formatMoney(subtotal));
        $('#txt-total-general').text(formatMoney(subtotal));
    }

    // ====================================================================
    // MOVIMIENTO DE ENVASES
    // ====================================================================
    let envaseMovimientos = [];
    let envaseVinculadoSeleccionado = null;
    let solicitudEnvaseActual = 0;

    function limpiarEnvaseVinculado() {
        envaseVinculadoSeleccionado = null;
        $('#select-envase-producto').val('');
        $('#input-envase-nombre').val('');
        $('#select-envase-cantidad').val('');
        $('#select-envase-precio').val('');
        $('#select-envase-fecha-limite').val('');
    }

    function cargarEnvaseVinculado(productoId) {
        const solicitudActual = ++solicitudEnvaseActual;
        limpiarEnvaseVinculado();

        fetch(`/productos/${productoId}/envase`)
            .then(response => response.ok ? response.json() : null)
            .then(relacionEnvase => {
                if (solicitudActual !== solicitudEnvaseActual) return null;
                const envaseId = relacionEnvase?.envaseId;
                if (!envaseId) {
                    return null;
                }

                // Solo la venta de envases autoselecciona el envase vinculado.
                // Para cualquier otro movimiento los campos ya quedaron limpios.
                if ($('#select-tipo-mov-envase').val() !== 'VENTA') {
                    return null;
                }

                return fetch(`/envases/api/${envaseId}`)
                    .then(response => response.ok ? response.json() : null)
                    .then(envase => {
                        if (!envase || solicitudActual !== solicitudEnvaseActual) return null;
                        envaseVinculadoSeleccionado = {
                            productoId,
                            envaseId: envase.id,
                            nombre: envase.nombre,
                            precioEnvase: Number(envase.precioEnvase) || 0
                        };
                        preseleccionarEnvaseVinculado();
                    });
            })
            .catch(() => {
                if (solicitudActual === solicitudEnvaseActual) {
                    limpiarEnvaseVinculado();
                }
            });
    }

    function preseleccionarEnvaseVinculado() {
        if (!envaseVinculadoSeleccionado) return;

        $('#select-envase-producto').val(envaseVinculadoSeleccionado.productoId);
        $('#input-envase-nombre').val(envaseVinculadoSeleccionado.nombre);
        $('#select-envase-cantidad').val(1);
        $('#select-envase-precio').val(envaseVinculadoSeleccionado.precioEnvase.toFixed(2));
    }

    function actualizarPrecioVentaEnvase(productoId, precioProductoAlternativo) {
        fetch(`/productos/${productoId}/envase`)
            .then(response => response.ok ? response.json() : null)
            .then(relacionEnvase => {
                const envaseId = relacionEnvase?.envaseId;

                if (!envaseId) {
                    return Number(precioProductoAlternativo) || 0;
                }

                return fetch(`/envases/api/${envaseId}`)
                    .then(response => response.ok ? response.json() : null)
                    .then(envase => Number(envase?.precioEnvase) || 0);
            })
            .then(precioUnitario => {
                if ($('#select-tipo-mov-envase').val() === 'VENTA'
                    && String($('#select-envase-producto').val()) === String(productoId)) {
                    $('#select-envase-precio').val(Number(precioUnitario).toFixed(2));
                }
            })
            .catch(() => {
                if ($('#select-tipo-mov-envase').val() === 'VENTA') {
                    $('#select-envase-precio').val((Number(precioProductoAlternativo) || 0).toFixed(2));
                }
            });
    }

    // Mostrar/ocultar subseccion segun tipo de movimiento
    $('#select-tipo-mov-envase').on('change', function() {
        const tipo = $(this).val();
        if (tipo === 'NINGUNO') {
            $('#subseccion-mov-envase').hide();
        } else {
            $('#subseccion-mov-envase').show();
            if (tipo === 'VENTA') {
                $('#grupo-envase-precio').show();
                $('#grupo-envase-fecha-limite').hide();
                $('.col-envase-precio').show();
                $('.col-envase-fecha').hide();
                // Al activar la venta de envases, consultar el vínculo del
                // producto principal que ya está seleccionado arriba.
                const productoPrincipalId = $('#select-producto').val();
                if (productoPrincipalId) {
                    cargarEnvaseVinculado(productoPrincipalId);
                } else {
                    // También invalida cualquier consulta anterior pendiente.
                    solicitudEnvaseActual++;
                    limpiarEnvaseVinculado();
                }
            } else if (tipo === 'PRESTAMO') {
                $('#grupo-envase-precio').hide();
                $('#grupo-envase-fecha-limite').show();
                $('.col-envase-precio').hide();
                $('.col-envase-fecha').show();
            }
        }
    });

    // Buscar producto para envase
    $('#btn-buscar-envase-producto').on('click', function() {
        destinoBusquedaProducto = 'envase';
        $('#modal-buscar-producto').modal('show');
    });

    $('#input-envase-nombre').on('click', function() {
        destinoBusquedaProducto = 'envase';
        $('#modal-buscar-producto').modal('show');
    });

    // =========================================================================
    // VALIDACIÓN: Envase - Cantidad y precio no negativos/cero
    // =========================================================================
    $('#btn-agregar-envase').on('click', function() {
        const idProducto = $('#select-envase-producto').val();
        const nombreProducto = $('#input-envase-nombre').val();
        const cantidad = parseInt($('#select-envase-cantidad').val()) || 0;
        const precio = parseMoney($('#select-envase-precio').val());
        const tipo = $('#select-tipo-mov-envase').val();
        const fechaLimite = $('#select-envase-fecha-limite').val() || '';

        // Validaciones con SweetAlert2
        if (!idProducto || !nombreProducto) {
            Swal.fire({
                icon: 'warning',
                title: 'Producto de envase no seleccionado',
                text: 'Seleccione un producto de envase.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }

        if (cantidad <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Cantidad inválida',
                text: 'La cantidad debe ser mayor a 0.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#select-envase-cantidad').focus();
            return;
        }

        if (tipo === 'VENTA' && precio <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Precio inválido',
                text: 'Ingrese un precio unitario válido para la venta del envase (mayor a 0).',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#select-envase-precio').focus();
            return;
        }

        const existingRow = $('#tabla-envases-mov tr').filter(function() {
            return $(this).data('productoId') === idProducto;
        }).first();

        if (existingRow.length) {
            const existingCantidad = parseInt(existingRow.data('cantidad')) || 0;
            const nuevaCantidad = existingCantidad + cantidad;
            existingRow.data('cantidad', nuevaCantidad);
            existingRow.find('td').eq(1).text(nuevaCantidad);
            if (tipo === 'VENTA') {
                const nuevoSubtotal = nuevaCantidad * parseFloat(existingRow.data('precio'));
                existingRow.find('td').eq(3).text('S/ ' + formatMoney(nuevoSubtotal));
            }
        } else {
            const tr = $('<tr>');
            if (tipo === 'VENTA') {
                const subtotal = cantidad * precio;
                tr.html(`
                    <td>${nombreProducto}</td>
                    <td class="text-center">${cantidad}</td>
                    <td class="text-right">S/ ${formatMoney(precio)}</td>
                    <td class="text-center col-envase-fecha" style="display:none;">-</td>
                    <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-quitar-envase"><i class="fas fa-trash"></i></button></td>
                `);
                tr.data('precio', precio);
            } else {
                tr.html(`
                    <td>${nombreProducto}</td>
                    <td class="text-center">${cantidad}</td>
                    <td class="text-right col-envase-precio" style="display:none;">-</td>
                    <td class="text-center col-envase-fecha">${fechaLimite || '-'}</td>
                    <td class="text-center"><button type="button" class="btn btn-danger btn-sm btn-quitar-envase"><i class="fas fa-trash"></i></button></td>
                `);
            }
            tr.attr('data-producto-id', idProducto);
            tr.data('productoId', idProducto);
            tr.data('cantidad', cantidad);
            tr.data('fechaLimite', fechaLimite);
            $('#tabla-envases-mov').append(tr);
        }

        // Recalcular totales (los envases VENTA afectan el total)
        calcularTotales();

        // Limpiar campos
        $('#select-envase-producto').val('');
        $('#input-envase-nombre').val('');
        $('#select-envase-cantidad').val('1');
        $('#select-envase-precio').val('');
        $('#select-envase-fecha-limite').val('');
    });

    // Quitar envase
    $(document).on('click', '.btn-quitar-envase', function() {
        $(this).closest('tr').remove();
        calcularTotales();
    });

    // Agregar pago
    $('#btn-agregar-pago').on('click', function() {
        const metodoId = $('#select-metodo').val();
        const metodoNombre = $('#select-metodo option:selected').text();
        const monto = parseMoney($('#input-monto-pago').val());
        const numOperacion = $('#input-num-operacion').val();

        if (!metodoId || monto <= 0) {
            Swal.fire({
                icon: 'warning',
                title: 'Datos de pago inválidos',
                text: 'Seleccione método de pago e ingrese un monto válido.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }

        if ((metodoNombre === 'YAPE' || metodoNombre === 'PLIN') && !numOperacion) {
            Swal.fire({
                icon: 'warning',
                title: 'Número de operación requerido',
                text: 'Ingrese el número de operación para Yape/Plin.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
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

    // =========================================================================
    // GUARDAR VENTA - Validaciones integrales con SweetAlert2
    // =========================================================================
    $('#form-venta').on('submit', function(e) {
        e.preventDefault();

        // ---------------------------------------------------------------
        // 1. Validar teléfono del cliente (AHORA OBLIGATORIO)
        // ---------------------------------------------------------------
        const telefono = $('#input-telefono-cliente').val().trim();
        if (!telefono) {
            Swal.fire({
                icon: 'warning',
                title: 'Teléfono requerido',
                text: 'El número de teléfono del cliente es obligatorio.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#input-telefono-cliente').focus();
            return;
        }

        // ---------------------------------------------------------------
        // 2. Validar nombre del cliente
        // ---------------------------------------------------------------
        const nombreCliente = $('#input-nombre-cliente').val().trim();
        if (!nombreCliente) {
            Swal.fire({
                icon: 'warning',
                title: 'Nombre requerido',
                text: 'El nombre del cliente es obligatorio.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            $('#input-nombre-cliente').focus();
            return;
        }

        // ---------------------------------------------------------------
        // 3. Validar que haya al menos productos de gas O envases
        // ---------------------------------------------------------------
        const hayProductosGas = $('#tabla-filas-venta tr').length > 0;
        const hayMovimientoEnvases = $('#tabla-envases-mov tr').length > 0;
        const tipoMovEnvase = $('#select-tipo-mov-envase').val() || 'NINGUNO';
        const hayEnvases = tipoMovEnvase !== 'NINGUNO' && hayMovimientoEnvases;

        if (!hayProductosGas && !hayEnvases) {
            Swal.fire({
                icon: 'warning',
                title: 'Sin productos ni envases',
                text: 'Debe agregar al menos un producto (gas) o registrar un movimiento de envases para guardar la venta.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }

        const tipoVenta = $('#select-tipo-venta').val();

        // ---------------------------------------------------------------
        // 4. Validar pagos para venta local
        // ---------------------------------------------------------------
        if (tipoVenta === 'LOCAL') {
            const pagosCount = $('#tabla-pagos-venta tr').filter(function() {
                return $(this).data('idMetodoPago') !== undefined;
            }).length;
            if (pagosCount === 0) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Pago requerido',
                    text: 'Para venta local debe registrar al menos un pago.',
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#3085d6'
                });
                return;
            }
        }

        // ---------------------------------------------------------------
        // 5. Validar motorizado para domicilio
        // ---------------------------------------------------------------
        if (tipoVenta === 'DOMICILIO') {
            const motorizado = $('#select-motorizado').val();
            if (!motorizado) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Motorizado requerido',
                    text: 'Para venta a domicilio debe seleccionar un motorizado/repartidor.',
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#3085d6'
                });
                return;
            }
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
                cantidadPrestada: 0
            });
        });

        // Recopilar pagos
        $('#tabla-pagos-venta tr').filter(function() {
            return $(this).data('idMetodoPago') !== undefined;
        }).each(function() {
            venta.pagos.push({
                idMetodoPago: $(this).data('idMetodoPago') ? Number($(this).data('idMetodoPago')) : null,
                monto: $(this).data('monto'),
                numOperacion: $(this).data('numOperacion')
            });
        });

        // Agregar datos de movimiento de envases
        venta.tipoMovimientoEnvase = tipoMovEnvase;
        venta.envaseMovimientos = [];
        if (tipoMovEnvase !== 'NINGUNO') {
            $('#tabla-envases-mov tr').each(function() {
                venta.envaseMovimientos.push({
                    idProducto: $(this).data('productoId') ? Number($(this).data('productoId')) : null,
                    cantidad: $(this).data('cantidad'),
                    precioUnitario: $(this).data('precio') || 0,
                    fechaLimiteDevolucion: $(this).data('fechaLimite') || '',
                    observacion: ''
                });
            });
        }

        // Validar IDs antes de enviar
        if (venta.detalles.some(d => d.idProducto == null)) {
            Swal.fire({
                icon: 'error',
                title: 'Error en productos',
                text: 'Uno de los productos no tiene ID. Seleccione el producto nuevamente.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
            return;
        }
        if (tipoVenta === 'LOCAL' && venta.pagos.some(p => p.idMetodoPago == null)) {
            Swal.fire({
                icon: 'error',
                title: 'Error en pagos',
                text: 'Uno de los pagos no tiene método de pago. Verifique los pagos registrados.',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#3085d6'
            });
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
                    Swal.fire({
                        icon: 'success',
                        title: '¡Venta registrada!',
                        text: 'La venta se ha registrado correctamente.',
                        confirmButtonText: 'Aceptar',
                        confirmButtonColor: '#28a745'
                    }).then(function() {
                        location.reload();
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Error al guardar',
                        text: response.message || 'Error al guardar la venta',
                        confirmButtonText: 'Entendido',
                        confirmButtonColor: '#3085d6'
                    });
                }
            },
            error: function(xhr) {
                const errorMsg = xhr.responseJSON?.message || xhr.responseText || 'Error al procesar la venta';
                Swal.fire({
                    icon: 'error',
                    title: 'Error del servidor',
                    text: errorMsg,
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#3085d6'
                });
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
                Swal.fire({
                    icon: 'error',
                    title: 'Error al cargar detalle',
                    text: 'No se pudo cargar el detalle de la venta. Intente de nuevo.',
                    confirmButtonText: 'Entendido',
                    confirmButtonColor: '#3085d6'
                });
            });
    });
});

// Inicialización de DataTables para Ventas
function initTablaVentas() {
    if (!$.fn.DataTable) return;

    try {
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
    } catch (e) {
        console.warn("Error inicializando DataTable de ventas:", e);
    }
}

// Inicializar tablas cuando se carga la página
document.addEventListener('DOMContentLoaded', function() {
    if ($.fn.DataTable) {
        try {
            initTablaVentas();
        } catch (e) {
            console.warn("Error inicializando tablas de ventas con DataTables. Los botones seguirán funcionando.", e);
        }
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
