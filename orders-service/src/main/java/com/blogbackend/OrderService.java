package com.blogbackend;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class OrderService {

    @Inject
    @Channel("orders-out")
    Emitter<String> ordersEmitter;

    @Transactional
    public void processOrder(OrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.product = request.product;
        entity.quantity = request.quantity;
        entity.persist();

        String message = "New order ID=" + entity.id
                + " (product=" + entity.product + ", quantity=" + entity.quantity + ")";
        ordersEmitter.send(message);

        System.out.println("Order saved + message sent: " + message);
    }
}
