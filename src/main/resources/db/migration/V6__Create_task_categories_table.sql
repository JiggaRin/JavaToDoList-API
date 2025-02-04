CREATE TABLE task_categories (
    task_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, category_id),
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);
