package com.isil.edu.pe.spedidos.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnulacionPedidoServiceImplTest {

    private PedidoRepository pedidoRepository;
    private AnulacionPedidoRepository anulacionRepository;
    private DevolucionStockClient devolucionStockClient;
    private AnulacionPedidoServiceImpl anulacionService;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        anulacionRepository = mock(AnulacionPedidoRepository.class);
        devolucionStockClient = mock(DevolucionStockClient.class);

        anulacionService = new AnulacionPedidoServiceImpl(
                pedidoRepository,
                anulacionRepository,
                devolucionStockClient
        );
    }

    @Test
    void debeAnularPedidoYRegistrarLaAnulacion() {
        Pedido pedido = nuevoPedido();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(anulacionRepository.findByPedidoId(1L)).thenReturn(Optional.empty());
        when(anulacionRepository.save(any(AnulacionPedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnulacionPedido resultado = anulacionService.anular(1L, "Cliente canceló");

        assertEquals(PedidoEstado.ANULADO, pedido.getEstado());
        assertEquals(2, resultado.getCantidadDevuelta());
        verify(devolucionStockClient, times(1)).devolverStock("LAP-001", 2);
        verify(anulacionRepository).save(any(AnulacionPedido.class));
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void debeRechazarAnulacionDuplicadaSinDevolverStock() {
        Pedido pedido = nuevoPedido();
        pedido.setEstado(PedidoEstado.ANULADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        AnulacionException error = assertThrows(
                AnulacionException.class,
                () -> anulacionService.anular(1L, "Otra vez")
        );

        assertEquals(409, error.getStatus());
        verify(devolucionStockClient, never()).devolverStock(anyString(), anyInt());
        verify(anulacionRepository, never()).save(any(AnulacionPedido.class));
    }

    @Test
    void noDebeConfirmarCambiosSiInventarioFalla() {
        Pedido pedido = nuevoPedido();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(anulacionRepository.findByPedidoId(1L)).thenReturn(Optional.empty());
        when(devolucionStockClient.devolverStock("LAP-001", 2))
                .thenThrow(new InventarioNoDisponibleException("Inventario apagado"));

        assertThrows(
                InventarioNoDisponibleException.class,
                () -> anulacionService.anular(1L, "Cliente canceló")
        );

        assertEquals(PedidoEstado.REGISTRADO, pedido.getEstado());
        verify(anulacionRepository, never()).save(any(AnulacionPedido.class));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    private Pedido nuevoPedido() {
        return new Pedido(
                1L,
                "Ana Torres",
                "LAP-001",
                "Laptop",
                new BigDecimal("2500.00"),
                2,
                new BigDecimal("5000.00"),
                LocalDateTime.now()
        );
    }
}
