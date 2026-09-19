-- Manual deployment script for MySQL. Not executed automatically.
-- Run against the same schema as users. Match users.id charset/collation first.
-- One current assignment per employee; reassign by updating this row.
CREATE TABLE absence_approver_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(255) NOT NULL,
    approver_id VARCHAR(255) NOT NULL,
    active BIT NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_absence_assignment_employee UNIQUE (employee_id),
    CONSTRAINT fk_absence_assignment_employee FOREIGN KEY (employee_id) REFERENCES users(id),
    CONSTRAINT fk_absence_assignment_approver FOREIGN KEY (approver_id) REFERENCES users(id),
    CONSTRAINT ck_absence_assignment_distinct CHECK (employee_id <> approver_id)
) ENGINE=InnoDB;

CREATE TABLE absence_requests (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    requester_id VARCHAR(255) NOT NULL,
    approver_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME(6) NULL,
    end_time TIME(6) NULL,
    reason VARCHAR(2000) NOT NULL,
    support_file VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    approved_at DATETIME(6) NULL,
    approval_comment VARCHAR(2000) NULL,
    CONSTRAINT fk_absence_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_absence_approver FOREIGN KEY (approver_id) REFERENCES users(id),
    CONSTRAINT ck_absence_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    INDEX idx_absence_requester_created (requester_id, created_at),
    INDEX idx_absence_approver_status (approver_id, status)
) ENGINE=InnoDB;

CREATE TABLE absence_mail_outbox (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    last_error VARCHAR(100) NULL,
    INDEX idx_absence_mail_pending (sent_at, next_attempt_at)
) ENGINE=InnoDB;
