-- We were using nanosecond precision for last_updated_time, but this
-- was causing optimistic locking issues.
-- We will drop the indexes and recreate them.

-- Drop indexes safely (IF EXISTS must be used with DROP INDEX ON ...)
DROP INDEX IF EXISTS idx_Concept_LUT ON Concept;
DROP INDEX IF EXISTS idx_ConceptDelegate_LUT ON ConceptDelegate;
DROP INDEX IF EXISTS idx_ConceptName_LUT ON ConceptName;
DROP INDEX IF EXISTS idx_History_LUT ON History;
DROP INDEX IF EXISTS idx_LinkRealization_LUT ON LinkRealization;
DROP INDEX IF EXISTS idx_LinkTemplate_LUT ON LinkTemplate;
DROP INDEX IF EXISTS idx_Media_LUT ON Media;
DROP INDEX IF EXISTS idx_Reference_LUT ON Reference;
DROP INDEX IF EXISTS idx_UserAccount_LUT ON UserAccount;

-- Alter precision of last_updated_time
ALTER TABLE Concept
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE ConceptDelegate
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE ConceptName
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE History
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE LinkRealization
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE LinkTemplate
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE Media
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE Reference
ALTER COLUMN last_updated_time datetimeoffset(6);

ALTER TABLE UserAccount
ALTER COLUMN last_updated_time datetimeoffset(6);

-- Switch timezone

UPDATE concept
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE ConceptDelegate
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE ConceptName
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE History
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE LinkRealization
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE LinkTemplate
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE Media
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE Reference
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

UPDATE UserAccount
SET last_updated_time = TODATETIMEOFFSET(last_updated_time, '+00:00')
WHERE last_updated_time IS NOT NULL;

-- Recreate indexes
CREATE INDEX idx_Concept_LUT ON Concept (LAST_UPDATED_TIME);
CREATE INDEX idx_ConceptDelegate_LUT ON ConceptDelegate (LAST_UPDATED_TIME);
CREATE INDEX idx_ConceptName_LUT ON ConceptName (LAST_UPDATED_TIME);
CREATE INDEX idx_History_LUT ON History (LAST_UPDATED_TIME);
CREATE INDEX idx_LinkRealization_LUT ON LinkRealization (LAST_UPDATED_TIME);
CREATE INDEX idx_LinkTemplate_LUT ON LinkTemplate (LAST_UPDATED_TIME);
CREATE INDEX idx_Media_LUT ON Media (LAST_UPDATED_TIME);
CREATE INDEX idx_Reference_LUT ON Reference (LAST_UPDATED_TIME);
CREATE INDEX idx_UserAccount_LUT ON UserAccount (LAST_UPDATED_TIME);
