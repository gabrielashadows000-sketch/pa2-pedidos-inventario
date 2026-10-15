package com.isil.edu.pe.spedidos.service.impl;

import com.isil.edu.pe.spedidos.client.DevolucionStockClient;
import com.isil.edu.pe.spedidos.client.InventarioClientException;
import com.isil.edu.pe.spedidos.entity.AnulacionPedido;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.entity.PedidoEstado;
import com.isil.edu.pe.spedidos.exception.AnulacionException;
import com.isil.edu.pe.spedidos.repository.AnulacionPedidoRepository;
import com.isil.edu.pe.spedidos.repository.PedidoRepository;
import com.isil.edu.pe.spedidos.service.AnulacionPedidoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnulacionPedidoServiceImpl implements AnulacionPedidoService {

    private final PedidoRepository pedidoRepository;
    private final AnulacionPedidoRepository anulacionRepository;
    private final DevolucionStockClient devolucionStockClient;

    public AnulacionPedidoServiceImpl(
            PedidoRepository pedidoRepository,
            AnulacionPedidoRepository anulacionRepository,
            DevolucionStockClient devolucionStockClient) {
        this.pedidoRepository = pedidoRepository;
        this.anulacionRepository = anulacionRepository;
        this.devolucionStockClient = devolucionStockClient;
    }

    @Override
    @Transactional
    public AnulacionPedido anular(Long pedidoId, String motivo) {
        if (pedidoId == null) {
            throw new AnulacionException(400, "El id del pedido es obligatorio.");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new AnulacionException(404,
                        "No existe el pedido con id " + pedidoId + "."));

        if (pedido.getEstado() == PedidoEstado.ANULADO
                || anulacionRepository.findByPedidoId(pedidoId).isPresent()) {
            throw new AnulacionException(409, "El pedido " + pedidoId + " ya fue anulado.");
        }

        try {
            devolucionStockClient.devolverStock(pedido.getProductoCodigo(), pedido.getCantidad());
        } catch (InventarioClientException e) {
            throw new AnulacionException(502,
                    "Inventario rechazó la devolución: " + e.getMessage());
        }

        AnulacionPedido anulacion = new AnulacionPedido(
                pedidoId, pedido.getProductoCodigo(), pedido.getCantidad(), motivo);
        AnulacionPedido guardada = anulacionRepository.save(anulacion);

        pedido.setEstado(PedidoEstado.ANULADO);
        pedidoRepository.save(pedido);

        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnulacionPedido> listar() {
        return anulacionRepository.findAll();
    }
}
