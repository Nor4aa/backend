package com.sportaccess.controller;

import com.sportaccess.model.*;
import com.sportaccess.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;

    public ReservationController(ReservationRepository rr, UserRepository ur, CourtRepository cr) {
        this.reservationRepository = rr;
        this.userRepository = ur;
        this.courtRepository = cr;
    }

    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation res) {
        try {
            System.out.println("=== NUEVA RESERVA ===");
            String uid = res.getUser().getFirebaseUid();
            String email = res.getUser().getEmail();
            Long courtId = res.getCourt().getId();

            System.out.println(">>> INTENTO DE RESERVA:");
            System.out.println("  - UID: " + uid);
            System.out.println("  - Email: " + email);
            System.out.println("  - Pista ID: " + courtId);
            System.out.println("  - Inicio: " + res.getFechaInicio());

            if (uid == null && email == null) {
                return ResponseEntity.status(400).body("Error: No se proporcionó UID ni Email del usuario");
            }

            // BUSQUEDA INTELIGENTE DEL USUARIO
            User user = userRepository.findByFirebaseUid(uid)
                    .orElseGet(() -> userRepository.findByEmail(email).orElseGet(() -> {
                        System.out.println(">>> Usuario no encontrado. Creando nuevo perfil para: " + (email != null ? email : uid));
                        User u = new User();
                        u.setFirebaseUid(uid);
                        u.setEmail(email != null ? email : uid + "@firebase.com");
                        u.setNombre(res.getUser().getNombre() != null ? res.getUser().getNombre() : "Usuario");
                        u.setTelefono("000000000"); // VALOR POR DEFECTO PARA EVITAR EL ERROR SQL 1048
                        u.setRol(User.Role.USER);
                        return userRepository.save(u);
                    }));

            Court court = courtRepository.findById(courtId)
                    .orElseThrow(() -> new RuntimeException("Pista no encontrada con ID: " + courtId));

            Reservation reservation = new Reservation();
            reservation.setCourt(court);
            reservation.setUser(user);
            reservation.setFechaInicio(res.getFechaInicio());
            reservation.setFechaFin(res.getFechaFin());
            reservation.setQrToken(res.getQrToken() != null ? res.getQrToken() : "QR_" + System.currentTimeMillis());
            reservation.setEstado(Reservation.ReservationStatus.CONFIRMADA);

            Reservation saved = reservationRepository.save(reservation);
            System.out.println("=== RESERVA GUARDADA CON ÉXITO ID: " + saved.getId() + " ===");
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
