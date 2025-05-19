
package com.example.bookingmassage;

public class User {
    private String uid;
    private String email;
    private String name; // ÚJ MEZŐ A NÉVNEK
    // private String userType; // Ha volt ilyen, maradhat

    public User() {
        // Firestore számára szükséges üres konstruktor
    }

    // Konstruktor emaillel és UID-dal (és most névvel)
    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        // this.userType = "client"; // Alapértelmezett felhasználói típus, ha van ilyen
    }

    // Getterek és Setterek
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() { // ÚJ GETTER
        return name;
    }

    public void setName(String name) { // ÚJ SETTER
        this.name = name;
    }

    // public String getUserType() { return userType; }
    // public void setUserType(String userType) { this.userType = userType; }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' + // Név hozzáadása a toString-hez
                '}';
    }
}
