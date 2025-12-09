package com.dragons.aurora.playstoreapiv2;

import java.io.IOException;

public class GooglePlayException extends IOException {

    protected int code;

    public GooglePlayException(String message) {
        super(message);
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
