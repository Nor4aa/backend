package com.sportaccess.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    private ReservationStatus estado = ReservationStatus.PENDIENTE;

    // ESTO ES LO QUE FALTABA:
    private String qrToken;

    public enum ReservationStatus {
        PENDIENTE, CONFIRMADA, CANCELADA
    }

    public Reservation() {}

    // --- GETTERS Y SETTERS MANUALES ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Court getCourt() { return court; }
    public void setCourt(Court court) { this.court = court; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public ReservationStatus getEstado() { return estado; }
    public void setEstado(ReservationStatus estado) { this.estado = estado; }

    // GETTER Y SETTER PARA QRTOKEN
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}

