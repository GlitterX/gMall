package com.gmall.decoration.interfaces.http;

import com.gmall.decoration.application.DecorationRejectedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DecorationHttpExceptionHandler {

    @ExceptionHandler(DecorationRejectedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleRejected(DecorationRejectedException exception) {
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

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalState(IllegalStateException exception) {
        return Map.of("detail", exception.getMessage());
    }
}
