package test.bankcardmanagement.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }
    public CardNotFoundException(Long id) {
        super("Card not found with id: " + id);
    }
}