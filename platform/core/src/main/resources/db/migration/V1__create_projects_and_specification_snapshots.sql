CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_projects_created_at ON projects (created_at DESC);
CREATE TABLE specification_snapshots (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    snapshot_version INTEGER NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    specification_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_specification_snapshots_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uq_specification_snapshots_project_version
        UNIQUE (project_id, snapshot_version),
    CONSTRAINT ck_specification_snapshots_version CHECK (snapshot_version > 0)
);
CREATE INDEX idx_specification_snapshots_project_created
    ON specification_snapshots (project_id, created_at DESC);
