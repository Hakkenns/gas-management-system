-- Migración manual 6B-3A. No ejecutar sin revisar ambos preflights.
-- No clasifica métodos desconocidos: deben corregirse manualmente antes de los NOT NULL.

ALTER TABLE metodo_pago
    ADD COLUMN codigo VARCHAR(50) NULL,
    ADD COLUMN tipo_financiero VARCHAR(20) NULL;

UPDATE metodo_pago
SET codigo = 'EFECTIVO',
    tipo_financiero = 'EFECTIVO'
WHERE UPPER(TRIM(nombre)) = 'EFECTIVO';

UPDATE metodo_pago
SET codigo = 'YAPE',
    tipo_financiero = 'DIGITAL'
WHERE UPPER(TRIM(nombre)) = 'YAPE';

UPDATE metodo_pago
SET codigo = 'PLIN',
    tipo_financiero = 'DIGITAL'
WHERE UPPER(TRIM(nombre)) = 'PLIN';

-- PREFLIGHT 1 OBLIGATORIO: debe devolver cero filas antes de ejecutar la etapa 2.
SELECT id_metodo, nombre, codigo, tipo_financiero
FROM metodo_pago
WHERE codigo IS NULL OR tipo_financiero IS NULL;

-- PREFLIGHT 2 OBLIGATORIO: debe devolver cero filas antes de ejecutar la etapa 2.
SELECT codigo, COUNT(*) AS cantidad
FROM metodo_pago
WHERE codigo IS NOT NULL
GROUP BY codigo
HAVING COUNT(*) > 1;

-- NO EJECUTAR LA ETAPA 2 si alguno de los preflights devuelve filas.
-- Etapa 2: primero restricciones NOT NULL y recién después unicidad del código.
ALTER TABLE metodo_pago
    MODIFY COLUMN codigo VARCHAR(50) NOT NULL,
    MODIFY COLUMN tipo_financiero VARCHAR(20) NOT NULL;

ALTER TABLE metodo_pago
    ADD CONSTRAINT uk_metodo_pago_codigo UNIQUE (codigo);
