ALTER TABLE advertisements RENAME COLUMN ad_moderation_status TO status;
ALTER TABLE advertisements ALTER COLUMN status TYPE VARCHAR(255) USING (
    CASE status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'APPROVED'
        WHEN 2 THEN 'REJECTED'
        ELSE 'PENDING'
    END
);

ALTER TABLE reviews RENAME COLUMN moderation_status TO status;
ALTER TABLE reviews ALTER COLUMN status TYPE VARCHAR(255) USING (
    CASE status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'APPROVED'
        WHEN 2 THEN 'REJECTED'
        ELSE 'PENDING'
    END
);
