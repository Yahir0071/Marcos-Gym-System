BEGIN;

ALTER TABLE membresias
    ADD COLUMN IF NOT EXISTS codigo_preinscripcion VARCHAR(40),
    ADD COLUMN IF NOT EXISTS preinscripcion_vence TIMESTAMP,
    ADD COLUMN IF NOT EXISTS monto_reservado NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS periodo_reservado VARCHAR(12);

ALTER TABLE pagos ADD COLUMN IF NOT EXISTS referencia VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS uk_membresias_codigo_preinscripcion ON membresias(codigo_preinscripcion);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pagos_referencia ON pagos(referencia);
CREATE INDEX IF NOT EXISTS idx_membresias_estado_preinscripcion ON membresias(estado, preinscripcion_vence);

COMMIT;
