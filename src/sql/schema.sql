SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS user_profiles;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- users
-- =========================
CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 PK',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '로그인 아이디',
    email VARCHAR(100) UNIQUE COMMENT '이메일 (선택)',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    nickname VARCHAR(50) NOT NULL COMMENT '닉네임',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);

-- =========================
-- posts
-- =========================
CREATE TABLE posts (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '게시글 PK',
    user_id INT UNSIGNED NOT NULL COMMENT '작성자 ID',
    title VARCHAR(200) NOT NULL COMMENT '제목',
    content TEXT NOT NULL COMMENT '내용',
    view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '조회수',
    comments_cnt INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '댓글수',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    INDEX idx_posts_user_id (user_id),
    INDEX idx_posts_created_at (created_at),

    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

-- =========================
-- comments
-- =========================
CREATE TABLE comments (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '댓글 PK',
    post_id INT UNSIGNED NOT NULL COMMENT '게시글 ID',
    user_id INT UNSIGNED NOT NULL COMMENT '댓글 작성자 ID',
    comment VARCHAR(500) NOT NULL COMMENT '댓글 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    INDEX idx_comments_post_id (post_id),
    INDEX idx_comments_user_id (user_id),

    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id),

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE user_profiles (
  user_id INT UNSIGNED NOT NULL,
  bio VARCHAR(300) NULL,
  phone VARCHAR(20) NULL,
  birth_date DATE NULL,
  profile_image_url VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_profiles_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;