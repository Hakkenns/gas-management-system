document.addEventListener("DOMContentLoaded", function () {
    const modalTitle = document.getElementById("modal-titulo-categoria");
    const form = document.getElementById("form-categoria");
    const idField = document.getElementById("categoria-id");
    const nombreField = document.getElementById("nombre-categoria");
    const descripcionField = document.getElementById("descripcion-categoria");
    const btnCreate = document.getElementById("btn-crear-categoria");
    const editButtons = document.querySelectorAll(".btn-editar-categoria");

    if (btnCreate) {
        btnCreate.addEventListener("click", function () {
            modalTitle.textContent = "Nueva Categoría";
            idField.value = "";
            nombreField.value = "";
            descripcionField.value = "";
            form.action = "/categorias";
        });
    }

    editButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const id = button.dataset.id;
            const nombre = button.dataset.nombre || "";
            const descripcion = button.dataset.descripcion || "";

            modalTitle.textContent = "Editar Categoría";
            idField.value = id;
            nombreField.value = nombre;
            descripcionField.value = descripcion;
            form.action = "/categorias";

            if (window.jQuery && typeof window.jQuery === "function") {
                window.jQuery("#modal-categoria").modal("show");
            }
        });
    });
});
