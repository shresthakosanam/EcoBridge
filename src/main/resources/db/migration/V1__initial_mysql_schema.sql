CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    google_id VARCHAR(255) NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    address TEXT NULL,
    location VARCHAR(255) NULL,
    profile_image_url TEXT NULL,
    password_hash VARCHAR(100) NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_google_id (google_id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_status (role, account_status),
    CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER','ROLE_COLLECTOR','ROLE_ADMIN')),
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE','SUSPENDED','DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE collectors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(100) NULL,
    vehicle_number VARCHAR(50) NULL,
    service_area VARCHAR(255) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    availability_status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_completed_pickups INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_collectors_user (user_id),
    KEY idx_collectors_service_area (service_area),
    KEY idx_collectors_verification (verification_status),
    KEY idx_collectors_availability (availability_status),
    CONSTRAINT fk_collectors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_collectors_rating CHECK (rating >= 0.00 AND rating <= 5.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pickup_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    collector_id BIGINT NULL,
    waste_type VARCHAR(50) NOT NULL,
    quantity_kg DECIMAL(10,2) NOT NULL,
    pickup_address TEXT NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    preferred_date DATE NOT NULL,
    preferred_time TIME NULL,
    notes TEXT NULL,
    image_url TEXT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    collected_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pickups_client_request (client_request_id),
    KEY idx_pickups_user_created (user_id, created_at),
    KEY idx_pickups_collector_status (collector_id, status),
    KEY idx_pickups_status_date (status, preferred_date),
    CONSTRAINT fk_pickups_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pickups_collector FOREIGN KEY (collector_id) REFERENCES collectors(id) ON DELETE RESTRICT,
    CONSTRAINT chk_pickups_quantity CHECK (quantity_kg > 0),
    CONSTRAINT chk_pickups_status CHECK (status IN ('REQUESTED','ACCEPTED','COLLECTOR_ASSIGNED','ON_THE_WAY','COLLECTED','COMPLETED','CANCELLED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE communities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    locality VARCHAR(255) NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_communities_creator (created_by),
    KEY idx_communities_locality_status (locality, status),
    CONSTRAINT fk_communities_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE community_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    community_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    membership_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_community_members (community_id, user_id),
    KEY idx_community_members_user (user_id),
    CONSTRAINT fk_community_members_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    community_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    event_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    location VARCHAR(255) NOT NULL,
    event_type VARCHAR(80) NULL,
    capacity INT NULL,
    image_url TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_events_community_date (community_id, event_date),
    KEY idx_events_creator (created_by),
    KEY idx_events_status_date (status, event_date),
    CONSTRAINT fk_events_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_events_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_events_capacity CHECK (capacity IS NULL OR capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    registered_at DATETIME(6) NOT NULL,
    cancelled_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_registrations (event_id, user_id),
    KEY idx_event_registrations_user (user_id),
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT NULL,
    activity_type VARCHAR(80) NULL,
    image_url TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_posts_user_created (user_id, created_at),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_likes (post_id, user_id),
    KEY idx_post_likes_user (user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comments_post_created (post_id, created_at),
    KEY idx_comments_user (user_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
