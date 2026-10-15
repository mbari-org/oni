-- Prefs must be unique on (NodeName, PrefKey). Databases created by V1.0.0 already
-- have that primary key, but legacy VARS databases that Flyway baselined never ran
-- V1.0.0 and may have no constraint at all, allowing duplicate rows.

-- 1. Remove ALL rows whose (NodeName, PrefKey) occurs more than once. Every copy is
--    dropped, not just the extras, because there's no way to tell which value is correct.
DELETE FROM prefs p
USING (
    SELECT nodename, prefkey
    FROM prefs
    GROUP BY nodename, prefkey
    HAVING COUNT(*) > 1
) d
WHERE p.nodename = d.nodename AND p.prefkey = d.prefkey;

-- 2. Add a unique constraint unless a unique index on exactly (nodename, prefkey) exists.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        WHERE i.indrelid = 'prefs'::regclass
          AND i.indisunique
          AND i.indpred IS NULL
          AND i.indnkeyatts = 2
          AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
               FROM pg_attribute a
               WHERE a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)) = ARRAY['nodename', 'prefkey']
    ) THEN
        ALTER TABLE prefs ADD CONSTRAINT uq_prefs_nodename_prefkey UNIQUE (nodename, prefkey);
    END IF;
END $$;
