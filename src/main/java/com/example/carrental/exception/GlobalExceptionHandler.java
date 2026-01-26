package com.example.carrental.exception;

import com.example.carrental.dto.exception.ErrorResponse;
import com.example.carrental.exception.base.EntityNotFoundException;
import com.example.carrental.exception.car.CarUnavailableException;
import com.example.carrental.exception.car.LicensePlateAlreadyExistsException;
import com.example.carrental.exception.rental.RentalAlreadyFinishedException;
import com.example.carrental.exception.user.UserAlreadyExistsException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RentalAlreadyFinishedException.class)
    public ResponseEntity<ErrorResponse> handleRentalAlreadyFinishedException(CarUnavailableException ex) {
        log.warn(ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CarUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCarUnavailableException(CarUnavailableException ex) {
        log.info(ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LicensePlateAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleLicensePlateAlreadyExistsException(LicensePlateAlreadyExistsException ex) {
        log.warn("License plate already exists: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Object> handleExpiredJwtException(ExpiredJwtException ex) {
        log.warn("Expired JWT Exception: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Object> handleSignatureException(SignatureException ex) {
        log.warn("Signature Exception: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<Object> handleMalformedJwtException(MalformedJwtException ex) {
        log.warn("Malformed JWT: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Object> handleGeneralJwtException(JwtException ex) {
        log.warn("JWT Exception: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request

    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<ObjectError> validationErrors = ex.getBindingResult().getAllErrors();

        for (ObjectError error : validationErrors) {
            String fieldName;
            if (error instanceof FieldError fe) {
                fieldName = fe.getField();
            } else {
                fieldName = error.getObjectName();
            }
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        }

        log.warn("Validation failed: {}", errors);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Something went wrong. Please try again later."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
