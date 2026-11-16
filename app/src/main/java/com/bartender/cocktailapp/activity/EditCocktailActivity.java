package com.bartender.cocktailapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.bartender.cocktailapp.R;
import com.bartender.cocktailapp.database.DatabaseHelper;
import com.bartender.cocktailapp.model.Cocktail;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity para CREAR o EDITAR un cóctel.
 * Si se pasa "cocktail_id" en el Intent, entra en modo edición.
 * Si no, entra en modo creación.
 */
public class EditCocktailActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;

    // Vistas del formulario
    private EditText editName, editDescription, editProcedure, editToppingDesc;
    private ImageView imageViewPreview;
    private Button btnPickImage, btnSave, btnAddAlcohol, btnAddMixer;
    private Switch switchSmoke, switchTopping;
    private LinearLayout layoutAlcohols, layoutMixers;
    private TextView textViewToppingDescLabel;

    private DatabaseHelper dbHelper;
    private Cocktail cocktail;          // null = modo creación
    private long cocktailId = -1;
    private String selectedImageUri = ""; // URI de imagen seleccionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_cocktail);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper   = DatabaseHelper.getInstance(this);
        cocktailId = getIntent().getLongExtra("cocktail_id", -1);

        // Referenciar vistas
        editName          = findViewById(R.id.editTextName);
        editDescription   = findViewById(R.id.editTextDescription);
        editProcedure     = findViewById(R.id.editTextProcedure);
        editToppingDesc   = findViewById(R.id.editTextToppingDesc);
        imageViewPreview  = findViewById(R.id.imageViewPreview);
        btnPickImage      = findViewById(R.id.btnPickImage);
        btnSave           = findViewById(R.id.btnSave);
        btnAddAlcohol     = findViewById(R.id.btnAddAlcohol);
        btnAddMixer       = findViewById(R.id.btnAddMixer);
        switchSmoke       = findViewById(R.id.switchSmoke);
        switchTopping     = findViewById(R.id.switchTopping);
        layoutAlcohols    = findViewById(R.id.layoutAlcohols);
        layoutMixers      = findViewById(R.id.layoutMixers);
        textViewToppingDescLabel = findViewById(R.id.labelToppingDesc);

        // Mostrar/ocultar campo de descripción de topping
        switchTopping.setOnCheckedChangeListener((btn, isChecked) -> {
            textViewToppingDescLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            editToppingDesc.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Botones para añadir ingredientes
        btnAddAlcohol.setOnClickListener(v -> addIngredientRow(layoutAlcohols, "Alcohol"));
        btnAddMixer.setOnClickListener(v -> addIngredientRow(layoutMixers, "Mezcla"));

        // Selector de imagen
        btnPickImage.setOnClickListener(v -> pickImage());

        // Guardar
        btnSave.setOnClickListener(v -> saveCocktail());

        // Si hay ID → modo edición
        if (cocktailId != -1) {
            cocktail = dbHelper.getCocktailById(cocktailId);
            if (cocktail != null) {
                getSupportActionBar().setTitle("Editar cóctel");
                populateForm();
            }
        } else {
            getSupportActionBar().setTitle("Nuevo cóctel");
        }
    }

    /** Rellena el formulario con los datos de un cóctel existente */
    private void populateForm() {
        editName.setText(cocktail.getName());
        editDescription.setText(cocktail.getDescription());
        editProcedure.setText(cocktail.getProcedure());
        switchSmoke.setChecked(cocktail.isHasSmoke());
        switchTopping.setChecked(cocktail.isHasTopping());

        if (cocktail.isHasTopping()) {
            textViewToppingDescLabel.setVisibility(View.VISIBLE);
            editToppingDesc.setVisibility(View.VISIBLE);
            editToppingDesc.setText(cocktail.getToppingDescription());
        }

        // Imagen
        selectedImageUri = cocktail.getImageUrl() != null ? cocktail.getImageUrl() : "";
        if (!selectedImageUri.isEmpty()) {
            try {
                imageViewPreview.setImageURI(Uri.parse(selectedImageUri));
                imageViewPreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                imageViewPreview.setVisibility(View.GONE);
            }
        }

        // Alcoholes
        layoutAlcohols.removeAllViews();
        if (cocktail.getAlcohols() != null) {
            for (String a : cocktail.getAlcohols()) {
                addIngredientRow(layoutAlcohols, "Alcohol", a);
            }
        }

        // Mezclas
        layoutMixers.removeAllViews();
        if (cocktail.getMixers() != null) {
            for (String m : cocktail.getMixers()) {
                addIngredientRow(layoutMixers, "Mezcla", m);
            }
        }
    }

    /**
     * Añade una fila de ingrediente editable (EditText + botón eliminar).
     * @param container  el LinearLayout donde se añade
     * @param hint       texto placeholder
     */
    private void addIngredientRow(LinearLayout container, String hint) {
        addIngredientRow(container, hint, "");
    }

    private void addIngredientRow(LinearLayout container, String hint, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etParams.setMargins(0, 0, 8, 0);
        editText.setLayoutParams(etParams);

        Button btnRemove = new Button(this);
        btnRemove.setText("✕");
        btnRemove.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btnRemove.setOnClickListener(v -> container.removeView(row));

        row.addView(editText);
        row.addView(btnRemove);
        container.addView(row);
    }

    /** Recoge los textos de un LinearLayout de ingredientes */
    private List<String> collectIngredients(LinearLayout container) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() > 0 && row.getChildAt(0) instanceof EditText) {
                    String val = ((EditText) row.getChildAt(0)).getText().toString().trim();
                    if (!val.isEmpty()) list.add(val);
                }
            }
        }
        return list;
    }

    /** Valida y guarda el cóctel (insertar o actualizar) */
    private void saveCocktail() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            editName.setError("El nombre es obligatorio");
            editName.requestFocus();
            return;
        }

        // Construir objeto
        if (cocktail == null) cocktail = new Cocktail();
        cocktail.setName(name);
        cocktail.setDescription(editDescription.getText().toString().trim());
        cocktail.setProcedure(editProcedure.getText().toString().trim());
        cocktail.setImageUrl(selectedImageUri);
        cocktail.setAlcohols(collectIngredients(layoutAlcohols));
        cocktail.setMixers(collectIngredients(layoutMixers));
        cocktail.setHasSmoke(switchSmoke.isChecked());
        cocktail.setHasTopping(switchTopping.isChecked());
        cocktail.setToppingDescription(
                switchTopping.isChecked() ? editToppingDesc.getText().toString().trim() : "");

        // Insertar o actualizar
        if (cocktailId == -1) {
            long newId = dbHelper.insertCocktail(cocktail);
            if (newId != -1) {
                Toast.makeText(this, "Cóctel creado ✅", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        } else {
            dbHelper.updateCocktail(cocktail);
            Toast.makeText(this, "Cóctel actualizado ✅", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** Abre el selector de imagen del dispositivo */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Persistir permiso de lectura
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                selectedImageUri = uri.toString();
                imageViewPreview.setImageURI(uri);
                imageViewPreview.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
