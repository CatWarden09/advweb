UPDATE users
SET total_earned = 0
WHERE total_earned IS NULL;

ALTER TABLE users
    ALTER COLUMN total_earned SET DEFAULT 0;
