package com.smarthr.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String notFound(ResourceNotFoundException ex, Model model) {
        return render(model, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String noResource(Model model) {
        return render(model, HttpStatus.NOT_FOUND, "The page you asked for does not exist.");
    }

    @ExceptionHandler({BusinessException.class, MethodArgumentTypeMismatchException.class})
    public String badRequest(Exception ex, Model model) {
        String msg = ex instanceof BusinessException ? ex.getMessage() : "One of the values in the request is not valid.";
        return render(model, HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(Exception.class)
    public String generic(Exception ex, HttpServletRequest request, Model model) throws Exception {
        // Let Spring Security deal with its own exceptions (login redirect / 403 page).
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) throw ex;
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return render(model, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.");
    }

    private String render(Model model, HttpStatus status, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        return "error";
    }
}
