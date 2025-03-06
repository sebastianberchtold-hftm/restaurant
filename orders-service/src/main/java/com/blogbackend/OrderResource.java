package com.blogbackend;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderRequest request) {
        System.out.println("Received POST with product: " + request.product + " and quantity: " + request.quantity);
        orderService.processOrder(request);
        return Response.ok("{\"message\":\"Order processed successfully\"}").build();
    }

    @GET
    public Response testEndpoint() {
        return Response.ok("{\"status\": \"orders-service is up\"}").build();
    }
}
