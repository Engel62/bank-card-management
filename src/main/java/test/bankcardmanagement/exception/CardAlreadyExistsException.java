package test.bankcardmanagement.exception;

public class CardAlreadyExistsException extends RuntimeException {
    public CardAlreadyExistsException(String message) {
        super(message);
    }
    public CardAlreadyExistsException(String cardNumber, Long userId) {
        super(String.format("Card ending with %s already exists for user %d",
                cardNumber.substring(cardNumber.length() - 4), userId));
    }
}