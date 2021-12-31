package one.digitalinnovation.beerstock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NegativeBeerStockException extends Exception {

    public NegativeBeerStockException(Long id, int quantityToDecrement,int quantity) {
        super(String.format("Beers with %s ID and quantity %s to decrement informed exceeds the quantity in stock: %s", id, quantity,quantityToDecrement));
    }
}
