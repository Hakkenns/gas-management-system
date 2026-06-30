$(function() {
    let detallesVenta = [];
    let pagosVenta = [];

    function parseMoney(value) {
        const number = Number(String(value).replace(/[^0-9.-]+/g, '').replace(',', '.'));
        return isNaN(number) ? 0 : number;
    }

    function formatMoney(value) {
        return parseMoney(value).toFixed(2);
    }

    function renderizarPagos() {
        const tbody = $('#tabla-pagos').empty();
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
        const totalPagado = pagosVenta.reduce((sum, pago) => sum + parseMoney(pago.monto), 0);
        $('#txt-total-pagado').text(formatMoney(totalPagado));
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

    function actualizarTotal() {
        const total = detallesVenta.reduce((sum, item) => sum + (item.cantidad * item.precioUnitario), 0);
        $('#txt-total-general').text(formatMoney(total));
    }

    function limpiarClienteSeleccionado() {
        $('#input-id-cliente').val('');
        $('#input-nombre-cliente').val('');
        $('#input-telefono-cliente').val('');
        $('#input-nombre-cliente, #input-telefono-cliente').prop('readonly', false);
        $('#cliente-feedback').text('Ingrese el DNI y busque el cliente, o complete los datos manualmente.');
    }

    function mostrarCliente(cliente) {
        $('#input-id-cliente').val(cliente.id);
        $('#input-nombre-cliente').val(cliente.nombre);
        $('#input-telefono-cliente').val(cliente.telefono);
        $('#input-nombre-cliente, #input-telefono-cliente').prop('readonly', true);
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
        const tbody = $('#tabla-filas-venta').empty();
        detallesVenta.forEach((item, index) => {
            const subtotal = item.cantidad * item.precioUnitario;
            const tr = $(
                `<tr>
                    <td>${item.nombreProducto}</td>
                    <td>${item.cantidad}</td>
                    <td>S/ ${formatMoney(item.precioUnitario)}</td>
                    <td>S/ ${formatMoney(subtotal)}</td>
                    <td class="text-center col-quitar-producto">
                        <button type="button" class="btn btn-danger btn-sm btn-remover-item" data-index="${index}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>`
            );
            tbody.append(tr);
        });
        actualizarTotal();
    }

    $('#btn-crear-venta').on('click', function() {
        detallesVenta = [];
        pagosVenta = [];
        $('#form-venta')[0].reset();
        $('#tabla-filas-venta').empty();
        $('#tabla-pagos').empty();
        $('#txt-total-general').text('0.00');
        $('#txt-total-pagado').text('0.00');

        $('#input-id-pedido').val('');
        limpiarClienteSeleccionado();
        $('#input-dni-cliente').val('');
        actualizarCamposMetodoPago();
        $('.modal-title').text('Registrar nueva venta local');

        fetch('/api/correlativos/next?tipo=VENTA_NOTA&serie=NV001')
            .then(r => r.json())
            .then(data => {
                if (data && data.codigo) {
                    $('#input-codigo').val(data.codigo);
                }
            })
            .catch(() => {
                $('#input-codigo').val('NV001-0000');
            })
            .finally(() => {
                $('#modal-venta').modal('show');
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
                $('.modal-title').text('Editar Venta Local: ' + data.codigo);

                $('#input-id-cliente').val(data.idCliente);
                $('#input-dni-cliente').val(data.dniCliente);
                $('#input-nombre-cliente').val(data.nombreCliente);
                $('#input-telefono-cliente').val(data.telefonoCliente);
                $('#input-observaciones').val(data.observaciones || '');

                $('#input-dni-cliente, #input-nombre-cliente, #input-telefono-cliente').prop('readonly', false);
                $('#btn-buscar-cliente').show();
                $('#card-agregar-productos').show();

                data.detalles.forEach(detalle => {
                    detallesVenta.push({
                        idProducto: detalle.idProducto,
                        nombreProducto: detalle.nombreProducto,
                        cantidad: detalle.cantidad,
                        precioUnitario: detalle.precioUnitario
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
        $('#input-buscar-nombre').val('');
        $('#tabla-busqueda-productos tbody tr').show();

        const selectCat = $('#select-buscar-categoria');
        if (selectCat.length && selectCat.find('option').length <= 1) {
            const seen = {};
            selectCat.find('option:gt(0)').remove();
            $('#tabla-busqueda-productos tbody tr').each(function() {
                const row = $(this);
                const catId = row.data('categoria');
                const catName = row.find('td').eq(1).text().trim();
                if (catId != null && catId !== '' && !seen[String(catId)]) {
                    seen[String(catId)] = true;
                    selectCat.append($('<option>').val(catId).text(catName));
                }
            });
        }

        if (selectCat.length) selectCat.val('');
        $('#modal-buscar-producto').modal('show');
    });

    $('#input-producto-nombre').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            const q = $(this).val().trim();
            $('#input-buscar-nombre').val(q);
            $('#select-buscar-categoria').val('');
            filtrarProductos();
            $('#modal-buscar-producto').modal('show');
        }
    });

    function filtrarProductos() {
        const nombre = $('#input-buscar-nombre').val().trim().toLowerCase();
        const categoria = $('#select-buscar-categoria').val();

        $('#tabla-busqueda-productos tbody tr').each(function() {
            const row = $(this);
            const nombreRow = (row.find('td').first().text() || '').toLowerCase();
            const categoriaRow = row.data('categoria') != null ? String(row.data('categoria')) : '';

            const matchNombre = nombre === '' || nombreRow.indexOf(nombre) !== -1;
            const matchCategoria = !categoria || categoria === '' || String(categoriaRow) === String(categoria);

            if (matchNombre && matchCategoria) {
                row.show();
            } else {
                row.hide();
            }
        });
    }

    $('#btn-filtrar-productos').on('click', function() {
        filtrarProductos();
    });

    $('#input-buscar-nombre').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            filtrarProductos();
        }
    });

    $(document).on('click', '.btn-seleccionar-producto', function() {
        const row = $(this).closest('tr');
        const id = row.data('id');
        const precio = row.data('precio');
        const nombreData = row.data('nombre');
        const categoria = row.data('categoria');
        const capacidad = row.data('capacidad');
        const unidad = row.data('unidad');

        const nombre = (nombreData !== undefined && nombreData !== null) ? String(nombreData).trim() : row.attr('data-nombre') || row.find('td').first().text().trim();

        function unidadLabel(u) {
            if (!u) return '';
            if (u === 'KG') return 'kg';
            if (u === 'L') return 'L';
            if (u === 'M') return 'm';
            if (u === 'NO_APLICA' || u === 'NO APLICA') return '';
            return u;
        }

        if (id) {
            const labelUnidad = unidadLabel(unidad);
            let displayName = nombre;
            const capVal = (capacidad !== undefined && capacidad !== null && String(capacidad).trim() !== '') ? String(capacidad).trim() : '';
            if (capVal !== '') {
                displayName = `${nombre} - ${capVal}${labelUnidad ? ' ' + labelUnidad : ''}`;
            }

            $('#select-producto').val(id);
            $('#input-producto-nombre').val(displayName);
            $('#select-precio').val(formatMoney(precio));

            $('#select-cantidad').val(1);
            $('#modal-buscar-producto').modal('hide');
        }
    });

    $('#select-metodo').on('change', function() {
        actualizarCamposMetodoPago();
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
            alert('Ingrese un monto de pago válido.');
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

        $('#input-monto-pago').val('');
        $('#input-num-operacion').val('');
        renderizarPagos();
    });

    $('#btn-agregar-detalle').on('click', function() {
        const idProducto = $('#select-producto').val();
        const nombreProducto = $('#input-producto-nombre').val();
        const cantidad = parseInt($('#select-cantidad').val(), 10);
        const precio = parseMoney($('#select-precio').val());

        if (!idProducto || cantidad < 1 || precio <= 0) {
            alert('Seleccione un producto y complete cantidad/precio.');
            return;
        }

        const existente = detallesVenta.find(item => item.idProducto === idProducto);
        if (existente) {
            existente.cantidad += cantidad;
        } else {
            detallesVenta.push({
                idProducto,
                nombreProducto,
                cantidad,
                precioUnitario: precio
            });
        }

        $('#select-producto').val('');
        $('#input-producto-nombre').val('');
        $('#select-cantidad').val('1');
        $('#select-precio').val('');
        renderizarFilas();
    });

    $(document).on('click', '.btn-remover-item', function() {
        const index = $(this).data('index');
        detallesVenta.splice(index, 1);
        renderizarFilas();
    });

    $(document).on('click', '.btn-remover-pago', function() {
        const index = $(this).data('index');
        pagosVenta.splice(index, 1);
        renderizarPagos();
    });

    $('#form-venta').on('submit', function(e) {
        e.preventDefault();

        if (detallesVenta.length === 0) {
            alert('Debe agregar al menos un producto.');
            return;
        }

        const clientId = parseInt($('#input-id-cliente').val(), 10) || null;
        const dniCliente = $('#input-dni-cliente').val().trim() || null;
        const nombreCliente = $('#input-nombre-cliente').val().trim();

        if (!nombreCliente) {
            alert('Debe completar el nombre del cliente.');
            return;
        }

        const metodoSeleccionado = $('#select-metodo option:selected').text().trim().toLowerCase();
        const numOperacion = $('#input-num-operacion').val().trim() || null;

        if (pagosVenta.length === 0 && $('#select-metodo').val()) {
            if ((metodoSeleccionado === 'yape' || metodoSeleccionado === 'plin') && !numOperacion) {
                alert('El número de operación es obligatorio para Yape y Plin.');
                return;
            }
        }

        const payload = {
            idPedido: parseInt($('#input-id-pedido').val(), 10) || null,
            idCliente: clientId,
            dniCliente: dniCliente,
            nombreCliente: nombreCliente,
            direccionCliente: null,
            telefonoCliente: $('#input-telefono-cliente').val().trim() || null,
            referenciaCliente: null,
            idEmpleado: null,
            idMetodoPago: parseInt($('#select-metodo').val(), 10) || null,
            numOperacion: numOperacion,
            estadoPedido: 'ENTREGADO',
            tipoVenta: 'LOCAL',
            pagos: pagosVenta.map(pago => ({
                idMetodoPago: pago.idMetodoPago,
                monto: pago.monto,
                numOperacion: pago.numOperacion
            })),
            observaciones: $('#input-observaciones').val(),
            detalles: detallesVenta.map(item => ({
                idProducto: parseInt(item.idProducto, 10),
                cantidad: item.cantidad,
                precioUnitario: item.precioUnitario
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

    $(document).on('click', '.btn-ver-detalle-venta', function() {
        const ventaId = $(this).data('id');
        if (!ventaId) return;

        $('#detalle-venta-body').empty();
        $('#detalle-venta-sin-items').hide();

        $.getJSON(`/ventas/detalle/${ventaId}`)
            .done(function(data) {
                const detalles = Array.isArray(data) ? data : data.detalles || [];
                if (detalles.length === 0) {
                    $('#detalle-venta-sin-items').show();
                    $('#modal-detalle-venta').modal('show');
                    return;
                }

                detalles.forEach(det => {
                    const subtotal = parseMoney(det.subtotal || (det.precioUnitario * det.cantidad));
                    const tr = $('<tr>');
                    tr.append(`<td>${det.producto || ''}</td>`);
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
                alert('No se pudo cargar el detalle.');
            });
    });
});
