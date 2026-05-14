$(document).ready(function() {

    
    
    // Evento Delegado para clics en la vista
    $(document).on("click", function (e) {

        
        if (e.target.closest("#btn-crear-proveedor")) {
            $("#form-proveedor")[0].reset();
            $("#proveedor-id").val('');
            $("#modal-titulo-proveedor").text("Crear Proveedor");
            $("#modal-proveedor").modal("show");
        }

      
        if (e.target.closest(".btn-editar-proveedor")) {
            const id = e.target.closest(".btn-editar-proveedor").dataset.id;

            // Llama a la propia vista para obtener el JSON del proveedor
            fetch(`views/proveedores.php?action=obtener&id=${id}`)
                .then(r => r.json())
                .then(data => {

                    $("#modal-titulo-proveedor").text("Editar Proveedor");
                
                    $("#proveedor-id").val(data.id_proveedor); 
                    $("#nombre-proveedor").val(data.nombre);
                    $("#telefono-proveedor").val(data.telefono);
                    $("#correo-electronico-proveedor").val(data.correo_electronico); 
                    $("#direccion-proveedor").val(data.direccion);
                    $("#descripcion-proveedor").val(data.descripcion); 

                    $("#modal-proveedor").modal("show");
                });
        }
    });

   
    // envio de formul de ajax
    
    // Maneja la inserción/actualización
    $("#form-proveedor").off("submit").on("submit", function(e) {
        e.preventDefault();

        let formData = new FormData(this);
        
        

        // Deshabilita el botón de guardar
        const btnGuardar = $(this).find('button[type="submit"]');
        btnGuardar.prop('disabled', true).text('Guardando...');

        $.ajax({
            // Llama a la propia vista para enrutar al Controlador
            url: "views/proveedores.php?action=guardar",
            method: "POST",
            data: formData,
            contentType: false,
            processData: false,
            success: function(r) {
                if (r.trim() === "OK") {
                    $("#modal-proveedor").modal('hide');
                    // Recarga la vista principal para ver los cambios
                    AbrirPagina("proveedores"); 
                } else {
                    console.error("Error al guardar proveedor:", r);
                    alert("Hubo un error al guardar: " + r);
                }
            },
            error: function() {
                alert("Ocurrió un error en la comunicación con el servidor.");
            },
            complete: function() {
                // Vuelve a habilitar el botón
                btnGuardar.prop('disabled', false).text('Guardar');
            }
        });
    });

    
    if ($.fn.DataTable) {
        $('#tabla-proveedores').DataTable({            
            "paging": true,
            "lengthChange": true,
            "searching": true,
            "ordering": true,
            "info": true,
            "autoWidth": false,
            "responsive": true,
        });
    }

});