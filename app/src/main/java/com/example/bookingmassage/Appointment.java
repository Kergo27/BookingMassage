package com.example.bookingmassage;

public class Appointment {
    private String appointmentId;
    private String userId;
    private String timeSlotId;
    private String date;
    private String time;
    private String massageType; // LEGYEN PONTOSAN EZ A NÉV
    private long bookingTimestamp;

    public Appointment() {
        // Firebase-hez szükséges üres konstruktor
    }

    // Konstruktor - ELLENŐRIZD, HOGY A massageType PARAMÉTER HELYESEN VAN-E FELHASZNÁLVA
    public Appointment(String userId, String timeSlotId, String date, String time, String massageType) {
        this.userId = userId;
        this.timeSlotId = timeSlotId;
        this.date = date;
        this.time = time;
        this.massageType = massageType; // EZ A SOR FONTOS: this.massageType = massageType
        this.bookingTimestamp = System.currentTimeMillis();
    }

    // Getterek és Setterek - ELLENŐRIZD A NEVEKET
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

    public String getMassageType() { return massageType; } // EZ A GETTER FONTOS
    public void setMassageType(String massageType) { this.massageType = massageType; } // EZ A SETTER FONTOS

    public long getBookingTimestamp() { return bookingTimestamp; }
    public void setBookingTimestamp(long bookingTimestamp) { this.bookingTimestamp = bookingTimestamp; }

    // toString() metódus - ELLENŐRIZD, HOGY A HELYES MEZŐNEVET HASZNÁLJA-E
    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", userId='" + userId + '\'' +
                ", timeSlotId='" + timeSlotId + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", massageType='" + massageType + '\'' + // ITT IS massageType LEGYEN
                ", bookingTimestamp=" + bookingTimestamp +
                '}';
    }
}
