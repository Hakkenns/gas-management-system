// assets/js/main.js

document.addEventListener("click", function (e) {

    // CREAR
    if (e.target.closest("#btn-crear")) {
        $("#form-producto")[0].reset();
        $("#producto-id").val('');
        $("#imagen-actual").val('');
        $("#imagen-preview").hide();

        $("#modal-titulo").text("Crear Producto");
        
        // [NUEVO]: Aseguramos valores por defecto de stock al crear
        $("#stock").val(0); 
        $("#stock-min").val(5);

        $("#modal-producto").modal("show");
    }

    // EDITAR
    if (e.target.closest(".btn-editar")) {
        const id = e.target.closest(".btn-editar").dataset.id;

        fetch(`views/productos.php?action=obtener&id=${id}`)
            .then(r => r.json())
            .then(data => {

                $("#modal-titulo").text("Editar Producto");

                $("#producto-id").val(data.id);
                $("#nombre").val(data.nombre);
                $("#descripcion").val(data.descripcion);
                $("#precio").val(data.precio);
                
                // [NUEVO]: Rellenar campos de stock y stock mínimo
                $("#stock").val(data.stock);
                $("#stock-min").val(data.stock_min);
                
                // Rellenar la categoría y la imagen
                $("#id-categoria").val(data.id_categoria);
                $("#imagen-actual").val(data.imagen_url);

                if (data.imagen_url) {
                    $("#imagen-preview").attr("src", data.imagen_url).show();
                } else {
                    $("#imagen-preview").hide();
                }

                $("#modal-producto").modal("show");
            });
    }

});

// función para previsualizar la imagen
document.addEventListener("change", function (e) {
    if (e.target.id === "imagen") {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = ev => {
            $("#imagen-preview").attr("src", ev.target.result).show();
        };
        reader.readAsDataURL(file);
    }
});

// Función que intercepta el envío de formularios y prevenir la recarga de la página
$("#form-producto").on("submit", function(e) {
    e.preventDefault();

    let formData = new FormData(this);

    $.ajax({
        url: "views/productos.php?action=guardar",
        method: "POST",
        data: formData,
        contentType: false,
        processData: false,
        success: function(r) {
            if (r.trim() === "OK") {
                $("#modal-producto").modal('hide'); 
                AbrirPagina("productos"); 
            }
        }
    });
});