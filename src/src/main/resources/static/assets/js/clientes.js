document.addEventListener("DOMContentLoaded", function() {
    // --- LÓGICA DE CONTROL DE TIPOS DE DOCUMENTO (DNI/RUC) ---
    const selectTipoDoc = document.getElementById("tipo_documento");
    const inputNumDoc = document.getElementById("numero_documento");
    const labelNumDoc = document.getElementById("label-num-doc");
    const helpTextDoc = document.getElementById("num-doc-help");
    const btnBuscarDoc = document.getElementById("btn-buscar-doc");

    function actualizarInputDocumento() {
        const tipo = selectTipoDoc.value;
        inputNumDoc.value = ''; // Limpiar campo al cambiar tipo

        // Deshabilitar botón de búsqueda por defecto
        $(btnBuscarDoc).prop('disabled', true);
        
        // Limpiar mensajes de ayuda
        $(helpTextDoc).removeClass("text-danger text-success");

        if (tipo === 'DNI') {
            // Configuración para DNI (8 dígitos, búsqueda API)
            labelNumDoc.textContent = 'Número Documento (DNI):';
            inputNumDoc.maxLength = 8;
            inputNumDoc.pattern = "\\d{8}";
            helpTextDoc.textContent = 'Ingresa 8 dígitos para buscar en RENIEC (DNI).';
            $(btnBuscarDoc).prop('disabled', false).show(); // Habilitar botón de búsqueda
        } else if (tipo === 'RUC') {
            // Configuración para RUC (11 dígitos, sin API)
            labelNumDoc.textContent = 'Número Documento (RUC):';
            inputNumDoc.maxLength = 11;
            inputNumDoc.pattern = "\\d{11}";
            helpTextDoc.textContent = 'Ingresa 11 dígitos para RUC. Búsqueda manual.';
            $(btnBuscarDoc).prop('disabled', true).hide(); // Ocultar/Deshabilitar botón de búsqueda
        } else {
            // Configuración para OTRO (sin límite)
            labelNumDoc.textContent = 'Número Documento:';
            inputNumDoc.maxLength = 20; // Límite amplio
            inputNumDoc.pattern = "";
            helpTextDoc.textContent = 'Documento de Identidad (Otro).';
            $(btnBuscarDoc).prop('disabled', true).hide();
        }
    }

    // Inicializar y agregar listener al select
    if (selectTipoDoc) {
        actualizarInputDocumento();
        selectTipoDoc.addEventListener('change', actualizarInputDocumento);
    }
});


document.addEventListener("click", function (e) {

    // modal crear
    if (e.target.closest("#btn-crear")) {
        const modal = document.getElementById("modal-cliente");
        document.getElementById("modal-titulo").textContent = "Crear Cliente";

        // Limpieza de campos y mensajes
        document.getElementById("form-cliente").reset();
        document.getElementById("cliente-id").value = "";

        $("#tipo_documento").prop('disabled', false);
        
        // Re-ejecutar la lógica de DNI/RUC para configurar el input
        const selectTipoDoc = document.getElementById("tipo_documento");
        if (selectTipoDoc) {
            selectTipoDoc.value = "DNI"; // Asegurar que inicie en DNI
            selectTipoDoc.dispatchEvent(new Event('change')); // Disparar el evento para configurar input
        }
        
        $("#num-doc-help").removeClass("text-danger text-success").text("Ingresa 8 dígitos para buscar en RENIEC (DNI).");

        $(modal).modal('show');
    }

    if (e.target.closest("#btn-cancelar") || e.target.closest(".close")) {
        $("#modal-cliente").modal('hide');
    }

    // editar cliente
   if (e.target.closest(".btn-editar")) {
        const id = e.target.closest(".btn-editar").dataset.id;
        document.getElementById("form-cliente").reset(); 

        // Petición al backend para obtener los datos del cliente
        fetch(`views/clientes.php?action=obtener&id=${id}`)
            .then(r => r.json())
            .then(data => {

                document.getElementById("modal-titulo").textContent = "Editar Cliente";

                document.getElementById("cliente-id").value = data.id;
                
                // 1. CARGAMOS LOS VALORES USANDO JQUERY Y EL DOM
                // Usamos jQuery para el select, que suele ser más robusto.
                $("#tipo_documento").val(data.tipo_documento); // <-- Establece el valor (DNI, RUC, OTRO)

                document.getElementById("numero_documento").value = data.numero_documento; 
                document.getElementById("nombre").value = data.nombre;
                document.getElementById("correo").value = data.correo;
                
                // 2. DISPARAMOS EL CAMBIO
                // Esto es crucial para configurar el input (maxlength, pattern, texto de ayuda)
                const selectTipoDoc = document.getElementById("tipo_documento");
                if (selectTipoDoc) {
                    // Usamos un pequeño retraso (setTimeout) para dar tiempo a la UI si es necesario
                    setTimeout(() => {
                        selectTipoDoc.dispatchEvent(new Event('change'));
                        // 3. DESHABILITAMOS EL SELECT SOLO DESPUÉS DE LA RENDERIZACIÓN
                        $("#tipo_documento").prop('disabled', true); // BLOQUEA el campo tipo_documento
                    }, 50); // 50 milisegundos de retraso
                }

                $("#modal-cliente").modal('show');
            });
    }

    // [LÓGICA DE BÚSQUEDA DNI] (Ahora ligada a #btn-buscar-doc)
    if (e.target.closest("#btn-buscar-doc")) {
        e.preventDefault(); // Previene comportamiento predeterminado

        const selectTipoDoc = document.getElementById("tipo_documento");
        const inputNumDoc = document.getElementById("numero_documento");
        const numero_documento = inputNumDoc.value.trim();
        const btnBuscar = $("#btn-buscar-doc");

        // 1. Validar que sea DNI y que no esté ya buscando
        if (selectTipoDoc.value !== 'DNI') {
            $("#num-doc-help").addClass("text-danger").text("La búsqueda API solo es para DNI.");
            return;
        }

        // PREVENCIÓN DE DOBLE EJECUCIÓN (Si ya está cargando)
        if (btnBuscar.prop('disabled') && btnBuscar.find('.fa-spinner').length > 0) {
            return; 
        }

        // Limpiamos mensajes de ayuda
        $("#num-doc-help").removeClass("text-danger text-success").text("Ingresa 8 dígitos para buscar en RENIEC (DNI).");
        
        if (numero_documento.length !== 8) {
            $("#num-doc-help").addClass("text-danger").text("El DNI debe tener 8 dígitos.");
            return;
        }

        // Indicador de carga
        btnBuscar.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        // Llamada AJAX al controlador (al endpoint 'buscar_dni')
        $.ajax({
            url: "views/clientes.php?action=buscar_dni&numero_documento=" + numero_documento,
            method: "GET",
            dataType: "json",
            success: function(response) {
                // RESTAURACIÓN DEL BOTÓN INMEDIATA al recibir respuesta exitosa (SOLUCIÓN al spinner bug)
                btnBuscar.prop('disabled', false).html('<i class="fas fa-search"></i>');

                if (response.success) {
                    document.getElementById("nombre").value = response.nombre;
                    document.getElementById("correo").value = response.correo || '';
                    $("#num-doc-help").addClass("text-success").text("Datos cargados: " + response.nombre);
                } else {
                    $("#num-doc-help").addClass("text-danger").text(response.message || "Error al buscar datos.");
                    document.getElementById("nombre").value = '';
                    document.getElementById("correo").value = ''; 
                }
            },
            error: function() {
                $("#num-doc-help").addClass("text-danger").text("Error de conexión al servidor.");
            },
            complete: function() {
                // Si hubo un error de conexión, también aseguramos la restauración
                if (btnBuscar.prop('disabled')) {
                     btnBuscar.prop('disabled', false).html('<i class="fas fa-search"></i>');
                }
            }
        });
    }

});


// ENVÍO DE FORMULARIO (submit AJAX)
$(document).off("submit", "#form-cliente").on("submit", "#form-cliente", function (e) {
    e.preventDefault();

    const tipoDocSelect = $("#tipo_documento");
    const estabaDeshabilitado = tipoDocSelect.prop('disabled');
    if (estabaDeshabilitado) {
        tipoDocSelect.prop('disabled', false);
    }

    // Limpiamos el mensaje de error antes de enviar
    $("#num-doc-help").removeClass("text-danger text-success"); 

    let formData = new FormData(this); 

    const btnGuardar = $(this).find('button[type="submit"]');
    btnGuardar.prop('disabled', true).text('Guardando...');

    $.ajax({
        url: "views/clientes.php?action=guardar",
        type: "POST",
        data: formData,
        contentType: false, 
        processData: false, 
        success: function (resp) {
            
            // MANEJO DE ERRORES: DOCUMENTO DUPLICADO O VACÍO
            if (resp.trim() === "ERROR_DUPLICATE_DOCUMENTO") {
                $("#num-doc-help").addClass("text-danger").text("ERROR: El número de documento ya está registrado.");
                btnGuardar.prop('disabled', false).text('Guardar'); 
            } else if (resp.trim() === "ERROR_EMPTY_DOCUMENTO") {
                 $("#num-doc-help").addClass("text-danger").text("ERROR: El número de documento es requerido.");
                 btnGuardar.prop('disabled', false).text('Guardar'); 
            } else {
                // Éxito
                $("#modal-cliente").modal('hide');
                AbrirPagina("clientes"); 
            }
        },
        error: function(xhr, status, error) {
            console.error("Error al guardar:", error);
            alert("Error: No se pudo conectar con el servidor para guardar el cliente.");
        },
        complete: function() {
            // Aseguramos la restauración si no se hizo en el bloque success/error
            if (btnGuardar.prop('disabled')) {
                btnGuardar.prop('disabled', false).text('Guardar');
            }
        }
    });
});