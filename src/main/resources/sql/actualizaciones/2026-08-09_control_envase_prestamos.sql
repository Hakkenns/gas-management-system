ALTER TABLE control_envase
    ADD COLUMN fecha_limite_devolucion DATE NULL,
    ADD COLUMN tipo_prestamo VARCHAR(20) NULL;

UPDATE control_envase
SET tipo_prestamo = 'LEGADO'
WHERE tipo_prestamo IS NULL;

ALTER TABLE control_envase
    MODIFY COLUMN tipo_prestamo VARCHAR(20) NOT NULL;
