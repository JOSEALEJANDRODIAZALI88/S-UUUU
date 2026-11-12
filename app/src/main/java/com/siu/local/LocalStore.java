package com.siu.local;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

final class LocalStore {
    final Context context;
    JSONObject data;
    LocalStore(Context c) {
        context = c;
        try {
            File f = new File(c.getFilesDir(), "content.json");
            try (InputStream in = f.exists() ? new FileInputStream(f) : c.getAssets().open("defaults.json")) { data = new JSONObject(new String(bytes(in), StandardCharsets.UTF_8)); }
        } catch (Exception e) {
            try (InputStream in = c.getAssets().open("defaults.json")) { data = new JSONObject(new String(bytes(in), StandardCharsets.UTF_8)); }
            catch (Exception fatal) { throw new IllegalStateException(fatal); }
        }
    }
    void save() {
        try {
            File temp = new File(context.getFilesDir(), "content.tmp");
            Files.write(temp.toPath(), data.toString(2).getBytes(StandardCharsets.UTF_8));
            Files.move(temp.toPath(), new File(context.getFilesDir(), "content.json").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new IllegalStateException("No se pudieron guardar los datos", e); }
    }
    JSONObject obj(String key) { return data.optJSONObject(key); }
    JSONArray arr(String key) { return data.optJSONArray(key); }
    static void put(JSONObject o, String k, Object v) { try { o.put(k, v); } catch (JSONException e) { throw new IllegalArgumentException(e); } }
    String importImage(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
        if (bounds.outWidth <= 0) throw new IOException("La imagen no es válida");
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1800) options.inSampleSize *= 2;
        Bitmap image;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) { image = BitmapFactory.decodeStream(in, null, options); }
        if (image == null) throw new IOException("No se pudo abrir la imagen");
        String name = "image_" + UUID.randomUUID() + ".png";
        try (OutputStream out = new FileOutputStream(new File(context.getFilesDir(), name))) { image.compress(Bitmap.CompressFormat.PNG, 100, out); }
        image.recycle(); return name;
    }
    Bitmap image(String name, String fallback) {
        try {
            if (!name.isEmpty() && name.matches("image_[a-zA-Z0-9-]+\\.png")) {
                Bitmap b = BitmapFactory.decodeFile(new File(context.getFilesDir(), name).getPath()); if (b != null) return b;
            }
            // Keep the historical fallback names compatible with the editor and backups,
            // but use clean, transparent original artwork instead of screenshot crops.
            if ("seal.jpg".equals(fallback)) fallback = "artwork/logo_varios.png";
            if ("watermark.jpg".equals(fallback)) fallback = "artwork/logo_carnet.png";
            if (fallback != null) try (InputStream in = context.getAssets().open(fallback)) { return BitmapFactory.decodeStream(in); }
        } catch (IOException ignored) { }
        return null;
    }
    void exportZip(Uri uri) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(context.getContentResolver().openOutputStream(uri))) {
            zip.putNextEntry(new ZipEntry("content.json")); zip.write(data.toString().getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            Set<String> names = new HashSet<>(); collectImages(data, names);
            for (String name : names) {
                File file = new File(context.getFilesDir(), name);
                if (file.isFile()) { zip.putNextEntry(new ZipEntry(name)); Files.copy(file.toPath(), zip); zip.closeEntry(); }
            }
        }
    }
    private void collectImages(Object o, Set<String> names) {
        if (o instanceof JSONObject j) { Iterator<String> keys = j.keys(); while (keys.hasNext()) collectImages(j.opt(keys.next()), names); }
        else if (o instanceof JSONArray a) { for (int i=0;i<a.length();i++) collectImages(a.opt(i), names); }
        else if (o instanceof String s && s.matches("image_[a-zA-Z0-9-]+\\.png")) names.add(s);
    }
    void importZip(Uri uri) throws Exception {
        JSONObject incoming = null;
        Map<String, byte[]> images = new HashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.equals("content.json") && !name.matches("image_[a-zA-Z0-9-]+\\.png")) throw new IOException("Archivo de copia no válido");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
                while ((n=zip.read(buffer))!=-1) { total += n; if (total > 100L*1024*1024) throw new IOException("Copia demasiado grande (máximo 100 MB)"); bytes.write(buffer,0,n); }
                if (name.equals("content.json")) incoming = new JSONObject(bytes.toString("UTF-8"));
                else images.put(name, bytes.toByteArray());
            }
        }
        if (incoming == null) throw new IOException("Faltan los datos de la copia");
        JSONObject defaults;
        try (InputStream in = context.getAssets().open("defaults.json")) { defaults = new JSONObject(new String(bytes(in), StandardCharsets.UTF_8)); }
        validate(defaults, incoming);
        for (Map.Entry<String, byte[]> entry : images.entrySet()) {
            BitmapFactory.Options b = new BitmapFactory.Options(); b.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(entry.getValue(),0,entry.getValue().length,b);
            if (b.outWidth < 1 || b.outHeight < 1 || b.outWidth > 8192 || b.outHeight > 8192) throw new IOException("Imagen de copia no válida");
        }
        Set<String> required = new HashSet<>(); collectImages(incoming,required);
        for (String name : required) if (!images.containsKey(name)) throw new IOException("La copia no contiene todas las imágenes");
        for (Map.Entry<String, byte[]> entry : images.entrySet()) Files.write(new File(context.getFilesDir(),entry.getKey()).toPath(),entry.getValue());
        data = incoming; save();
    }
    private void validate(Object template, Object value) throws JSONException, IOException {
        if (template instanceof JSONObject t) {
            if (!(value instanceof JSONObject v)) throw new IOException("Formato de datos no válido");
            Iterator<String> keys=t.keys(); while(keys.hasNext()) { String k=keys.next(); if(!v.has(k)) throw new IOException("Falta el campo: " + k); validate(t.get(k),v.get(k)); }
        } else if (template instanceof JSONArray t) {
            if (!(value instanceof JSONArray v)) throw new IOException("Lista no válida");
            if(t.length()>0) for(int i=0;i<v.length();i++) validate(t.get(0),v.get(i));
        } else if (template instanceof Boolean && !(value instanceof Boolean) || template instanceof String && !(value instanceof String)) throw new IOException("Tipo de campo no válido");
    }
    static byte[] bytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer,0,n);
        return out.toByteArray();
    }
}
