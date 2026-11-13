ALTER TABLE projects
    ADD COLUMN owner_subject VARCHAR(255) NOT NULL DEFAULT 'legacy';

CREATE INDEX idx_projects_owner_created_at
    ON projects (owner_subject, created_at DESC);
