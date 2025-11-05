-- We were using nanosecond precision for last_updated_time, but this
-- was causing optimistic locking issues.

-- We will drop the index and recreate it.

DROP INDEX IF EXISTS idx_Concept_LUT;
DROP INDEX IF EXISTS idx_ConceptDelegate_LUT;
DROP INDEX IF EXISTS idx_ConceptName_LUT;
DROP INDEX IF EXISTS idx_History_LUT;
DROP INDEX IF EXISTS idx_LinkRealization_LUT;
DROP INDEX IF EXISTS idx_LinkTemplate_LUT;
DROP INDEX IF EXISTS idx_Media_LUT;
DROP INDEX IF EXISTS idx_Reference_LUT;
DROP INDEX IF EXISTS idx_UserAccount_LUT;

-- ALTER precision of last_updated_time

ALTER TABLE concept
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE conceptdelegate
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE conceptname
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE history
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE linkrealization
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE linktemplate
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE media
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE reference
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

ALTER TABLE useraccount
ALTER COLUMN last_updated_time TYPE timestamptz USING last_updated_time::timestamptz;

-- Switch timezone

UPDATE concept
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE conceptdelegate
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE conceptname
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE history
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE linkrealization
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE linktemplate
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE media
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE reference
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;

UPDATE useraccount
SET last_updated_time = last_updated_time AT TIME ZONE 'UTC'
WHERE last_updated_time IS NOT NULL;


-- Recreate indexes

CREATE INDEX idx_Concept_LUT ON concept (last_updated_time);
CREATE INDEX idx_ConceptDelegate_LUT ON conceptdelegate (last_updated_time);
CREATE INDEX idx_ConceptName_LUT ON conceptname (last_updated_time);
CREATE INDEX idx_History_LUT ON history (last_updated_time);
CREATE INDEX idx_LinkRealization_LUT ON linkrealization (last_updated_time);
CREATE INDEX idx_LinkTemplate_LUT ON linktemplate (last_updated_time);
CREATE INDEX idx_Media_LUT ON media (last_updated_time);
CREATE INDEX idx_Reference_LUT ON reference (last_updated_time);
CREATE INDEX idx_UserAccount_LUT ON useraccount (last_updated_time);
