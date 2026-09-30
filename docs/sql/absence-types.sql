CREATE TABLE IF NOT EXISTS absence_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_absence_type_name
        UNIQUE (name),

    INDEX idx_absence_type_active_name
        (active, name)
)
ENGINE=InnoDB
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;


INSERT IGNORE INTO absence_types
    (name, description, active)
VALUES
    (
        'Ausencia personal',
        'Ausencia o permiso por motivos personales',
        b'1'
    ),
    (
        'Ausencia laboral',
        'Ausencia relacionada con actividades o diligencias laborales',
        b'1'
    ),
    (
        'Ausencia médica',
        'Ausencia relacionada con motivos médicos o de salud',
        b'1'
    );
