package model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Book.java
 * Represents a book in the library catalogue.
 *
 * Tracks both catalogue metadata (title, author, ISBN, etc.) and
 * inventory state (total copies, available copies, shelf location).
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Catalogue Metadata ───────────────────────────────────────────────
    /** Unique system-generated book ID, e.g. "BK-0001" */
    private String bookId;
    /** Full title of the book */
    private String title;
    /** Primary author name */
    private String author;
    /** 13-digit ISBN */
    private String isbn;
    /** Genre / category, e.g. "Fiction", "Science", "History" */
    private String genre;
    /** Library category label for filtering and browsing */
    private String category;
    /** Optional image path for book cover uploads */
    private String imagePath;
    /** Publisher name */
    private String publisher;
    /** Year of publication */
    private int    publicationYear;
    /** Brief description or synopsis */
    private String description;
    /** Language the book is written in */
    private String language;
    /** Number of pages */
    private int    pages;

    // ─── Inventory State ──────────────────────────────────────────────────
    /** Total physical copies in the library */
    private int  totalCopies;
    /** Copies currently on the shelf (not borrowed) */
    private int  availableCopies;
    /** Physical shelf / rack location code */
    private String location;
    /** Date this book was added to the catalogue */
    private LocalDate addedDate;
    /** Whether this book is active in the system (false = soft-deleted) */
    private boolean active;

    // ─── Constructors ─────────────────────────────────────────────────────

    /**
     * Full constructor – used when loading from storage.
     */
    public Book(String bookId, String title, String author, String isbn,
                String genre, String publisher, int publicationYear,
                String description, String language, int pages,
                int totalCopies, int availableCopies, String location) {
        this.bookId          = bookId;
        this.title           = title;
        this.author          = author;
        this.isbn            = isbn;
        this.genre           = genre;
        this.category        = genre;
        this.imagePath       = "";
        this.publisher       = publisher;
        this.publicationYear = publicationYear;
        this.description     = description;
        this.language        = language;
        this.pages           = pages;
        this.totalCopies     = totalCopies;
        this.availableCopies = availableCopies;
        this.location        = location;
        this.addedDate       = LocalDate.now();
        this.active          = true;
    }

    /**
     * Quick constructor for simple book creation (sets defaults).
     */
    public Book(String bookId, String title, String author, String isbn, String genre) {
        this(bookId, title, author, isbn, genre, "Unknown", 2024,
                "", "English", 0, 1, 1, "General");
    }

    // ─── Availability Logic ───────────────────────────────────────────────

    /**
     * @return true if at least one copy is on the shelf.
     */
    public boolean isAvailable() {
        return availableCopies > 0 && active;
    }

    /**
     * Decrements available copies when a book is issued.
     *
     * @return true if a copy was reserved successfully.
     */
    public boolean issueOneCopy() {
        if (availableCopies > 0) {
            availableCopies--;
            return true;
        }
        return false;
    }

    /**
     * Increments available copies when a book is returned.
     */
    public void returnOneCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    /**
     * Adds new physical copies to the library inventory.
     *
     * @param count Number of copies to add (must be > 0).
     */
    public void addCopies(int count) {
        if (count > 0) {
            totalCopies     += count;
            availableCopies += count;
        }
    }

    /**
     * Returns the number of copies currently on loan.
     */
    public int getBorrowedCopies() {
        return totalCopies - availableCopies;
    }

    // ─── Soft Delete ─────────────────────────────────────────────────────

    /** Marks the book as inactive (soft-delete). */
    public void deactivate() { this.active = false; }

    /** Re-activates a previously deactivated book. */
    public void activate()   { this.active = true; }

    // ─── Getters & Setters ────────────────────────────────────────────────

    public String getBookId()          { return bookId; }
    public void   setBookId(String id) { this.bookId = id; }

    public String getTitle()           { return title; }
    public void   setTitle(String t)   { this.title = t; }

    public String getAuthor()          { return author; }
    public void   setAuthor(String a)  { this.author = a; }

    public String getIsbn()            { return isbn; }
    public void   setIsbn(String i)    { this.isbn = i; }

    public String getGenre()           { return genre; }
    public void   setGenre(String g)   { this.genre = g; }

    public String getCategory()        { return category; }
    public void   setCategory(String c){ this.category = c; }

    public String getImagePath()       { return imagePath; }
    public void   setImagePath(String p){ this.imagePath = p; }

    public String getPublisher()       { return publisher; }
    public void   setPublisher(String p){ this.publisher = p; }

    public int  getPublicationYear()   { return publicationYear; }
    public void setPublicationYear(int y) { this.publicationYear = y; }

    public String getDescription()     { return description; }
    public void   setDescription(String d) { this.description = d; }

    public String getLanguage()        { return language; }
    public void   setLanguage(String l){ this.language = l; }

    public int  getPages()             { return pages; }
    public void setPages(int p)        { this.pages = p; }

    public int  getTotalCopies()       { return totalCopies; }
    public void setTotalCopies(int t)  { this.totalCopies = t; }

    public int  getAvailableCopies()   { return availableCopies; }
    public void setAvailableCopies(int a) { this.availableCopies = a; }

    public String getLocation()        { return location; }
    public void   setLocation(String l){ this.location = l; }

    public LocalDate getAddedDate()    { return addedDate; }
    public void setAddedDate(LocalDate d) { this.addedDate = d; }

    public boolean isActive()          { return active; }
    public void    setActive(boolean a){ this.active = a; }

    // ─── Serialisation ────────────────────────────────────────────────────

    /**
     * Serialises the book to a pipe-delimited string for flat-file storage.
     * Format: bookId|title|author|isbn|genre|publisher|publicationYear|
     *         description|language|pages|totalCopies|availableCopies|location|active
     */
    public String toFileString() {
        return bookId + "|" + title + "|" + author + "|" + isbn + "|"
                + genre  + "|" + category + "|" + imagePath + "|" + publisher + "|" + publicationYear + "|"
                + description + "|" + language + "|" + pages + "|"
                + totalCopies + "|" + availableCopies + "|" + location + "|" + active;
    }

    @Override
    public String toString() {
        return "Book{id='" + bookId + "', title='" + title
                + "', author='" + author + "', available=" + availableCopies
                + "/" + totalCopies + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Book)) return false;
        Book other = (Book) obj;
        return bookId != null && bookId.equals(other.bookId);
    }

    @Override
    public int hashCode() {
        return bookId == null ? 0 : bookId.hashCode();
    }
}