package com.isil.edu.pe.spedidos.exception;

public class AnulacionException extends RuntimeException {

    private final int status;

    public AnulacionException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
