CREATE TABLE user_favorite_advertisements (
    user_id BIGINT NOT NULL,
    advertisement_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, advertisement_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ad FOREIGN KEY (advertisement_id) REFERENCES advertisements(id) ON DELETE CASCADE
);