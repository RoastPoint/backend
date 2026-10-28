package ru.roastpoint;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import java.time.Instant;
import java.util.List;
@RestControllerAdvice
public class ErrorHandler {
 private static final Logger log=LoggerFactory.getLogger(ErrorHandler.class);
 record ErrorResponse(Instant timestamp,int status,String message,List<String> details) {}
 private ResponseEntity<ErrorResponse> response(HttpStatus status,String message,List<String> details) {
  return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(),status.value(),message,details));
 }
 @ExceptionHandler(ApiException.class)
 ResponseEntity<ErrorResponse> domain(ApiException e) { return response(e.status,e.getMessage(),List.of()); }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
  return response(HttpStatus.BAD_REQUEST,"Ошибка входных данных",e.getBindingResult().getFieldErrors().stream()
   .map(f->f.getField()+": "+f.getDefaultMessage()).toList());
 }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,ConstraintViolationException.class,
  org.springframework.web.bind.MissingServletRequestParameterException.class})
 ResponseEntity<ErrorResponse> invalid(Exception e) { return response(HttpStatus.BAD_REQUEST,"Проверьте JSON, типы и значения параметров",List.of()); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<ErrorResponse> integrity(Exception e) { log.warn("Database constraint violation",e); return response(HttpStatus.CONFLICT,"Нарушено ограничение данных",List.of()); }
 @ExceptionHandler({DataAccessResourceFailureException.class,TransientDataAccessException.class})
 ResponseEntity<ErrorResponse> unavailable(Exception e) { log.error("Database unavailable",e); return response(HttpStatus.SERVICE_UNAVAILABLE,"База данных временно недоступна",List.of()); }
 @ExceptionHandler(NoResourceFoundException.class)
 ResponseEntity<ErrorResponse> notFound(Exception e) { return response(HttpStatus.NOT_FOUND,"Маршрут не найден",List.of()); }
 @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
 ResponseEntity<ErrorResponse> method(Exception e) { return response(HttpStatus.METHOD_NOT_ALLOWED,"Метод не поддерживается",List.of()); }
 @ExceptionHandler(Exception.class)
 ResponseEntity<ErrorResponse> other(Exception e) { log.error("Unexpected error",e); return response(HttpStatus.INTERNAL_SERVER_ERROR,"Внутренняя ошибка сервера",List.of()); }
}
