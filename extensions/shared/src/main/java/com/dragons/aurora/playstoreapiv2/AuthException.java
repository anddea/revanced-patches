package com.dragons.aurora.playstoreapiv2;

public class AuthException extends GooglePlayException {

    private String twoFactorUrl;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, int code) {
        super(message);
        setCode(code);
    }

    public String getTwoFactorUrl() {
        return twoFactorUrl;
    }

    public void setTwoFactorUrl(String twoFactorUrl) {
        this.twoFactorUrl = twoFactorUrl;
    }
}
