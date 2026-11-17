CREATE TABLE refresh_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6) NULL,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                PRIMARY KEY (id),
                                CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);