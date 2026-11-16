package com.bartender.cocktailapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bartender.cocktailapp.R;
import com.bartender.cocktailapp.database.DatabaseHelper;
import com.bartender.cocktailapp.model.Cocktail;
import com.bartender.cocktailapp.model.Rating;

import java.util.List;

/**
 * Pantalla de detalle de un cóctel.
 * Muestra todos los datos: imagen, descripción, ingredientes, procedimiento y valoraciones.
 * Desde aquí se puede añadir una valoración o ir a editar.
 */
public class DetailCocktailActivity extends AppCompatActivity {

    // Vistas
    private ImageView imageViewCocktail;
    private TextView  textViewName, textViewDescription;
    private TextView  textViewAlcohols, textViewMixers;
    private TextView  textViewSmoke, textViewTopping;
    private TextView  textViewProcedure;
    private TextView  textViewAverageRating, textViewRatingCount;
    private LinearLayout layoutRatingsList;
    private Button    btnAddRating;
    private RatingBar ratingBarAverage;

    private DatabaseHelper dbHelper;
    private Cocktail cocktail;
    private long cocktailId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_cocktail);

        // Configurar Toolbar con botón atrás
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper   = DatabaseHelper.getInstance(this);
        cocktailId = getIntent().getLongExtra("cocktail_id", -1);

        // Referenciar todas las vistas
        imageViewCocktail    = findViewById(R.id.imageViewCocktailDetail);
        textViewName         = findViewById(R.id.textViewDetailName);
        textViewDescription  = findViewById(R.id.textViewDetailDescription);
        textViewAlcohols     = findViewById(R.id.textViewDetailAlcohols);
        textViewMixers       = findViewById(R.id.textViewDetailMixers);
        textViewSmoke        = findViewById(R.id.textViewDetailSmoke);
        textViewTopping      = findViewById(R.id.textViewDetailTopping);
        textViewProcedure    = findViewById(R.id.textViewDetailProcedure);
        textViewAverageRating = findViewById(R.id.textViewAverageRating);
        textViewRatingCount  = findViewById(R.id.textViewRatingCount);
        layoutRatingsList    = findViewById(R.id.layoutRatingsList);
        btnAddRating         = findViewById(R.id.btnAddRating);
        ratingBarAverage     = findViewById(R.id.ratingBarAverage);

        // Botón para añadir voto
        btnAddRating.setOnClickListener(v -> showAddRatingDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar cóctel (puede haberse editado)
        if (cocktailId != -1) {
            cocktail = dbHelper.getCocktailById(cocktailId);
            populateViews();
        }
    }

    /** Rellena todas las vistas con los datos del cóctel */
    private void populateViews() {
        if (cocktail == null) return;

        // Título de la app bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(cocktail.getName());
        }

        textViewName.setText(cocktail.getName());
        textViewDescription.setText(cocktail.getDescription() != null
                ? cocktail.getDescription() : "Sin descripción");

        // Imagen (URL o placeholder)
        if (cocktail.getImageUrl() != null && !cocktail.getImageUrl().isEmpty()) {
            try {
                imageViewCocktail.setImageURI(Uri.parse(cocktail.getImageUrl()));
            } catch (Exception e) {
                imageViewCocktail.setImageResource(R.drawable.ic_cocktail_placeholder);
            }
        } else {
            imageViewCocktail.setImageResource(R.drawable.ic_cocktail_placeholder);
        }

        // Alcoholes
        List<String> alcohols = cocktail.getAlcohols();
        if (alcohols != null && !alcohols.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String a : alcohols) sb.append("• ").append(a).append("\n");
            textViewAlcohols.setText(sb.toString().trim());
        } else {
            textViewAlcohols.setText("—");
        }

        // Mezclas
        List<String> mixers = cocktail.getMixers();
        if (mixers != null && !mixers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String m : mixers) sb.append("• ").append(m).append("\n");
            textViewMixers.setText(sb.toString().trim());
        } else {
            textViewMixers.setText("—");
        }

        // Opciones adicionales
        textViewSmoke.setText(cocktail.isHasSmoke() ? "✅ Tiene humo" : "❌ Sin humo");
        if (cocktail.isHasTopping()) {
            String desc = cocktail.getToppingDescription();
            textViewTopping.setText("✅ Topping" + (desc != null && !desc.isEmpty() ? ": " + desc : ""));
        } else {
            textViewTopping.setText("❌ Sin topping");
        }

        // Procedimiento
        textViewProcedure.setText(cocktail.getProcedure() != null && !cocktail.getProcedure().isEmpty()
                ? cocktail.getProcedure()
                : "No se ha añadido procedimiento aún.");

        // Valoraciones
        updateRatingsUI();
    }

    /** Actualiza la sección de valoraciones */
    private void updateRatingsUI() {
        List<Rating> ratings = cocktail.getRatings();
        float avg = cocktail.getAverageRating();

        // Promedio
        ratingBarAverage.setNumStars(6);
        ratingBarAverage.setMax(6);
        ratingBarAverage.setRating(avg);
        textViewAverageRating.setText(avg > 0
                ? String.format("%.1f / 6 ⭐", avg)
                : "Sin valoraciones aún");
        textViewRatingCount.setText(ratings.size() + " voto(s)");

        // Lista de votos individuales
        layoutRatingsList.removeAllViews();
        for (Rating r : ratings) {
            TextView tv = new TextView(this);
            tv.setText("👤 " + r.getVoterName() + "  →  " + r.getStars() + " ⭐");
            tv.setPadding(0, 8, 0, 8);
            tv.setTextSize(14f);
            layoutRatingsList.addView(tv);
        }
    }

    /** Muestra el diálogo para añadir una nueva valoración */
    private void showAddRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_rating, null);
        EditText editVoterName = dialogView.findViewById(R.id.editVoterName);
        RatingBar ratingBarDialog = dialogView.findViewById(R.id.ratingBarDialog);
        ratingBarDialog.setNumStars(6);
        ratingBarDialog.setMax(6);
        ratingBarDialog.setRating(3);

        new AlertDialog.Builder(this)
                .setTitle("Añadir valoración")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String voterName = editVoterName.getText().toString().trim();
                    int stars = (int) ratingBarDialog.getRating();

                    if (voterName.isEmpty()) {
                        Toast.makeText(this, "Introduce el nombre del votante", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (stars < 1) {
                        Toast.makeText(this, "Selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Guardar valoración en BD
                    Rating rating = new Rating(cocktailId, voterName, stars);
                    dbHelper.insertRating(rating);

                    // Refrescar datos
                    cocktail = dbHelper.getCocktailById(cocktailId);
                    updateRatingsUI();
                    Toast.makeText(this, "Voto de " + voterName + " guardado ✅", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─── Menú ───────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_edit) {
            Intent intent = new Intent(this, EditCocktailActivity.class);
            intent.putExtra("cocktail_id", cocktailId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar cóctel")
                    .setMessage("¿Seguro que quieres eliminar este cóctel?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        dbHelper.deleteCocktail(cocktailId);
                        Toast.makeText(this, "Cóctel eliminado", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
