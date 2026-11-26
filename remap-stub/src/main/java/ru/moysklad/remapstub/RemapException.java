package ru.moysklad.remapstub;

public class RemapException extends RuntimeException {

    private final int httpStatus;
    private final Integer code;

    public RemapException(int httpStatus, Integer code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public Integer code() {
        return code;
    }

    public static RemapException notFound(String message) {
        return new RemapException(404, null, message);
    }

    public static RemapException precondition(String message) {
        return new RemapException(412, null, message);
    }

    public static RemapException conflict(String message) {
        return new RemapException(409, null, message);
    }

    public static RemapException badRequest(String message) {
        return new RemapException(400, null, message);
    }
}
