package com.izhiliu.erp.web.rest.errors;

import com.izhiliu.core.Exception.AbstractException;
import com.izhiliu.erp.config.BodyValidStatus;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.web.rest.util.HeaderUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.*;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.validation.ConstraintViolationProblem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807)
 */
@Slf4j
@ControllerAdvice
public class ExceptionTranslator implements ProblemHandling {
    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo handleProductExceptionInfo;

    /**
     * Post-process Problem payload to add the message key for front-end if needed
     */
    @Override
     public ResponseEntity<Problem> process(@Nullable ResponseEntity<Problem> entity, NativeWebRequest request) {
        if (entity == null || entity.getBody() == null) {
            return entity;
        }
        Problem problem = entity.getBody();
        if (!(problem instanceof ConstraintViolationProblem || problem instanceof DefaultProblem)) {
            final String code = UUID.randomUUID().toString();
            Exception problem1 = (Exception) problem;
            log.error("code:{"+code+"} message:{"+problem1.getMessage()+"} e",problem1);
          return   abstractThrowableProblemProcessor(entity,request,code);
//            return entity;
        }
        ProblemBuilder builder = Problem.builder()
            .withType(Problem.DEFAULT_TYPE.equals(problem.getType()) ? ErrorConstants.DEFAULT_TYPE : problem.getType())
            .withStatus(problem.getStatus())
            .withTitle(problem.getTitle())
            .with("path", request.getNativeRequest(HttpServletRequest.class).getRequestURI());

        if (problem instanceof ConstraintViolationProblem) {
            builder
                .with("violations", ((ConstraintViolationProblem) problem).getViolations())
                .with("message", ErrorConstants.ERR_VALIDATION);
            return new ResponseEntity<>(builder.build(), entity.getHeaders(), entity.getStatusCode());
        } else {
            builder
                .withCause(((DefaultProblem) problem).getCause())
                .withDetail(problem.getDetail())
                .withInstance(problem.getInstance());
            problem.getParameters().forEach(builder::with);
            if (!problem.getParameters().containsKey("message") && problem.getStatus() != null) {
                builder.with("message", "error.http." + problem.getStatus().getStatusCode());
            }
            return new ResponseEntity<>(builder.build(), entity.getHeaders(), entity.getStatusCode());
        }
    }

    @Override
    public ResponseEntity<Problem> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @Nonnull NativeWebRequest request) {
        BindingResult result = ex.getBindingResult();
        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
            .map(f -> new FieldErrorVM(f.getObjectName(), f.getField(), f.getDefaultMessage()))
            .collect(Collectors.toList());

        Problem problem = Problem.builder()
            .withType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE)
            .withTitle("Method argument not valid ".concat(handleProductExceptionInfo.doMessage(fieldErrors.iterator().next().getMessage())))
//            .withStatus(defaultConstraintViolationStatus())
            .withStatus(Status.INTERNAL_SERVER_ERROR)
            .with("message", ErrorConstants.ERR_VALIDATION)
            .with("fieldErrors", fieldErrors)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Problem> handleNoSuchElementException(NoSuchElementException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.NOT_FOUND)
            .with("message", ErrorConstants.ENTITY_NOT_FOUND_TYPE)
            .build();
        return create(ex, problem, request);
    }

    @ExceptionHandler(BadRequestAlertException.class)
    public ResponseEntity<Problem> handleBadRequestAlertException(BadRequestAlertException ex, NativeWebRequest request) {
        handleProductExceptionInfo.country(ex);
        return create(ex, request, HeaderUtil.createFailureAlert(ex.getEntityName(), ex.getErrorKey(), ex.getMessage()));
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Problem> handleConcurrencyFailure(ConcurrencyFailureException ex, NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withStatus(Status.CONFLICT)
            .with("message", ErrorConstants.ERR_CONCURRENCY_FAILURE)
            .build();
        return create(ex, problem, request);
    }



    @SuppressWarnings("rawtypes")
//    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<?> handle(Exception e, NativeWebRequest request) {
        final String code = UUID.randomUUID().toString();
        log.error("code:{"+code+"} message:{"+e.getMessage()+"} e",e);
        if(e instanceof MethodArgumentNotValidException){
            ResponseEntity bodyValidStatus = doMethodArgumentNotValidExceptionProcessor((MethodArgumentNotValidException) e, code);
            if (bodyValidStatus != null) return bodyValidStatus;
        }
        return ResponseEntity.status(500).body( com.izhiliu.erp.config.BodyValidStatus.builder().code(code).title(handleProductExceptionInfo.doMessage("system.error")).field(e.getMessage()+"").type(e.toString()).build());
    }

    public ResponseEntity<Problem> abstractThrowableProblemProcessor(ResponseEntity<Problem> responseEntity, NativeWebRequest request, String code){
        final Problem e = responseEntity.getBody();
        if(e instanceof  BadRequestAlertException){
            BadRequestAlertException x = (BadRequestAlertException) e;
            handleProductExceptionInfo.country(x);
            return ResponseEntity.status(500).body(x);
        }else if(e instanceof AbstractException){
            AbstractException x = (AbstractException) e;
            if(x.isGlobalization()){
                handleProductExceptionInfo.country(x);
            }
            x.setCode(code);
            return ResponseEntity.status(500).body(x);
        }else if(e instanceof AbstractThrowableProblem){
           return responseEntity;
        }
        return null;
    }


    private ResponseEntity doMethodArgumentNotValidExceptionProcessor(MethodArgumentNotValidException e, String code) {
        MethodArgumentNotValidException x = e;
        BindingResult bindingResult = x.getBindingResult();
        if (bindingResult.hasErrors() && bindingResult.hasFieldErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            BodyValidStatus bodyValidStatus = BodyValidStatus.builder().code(code)
                .title(fieldError.getDefaultMessage())
                .field(fieldError.getField()).build();
            log.warn(bodyValidStatus.getTitle() + x);
            return new ResponseEntity<>(bodyValidStatus, HttpStatus.OK);
        }
        return null;
    }

}
