package com.freelancehub.controller.web;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.ForbiddenException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.exception.UnauthorizedException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/** Friendly error pages for the Thymeleaf MVC controllers (never a raw stack trace). */
@ControllerAdvice(basePackages = "com.freelancehub.controller.web")
public class WebExceptionHandler {

    private ModelAndView page(HttpStatus status, String title, String message) {
        return new ModelAndView("error/generic",
                Map.of("status", status.value(), "heading", title, "detail", message), status);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String unauthorized(UnauthorizedException ex) {
        return "redirect:/login?required";
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView forbidden(ForbiddenException ex) {
        return page(HttpStatus.FORBIDDEN, "Access denied", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException ex) {
        return page(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView businessRule(BusinessRuleException ex) {
        return page(HttpStatus.BAD_REQUEST, "Request not allowed", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public ModelAndView badRequest(Exception ex) {
        return page(HttpStatus.BAD_REQUEST, "Bad request", "Some of the submitted values were invalid.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView conflict(DataIntegrityViolationException ex) {
        return page(HttpStatus.CONFLICT, "Conflict", "That action conflicts with existing data (duplicate or still in use).");
    }
}
