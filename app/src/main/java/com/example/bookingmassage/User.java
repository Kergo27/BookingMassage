package com.example.bookingmassage;

public class User {
    private String uid;
    private String email;
    private String phoneNumber;

    public User() {
        // Firebase-hez szükséges üres konstruktor
    }

    public User(String uid, String email, String phoneNumber) {
        this.uid = uid;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getterek és Setterek
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
