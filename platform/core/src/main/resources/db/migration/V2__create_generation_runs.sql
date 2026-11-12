CREATE TABLE generation_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    snapshot_id UUID NOT NULL,
    generator_id VARCHAR(160) NOT NULL,
    generator_version VARCHAR(40) NOT NULL,
    status VARCHAR(24) NOT NULL,
    output_sha256 CHAR(64) NOT NULL,
    generated_file_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_generation_runs_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_generation_runs_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES specification_snapshots (id),
    CONSTRAINT ck_generation_runs_file_count CHECK (generated_file_count > 0)
);
CREATE INDEX idx_generation_runs_project_created
    ON generation_runs (project_id, created_at DESC);
