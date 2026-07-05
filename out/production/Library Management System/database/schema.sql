-- Create and select the database
CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_db;

-- ==============================
-- Lookup / Reference Tables
-- ==============================

CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE authors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    biography TEXT,
    nationality VARCHAR(100),
    UNIQUE(name)
);

CREATE TABLE publishers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    address TEXT
);

-- ==============================
-- Core Domain Tables
-- ==============================

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    libraryId VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('STAFF','STUDENT','TEACHER','LIBRARIAN','ADMIN','SUPERADMIN') NOT NULL DEFAULT 'STUDENT',
    access_level INT NOT NULL DEFAULT 1,
    phone VARCHAR(30),
    address TEXT,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    CONSTRAINT ux_users_username UNIQUE (username)
);

-- Member-specific profile data kept separate for normalization
CREATE TABLE member_profiles (
    user_id INT PRIMARY KEY,
    student_number VARCHAR(50),
    department VARCHAR(150),
    year_of_study VARCHAR(50),
    membership_type VARCHAR(50) DEFAULT 'STANDARD',
    registration_date DATE DEFAULT (CURRENT_DATE),
    expiry_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(500) NOT NULL,
    publisher_id INT,
    edition VARCHAR(50),
    publication_year YEAR,
    language VARCHAR(50),
    description TEXT,
    category_id INT,
    total_quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers(id)
        ON DELETE SET NULL
);

-- Individual physical copies (allows tracking availability at copy-level)
CREATE TABLE book_copies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    copy_number INT NOT NULL,
    barcode VARCHAR(100) UNIQUE,
    shelf_location VARCHAR(200),
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    UNIQUE (book_id, copy_number)
);

-- Many-to-many between books and authors
CREATE TABLE book_authors (
    book_id INT NOT NULL,
    author_id INT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

-- ==============================
-- Transaction Tables
-- ==============================

CREATE TABLE loans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    copy_id INT NOT NULL,
    borrower_id INT NOT NULL,
    issued_by INT NOT NULL,
    issue_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    status ENUM('ISSUED','RETURNED','OVERDUE','LOST') NOT NULL DEFAULT 'ISSUED',
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (copy_id) REFERENCES book_copies(id),
    FOREIGN KEY (borrower_id) REFERENCES users(id),
    FOREIGN KEY (issued_by) REFERENCES users(id)
);

CREATE TABLE reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    user_id INT NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE','CANCELLED','FULFILLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    queue_position INT NOT NULL DEFAULT 0,
    expires_at DATETIME NULL,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE fines (
    id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id INT NULL,
    user_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid TINYINT(1) NOT NULL DEFAULT 0,
    paid_at TIMESTAMP NULL,
    reason VARCHAR(255),
    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE fine_payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fine_id INT NOT NULL,
    user_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method VARCHAR(50),
    FOREIGN KEY (fine_id) REFERENCES fines(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==============================
-- Auxiliary Tables
-- ==============================

CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    type VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    action VARCHAR(255) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE book_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- ==============================
-- Sample Data (expanded, richer demo seed)
-- ==============================

INSERT INTO authors (name, biography, nationality) VALUES
('Joshua Bloch', 'Author of Effective Java', 'USA'),
('Robert C. Martin', 'Uncle Bob, Clean Code author', 'USA'),
('Gang of Four', 'Authors of Design Patterns', 'USA'),
('David Thomas', 'Co-author of The Pragmatic Programmer', 'USA'),
('Thomas H. Cormen', 'Author of Introduction to Algorithms', 'USA'),
('Donald Knuth', 'Author of The Art of Computer Programming', 'USA'),
('Stuart Russell', 'Author of Artificial Intelligence: A Modern Approach', 'USA'),
('Jennifer Widom', 'Expert in database systems and education', 'USA'),
('Paul Deitel', 'Author of Java programming textbooks', 'USA'),
('Kent Beck', 'Creator of Extreme Programming practices', 'USA'),
('Eric Evans', 'Domain-driven design pioneer', 'USA'),
('Martin Fowler', 'Software design and refactoring expert', 'UK');

INSERT INTO publishers (name, address) VALUES
('O''Reilly Media','1005 Gravenstein Highway North, Sebastopol, CA'),
('Pearson','221 River Street, Hoboken, NJ'),
('Prentice Hall','One Lake Street, Upper Saddle River, NJ'),
('Addison-Wesley','501 Boylston Street, Boston, MA'),
('MIT Press','One Rogers Street, Cambridge, MA'),
('Academic Press','525 B Street, San Diego, CA'),
('Wiley','111 River Street, Hoboken, NJ'),
('Springer','233 Spring Street, New York, NY');

-- Sample users (plain-text passwords matching demo login credentials)
INSERT INTO users (username, libraryId, email, password, role, access_level, phone, address) VALUES
('prabesh','SA-001','prabeshadmin@library.com','super123','SUPERADMIN',5,'+10000000000','Library HQ'),
('admin','ADM-001','admin@library.com','admin123','ADMIN',4,'+10000000001','Main Branch'),
('librarian','LIB-002','librarian@library.com','lib123','LIBRARIAN',3,'+10000000002','Main Branch'),
('ahmed','STU-001','ahmed@university.edu','pass123','STUDENT',1,'+10000000003','789 College Dr'),
('fatima','STU-002','fatima@university.edu','pass456','STUDENT',1,'+10000000004','321 Campus Ln'),
('ali','STU-003','ali@university.edu','pass789','STUDENT',1,'+10000000005','654 Dorm Way'),
('sara','TEA-001','teacher@university.edu','teach123','TEACHER',2,'+10000000006','Faculty Office'),
('mina','STF-001','staff@library.com','staff123','STAFF',0,'+10000000007','Library Support'),
('emily','STU-004','emily@university.edu','study123','STUDENT',1,'+10000000008','210 Campus Cir'),
('jason','STU-005','jason@university.edu','learning','STUDENT',1,'+10000000009','111 Dorm Way');

INSERT INTO member_profiles (user_id, student_number, department, year_of_study, membership_type, registration_date, expiry_date) VALUES
(4,'S1001','Computer Science','Year 2','STANDARD','2026-01-10','2027-01-10'),
(5,'S1002','Engineering','Year 3','STANDARD','2026-02-15','2027-02-15'),
(6,'S1003','Business','Year 1','STANDARD','2026-03-20','2027-03-20'),
(9,'S1004','Information Systems','Year 4','STANDARD','2026-04-05','2027-04-05'),
(10,'S1005','Mathematics','Year 2','STANDARD','2026-05-12','2027-05-12');

-- Add sample catalogue reference data
INSERT INTO categories (id, name, description) VALUES
(1, 'Programming', 'Books related to computer programming and software engineering'),
(2, 'Science', 'General science textbooks and reference'),
(3, 'Literature', 'Novels, poetry and essays'),
(4, 'Computer Science', 'Theory and systems for computing'),
(5, 'Technology', 'Applied technology and engineering'),
(6, 'Database', 'Database design, SQL, and data modeling'),
(7, 'Web Development', 'Web programming, frameworks and applications'),
(8, 'Software Engineering', 'Software architecture, best practices and patterns'),
(9, 'Artificial Intelligence', 'AI, machine learning, robotics and intelligent systems'),
(10, 'Education', 'Academic and teaching resources');

INSERT INTO books (isbn, title, publisher_id, edition, publication_year, language, description, category_id, total_quantity)
VALUES
('9780132350884','Clean Code',3,'1st',2008,'English','How to write clean, maintainable code',1,4),
('9780201633610','Design Patterns',4,'1st',1994,'English','Reusable solutions to common design problems',1,3),
('9780201616224','The Pragmatic Programmer',4,'1st',1999,'English','Your journey to mastery in software development',1,5),
('9780262033848','Introduction to Algorithms',5,'3rd',2009,'English','Comprehensive study of algorithms',4,3),
('9780201896831','The Art of Computer Programming',4,'1st',1997,'English','Fundamental algorithms and programming techniques',4,2),
('9780135492619','Artificial Intelligence',2,'4th',2020,'English','Modern approach to artificial intelligence',5,4),
('9780133970778','Database Design',6,'1st',2008,'English','Relational database design principles',6,3),
('9780132760263','Web Development with Java',3,'1st',2012,'English','Comprehensive guide to web development using Java',6,4);

INSERT INTO book_copies (book_id, copy_number, barcode, shelf_location, is_available) VALUES
(1,1,'BC-0001','A1-01',1),
(1,2,'BC-0002','A1-01',1),
(1,3,'BC-0003','A1-01',1),
(1,4,'BC-0004','A1-01',1),
(2,1,'BC-0005','A1-02',1),
(2,2,'BC-0006','A1-02',1),
(2,3,'BC-0007','A1-02',0),
(3,1,'BC-0008','A1-03',1),
(3,2,'BC-0009','A1-03',1),
(3,3,'BC-0010','A1-03',1),
(3,4,'BC-0011','A1-03',1),
(4,1,'BC-0012','B1-01',1),
(4,2,'BC-0013','B1-01',1),
(4,3,'BC-0014','B1-01',1),
(5,1,'BC-0015','B1-02',1),
(6,1,'BC-0016','C1-01',1),
(6,2,'BC-0017','C1-01',1),
(6,3,'BC-0018','C1-01',1),
(6,4,'BC-0019','C1-01',1),
(7,1,'BC-0020','C2-01',1),
(7,2,'BC-0021','C2-01',1),
(7,3,'BC-0022','C2-01',1),
(8,1,'BC-0023','D1-01',1),
(8,2,'BC-0024','D1-01',0),
(8,3,'BC-0025','D1-01',0),
(9,1,'BC-0026','A2-01',1),
(9,2,'BC-0027','A2-01',1),
(9,3,'BC-0028','A2-01',1),
(10,1,'BC-0029','B2-01',1),
(10,2,'BC-0030','B2-01',1),
(11,1,'BC-0031','B2-02',1),
(11,2,'BC-0032','B2-02',1),
(12,1,'BC-0033','C3-01',1),
(12,2,'BC-0034','C3-01',1),
(12,3,'BC-0035','C3-01',1);

INSERT INTO book_authors (book_id, author_id) VALUES
(1,2),
(2,3),
(3,4),
(4,5),
(5,6),
(6,7),
(7,8),
(8,9),
(9,1),
(10,11),
(11,10),
(12,12);

-- Add sample loans
INSERT INTO loans (copy_id, borrower_id, issued_by, issue_date, due_date, return_date, status, fine_amount)
VALUES
(1,4,3,'2026-06-01 09:00:00','2026-06-15 09:00:00',NULL,'ISSUED',0.00),
(8,5,3,'2026-06-05 11:30:00','2026-06-19 11:30:00',NULL,'ISSUED',0.00),
(12,6,3,'2026-05-10 10:00:00','2026-05-24 10:00:00','2026-05-21 15:45:00','RETURNED',0.00),
(22,9,3,'2026-06-10 14:20:00','2026-06-24 14:20:00',NULL,'ISSUED',0.00),
(24,10,3,'2026-06-12 16:00:00','2026-06-26 16:00:00',NULL,'ISSUED',0.00),
(5,4,3,'2026-05-01 09:30:00','2026-05-15 09:30:00','2026-05-20 10:15:00','OVERDUE',10.00);

INSERT INTO reservations (book_id, user_id, reserved_at, status, queue_position, expires_at)
VALUES
(2,4,'2026-06-20 12:00:00','ACTIVE',1,'2026-06-27 12:00:00'),
(1,5,'2026-06-22 09:15:00','ACTIVE',2,'2026-06-29 09:15:00'),
(4,9,'2026-06-25 14:05:00','ACTIVE',1,'2026-07-02 14:05:00'),
(3,6,'2026-05-20 08:00:00','FULFILLED',1,'2026-05-27 08:00:00');

INSERT INTO fines (loan_id, user_id, amount, issued_at, paid, paid_at, reason)
VALUES
(6,4,10.00,'2026-05-20 10:30:00',1,'2026-05-20 10:45:00','Late return of Clean Code'),
(NULL,6,5.00,'2026-06-14 09:00:00',0,NULL,'Damaged cover charge');

INSERT INTO fine_payments (fine_id, user_id, amount, paid_at, method)
VALUES
(1,4,10.00,'2026-05-20 10:45:00','CARD');

INSERT INTO notifications (user_id, message, sent_at, read_flag, type)
VALUES
(4,'Your reservation for Design Patterns is now active.','2026-06-20 12:05:00',0,'RESERVATION'),
(5,'Book loan due soon: The Pragmatic Programmer.','2026-06-17 08:00:00',0,'LOAN'),
(6,'Your returned book has been processed successfully.','2026-05-21 16:10:00',1,'RETURN'),
(9,'Your reserved book is ready for pickup.','2026-07-02 14:10:00',0,'RESERVATION');

INSERT INTO audit_logs (user_id, action, detail, created_at)
VALUES
(3,'Issue Loan','Loan created for STU-001 on copy BC-0001','2026-06-01 09:01:00'),
(3,'Issue Loan','Loan created for STU-002 on copy BC-0008','2026-06-05 11:34:00'),
(3,'Return Book','Returned loan for STU-003 on copy BC-0012','2026-05-21 15:50:00'),
(3,'Fine Payment','Recorded payment for fine 1 by STU-001','2026-05-20 10:46:00'),
(3,'Reservation','Reservation created for STU-004 on Design Patterns','2026-06-20 12:10:00');

INSERT INTO book_images (book_id, file_path, uploaded_at)
VALUES
(1,'images/books/clean_code.jpg','2026-06-01 08:00:00'),
(8,'images/books/web_development_java.jpg','2026-06-01 08:05:00'),
(9,'images/books/effective_java.jpg','2026-06-02 09:00:00'),
(12,'images/books/hands_on_ml.jpg','2026-06-02 09:10:00');

-- ==============================
-- Indexes for performance
-- ==============================
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_book_copies_barcode ON book_copies(barcode);
CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_reservations_book ON reservations(book_id);

