ALTER TABLE absence_requests
    DROP CHECK ck_absence_status;

ALTER TABLE absence_requests
    ADD COLUMN boss_decision VARCHAR(20) NULL
        AFTER approval_comment,
    ADD COLUMN boss_decision_at DATETIME(6) NULL
        AFTER boss_decision,
    ADD COLUMN boss_comment VARCHAR(2000) NULL
        AFTER boss_decision_at,
    ADD COLUMN hr_decision VARCHAR(20) NULL
        AFTER boss_comment,
    ADD COLUMN hr_decision_at DATETIME(6) NULL
        AFTER hr_decision,
    ADD COLUMN hr_comment VARCHAR(2000) NULL
        AFTER hr_decision_at;

ALTER TABLE absence_requests
    ADD CONSTRAINT ck_absence_status
        CHECK (
            status IN (
                'PENDING',
                'PENDING_HR',
                'APPROVED',
                'REJECTED',
                'CANCELLED'
            )
        );

ALTER TABLE absence_requests
    ADD CONSTRAINT ck_absence_boss_decision
        CHECK (
            boss_decision IS NULL
            OR boss_decision IN ('APPROVED', 'REJECTED')
        );

ALTER TABLE absence_requests
    ADD CONSTRAINT ck_absence_hr_decision
        CHECK (
            hr_decision IS NULL
            OR hr_decision IN ('APPROVED', 'REJECTED')
        );
