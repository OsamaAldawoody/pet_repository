package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    //whole table code ..
    private static final int pets = 100;
    //single row of the column code ..
    private static final int pets_id = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //set up the uri matcher
    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS,pets);


        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS + "/#",pets_id);
    }
    PetDbHelper mDbHelper;
    /** Tag for the log messages */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query( Uri uri,  String[] projection,  String selection,  String[] selectionArgs,  String s1) {

        SQLiteDatabase database;

        int match = sUriMatcher.match(uri);

        database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        switch (match){
            case pets:
                cursor = database.query(PetEntry.TABLE_NAME,projection,selection,selectionArgs,null,null,null,null);
           break;
            case pets_id:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(PetEntry.TABLE_NAME,projection,selection,selectionArgs
                ,null,null,s1);
                break;
                default:
                    throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case pets:
                return PetEntry.CONTENT_LIST_TYPE;
            case pets_id:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        int match = sUriMatcher.match(uri);
        switch (match){
            case pets:
                return insertPet(uri,contentValues);

                default:
                    throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }


    }

    private Uri insertPet(Uri uri,ContentValues contentValues){
        // Check that the name is not null
        String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this.getContext(), "name needed", Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("Pet requires a name");
        }

        //check gender is valid
        Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetEntry.isValidGender(gender)){
            throw new IllegalArgumentException("invalid gender type");
        }
        // If the weight is provided, check that it's greater than or equal to 0 kg
        Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight < 0) {
            Toast.makeText(this.getContext(), "you need a valid weight", Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("Pet requires valid weight");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(PetEntry.TABLE_NAME , null , contentValues);



        if (id == -1 ){
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return ContentUris.withAppendedId(uri,id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowDeleted;
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case pets:
                // Delete all rows that match the selection and selection args
                rowDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);

                if (rowDeleted != 0) {
                    // delete automatically from user interface
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowDeleted;
            case pets_id:
                // Delete a single row given by the ID in the URI
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                rowDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);

                if (rowDeleted != 0 ) {
                    // delete automatically from user interface
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowDeleted;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case pets:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case pets_id:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Check that the name is not null
        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this.getContext(), "name needed", Toast.LENGTH_SHORT).show();
                throw new IllegalArgumentException("Pet requires a name");
            }
        }
        //check gender is valid
        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("invalid gender type");
            }
        }
        // If the weight is provided, check that it's greater than or equal to 0 kg
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight < 0) {
                Toast.makeText(this.getContext(), "you need a valid weight", Toast.LENGTH_SHORT).show();
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        if (values.size() == 0){
            return 0;
        }


        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            // update automatically from user interface
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
