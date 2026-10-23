package com.abaho.farecollector;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Exposes only the explicitly shared, private diagnostic file, read-only. */
public final class DiagnosticProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException{
        if(!"/diagnostic.json".equals(uri.getPath()))throw new FileNotFoundException();
        File file=new File(getContext().getFilesDir(),"diagnostic.json");if(!file.isFile())throw new FileNotFoundException();return file;
    }
    public String getType(Uri uri){return "application/json";}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try{File f=file(uri);MatrixCursor cursor=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});cursor.addRow(new Object[]{"fare-screen-diagnostic.json",f.length()});return cursor;}
        catch(FileNotFoundException e){return null;}
    }
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}
