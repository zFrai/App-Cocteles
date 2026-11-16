package com.bartender.cocktailapp.model;

import java.io.Serializable;

/**
 * Modelo de una valoración individual de un cóctel.
 * Contiene el nombre del votante y su puntuación (1-6 estrellas).
 */
public class Rating implements Serializable {

    private long id;
    private long cocktailId;   // FK al cóctel
    private String voterName;  // Nombre del votante (ej: "Juan")
    private int stars;         // Puntuación de 1 a 6

    // Constructor vacío
    public Rating() {}

    // Constructor principal
    public Rating(long cocktailId, String voterName, int stars) {
        this.cocktailId = cocktailId;
        this.voterName = voterName;
        this.stars = stars;
    }

    // ─── Getters y Setters ─────────────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCocktailId() { return cocktailId; }
    public void setCocktailId(long cocktailId) { this.cocktailId = cocktailId; }

    public String getVoterName() { return voterName; }
    public void setVoterName(String voterName) { this.voterName = voterName; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    @Override
    public String toString() {
        return voterName + ": " + stars + " ⭐";
    }
}
