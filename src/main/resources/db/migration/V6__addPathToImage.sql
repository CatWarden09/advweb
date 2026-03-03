ALTER TABLE images
    ADD path VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uc_image_path UNIQUE (path),
    ADD CONSTRAINT uc_image_url UNIQUE (url);