-- Book Service Database Initialization
CREATE DATABASE IF NOT EXISTS bookstore_books CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user for book service
CREATE USER IF NOT EXISTS 'book_user'@'%' IDENTIFIED BY 'book_password';
GRANT ALL PRIVILEGES ON bookstore_books.* TO 'book_user'@'%';

-- Switch to book database
USE bookstore_books;

-- Categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

-- Books table
CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(13) UNIQUE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    category ENUM('FICTION', 'NON_FICTION', 'SCIENCE', 'TECHNOLOGY', 'BIOGRAPHY', 'MYSTERY', 'ROMANCE') NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    
    INDEX idx_category (category),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_available (available),
    INDEX idx_created_at (created_at),
    
    CONSTRAINT chk_price CHECK (price > 0),
    CONSTRAINT chk_stock CHECK (stock >= 0)
);

-- Book reviews table
CREATE TABLE book_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_book_id (book_id),
    INDEX idx_user_id (user_id),
    INDEX idx_rating (rating),
    
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Inventory transactions table
CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    transaction_type ENUM('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    reference_id VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_book_id (book_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_reference_id (reference_id),
    INDEX idx_created_at (created_at)
);

-- Insert sample data
INSERT INTO categories (name, description) VALUES
('Fiction', 'Fictional literature and novels'),
('Technology', 'Technical and programming books'),
('Science', 'Scientific research and discoveries'),
('Biography', 'Life stories and biographies'),
('Mystery', 'Mystery and thriller novels');

INSERT INTO books (title, author, isbn, price, stock, category, description, available) VALUES
('Clean Code', 'Robert C. Martin', '9780132350884', 299.99, 50, 'TECHNOLOGY', 'A Handbook of Agile Software Craftsmanship', true),
('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 149.99, 30, 'FICTION', 'A classic American novel', true),
('Sapiens', 'Yuval Noah Harari', '9780062316097', 399.99, 25, 'SCIENCE', 'A Brief History of Humankind', true),
('Spring Boot in Action', 'Craig Walls', '9781617292545', 449.99, 40, 'TECHNOLOGY', 'Spring Boot guide for developers', true),
('To Kill a Mockingbird', 'Harper Lee', '9780061120084', 199.99, 35, 'FICTION', 'A novel about racial injustice', true),
('Steve Jobs', 'Walter Isaacson', '9781451648539', 349.99, 20, 'BIOGRAPHY', 'Biography of Apple co-founder', true),
('The Da Vinci Code', 'Dan Brown', '9780307474278', 249.99, 45, 'MYSTERY', 'Mystery thriller novel', true);

FLUSH PRIVILEGES;
