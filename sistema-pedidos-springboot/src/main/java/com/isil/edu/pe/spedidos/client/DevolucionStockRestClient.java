package com.isil.edu.pe.spedidos.client;

import com.isil.edu.pe.spedidos.client.dto.ApiError;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockRequest;
import com.isil.edu.pe.spedidos.client.dto.DevolucionStockResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DevolucionStockRestClient implements DevolucionStockClient {

    private final RestClient restClient;
    private final String baseUrl;

    public DevolucionStockRestClient(
            @Qualifier("inventarioHttpClient") RestClient restClient,
            @Value("${inventario.api.base-url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public DevolucionStockResponse devolverStock(String codigo, int cantidad) {
        DevolucionStockRequest request = new DevolucionStockRequest(cantidad);

        try {
            return restClient
                    .post()
                    .uri(baseUrl + "/productos/codigo/{codigo}/devoluciones", codigo)
                    .body(request)
                    .retrieve()
                    .body(DevolucionStockResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();

            if (e.getStatusCode().is5xxServerError()) {
                throw new InventarioNoDisponibleException(
                        "inventario-service respondió HTTP " + status + ".");
            }
            throw new InventarioClientException(status, obtenerMensaje(e));
        } catch (ResourceAccessException e) {
            throw new InventarioNoDisponibleException(
                    "El servicio de inventario no se encuentra disponible o excedió el tiempo de espera.",
                    e);
        }
    }

    private String obtenerMensaje(RestClientResponseException e) {
        String mensaje = "inventario-service respondió HTTP " + e.getStatusCode().value() + ".";

        try {
            ApiError error = e.getResponseBodyAs(ApiError.class);
            if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
                mensaje = error.getMessage();
            }
        } catch (RuntimeException ignored) {
            // si el cuerpo no tiene el formato esperado, se conserva el mensaje genérico
        }

        return mensaje;
    }
}
