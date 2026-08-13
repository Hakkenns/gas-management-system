CREATE TABLE IF NOT EXISTS caja (
    id_caja BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    activa TINYINT(1) NOT NULL,
    PRIMARY KEY (id_caja),
    CONSTRAINT uk_caja_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS sesion_caja (
    id_sesion_caja BIGINT NOT NULL AUTO_INCREMENT,
    id_caja BIGINT NOT NULL,
    fecha_hora_apertura DATETIME NOT NULL,
    fecha_hora_cierre DATETIME NULL,
    estado VARCHAR(20) NOT NULL,
    id_usuario_apertura BIGINT NOT NULL,
    id_usuario_cierre BIGINT NULL,
    monto_esperado_cierre DECIMAL(12,2) NULL,
    monto_declarado_cierre DECIMAL(12,2) NULL,
    diferencia_cierre DECIMAL(12,2) NULL,
    observaciones TEXT NULL,
    PRIMARY KEY (id_sesion_caja),
    KEY idx_sesion_caja_caja_estado (id_caja, estado),
    KEY idx_sesion_caja_usuario_apertura (id_usuario_apertura),
    KEY idx_sesion_caja_usuario_cierre (id_usuario_cierre),
    CONSTRAINT fk_sesion_caja_caja FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
    CONSTRAINT fk_sesion_caja_usuario_apertura FOREIGN KEY (id_usuario_apertura)
        REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_sesion_caja_usuario_cierre FOREIGN KEY (id_usuario_cierre)
        REFERENCES usuarios (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS movimiento_caja (
    id_movimiento_caja BIGINT NOT NULL AUTO_INCREMENT,
    id_caja BIGINT NOT NULL,
    id_sesion_caja BIGINT NULL,
    fecha_hora DATETIME NOT NULL,
    sentido VARCHAR(20) NOT NULL,
    origen VARCHAR(40) NOT NULL,
    canal_fondos VARCHAR(40) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    id_usuario_responsable BIGINT NOT NULL,
    descripcion TEXT NULL,
    referencia VARCHAR(100) NULL,
    id_metodo BIGINT NULL,
    id_pago BIGINT NULL,
    id_empleado_custodio BIGINT NULL,
    id_movimiento_original BIGINT NULL,
    PRIMARY KEY (id_movimiento_caja),
    UNIQUE KEY uk_movimiento_caja_pedido_pago (id_pago),
    KEY idx_movimiento_caja_sesion_fecha (id_sesion_caja, fecha_hora),
    KEY idx_movimiento_caja_caja_fecha (id_caja, fecha_hora),
    KEY idx_movimiento_caja_usuario (id_usuario_responsable),
    KEY idx_movimiento_caja_origen (origen),
    KEY idx_movimiento_caja_original (id_movimiento_original),
    CONSTRAINT fk_movimiento_caja_caja FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
    CONSTRAINT fk_movimiento_caja_sesion FOREIGN KEY (id_sesion_caja)
        REFERENCES sesion_caja (id_sesion_caja),
    CONSTRAINT fk_movimiento_caja_usuario FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuarios (id_usuario),
    CONSTRAINT fk_movimiento_caja_metodo FOREIGN KEY (id_metodo) REFERENCES metodo_pago (id_metodo),
    CONSTRAINT fk_movimiento_caja_pago FOREIGN KEY (id_pago) REFERENCES pedido_pagos (id_pago),
    CONSTRAINT fk_movimiento_caja_empleado FOREIGN KEY (id_empleado_custodio)
        REFERENCES empleados (id_empleado),
    CONSTRAINT fk_movimiento_caja_original FOREIGN KEY (id_movimiento_original)
        REFERENCES movimiento_caja (id_movimiento_caja)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO caja (codigo, nombre, activa)
VALUES ('CAJA_PRINCIPAL', 'Caja principal', 1)
ON DUPLICATE KEY UPDATE codigo = VALUES(codigo);
