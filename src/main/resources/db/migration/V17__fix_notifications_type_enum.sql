-- The V1 migration created notifications.type as an ENUM with only three values.
-- STRIKE and BUSINESS_VERIFICATION were added to the Java NotificationType enum later
-- but the column was never updated, causing a data error (→ 500) when saving a
-- STRIKE notification.  Switching to VARCHAR(50) avoids enum drift in future sprints.
ALTER TABLE notifications
    MODIFY COLUMN type VARCHAR(50);
