package com.isil.edu.pe.spedidos.client.dto;

public class DevolucionStockRequest {
    private int cantidad;

    public DevolucionStockRequest() {
    }

    public DevolucionStockRequest(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
