package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionRequest;
import com.isil.edu.pe.spedidos.controller.dto.AnulacionResponse;
import com.isil.edu.pe.spedidos.controller.dto.ErrorResponse;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.exception.AnulacionException;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anulaciones")
public class AnulacionPedidoController {

    private final AnulacionPedidoService anulacionService;

    public AnulacionPedidoController(AnulacionPedidoService anulacionService) {
        this.anulacionService = anulacionService;
    }

    @PostMapping
    public ResponseEntity<AnulacionResponse> anular(@RequestBody AnulacionRequest request) {
        AnulacionPedido anulacion = anulacionService.anular(request.pedidoId(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(AnulacionResponse.from(anulacion));
    }

    @GetMapping
    public List<AnulacionResponse> listar() {
        return anulacionService.listar()
                .stream()
                .map(AnulacionResponse::from)
                .toList();
    }

    @ExceptionHandler(AnulacionException.class)
    public ResponseEntity<ErrorResponse> manejarAnulacion(AnulacionException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(new ErrorResponse(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(InventarioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarInventarioNoDisponible(
            InventarioNoDisponibleException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, e.getMessage()));
    }
}
