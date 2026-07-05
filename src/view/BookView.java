package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import controller.BookController;
import controller.LoanController;
import model.Book;
import model.User;
import service.AuthService;
import exception.InvalidInputException;
import util.IDGenerator;
import java.awt.GridLayout;

/**
 * BookView.java
 * Interface for viewing, searching, and managing books.
 */
public class BookView extends JFrame {

    private static final long serialVersionUID = 1L;
    
    private BookController bookController;
    private LoanController loanController;
    private User currentUser;
    private AuthService authService;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public BookView(User currentUser) {
        this.currentUser = currentUser;
        this.bookController = new BookController();
        this.loanController = new LoanController();
        this.authService = new AuthService();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Book Catalogue");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        // Search panel
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setBounds(50, 20, 100, 25);
        searchField = new JTextField();
        searchField.setBounds(150, 20, 300, 25);
        JButton searchButton = new JButton("Search");
        searchButton.setBounds(460, 20, 100, 25);
        searchButton.addActionListener(e -> searchBooks(searchField.getText().trim()));
        
        panel.add(searchLabel);
        panel.add(searchField);
        panel.add(searchButton);

        // Books table
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Title", "Author", "Genre", "Category", "ISBN", "Available", "Total", "Location", "Image"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        booksTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.setBounds(50, 70, 800, 400);
        panel.add(scrollPane);

        // Action buttons
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBounds(50, 490, 100, 30);
        refreshButton.addActionListener(e -> refreshBookTable());
        panel.add(refreshButton);

        JButton addButton = new JButton("Add Book");
        addButton.setBounds(160, 490, 110, 30);
        addButton.setEnabled(currentUser != null && authService.hasPermission(currentUser, "manage_books"));
        addButton.setToolTipText(currentUser != null && authService.hasPermission(currentUser, "manage_books")
                ? "Add a new book to the catalogue"
                : "Only librarians and admins can add books");
        addButton.addActionListener(e -> showAddBookDialog());
        panel.add(addButton);

        JButton editButton = new JButton("Edit Book");
        editButton.setBounds(280, 490, 110, 30);
        editButton.setEnabled(currentUser != null && authService.hasPermission(currentUser, "manage_books"));
        editButton.setToolTipText(currentUser != null && authService.hasPermission(currentUser, "manage_books")
                ? "Edit selected book"
                : "Only librarians and admins can edit books");
        editButton.addActionListener(e -> showEditBookDialog());
        panel.add(editButton);

        JButton deleteButton = new JButton("Delete Book");
        deleteButton.setBounds(400, 490, 110, 30);
        deleteButton.setEnabled(currentUser != null && authService.hasPermission(currentUser, "manage_books"));
        deleteButton.setToolTipText(currentUser != null && authService.hasPermission(currentUser, "manage_books")
                ? "Delete selected book"
                : "Only librarians and admins can delete books");
        deleteButton.addActionListener(e -> showDeleteBookDialog());
        panel.add(deleteButton);

        JButton borrowButton = new JButton("Borrow");
        borrowButton.setBounds(520, 490, 100, 30);
        borrowButton.setEnabled(authService.hasPermission(currentUser, "borrow_books"));
        borrowButton.setToolTipText(authService.hasPermission(currentUser, "borrow_books") ? "Borrow selected book" : "You do not have permission to borrow books");
        borrowButton.addActionListener(e -> borrowSelectedBook());
        panel.add(borrowButton);

        JButton closeButton = new JButton("Close");
        closeButton.setBounds(750, 490, 100, 30);
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);

        add(panel);
        refreshBookTable();
    }

    private void refreshBookTable() {
        loadBooks(bookController.getAllBooks());
    }

    private void searchBooks(String query) {
        if (query == null || query.isEmpty()) {
            refreshBookTable();
            return;
        }
        loadBooks(bookController.searchBooks(query));
    }

    private void borrowSelectedBook() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a book to borrow.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = (String) tableModel.getValueAt(selectedRow, 0);
        String borrowerId;
        if (currentUser != null && !authService.canBorrowForOtherUser(currentUser)) {
            borrowerId = currentUser.getLibraryId();
            if (borrowerId == null || borrowerId.trim().isEmpty()) {
                borrowerId = currentUser.getUserId();
            }
        } else {
            borrowerId = JOptionPane.showInputDialog(this, "Enter member ID for borrowing:", "Borrow Book", JOptionPane.PLAIN_MESSAGE);
        }

        if (borrowerId == null || borrowerId.trim().isEmpty()) {
            return;
        }

        try {
            loanController.issueBook(IDGenerator.generateLoanId(), bookId, borrowerId.trim(), currentUser != null ? currentUser.getUserId() : "SYSTEM", currentUser);
            JOptionPane.showMessageDialog(this, "Book borrowed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshBookTable();
        } catch (InvalidInputException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Borrow failed", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to borrow book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBooks(java.util.List<Book> books) {
        tableModel.setRowCount(0);
        if (books == null) {
            return;
        }

        for (Book book : books) {
            tableModel.addRow(new Object[]{
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getGenre(),
                    book.getCategory() != null ? book.getCategory() : book.getGenre(),
                    book.getIsbn(),
                    book.getAvailableCopies(),
                    book.getTotalCopies(),
                    book.getLocation(),
                    book.getImagePath()
            });
        }
    }

    private void showAddBookDialog() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField isbnField = new JTextField();
        JTextField genreField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField imagePathField = new JTextField("images/default-book.png");
        JTextField publisherField = new JTextField("Unknown");
        JTextField yearField = new JTextField(String.valueOf(java.time.LocalDate.now().getYear()));
        JTextField pagesField = new JTextField("0");
        JTextField copiesField = new JTextField("1");
        JTextField locationField = new JTextField("General");

        form.add(new JLabel("Title:")); form.add(titleField);
        form.add(new JLabel("Author:")); form.add(authorField);
        form.add(new JLabel("ISBN:")); form.add(isbnField);
        form.add(new JLabel("Genre:")); form.add(genreField);
        form.add(new JLabel("Category:")); form.add(categoryField);
        form.add(new JLabel("Image Path:")); form.add(imagePathField);
        form.add(new JLabel("Publisher:")); form.add(publisherField);
        form.add(new JLabel("Publication Year:")); form.add(yearField);
        form.add(new JLabel("Pages:")); form.add(pagesField);
        form.add(new JLabel("Total Copies:")); form.add(copiesField);
        form.add(new JLabel("Location:")); form.add(locationField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String genre = genreField.getText().trim();
        String category = categoryField.getText().trim();
        String imagePath = imagePathField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();
        String pagesText = pagesField.getText().trim();
        String copiesText = copiesField.getText().trim();
        String location = locationField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title, author and genre are required.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int publicationYear = Integer.parseInt(yearText);
            int pages = Integer.parseInt(pagesText);
            int totalCopies = Integer.parseInt(copiesText);

            if (totalCopies <= 0) {
                throw new NumberFormatException();
            }

            Book book = new Book(IDGenerator.generateBookId(), title, author, isbn,
                    genre, publisher, publicationYear, "", "English", pages,
                    totalCopies, totalCopies, location);
            book.setCategory(category.isEmpty() ? genre : category);
            book.setImagePath(imagePath);
            bookController.addBook(book);
            JOptionPane.showMessageDialog(this, "Book added successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshBookTable();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Publication year, pages and total copies must be valid numbers.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidInputException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEditBookDialog() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a book to edit.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = (String) tableModel.getValueAt(selectedRow, 0);
        Book book = null;
        
        try {
            book = bookController.getBook(bookId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Book not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField titleField = new JTextField(book.getTitle() != null ? book.getTitle() : "");
        JTextField authorField = new JTextField(book.getAuthor() != null ? book.getAuthor() : "");
        JTextField isbnField = new JTextField(book.getIsbn() != null ? book.getIsbn() : "");
        JTextField genreField = new JTextField(book.getGenre() != null ? book.getGenre() : "");
        JTextField categoryField = new JTextField(book.getCategory() != null ? book.getCategory() : "");
        JTextField imagePathField = new JTextField(book.getImagePath() != null ? book.getImagePath() : "");
        JTextField publisherField = new JTextField(book.getPublisher() != null ? book.getPublisher() : "");
        JTextField yearField = new JTextField(String.valueOf(book.getPublicationYear()));
        JTextField pagesField = new JTextField(String.valueOf(book.getPages()));
        JTextField copiesField = new JTextField(String.valueOf(book.getTotalCopies()));
        JTextField locationField = new JTextField(book.getLocation() != null ? book.getLocation() : "");

        form.add(new JLabel("Title:")); form.add(titleField);
        form.add(new JLabel("Author:")); form.add(authorField);
        form.add(new JLabel("ISBN:")); form.add(isbnField);
        form.add(new JLabel("Genre:")); form.add(genreField);
        form.add(new JLabel("Category:")); form.add(categoryField);
        form.add(new JLabel("Image Path:")); form.add(imagePathField);
        form.add(new JLabel("Publisher:")); form.add(publisherField);
        form.add(new JLabel("Publication Year:")); form.add(yearField);
        form.add(new JLabel("Pages:")); form.add(pagesField);
        form.add(new JLabel("Total Copies:")); form.add(copiesField);
        form.add(new JLabel("Location:")); form.add(locationField);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String genre = genreField.getText().trim();
        String category = categoryField.getText().trim();
        String imagePath = imagePathField.getText().trim();
        String publisher = publisherField.getText().trim();
        String yearText = yearField.getText().trim();
        String pagesText = pagesField.getText().trim();
        String copiesText = copiesField.getText().trim();
        String location = locationField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || genre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title, author and genre are required.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int publicationYear = Integer.parseInt(yearText);
            int pages = Integer.parseInt(pagesText);
            int totalCopies = Integer.parseInt(copiesText);

            if (totalCopies <= 0) {
                throw new NumberFormatException();
            }

            book.setTitle(title);
            book.setAuthor(author);
            book.setIsbn(isbn);
            book.setGenre(genre);
            book.setCategory(category.isEmpty() ? genre : category);
            book.setImagePath(imagePath);
            book.setPublisher(publisher);
            book.setPublicationYear(publicationYear);
            book.setPages(pages);
            book.setTotalCopies(totalCopies);
            book.setLocation(location);

            bookController.updateBook(book);
            JOptionPane.showMessageDialog(this, "Book updated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshBookTable();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Publication year, pages and total copies must be valid numbers.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating book: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDeleteBookDialog() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a book to delete.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = (String) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this book?\n\n" + title + " (" + bookId + ")\n\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            bookController.deleteBook(bookId);
            JOptionPane.showMessageDialog(this, "Book '" + title + "' deleted successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshBookTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
