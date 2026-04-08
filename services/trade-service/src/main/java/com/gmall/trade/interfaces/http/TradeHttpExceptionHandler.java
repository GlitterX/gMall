package com.gmall.trade.interfaces.http;

import com.gmall.trade.application.EligibilityNotFoundException;
import com.gmall.trade.application.OrderRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TradeHttpExceptionHandler {

    @ExceptionHandler(EligibilityNotFoundException.class)
    ProblemDetail handleEligibilityNotFound(EligibilityNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Eligibility Not Found");
        return problemDetail;
    }

    @ExceptionHandler(OrderRejectedException.class)
    ProblemDetail handleOrderRejected(OrderRejectedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Order Rejected");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid Trade Request");
        return problemDetail;
    }
}
