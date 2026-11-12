package com.siu.local;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import org.json.*;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;

/** End-to-end smoke checks for navigation, local editing, images and backup round trips. */
public class SmokeInstrumentation extends Instrumentation {
    boolean screenshots;
    @Override public void onCreate(Bundle args) {super.onCreate(args);screenshots=args!=null&&"true".equals(args.getString("screenshots"));start();}
    int checks;
    void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    @Override public void onStart(){
        Bundle result=new Bundle();MainActivity activity=null;LocalStore previous=null;
        try{
            Context context=getTargetContext();
            File sandbox=new File(context.getCacheDir(),"smoke-store");sandbox.mkdirs();
            Context isolated=new ContextWrapper(context){@Override public File getFilesDir(){return sandbox;}};
            File data=new File(sandbox,"content.json");data.delete();
            LocalStore local=new LocalStore(isolated);
            LocalStore.put(local.obj("perfil"),"nombre","PRUEBA DE PERSISTENCIA");local.save();
            check(new LocalStore(isolated).obj("perfil").getString("nombre").equals("PRUEBA DE PERSISTENCIA"),"Profile persistence");
            Bitmap picture=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);picture.eraseColor(Color.MAGENTA);
            File imageFile=new File(sandbox,"source.png");try(OutputStream out=new FileOutputStream(imageFile)){picture.compress(Bitmap.CompressFormat.PNG,100,out);}
            String imageName=local.importImage(Uri.fromFile(imageFile));LocalStore.put(local.obj("perfil"),"foto",imageName);local.save();
            check(local.image(imageName,null).getPixel(0,0)==Color.MAGENTA,"Image import and reading");
            File zip=new File(sandbox,"backup.zip");local.exportZip(Uri.fromFile(zip));
            LocalStore.put(local.obj("perfil"),"nombre","CAMBIADO");local.save();new File(sandbox,imageName).delete();
            local.importZip(Uri.fromFile(zip));
            check(local.obj("perfil").getString("nombre").equals("PRUEBA DE PERSISTENCIA"),"Backup restores data");
            check(local.image(imageName,null).getPixel(0,0)==Color.MAGENTA,"Backup restores images");
            File invalid=new File(sandbox,"invalid.zip");try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(invalid))){out.putNextEntry(new ZipEntry("../escape.png"));out.write(new byte[]{1});out.closeEntry();}
            boolean rejected=false;try{local.importZip(Uri.fromFile(invalid));}catch(IOException expected){rejected=true;}
            check(rejected,"Reject path traversal in backup");
            check(local.obj("perfil").getString("nombre").equals("PRUEBA DE PERSISTENCIA"),"Failed import keeps data");

            Intent intent=new Intent(context,MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=(MainActivity)startActivitySync(intent);activity=checkCarnetRecreation(activity);MainActivity a=activity;previous=a.store;
            runOnMainSync(()->{a.store=local;a.history.clear();});
            String[] routes={"home","drawer","perfil","carnet","ajustes","Social","Materias","Académica","Recursos","Tip's","contactos:0","contactos:1","contactos:2","contactos:3","contactos:4","lugares:0","consultas","consulta:0","nuevaConsulta:0","sugerencias","sugerencia:0","documentos:0","documentos:1","horarios:0","horarios:1","horarios:5","notas","pagos","costos","tramites","tipos","detalle:0","biblioteca","descuentos","correo","proyectos","bancos:0","bancos:1","ayuda","ayudaDetalle:0","ayudaDetalle:1","ayudaDetalle:2","materia:0","editor:","editor:perfil","editor:materias","editor:materias/0","editor:horarios","editor:tiposTramites/0/requisitos","acceso"};
            for(String route:routes){runOnMainSync(()->{a.route=route;a.render();check(a.frame.getChildCount()>0,"Render "+route);});waitForIdleSync();}
            runOnMainSync(()->{a.selectedGestion=local.arr("gestiones").optString(0);a.route="notas";a.render();check(a.body.getChildCount()>=2,"Grades selector and results");a.route="editor:perfil";a.render();EditText field=findEdit(a.root);check(field!=null,"Profile editor has editable fields");field.setText("EDITADO EN UI");a.setPath("materias/0/parcial1","99,0");
                a.route="editor:notasAnteriores/Gestión 1~12026";a.render();check(a.resolve("notasAnteriores/Gestión 1~12026") instanceof JSONArray,"Historical semester path supports slash");
            });
            check(new LocalStore(isolated).obj("perfil").getString("nombre").equals("EDITADO EN UI"),"Editor saves profile");
            check(new LocalStore(isolated).arr("materias").getJSONObject(0).getString("parcial1").equals("99,0"),"Editor saves grades");
            runOnMainSync(()->{LocalStore.put(local.obj("ajustes"),"tema","Claro");a.route="home";a.render();check(a.bg!=Color.BLACK,"Light theme");LocalStore.put(local.obj("ajustes"),"tema","Oscuro");});
            checkCarnet(a,local,imageName);
            check(local.arr("pagos").length()==6,"Reference payment plan");
            check(local.image("","artwork/logo_carnet.png").getWidth()==886,"Original high-resolution watermark");
            check(local.image("","portrait_carnet.png").getWidth()==178,"Carnet uses the newer reference portrait framing");
            check(local.image("","seal.jpg").getWidth()==472,"Legacy seal fallback uses original artwork");
            check(SiuIcon.codepoint("Social")==0xf500,"Original two-person social glyph");
            check(context.getPackageManager().getApplicationIcon(context.getPackageName()) instanceof android.graphics.drawable.AdaptiveIconDrawable,"Adaptive launcher icon");
            runOnMainSync(()->{LocalStore.put(local.obj("ajustes"),"cuadricula",true);a.route="home";a.render();check(findImageResource(a.root)==6,"Grid contains five original PNG icons and watermark");a.route="perfil";a.render();check(findDescription(a.root,"Bandera de Bolivia")!=null,"Profile uses original flag, not emoji");a.route="carnet";a.render();check(findDescription(a.root,"Bandera de Bolivia")!=null,"Carnet flag");});
            runOnMainSync(()->{
                LocalStore.put(local.obj("ajustes"),"cuadricula",false);a.route="home";a.render();
                findDescription(a.root,"Abrir menú").performClick();check(a.route.equals("drawer"),"Open drawer from home");
                ((android.view.View)findText(a.frame,"Perfil de usuario").getParent()).performClick();check(a.route.equals("perfil"),"Drawer opens profile");
                findDescription(a.frame,"Editar perfil").performClick();check(a.route.equals("editor:perfil"),"Profile edit button opens editor");a.back();check(a.route.equals("perfil"),"Back returns to profile");
                a.route="home";a.history.clear();a.render();findDescription(a.root,"Ver cuadrícula").performClick();check(local.obj("ajustes").optBoolean("cuadricula"),"Grid toggle saves preference");
                findDescription(a.root,"Ver lista").performClick();check(!local.obj("ajustes").optBoolean("cuadricula"),"List toggle saves preference");
                a.route="ajustes";a.render();findText(a.root,"Claro").performClick();check(a.bg!=Color.BLACK,"Theme radio updates appearance");findText(a.root,"Oscuro").performClick();check(a.bg==Color.BLACK,"Dark theme radio updates appearance");
            });
            if(screenshots) captureScreens(a,local);
            result.putString("stream","\nOK: "+checks+" comprobaciones: pantallas, persistencia, edición, imágenes, QR y copia ZIP.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","\nERROR: "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally{if(activity!=null&&previous!=null){MainActivity a=activity;LocalStore original=previous;runOnMainSync(()->{a.store=original;a.route="home";a.selectedGestion="";a.history.clear();a.render();});}}
    }
    EditText findEdit(android.view.View view){if(view instanceof EditText e)return e;if(view instanceof android.view.ViewGroup g)for(int i=0;i<g.getChildCount();i++){EditText e=findEdit(g.getChildAt(i));if(e!=null)return e;}return null;}
    TextView findText(android.view.View view,String label){if(view instanceof TextView t&&label.contentEquals(t.getText()))return t;if(view instanceof android.view.ViewGroup g)for(int i=0;i<g.getChildCount();i++){TextView found=findText(g.getChildAt(i),label);if(found!=null)return found;}return null;}
    android.view.View findDescription(android.view.View view,String description){if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof android.view.ViewGroup g)for(int i=0;i<g.getChildCount();i++){android.view.View found=findDescription(g.getChildAt(i),description);if(found!=null)return found;}return null;}
    int findImageResource(android.view.View view){int count=view instanceof ImageView?1:0;if(view instanceof android.view.ViewGroup g)for(int i=0;i<g.getChildCount();i++)count+=findImageResource(g.getChildAt(i));return count;}
    MainActivity checkCarnetRecreation(MainActivity activity){
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        runOnMainSync(()->{activity.route="carnet";activity.carnetUser=true;activity.render();activity.recreate();});
        MainActivity recreated=(MainActivity)waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);check(recreated!=null,"Activity recreated");waitForIdleSync();
        runOnMainSync(()->{check(recreated.route.equals("carnet")&&recreated.carnetUser,"Activity recreation restores carnet tab");check(findText(recreated.root,"Usuario").isSelected()&&findText(recreated.root,"Sede:")!=null,"Recreated carnet renders Usuario");});return recreated;
    }
    String decodeQr(Bitmap qr){
        int width=qr.getWidth(),height=qr.getHeight();int[] pixels=new int[width*height];qr.getPixels(pixels,0,width,0,0,width,height);
        try{return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width,height,pixels)))).getText();}catch(NotFoundException e){throw new AssertionError("Carnet QR must be readable",e);}
    }
    Bitmap displayedQr(MainActivity a){ImageView image=(ImageView)findDescription(a.root,"Código QR local, sin validación institucional");check(image!=null&&image.getVisibility()==android.view.View.VISIBLE,"QR is embedded in the carnet");return ((android.graphics.drawable.BitmapDrawable)image.getDrawable()).getBitmap();}
    void checkCarnet(MainActivity a,LocalStore local,String importedImage){
        runOnMainSync(()->{
            JSONObject p=local.obj("perfil");String oldText=p.optString("qrTexto"),oldImage=p.optString("qrImagen");
            LocalStore.put(p,"qrImagen","");LocalStore.put(p,"qrTexto","SIU-LOCAL:prueba-123");a.route="home";a.history.clear();a.carnetUser=true;a.render();findDescription(a.root,"Carnet virtual").performClick();
            check(a.route.equals("carnet")&&!a.carnetUser,"Opening carnet selects QR tab");check(findText(a.root,"Carnet").isSelected(),"Carnet tab is selected");check(decodeQr(displayedQr(a)).equals("SIU-LOCAL:prueba-123"),"Displayed QR encodes configured local text");check(findText(a.root,"Sede:")==null,"Carnet tab does not show user fields");check(a.carnetName("LUZ GABRIELA CASTAÑETA TARQUINO").equals("LUZ GABRIELA\nCASTAÑETA TARQUINO"),"Two-line carnet name matches reference");
            int history=a.history.size();LocalStore.put(p,"qrTexto","SIU-LOCAL:actualizado-á");findDescription(a.root,"Actualizar QR local").performClick();check(decodeQr(displayedQr(a)).equals("SIU-LOCAL:actualizado-á"),"Refresh regenerates QR from latest local content");
            findText(a.root,"Usuario").performClick();check(a.carnetUser&&findText(a.root,"Usuario").isSelected(),"Usuario tab is functional and selected");check(findText(a.root,"Sede:")!=null&&findText(a.root,p.optString("correo"))!=null,"Usuario shows profile fields");check(findDescription(a.root,"Código QR local, sin validación institucional")==null,"Usuario hides QR");check(findDescription(a.root,"Bandera de Bolivia")!=null,"Usuario keeps portrait flag");check(a.history.size()==history,"Tab changes do not add back-stack entries");
            Bundle state=new Bundle();a.onSaveInstanceState(state);check(state.getBoolean("carnetUser")&&state.getString("route").equals("carnet"),"Selected carnet tab is saved for recreation");a.render();check(findText(a.root,"Usuario").isSelected(),"Rerender preserves selected tab");
            LocalStore.put(local.obj("ajustes"),"tema","Claro");a.render();check(a.bg!=Color.BLACK&&findText(a.root,"Usuario").isSelected(),"Carnet supports light theme without losing selection");LocalStore.put(local.obj("ajustes"),"tema","Oscuro");findText(a.root,"Carnet").performClick();check(!a.carnetUser&&decodeQr(displayedQr(a)).equals("SIU-LOCAL:actualizado-á"),"Switch back to embedded QR");
            String longPayload="siu-local-".repeat(80);LocalStore.put(p,"qrTexto",longPayload);findDescription(a.root,"Actualizar QR local").performClick();check(decodeQr(displayedQr(a)).equals(longPayload),"QR grows beyond reference grid for larger valid payloads");
            LocalStore.put(p,"qrTexto","");findDescription(a.root,"Actualizar QR local").performClick();check(findText(a.root,"Introduce el contenido del QR en Ajustes → Editar contenido → Perfil.")!=null,"Empty QR has actionable inline message");
            LocalStore.put(p,"qrTexto","x".repeat(5000));findDescription(a.root,"Actualizar QR local").performClick();check(findText(a.root,"El contenido es demasiado largo para generar un QR.")!=null,"Oversized QR has inline error");
            LocalStore.put(p,"qrImagen",importedImage);findDescription(a.root,"Actualizar QR local").performClick();check(displayedQr(a).getPixel(0,0)==Color.MAGENTA,"Imported QR image takes priority over text");
            LocalStore.put(p,"qrTexto",oldText);LocalStore.put(p,"qrImagen",oldImage);a.back();check(a.route.equals("home"),"Back from carnet returns to entry screen, not previous tab");
            a.go("drawer");((android.view.View)findText(a.frame,"Carnet virtual").getParent()).performClick();check(a.route.equals("carnet")&&!a.carnetUser,"Drawer opens QR tab");
        });waitForIdleSync();
    }
    void captureScreens(MainActivity a,LocalStore local)throws Exception{
        // Screenshots use fresh reference data in memory, never overwrite the real store.
        runOnMainSync(()->{try{local.data=new JSONObject(new String(a.readAsset("defaults.json"),java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}a.selectedGestion="";});
        File directory=new File(getTargetContext().getExternalFilesDir(null),"visual-checks");directory.mkdirs();
        JSONObject bounds=new JSONObject();
        for(String screen:new String[]{"home","grid","drawer","perfil","carnet","carnet-usuario","ajustes","Social","Materias","Académica","Recursos","Tip's","documentos:0","horarios:0","notas","grades","pagos","contactos:0","tipos","detalle:0","ayuda"}){
            runOnMainSync(()->{LocalStore.put(local.obj("ajustes"),"cuadricula",screen.equals("grid"));a.route=screen.equals("grid")?"home":screen.equals("grades")?"notas":screen.equals("carnet-usuario")?"carnet":screen;a.carnetUser=screen.equals("carnet-usuario");if(screen.equals("grades"))a.selectedGestion=local.arr("gestiones").optString(0);a.render();});waitForIdleSync();
            // A fresh frame after layout is necessary: idle alone can precede traversal.
            java.util.concurrent.CountDownLatch drawn=new java.util.concurrent.CountDownLatch(1);
            runOnMainSync(()->a.frame.postOnAnimation(()->a.frame.postOnAnimation(drawn::countDown)));
            check(drawn.await(5,java.util.concurrent.TimeUnit.SECONDS),"Screenshot frame "+screen);
            getUiAutomation().waitForIdle(200,5000);
            runOnMainSync(()->{int[] origin=new int[2];a.frame.getLocationOnScreen(origin);try{bounds.put(screen,new JSONArray(new int[]{origin[0],origin[1],origin[0]+a.frame.getWidth(),origin[1]+a.frame.getHeight()}));}catch(JSONException e){throw new IllegalStateException(e);}});
            Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot "+screen);
            try(OutputStream out=new FileOutputStream(new File(directory,screen.replace(':','-')+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        }
        try(java.io.Writer out=new OutputStreamWriter(new FileOutputStream(new File(directory,"bounds.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(bounds.toString(2));}
    }
}
