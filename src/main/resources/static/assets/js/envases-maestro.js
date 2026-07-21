document.addEventListener('DOMContentLoaded', function () {
    const apiUrl = '/envases/api';
    const tbody = document.getElementById('cuerpo-tabla-envases-maestro');
    const form = document.getElementById('form-envase-maestro');
    const modal = window.jQuery ? window.jQuery('#modal-envase-maestro') : null;

    const campos = {
        id: document.getElementById('envase-id'),
        nombre: document.getElementById('envase-nombre'),
        capacidad: document.getElementById('envase-capacidad'),
        unidadMedida: document.getElementById('envase-unidad'),
        precioEnvase: document.getElementById('envase-precio-envase'),
        stockInicial: document.getElementById('envase-stock-inicial'),
        descripcion: document.getElementById('envase-descripcion')
    };

    const moneda = valor => `S/ ${Number(valor || 0).toFixed(2)}`;
    const escaparHtml = valor => String(valor ?? '').replace(/[&<>'"]/g, caracter => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[caracter]));

    function abrirNuevo() {
        form.reset();
        campos.id.value = '';
        campos.stockInicial.value = 0;
        document.getElementById('titulo-modal-envase').textContent = 'Nuevo Envase';
        modal.modal('show');
    }

    function abrirEdicion(envase) {
        campos.id.value = envase.id;
        campos.nombre.value = envase.nombre || '';
        campos.capacidad.value = envase.capacidad ?? '';
        campos.unidadMedida.value = envase.unidadMedida || '';
        campos.precioEnvase.value = envase.precioEnvase ?? '';
        campos.stockInicial.value = envase.stockInicial ?? 0;
        campos.descripcion.value = envase.descripcion || '';
        document.getElementById('titulo-modal-envase').textContent = 'Editar Envase';
        modal.modal('show');
    }

    async function cargarEnvases() {
        try {
            const response = await fetch(apiUrl);
            if (!response.ok) throw new Error('No se pudieron cargar los envases');
            const envases = await response.json();
            tbody.innerHTML = envases.map(envase => `
                <tr>
                    <td>${envase.id}</td>
                    <td>${escaparHtml(envase.nombre)}</td>
                    <td>${envase.capacidad}</td>
                    <td>${escaparHtml(envase.unidadMedida)}</td>
                    <td>${moneda(envase.precioEnvase)}</td>
                    <td>${envase.stockInicial}</td>
                    <td><span class="badge badge-success">ACTIVO</span></td>
                    <td class="text-nowrap">
                        <button class="btn btn-warning btn-sm btn-editar-envase" data-id="${envase.id}"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger btn-sm btn-eliminar-envase" data-id="${envase.id}"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>`).join('') || '<tr><td colspan="8" class="text-center text-muted">No hay envases registrados.</td></tr>';
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger">No se pudo cargar el catálogo de envases.</td></tr>';
            console.error(error);
        }
    }

    document.getElementById('btn-nuevo-envase').addEventListener('click', abrirNuevo);

    tbody.addEventListener('click', async function (event) {
        const botonEditar = event.target.closest('.btn-editar-envase');
        const botonEliminar = event.target.closest('.btn-eliminar-envase');

        if (botonEditar) {
            const response = await fetch(`${apiUrl}/${botonEditar.dataset.id}`);
            if (response.ok) abrirEdicion(await response.json());
        }

        if (botonEliminar && confirm('¿Desea desactivar este envase?')) {
            const response = await fetch(`${apiUrl}/${botonEliminar.dataset.id}`, { method: 'DELETE' });
            if (response.ok) cargarEnvases();
            else alert('No se pudo desactivar el envase.');
        }
    });

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        const id = campos.id.value;
        const envase = {
            nombre: campos.nombre.value.trim(),
            capacidad: Number(campos.capacidad.value),
            unidadMedida: campos.unidadMedida.value.trim(),
            precioEnvase: Number(campos.precioEnvase.value),
            stockInicial: Number(campos.stockInicial.value),
            descripcion: campos.descripcion.value.trim() || null,
            estado: true
        };

        const response = await fetch(id ? `${apiUrl}/${id}` : apiUrl, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(envase)
        });

        if (!response.ok) {
            alert('No se pudo guardar el envase.');
            return;
        }

        modal.modal('hide');
        cargarEnvases();
    });

    cargarEnvases();
});
