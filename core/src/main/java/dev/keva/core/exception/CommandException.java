package dev.keva.core.exception;

/**
 * CommandException indicates that a command has failed to execute.
 */
public class CommandException extends RuntimeException {
    public CommandException(String s) {
        super(s);
    }
}
