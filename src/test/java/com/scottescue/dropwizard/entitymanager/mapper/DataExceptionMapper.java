package com.scottescue.dropwizard.entitymanager.mapper;

import io.dropwizard.jersey.errors.ErrorMessage;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.sql.SQLDataException;

@Provider
public class DataExceptionMapper implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException e) {
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        SQLDataException sqlException = unwrapDataException(e);

        String message = "Wrong input";
        if (sqlException != null && sqlException.getMessage().contains("EMAIL")) {
            message = "Wrong email";
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(), message))
                .build();
    }

    private SQLDataException unwrapDataException(Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            return null;
        }
        if (cause instanceof SQLDataException) {
            return (SQLDataException) cause;
        } else {
            return unwrapDataException(cause);
        }
    }

}
