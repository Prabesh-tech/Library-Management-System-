package controller;

import model.Reservation;
import service.BookService;
import service.ReservationService;
import service.UserService;
import exception.InvalidInputException;
import model.Book;
import model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReservationController.java
 * Coordinates reservation actions between views and business services.
 */
public class ReservationController {

    private final ReservationService reservationService;
    private final BookService bookService;
    private final UserService userService;

    public ReservationController() {
        this.reservationService = new ReservationService();
        this.bookService = new BookService();
        this.userService = new UserService();
    }

    public Reservation reserveBook(String reservationId, String bookId, String userId)
            throws InvalidInputException {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new InvalidInputException("bookId", "Book ID is required.");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new InvalidInputException("userId", "Member ID is required.");
        }

        Book book = bookService.getBook(bookId);
        if (book == null) {
            throw new InvalidInputException("bookId", "Book not found.");
        }
        User user = userService.getUserByLibraryId(userId);
        if (user == null) {
            throw new InvalidInputException("userId", "Member not found.");
        }

        Reservation reservation = new Reservation(reservationId, bookId, user.getLibraryId(), 0, LocalDateTime.now().plusDays(7));
        if (reservationService.createReservation(reservation)) {
            return reservation;
        }
        throw new InvalidInputException("reservation", "Unable to create reservation.");
    }

    public boolean cancelReservation(String reservationId) {
        return reservationService.cancelReservation(reservationId);
    }

    public List<Reservation> getReservationsForUser(String userId) {
        return reservationService.getUserReservations(userId);
    }

    public List<Reservation> getReservationsForBook(String bookId) {
        return reservationService.getBookReservations(bookId);
    }

    public List<Reservation> getActiveReservations() {
        return reservationService.getActiveReservations();
    }
}
