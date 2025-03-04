package com.blogbackend;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders") // Der Tabellenname in der DB
public class OrderEntity extends PanacheEntity {
    public String product;
    public int quantity;
}
