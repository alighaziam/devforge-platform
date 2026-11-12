CREATE TABLE architecture_decisions (
 id UUID PRIMARY KEY, project_id UUID NOT NULL, snapshot_id UUID NOT NULL,
 title VARCHAR(240) NOT NULL, context_text TEXT NOT NULL, decision_text TEXT NOT NULL,
 alternatives_json TEXT NOT NULL, consequences_json TEXT NOT NULL, status VARCHAR(24) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 row_version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_arch_dec_project FOREIGN KEY (project_id) REFERENCES projects(id),
 CONSTRAINT fk_arch_dec_snapshot FOREIGN KEY (snapshot_id) REFERENCES specification_snapshots(id)
);
CREATE INDEX idx_arch_dec_project_created ON architecture_decisions(project_id, created_at DESC);
CREATE TABLE architecture_decision_events (
 id UUID PRIMARY KEY, decision_id UUID NOT NULL, from_status VARCHAR(24),
 to_status VARCHAR(24) NOT NULL, comment_text VARCHAR(2000) NOT NULL,
 actor VARCHAR(160) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT fk_arch_dec_event FOREIGN KEY (decision_id) REFERENCES architecture_decisions(id)
);
CREATE INDEX idx_arch_dec_events_decision ON architecture_decision_events(decision_id, created_at ASC);
