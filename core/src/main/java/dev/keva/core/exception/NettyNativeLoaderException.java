package dev.keva.core.exception;

/**
 * NettyNativeLoaderException indicates that the native library could not be loaded.
 */
public class NettyNativeLoaderException extends Exception {
    public NettyNativeLoaderException(String message) {
        super(message);
    }
}
