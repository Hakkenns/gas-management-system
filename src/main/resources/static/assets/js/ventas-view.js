$(function() {
    let detallesVenta = [];

    function parseMoney(value) {
        const number = Number(String(value).replace(/[^0-9.-]+/g, '').replace(',', '.'));
        return isNaN(number) ? 0 : number;
    }

    function formatMoney(value) {
        return parseMoney(value).toFixed(2);
    }

    function actualizarTotal() {
        const total = detallesVenta.reduce((sum, item) => sum + (item.cantidad * item.precioUnitario), 0);
        $('#txt-total-general').text(formatMoney(total));
    }

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
        $('#input-direccion-cliente').val(cliente.direccion);
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
        const tbody = $('#tabla-filas-venta').empty();
        detallesVenta.forEach((item, index) => {
            const subtotal = item.cantidad * item.precioUnitario;
            const tr = $(
                `<tr>
                    <td>${item.nombreProducto}</td>
                    <td>${item.cantidad}</td>
                    <td>S/ ${formatMoney(item.precioUnitario)}</td>
                    <td>S/ ${formatMoney(subtotal)}</td>
                    <td class="text-center">
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
        $('#form-venta')[0].reset();
        $('#tabla-filas-venta').empty();
        $('#txt-total-general').text('0.00');

        limpiarClienteSeleccionado();
        $('#input-dni-cliente').val('');

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

    $('#select-producto').on('change', function() {
        const precio = $(this).find('option:selected').data('precio');
        if (precio !== undefined) {
            $('#select-precio').val(formatMoney(precio));
        }
    });

    $('#btn-agregar-detalle').on('click', function() {
        const idProducto = $('#select-producto').val();
        const nombreProducto = $('#select-producto option:selected').text();
        const cantidad = parseInt($('#select-cantidad').val(), 10);
        const precio = parseMoney($('#select-precio').val());

        if (!idProducto || cantidad < 1 || precio <= 0) {
            alert('Seleccione un producto y complete cantidad/precio válidos.');
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
        $('#select-cantidad').val('1');
        $('#select-precio').val('');
        renderizarFilas();
    });

    $(document).on('click', '.btn-remover-item', function() {
        const index = $(this).data('index');
        detallesVenta.splice(index, 1);
        renderizarFilas();
    });

    $('#form-venta').on('submit', function(e) {
        e.preventDefault();

        if (detallesVenta.length === 0) {
            alert('Debe agregar al menos un producto a la venta.');
            return;
        }

        const clientId = parseInt($('#input-id-cliente').val(), 10) || null;
        const dniCliente = $('#input-dni-cliente').val().trim() || null;
        const nombreCliente = $('#input-nombre-cliente').val().trim();
        const direccionCliente = $('#input-direccion-cliente').val().trim();
        const telefonoCliente = $('#input-telefono-cliente').val().trim();
        const referenciaCliente = $('#input-referencia-cliente').val().trim() || null;

        if (!nombreCliente || !direccionCliente || !telefonoCliente) {
            alert('Debe completar el nombre, dirección y teléfono del cliente.');
            return;
        }

        const payload = {
            idCliente: clientId,
            dniCliente: dniCliente,
            nombreCliente: nombreCliente,
            direccionCliente: direccionCliente,
            telefonoCliente: telefonoCliente,
            referenciaCliente: referenciaCliente,
            idMetodoPago: parseInt($('#select-metodo').val(), 10),
            observaciones: $('#input-observaciones').val(),
            detalles: detallesVenta.map(item => ({
                idProducto: parseInt(item.idProducto, 10),
                cantidad: item.cantidad,
                precioUnitario: item.precioUnitario
            }))
        };

        const btnSubmit = $(this).find('button[type="submit"]');
        btnSubmit.prop('disabled', true).text('Registrando...');

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
                btnSubmit.prop('disabled', false).text('Registrar Venta');
            }
        });
    });

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
                if (!Array.isArray(data) || data.length === 0) {
                    $('#detalle-venta-sin-items').show();
                    $('#modal-detalle-venta').modal('show');
                    return;
                }

                data.forEach(det => {
                    const subtotal = parseMoney(det.subtotal || (det.precioUnitario * det.cantidad));
                    const tr = $('<tr>');
                    tr.append(`<td>${det.producto || ''}</td>`);
                    tr.append(`<td class="text-center">${det.cantidad || 0}</td>`);
                    tr.append(`<td class="text-right">S/ ${formatMoney(det.precioUnitario)}</td>`);
                    tr.append(`<td class="text-right">S/ ${formatMoney(subtotal)}</td>`);
                    $('#detalle-venta-body').append(tr);
                });

                $('#modal-detalle-venta').modal('show');
            })
            .fail(function() {
                alert('No se pudo cargar el detalle de la venta. Intenta de nuevo.');
            });
    });
});
