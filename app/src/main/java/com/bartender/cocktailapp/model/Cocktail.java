package com.bartender.cocktailapp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo principal de un cóctel.
 * Implementa Serializable para poder pasar objetos entre Activities/Fragments.
 */
public class Cocktail implements Serializable {

    private long id;
    private String name;
    private String description;
    private String imageUrl;       // URL o ruta local de la imagen
    private String procedure;      // Preparación paso a paso

    // Ingredientes separados por categoría
    private List<String> alcohols;   // Lista de alcoholes
    private List<String> mixers;     // Lista de mezclas

    // Opciones adicionales
    private boolean hasSmoke;        // ¿Tiene humo?
    private boolean hasTopping;      // ¿Tiene topping?
    private String toppingDescription; // Descripción del topping (opcional)

    // Sistema de valoraciones
    private List<Rating> ratings;    // Lista de votos individuales

    // Constructor vacío requerido para base de datos
    public Cocktail() {
        this.alcohols = new ArrayList<>();
        this.mixers = new ArrayList<>();
        this.ratings = new ArrayList<>();
    }

    // Constructor completo
    public Cocktail(String name, String description, String imageUrl, String procedure) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.procedure = procedure;
        this.alcohols = new ArrayList<>();
        this.mixers = new ArrayList<>();
        this.ratings = new ArrayList<>();
    }

    // ─── Getters y Setters ─────────────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getProcedure() { return procedure; }
    public void setProcedure(String procedure) { this.procedure = procedure; }

    public List<String> getAlcohols() { return alcohols; }
    public void setAlcohols(List<String> alcohols) { this.alcohols = alcohols; }

    public List<String> getMixers() { return mixers; }
    public void setMixers(List<String> mixers) { this.mixers = mixers; }

    public boolean isHasSmoke() { return hasSmoke; }
    public void setHasSmoke(boolean hasSmoke) { this.hasSmoke = hasSmoke; }

    public boolean isHasTopping() { return hasTopping; }
    public void setHasTopping(boolean hasTopping) { this.hasTopping = hasTopping; }

    public String getToppingDescription() { return toppingDescription; }
    public void setToppingDescription(String toppingDescription) { this.toppingDescription = toppingDescription; }

    public List<Rating> getRatings() { return ratings; }
    public void setRatings(List<Rating> ratings) { this.ratings = ratings; }

    /**
     * Calcula el promedio de puntuación de todas las valoraciones.
     * @return promedio entre 1.0 y 6.0, o 0.0 si no hay votos
     */
    public float getAverageRating() {
        if (ratings == null || ratings.isEmpty()) return 0f;
        float sum = 0f;
        for (Rating r : ratings) {
            sum += r.getStars();
        }
        return sum / ratings.size();
    }

    /**
     * Añade una nueva valoración al cóctel.
     */
    public void addRating(Rating rating) {
        if (this.ratings == null) this.ratings = new ArrayList<>();
        this.ratings.add(rating);
    }
}
