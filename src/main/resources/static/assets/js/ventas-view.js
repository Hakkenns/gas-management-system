$(function() {
    function parseMoney(value) {
        const number = Number(String(value).replace(/[^0-9.-]+/g, '').replace(',', '.'));
        return isNaN(number) ? 0 : number;
    }

    function formatMoney(value) {
        return parseMoney(value).toFixed(2);
    }

    function renderDetalle(detalles) {
        const $body = $('#detalle-venta-body').empty();
        if (!Array.isArray(detalles) || detalles.length === 0) {
            $('#detalle-venta-sin-items').show();
            return;
        }

        $('#detalle-venta-sin-items').hide();
        detalles.forEach(det => {
            const subtotal = formatMoney(det.subtotal || (det.precioUnitario * det.cantidad));
            const $tr = $('<tr>');
            $tr.append(`<td>${det.producto || ''}</td>`);
            $tr.append(`<td class="text-center">${det.cantidad || 0}</td>`);
            $tr.append(`<td class="text-right">S/ ${formatMoney(det.precioUnitario)}</td>`);
            $tr.append(`<td class="text-right">S/ ${subtotal}</td>`);
            $body.append($tr);
        });
    }

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
                renderDetalle(data);
                $('#modal-detalle-venta').modal('show');
            })
            .fail(function() {
                alert('No se pudo cargar el detalle de la venta. Intenta de nuevo.');
            });
    });
});
