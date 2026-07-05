package controller;

import config.AppConfig;
import model.Loan;
import model.Book;
import model.Fine;
import service.LoanService;
import service.BookService;
import service.FineService;
import service.ReservationService;
import service.AuthService;
import model.User;
import util.IDGenerator;
import exception.BookNotFoundException;
import exception.InvalidInputException;
import java.util.Comparator;
import java.util.List;

/**
 * LoanController.java
 * Manages book borrowing and return operations,
 * and handles fine calculation and management.
 */
public class LoanController {

    private LoanService loanService;
    private BookService bookService;
    private FineService fineService;
    private ReservationService reservationService;

    public LoanController() {
        this.loanService = new LoanService();
        this.bookService = new BookService();
        this.fineService = new FineService();
        this.reservationService = new ReservationService();
    }

    // ─── Issue Book (Borrow) ───────────────────────────────────────────────

    /**
     * Issues a book to a student (creates a new loan).
     *
     * @param loanId Unique loan identifier.
     * @param bookId The book being borrowed.
     * @param userId The student borrowing the book.
     * @param issuedById The librarian processing the issue.
     * @return The created Loan object.
     * @throws BookNotFoundException if book does not exist.
     * @throws InvalidInputException if student cannot borrow.
     */
    public Loan issueBook(String loanId, String bookId, String userId, String issuedById)
            throws BookNotFoundException, InvalidInputException {
        return issueBook(loanId, bookId, userId, issuedById, null);
    }

    public Loan issueBook(String loanId, String bookId, String userId, String issuedById, User actingUser)
            throws BookNotFoundException, InvalidInputException {
        AuthService authService = new AuthService();
        if (actingUser != null && !authService.canBorrowForOtherUser(actingUser)) {
            String actingUserBorrowerId = actingUser.getLibraryId();
            if (actingUserBorrowerId == null || actingUserBorrowerId.trim().isEmpty()) {
                actingUserBorrowerId = actingUser.getUserId();
            }
            if (userId != null && !userId.trim().isEmpty() && !userId.trim().equals(actingUserBorrowerId)) {
                throw new InvalidInputException("userId", "Only librarians and admins can borrow books for another member.");
            }
        }

        Book book = bookService.getBook(bookId);
        if (book == null) {
            throw new BookNotFoundException(bookId);
        }
        long outstandingCount = loanService.getLoansByUser(userId).stream()
                .filter(l -> !Loan.STATUS_RETURNED.equals(l.getStatus()))
                .count();
        if (outstandingCount >= AppConfig.MAX_BORROW_LIMIT) {
            throw new InvalidInputException("userId", "User has reached the maximum borrow limit.");
        }

        List<model.Reservation> reservations = reservationService.getBookReservations(bookId);
        model.Reservation nextReservation = reservations.stream()
                .filter(r -> model.Reservation.STATUS_ACTIVE.equals(r.getStatus()))
                .min(Comparator.comparingInt(model.Reservation::getQueuePosition))
                .orElse(null);
        if (nextReservation != null && !nextReservation.getUserId().equals(userId)) {
            throw new InvalidInputException("bookId", "This book is reserved by another member.");
        }

        if (!book.isAvailable()) {
            throw new InvalidInputException("bookId", "Book is not available for borrowing");
        }

        Loan loan = new Loan(loanId, bookId, userId, issuedById);
        book.issueOneCopy();
        bookService.updateBook(book);
        loanService.createLoan(loan);

        return loan;
    }

    /**
     * Returns a borrowed book and calculates any fines.
     *
     * @param loanId The loan to process.
     * @param returnedBy Optional remark about who processed the return.
     * @return The updated Loan object.
     */
    public Loan returnBook(String loanId, String returnedBy) {
        Loan loan = loanService.getLoan(loanId);
        if (loan != null) {
            loan.processReturn(returnedBy);

            // Update book availability
            Book book = bookService.getBook(loan.getBookId());
            if (book != null) {
                book.returnOneCopy();
                bookService.updateBook(book);
            }

            loanService.updateLoan(loan);

            if (loan.getFine() > 0.0 && !loan.isFinePaid()) {
                Fine fine = new Fine(IDGenerator.generateFineId(), loan.getLoanId(), loan.getUserId(), loan.getFine(), "Overdue return fine");
                fine.setPaymentMethod("UNPAID");
                fineService.createFine(fine);
            }

            if (loan != null && reservationService.getBookReservations(loan.getBookId()) != null) {
                List<model.Reservation> bookReservations = reservationService.getBookReservations(loan.getBookId());
                if (!bookReservations.isEmpty()) {
                    model.Reservation first = bookReservations.stream()
                            .filter(r -> model.Reservation.STATUS_ACTIVE.equals(r.getStatus()))
                            .min(Comparator.comparingInt(model.Reservation::getQueuePosition))
                            .orElse(null);
                    if (first != null) {
                        first.setStatus(model.Reservation.STATUS_FULFILLED);
                        reservationService.updateReservation(first);
                    }
                }
            }
        }
        return loan;
    }

    // ─── Loan Status Queries ──────────────────────────────────────────────

    /**
     * Retrieves all active loans for a student.
     *
     * @param userId The student's ID.
     * @return List of active loans.
     */
    public List<Loan> getStudentLoans(String userId) {
        return loanService.getLoansByUser(userId);
    }

    /**
     * Retrieves all overdue loans in the system.
     *
     * @return List of overdue loans.
     */
    public List<Loan> getOverdueLoans() {
        return loanService.getOverdueLoans();
    }

    /**
     * Retrieves a specific loan by ID.
     *
     * @param loanId The loan identifier.
     * @return The Loan object or null if not found.
     */
    public Loan getLoan(String loanId) {
        return loanService.getLoan(loanId);
    }

    /**
     * Checks if a loan is overdue.
     *
     * @param loanId The loan to check.
     * @return true if overdue.
     */
    public boolean isLoanOverdue(String loanId) {
        Loan loan = loanService.getLoan(loanId);
        return loan != null && loan.isOverdue();
    }

    // ─── Fine Management ──────────────────────────────────────────────────

    /**
     * Calculates the fine for a specific loan.
     *
     * @param loanId The loan to calculate fine for.
     * @return Fine amount in currency units.
     */
    public double calculateFine(String loanId) {
        Loan loan = loanService.getLoan(loanId);
        if (loan != null) {
            return loan.calculateFine();
        }
        return 0.0;
    }

    /**
     * Marks a fine as paid.
     *
     * @param loanId The loan whose fine is being paid.
     * @return true if successfully marked as paid.
     */
    public boolean payFine(String loanId) {
        Loan loan = loanService.getLoan(loanId);
        boolean loanUpdated = false;
        if (loan != null) {
            loan.setFinePaid(true);
            loanUpdated = loanService.updateLoan(loan);
        }
        boolean fineUpdated = fineService.markFinePaidByLoanId(loanId, "CASH");
        return loanUpdated || fineUpdated;
    }

    /**
     * Retrieves all loans with unpaid fines.
     *
     * @return List of loans with outstanding fines.
     */
    public List<Loan> getLoansWithFines() {
        return loanService.getLoansWithUnpaidFines();
    }

    /**
     * Retrieves all loans in the system.
     *
     * @return List of all loans.
     */
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }
}
