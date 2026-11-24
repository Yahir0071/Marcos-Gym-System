CREATE TABLE IF NOT EXISTS registros_acceso (
    id BIGSERIAL PRIMARY KEY,
    socio_id BIGINT NOT NULL REFERENCES socios(id),
    qr_token VARCHAR(80) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    franja_horaria VARCHAR(20),
    fecha_emision TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_uso TIMESTAMP,
    motivo VARCHAR(255)
);
