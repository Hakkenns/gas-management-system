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

        const payload = {
            idCliente: parseInt($('#select-cliente').val(), 10),
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
