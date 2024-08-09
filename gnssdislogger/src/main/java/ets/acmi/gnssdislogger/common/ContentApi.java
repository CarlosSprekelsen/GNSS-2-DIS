package ets.acmi.gnssdislogger.common;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.slf4j.Logger;

import ets.acmi.gnssdislogger.common.slf4j.Logs;

public class ContentApi extends ContentProvider {

    private static final Logger LOG = Logs.of(ContentApi.class);
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        String queryType = uri.getPathSegments().get(0);
        LOG.debug(queryType);
        String result;

        if ("gnsslogger_folder".equals(queryType)) {
            result = preferenceHelper.getGpsLoggerFolder();
        } else {
            result = "NULL";
        }


        LOG.debug(result);
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Column1"});

        matrixCursor.newRow().add(result);
        return matrixCursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
