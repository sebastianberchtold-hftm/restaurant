package com.blogbackend;

import io.smallrye.graphql.api.Subscription;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.util.List;
import java.util.concurrent.Flow;

@ApplicationScoped
@GraphQLApi
public class OrderGraphQLResource {

    @Inject
    OrderService orderService;

    @Inject
    @Channel("inventory-in")
    Multi<String> orderUpdates;

    @Query
    public List<OrderEntity> orders() {
        return OrderEntity.listAll();
    }

    // Query: Liefert eine einzelne Order anhand der ID
    @Query
    public OrderEntity order(Long id) {
        return OrderEntity.findById(id);
    }

    // Mutation: Erstellt eine neue Order
    @Mutation
    @Transactional
    public OrderEntity createOrder(String product, int quantity) {
        OrderEntity order = new OrderEntity();
        order.product = product;
        order.quantity = quantity;
        order.persist();

        // Sende die Nachricht nur, wenn der Kafka-Emitter vorhanden ist
        if (orderService.ordersEmitter != null) {
            orderService.ordersEmitter.send("New order ID=" + order.id
                    + " (product=" + product + ", quantity=" + quantity + ")");
        } else {
            System.err.println("Kafka Emitter is not initialized!");
        }

        return order;
    }

    @Subscription
    public Multi<String> orderUpdates() {
        return orderUpdates;
    }
}
