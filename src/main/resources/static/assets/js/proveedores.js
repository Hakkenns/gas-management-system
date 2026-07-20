document.addEventListener("click", function (e) {
    if (e.target.closest("#btn-crear-proveedor")) {
        document.getElementById("form-proveedor").reset();
        document.getElementById("input-id").value = "";
        document.getElementById("modal-titulo-proveedor").textContent = "Crear Proveedor";
        $("#modal-proveedor").modal("show");
    }

    if (e.target.closest(".btn-editar-proveedor")) {
        const btn = e.target.closest(".btn-editar-proveedor");
        const id = btn.dataset.id;

        fetch(`/proveedores/${id}`)
            .then(r => r.json())
            .then(data => {
                document.getElementById("modal-titulo-proveedor").textContent = "Editar Proveedor";
                document.getElementById("input-id").value = data.id;
                document.getElementById("input-ruc").value = data.ruc;
                document.getElementById("input-nombre").value = data.nombre;
                document.getElementById("input-telefono").value = data.telefono;
                document.getElementById("input-correo").value = data.correo;
                document.getElementById("input-rubro").value = data.idRubro;
                $("#modal-proveedor").modal("show");
            })
            .catch(err => console.error("ERROR JSON PROVEEDOR:", err));
    }

    if (e.target.closest(".btn-cambiar-estado")) {
        const btn = e.target.closest(".btn-cambiar-estado");
        const id = btn.dataset.id;
        const estadoActual = parseInt(btn.dataset.estado, 10);
        const nuevoEstado = estadoActual === 1 ? 0 : 1;
        ProveedorCambiarEstado(id, nuevoEstado);
        e.preventDefault();
    }

    if (e.target.closest(".btn-eliminar-proveedor")) {
        const id = e.target.closest(".btn-eliminar-proveedor").dataset.id;
        ProveedorEliminar(id);
        e.preventDefault();
    }
});

function initTablaProveedores() {
    if ($.fn.DataTable) {
        const table = $('#tabla-proveedores');
        if ($.fn.dataTable.isDataTable(table)) {
            table.DataTable().destroy();
        }

        const dataTable = table.DataTable({
            dom: 'rt<"bottom"ip><"clear">',
            paging: true,
            pageLength: 10,
            lengthMenu: [[10, 20, 30, 40, 50], [10, 20, 30, 40, 50]],
            lengthChange: true,
            searching: true,
            ordering: true,
            info: true,
            autoWidth: false,
            responsive: true,
            pagingType: 'simple',
            scrollY: "400px",
            scrollCollapse: true,
            language: {
                search: 'Buscar:',
                lengthMenu: 'Mostrar _MENU_ registros',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ registros',
                infoEmpty: 'No hay registros',
                infoFiltered: '(filtrado de _MAX_ registros)',
                zeroRecords: 'No se encontraron resultados',
                paginate: {
                    previous: 'Anterior',
                    next: 'Siguiente'
                }
            }
        });

        // Forzar reajuste de columnas al cambiar el tamaño de la ventana o zoom
        $(window).on('resize', function () {
            dataTable.columns.adjust().draw();
        });

        // Conectar controles personalizados
        const $lengthSelect = $('#proveedores-length');
        const $searchInput = $('#proveedores-search');

        // Cambiar número de registros por página
        if ($lengthSelect.length) {
            $lengthSelect.on('change', function () {
                const pageLength = parseInt($(this).val(), 10);
                dataTable.page.len(pageLength).draw();
            });
        }

        // Búsqueda personalizada
        if ($searchInput.length) {
            $searchInput.on('keyup', function () {
                dataTable.search(this.value).draw();
            });
        }

        // Ocultar controles nativos duplicados de DataTables
        const $wrapper = table.closest('.dataTables_wrapper');
        if ($wrapper.length) {
            const wrapperEl = $wrapper[0];
            const paginateContainer = wrapperEl.querySelector('.dataTables_paginate');
            if (paginateContainer) {
                paginateContainer.style.display = 'none';
            }

            const defaultInfo = wrapperEl.querySelector('.dataTables_info');
            if (defaultInfo) {
                defaultInfo.style.display = 'none';
            }

            const pagerRow = document.createElement('div');
            pagerRow.className = 'compras-pager-row';
            wrapperEl.appendChild(pagerRow);

            const infoBar = document.createElement('div');
            infoBar.className = 'compras-info-bar';
            pagerRow.appendChild(infoBar);

            const customPager = document.createElement('div');
            customPager.className = 'compras-custom-pagination';
            customPager.setAttribute('aria-label', 'Paginación de proveedores');
            pagerRow.appendChild(customPager);

            injectCustomPaginationStyles();
            renderCustomInfo(dataTable, infoBar);
            renderCustomPagination(dataTable, customPager);
            attachSwipePagination(wrapperEl, dataTable);

            dataTable.on('draw.dt', () => {
                renderCustomInfo(dataTable, infoBar);
                renderCustomPagination(dataTable, customPager);
            });
        }
    }
}

function reloadProveedoresTable() {
    fetch("/proveedores/tabla")
        .then(r => {
            if (!r.ok) throw new Error("Error cargando tabla de proveedores");
            return r.text();
        })
        .then(html => {
            const container = document.getElementById("contenedor-tabla");
            if (container) {
                container.innerHTML = html;
                initTablaProveedores();
            }
        })
        .catch(err => console.error("ERROR recargando tabla de proveedores:", err));
}

initTablaProveedores();

$("#form-proveedor").on("submit", function (e) {
    e.preventDefault();

    const btnGuardar = $(this).find('button[type="submit"]');
    btnGuardar.prop('disabled', true).text('Guardando...');

    $.ajax({
        url: "/proveedores",
        type: "POST",
        data: $(this).serialize(),
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                $("#modal-proveedor").modal('hide');
                reloadProveedoresTable();
            } else {
                const message = resp.message || "Error guardando proveedor";
                alert(message);
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.error("Error guardando proveedor:", msg);
        },
        complete: function () {
            btnGuardar.prop('disabled', false).text('Guardar');
        }
    });
});

function ProveedorCambiarEstado(id, estado) {
    $.ajax({
        url: `/proveedores/${id}/estado`,
        type: "POST",
        data: { estado: estado },
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadProveedoresTable();
            } else {
                alert(resp.message || "Error cambiando estado");
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.error("Error cambiando estado:", msg);
        }
    });
}

function ProveedorEliminar(id) {
    if (!confirm("¿Eliminar el proveedor?")) return;

    $.ajax({
        url: `/proveedores/${id}/eliminar`,
        type: "POST",
        dataType: "json",
        success: function (resp) {
            if (resp.status === "OK") {
                reloadProveedoresTable();
            } else {
                alert(resp.message || "ERROR ELIMINANDO PROVEEDOR");
            }
        },
        error: function (xhr) {
            let msg = xhr.responseText;
            try {
                const json = JSON.parse(xhr.responseText);
                msg = json.message || msg;
            } catch (e) {}
            alert(msg);
            console.error("ERROR ELIMINANDO PROVEEDOR:", msg);
        }
    });
}

// Funciones de paginación personalizada (reutilizadas de compras.js)
function injectCustomPaginationStyles() {
    if (document.getElementById('compras-custom-pagination-style')) {
        return;
    }

    const style = document.createElement('style');
    style.id = 'compras-custom-pagination-style';
    style.textContent = `
        .compras-info-bar {
            display: flex;
            justify-content: flex-start;
            align-items: center;
            margin: 10px 0 6px;
            font-size: 0.95rem;
            color: #495057;
            font-weight: 600;
            visibility: visible !important;
            opacity: 1 !important;
            flex: 1;
        }
        .compras-pager-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            margin-top: 4px;
            gap: 12px;
        }
        .compras-custom-pagination {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 4px;
            padding: 6px 0;
            flex-wrap: nowrap;
            white-space: nowrap;
            overflow: hidden;
            visibility: visible !important;
            opacity: 1 !important;
        }
        .compras-custom-pagination .page-btn {
            min-width: 38px;
            height: 38px;
            padding: 0 10px;
            border: 1px solid #ced4da;
            border-radius: 6px;
            background: #fff;
            color: #343a40;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            cursor: pointer;
            line-height: 1;
        }
        .compras-custom-pagination .page-btn.active {
            background: #0d6efd;
            color: #fff;
            border-color: #0d6efd;
        }
        .compras-custom-pagination .page-btn:disabled {
            opacity: 0.65;
            cursor: not-allowed;
        }
        .compras-custom-pagination .page-btn:hover:not(:disabled) {
            background: #e9ecef;
        }
        /* Ocultar controles nativos duplicados de DataTables */
        .dataTables_wrapper .dataTables_length:not(:first-of-type),
        .dataTables_wrapper .dataTables_filter:not(:first-of-type) {
            display: none !important;
        }
    `;
    document.head.appendChild(style);
}

function renderCustomInfo(dataTable, infoElement) {
    const info = dataTable.page.info();
    const totalRecords = info.recordsTotal;
    const start = totalRecords === 0 ? 0 : info.start + 1;
    const end = totalRecords === 0 ? 0 : info.end;

    infoElement.textContent = totalRecords === 0
        ? 'No hay registros para mostrar'
        : `Mostrando ${start} a ${end} de ${totalRecords} registros`;
}

function renderCustomPagination(dataTable, pagerElement) {
    const info = dataTable.page.info();
    const totalPages = info.pages;
    const currentPage = info.page;

    pagerElement.innerHTML = '';

    if (totalPages <= 1) {
        pagerElement.style.display = 'none';
        return;
    }

    pagerElement.style.display = 'flex';
    pagerElement.style.visibility = 'visible';
    pagerElement.style.opacity = '1';

    const createPageButton = (label, pageIndex, isActive = false, disabled = false) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = label;
        button.className = `page-btn${isActive ? ' active' : ''}`;
        button.setAttribute('aria-current', isActive ? 'page' : 'false');

        if (disabled) {
            button.disabled = true;
        } else {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                if (document.activeElement instanceof HTMLElement) {
                    document.activeElement.blur();
                }
                dataTable.page(pageIndex).draw(false);
            });
        }

        return button;
    };

    const prevButton = createPageButton('‹', Math.max(0, currentPage - 1), false, currentPage === 0);
    prevButton.setAttribute('aria-label', 'Página anterior');
    pagerElement.appendChild(prevButton);

    const startPage = Math.max(0, Math.min(currentPage - 1, totalPages - 3));
    const endPage = Math.min(totalPages - 1, startPage + 2);

    for (let pageIndex = startPage; pageIndex <= endPage; pageIndex += 1) {
        const pageButton = createPageButton(String(pageIndex + 1), pageIndex, pageIndex === currentPage);
        pagerElement.appendChild(pageButton);
    }

    const nextButton = createPageButton('›', Math.min(totalPages - 1, currentPage + 1), false, currentPage >= totalPages - 1);
    nextButton.setAttribute('aria-label', 'Página siguiente');
    pagerElement.appendChild(nextButton);
}

function attachSwipePagination(wrapperElement, dataTable) {
    let touchStartX = 0;

    wrapperElement.addEventListener('touchstart', (event) => {
        touchStartX = event.touches[0].clientX;
    }, { passive: true });

    wrapperElement.addEventListener('touchend', (event) => {
        const touchEndX = event.changedTouches[0].clientX;
        const deltaX = touchEndX - touchStartX;

        if (Math.abs(deltaX) < 50) {
            return;
        }

        if (deltaX < 0) {
            dataTable.page('next').draw(false);
        } else {
            dataTable.page('previous').draw(false);
        }
    }, { passive: true });
}
