package pe.com.proveperu.sgc.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties =
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
@AutoConfigureMockMvc
class OpenApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void especificacionOpenApiEsPublicaYContieneLosEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.openapi").value("3.0.1"))
            .andExpect(jsonPath("$.info.title").value("SGC PROVEPERU API"))
            .andExpect(jsonPath("$.info.version").value("v1"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/usuarios'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/roles'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/permisos'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/categorias'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/marcas'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/unidades-medida'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/productos'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/productos/{idProducto}/conversiones'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/productos/{idProducto}/precios'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventario'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventario/stock-bajo'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventario/ajustes'].post.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventario/movimientos'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/kardex/{idProducto}'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/clientes'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/clientes/{id}'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/clientes/{id}/historial'].get.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/clientes/{idCliente}/precios-especiales'].get.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/clientes/{idCliente}/precios-especiales'].post.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/proveedores'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/proveedores/{id}'].get.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/proveedores/{id}/compras'].get.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/transportistas'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/transportistas/{id}'].get.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/transportistas/{id}/gastos'].get.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/gastos'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/gastos'].post.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/compras'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/compras'].post.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/compras/{id}'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/compras/{id}'].put.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/compras/{id}/estado'].patch.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/compras/{id}/gastos'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/compras/{id}/recepciones'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/compras/{id}/recepciones'].get.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/cuentas-pagar'].get.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cuentas-pagar/{id}'].get.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cuentas-pagar/{id}/pagos'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cuentas-pagar/vencidas'].get.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/cotizaciones'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/cotizaciones'].post.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cotizaciones/{id}'].get.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cotizaciones/{id}'].put.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cotizaciones/{id}/estado'].patch.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/cotizaciones/{id}/convertir-pedido'].post.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/pedidos'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/pedidos'].post.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/pedidos/{id}'].get.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/pedidos/{id}/confirmar'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/pedidos/{id}/cancelar'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/pedidos/{id}/estado'].patch.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/pedidos/{id}/reservas'].get.security"
            ).exists())
            .andExpect(jsonPath("$.paths['/api/v1/ventas'].get.security").exists())
            .andExpect(jsonPath("$.paths['/api/v1/ventas'].post.security").exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/ventas/{id}'].get.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/ventas/{id}/anular'].post.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/ventas/{id}/comprobante'].get.security"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/v1/ventas/metodos-pago'].get.security"
            ).exists());
    }

    @Test
    void interfazSwaggerEsPublica() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Swagger UI")));
    }
}
