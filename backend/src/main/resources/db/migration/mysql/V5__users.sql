CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  picture VARCHAR(1024),
  google_id VARCHAR(255),
  role VARCHAR(50) NOT NULL DEFAULT 'OWNER',
  created_at DATETIME NOT NULL,
  last_login_at DATETIME NOT NULL
);

CREATE TABLE user_tokens (
  token VARCHAR(128) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_user_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO users (email, name, picture, role, created_at, last_login_at)
VALUES ('rakesh@sharmapackaging.in', 'Rakesh Sharma', NULL, 'OWNER', NOW(), NOW());
