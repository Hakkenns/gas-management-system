window.AppModules = window.AppModules || {};

(function () {
    var estadoEnvasesMaestro = {
        initialized: false, root: null, lifecycleId: 0, clickHandler: null,
        form: null, submitHandler: null, modal: null, campos: null,
        listAbortController: null, listRequestId: 0,
        editAbortController: null, editRequestId: 0
    };
    var apiUrl = '/envases/api';

    function activo(lifecycleId) {
        return estadoEnvasesMaestro.initialized
            && estadoEnvasesMaestro.lifecycleId === lifecycleId
            && estadoEnvasesMaestro.root
            && estadoEnvasesMaestro.root.isConnected;
    }

    function moneda(valor) {
        return 'S/ ' + Number(valor || 0).toFixed(2);
    }

    function escaparHtml(valor) {
        return String(valor == null ? '' : valor).replace(/[&<>'"]/g, function (caracter) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[caracter];
        });
    }

    function abrirNuevo() {
        var campos = estadoEnvasesMaestro.campos;
        estadoEnvasesMaestro.form.reset();
        campos.id.value = '';
        campos.stockInicial.value = 0;
        estadoEnvasesMaestro.root.querySelector('#titulo-modal-envase').textContent = 'Nuevo Envase';
        if (estadoEnvasesMaestro.modal) estadoEnvasesMaestro.modal.modal('show');
    }

    function abrirEdicion(envase, lifecycleId) {
        if (!activo(lifecycleId)) return;
        var campos = estadoEnvasesMaestro.campos;
        campos.id.value = envase.id;
        campos.nombre.value = envase.nombre || '';
        campos.capacidad.value = envase.capacidad == null ? '' : envase.capacidad;
        campos.unidadMedida.value = envase.unidadMedida || '';
        campos.precioEnvase.value = envase.precioEnvase == null ? '' : envase.precioEnvase;
        campos.stockInicial.value = envase.stockInicial == null ? 0 : envase.stockInicial;
        campos.descripcion.value = envase.descripcion || '';
        estadoEnvasesMaestro.root.querySelector('#titulo-modal-envase').textContent = 'Editar Envase';
        if (estadoEnvasesMaestro.modal) estadoEnvasesMaestro.modal.modal('show');
    }

    function cargarEnvases() {
        if (!estadoEnvasesMaestro.initialized) return Promise.resolve(false);
        if (estadoEnvasesMaestro.listAbortController) estadoEnvasesMaestro.listAbortController.abort();
        var lifecycleId = estadoEnvasesMaestro.lifecycleId;
        var requestId = ++estadoEnvasesMaestro.listRequestId;
        var controller = new AbortController();
        estadoEnvasesMaestro.listAbortController = controller;
        return fetch(apiUrl, { signal: controller.signal })
            .then(function (response) {
                if (!response.ok) throw new Error('No se pudieron cargar los envases');
                return response.json();
            })
            .then(function (envases) {
                if (!activo(lifecycleId) || requestId !== estadoEnvasesMaestro.listRequestId) return false;
                var filas = envases.map(function (envase) {
                    return '<tr>'
                        + '<td>' + envase.id + '</td>'
                        + '<td>' + escaparHtml(envase.nombre) + '</td>'
                        + '<td>' + envase.capacidad + '</td>'
                        + '<td>' + escaparHtml(envase.unidadMedida) + '</td>'
                        + '<td>' + moneda(envase.precioEnvase) + '</td>'
                        + '<td>' + envase.stockInicial + '</td>'
                        + '<td><span class="badge badge-success">ACTIVO</span></td>'
                        + '<td class="text-nowrap">'
                        + '<button type="button" class="btn btn-warning btn-sm btn-editar-envase" data-id="' + envase.id + '"><i class="fas fa-edit"></i></button> '
                        + '<button type="button" class="btn btn-danger btn-sm btn-eliminar-envase" data-id="' + envase.id + '"><i class="fas fa-trash"></i></button>'
                        + '</td></tr>';
                }).join('');
                estadoEnvasesMaestro.root.querySelector('#cuerpo-tabla-envases-maestro').innerHTML = filas
                    || '<tr><td colspan="8" class="text-center text-muted">No hay envases registrados.</td></tr>';
                return true;
            })
            .catch(function (error) {
                if (error.name === 'AbortError') return false;
                if (activo(lifecycleId) && requestId === estadoEnvasesMaestro.listRequestId) {
                    estadoEnvasesMaestro.root.querySelector('#cuerpo-tabla-envases-maestro').innerHTML = '<tr><td colspan="8" class="text-center text-danger">No se pudo cargar el catálogo de envases.</td></tr>';
                    console.error(error);
                }
                return false;
            })
            .finally(function () {
                if (requestId === estadoEnvasesMaestro.listRequestId) estadoEnvasesMaestro.listAbortController = null;
            });
    }

    function editarEnvase(id, lifecycleId) {
        if (estadoEnvasesMaestro.editAbortController) estadoEnvasesMaestro.editAbortController.abort();
        var requestId = ++estadoEnvasesMaestro.editRequestId;
        var controller = new AbortController();
        estadoEnvasesMaestro.editAbortController = controller;
        fetch(apiUrl + '/' + id, { signal: controller.signal })
            .then(function (response) {
                if (!response.ok) return null;
                return response.json();
            })
            .then(function (envase) {
                if (envase && activo(lifecycleId) && requestId === estadoEnvasesMaestro.editRequestId) abrirEdicion(envase, lifecycleId);
            })
            .catch(function (error) {
                if (error.name !== 'AbortError' && activo(lifecycleId) && requestId === estadoEnvasesMaestro.editRequestId) console.error(error);
            })
            .finally(function () {
                if (requestId === estadoEnvasesMaestro.editRequestId) estadoEnvasesMaestro.editAbortController = null;
            });
    }

    function manejarClick(event) {
        var botonNuevo = event.target.closest('#btn-nuevo-envase');
        if (botonNuevo) { event.preventDefault(); abrirNuevo(); return; }
        var editar = event.target.closest('.btn-editar-envase');
        if (editar) { editarEnvase(editar.dataset.id, estadoEnvasesMaestro.lifecycleId); return; }
        var eliminar = event.target.closest('.btn-eliminar-envase');
        if (eliminar) eliminarEnvase(eliminar.dataset.id, estadoEnvasesMaestro.lifecycleId);
    }

    function eliminarEnvase(id, lifecycleId) {
        if (!window.confirm('¿Desea desactivar este envase?')) return;
        fetch(apiUrl + '/' + id, { method: 'DELETE' })
            .then(function (response) {
                if (response.ok) {
                    if (activo(lifecycleId)) return cargarEnvases();
                    return false;
                }
                if (activo(lifecycleId)) window.alert('No se pudo desactivar el envase.');
                return false;
            })
            .catch(function (error) { if (activo(lifecycleId)) console.error(error); });
    }

    function manejarSubmit(event) {
        event.preventDefault();
        var lifecycleId = estadoEnvasesMaestro.lifecycleId;
        var campos = estadoEnvasesMaestro.campos;
        var id = campos.id.value;
        var envase = {
            nombre: campos.nombre.value.trim(),
            capacidad: Number(campos.capacidad.value),
            unidadMedida: campos.unidadMedida.value.trim(),
            precioEnvase: Number(campos.precioEnvase.value),
            stockInicial: Number(campos.stockInicial.value),
            descripcion: campos.descripcion.value.trim() || null,
            estado: true
        };
        fetch(id ? apiUrl + '/' + id : apiUrl, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(envase)
        })
            .then(function (response) {
                if (!response.ok) throw new Error('No se pudo guardar el envase.');
                return response;
            })
            .then(function () {
                if (!activo(lifecycleId)) return;
                if (estadoEnvasesMaestro.modal) estadoEnvasesMaestro.modal.modal('hide');
                cargarEnvases();
            })
            .catch(function (error) { if (activo(lifecycleId)) window.alert(error.message || 'No se pudo guardar el envase.'); });
    }

    function destroyEnvasesMaestro() {
        if (!estadoEnvasesMaestro.initialized) return;
        estadoEnvasesMaestro.root.removeEventListener('click', estadoEnvasesMaestro.clickHandler);
        if (estadoEnvasesMaestro.form) estadoEnvasesMaestro.form.removeEventListener('submit', estadoEnvasesMaestro.submitHandler);
        if (estadoEnvasesMaestro.listAbortController) estadoEnvasesMaestro.listAbortController.abort();
        if (estadoEnvasesMaestro.editAbortController) estadoEnvasesMaestro.editAbortController.abort();
        estadoEnvasesMaestro.listAbortController = null;
        estadoEnvasesMaestro.editAbortController = null;
        estadoEnvasesMaestro.listRequestId += 1;
        estadoEnvasesMaestro.editRequestId += 1;
        if (estadoEnvasesMaestro.modal) estadoEnvasesMaestro.modal.modal('hide');
        estadoEnvasesMaestro.lifecycleId += 1;
        estadoEnvasesMaestro.root = null;
        estadoEnvasesMaestro.clickHandler = null;
        estadoEnvasesMaestro.form = null;
        estadoEnvasesMaestro.submitHandler = null;
        estadoEnvasesMaestro.modal = null;
        estadoEnvasesMaestro.campos = null;
        estadoEnvasesMaestro.initialized = false;
    }

    function initEnvasesMaestro() {
        if (estadoEnvasesMaestro.initialized) return;
        estadoEnvasesMaestro.root = document.querySelector('[data-modulo="envasesMaestro"]');
        if (!estadoEnvasesMaestro.root) return;
        estadoEnvasesMaestro.lifecycleId += 1;
        estadoEnvasesMaestro.form = estadoEnvasesMaestro.root.querySelector('#form-envase-maestro');
        estadoEnvasesMaestro.modal = window.jQuery ? window.jQuery(estadoEnvasesMaestro.root.querySelector('#modal-envase-maestro')) : null;
        estadoEnvasesMaestro.campos = {
            id: estadoEnvasesMaestro.root.querySelector('#envase-id'),
            nombre: estadoEnvasesMaestro.root.querySelector('#envase-nombre'),
            capacidad: estadoEnvasesMaestro.root.querySelector('#envase-capacidad'),
            unidadMedida: estadoEnvasesMaestro.root.querySelector('#envase-unidad'),
            precioEnvase: estadoEnvasesMaestro.root.querySelector('#envase-precio-envase'),
            stockInicial: estadoEnvasesMaestro.root.querySelector('#envase-stock-inicial'),
            descripcion: estadoEnvasesMaestro.root.querySelector('#envase-descripcion')
        };
        estadoEnvasesMaestro.clickHandler = manejarClick;
        estadoEnvasesMaestro.root.addEventListener('click', estadoEnvasesMaestro.clickHandler);
        estadoEnvasesMaestro.submitHandler = manejarSubmit;
        estadoEnvasesMaestro.form.addEventListener('submit', estadoEnvasesMaestro.submitHandler);
        estadoEnvasesMaestro.initialized = true;
        cargarEnvases();
    }

    window.AppModules.envasesMaestro = {
        init: initEnvasesMaestro,
        destroy: destroyEnvasesMaestro,
        reload: cargarEnvases
    };
}());
