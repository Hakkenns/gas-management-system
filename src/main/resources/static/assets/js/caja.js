(function () {
    let estadoCaja = null;
    let saldoSeleccionado = 0;
    let efectivoEsperadoCierre = 0;

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
        const informacionSesion = abierta
            ? `<div class="mt-3 small">
                    <div><span class="text-muted">Sesión:</span> #${estado.idSesionCaja}</div>
                    <div><span class="text-muted">Apertura:</span> ${formatearFecha(estado.fechaHoraApertura)}</div>
                    <div><span class="text-muted">Efectivo esperado:</span> <strong>${formatoMoneda.format(Number(estado.efectivoEsperado || 0))}</strong></div>
                </div>`
            : '';
        const accion = !activa
            ? ''
            : abierta
                ? `<button type="button" class="btn btn-danger btn-cerrar-caja mt-3"><i class="fas fa-lock mr-1"></i>Cerrar Caja</button>`
                : `<button type="button" class="btn btn-success btn-abrir-caja mt-3"><i class="fas fa-lock-open mr-1"></i>Abrir Caja</button>`;
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
                    : ''}
            ${informacionSesion}
            ${accion}`;
        const botonAbrir = contenedor.querySelector('.btn-abrir-caja');
        if (botonAbrir) botonAbrir.addEventListener('click', abrirModalApertura);
        const botonCerrar = contenedor.querySelector('.btn-cerrar-caja');
        if (botonCerrar) botonCerrar.addEventListener('click', abrirModalCierre);
    }

    function formatearFecha(valor) {
        if (!valor) return '—';
        const fecha = new Date(valor);
        if (Number.isNaN(fecha.getTime())) return valor;
        return new Intl.DateTimeFormat('es-PE', {
            dateStyle: 'short', timeStyle: 'short'
        }).format(fecha);
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

    function abrirModalApertura() {
        document.getElementById('apertura-monto-inicial').value = '';
        document.getElementById('apertura-observaciones').value = '';
        $('#modal-abrir-caja').modal('show');
    }

    function abrirModalCierre() {
        efectivoEsperadoCierre = Number(estadoCaja?.efectivoEsperado || 0);
        document.getElementById('cierre-efectivo-esperado').value = formatoMoneda.format(efectivoEsperadoCierre);
        document.getElementById('cierre-monto-declarado').value = '';
        document.getElementById('cierre-observaciones').value = '';
        actualizarDiferenciaCierre();
        $('#modal-cerrar-caja').modal('show');
    }

    async function refrescarEstadoCierre() {
        const estado = await requestJson('/api/caja/estado');
        estadoCaja = estado;
        mostrarEstado(estado);
        efectivoEsperadoCierre = Number(estado.efectivoEsperado || 0);
        document.getElementById('cierre-efectivo-esperado').value = formatoMoneda.format(efectivoEsperadoCierre);
        actualizarDiferenciaCierre();
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

    function montoLiquidacionValido(monto) {
        return Number.isFinite(monto) && monto > 0 && monto <= saldoSeleccionado
            && /^\d+(\.\d{1,2})?$/.test(document.getElementById('liquidacion-monto').value.trim());
    }

    function montoCajaValido(valor) {
        return /^\d+(\.\d{1,2})?$/.test(valor) && Number(valor) >= 0;
    }

    function calcularCentavos(valor) {
        if (!montoCajaValido(valor)) return null;
        const [entero, decimal = ''] = valor.split('.');
        return Number(entero) * 100 + Number(`${decimal}00`.slice(0, 2));
    }

    function actualizarDiferenciaCierre() {
        const valor = document.getElementById('cierre-monto-declarado').value.trim();
        const diferencia = document.getElementById('cierre-diferencia');
        const aviso = document.getElementById('cierre-observacion-obligatoria');
        const etiqueta = document.getElementById('cierre-observaciones-etiqueta');
        const centavosContados = calcularCentavos(valor);
        if (centavosContados === null) {
            diferencia.textContent = '—';
            diferencia.classList.remove('text-danger');
            aviso.classList.add('d-none');
            etiqueta.textContent = '(opcional)';
            return null;
        }
        const diferenciaCentavos = centavosContados - Math.round(efectivoEsperadoCierre * 100);
        const hayDiferencia = diferenciaCentavos !== 0;
        diferencia.textContent = formatoMoneda.format(diferenciaCentavos / 100);
        diferencia.classList.toggle('text-danger', hayDiferencia);
        aviso.classList.toggle('d-none', !hayDiferencia);
        etiqueta.textContent = hayDiferencia ? '(obligatorias)' : '(opcional)';
        return diferenciaCentavos;
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('btn-liquidar-todo').addEventListener('click', () => {
            document.getElementById('liquidacion-monto').value = saldoSeleccionado.toFixed(2);
        });

        document.getElementById('btn-confirmar-liquidacion').addEventListener('click', async () => {
            const montoInput = document.getElementById('liquidacion-monto');
            const monto = Number(montoInput.value);
            if (!montoLiquidacionValido(monto)) {
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

        document.getElementById('btn-confirmar-apertura').addEventListener('click', async event => {
            const boton = event.currentTarget;
            const montoInput = document.getElementById('apertura-monto-inicial');
            const montoInicial = montoInput.value.trim();
            if (!montoCajaValido(montoInicial)) {
                Swal.fire('Atención', 'El monto inicial es obligatorio, no puede ser negativo y debe tener máximo dos decimales.', 'warning');
                return;
            }
            boton.disabled = true;
            try {
                const resultado = await requestJson('/api/caja/abrir', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        montoInicial,
                        observaciones: document.getElementById('apertura-observaciones').value.trim() || null
                    })
                });
                $('#modal-abrir-caja').modal('hide');
                await Swal.fire('Apertura registrada',
                    `Monto inicial: ${formatoMoneda.format(Number(resultado.montoInicial))}<br>Fecha: ${formatearFecha(resultado.fechaHoraApertura)}`,
                    'success');
                recargar();
            } catch (error) {
                if (error.status === 403) {
                    Swal.fire('Acceso denegado', error.message, 'error');
                } else if (error.status === 409) {
                    Swal.fire('Caja actualizada', error.message, 'warning');
                    recargar();
                } else if (error.status !== 401) {
                    Swal.fire('Error', error.message || 'No se pudo abrir Caja', 'error');
                }
            } finally {
                boton.disabled = false;
            }
        });

        document.getElementById('cierre-monto-declarado').addEventListener('input', actualizarDiferenciaCierre);

        document.getElementById('btn-confirmar-cierre').addEventListener('click', async event => {
            const boton = event.currentTarget;
            const montoInput = document.getElementById('cierre-monto-declarado');
            const montoDeclarado = montoInput.value.trim();
            if (!montoCajaValido(montoDeclarado)) {
                Swal.fire('Atención', 'El efectivo contado es obligatorio, no puede ser negativo y debe tener máximo dos decimales.', 'warning');
                return;
            }
            const diferenciaCentavos = actualizarDiferenciaCierre();
            const observaciones = document.getElementById('cierre-observaciones').value.trim();
            if (diferenciaCentavos !== 0 && !observaciones) {
                Swal.fire('Atención', 'La observación es obligatoria cuando existe diferencia.', 'warning');
                return;
            }
            boton.disabled = true;
            try {
                const resultado = await requestJson('/api/caja/cerrar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        montoDeclarado,
                        observaciones: observaciones || null
                    })
                });
                $('#modal-cerrar-caja').modal('hide');
                await Swal.fire('Caja cerrada',
                    `Efectivo esperado: ${formatoMoneda.format(Number(resultado.montoEsperado))}<br>Efectivo contado: ${formatoMoneda.format(Number(resultado.montoDeclarado))}<br>Diferencia: ${formatoMoneda.format(Number(resultado.diferencia))}`,
                    'success');
                recargar();
            } catch (error) {
                if (error.status === 403) {
                    Swal.fire('Acceso denegado', error.message, 'error');
                } else if (error.status === 409) {
                    Swal.fire('Caja actualizada', error.message, 'warning');
                    recargar();
                } else if (error.status === 400) {
                    try {
                        await refrescarEstadoCierre();
                        Swal.fire('Cierre actualizado', error.message, 'warning');
                    } catch (refreshError) {
                        if (refreshError.status !== 401) {
                            Swal.fire('No se pudo cerrar Caja', error.message, 'error');
                        }
                    }
                } else if (error.status !== 401) {
                    Swal.fire('Error', error.message || 'No se pudo cerrar Caja', 'error');
                }
            } finally {
                boton.disabled = false;
            }
        });

        recargar();
    });
}());
