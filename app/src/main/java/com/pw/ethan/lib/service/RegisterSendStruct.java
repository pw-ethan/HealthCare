package com.pw.ethan.lib.service;


public class RegisterSendStruct {
    public String name;
    private String email;
    private String password;

    public RegisterSendStruct(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public String toString() {
        return "name:" + name + " email:" + email + " password:" + password;
    }
}
