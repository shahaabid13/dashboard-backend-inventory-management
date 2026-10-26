INSERT IGNORE INTO comment_mentions (comment_id, mentioned_user_id)
WITH RECURSIVE mention_scan AS (
    SELECT
        c.id AS comment_id,
        c.author_id,
        1 AS occurrence,
        REGEXP_SUBSTR(c.body, '@([A-Za-z0-9._-]+)', 1, 1, NULL, 1) AS username
    FROM comments c
    WHERE c.entity_type IN ('TASK', 'TICKET')
      AND REGEXP_LIKE(c.body, '@[A-Za-z0-9._-]+')

    UNION ALL

    SELECT
        mention_scan.comment_id,
        mention_scan.author_id,
        mention_scan.occurrence + 1,
        REGEXP_SUBSTR(
            c.body,
            '@([A-Za-z0-9._-]+)',
            1,
            mention_scan.occurrence + 1,
            NULL,
            1
        ) AS username
    FROM mention_scan
    JOIN comments c ON c.id = mention_scan.comment_id
    WHERE mention_scan.username IS NOT NULL
)
SELECT mention_scan.comment_id, u.id
FROM mention_scan
JOIN app_user u ON u.username = mention_scan.username
WHERE mention_scan.username IS NOT NULL
  AND u.id <> mention_scan.author_id;
