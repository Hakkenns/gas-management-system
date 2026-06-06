document.addEventListener("DOMContentLoaded", () => {
    let arrayDetalles = []; // Almacena temporalmente los artículos antes de enviar al controller

    initTablaCompras();

    // Delegación de eventos para clicks
    document.addEventListener("click", function (e) {
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
            fetch('/api/correlativos/next?tipo=COMPRA_NOTA&serie=NC001')
                .then(r => r.json())
                .then(data => {
                    if (data && data.codigo) {
                        document.getElementById('input-documento').value = data.codigo;
                    }
                })
                .catch(err => console.warn('No se pudo obtener correlativo:', err))
                .finally(() => {
                    $("#modal-compra").modal("show");
                });
        }

        if (e.target.closest(".btn-ver-detalle")) {
            const id = e.target.closest(".btn-ver-detalle").dataset.id;
            fetch(`/compras/detalle/${id}`)
                .then(r => r.json())
                .then(data => {
                    const tbody = document.getElementById("filas-ver-detalle");
                    tbody.innerHTML = "";
                    data.forEach(item => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td>${item.producto}</td>
                            <td>${item.cantidad} unidades</td>
                            <td>S/ ${parseFloat(item.precio).toFixed(2)}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                    $("#modal-detalle-ver").modal("show");
                })
                .catch(err => console.error("ERROR CARGANDO DETALLES:", err));
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
                    if (resp.status === 'OK') {
                        reloadComprasTable();
                    } else {
                        alert(resp.message || 'No se pudo anular la compra.');
                    }
                })
                .catch(err => {
                    console.error('ERROR ANULANDO COMPRA:', err);
                    alert('No se pudo anular la compra.');
                });
        }
    });

    // Añadir artículo al listado interno del modal
    document.getElementById("btn-agregar-lista").addEventListener("click", () => {
        const selectProd = document.getElementById("select-producto");
        const inputCant = document.getElementById("select-cantidad");
        const inputPrecio = document.getElementById("select-precio");

        const idProducto = selectProd.value;
        const nombreProducto = selectProd.options[selectProd.selectedIndex].text;
        const cantidad = parseInt(inputCant.value, 10);
        const precio = parseFloat(inputPrecio.value);

        if (!idProducto || cantidad < 1 || isNaN(precio) || precio <= 0) {
            alert("Seleccione un artículo e ingrese cantidades/precios correctos.");
            return;
        }

        const duplicado = arrayDetalles.find(item => item.idProducto === idProducto);
        if (duplicado) {
            duplicado.cantidad += cantidad;
        } else {
            arrayDetalles.push({ idProducto, nombreProducto, cantidad, precioCostoUnitario: precio });
        }

        selectProd.value = "";
        inputCant.value = "1";
        inputPrecio.value = "";

        renderizarFilas();
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
            btn.addEventListener("click", (e) => {
                const idx = e.currentTarget.dataset.index;
                arrayDetalles.splice(idx, 1);
                renderizarFilas();
            });
        });
    }

    // Guardar el formulario completo vía AJAX enviando RequestBody
    $("#form-compra").on("submit", function (e) {
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

        const payload = {
            idProveedor: parseInt(document.getElementById("input-proveedor").value, 10),
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
                if (resp.status === "OK") {
                    $("#modal-compra").modal('hide');
                    // limpiar modo edición
                    delete form.dataset.editId;
                    reloadComprasTable();
                } else {
                    alert(resp.message || "Error al procesar la compra");
                }
            },
            error: function (xhr) {
                alert("Error crítico en la transacción de almacén.");
                console.error(xhr.responseText);
            },
            complete: function () {
                btnSubmit.prop('disabled', false).text('Registrar Ingreso');
            }
        });
    });
});

function initTablaCompras() {
    if ($.fn.DataTable) {
        const table = $('#tabla-compras');
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().destroy();
        }
        table.DataTable({
            paging: true,
            lengthChange: true,
            searching: true,
            ordering: true,
            info: true,
            autoWidth: false,
            responsive: true,
        });
    }
}

function reloadComprasTable() {
    fetch("/compras/tabla")
        .then(r => {
            if (!r.ok) throw new Error("Error cargando tabla de compras");
            return r.text();
        })
        .then(html => {
            const container = document.getElementById("contenedor-tabla");
            if (container) {
                container.innerHTML = html;
                initTablaCompras();
            }
        })
        .catch(err => console.error("ERROR RECARGANDO TABLA:", err));
}