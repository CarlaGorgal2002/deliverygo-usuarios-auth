package org.deliverygo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Pruebas")
public class Prueba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    public Prueba() {
    }

    public Prueba(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Prueba{" +
                "text='" + text + '\'' +
                '}';
    }
}
