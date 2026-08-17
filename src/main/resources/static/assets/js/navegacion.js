(function () {
    const modulos = {
        clientes: {
            ruta: '/clientes',
            fragmento: '/clientes/fragment',
            script: '/assets/js/clientes.js',
            titulo: 'Gestion de Clientes'
        }
    };
    const promesasScripts = {};
    const origenesSeguros = new Set(['/', '/dashboard', '/clientes']);

    function moduloActual() {
        const contenedor = document.getElementById('contenido-principal');
        return contenedor ? contenedor.querySelector('[data-modulo]') : null;
    }

    function mostrarError(titulo, mensaje) {
        if (window.Swal) {
            window.Swal.fire(titulo, mensaje, 'error');
        }
    }

    function limpiarModalesDelContenido() {
        const contenedor = document.getElementById('contenido-principal');
        if (!contenedor) return;

        const modales = Array.from(contenedor.querySelectorAll('.modal.show'));
        modales.forEach(modal => {
            if (window.jQuery && window.jQuery.fn.modal) {
                window.jQuery(modal).modal('hide');
            } else {
                modal.classList.remove('show');
                modal.style.display = 'none';
            }
            modal.classList.remove('show');
        });

        const modalFueraDelContenido = Array.from(document.querySelectorAll('.modal.show'))
            .some(modal => !contenedor.contains(modal));
        if (!modalFueraDelContenido) {
            document.querySelectorAll('.modal-backdrop').forEach(backdrop => backdrop.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('padding-right');
            document.body.style.removeProperty('overflow');
        }
    }

    function actualizarSidebar(ruta) {
        const rutaActiva = ruta === '/' ? '/dashboard' : ruta;
        document.querySelectorAll('.main-sidebar a.nav-link').forEach(enlace => {
            const href = enlace.getAttribute('href');
            let esActivo = false;
            try {
                esActivo = href && new URL(href, window.location.origin).pathname === rutaActiva;
            } catch (e) {
                esActivo = false;
            }
            enlace.classList.toggle('active', esActivo);
            if (esActivo) {
                enlace.setAttribute('aria-current', 'page');
                const grupo = enlace.closest('.nav-treeview');
                if (grupo) {
                    const padre = grupo.closest('.has-treeview');
                    if (padre) padre.classList.add('menu-open');
                }
            } else {
                enlace.removeAttribute('aria-current');
            }
        });
    }

    function cargarScript(config) {
        if (window.AppModules && window.AppModules[config.modulo || 'clientes']) {
            return Promise.resolve();
        }
        if (promesasScripts[config.script]) return promesasScripts[config.script];

        promesasScripts[config.script] = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = config.script;
            script.async = false;
            script.onload = resolve;
            script.onerror = () => reject(new Error('No se pudo cargar el modulo de Clientes'));
            document.head.appendChild(script);
        });
        return promesasScripts[config.script];
    }

    async function obtenerFragmento(config) {
        const response = await fetch(config.fragmento, {
            credentials: 'same-origin',
            headers: {
                'X-Requested-With': 'fetch',
                'Accept': 'text/html'
            }
        });

        const urlFinal = new URL(response.url, window.location.origin);
        if (urlFinal.pathname === '/login') {
            window.location.assign('/login');
            throw Object.assign(new Error('Sesion expirada'), { status: 401 });
        }
        if (response.status === 401) {
            window.location.assign('/login');
            throw Object.assign(new Error('Sesion expirada'), { status: 401 });
        }
        if (!response.ok) {
            throw Object.assign(new Error(response.status === 403
                ? 'No tienes permiso para acceder a Clientes.'
                : 'No se pudo cargar Clientes.'), { status: response.status });
        }

        const html = await response.text();
        const documento = new DOMParser().parseFromString(html, 'text/html');
        if (!documento.querySelector('[data-modulo="clientes"]')) {
            throw Object.assign(new Error('La respuesta no contiene el fragmento de Clientes.'), { status: 500 });
        }
        return documento.body.innerHTML;
    }

    async function cargarClientes(pushState) {
        const config = modulos.clientes;
        let html;
        try {
            html = await obtenerFragmento(config);
            await cargarScript({ ...config, modulo: 'clientes' });
        } catch (error) {
            if (error.status === 401) return;
            if (error.status === 403) {
                mostrarError('Acceso denegado', error.message);
            } else {
                mostrarError('Error de navegacion', error.message || 'No se pudo cargar Clientes.');
            }
            return;
        }

        const contenedor = document.getElementById('contenido-principal');
        if (!contenedor) return;

        const anterior = moduloActual();
        const modulo = anterior && window.AppModules ? window.AppModules[anterior.dataset.modulo] : null;
        if (modulo && typeof modulo.destroy === 'function') modulo.destroy();
        limpiarModalesDelContenido();
        contenedor.innerHTML = html;

        const clientes = window.AppModules && window.AppModules.clientes;
        if (clientes && typeof clientes.init === 'function') clientes.init();
        actualizarSidebar(config.ruta);
        document.title = config.titulo;
        if (pushState && window.location.pathname !== config.ruta) {
            window.history.pushState({ modulo: 'clientes' }, '', config.ruta);
        }
    }

    function esEnlaceClientesSeguro(event, enlace) {
        if (event.button !== 0 || event.ctrlKey || event.shiftKey || event.altKey || event.metaKey) return false;
        if (enlace.target && enlace.target !== '_self') return false;
        if (enlace.hasAttribute('download')) return false;
        const url = new URL(enlace.href, window.location.origin);
        return url.origin === window.location.origin
            && url.pathname === modulos.clientes.ruta
            && origenesSeguros.has(window.location.pathname);
    }

    function inicializarNavegacion() {
        document.addEventListener('click', event => {
            const enlace = event.target instanceof Element ? event.target.closest('a') : null;
            if (!enlace || !esEnlaceClientesSeguro(event, enlace)) return;
            event.preventDefault();
            cargarClientes(true);
        });

        window.addEventListener('popstate', () => {
            if (window.location.pathname === modulos.clientes.ruta) {
                cargarClientes(false);
            } else {
                window.location.assign(window.location.href);
            }
        });

        const inicial = moduloActual();
        if (inicial && inicial.dataset.modulo === 'clientes') {
            const clientes = window.AppModules && window.AppModules.clientes;
            if (clientes && typeof clientes.init === 'function') clientes.init();
            document.title = inicial.dataset.pageTitle || modulos.clientes.titulo;
        }
        actualizarSidebar(window.location.pathname);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', inicializarNavegacion, { once: true });
    } else {
        inicializarNavegacion();
    }
}());
