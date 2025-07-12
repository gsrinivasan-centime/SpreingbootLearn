-- Create databases if they don't exist
CREATE DATABASE IF NOT EXISTS bookstore_books;
CREATE DATABASE IF NOT EXISTS bookstore_users;

-- Create users and grant permissions
CREATE USER IF NOT EXISTS 'bookstore_user'@'%' IDENTIFIED BY 'bookstore_pass';
GRANT ALL PRIVILEGES ON bookstore_books.* TO 'bookstore_user'@'%';
GRANT ALL PRIVILEGES ON bookstore_users.* TO 'bookstore_user'@'%';
FLUSH PRIVILEGES;

-- Use the books database
USE bookstore_books;

-- Create a sample books table (if Liquibase doesn't handle it)
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    price DECIMAL(10,2),
    publication_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert some sample data
INSERT IGNORE INTO books (title, author, isbn, price, publication_date) VALUES
('The Spring Framework Guide', 'John Doe', '978-1234567890', 29.99, '2024-01-15'),
('Microservices with Spring Boot', 'Jane Smith', '978-0987654321', 39.99, '2024-02-20'),
('Docker for Developers', 'Bob Johnson', '978-1122334455', 34.99, '2024-03-10');

-- Use the users database
USE bookstore_users;

-- Create a sample users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert some sample users
INSERT IGNORE INTO users (first_name, last_name, email, phone_number) VALUES
('Alice', 'Wonder', 'alice@example.com', '+1-555-0101'),
('Bob', 'Builder', 'bob@example.com', '+1-555-0102'),
('Charlie', 'Brown', 'charlie@example.com', '+1-555-0103');
