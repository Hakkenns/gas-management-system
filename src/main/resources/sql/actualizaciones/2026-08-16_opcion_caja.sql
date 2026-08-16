INSERT INTO opciones (nombre, icono, ruta, estado, id_padre)
SELECT 'Caja', 'fas fa-cash-register', 'caja', 1, NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM opciones
    WHERE ruta = 'caja'
);
