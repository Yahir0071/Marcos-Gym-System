-- =============================================================================
-- Migración de Base de Datos - Módulo de Operaciones y Validaciones
-- Responsable: Chancan Moreno, Esaul Ronaldo Soto
-- Rama: tarea/esaul-operaciones-validaciones
-- =============================================================================

BEGIN;

-- 1. Incorporar campos para registro completo y coherente de Socios (M01)
ALTER TABLE IF EXISTS socios
    ADD COLUMN IF NOT EXISTS genero VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

-- 2. Asegurar índices para búsquedas optimizadas por DNI y usuario
CREATE INDEX IF NOT EXISTS idx_socios_dni ON socios(dni);
CREATE INDEX IF NOT EXISTS idx_socios_usuario_id ON socios(usuario_id);

-- 3. Índices en reservas y membresías para validación de propiedad y vigencia (C03, C06)
CREATE INDEX IF NOT EXISTS idx_reservas_socio_id ON reservas(socio_id);
CREATE INDEX IF NOT EXISTS idx_membresias_socio_id_estado ON membresias(socio_id, estado);

COMMIT;
