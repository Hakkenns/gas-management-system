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
            $('#group-condicion-pago').hide();
            $('.group-campos-pago').hide();
            $('#group-estado-pedido-parent').hide();
            $('#select-estado-pedido').val('PENDIENTE');
            $('#input-estado-pedido-local').hide();

            if (!$('#input-id-pedido').val()) {
                $('#select-estado-pedido').val('PENDIENTE');
            }
        }
    }

    function renderizarPagos() {
        const tbody = $('#tabla-pagos-venta').empty();
        if (pagosVenta.length === 0) {
            tbody.append('<tr><td colspan="4" class="text-center text-muted">No se han registrado pagos aún.</td></tr>');
            actualizarTotalPagos();
            return;
        }

        pagosVenta.forEach((pago, index) => {
            const tr = $(
                `<tr>
                    <td>${pago.metodoNombre}</td>
                    <td>S/ ${formatMoney(pago.monto)}</td>
                    <td>${pago.numOperacion || ''}</td>
                    <td class="text-center">
                        <button type="button" class="btn btn-danger btn-sm btn-remover-pago" data-index="${index}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>`
            );
            tbody.append(tr);
        });
        actualizarTotalPagos();
    }

    function actualizarTotalPagos() {
        // No hay campo de texto acumulativo de pagos, pero si existiera, se actualizaría aquí.
    }

    function actualizarCamposMetodoPago() {
        const metodoSeleccionado = $('#select-metodo option:selected').text().trim().toLowerCase();
        const esDigital = metodoSeleccionado === 'yape' || metodoSeleccionado === 'plin';

        if (esDigital) {
            $('#row-num-operacion').show();
            $('#input-num-operacion').prop('required', true);
        } else {
            $('#row-num-operacion').hide();
            $('#input-num-operacion').prop('required', false).val('');
        }
    }

    $('#select-metodo').on('change', function() {
        actualizarCamposMetodoPago();
    });

    function actualizarTotal() {
        const subtotal = detallesVenta.reduce((sum, item) => sum + (item.cantidad * item.precioUnitario), 0);
        $('#txt-subtotal').text(formatMoney(subtotal));
        $('#txt-total-general').text(formatMoney(subtotal));
    }

    function configurarLimitesFechaCredito() {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(today.getDate() + 1);
        const maxDate = new Date(today);
        maxDate.setDate(today.getDate() + 2);

        const formatDate = (d) => {
            const yyyy = d.getFullYear();
            const mm = String(d.getMonth() + 1).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        };

        $('#input-fecha-limite').attr('min', formatDate(today));
        $('#input-fecha-limite').attr('max', formatDate(maxDate));
        $('#input-fecha-limite').val(formatDate(tomorrow));
    }

    $(document).on('change', '#select-condicion-pago', function() {
        const condicion = $(this).val();
        if (condicion === 'CREDITO') {
            $('#group-fecha-limite').show();
            configurarLimitesFechaCredito();
            $('#select-metodo').prop('required', false);
        } else {
            $('#group-fecha-limite').hide();
            $('#input-fecha-limite').val('');
            $('#select-metodo').prop('required', true);
        }
    });

    function limpiarClienteSeleccionado() {
        $('#input-id-cliente').val('');
        $('#input-nombre-cliente').val('');
        $('#input-telefono-cliente').val('');
        $('#input-direccion-cliente').val('');
        $('#input-referencia-cliente').val('');
        $('#input-nombre-cliente, #input-telefono-cliente, #input-direccion-cliente, #input-referencia-cliente').prop('readonly', false);
        $('#cliente-feedback').text('Ingrese el DNI y busque el cliente, o complete los datos manualmente.');
    }

    function mostrarCliente(cliente) {
        $('#input-id-cliente').val(cliente.id);
        $('#input-nombre-cliente').val(cliente.nombre);
        $('#input-telefono-cliente').val(cliente.telefono);
        $('#input-direccion-cliente').val(cliente.direccion || '');
        $('#input-referencia-cliente').val(cliente.referencia || '');
        $('#input-nombre-cliente, #input-telefono-cliente, #input-direccion-cliente, #input-referencia-cliente').prop('readonly', true);
        $('#cliente-feedback').text('Cliente encontrado. Si desea usar datos distintos, borre el DNI o comience nuevamente.');
    }

    function buscarClientePorDni(dni) {
        if (!dni) {
            limpiarClienteSeleccionado();
            $('#cliente-feedback').text('DNI opcional, complete los datos del cliente manualmente.');
            return;
        }
        if (!/^[0-9]{8}$/.test(dni)) {
            limpiarClienteSeleccionado();
            $('#input-dni-cliente').val(dni);
            $('#cliente-feedback').text('Ingrese un DNI válido de 8 dígitos.');
            return;
        }

        fetch(`/ventas/cliente?dni=${dni}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Cliente no encontrado');
                }
                return response.json();
            })
            .then(cliente => mostrarCliente(cliente))
            .catch(() => {
                limpiarClienteSeleccionado();
                $('#input-dni-cliente').val(dni);
                $('#cliente-feedback').text('No se encontró un cliente con ese DNI. Complete los datos y la venta creará un nuevo cliente.');
            });
    }

    function renderizarFilas() {
        const isMotorizado = $('#session-perfil-id').val() == '4';
        const estadoPedido = $('#select-estado-pedido').val();
        const isPedidoEditable = !$('#input-id-pedido').val() || estadoPedido === 'PENDIENTE';

        const tbody = $('#tabla-filas-venta').empty();
        detallesVenta.forEach((item, index) => {
            const subtotal = item.cantidad * item.precioUnitario;
            
            const inputPrestados = isPedidoEditable 
                ? `<input type="number" class="form-control form-control-sm input-cantidad-prestada-fila" 
                          min="0" max="${item.cantidad}" value="${item.cantidadPrestada || 0}" 
                          style="width: 80px;" data-index="${index}">`
                : `<span>${item.cantidadPrestada || 0}</span>`;

            const tr = $(
                `<tr>
                    <td>${item.nombreProducto}</td>
                    <td>${item.cantidad}</td>
                    <td>S/ ${formatMoney(item.precioUnitario)}</td>
                    <td>${inputPrestados}</td>
                    <td>S/ ${formatMoney(subtotal)}</td>
                    <td class="text-center col-quitar-producto" ${isMotorizado ? 'style="display:none;"' : ''}>
                        <button type="button" class="btn btn-danger btn-sm btn-remover-item" data-index="${index}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>`
            );
            tbody.append(tr);
        });
        actualizarTotal();

        if (isMotorizado) {
            $('th.col-quitar-producto').hide();
        } else {
            $('th.col-quitar-producto').show();
        }
    }

    // Modal triggers for creating new sale (Local or Domicilio)
    $(document).on('click', '.btn-crear-venta', function() {
        const tipoDefault = $(this).data('tipo'); // LOCAL or DOMICILIO
        
        detallesVenta = [];
        pagosVenta = [];
        
        try {
            if ($('#form-venta')[0]) {
                $('#form-venta')[0].reset();
            }
        } catch(e) {
            console.error("Error al reiniciar el formulario:", e);
        }
        
        $('#tabla-filas-venta').empty();
        $('#tabla-pagos-venta').html('<tr><td colspan="4" class="text-center text-muted">No se han registrado pagos aún.</td></tr>');
        $('#txt-total-general').text('0.00');
        $('#txt-subtotal').text('0.00');

        $('#input-id-pedido').val('');
        limpiarClienteSeleccionado();
        $('#input-dni-cliente').val('');
        $('#select-motorizado').val('');
        $('#select-condicion-pago').val('CONTADO');
        $('#group-fecha-limite').hide();
        $('#input-fecha-limite').val('');
        $('#input-cantidad-prestada').val('0');
        
        // Configurar y bloquear el tipo de venta
        $('#select-tipo-venta').val(tipoDefault).prop('disabled', true);
        actualizarCamposPorTipoVenta();
        actualizarCamposMetodoPago();

        const titulo = tipoDefault === 'LOCAL' ? 'Registrar nueva venta local' : 'Registrar nueva venta a domicilio';
        $('#modal-title-venta').text(titulo);

        // Mostrar el modal INMEDIATAMENTE
        $('#modal-venta').modal('show');
        $('#input-codigo').val('Cargando...');

        fetch('/api/correlativos/next?tipo=VENTA_NOTA&serie=NV001')
            .then(r => {
                if (!r.ok) throw new Error("Error HTTP " + r.status);
                return r.json();
            })
            .then(data => {
                if (data && data.codigo) {
                    $('#input-codigo').val(data.codigo);
                } else {
                    $('#input-codigo').val('NV001-0000');
                }
            })
            .catch(err => {
                console.warn("Fallo al obtener correlativo, usando por defecto:", err);
                $('#input-codigo').val('NV001-0000');
            });
    });

    $(document).on('click', '.btn-editar-venta', function() {
        const ventaId = $(this).data('id');
        if (!ventaId) return;

        fetch(`/ventas/editar/${ventaId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('No se pudo cargar los datos de la venta para editar.');
                }
                return response.json();
            })
            .then(data => {
                $('#form-venta')[0].reset();
                detallesVenta = [];
                pagosVenta = [];

                $('#input-id-pedido').val(data.idPedido);
                $('#input-codigo').val(data.codigo);
                
                const tipoStr = data.tipoVenta || 'LOCAL';
                $('#select-tipo-venta').val(tipoStr).prop('disabled', true);
                
                const titulo = tipoStr === 'LOCAL' ? 'Editar Venta Local: ' : 'Editar Venta Domicilio: ';
                $('#modal-title-venta').text(titulo + data.codigo);

                $('#input-id-cliente').val(data.idCliente);
                $('#input-dni-cliente').val(data.dniCliente);
                $('#input-nombre-cliente').val(data.nombreCliente);
                $('#input-telefono-cliente').val(data.telefonoCliente);
                
                actualizarCamposPorTipoVenta();
                
                if (tipoStr === 'DOMICILIO') {
                    $('#input-direccion-cliente').val(data.direccionCliente || '');
                    $('#input-referencia-cliente').val(data.referenciaCliente || '');
                    $('#select-motorizado').val(data.idEmpleado || '');
                    $('#select-estado-pedido').val(data.estadoPedido || 'PENDIENTE');
                }

                if (data.fechaLimitePago) {
                    $('#select-condicion-pago').val('CREDITO');
                    $('#group-fecha-limite').show();
                    configurarLimitesFechaCredito();
                    $('#input-fecha-limite').val(data.fechaLimitePago.split('T')[0]);
                } else {
                    $('#select-condicion-pago').val('CONTADO');
                    $('#group-fecha-limite').hide();
                    $('#input-fecha-limite').val('');
                }

                $('#input-observaciones').val(data.observaciones || '');

                const perfilId = $('#session-perfil-id').val();
                if (perfilId == '4') {
                    // Bloqueo para motorizados
                    $('#input-dni-cliente, #input-nombre-cliente, #input-telefono-cliente, #input-direccion-cliente, #input-referencia-cliente').prop('readonly', true);
                    $('#btn-buscar-cliente').hide();
                    $('#select-motorizado').prop('disabled', true);
                    $('#input-observaciones').prop('readonly', true);
                    $('#card-agregar-productos').hide();
                } else {
                    // Operador normal
                    $('#input-dni-cliente, #input-nombre-cliente, #input-telefono-cliente, #input-direccion-cliente, #input-referencia-cliente').prop('readonly', false);
                    $('#btn-buscar-cliente').show();
                    $('#select-motorizado').prop('disabled', false);
                    $('#input-observaciones').prop('readonly', false);
                    $('#card-agregar-productos').show();
                }

                data.detalles.forEach(detalle => {
                    detallesVenta.push({
                        idProducto: detalle.idProducto,
                        nombreProducto: detalle.nombreProducto,
                        cantidad: detalle.cantidad,
                        precioUnitario: detalle.precioUnitario,
                        cantidadPrestada: detalle.cantidadPrestada || 0
                    });
                });

                data.pagos.forEach(pago => {
                    pagosVenta.push({
                        idMetodoPago: pago.idMetodoPago,
                        metodoNombre: pago.metodoNombre,
                        monto: pago.monto,
                        numOperacion: pago.numOperacion
                    });
                });

                renderizarFilas();
                renderizarPagos();
                actualizarCamposMetodoPago();
                $('#modal-venta').modal('show');
            })
            .catch(error => {
                alert(error.message);
            });
    });

    $('#btn-buscar-cliente').on('click', function() {
        buscarClientePorDni($('#input-dni-cliente').val().trim());
    });

    $('#input-dni-cliente').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            buscarClientePorDni($(this).val().trim());
        }
    });

    $('#input-dni-cliente').on('input', function() {
        if ($(this).val().trim() === '') {
            limpiarClienteSeleccionado();
        }
    });

    $('#btn-buscar-producto').on('click', function() {
        $('#buscar-producto-filtro').val('');
        $('#tabla-busqueda-productos tbody tr').show();
        $('#select-buscar-categoria').val('');
        $('#modal-buscar-producto').modal('show');
    });

    $('#buscar-producto-filtro').on('input', filtrarProductosModal);
    $('#select-buscar-categoria').on('change', filtrarProductosModal);

    function filtrarProductosModal() {
        const text = $('#buscar-producto-filtro').val().toLowerCase().trim();
        const cat = $('#select-buscar-categoria').val();

        $('#tabla-busqueda-productos tbody tr').each(function() {
            const row = $(this);
            const name = row.find('td').eq(0).text().toLowerCase();
            const rowCat = String(row.data('categoria') || '');

            const matchText = name.indexOf(text) > -1;
            const matchCat = !cat || rowCat === cat;

            if (matchText && matchCat) {
                row.show();
            } else {
                row.hide();
            }
        });
    }

    $(document).on('click', '.btn-seleccionar-producto', function() {
        const row = $(this).closest('tr');
        const id = row.data('id');
        const nombre = row.data('nombre');
        const precio = row.data('precio');

        $('#select-producto').val(id);
        $('#input-producto-nombre').val(nombre);
        $('#select-precio').val(precio);
        $('#select-cantidad').val(1);
        $('#input-cantidad-prestada').val(0);

        $('#modal-buscar-producto').modal('hide');
    });

    $('#btn-agregar-pago').on('click', function() {
        const metodoId = $('#select-metodo').val();
        const metodoNombre = $('#select-metodo option:selected').text().trim();
        const monto = parseMoney($('#input-monto-pago').val());
        let numOperacion = $('#input-num-operacion').val().trim() || null;

        if (!metodoId) {
            alert('Seleccione un método de pago.');
            return;
        }
        if (monto <= 0) {
            alert('Ingrese un monto de pago mayor a cero.');
            return;
        }
        if ((metodoNombre.toLowerCase() === 'yape' || metodoNombre.toLowerCase() === 'plin') && !numOperacion) {
            alert('El número de operación es obligatorio para Yape y Plin.');
            return;
        }

        pagosVenta.push({
            idMetodoPago: parseInt(metodoId, 10),
            metodoNombre: metodoNombre,
            monto: monto,
            numOperacion: numOperacion
        });

        $('#select-metodo').val('');
        $('#input-monto-pago').val('');
        $('#input-num-operacion').val('');
        actualizarCamposMetodoPago();
        renderizarPagos();
    });

    $(document).on('click', '.btn-remover-pago', function() {
        const index = $(this).data('index');
        pagosVenta.splice(index, 1);
        renderizarPagos();
    });

    $('#btn-agregar-detalle').on('click', function() {
        const idProducto = $('#select-producto').val();
        const nombreProducto = $('#input-producto-nombre').val();
        const cantidad = parseInt($('#select-cantidad').val(), 10) || 0;
        const precio = parseMoney($('#select-precio').val());
        const cantidadPrestada = parseInt($('#input-cantidad-prestada').val(), 10) || 0;

        if (!idProducto || cantidad < 1 || precio <= 0) {
            alert('Seleccione un producto y complete cantidad/precio válidos.');
            return;
        }

        if (cantidadPrestada > cantidad) {
            alert('La cantidad de envases prestados no puede ser mayor a la cantidad comprada.');
            return;
        }

        const existente = detallesVenta.find(item => item.idProducto === idProducto);
        if (existente) {
            existente.cantidad += cantidad;
            existente.cantidadPrestada = (existente.cantidadPrestada || 0) + cantidadPrestada;
            if (existente.cantidadPrestada > existente.cantidad) {
                existente.cantidadPrestada = existente.cantidad;
            }
        } else {
            detallesVenta.push({
                idProducto,
                nombreProducto,
                cantidad,
                precioUnitario: precio,
                cantidadPrestada
            });
        }

        $('#select-producto').val('');
        $('#input-producto-nombre').val('');
        $('#select-cantidad').val('1');
        $('#select-precio').val('');
        $('#input-cantidad-prestada').val('0');
        renderizarFilas();
    });

    $(document).on('click', '.btn-remover-item', function() {
        const index = $(this).data('index');
        detallesVenta.splice(index, 1);
        renderizarFilas();
    });

    $(document).on('change', '.input-cantidad-prestada-fila', function() {
        const index = $(this).data('index');
        const val = parseInt($(this).val(), 10) || 0;
        const item = detallesVenta[index];
        if (item) {
            if (val < 0) {
                alert('La cantidad de envases prestados no puede ser negativa.');
                $(this).val(0);
                item.cantidadPrestada = 0;
            } else if (val > item.cantidad) {
                alert('La cantidad de envases prestados no puede ser mayor a la cantidad comprada.');
                $(this).val(item.cantidad);
                item.cantidadPrestada = item.cantidad;
            } else {
                item.cantidadPrestada = val;
            }
        }
    });

    // Form submit validation & AJAX request
    $('#form-venta').on('submit', function(e) {
        e.preventDefault();

        if (detallesVenta.length === 0) {
            alert('Debe agregar al menos un producto a la venta.');
            return;
        }

        const tipoVenta = $('#select-tipo-venta').val();
        const clientId = parseInt($('#input-id-cliente').val(), 10) || null;
        const dniCliente = $('#input-dni-cliente').val().trim() || null;
        const nombreCliente = $('#input-nombre-cliente').val().trim();
        const telefonoCliente = $('#input-telefono-cliente').val().trim() || null;

        if (!nombreCliente) {
            alert('Debe completar el nombre del cliente.');
            return;
        }

        let direccionCliente = null;
        let referenciaCliente = null;
        let idMotorizado = null;

        if (tipoVenta === 'DOMICILIO') {
            direccionCliente = $('#input-direccion-cliente').val().trim() || null;
            idMotorizado = parseInt($('#select-motorizado').val(), 10) || null;
            referenciaCliente = $('#input-referencia-cliente').val().trim() || null;
            if (!direccionCliente) {
                alert('Debe completar la dirección de envío para ventas a domicilio.');
                return;
            }
            if (!idMotorizado) {
                alert('Debe seleccionar un motorizado para ventas a domicilio.');
                return;
            }
        }

        const totalGeneral = parseMoney($('#txt-total-general').text());
        const totalPagado = pagosVenta.reduce((sum, p) => sum + parseMoney(p.monto), 0);
        const condicion = $('#select-condicion-pago').val();

        // VALIDACIÓN DE CONDICIÓN DE PAGO
        if (condicion === 'CONTADO') {
            const estadoPedido = $('#select-estado-pedido').val();
            // Para ventas a domicilio al contado en estado PENDIENTE, permitimos guardar sin pagos asociados.
            // Para otros casos (Ventas Local o Domicilio ya entregado), el pago debe cubrir la totalidad.
            const esPendienteDomicilio = (tipoVenta === 'DOMICILIO' && estadoPedido === 'PENDIENTE');
            
            if (esPendienteDomicilio && pagosVenta.length === 0 && !$('#select-metodo').val()) {
                // Permitido guardar vacío.
            } else {
                if (pagosVenta.length === 0 && $('#select-metodo').val()) {
                    const montoInput = parseMoney($('#input-monto-pago').val() || totalGeneral);
                    if (Math.abs(montoInput - totalGeneral) > 0.01) {
                        alert('Para ventas al contado, el pago debe cubrir la totalidad de la venta.');
                        return;
                    }
                } else if (Math.abs(totalPagado - totalGeneral) > 0.01) {
                    alert('Para ventas al contado, el total de los pagos agregados debe ser igual al total general de la venta.');
                    return;
                }
            }
        } else {
            // A CRÉDITO
            if (totalPagado > totalGeneral) {
                alert('El total pagado no puede ser mayor al total general de la venta a crédito.');
                return;
            }
            if (!$('#input-fecha-limite').val()) {
                alert('Debe especificar una fecha límite de pago para ventas a crédito.');
                return;
            }
        }

        const payload = {
            idPedido: parseInt($('#input-id-pedido').val(), 10) || null,
            idCliente: clientId,
            dniCliente: dniCliente,
            nombreCliente: nombreCliente,
            direccionCliente: direccionCliente,
            telefonoCliente: telefonoCliente,
            referenciaCliente: referenciaCliente,
            idEmpleado: idMotorizado,
            estadoPedido: $('#select-estado-pedido').val(),
            tipoVenta: tipoVenta,
            fechaLimitePago: condicion === 'CREDITO' ? $('#input-fecha-limite').val() + 'T23:59:59' : null,
            pagos: pagosVenta.map(pago => ({
                idMetodoPago: pago.idMetodoPago,
                monto: pago.monto,
                numOperacion: pago.numOperacion
            })),
            observaciones: $('#input-observaciones').val(),
            detalles: detallesVenta.map(item => ({
                idProducto: parseInt(item.idProducto, 10),
                cantidad: item.cantidad,
                precioUnitario: item.precioUnitario,
                cantidadPrestada: item.cantidadPrestada || 0
            }))
        };

        const btnSubmit = $(this).find('button[type="submit"]');
        btnSubmit.prop('disabled', true).text('Guardando...');

        $.ajax({
            url: '/ventas',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(payload),
            success: function(resp) {
                if (resp.status === 'OK') {
                    $('#modal-venta').modal('hide');
                    window.location.reload();
                } else {
                    alert(resp.message || 'No se pudo registrar la venta.');
                }
            },
            error: function(xhr) {
                let message = 'Error al registrar la venta.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                alert(message);
            },
            complete: function() {
                btnSubmit.prop('disabled', false).text('Guardar Venta');
            }
        });
    });

    // Detail view
    $(document).on('click', '.btn-ver-detalle-venta', function() {
        const ventaId = $(this).data('id');
        if (!ventaId) {
            alert('ID de venta inválido');
            return;
        }

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
