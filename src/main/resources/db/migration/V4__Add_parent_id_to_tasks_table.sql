ALTER TABLE tasks ADD COLUMN parent_id BIGINT after id;

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_parent
FOREIGN KEY (parent_id) REFERENCES tasks(id) ON DELETE SET NULL;
