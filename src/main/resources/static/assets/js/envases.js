const estadoDeudores = {
    lista: [],
    filtro: "TODOS",
    busqueda: "",
    limite: 10
};

document.addEventListener("DOMContentLoaded", function() {
    cargarDeudores();

    document.addEventListener("click", function(e) {
        const filtro = e.target.closest(".btn-filtro-deudores");
        if (filtro) {
            estadoDeudores.filtro = filtro.dataset.filtro;
            actualizarBotonesFiltro();
            renderizarDeudores();
            return;
        }

        const btn = e.target.closest(".btn-registrar-devolucion");
        if (!btn) return;

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
    });

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

    const searchInput = document.getElementById("envases-search");
    if (searchInput) {
        searchInput.addEventListener("input", function() {
            estadoDeudores.busqueda = this.value.toLowerCase().trim();
            renderizarDeudores();
        });
    }

    const lengthSelect = document.getElementById("envases-length");
    if (lengthSelect) {
        lengthSelect.addEventListener("change", function() {
            estadoDeudores.limite = parseInt(this.value) || 10;
            renderizarDeudores();
        });
    }
});

function cargarDeudores() {
    const tbody = document.getElementById("cuerpo-tabla-envases");
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="12" class="text-center text-muted">
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
            estadoDeudores.lista = Array.isArray(data) ? data : [];
            actualizarConteos();
            actualizarBotonesFiltro();
            renderizarDeudores();
        })
        .catch(err => {
            console.error("Error cargando deudores:", err);
            actualizarConteos();
            const mensaje = document.getElementById("cuerpo-tabla-envases");
            mensaje.innerHTML = `
                <tr>
                    <td colspan="12" class="text-center text-danger">
                        <i class="fas fa-exclamation-triangle"></i> Error al cargar los deudores
                    </td>
                </tr>
            `;
        });
}

function obtenerDeudoresFiltrados() {
    return estadoDeudores.lista.filter(item => {
        if (estadoDeudores.filtro === "VENCIDOS") return item.vencido === true;
        if (estadoDeudores.filtro === "VIGENTES") {
            return item.tipoPrestamo === "NORMAL"
                && item.vencido !== true
                && Boolean(item.fechaLimiteDevolucion);
        }
        if (estadoDeudores.filtro === "ESPECIALES") return item.tipoPrestamo === "ESPECIAL";
        return true;
    }).filter(item => {
        if (!estadoDeudores.busqueda) return true;
        return obtenerTextoBusqueda(item).includes(estadoDeudores.busqueda);
    });
}

function obtenerTextoBusqueda(item) {
    const estadoVisible = item.estado === "PRESTADO"
        ? "Pendiente"
        : item.estado === "PARCIAL" ? "Parcial" : item.estado || "";
    const tipoVisible = item.tipoPrestamo === "NORMAL"
        ? "Normal"
        : item.tipoPrestamo === "ESPECIAL" ? "Especial"
            : item.tipoPrestamo === "LEGADO" ? "Legado" : item.tipoPrestamo || "";

    let fechaLimiteVisible = formatearFecha(item.fechaLimiteDevolucion);
    if (!item.fechaLimiteDevolucion && item.tipoPrestamo === "ESPECIAL") {
        fechaLimiteVisible = "Sin fecha fija";
    } else if (!item.fechaLimiteDevolucion && item.tipoPrestamo === "LEGADO") {
        fechaLimiteVisible = "No registrada";
    }

    return [
        item.nombreCliente,
        item.telefonoCliente,
        item.direccionCliente,
        item.codigoPedido,
        item.productoNombre,
        item.estado,
        estadoVisible,
        item.tipoPrestamo,
        tipoVisible,
        item.vencido === true ? "Vencido" : "",
        formatearFecha(item.fechaEntrega),
        formatearFecha(item.fechaPrestamo),
        fechaLimiteVisible,
        item.cantidadPrestada,
        item.cantidadDevuelta,
        item.cantidadPendiente
    ].filter(Boolean).join(" ").toLowerCase();
}

function actualizarConteos() {
    const conteos = {
        "conteo-todos": estadoDeudores.lista.length,
        "conteo-vencidos": estadoDeudores.lista.filter(item => item.vencido === true).length,
        "conteo-vigentes": estadoDeudores.lista.filter(item =>
            item.tipoPrestamo === "NORMAL"
            && item.vencido !== true
            && Boolean(item.fechaLimiteDevolucion)
        ).length,
        "conteo-especiales": estadoDeudores.lista.filter(item => item.tipoPrestamo === "ESPECIAL").length,
        "conteo-legado": estadoDeudores.lista.filter(item => item.tipoPrestamo === "LEGADO").length
    };

    Object.entries(conteos).forEach(([id, valor]) => {
        const elemento = document.getElementById(id);
        if (elemento) elemento.textContent = valor;
    });
}

function actualizarBotonesFiltro() {
    document.querySelectorAll(".btn-filtro-deudores").forEach(btn => {
        const activo = btn.dataset.filtro === estadoDeudores.filtro;
        btn.classList.toggle("active", activo);
        btn.setAttribute("aria-pressed", activo ? "true" : "false");
    });
}

function renderizarDeudores() {
    const tbody = document.getElementById("cuerpo-tabla-envases");
    if (!tbody) return;

    const deudores = obtenerDeudoresFiltrados();
    if (deudores.length === 0) {
        const mensajes = {
            TODOS: "No hay clientes con envases pendientes.",
            VENCIDOS: "No hay préstamos vencidos.",
            VIGENTES: "No hay préstamos normales vigentes.",
            ESPECIALES: "No hay préstamos especiales pendientes."
        };
        tbody.innerHTML = `
            <tr>
                <td colspan="12" class="text-center text-success">
                    <i class="fas fa-check-circle"></i> ${mensajes[estadoDeudores.filtro]}
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = "";
    deudores.slice(0, estadoDeudores.limite).forEach(item => tbody.appendChild(crearFilaDeudor(item)));
}

function crearFilaDeudor(item) {
    const pendiente = item.cantidadPendiente || 0;
    let estadoBadge = "";
    if (item.estado === "PRESTADO") {
        estadoBadge = '<span class="badge badge-warning">Pendiente</span>';
    } else if (item.estado === "PARCIAL") {
        estadoBadge = '<span class="badge badge-info">Parcial</span>';
    } else {
        estadoBadge = '<span class="badge badge-secondary">' + item.estado + '</span>';
    }
    if (item.vencido) estadoBadge += ' <span class="badge badge-danger">Vencido</span>';

    let tipoPrestamo = item.tipoPrestamo || "-";
    if (tipoPrestamo === "NORMAL") tipoPrestamo = '<span class="badge badge-primary">Normal</span>';
    else if (tipoPrestamo === "ESPECIAL") tipoPrestamo = '<span class="badge badge-success">Especial</span>';
    else if (tipoPrestamo === "LEGADO") tipoPrestamo = '<span class="badge badge-secondary">Legado</span>';

    const fechaStr = formatearFecha(item.fechaEntrega);
    const fechaPrestamoStr = formatearFecha(item.fechaPrestamo);
    let fechaLimiteStr = formatearFecha(item.fechaLimiteDevolucion);
    if (!item.fechaLimiteDevolucion && item.tipoPrestamo === "ESPECIAL") fechaLimiteStr = "Sin fecha fija";
    else if (!item.fechaLimiteDevolucion && item.tipoPrestamo === "LEGADO") fechaLimiteStr = "No registrada";

    const tr = document.createElement("tr");
    tr.dataset.idControl = item.idControl;
    tr.dataset.clienteNombre = item.nombreCliente || "";
    tr.dataset.productoNombre = item.productoNombre || "";
    tr.dataset.cantidadPrestada = item.cantidadPrestada || 0;
    tr.dataset.cantidadDevuelta = item.cantidadDevuelta || 0;
    tr.innerHTML = `
        <td><strong>${item.nombreCliente || "-"}</strong></td>
        <td>${item.telefonoCliente || "-"}</td>
        <td>${item.direccionCliente || "-"}</td>
        <td>${item.codigoPedido || "-"}</td>
        <td>${fechaStr}</td>
        <td>${fechaPrestamoStr}</td>
        <td class="text-center">${tipoPrestamo}</td>
        <td class="text-center">${fechaLimiteStr}</td>
        <td>${item.productoNombre || "-"}</td>
        <td class="text-center"><span class="badge badge-danger" style="font-size: 1rem;">${pendiente}</span></td>
        <td class="text-center">${estadoBadge}</td>
        <td class="text-center">
            <button type="button" class="btn btn-success btn-sm btn-registrar-devolucion" ${pendiente <= 0 ? "disabled" : ""}>
                <i class="fas fa-undo-alt"></i> Registrar Devolucion
            </button>
        </td>
    `;
    return tr;
}

function formatearFecha(fecha) {
    if (!fecha) return "-";
    if (/^\d{4}-\d{2}-\d{2}$/.test(fecha)) {
        const [anio, mes, dia] = fecha.split("-");
        return `${dia}/${mes}/${anio}`;
    }
    const date = new Date(fecha);
    return Number.isNaN(date.getTime())
        ? "-"
        : date.toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit", year: "numeric" });
}
