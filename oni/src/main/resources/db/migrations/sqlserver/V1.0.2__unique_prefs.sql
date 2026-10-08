-- Prefs must be unique on (NodeName, PrefKey). Databases created by V1.0.0 already
-- have that primary key, but legacy VARS databases that Flyway baselined never ran
-- V1.0.0 and may have no constraint at all, allowing duplicate rows.

-- 1. Remove ALL rows whose (NodeName, PrefKey) occurs more than once. Every copy is
--    dropped, not just the extras, because there's no way to tell which value is correct.
--    Grouping uses the column collation, which is what the constraint below enforces.
--    This is safe on merge replicated databases; the deletes replicate like any other.
WITH dupes AS (
    SELECT COUNT(*) OVER (PARTITION BY NodeName, PrefKey) AS n
    FROM Prefs
)
DELETE FROM dupes WHERE n > 1;

-- 2. Add a unique constraint unless one already exists or the table is replicated.
--    Schema changes to replicated tables must be made at the publisher after the
--    cleanup above has synced to all subscribers, so those are left to a DBA:
--      ALTER TABLE Prefs ADD CONSTRAINT UQ_Prefs_NodeName_PrefKey UNIQUE NONCLUSTERED (NodeName, PrefKey);
--    NONCLUSTERED is required: the key can be 1280 bytes, over the 900 byte clustered limit.
DECLARE @prefs INT = OBJECT_ID('Prefs');

IF EXISTS (
    SELECT 1
    FROM sys.indexes i
    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE i.object_id = @prefs AND i.is_unique = 1 AND i.has_filter = 0
    GROUP BY i.index_id
    HAVING COUNT(*) = 2 AND SUM(CASE WHEN c.name IN ('NodeName', 'PrefKey') THEN 1 ELSE 0 END) = 2
)
    PRINT 'Prefs already has a unique key on (NodeName, PrefKey)';
ELSE IF EXISTS (SELECT 1 FROM sys.tables WHERE object_id = @prefs AND (is_merge_published = 1 OR is_replicated = 1))
     OR EXISTS (SELECT 1 FROM sys.triggers WHERE parent_id = @prefs AND name LIKE 'MSmerge[_]%')
     OR EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = @prefs AND name LIKE 'MSmerge[_]%')
    PRINT 'Prefs is replicated; skipping unique constraint. A DBA must add it at the publisher.';
ELSE
    ALTER TABLE Prefs ADD CONSTRAINT UQ_Prefs_NodeName_PrefKey UNIQUE NONCLUSTERED (NodeName, PrefKey);
