-- name: create-task-table!
CREATE TABLE IF NOT EXISTS task (
	id INTEGER PRIMARY KEY,
	title VARCHAR(255))

-- name: list-tasks
SELECT id, title FROM task ORDER BY id DESC

-- name: next-task-id
SELECT MAX(id)+1 AS newid FROM task

-- name: new-task!
INSERT INTO task (id, title) VALUES (:id, :title)

-- name: get-task
SELECT id, title FROM task WHERE id = :id

-- name: update-task!
UPDATE task SET title = :title WHERE id = :id

-- name: delete-task!
DELETE FROM task WHERE id = :id
