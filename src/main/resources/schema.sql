CREATE TABLE IF NOT EXISTS building_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL DEFAULT 'Infinity Tower',
    floors INT NOT NULL DEFAULT 12,
    basements INT NOT NULL DEFAULT 2,
    lobby INT NOT NULL DEFAULT 1,
    idle_floor INT NOT NULL DEFAULT 1,
    parking_floors VARCHAR(255) NOT NULL DEFAULT '-1,-2',
    vip_floors VARCHAR(255) NOT NULL DEFAULT '10,11,12',
    service_floors VARCHAR(255) NOT NULL DEFAULT '-2',
    mechanical_floors VARCHAR(255) NOT NULL DEFAULT '',
    elevator_count INT NOT NULL DEFAULT 4,
    capacity INT NOT NULL DEFAULT 8,
    weight_limit INT NOT NULL DEFAULT 900,
    max_speed DOUBLE NOT NULL DEFAULT 2.2,
    acceleration DOUBLE NOT NULL DEFAULT 1.1,
    deceleration DOUBLE NOT NULL DEFAULT 1.1,
    door_open_time DOUBLE NOT NULL DEFAULT 1.6,
    door_close_time DOUBLE NOT NULL DEFAULT 1.6,
    load_time DOUBLE NOT NULL DEFAULT 1.2,
    unload_time DOUBLE NOT NULL DEFAULT 1.0,
    energy_saving BOOLEAN NOT NULL DEFAULT FALSE,
    algorithm VARCHAR(40) NOT NULL DEFAULT 'look',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS simulation_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sim_day INT NOT NULL,
    sim_clock VARCHAR(16) NOT NULL,
    category VARCHAR(40) NOT NULL,
    level VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sim_log_category (category),
    INDEX idx_sim_log_created (created_at)
);

CREATE TABLE IF NOT EXISTS report_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(40) NOT NULL,
    building_name VARCHAR(120) NOT NULL,
    algorithm_used VARCHAR(40) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO building_config (id, name)
SELECT 1, 'Infinity Tower'
WHERE NOT EXISTS (SELECT 1 FROM building_config WHERE id = 1);
