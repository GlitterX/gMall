package com.gmall.pricinginventory.interfaces.http;

import com.gmall.pricinginventory.application.PricingInventoryRejectedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PricingInventoryHttpExceptionHandler {

    @ExceptionHandler(PricingInventoryRejectedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleRejected(PricingInventoryRejectedException exception) {
        return Map.of(
                "reasonCode", exception.reasonCode(),
                "detail", exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of("detail", exception.getMessage());
    }
}
