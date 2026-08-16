(function () {
    let estadoCaja = null;
    let saldoSeleccionado = 0;

    const formatoMoneda = new Intl.NumberFormat('es-PE', {
        style: 'currency', currency: 'PEN', minimumFractionDigits: 2
    });

    async function requestJson(url, options) {
        const response = await fetch(url, options);
        const data = await response.json().catch(() => ({}));
        if (response.status === 401) {
            window.location.assign('/login');
            throw Object.assign(new Error('Sesión expirada'), { status: 401 });
        }
        if (!response.ok) {
            throw Object.assign(new Error(data.message || 'No se pudo completar la operación'), { status: response.status });
        }
        return data;
    }

    function mostrarEstado(estado) {
        const contenedor = document.getElementById('caja-estado-contenido');
        if (!contenedor) return;
        const abierta = Boolean(estado.sesionAbierta);
        const activa = estado.activa === true;
        contenedor.innerHTML = `
            <div class="d-flex align-items-center justify-content-between flex-wrap">
                <div>
                    <strong>${estado.codigoCaja || 'Caja principal'}</strong>
                    <span class="badge badge-${abierta ? 'success' : 'secondary'} ml-2">${abierta ? 'ABIERTA' : 'CERRADA'}</span>
                </div>
                <span class="text-muted small">${activa ? 'Caja activa' : 'Caja inactiva'}</span>
            </div>
            ${!activa
                ? '<div class="alert alert-danger mb-0 mt-3">La Caja principal está inactiva.</div>'
                : !abierta
                    ? '<div class="alert alert-warning mb-0 mt-3">Debes abrir Caja antes de recibir efectivo del motorizado.</div>'
                    : ''}`;
    }

    function escaparHtml(valor) {
        return String(valor ?? '').replace(/[&<>'"]/g, caracter => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
        }[caracter]));
    }

    function mostrarCustodias(custodias) {
        const body = document.getElementById('custodias-pendientes-body');
        if (!body) return;
        if (!custodias.length) {
            body.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No hay efectivo pendiente de motorizados.</td></tr>';
            return;
        }
        const cajaOperativa = Boolean(estadoCaja
            && estadoCaja.activa === true
            && estadoCaja.sesionAbierta === true);
        body.innerHTML = custodias.map(custodia => `
            <tr>
                <td>${escaparHtml(custodia.nombreEmpleado)}</td>
                <td><span class="badge badge-${custodia.estadoEmpleado === 1 ? 'success' : 'light border'}">${custodia.estadoEmpleado === 1 ? 'Activo' : 'Inactivo'}</span></td>
                <td class="text-right font-weight-bold">${formatoMoneda.format(Number(custodia.saldoPendiente))}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-success btn-liquidar-custodia"
                        data-id="${custodia.empleadoId}"
                        data-nombre="${escaparHtml(custodia.nombreEmpleado)}"
                        data-saldo="${custodia.saldoPendiente}"
                        ${cajaOperativa ? '' : 'disabled title="La Caja no está operativa"'}>
                        Liquidar
                    </button>
                </td>
            </tr>`).join('');
        document.querySelectorAll('.btn-liquidar-custodia').forEach(boton => {
            boton.addEventListener('click', () => abrirModal(boton));
        });
    }

    function abrirModal(boton) {
        saldoSeleccionado = Number(boton.dataset.saldo);
        document.getElementById('liquidacion-empleado-id').value = boton.dataset.id;
        document.getElementById('liquidacion-empleado-nombre').value = boton.dataset.nombre;
        document.getElementById('liquidacion-saldo-actual').value = formatoMoneda.format(saldoSeleccionado);
        document.getElementById('liquidacion-monto').value = saldoSeleccionado.toFixed(2);
        document.getElementById('liquidacion-observacion').value = '';
        $('#modal-liquidacion-custodia').modal('show');
    }

    async function recargar() {
        try {
            const [estado, custodias] = await Promise.all([
                requestJson('/api/caja/estado'),
                requestJson('/api/caja/custodias')
            ]);
            estadoCaja = estado;
            mostrarEstado(estado);
            mostrarCustodias(custodias);
        } catch (error) {
            if (error.status === 403) {
                Swal.fire('Acceso denegado', error.message, 'error');
                return;
            }
            if (error.status !== 401) {
                Swal.fire('Error', error.message || 'No se pudo cargar Caja', 'error');
            }
        }
    }

    function montoValido(monto) {
        return Number.isFinite(monto) && monto > 0 && monto <= saldoSeleccionado
            && /^\d+(\.\d{1,2})?$/.test(document.getElementById('liquidacion-monto').value.trim());
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('btn-liquidar-todo').addEventListener('click', () => {
            document.getElementById('liquidacion-monto').value = saldoSeleccionado.toFixed(2);
        });

        document.getElementById('btn-confirmar-liquidacion').addEventListener('click', async () => {
            const montoInput = document.getElementById('liquidacion-monto');
            const monto = Number(montoInput.value);
            if (!montoValido(monto)) {
                Swal.fire('Atención', 'El monto debe ser mayor a cero, no superar el saldo y tener máximo dos decimales.', 'warning');
                return;
            }
            try {
                const resultado = await requestJson('/api/caja/custodias/liquidar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        empleadoCustodioId: Number(document.getElementById('liquidacion-empleado-id').value),
                        monto: montoInput.value,
                        observacion: document.getElementById('liquidacion-observacion').value.trim() || null
                    })
                });
                $('#modal-liquidacion-custodia').modal('hide');
                await Swal.fire('Liquidación registrada',
                    `Monto: ${formatoMoneda.format(Number(resultado.montoLiquidado))}<br>Saldo restante: ${formatoMoneda.format(Number(resultado.saldoPendiente))}<br>Referencia: ${escaparHtml(resultado.referencia)}`,
                    'success');
                recargar();
            } catch (error) {
                if (error.status === 403) {
                    Swal.fire('Acceso denegado', error.message, 'error');
                } else if (error.status === 409) {
                    Swal.fire('Caja actualizada', error.message, 'warning');
                    recargar();
                } else if (error.status !== 401) {
                    Swal.fire('Error', error.message || 'No se pudo registrar la liquidación', 'error');
                }
            }
        });

        recargar();
    });
}());
