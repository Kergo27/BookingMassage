package com.example.bookingmassage;

public class Appointment {
    private String appointmentId;     // Ennek a foglalási bejegyzésnek az egyedi ID-ja
    private String userId;            // A felhasználó UID-ja
    private String timeSlotId;        // Az eredeti TimeSlot ID-ja (pl. "2025-05-20_08:00")
    private String date;              // A foglalás dátuma
    private String time;              // A foglalás ideje
    private String serviceType;       // A választott masszázs típusa
    private long bookingTimestamp;    // Mikor történt a foglalás

    public Appointment() {
        // Firebase-hez szükséges üres konstruktor
    }

    public Appointment(String userId, String timeSlotId, String date, String time, String massageType) {
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.date = date;
        this.time = time;
        this.serviceType = serviceType;
        this.bookingTimestamp = System.currentTimeMillis(); // Foglalás ideje most
    }

    // Getterek és Setterek
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public long getBookingTimestamp() { return bookingTimestamp; }
    public void setBookingTimestamp(long bookingTimestamp) { this.bookingTimestamp = bookingTimestamp; }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", userId='" + userId + '\'' +
                ", timeSlotId='" + timeSlotId + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", bookingTimestamp=" + bookingTimestamp +
                '}';
    }
}
