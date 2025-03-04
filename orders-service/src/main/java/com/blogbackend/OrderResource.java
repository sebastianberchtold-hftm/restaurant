package com.blogbackend;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/orders")
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    public Response createOrder(OrderRequest request) {
        orderService.processOrder(request);
        return Response.ok("Order processed successfully").build();
    }
}