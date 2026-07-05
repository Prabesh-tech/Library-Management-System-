package test;

import model.Book;

public class BookMetadataTests {
    public static void main(String[] args) {
        Book book = new Book("BK-9999", "Test Book", "Test Author", "978-0000000000", "Fiction");
        book.setCategory("Programming");
        book.setImagePath("images/test-book.png");

        if (!"Programming".equals(book.getCategory())) {
            throw new AssertionError("Category should be stored on the book model");
        }
        if (!"images/test-book.png".equals(book.getImagePath())) {
            throw new AssertionError("Image path should be stored on the book model");
        }

        System.out.println("Book metadata tests passed.");
    }
}
