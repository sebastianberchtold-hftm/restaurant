package com.blogbackend;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class OrderService {

    @Inject
    @Channel("orders-out")
    Emitter<String> ordersEmitter; // Zum Senden an Topic "orders"

    public void processOrder(OrderRequest request) {
        // 1) In DB speichern
        OrderEntity entity = new OrderEntity();
        entity.product = request.product;
        entity.quantity = request.quantity;
        entity.persist();  // Panache: speichert direkt in DB

        // 2) Kafka-Nachricht an Topic "orders" senden
        String message = "New order ID=" + entity.id
                + " (product=" + entity.product + ", quantity=" + entity.quantity + ")";
        ordersEmitter.send(message);

        System.out.println("Order saved + message sent: " + message);
    }
}