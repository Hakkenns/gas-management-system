document.addEventListener("DOMContentLoaded", function() {
    cargarDeudores();

    // Delegacion de eventos para botones de devolucion
    document.addEventListener("click", function(e) {
        // Boton "Registrar Devolucion"
        if (e.target.closest(".btn-registrar-devolucion")) {
            const btn = e.target.closest(".btn-registrar-devolucion");
            const row = btn.closest("tr");
            if (!row) return;

            const idControl = row.dataset.idControl;
            const clienteNombre = row.dataset.clienteNombre;
            const productoNombre = row.dataset.productoNombre;
            const cantidadPrestada = parseInt(row.dataset.cantidadPrestada) || 0;
            const cantidadDevuelta = parseInt(row.dataset.cantidadDevuelta) || 0;
            const pendiente = cantidadPrestada - cantidadDevuelta;

            document.getElementById("devolucion-id-control").value = idControl;
            document.getElementById("devolucion-cliente-nombre").textContent = clienteNombre;
            document.getElementById("devolucion-producto-nombre").textContent = productoNombre;
            document.getElementById("devolucion-cantidad-prestada").textContent = cantidadPrestada;
            document.getElementById("devolucion-cantidad-devueltos").textContent = cantidadDevuelta;
            document.getElementById("devolucion-pendiente").textContent = pendiente;
            document.getElementById("devolucion-cantidad").value = 1;
            document.getElementById("devolucion-cantidad").max = pendiente;

            $("#modal-devolucion").modal("show");
        }
    });

    // Confirmar devolucion
    document.getElementById("btn-confirmar-devolucion").addEventListener("click", function() {
        const idControl = document.getElementById("devolucion-id-control").value;
        const cantidad = parseInt(document.getElementById("devolucion-cantidad").value);

        if (!idControl || isNaN(cantidad) || cantidad < 1) {
            alert("Ingrese una cantidad valida a devolver.");
            return;
        }

        const pendiente = parseInt(document.getElementById("devolucion-pendiente").textContent);
        if (cantidad > pendiente) {
            alert("La cantidad a devolver no puede exceder el saldo pendiente (" + pendiente + ").");
            return;
        }

        const btn = this;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';

        fetch("/envases/api/devolucion", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                idControl: parseInt(idControl),
                cantidadDevolver: cantidad
            })
        })
        .then(r => r.json())
        .then(resp => {
            if (resp.status === "OK") {
                alert(resp.message || "Devolucion registrada correctamente");
                $("#modal-devolucion").modal("hide");
                cargarDeudores();
            } else {
                alert(resp.message || "Error al registrar devolucion");
            }
        })
        .catch(err => {
            console.error("Error:", err);
            alert("Error al procesar la devolucion");
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check"></i> Confirmar Devolucion';
        });
    });

    // Filtros de busqueda
    const searchInput = document.getElementById("envases-search");
    if (searchInput) {
        searchInput.addEventListener("keyup", function() {
            const texto = this.value.toLowerCase().trim();
            document.querySelectorAll("#cuerpo-tabla-envases tr").forEach(row => {
                const textoFila = row.textContent.toLowerCase();
                row.style.display = textoFila.includes(texto) ? "" : "none";
            });
        });
    }

    const lengthSelect = document.getElementById("envases-length");
    if (lengthSelect) {
        lengthSelect.addEventListener("change", function() {
            const pageLength = parseInt(this.value);
            const rows = document.querySelectorAll("#cuerpo-tabla-envases tr");
            rows.forEach((row, index) => {
                if (index < pageLength) {
                    row.style.display = "";
                } else {
                    row.style.display = "none";
                }
            });
        });
    }
});

function cargarDeudores() {
    const tbody = document.getElementById("cuerpo-tabla-envases");
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="9" class="text-center text-muted">
                <i class="fas fa-spinner fa-spin"></i> Cargando deudores...
            </td>
        </tr>
    `;

    fetch("/envases/api/deudores")
        .then(r => {
            if (!r.ok) throw new Error("Error al cargar deudores");
            return r.json();
        })
        .then(data => {
            tbody.innerHTML = "";

            if (!data || data.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="9" class="text-center text-success">
                            <i class="fas fa-check-circle"></i> No hay clientes con envases pendientes
                        </td>
                    </tr>
                `;
                return;
            }

            data.forEach(item => {
                const pendiente = item.cantidadPendiente || 0;
                let estadoBadge = "";
                if (item.estado === "PRESTADO") {
                    estadoBadge = '<span class="badge badge-warning">Pendiente</span>';
                } else if (item.estado === "PARCIAL") {
                    estadoBadge = '<span class="badge badge-info">Parcial</span>';
                } else {
                    estadoBadge = '<span class="badge badge-secondary">' + item.estado + '</span>';
                }

                const tr = document.createElement("tr");
                tr.dataset.idControl = item.idControl;
                tr.dataset.clienteNombre = item.nombreCliente || "";
                tr.dataset.productoNombre = item.productoNombre || "";
                tr.dataset.cantidadPrestada = item.cantidadPrestada || 0;
                tr.dataset.cantidadDevuelta = item.cantidadDevuelta || 0;

                let fechaStr = "-";
                if (item.fechaEntrega) {
                    try {
                        const d = new Date(item.fechaEntrega);
                        fechaStr = d.toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit", year: "numeric" });
                    } catch(e) {}
                }

                tr.innerHTML = `
                    <td><strong>${item.nombreCliente || "-"}</strong></td>
                    <td>${item.telefonoCliente || "-"}</td>
                    <td>${item.direccionCliente || "-"}</td>
                    <td>${item.codigoPedido || "-"}</td>
                    <td>${fechaStr}</td>
                    <td>${item.productoNombre || "-"}</td>
                    <td class="text-center"><span class="badge badge-danger" style="font-size: 1rem;">${pendiente}</span></td>
                    <td class="text-center">${estadoBadge}</td>
                    <td class="text-center">
                        <button type="button" class="btn btn-success btn-sm btn-registrar-devolucion" ${pendiente <= 0 ? 'disabled' : ''}>
                            <i class="fas fa-undo-alt"></i> Registrar Devolucion
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            // Aplicar paginaciรณn inicial
            const lengthSelect = document.getElementById("envases-length");
            if (lengthSelect) {
                const pageLength = parseInt(lengthSelect.value);
                const rows = document.querySelectorAll("#cuerpo-tabla-envases tr");
                rows.forEach((row, index) => {
                    if (index >= pageLength) {
                        row.style.display = "none";
                    }
                });
            }
        })
        .catch(err => {
            console.error("Error cargando deudores:", err);
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center text-danger">
                        <i class="fas fa-exclamation-triangle"></i> Error al cargar los deudores
                    </td>
                </tr>
            `;
        });
}