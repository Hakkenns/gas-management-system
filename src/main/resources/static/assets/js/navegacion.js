(function () {
    const modulos = {
        clientes: {
            ruta: '/clientes',
            fragmento: '/clientes/fragment',
            script: '/assets/js/clientes.js',
            titulo: 'Gestion de Clientes',
            nombre: 'Clientes'
        },
        usuarios: {
            ruta: '/usuarios',
            fragmento: '/usuarios/fragment',
            script: '/assets/js/usuario.js',
            titulo: 'Gestion de Usuarios',
            nombre: 'Usuarios'
        },
        empleados: {
            ruta: '/empleados',
            fragmento: '/empleados/fragment',
            script: '/assets/js/empleados.js',
            titulo: 'Gestion de Empleados',
            nombre: 'Empleados'
        },
        motos: {
            ruta: '/motos',
            fragmento: '/motos/fragment',
            script: '/assets/js/motos.js',
            titulo: 'Gestion de Motos',
            nombre: 'Motos'
        },
        proveedores: {
            ruta: '/proveedores',
            fragmento: '/proveedores/fragment',
            script: '/assets/js/proveedores.js',
            titulo: 'Gestion de Proveedores',
            nombre: 'Proveedores'
        },
        rubros: {
            ruta: '/rubros',
            fragmento: '/rubros/fragment',
            script: '/assets/js/rubro.js',
            titulo: 'Gestion de Rubros',
            nombre: 'Rubros'
        },
        asignacionMotos: {
            ruta: '/asignacion_motos',
            fragmento: '/asignacion_motos/fragment',
            script: '/assets/js/asignacion_motos.js',
            titulo: 'Asignacion de Motos',
            nombre: 'Asignacion de Motos'
        },
        categorias: {
            ruta: '/categorias',
            fragmento: '/categorias/fragment',
            script: '/assets/js/categoria.js',
            titulo: 'Gestion de Categorias',
            nombre: 'Tipos de productos'
        },
        envasesMaestro: {
            ruta: '/envases/vista',
            fragmento: '/envases/vista/fragment',
            script: '/assets/js/envases-maestro.js',
            titulo: 'Catalogo de Envases',
            nombre: 'Envases'
        },
        productos: {
            ruta: '/productos',
            fragmento: '/productos/fragment',
            script: '/assets/js/productos.js',
            titulo: 'Gestion de Productos',
            nombre: 'Productos'
        }
    };
    const promesasScripts = {};
    const origenesSeguros = new Set(['/', '/dashboard', '/clientes', '/usuarios', '/empleados', '/motos', '/proveedores', '/rubros', '/asignacion_motos', '/categorias', '/envases/vista', '/productos']);

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
        const enlaces = Array.from(document.querySelectorAll('.main-sidebar a.nav-link'));
        enlaces.forEach(enlace => {
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
            } else {
                enlace.removeAttribute('aria-current');
            }
        });

        document.querySelectorAll('.main-sidebar .nav-item.has-treeview').forEach(grupo => {
            const submenu = grupo.querySelector('.nav-treeview');
            const contieneActivo = submenu && Array.from(submenu.querySelectorAll('a.nav-link'))
                .some(enlace => enlace.getAttribute('aria-current') === 'page');

            grupo.classList.toggle('menu-open', Boolean(contieneActivo));
            if (submenu) {
                if (window.jQuery && window.jQuery.fn && window.jQuery.fn.stop) {
                    const $submenu = window.jQuery(submenu);
                    $submenu.stop(true, true);
                    if (contieneActivo) {
                        $submenu.slideDown(200);
                    } else {
                        $submenu.slideUp(200);
                    }
                } else {
                    submenu.style.display = contieneActivo ? 'block' : 'none';
                }
            }
        });
    }

    function cargarScript(nombreModulo, config) {
        if (window.AppModules && window.AppModules[nombreModulo]) {
            return Promise.resolve();
        }
        if (promesasScripts[config.script]) return promesasScripts[config.script];

        promesasScripts[config.script] = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = config.script;
            script.async = false;
            script.onload = resolve;
            script.onerror = () => reject(new Error('No se pudo cargar el modulo de ' + config.nombre + '.'));
            document.head.appendChild(script);
        });
        return promesasScripts[config.script];
    }

    async function obtenerFragmento(nombreModulo, config) {
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
                ? 'No tienes permiso para acceder a ' + config.nombre + '.'
                : 'No se pudo cargar ' + config.nombre + '.'), { status: response.status });
        }

        const html = await response.text();
        const documento = new DOMParser().parseFromString(html, 'text/html');
        if (!documento.querySelector('[data-modulo="' + nombreModulo + '"]')) {
            throw Object.assign(new Error('La respuesta no contiene el fragmento de ' + config.nombre + '.'), { status: 500 });
        }
        return documento.body.innerHTML;
    }

    async function cargarModulo(nombreModulo, pushState) {
        const config = modulos[nombreModulo];
        let html;
        try {
            html = await obtenerFragmento(nombreModulo, config);
            await cargarScript(nombreModulo, config);
        } catch (error) {
            if (error.status === 401) return;
            if (error.status === 403) {
                mostrarError('Acceso denegado', error.message);
            } else {
                mostrarError('Error de navegacion', error.message || ('No se pudo cargar ' + config.nombre + '.'));
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

        const nuevoModulo = window.AppModules && window.AppModules[nombreModulo];
        if (nuevoModulo && typeof nuevoModulo.init === 'function') nuevoModulo.init();
        actualizarSidebar(config.ruta);
        document.title = config.titulo;
        if (pushState && window.location.pathname !== config.ruta) {
            window.history.pushState({ modulo: nombreModulo }, '', config.ruta);
        }
    }

    function moduloPorRuta(ruta) {
        return Object.keys(modulos).find(nombre => modulos[nombre].ruta === ruta);
    }

    function esEnlaceModuloSeguro(event, enlace) {
        if (event.button !== 0 || event.ctrlKey || event.shiftKey || event.altKey || event.metaKey) return false;
        if (enlace.target && enlace.target !== '_self') return false;
        if (enlace.hasAttribute('download')) return false;
        if (enlace.matches('[data-toggle="treeview"], .has-treeview > .nav-link')) return false;
        const url = new URL(enlace.href, window.location.origin);
        return url.origin === window.location.origin
            && Boolean(moduloPorRuta(url.pathname))
            && origenesSeguros.has(window.location.pathname);
    }

    function inicializarNavegacion() {
        document.addEventListener('click', event => {
            const enlace = event.target instanceof Element ? event.target.closest('a') : null;
            if (!enlace || !esEnlaceModuloSeguro(event, enlace)) return;
            event.preventDefault();
            cargarModulo(moduloPorRuta(new URL(enlace.href, window.location.origin).pathname), true);
        });

        window.addEventListener('popstate', () => {
            const nombreModulo = moduloPorRuta(window.location.pathname);
            if (nombreModulo) {
                cargarModulo(nombreModulo, false);
            } else {
                window.location.assign(window.location.href);
            }
        });

        const inicial = moduloActual();
        if (inicial) {
            const nombreModulo = inicial.dataset.modulo;
            const config = modulos[nombreModulo];
            const modulo = window.AppModules && window.AppModules[nombreModulo];
            if (config && modulo && typeof modulo.init === 'function') modulo.init();
            if (config) document.title = inicial.dataset.pageTitle || config.titulo;
        }
        actualizarSidebar(window.location.pathname);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', inicializarNavegacion, { once: true });
    } else {
        inicializarNavegacion();
    }
}());
