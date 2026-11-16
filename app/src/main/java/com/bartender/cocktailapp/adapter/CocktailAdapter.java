package com.bartender.cocktailapp.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bartender.cocktailapp.R;
import com.bartender.cocktailapp.model.Cocktail;

import java.util.List;

/**
 * Adapter para el RecyclerView de la lista de cócteles.
 * Cada tarjeta muestra: imagen, nombre, puntuación media y botones de acción.
 */
public class CocktailAdapter extends RecyclerView.Adapter<CocktailAdapter.CocktailViewHolder> {

    /**
     * Interface que la Activity debe implementar para manejar los clicks.
     */
    public interface OnCocktailClickListener {
        void onCocktailClick(Cocktail cocktail);   // Ver detalle
        void onEditClick(Cocktail cocktail);        // Editar
        void onDeleteClick(Cocktail cocktail);      // Eliminar
    }

    private final List<Cocktail> cocktailList;
    private final OnCocktailClickListener listener;

    public CocktailAdapter(List<Cocktail> cocktailList, OnCocktailClickListener listener) {
        this.cocktailList = cocktailList;
        this.listener     = listener;
    }

    @NonNull
    @Override
    public CocktailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cocktail, parent, false);
        return new CocktailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CocktailViewHolder holder, int position) {
        Cocktail cocktail = cocktailList.get(position);
        holder.bind(cocktail, listener);
    }

    @Override
    public int getItemCount() {
        return cocktailList.size();
    }

    // ─── ViewHolder ─────────────────────────────────────────────────────────────

    static class CocktailViewHolder extends RecyclerView.ViewHolder {

        private final ImageView  imageViewThumbnail;
        private final TextView   textViewName;
        private final TextView   textViewDescription;
        private final TextView   textViewRating;
        private final RatingBar  ratingBar;
        private final TextView   textViewSmoke;
        private final TextView   textViewTopping;
        private final Button     btnEdit;
        private final Button     btnDelete;

        public CocktailViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.imageViewThumbnail);
            textViewName       = itemView.findViewById(R.id.textViewCocktailName);
            textViewDescription = itemView.findViewById(R.id.textViewCocktailDescription);
            textViewRating     = itemView.findViewById(R.id.textViewRating);
            ratingBar          = itemView.findViewById(R.id.ratingBarItem);
            textViewSmoke      = itemView.findViewById(R.id.textViewSmokeTag);
            textViewTopping    = itemView.findViewById(R.id.textViewToppingTag);
            btnEdit            = itemView.findViewById(R.id.btnEdit);
            btnDelete          = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Cocktail cocktail, OnCocktailClickListener listener) {
            // Nombre
            textViewName.setText(cocktail.getName());

            // Descripción (máximo 2 líneas en XML)
            String desc = cocktail.getDescription();
            textViewDescription.setText(desc != null && !desc.isEmpty() ? desc : "Sin descripción");

            // Imagen
            if (cocktail.getImageUrl() != null && !cocktail.getImageUrl().isEmpty()) {
                try {
                    imageViewThumbnail.setImageURI(Uri.parse(cocktail.getImageUrl()));
                } catch (Exception e) {
                    imageViewThumbnail.setImageResource(R.drawable.ic_cocktail_placeholder);
                }
            } else {
                imageViewThumbnail.setImageResource(R.drawable.ic_cocktail_placeholder);
            }

            // Valoración
            float avg = cocktail.getAverageRating();
            ratingBar.setNumStars(6);
            ratingBar.setMax(6);
            ratingBar.setRating(avg);
            if (avg > 0) {
                textViewRating.setText(String.format("%.1f ⭐ (%d votos)",
                        avg, cocktail.getRatings().size()));
            } else {
                textViewRating.setText("Sin valoraciones");
            }

            // Tags opcionales
            textViewSmoke.setVisibility(cocktail.isHasSmoke() ? View.VISIBLE : View.GONE);
            textViewTopping.setVisibility(cocktail.isHasTopping() ? View.VISIBLE : View.GONE);

            // Click en la tarjeta → ver detalle
            itemView.setOnClickListener(v -> listener.onCocktailClick(cocktail));

            // Botones de acción
            btnEdit.setOnClickListener(v -> listener.onEditClick(cocktail));
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(cocktail));
        }
    }
}
