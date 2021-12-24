package dev.keva.core.exception;

/**
 * StartupException indicates any fatal error encountered during server boot.
 */
public class StartupException extends RuntimeException{

    public StartupException(final String msg, final Throwable cause){
        super(msg, cause);
    }
}
