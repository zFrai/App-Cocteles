package com.bartender.cocktailapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bartender.cocktailapp.model.Cocktail;
import com.bartender.cocktailapp.model.Rating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper de base de datos SQLite.
 * Gestiona todas las operaciones CRUD para cócteles y valoraciones.
 *
 * Tablas:
 *   - cocktails: datos principales del cóctel
 *   - ratings: votos individuales ligados a un cóctel
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ─── Constantes de la base de datos ────────────────────────────────────────
    private static final String DB_NAME    = "cocktails.db";
    private static final int    DB_VERSION = 1;

    // Tabla cócteles
    public static final String TABLE_COCKTAILS       = "cocktails";
    public static final String COL_ID                = "id";
    public static final String COL_NAME              = "name";
    public static final String COL_DESCRIPTION       = "description";
    public static final String COL_IMAGE_URL         = "image_url";
    public static final String COL_PROCEDURE         = "procedure";
    // Ingredientes guardados como texto separado por "|"
    public static final String COL_ALCOHOLS          = "alcohols";
    public static final String COL_MIXERS            = "mixers";
    public static final String COL_HAS_SMOKE         = "has_smoke";
    public static final String COL_HAS_TOPPING       = "has_topping";
    public static final String COL_TOPPING_DESC      = "topping_description";

    // Tabla valoraciones
    public static final String TABLE_RATINGS         = "ratings";
    public static final String COL_RATING_ID         = "id";
    public static final String COL_RATING_COCKTAIL_ID = "cocktail_id";
    public static final String COL_RATING_VOTER      = "voter_name";
    public static final String COL_RATING_STARS      = "stars";

    // Separador para listas guardadas como String
    private static final String LIST_SEPARATOR = "|";

    // Singleton para evitar múltiples instancias
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ─── Creación de tablas ─────────────────────────────────────────────────────

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla de cócteles
        db.execSQL("CREATE TABLE " + TABLE_COCKTAILS + " (" +
                COL_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME         + " TEXT NOT NULL, " +
                COL_DESCRIPTION  + " TEXT, " +
                COL_IMAGE_URL    + " TEXT, " +
                COL_PROCEDURE    + " TEXT, " +
                COL_ALCOHOLS     + " TEXT, " +
                COL_MIXERS       + " TEXT, " +
                COL_HAS_SMOKE    + " INTEGER DEFAULT 0, " +
                COL_HAS_TOPPING  + " INTEGER DEFAULT 0, " +
                COL_TOPPING_DESC + " TEXT" +
                ")");

        // Crear tabla de valoraciones
        db.execSQL("CREATE TABLE " + TABLE_RATINGS + " (" +
                COL_RATING_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RATING_COCKTAIL_ID + " INTEGER NOT NULL, " +
                COL_RATING_VOTER       + " TEXT NOT NULL, " +
                COL_RATING_STARS       + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_RATING_COCKTAIL_ID + ") REFERENCES " +
                TABLE_COCKTAILS + "(" + COL_ID + ") ON DELETE CASCADE" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // En versiones futuras aquí se harían migraciones
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RATINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COCKTAILS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        // Habilitar claves foráneas
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ─── CRUD Cócteles ──────────────────────────────────────────────────────────

    /**
     * Inserta un nuevo cóctel en la base de datos.
     * @return ID del cóctel insertado, o -1 si hubo error
     */
    public long insertCocktail(Cocktail cocktail) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = cocktailToContentValues(cocktail);
        long id = db.insert(TABLE_COCKTAILS, null, cv);
        cocktail.setId(id);
        return id;
    }

    /**
     * Actualiza todos los datos de un cóctel existente.
     * @return número de filas afectadas
     */
    public int updateCocktail(Cocktail cocktail) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = cocktailToContentValues(cocktail);
        return db.update(TABLE_COCKTAILS, cv,
                COL_ID + "=?",
                new String[]{String.valueOf(cocktail.getId())});
    }

    /**
     * Elimina un cóctel y todas sus valoraciones (CASCADE).
     */
    public void deleteCocktail(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_COCKTAILS, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    /**
     * Obtiene todos los cócteles con sus valoraciones.
     */
    public List<Cocktail> getAllCocktails() {
        List<Cocktail> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COCKTAILS, null, null, null, null, null, COL_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Cocktail c = cursorToCocktail(cursor);
                c.setRatings(getRatingsForCocktail(c.getId()));
                list.add(c);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Obtiene un único cóctel por su ID.
     */
    public Cocktail getCocktailById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COCKTAILS, null,
                COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);

        Cocktail cocktail = null;
        if (cursor.moveToFirst()) {
            cocktail = cursorToCocktail(cursor);
            cocktail.setRatings(getRatingsForCocktail(id));
        }
        cursor.close();
        return cocktail;
    }

    // ─── CRUD Valoraciones ──────────────────────────────────────────────────────

    /**
     * Inserta una nueva valoración para un cóctel.
     * @return ID de la valoración insertada
     */
    public long insertRating(Rating rating) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_RATING_COCKTAIL_ID, rating.getCocktailId());
        cv.put(COL_RATING_VOTER, rating.getVoterName());
        cv.put(COL_RATING_STARS, rating.getStars());
        long id = db.insert(TABLE_RATINGS, null, cv);
        rating.setId(id);
        return id;
    }

    /**
     * Obtiene todas las valoraciones de un cóctel específico.
     */
    public List<Rating> getRatingsForCocktail(long cocktailId) {
        List<Rating> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RATINGS, null,
                COL_RATING_COCKTAIL_ID + "=?",
                new String[]{String.valueOf(cocktailId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Rating r = new Rating();
                r.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_RATING_ID)));
                r.setCocktailId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_RATING_COCKTAIL_ID)));
                r.setVoterName(cursor.getString(cursor.getColumnIndexOrThrow(COL_RATING_VOTER)));
                r.setStars(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RATING_STARS)));
                list.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Elimina una valoración por su ID.
     */
    public void deleteRating(long ratingId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_RATINGS, COL_RATING_ID + "=?", new String[]{String.valueOf(ratingId)});
    }

    // ─── Métodos auxiliares ─────────────────────────────────────────────────────

    /** Convierte un Cocktail a ContentValues para operaciones de BD */
    private ContentValues cocktailToContentValues(Cocktail c) {
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME,         c.getName());
        cv.put(COL_DESCRIPTION,  c.getDescription());
        cv.put(COL_IMAGE_URL,    c.getImageUrl());
        cv.put(COL_PROCEDURE,    c.getProcedure());
        cv.put(COL_ALCOHOLS,     listToString(c.getAlcohols()));
        cv.put(COL_MIXERS,       listToString(c.getMixers()));
        cv.put(COL_HAS_SMOKE,    c.isHasSmoke()   ? 1 : 0);
        cv.put(COL_HAS_TOPPING,  c.isHasTopping() ? 1 : 0);
        cv.put(COL_TOPPING_DESC, c.getToppingDescription());
        return cv;
    }

    /** Convierte un Cursor a un objeto Cocktail */
    private Cocktail cursorToCocktail(Cursor cursor) {
        Cocktail c = new Cocktail();
        c.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        c.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
        c.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        c.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_URL)));
        c.setProcedure(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROCEDURE)));
        c.setAlcohols(stringToList(cursor.getString(cursor.getColumnIndexOrThrow(COL_ALCOHOLS))));
        c.setMixers(stringToList(cursor.getString(cursor.getColumnIndexOrThrow(COL_MIXERS))));
        c.setHasSmoke(cursor.getInt(cursor.getColumnIndexOrThrow(COL_HAS_SMOKE)) == 1);
        c.setHasTopping(cursor.getInt(cursor.getColumnIndexOrThrow(COL_HAS_TOPPING)) == 1);
        c.setToppingDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TOPPING_DESC)));
        return c;
    }

    /** Convierte una lista de Strings a un único String separado por "|" */
    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(LIST_SEPARATOR);
        }
        return sb.toString();
    }

    /** Convierte un String separado por "|" a una lista de Strings */
    private List<String> stringToList(String str) {
        List<String> list = new ArrayList<>();
        if (str == null || str.isEmpty()) return list;
        String[] parts = str.split("\\|");
        list.addAll(Arrays.asList(parts));
        return list;
    }
}
