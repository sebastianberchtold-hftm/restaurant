package com.blogbackend;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class InventoryService {

    @Inject
    @Channel("inventory-out")
    Emitter<String> inventoryEmitter;

    @Incoming("orders-in")
    public void onOrderReceived(String orderMessage) {
        System.out.println("Received order: " + orderMessage);

        // Hier könnte man z.B. den Lagerbestand prüfen oder in einer DB schauen
        // Wir senden anschließend eine Nachricht zurück aufs Topic "inventory"
        String response = "Inventory updated for order: " + orderMessage;
        inventoryEmitter.send(response);

        System.out.println("Response sent to 'inventory' topic: " + response);
    }
}