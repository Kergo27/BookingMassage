package com.example.bookingmassage;

public class TimeSlot {
    private String id; // Egyedi azonosító, pl. "2025-05-20_08:00"
    private String date; // Formátum: "yyyy-MM-dd"
    private String time; // Formátum: "HH:00" (csak az órát tároljuk, pl. "08:00", "09:00")
    private boolean available;
    private String bookedByUserId;    // Ki foglalta le (ha foglalt)
    private String bookedMassageType; // Milyen masszázst foglaltak ide (ha foglalt)

    public TimeSlot() {
        // Firebase-hez szükséges üres konstruktor
    }

    public TimeSlot(String id, String date, String time) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.available = true; // Alapból elérhető
        this.bookedByUserId = null;
        this.bookedMassageType = null;
    }

    // Getterek és Setterek
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getBookedByUserId() { return bookedByUserId; }
    public void setBookedByUserId(String bookedByUserId) { this.bookedByUserId = bookedByUserId; }
    public String getBookedMassageType() { return bookedMassageType; }
    public void setBookedMassageType(String bookedMassageType) { this.bookedMassageType = bookedMassageType; }

    @Override
    public String toString() {
        return "TimeSlot{" +
                "id='" + id + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", available=" + available +
                ", bookedByUserId='" + bookedByUserId + '\'' +
                ", bookedMassageType='" + bookedMassageType + '\'' +
                '}';
    }
}
