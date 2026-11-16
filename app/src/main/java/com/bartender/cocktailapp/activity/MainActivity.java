package com.bartender.cocktailapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bartender.cocktailapp.R;
import com.bartender.cocktailapp.adapter.CocktailAdapter;
import com.bartender.cocktailapp.database.DatabaseHelper;
import com.bartender.cocktailapp.model.Cocktail;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal.
 * Muestra la lista de todos los cócteles con buscador y botón para añadir.
 */
public class MainActivity extends AppCompatActivity implements CocktailAdapter.OnCocktailClickListener {

    private RecyclerView recyclerView;
    private CocktailAdapter adapter;
    private EditText searchEditText;
    private TextView emptyTextView;
    private FloatingActionButton fabAdd;

    private DatabaseHelper dbHelper;
    private List<Cocktail> allCocktails;    // Lista completa
    private List<Cocktail> filteredList;    // Lista filtrada por búsqueda

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar ActionBar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("🍹 Mis Cócteles");
        }

        // Inicializar base de datos
        dbHelper = DatabaseHelper.getInstance(this);

        // Referenciar vistas
        recyclerView    = findViewById(R.id.recyclerViewCocktails);
        searchEditText  = findViewById(R.id.editTextSearch);
        emptyTextView   = findViewById(R.id.textViewEmpty);
        fabAdd          = findViewById(R.id.fabAddCocktail);

        // Configurar RecyclerView
        allCocktails = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new CocktailAdapter(filteredList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Buscador en tiempo real
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterCocktails(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Botón flotante → abrir pantalla de creación
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditCocktailActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar siempre que volvemos (tras crear/editar/borrar)
        loadCocktails();
    }

    /** Carga todos los cócteles de la base de datos y actualiza la lista */
    private void loadCocktails() {
        allCocktails = dbHelper.getAllCocktails();
        filterCocktails(searchEditText.getText().toString());
    }

    /** Filtra la lista por nombre o descripción */
    private void filterCocktails(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(allCocktails);
        } else {
            String lower = query.toLowerCase().trim();
            for (Cocktail c : allCocktails) {
                if (c.getName().toLowerCase().contains(lower) ||
                        (c.getDescription() != null && c.getDescription().toLowerCase().contains(lower))) {
                    filteredList.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();

        // Mostrar mensaje si no hay resultados
        if (filteredList.isEmpty()) {
            emptyTextView.setText(allCocktails.isEmpty()
                    ? "¡Añade tu primer cóctel! 🍸"
                    : "No se encontraron cócteles");
            emptyTextView.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            emptyTextView.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }

    // ─── Callbacks del Adapter ──────────────────────────────────────────────────

    /** Al pulsar un cóctel → ir al detalle */
    @Override
    public void onCocktailClick(Cocktail cocktail) {
        Intent intent = new Intent(this, DetailCocktailActivity.class);
        intent.putExtra("cocktail_id", cocktail.getId());
        startActivity(intent);
    }

    /** Al pulsar editar → ir a EditCocktailActivity con el ID */
    @Override
    public void onEditClick(Cocktail cocktail) {
        Intent intent = new Intent(this, EditCocktailActivity.class);
        intent.putExtra("cocktail_id", cocktail.getId());
        startActivity(intent);
    }

    /** Al pulsar eliminar → confirmar y borrar */
    @Override
    public void onDeleteClick(Cocktail cocktail) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar cóctel")
                .setMessage("¿Seguro que quieres eliminar \"" + cocktail.getName() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbHelper.deleteCocktail(cocktail.getId());
                    Toast.makeText(this, "Cóctel eliminado", Toast.LENGTH_SHORT).show();
                    loadCocktails();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cocktail Manager")
                    .setMessage("App para bartenders\nVersion 1.0\n\nGestiona tu colección de cócteles de forma fácil y rápida.")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
