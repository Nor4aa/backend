package com.sportaccess.controller;

import com.sportaccess.model.*;
import com.sportaccess.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*") // Permite peticiones desde Flutter
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;

    public ReservationController(ReservationRepository rr, UserRepository ur, CourtRepository cr) {
        this.reservationRepository = rr;
        this.userRepository = ur;
        this.courtRepository = cr;
    }

    // --- 1. OBTENER TODAS LAS RESERVAS (Opcional, para panel de admin) ---
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // --- 2. OBTENER HORARIOS OCUPADOS POR PISTA Y DÍA (Para Flutter) ---
    // URL ejemplo: /api/reservations/court/1/horarios?fecha=2024-05-20
    @GetMapping("/court/{courtId}/horarios")
    public ResponseEntity<List<Reservation>> getReservasPorPistaYDia(
            @PathVariable Long courtId, 
            @RequestParam String fecha) {
        try {
            LocalDate dia = LocalDate.parse(fecha);
            LocalDateTime inicioDelDia = dia.atStartOfDay();
            LocalDateTime finDelDia = dia.atTime(LocalTime.MAX);

            List<Reservation> reservasActivas = reservationRepository.findReservasActivasPorPistaYRango(
                    courtId, inicioDelDia, finDelDia
            );

            return ResponseEntity.ok(reservasActivas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- 3. CREAR NUEVA RESERVA ---
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation res) {
        try {
            String uid = res.getUser().getFirebaseUid();
            String email = res.getUser().getEmail();
            Long courtId = res.getCourt().getId();

            if (uid == null && email == null) {
                return ResponseEntity.badRequest().body("Error: Faltan credenciales de usuario.");
            }

            // A) VALIDACIÓN ESTRICTA DE SOLAPES
            boolean pistaOcupada = reservationRepository.existeSolape(
                    courtId, res.getFechaInicio(), res.getFechaFin()
            );

            if (pistaOcupada) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("La pista ya está ocupada en ese horario.");
            }

            // B) BUSCAR O CREAR USUARIO (Lógica de Firebase)
            User user = userRepository.findByFirebaseUid(uid)
                    .orElseGet(() -> userRepository.findByEmail(email).orElseGet(() -> {
                        User u = new User();
                        u.setFirebaseUid(uid);
                        u.setEmail(email != null ? email : uid + "@firebase.com");
                        u.setNombre(res.getUser().getNombre() != null ? res.getUser().getNombre() : "Usuario");
                        u.setTelefono("000000000"); 
                        u.setRol(User.Role.USER);
                        return userRepository.save(u);
                    }));

            // C) BUSCAR PISTA
            Court court = courtRepository.findById(courtId)
                    .orElseThrow(() -> new RuntimeException("Pista no encontrada"));

            // D) CONSTRUIR Y GUARDAR RESERVA
            Reservation nuevaReserva = new Reservation();
            nuevaReserva.setCourt(court);
            nuevaReserva.setUser(user);
            nuevaReserva.setFechaInicio(res.getFechaInicio());
            nuevaReserva.setFechaFin(res.getFechaFin());
            nuevaReserva.setQrToken(res.getQrToken() != null ? res.getQrToken() : "QR_" + System.currentTimeMillis());
            nuevaReserva.setEstado(Reservation.ReservationStatus.CONFIRMADA);

            Reservation saved = reservationRepository.save(nuevaReserva);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage());
        }
    }

    // --- 4. CANCELAR RESERVA (Borrado Lógico) ---
    // URL ejemplo: /api/reservations/15/cancel
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelarReserva(@PathVariable Long id) {
        try {
            Optional<Reservation> reservaOpt = reservationRepository.findById(id);
            if (reservaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Reservation reserva = reservaOpt.get();
            reserva.setEstado(Reservation.ReservationStatus.CANCELADA);
            reservationRepository.save(reserva);

            return ResponseEntity.ok("Reserva cancelada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al cancelar");
        }
    }
}