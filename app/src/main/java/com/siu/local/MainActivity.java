package com.siu.local;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import org.json.*;
import java.util.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    static final int BRAND=Color.rgb(165,19,60), PINK=Color.rgb(242,126,155), WHITE=Color.rgb(239,239,239), GRAY=Color.rgb(140,140,140);
    int bg=Color.BLACK, card=Color.rgb(28,28,28), fg=WHITE;
    LocalStore store;
    LinearLayout root, body, bar;
    FrameLayout frame;
    String route="home", pendingImagePath="", selectedGestion="";
    final ArrayDeque<String> history=new ArrayDeque<>();
    final String[] categories={"Social","Materias","Académica","Recursos","Tip's"};
    final String[] cities={"Cochabamba","La Paz","Sucre","Trinidad","Santa Cruz"};
    final String[] days={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"};
    boolean busy, carnetUser;
    final HashMap<String,Bitmap> artworkCache=new HashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new LocalStore(this);
        if(state!=null) { route=state.getString("route","home"); pendingImagePath=state.getString("image",""); selectedGestion=state.getString("gestion",""); carnetUser=state.getBoolean("carnetUser",false); ArrayList<String> h=state.getStringArrayList("history");if(h!=null)history.addAll(h); }
        getWindow().setStatusBarColor(Color.rgb(17,17,17));getWindow().setNavigationBarColor(Color.BLACK);
        if(Build.VERSION.SDK_INT>=35) getWindow().setDecorFitsSystemWindows(true);
        getOnBackInvokedDispatcherCompat(); render();
    }
    private void getOnBackInvokedDispatcherCompat() {
        if(Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,this::back);
    }
    @Override public void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b);b.putString("route",route);b.putString("image",pendingImagePath);b.putString("gestion",selectedGestion);b.putBoolean("carnetUser",carnetUser);b.putStringArrayList("history",new ArrayList<>(history)); }
    @Override public void onBackPressed() { back(); }
    void back() { if(busy)return; hideKeyboard(); if(history.isEmpty()) { if(!route.equals("home")){route="home";render();}else super.onBackPressed(); }else {route=history.removeLast();render();} }
    void go(String s) { hideKeyboard();if(s.equals("carnet")&&!route.equals("carnet"))carnetUser=false;history.addLast(route);route=s;render(); }
    void hideKeyboard() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),0); }
    int dp(float n) {return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    LinearLayout col() {LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row() {LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    GradientDrawable shape(int color,int radius) {GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    GradientDrawable outline(int color,int radius) {GradientDrawable d=shape(bg,radius);d.setStroke(dp(2),color);return d;}
    TextView text(String s,int size,int color,boolean bold) {TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setIncludeFontPadding(false);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    TextView txt(String s) {return text(s,14,fg,false);}
    void add(LinearLayout parent,View v,int w,int h) {parent.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));}
    void weight(LinearLayout parent,View v,int h) {parent.addView(v,new LinearLayout.LayoutParams(0,h<0?h:dp(h),1));}
    void gap(LinearLayout p,int h) {add(p,new View(this),1,h);}
    void pad(View v,int x,int y) {v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    TextView button(String label,Runnable action) {TextView t=text(label,14,PINK,true);t.setGravity(Gravity.CENTER);t.setBackground(shape(card,22));pad(t,14,10);t.setMinHeight(dp(40));t.setOnClickListener(v->action.run());return t;}
    void icon(LinearLayout parent,String name,int color,int size,Runnable action) {SiuIcon i=new SiuIcon(this,name,color);add(parent,i,size,size);if(action!=null){i.setOnClickListener(v->action.run());i.setFocusable(true);i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);i.setContentDescription(switch(name){case "menu"->"Abrir menú";case "back"->"Volver";case "qr"->"Carnet virtual";case "grid"->"Ver cuadrícula";case "list"->"Ver lista";default->name;});}}
    LinearLayout panel(LinearLayout parent,int margin,int padding) {LinearLayout p=col();p.setBackground(shape(card,13));pad(p,padding,padding);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(margin),dp(4),dp(margin),dp(4));parent.addView(p,lp);return p;}
    void title(String s) {((TextView)bar.getChildAt(1)).setText(s);}
    LinearLayout scroll() {ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setClipToPadding(false);body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));LinearLayout l=col();pad(l,0,6);sv.addView(l);return l;}
    void header(String s,boolean home) {
        bar=row();bar.setBackgroundColor(BRAND);pad(bar,18,0);add(root,bar,-1,56);
        icon(bar,home?"menu":"back",WHITE,21,home?()->go("drawer"):this::back);
        TextView t=text(s,20,WHITE,false);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(32),0,0,0);bar.addView(t,lp);
        if(home)icon(bar,"qr",WHITE,21,()->go("carnet"));
    }
    void render() {
        String theme=store.obj("ajustes").optString("tema");boolean light=theme.equals("Claro") || theme.equals("Sistema") && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_NO;
        // References use the dark appearance. System mode follows the actual device setting.
        bg=light?Color.rgb(246,246,246):Color.BLACK;card=light?Color.WHITE:Color.rgb(28,28,28);fg=light?Color.rgb(30,30,30):WHITE;
        frame=new FrameLayout(this);frame.setBackgroundColor(bg);root=col();frame.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);
        String section=route.split(":",2)[0];
        if(section.equals("drawer")){header("Univalle",true);body=col();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));home();drawer();return;}
        header(screenTitle(section),section.equals("home"));body=col();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        switch(section) {
            case "home" -> home();
            case "Social", "Académica", "Recursos", "Tip's" -> category(section);
            case "Materias" -> subjects();
            case "perfil" -> profile();
            case "carnet" -> virtualCard();
            case "ajustes" -> settings();
            case "editor" -> editor(route.contains(":")?route.substring(route.indexOf(':')+1):"");
            case "contactos", "lugares" -> contacts(section.equals("contactos"));
            case "consultas" -> departments();
            case "consulta" -> queries(arg());
            case "nuevaConsulta" -> queryForm(arg());
            case "sugerencias" -> suggestions();
            case "sugerencia" -> suggestionForm(arg());
            case "documentos" -> documents(arg());
            case "horarios" -> schedule(arg());
            case "notas" -> grades();
            case "pagos" -> payments();
            case "costos" -> costs();
            case "tramites" -> records();
            case "tipos" -> procedureTypes();
            case "detalle" -> procedure(arg());
            case "biblioteca" -> library();
            case "descuentos" -> discounts();
            case "correo" -> mail();
            case "proyectos" -> projects();
            case "bancos" -> banks(arg());
            case "ayuda" -> helpList();
            case "ayudaDetalle" -> helpDetail(arg());
            case "acceso" -> access();
            case "materia" -> subjectDetail(arg());
            default -> {TextView t=txt("No se encontraron resultados.");add(body,t,-1,-2);}
        }
    }
    int arg() {try{return Integer.parseInt(route.substring(route.indexOf(':')+1));}catch(Exception e){return 0;}}
    String screenTitle(String s) {return switch(s) {
        case "home" -> "Univalle";case "perfil"->"Perfil de usuario";case "carnet"->"Carnet Virtual";case "ajustes"->"Ajustes";case "editor"->"Editar contenido";
        case "contactos","lugares"->cities[Math.min(arg(),4)];case "consultas"->"Consultas";case "consulta","nuevaConsulta"->"Consulta";case "sugerencias"->"Sugerencia";case "sugerencia"->store.arr("categoriasSugerencias").optString(arg());
        case "documentos"->arg()==0?"Entregados":"Pendientes";case "horarios"->days[Math.min(arg(),5)];case "notas"->"Notas";case "pagos"->"Plan de Pagos";case "costos"->"Matrícula, Costo crédito";
        case "tramites"->"Mis Trámites";case "tipos"->"Tipos de Trámites";case "detalle"->"Tipos de Trámites - Detalle";case "biblioteca"->"Biblioteca";case "descuentos"->"Vitrina de Descuentos";case "correo"->"Contraseña de Correo";case "proyectos"->"Participación en UProyectos";case "bancos"->arg()==0?"Bancos":"Cajas";case "ayuda"->"Sabías que...";case "ayudaDetalle"->store.arr("ayuda").optJSONObject(arg()).optString("titulo");case "acceso"->"SIU Local";case "materia"->"Materia";default->s;
    };}
    void tabs(String[] labels,int selected,java.util.function.IntConsumer action) {
        LinearLayout r=row();r.setBackgroundColor(Color.rgb(128,128,128));add(body,r,-1,48);
        String[] dayLabels={"LUNE\nS","MART\nES","MIÉRC\nOLES","JUEVE\nS","VIERN\nES","SÁBA\nDO"};
        for(int i=0;i<labels.length;i++){final int index=i;LinearLayout c=col();c.setGravity(Gravity.CENTER);TextView t=text(labels==days?dayLabels[i]:labels[i].toUpperCase(Locale.ROOT),labels.length>4?12:14,WHITE,false);t.setLetterSpacing(.08f);t.setGravity(Gravity.CENTER);if(labels.length==5)pad(t,8,0);c.addView(t,new LinearLayout.LayoutParams(-1,0,1));View line=new View(this);line.setBackgroundColor(i==selected?BRAND:Color.TRANSPARENT);add(c,line,-1,2);weight(r,c,-1);c.setOnClickListener(v->action.accept(index));}
    }
    void bottom() {LinearLayout r=row();r.setBackground(shape(BRAND,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(53));p.setMargins(dp(11),dp(6),dp(11),dp(8));body.addView(r,p);for(String name:categories){LinearLayout c=col();c.setGravity(Gravity.CENTER);icon(c,name,WHITE,21,null);gap(c,4);TextView t=text(name,12,WHITE,false);t.setGravity(Gravity.CENTER);add(c,t,-1,-2);weight(r,c,-1);c.setOnClickListener(v->{route=name;render();});}}
    void home() {
        body.setBackgroundColor(BRAND);FrameLayout content=new FrameLayout(this);GradientDrawable rounded=shape(bg,0);rounded.setCornerRadii(new float[]{dp(28),dp(28),dp(28),dp(28),0,0,0,0});content.setBackground(rounded);content.setClipToOutline(true);body.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        ImageView wm=watermark(.10f);FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(dp(260),dp(300),Gravity.TOP|Gravity.RIGHT);mp.topMargin=dp(105);content.addView(wm,mp);
        ScrollView sv=new ScrollView(this);content.addView(sv,new FrameLayout.LayoutParams(-1,-1));LinearLayout list=col();pad(list,10,14);sv.addView(list);
        LinearLayout r=row();pad(r,10,0);TextView menu=text("Menu",14,GRAY,false);weight(r,menu,-2);boolean grid=store.obj("ajustes").optBoolean("cuadricula");icon(r,grid?"list":"grid",GRAY,18,()->{LocalStore.put(store.obj("ajustes"),"cuadricula",!grid);store.save();render();});add(list,r,-1,26);gap(list,10);
        if(grid){
            int[] images={R.drawable.icon_social,R.drawable.icon_materias,R.drawable.icon_academica,R.drawable.icon_recursos,R.drawable.icon_tips};
            for(int i=0;i<categories.length;i+=2){
                LinearLayout rr=row();rr.setGravity(Gravity.TOP);rr.setBaselineAligned(false);
                for(int j=i;j<Math.min(i+2,categories.length);j++){
                    String name=categories[j];LinearLayout c=col();c.setBackground(shape(Color.rgb(247,247,247),21));
                    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(185),1);cp.setMargins(0,0,j%2==0?dp(10):0,dp(12));rr.addView(c,cp);
                    LinearLayout center=col();center.setGravity(Gravity.CENTER);ImageView art=new ImageView(this);art.setImageResource(images[j]);art.setImageTintList(ColorStateList.valueOf(Color.rgb(241,241,245)));art.setScaleType(ImageView.ScaleType.FIT_CENTER);add(center,art,70,70);c.addView(center,new LinearLayout.LayoutParams(-1,0,1));
                    View line=new View(this);line.setBackgroundColor(PINK);add(c,line,-1,1);
                    FrameLayout footer=new FrameLayout(this);GradientDrawable footerShape=shape(Color.rgb(153,153,153),0);footerShape.setCornerRadii(new float[]{0,0,0,0,dp(21),dp(21),dp(21),dp(21)});footer.setBackground(footerShape);
                    TextView t=text(name,14,WHITE,true);t.setGravity(Gravity.CENTER);footer.addView(t,new FrameLayout.LayoutParams(-1,-1));SiuIcon arrow=new SiuIcon(this,"arrow",Color.rgb(180,180,180));FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.RIGHT|Gravity.CENTER_VERTICAL);ap.rightMargin=dp(10);footer.addView(arrow,ap);add(c,footer,-1,50);c.setOnClickListener(v->go(name));
                }
                if(i+1>=categories.length)weight(rr,new View(this),1);add(list,rr,-1,-2);
            }
        }
        else {for(String name:categories){LinearLayout rr=row();FrameLayout rail=new FrameLayout(this);View line=new View(this);line.setBackgroundColor(Color.GRAY);rail.addView(line,new FrameLayout.LayoutParams(dp(1),-1,Gravity.CENTER));View dot=new View(this);dot.setBackground(shape(PINK,20));rail.addView(dot,new FrameLayout.LayoutParams(dp(12),dp(12),Gravity.CENTER));add(rr,rail,40,83);LinearLayout tile=col();tile.setGravity(Gravity.CENTER);tile.setBackground(shape(BRAND,10));icon(tile,name,Color.rgb(183,117,136),46,null);add(rr,tile,71,71);TextView label=text(name,14,fg,true);label.setPadding(dp(10),0,0,0);add(rr,label,-1,-2);rr.setOnClickListener(v->go(name));add(list,rr,-1,83);}}
    }
    void menuItem(LinearLayout p,String iconName,String label,String description,Runnable action) {
        LinearLayout box=panel(p,11,8);LinearLayout r=row();add(box,r,-1,-2);icon(r,iconName,BRAND,31,null);LinearLayout words=col();words.setPadding(dp(10),dp(5),0,dp(4));TextView t=text(label,16,PINK,true);add(words,t,-1,-2);gap(words,3);if(!description.isEmpty())add(words,txt(description),-1,-2);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));box.setMinimumHeight(dp(63));box.setOnClickListener(v->action.run());
    }
    void category(String s) {
        LinearLayout list=scroll();gap(list,1);
        switch(s){
            case "Social" -> {menuItem(list,"edit","Consultas","Consulta al personal de la Universidad.",()->go("consultas"));menuItem(list,"contacts","Contáctenos","Mira cómo puedes contactarte con la Universidad.",()->go("contactos:0"));menuItem(list,"suggestion","Sugerencias","Envía tu sugerencia a la Universidad del Valle.",()->go("sugerencias"));}
            case "Académica" -> {menuItem(list,"folder","Documentos","Mira los documentos entregados.",()->go("documentos:0"));menuItem(list,"clock","Horarios","Mira tu horario.",()->go("horarios:0"));menuItem(list,"document","Notas","Visualiza todas tus notas.",()->go("notas"));}
            case "Recursos" -> {
                String[][] items={{"Biblioteca","Consulta a Biblioteca","Busca un libro en la biblioteca de la Universidad.","biblioteca"},{"location","Lugares de Interés","Busca un lugar de interés en la Universidad.","lugares:0"},{"document","Matrícula y Costo crédito","Mira el costo de la matrícula y el costo de crédito.","costos"},{"document","Mis Trámites","Mira el estado de tus trámites.","tramites"},{"document","Tipo de Trámites","Mira los tipos de trámites que puedes realizar.","tipos"},{"payments","Plan de Pagos","Gestiona tu plan de pagos.","pagos"},{"discount","Vitrina de descuento","Descuentos a los estudiantes de la Universidad.","descuentos"},{"mail","Contraseña de Correo","Restablece tu contraseña de correo educativo.","correo"},{"projects","Participar en UProyectos","Únete a proyectos de sostenibilidad, ayuda y cuidado de la comunidad.","proyectos"}};
                for(String[] item:items)menuItem(list,item[0],item[1],item[2],()->go(item[3]));
            }
            case "Tip's" -> {menuItem(list,"map","Lugares de pago","Mira donde puedes pagar tus mensualidades y más.",()->go("bancos:0"));menuItem(list,"info","Sabías que...","Mira los temas de ayuda que tiene la Universidad para ti.",()->go("ayuda"));}
        }bottom();
    }
    ImageView picture(String value,String fallback,int radius) {ImageView i=new ImageView(this);i.setImageBitmap(store.image(value,fallback));i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setBackground(shape(card,radius));i.setClipToOutline(true);return i;}
    ImageView artwork(String name) {ImageView i=new ImageView(this);Bitmap bitmap=artworkCache.get(name);if(bitmap==null){bitmap=store.image("","artwork/"+name+".png");if(bitmap!=null)artworkCache.put(name,bitmap);}i.setImageBitmap(bitmap);i.setScaleType(ImageView.ScaleType.FIT_CENTER);return i;}
    ImageView watermark(float alpha) {ImageView i=picture(store.obj("imagenes").optString("marcaAgua"),"artwork/logo_carnet.png",0);i.setBackgroundColor(Color.TRANSPARENT);i.setScaleType(ImageView.ScaleType.FIT_CENTER);i.setAlpha(alpha);return i;}
    ImageView flag() {ImageView i=artwork("bandera_bolivia");i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setBackground(shape(card,50));i.setClipToOutline(true);i.setContentDescription("Bandera de Bolivia");return i;}
    void drawer() {
        View scrim=new View(this);scrim.setBackgroundColor(0x99000000);frame.addView(scrim,new FrameLayout.LayoutParams(-1,-1));scrim.setOnClickListener(v->back());
        LinearLayout drawer=col();drawer.setBackground(shape(card,25));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.84f),-1,Gravity.LEFT);frame.addView(drawer,p);
        JSONObject profile=store.obj("perfil");LinearLayout top=col();top.setGravity(Gravity.CENTER);top.setBackground(shape(bg,28));pad(top,10,16);add(drawer,top,-1,185);add(top,picture(profile.optString("foto"),"portrait.jpg",24),100,100);gap(top,9);TextView n=text(profile.optString("nombre"),12,fg,true);n.setGravity(Gravity.CENTER);add(top,n,-1,-2);gap(top,7);TextView career=text(profile.optString("carrera"),10,GRAY,false);career.setGravity(Gravity.CENTER);add(top,career,-1,-2);
        String[][] items={{"home","Página de inicio","home"},{"user","Perfil de usuario","perfil"},{"id","Carnet virtual","carnet"},{"logout","Cerrar sesión","acceso"}};
        for(String[] item:items){LinearLayout r=row();pad(r,12,10);icon(r,item[0],fg,30,null);TextView t=text(item[1],16,fg,false);t.setPadding(dp(14),0,0,0);add(r,t,-1,-2);add(drawer,r,-1,61);r.setOnClickListener(v->{if(item[2].equals("acceso")){new AlertDialog.Builder(this).setTitle("Cerrar sesión local").setMessage("Tus datos se conservarán en este teléfono.").setPositiveButton("Cerrar sesión",(d,w)->{history.clear();route="acceso";render();}).setNegativeButton("Cancelar",null).show();}else{if(item[2].equals("carnet"))carnetUser=false;route=item[2];render();}});}
        FrameLayout middle=new FrameLayout(this);drawer.addView(middle,new LinearLayout.LayoutParams(-1,0,1));ImageView seal=picture(store.obj("imagenes").optString("sello"),"artwork/logo.png",0);seal.setBackgroundColor(Color.TRANSPARENT);seal.setAlpha(.25f);seal.setScaleType(ImageView.ScaleType.FIT_CENTER);middle.addView(seal,new FrameLayout.LayoutParams(dp(140),dp(140),Gravity.CENTER));
        LinearLayout settings=row();pad(settings,12,8);settings.setBackground(shape(bg,35));icon(settings,"settings",fg,30,null);TextView settingLabel=text("Ajustes",16,fg,false);settingLabel.setPadding(dp(14),0,0,0);add(settings,settingLabel,-1,-2);settings.setOnClickListener(v->go("ajustes"));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(61));sp.setMargins(dp(6),0,dp(6),dp(8));drawer.addView(settings,sp);
        frame.setOnClickListener(v->back());drawer.setOnClickListener(v->{});
    }
    void access(){LinearLayout l=scroll();gap(l,60);ImageView seal=picture(store.obj("imagenes").optString("sello"),"seal.jpg",0);LinearLayout center=col();center.setGravity(Gravity.CENTER);add(center,seal,110,110);add(l,center,-1,-2);gap(l,30);TextView t=text("SIU Local",30,fg,true);t.setGravity(Gravity.CENTER);add(l,t,-1,-2);gap(l,30);add(l,button("Entrar con mis datos locales",()->{route="home";render();}),-1,-2);}
    void profile() {
        JSONObject p=store.obj("perfil");LinearLayout list=scroll();list.setPadding(0,dp(7),0,0);
        FrameLayout paper=new FrameLayout(this);paper.setMinimumHeight(dp(600));paper.setBackground(shape(card,20));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2,1);pp.setMargins(dp(11),dp(4),dp(11),0);list.addView(paper,pp);
        LinearLayout box=col();box.setPadding(dp(11),dp(11),dp(11),dp(60));paper.addView(box,new FrameLayout.LayoutParams(-1,-2));
        FrameLayout top=new FrameLayout(this);LinearLayout r=row();r.setGravity(Gravity.BOTTOM);add(r,picture(p.optString("foto"),"portrait.jpg",20),100,100);LinearLayout words=col();words.setPadding(dp(15),0,dp(30),dp(5));add(words,text(p.optString("nombre"),14,fg,true),-1,-2);gap(words,8);add(words,txt(p.optString("nacionalidad")),-1,-2);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));top.addView(r,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.TOP|Gravity.RIGHT);fp.topMargin=dp(4);top.addView(flag(),fp);add(box,top,-1,100);gap(box,25);
        profileFields(box,false);
        LinearLayout edit=col();edit.setGravity(Gravity.CENTER);edit.setBackground(shape(Color.rgb(63,63,63),25));icon(edit,"pencil",WHITE,29,null);edit.setContentDescription("Editar perfil");edit.setOnClickListener(v->go("editor:perfil"));FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.BOTTOM|Gravity.RIGHT);ep.setMargins(0,dp(15),dp(20),dp(20));paper.addView(edit,ep);
    }
    void profileFields(LinearLayout box,boolean carnet) {
        JSONObject p=store.obj("perfil");
        String[][] fields={{"Sede:","sede"},{"Carrera:","carrera"},{"Apoderado:","apoderado"},{"Género:","genero"},{"Fecha Nacimiento:","nacimiento"},{"Correo:","correo"},{"País:","pais"}};
        LinearLayout fieldsBox=col();fieldsBox.setPadding(dp(4),dp(3),0,0);add(box,fieldsBox,-1,-2);
        for(String[] f:fields){LinearLayout r=row();TextView label=text(f[0],14,PINK,true);r.addView(label,new LinearLayout.LayoutParams(0,-2,.472f));TextView val=txt(p.optString(f[1]));r.addView(val,new LinearLayout.LayoutParams(0,-2,.528f));r.setMinimumHeight(dp(carnet?24:27));add(fieldsBox,r,-1,-2);}
        gap(fieldsBox,carnet?0:14);LinearLayout reader=row();add(reader,text("Autenticación con Lector:",14,PINK,true),-2,-2);
        if(carnet)add(reader,txt(p.optBoolean("lector")?" Habilitado":" Deshabilitado"),-2,-2);else{Switch toggle=new Switch(this);toggle.setChecked(p.optBoolean("lector"));toggle.setPadding(dp(12),0,0,0);tintSwitch(toggle);toggle.setOnCheckedChangeListener((b,c)->{LocalStore.put(p,"lector",c);store.save();});add(reader,toggle,-2,-2);}add(fieldsBox,reader,-1,-2);
    }
    void virtualCard() {
        JSONObject p=store.obj("perfil");LinearLayout list=scroll();list.setPadding(0,dp(20),0,0);
        LinearLayout tabRow=row();tabRow.setGravity(Gravity.CENTER);LinearLayout segment=row();segment.setBackground(shape(Color.GRAY,7));segment.setClipToOutline(true);
        for(int i=0;i<2;i++){
            boolean user=i==1;TextView tab=text(user?"Usuario":"Carnet",10,WHITE,true);tab.setGravity(Gravity.CENTER);tab.setSelected(carnetUser==user);tab.setBackground(shape(carnetUser==user?card:Color.TRANSPARENT,7));tab.setFocusable(true);
            tab.setContentDescription(user?"Mostrar datos de usuario":"Mostrar carnet");tab.setOnClickListener(v->{if(carnetUser!=user){carnetUser=user;render();}});add(segment,tab,98,30);
        }
        add(tabRow,segment,-2,30);add(list,tabRow,-1,30);gap(list,12);
        FrameLayout paper=new FrameLayout(this);paper.setMinimumHeight(dp(600));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2,1);pp.setMargins(dp(11),0,dp(11),0);list.addView(paper,pp);
        View backing=new View(this);backing.setBackground(shape(card,20));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1);bp.topMargin=dp(50);paper.addView(backing,bp);
        ImageView wm=watermark(.13f);FrameLayout.LayoutParams wp=new FrameLayout.LayoutParams(-1,dp(365),Gravity.TOP);wp.topMargin=dp(carnetUser?135:180);paper.addView(wm,wp);
        LinearLayout box=col();box.setPadding(dp(11),0,dp(11),dp(60));paper.addView(box,new FrameLayout.LayoutParams(-1,-2));
        FrameLayout imageRow=new FrameLayout(this),portrait=new FrameLayout(this);portrait.addView(picture(p.optString("foto"),"portrait_carnet.png",120),new FrameLayout.LayoutParams(-1,-1));portrait.addView(flag(),new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.BOTTOM|Gravity.LEFT));imageRow.addView(portrait,new FrameLayout.LayoutParams(dp(118),dp(118),Gravity.TOP|Gravity.CENTER_HORIZONTAL));add(box,imageRow,-1,118);
        if(carnetUser){
            gap(box,22);TextView name=text(p.optString("nombre"),14,fg,true);name.setGravity(Gravity.CENTER);add(box,name,-1,-2);gap(box,3);TextView nationality=txt(p.optString("nacionalidad"));nationality.setGravity(Gravity.CENTER);add(box,nationality,-1,-2);gap(box,2);profileFields(box,true);
        }else{
            gap(box,16);TextView name=text(carnetName(p.optString("nombre")),18,fg,true);name.setTextSize(18.5f);name.setGravity(Gravity.CENTER);name.setLineSpacing(dp(8),1);add(box,name,-1,-2);gap(box,8);
            TextView career=text(p.optString("carrera"),14,fg,true);career.setGravity(Gravity.CENTER);add(box,career,-1,-2);gap(box,8);
            View divider=new View(this);divider.setBackgroundColor(Color.rgb(169,182,191));LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(dp(200),dp(2));line.gravity=Gravity.CENTER_HORIZONTAL;box.addView(divider,line);gap(box,18);
            TextView site=text("SEDE "+p.optString("sede"),16,GRAY,true);site.setGravity(Gravity.CENTER);add(box,site,-1,-2);gap(box,8);
            FrameLayout qrPaper=new FrameLayout(this);qrPaper.setBackground(shape(Color.WHITE,30));qrPaper.setClipToOutline(true);pad(qrPaper,10,10);
            ImageView qr=new ImageView(this);qr.setScaleType(ImageView.ScaleType.FIT_CENTER);qr.setContentDescription("Código QR local, sin validación institucional");qrPaper.addView(qr,new FrameLayout.LayoutParams(-1,-1));
            TextView error=text("",13,Color.DKGRAY,false);error.setGravity(Gravity.CENTER);qrPaper.addView(error,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(250),dp(250));qp.gravity=Gravity.CENTER_HORIZONTAL;box.addView(qrPaper,qp);updateCarnetQr(qr,error);gap(box,8);
            FrameLayout refresh=new FrameLayout(this);View circle=new View(this);circle.setBackground(shape(bg==Color.BLACK?Color.rgb(23,23,23):bg,30));refresh.addView(circle,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.CENTER));SiuIcon arrow=new SiuIcon(this,"refresh",BRAND);refresh.addView(arrow,new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.CENTER));refresh.setContentDescription("Actualizar QR local");refresh.setFocusable(true);refresh.setOnClickListener(v->{updateCarnetQr(qr,error);refresh.announceForAccessibility("QR local actualizado");});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(48),dp(48));rp.gravity=Gravity.CENTER_HORIZONTAL;box.addView(refresh,rp);
        }
    }
    String carnetName(String value) {
        String name=value.trim().replaceAll("\\s+"," ");int split=-1;
        for(int i=0;i<name.length();i++)if(name.charAt(i)==' '&&(split<0||Math.abs(i-name.length()/2)<Math.abs(split-name.length()/2)))split=i;
        return split<0?name:name.substring(0,split)+"\n"+name.substring(split+1);
    }
    void updateCarnetQr(ImageView image,TextView error) {
        Bitmap qr=null;String message="Introduce el contenido del QR en Ajustes → Editar contenido → Perfil.";
        try{qr=profileQr();}catch(WriterException|IllegalArgumentException e){message="El contenido es demasiado largo para generar un QR.";}
        image.setImageBitmap(qr);image.setVisibility(qr==null?View.GONE:View.VISIBLE);error.setText(message);error.setVisibility(qr==null?View.VISIBLE:View.GONE);
    }
    void tintSwitch(Switch toggle) {toggle.setThumbTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{BRAND,Color.LTGRAY}));toggle.setTrackTintList(ColorStateList.valueOf(Color.GRAY));}
    Bitmap profileQr() throws WriterException {
        JSONObject p=store.obj("perfil");Bitmap qr=store.image(p.optString("qrImagen"),null);if(qr!=null)return qr;
        String value=p.optString("qrTexto");if(value.trim().isEmpty())return null;
        Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");hints.put(EncodeHintType.MARGIN,2);
        // Match the reference's 53-module grid without copying its credential payload.
        int version=com.google.zxing.qrcode.encoder.Encoder.encode(value,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L,hints).getVersion().getVersionNumber();hints.put(EncodeHintType.QR_VERSION,Math.max(9,version));
        BitMatrix m=new MultiFormatWriter().encode(value,BarcodeFormat.QR_CODE,700,700,hints);qr=Bitmap.createBitmap(700,700,Bitmap.Config.ARGB_8888);int[] pixels=new int[490000];for(int y=0;y<700;y++)for(int x=0;x<700;x++)pixels[y*700+x]=m.get(x,y)?Color.BLACK:Color.WHITE;qr.setPixels(pixels,0,700,0,0,700,700);return qr;
    }
    void showQr() {
        JSONObject p=store.obj("perfil");Bitmap qr;
        try{qr=profileQr();}catch(WriterException|IllegalArgumentException e){message("QR","El contenido es demasiado largo para generar un QR.");return;}
        if(qr==null){message("QR","Introduce el contenido del QR en Ajustes -> Editar contenido -> Perfil.");return;}
        LinearLayout content=col();pad(content,20,15);ImageView image=new ImageView(this);image.setImageBitmap(qr);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(Color.WHITE);add(content,image,-1,280);TextView label=txt(p.optString("nombre"));label.setGravity(Gravity.CENTER);gap(content,12);add(content,label,-1,-2);TextView local=text("Credencial local . sin validación institucional",11,GRAY,false);local.setGravity(Gravity.CENTER);gap(content,8);add(content,local,-1,-2);new AlertDialog.Builder(this).setTitle("Carnet Virtual").setView(content).setPositiveButton("Cerrar",null).show();
    }
    void settings() {
        icon(bar,"more",WHITE,20,this::localTools);bar.getChildAt(bar.getChildCount()-1).setContentDescription("Herramientas locales");
        LinearLayout l=scroll();gap(l,10);LinearLayout p=panel(l,20,0);p.setBackground(shape(Color.rgb(63,63,63),13));p.setPadding(0,dp(4),0,0);add(p,text("Seleccione el tema:",18,Color.LTGRAY,false),-1,-2);LinearLayout r=row();String[] names={"Claro","Oscuro","Sistema"};String[] symbols={"sun","moon","laptop"};int[] colors={Color.rgb(255,218,45),Color.rgb(171,208,251),Color.WHITE};for(int i=0;i<3;i++){String name=names[i];LinearLayout c=col();c.setGravity(Gravity.CENTER);icon(c,symbols[i],colors[i],52,null);RadioButton radio=new RadioButton(this);radio.setText(name);radio.setTextSize(14);radio.setTextColor(Color.LTGRAY);radio.setButtonTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{BRAND,Color.LTGRAY}));radio.setChecked(store.obj("ajustes").optString("tema").equals(name));radio.setMinimumHeight(0);radio.setMinHeight(0);add(c,radio,-2,32);radio.setOnClickListener(v->{LocalStore.put(store.obj("ajustes"),"tema",name);store.save();render();});weight(r,c,-2);}add(p,r,-1,-2);gap(l,13);
        LinearLayout q=panel(l,20,0);q.setBackground(shape(Color.rgb(63,63,63),13));LinearLayout rr=row();TextView text=text("Mostrar resumen de semana:",18,Color.LTGRAY,false);weight(rr,text,-2);Switch toggle=new Switch(this);toggle.setChecked(store.obj("ajustes").optBoolean("resumen"));tintSwitch(toggle);toggle.setOnCheckedChangeListener((b,c)->{LocalStore.put(store.obj("ajustes"),"resumen",c);store.save();});add(rr,toggle,-2,-2);rr.setPadding(0,dp(8),dp(10),dp(8));add(q,rr,-1,50);
    }
    void localTools() {new AlertDialog.Builder(this).setTitle("Herramientas locales").setItems(new String[]{"Editar contenido","Exportar copia de seguridad","Importar copia de seguridad"},(d,index)->{switch(index){case 0->go("editor:");case 1->exportBackup();case 2->importBackup();}}).setNegativeButton("Cancelar",null).show();}
    void metadata(LinearLayout parent,String label,String value) {LinearLayout r=row();add(r,text(label,10,GRAY,false),-2,-2);TextView v=text("  "+value,14,fg,label.equals("GRUPO:"));r.addView(v,new LinearLayout.LayoutParams(0,-2,1));add(parent,r,-1,-2);}
    void subjects() {LinearLayout l=scroll();l.setPadding(0,dp(2),0,dp(6));JSONArray a=store.arr("materias");for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;int index=i;LinearLayout p=panel(l,6,10);LinearLayout r=row();if(m.optString("imagen").isEmpty())add(r,artwork("materia_mensaje"),49,49);else add(r,picture(m.optString("imagen"),null,50),49,49);LinearLayout words=col();words.setPadding(dp(10),0,0,0);add(words,text(m.optString("nombre"),14,fg,true),-1,-2);gap(words,8);metadata(words,"GRUPO:",m.optString("grupo"));gap(words,6);metadata(words,"DOCENTE:",m.optString("docente"));r.addView(words,new LinearLayout.LayoutParams(0,-2,1));add(p,r,-1,-2);p.setMinimumHeight(dp(84));p.setOnClickListener(v->go("materia:"+index));}}
    void subjectDetail(int index){JSONObject m=store.arr("materias").optJSONObject(index);if(m==null)return;LinearLayout l=scroll();LinearLayout p=panel(l,10,15);add(p,text(m.optString("nombre"),19,fg,true),-1,-2);gap(p,12);add(p,txt("Grupo: "+m.optString("grupo")+"\n\nDocente: "+m.optString("docente")),-1,-2);gap(p,16);add(p,button("Ver notas",()->go("notas")),-1,-2);gap(p,8);add(p,button("Editar esta materia",()->go("editor:materias/"+index)),-1,-2);}
    void contacts(boolean details) {
        int selected=Math.min(arg(),4);tabs(cities,selected,i->{route=(details?"contactos:":"lugares:")+i;render();});LinearLayout l=scroll();JSONArray a=store.arr("contactos");for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null||!c.optString("ciudad").equalsIgnoreCase(cities[selected]))continue;LinearLayout p=panel(l,6,7);add(p,text(c.optString("nombre"),details?16:14,fg,true),-1,-2);gap(p,details?10:12);if(details){contactLink(p,"location",c.optString("direccion"),Color.RED,()->open("geo:0,0?q="+Uri.encode(c.optString("direccion"))));contactLink(p,"mail",c.optString("correo"),Color.rgb(105,161,166),()->open("mailto:"+c.optString("correo")));contactLink(p,"phone",c.optString("telefono"),Color.rgb(78,175,66),()->open("tel:"+c.optString("telefono")));gap(p,9);}else{TextView address=txt(c.optString("direccion"));add(p,address,-1,-2);p.setOnClickListener(v->open("geo:0,0?q="+Uri.encode(c.optString("direccion"))));}}
    }
    void contactLink(LinearLayout p,String symbol,String value,int color,Runnable action){LinearLayout r=row();r.setPadding(dp(16),0,0,0);icon(r,symbol,color,24,null);TextView t=txt(value);t.setPaintFlags(t.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);t.setPadding(dp(6),0,0,0);t.setSingleLine(true);add(r,t,-1,-2);add(p,r,-1,32);r.setOnClickListener(v->{if(!value.trim().isEmpty())action.run();});}
    void departments(){LinearLayout l=scroll();JSONArray a=store.arr("departamentos");for(int i=0;i<a.length();i++){int index=i;String dep=a.optString(i);int count=0;JSONArray queries=store.arr("consultas");for(int j=0;j<queries.length();j++)if(queries.optJSONObject(j)!=null&&queries.optJSONObject(j).optString("departamento").equals(dep))count++;LinearLayout p=panel(l,6,7);add(p,text(dep,14,fg,true),-1,-2);gap(p,8);add(p,txt("Pendientes:  "+count+"  Abiertas:  0  Cerradas:  0"),-1,-2);p.setOnClickListener(v->go("consulta:"+index));}}
    void queries(int index){String dep=store.arr("departamentos").optString(index);title(dep);LinearLayout l=scroll();boolean any=false;JSONArray a=store.arr("consultas");for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q!=null&&q.optString("departamento").equals(dep)){any=true;LinearLayout p=panel(l,8,12);add(p,text(q.optString("asunto"),16,PINK,true),-1,-2);gap(p,8);add(p,txt(q.optString("mensaje")),-1,-2);gap(p,8);add(p,text("Pendiente . guardada localmente",12,GRAY,false),-1,-2);}}if(!any)empty(l);add(l,button("Nueva consulta",()->go("nuevaConsulta:"+index)),-1,-2);}
    EditText input(LinearLayout p,String hint,String value,boolean multi,int height) {EditText e=new EditText(this);e.setTextColor(fg);e.setHintTextColor(GRAY);e.setTextSize(14);e.setHint(hint);e.setText(value);e.setSingleLine(!multi);e.setGravity(multi?Gravity.TOP:Gravity.CENTER_VERTICAL);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|(multi?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));e.setBackground(shape(card,13));pad(e,10,12);add(p,e,-1,height);return e;}
    void queryForm(int index){LinearLayout l=scroll();pad(l,8,10);EditText subject=input(l,"Asunto","",false,48);gap(l,10);EditText message=input(l,"Escribe tu consulta","",true,160);gap(l,12);add(l,button("Guardar consulta",()->{if(subject.getText().toString().trim().isEmpty()||message.getText().toString().trim().isEmpty()){toast("Completa el asunto y el mensaje");return;}JSONObject q=new JSONObject();LocalStore.put(q,"departamento",store.arr("departamentos").optString(index));LocalStore.put(q,"asunto",subject.getText().toString());LocalStore.put(q,"mensaje",message.getText().toString());store.arr("consultas").put(q);store.save();toast("Consulta guardada en este teléfono");back();}),-1,-2);}
    void suggestions(){LinearLayout l=scroll();JSONArray a=store.arr("categoriasSugerencias");for(int i=0;i<a.length();i++){int index=i;LinearLayout p=panel(l,6,6);LinearLayout r=row();add(r,picture(store.obj("imagenes").optString("sello"),"seal.jpg",0),51,51);TextView t=text(a.optString(i),14,fg,true);t.setPadding(dp(10),0,0,0);add(r,t,-1,-2);add(p,r,-1,-2);p.setOnClickListener(v->go("sugerencia:"+index));}}
    void suggestionForm(int index){LinearLayout l=scroll();pad(l,7,7);EditText e=input(l,"Ingrese su sugerencia","",true,150);gap(l,12);LinearLayout r=row();r.setGravity(Gravity.RIGHT);add(r,button("Enviar sugerencia",()->{if(e.getText().toString().trim().isEmpty()){e.setError("Escribe una sugerencia");return;}JSONObject s=new JSONObject();LocalStore.put(s,"categoria",store.arr("categoriasSugerencias").optString(index));LocalStore.put(s,"texto",e.getText().toString());store.arr("sugerencias").put(s);store.save();message("Sugerencia guardada","Se guardó localmente en el teléfono.");e.setText("");}),181,40);add(l,r,-1,-2);}
    void documentValue(LinearLayout parent,String label,String value){LinearLayout r=row();r.setPadding(dp(16),0,0,0);add(r,text(label+" ",14,GRAY,false),-2,-2);add(r,txt(value),-2,-2);add(parent,r,-1,-2);}
    void documents(int tab){
        tabs(new String[]{"Entregados","Pendientes"},tab,i->{route="documentos:"+i;render();});LinearLayout l=scroll();l.setPadding(0,dp(2),0,dp(6));boolean found=false;JSONArray a=store.arr("documentos");
        for(int i=0;i<a.length();i++){
            JSONObject d=a.optJSONObject(i);if(d==null||d.optBoolean("pendiente")!=(tab==1))continue;found=true;LinearLayout p=panel(l,6,12);LinearLayout r=row();add(r,text(d.optString("cantidad"),36,PINK,true),37,-2);LinearLayout content=col();
            TextView name=text(d.optString("nombre"),16,fg,true);name.setMaxWidth(dp(250));name.setLineSpacing(dp(6),1);add(content,name,-2,-2);gap(content,18);documentValue(content,"RECIBIDO:",d.optString("recibido"));gap(content,7);documentValue(content,"VENCE:",d.optString("vence"));gap(content,7);documentValue(content,"TIPO:",d.optString("tipo"));
            r.addView(content,new LinearLayout.LayoutParams(0,-2,1));add(p,r,-1,-2);p.setMinimumHeight(dp(146));
        }
        if(!found)empty(l);
    }
    void schedule(int day){
        day=Math.min(day,5);final int selected=day;tabs(days,selected,i->{route="horarios:"+i;render();});LinearLayout l=scroll();pad(l,10,14);add(l,text(days[selected]+" - "+store.arr("gestiones").optString(0),20,fg,false),-1,-2);gap(l,12);
        // Slot labels are centered on timeline ticks; align the first tick below the heading.
        LinearLayout timeline=col();LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=-dp(32);l.addView(timeline,tp);
        JSONArray a=store.arr("horarios");boolean found=false;for(int i=0;i<a.length();i++){
            JSONObject h=a.optJSONObject(i);if(h==null||!h.optString("dia").equalsIgnoreCase(days[selected]))continue;if(!found&&!h.optString("materia").isEmpty()){tp.topMargin=0;timeline.setLayoutParams(tp);}found=true;LinearLayout r=row();LinearLayout time=col();pad(time,8,0);add(time,text(h.optString("inicio"),14,fg,true),-1,-2);gap(time,4);add(time,text(h.optString("fin"),12,fg,false),-1,-2);add(r,time,53,-2);
            if(!h.optString("materia").isEmpty()){View stripe=new View(this);stripe.setBackground(shape(Color.rgb(144,177,202),4));add(r,stripe,10,77);LinearLayout c=col();c.setPadding(dp(8),0,0,0);add(c,text(h.optString("materia"),14,fg,true),-1,-2);gap(c,3);add(c,txt("Aula: "+h.optString("aula")+"\nGrupo: "+h.optString("grupo")+"\nDocente: "+h.optString("docente")),-1,-2);r.addView(c,new LinearLayout.LayoutParams(0,-2,1));}add(timeline,r,-1,93);
        }
        if(!found){l.removeView(timeline);empty(l);}
    }
    void selector(LinearLayout p,String label,String selected,String[] options,java.util.function.Consumer<String> action){LinearLayout r=row();pad(r,10,10);add(r,txt(label),-2,-2);LinearLayout select=col();select.setBackground(shape(card,8));TextView choice=txt(selected.isEmpty()?"Seleccione una opción":selected);choice.setTextColor(selected.isEmpty()?GRAY:fg);pad(choice,4,6);add(select,choice,-1,-2);View underline=new View(this);underline.setBackgroundColor(Color.LTGRAY);add(select,underline,-1,1);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(35),1);lp.setMargins(dp(10),0,0,0);r.addView(select,lp);select.setOnClickListener(v->{String[] entries=new String[options.length+1];entries[0]="Seleccione una opción";System.arraycopy(options,0,entries,1,options.length);AlertDialog dialog=new AlertDialog.Builder(this).setItems(entries,(d,w)->{String value=w==0?"":options[w-1];choice.setText(value.isEmpty()?entries[0]:value);choice.setTextColor(value.isEmpty()?GRAY:fg);action.accept(value);}).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(d->{ListView list=dialog.getListView();list.setDivider(null);});dialog.show();});add(p,r,-1,-2);}
    String[] strings(JSONArray a){String[] s=new String[a.length()];for(int i=0;i<s.length;i++)s[i]=a.optString(i);return s;}
    void gradeValue(LinearLayout parent,String label,String value,boolean bold){LinearLayout r=row();add(r,text(label,14,fg,bold),90,-2);add(r,text(value,14,fg,bold),-2,-2);add(parent,r,-1,-2);}
    void grades(){
        gap(body,16);selector(body,"Gestión:",selectedGestion,strings(store.arr("gestiones")),s->{selectedGestion=s;render();});LinearLayout l=scroll();if(selectedGestion.isEmpty())return;
        JSONArray a=selectedGestion.equals(store.arr("gestiones").optString(0))?store.arr("materias"):store.obj("notasAnteriores").optJSONArray(selectedGestion);if(a==null||a.length()==0){empty(l);return;}
        for(int i=0;i<a.length();i++){
            JSONObject m=a.optJSONObject(i);if(m==null)continue;LinearLayout p=panel(l,11,6);add(p,text(m.optString("nombre"),20,fg,false),-1,-2);gap(p,5);LinearLayout r=row();r.setGravity(Gravity.TOP);LinearLayout left=col();gradeValue(left,"Grupo:",m.optString("grupo"),true);
            for(String[] f:new String[][]{{"Parcial 1:","parcial1"},{"Parcial 2:","parcial2"},{"Parcial 3:","parcial3"},{"Parcial 4:","parcial4"},{"Exámen Final:","examen"}}){gap(left,3);gradeValue(left,f[0],m.optString(f[1]),false);}
            add(r,left,145,-2);TextView fin=text("Nota Final\n"+m.optString("final"),20,fg,false);fin.setGravity(Gravity.CENTER);fin.setPadding(0,dp(4),0,0);fin.setLineSpacing(dp(3),1);add(r,fin,-2,-2);add(p,r,-1,-2);
        }
    }
    void payments(){
        LinearLayout l=scroll();pad(l,0,7);TextView amount=text("Monto a pagar: "+store.obj("economico").optString("montoPagar"),14,fg,true);pad(amount,10,5);add(l,amount,-1,-2);
        LinearLayout alert=row();pad(alert,25,12);icon(alert,"warning",BRAND,30,null);TextView warning=text(store.obj("economico").optString("aviso"),14,fg,true);warning.setPadding(dp(9),0,0,0);alert.addView(warning,new LinearLayout.LayoutParams(0,-2,1));add(l,alert,-1,-2);TextView title=txt("Plan de pagos");pad(title,10,7);add(l,title,-1,-2);
        JSONArray a=store.arr("pagos");for(int i=0;i<a.length();i++){
            JSONObject q=a.optJSONObject(i);if(q==null)continue;
            LinearLayout p=panel(l,11,11);p.setGravity(Gravity.CENTER_VERTICAL);p.setMinimumHeight(dp(83));LinearLayout r=row();LinearLayout name=col();name.setGravity(Gravity.CENTER);
            TextView n=text(q.optString("nombre"),14,fg,true);n.setGravity(Gravity.CENTER);add(name,n,-1,-2);gap(name,7);
            boolean paid=q.optBoolean("pagada");TextView badge=text(paid?"PAGADA":"POR\nPAGAR",12,WHITE,false);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(paid?Color.rgb(21,119,5):Color.rgb(190,71,0),5));pad(badge,5,2);add(name,badge,paid?60:70,-2);add(r,name,80,-2);
            TextView desc=txt("Monto: "+q.optString("monto")+"\nA cuenta: "+q.optString("aCuenta")+"\nVencimiento: "+q.optString("vencimiento"));desc.setLineSpacing(dp(3),1);r.addView(desc,new LinearLayout.LayoutParams(0,-2,1));add(p,r,-1,-2);
        }
    }
    void costs(){LinearLayout l=scroll();JSONObject e=store.obj("economico");for(String[] f:new String[][]{{"COSTO DE CREDITO","costoCredito","Costo del crédito:"},{"COSTO DE MATRICULA","costoMatricula","Costo de la matrícula:"}}){LinearLayout p=panel(l,9,15);LinearLayout r=row();r.setGravity(Gravity.CENTER);add(r,text(f[0]+"   ",14,fg,true),-2,-2);icon(r,"money",Color.rgb(77,175,80),21,null);add(p,r,-1,-2);gap(p,26);add(p,text("Gestión actual:  "+e.optString("gestion"),14,fg,true),-1,-2);gap(p,22);add(p,text(f[2]+"  "+e.optString(f[1]),14,fg,true),-1,-2);gap(p,18);}}
    void records(){LinearLayout l=scroll();JSONArray a=store.arr("misTramites");if(a.length()==0){empty(l);TextView t=button("Ver tipos de trámites",()->go("tipos"));add(l,t,-1,-2);}else for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);LinearLayout p=panel(l,10,14);add(p,text(o==null?a.optString(i):o.optString("nombre"),16,fg,true),-1,-2);if(o!=null)add(p,txt(o.optString("estado")),-1,-2);}}
    void procedureTypes(){LinearLayout l=scroll();JSONArray a=store.arr("tiposTramites");for(int i=0;i<a.length();i++){JSONObject t=a.optJSONObject(i);if(t==null)continue;int index=i;LinearLayout p=panel(l,10,11);LinearLayout r=row();LinearLayout left=col();add(left,picture(store.obj("imagenes").optString("sello"),"seal.jpg",0),65,55);gap(left,8);add(left,text(t.optBoolean("factura")?"SI necesita factura.":"Sin factura.",10,GRAY,true),90,-2);add(r,left,92,-2);LinearLayout words=col();add(words,text(t.optString("nombre"),14,fg,true),-1,-2);gap(words,9);add(words,text(t.optString("dias")+" días estimados.",10,GRAY,true),-1,-2);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));icon(r,"chevron",GRAY,16,null);add(p,r,-1,-2);p.setMinimumHeight(dp(98));p.setOnClickListener(v->go("detalle:"+index));}}
    void procedure(int index){JSONObject t=store.arr("tiposTramites").optJSONObject(index);if(t==null)return;LinearLayout l=scroll();gap(l,6);LinearLayout p=panel(l,11,15);add(p,text(t.optString("nombre"),14,fg,true),-1,-2);gap(p,10);add(p,txt(t.optString("dias")+" días estimados.\n\n"+(t.optBoolean("factura")?"SI necesitas factura.":"Sin factura.")+"\n\nCosto: "+t.optString("costo")),-1,-2);gap(l,22);TextView d=text("DETALLES DEL DOCUMENTO",12,GRAY,false);pad(d,10,0);add(l,d,-1,-2);LinearLayout requirements=panel(l,10,9);add(requirements,text("REQUISITOS",14,fg,true),-1,-2);JSONArray a=t.optJSONArray("requisitos");if(a!=null)for(int i=0;i<a.length();i++){LinearLayout r=row();pad(r,7,12);weight(r,txt(a.optString(i)),-2);icon(r,"document",fg,24,null);add(requirements,r,-1,55);}LinearLayout docs=panel(l,10,9);add(docs,text("DOCUMENTOS",14,fg,true),-1,-2);gap(docs,24);empty(docs,"Sin documentos");}
    void library(){LinearLayout top=row();pad(top,8,8);EditText search=new EditText(this);search.setTextColor(fg);search.setHintTextColor(GRAY);search.setTextSize(14);search.setHint("Ingrese título o autor");search.setSingleLine(true);search.setBackground(shape(card,12));pad(search,6,8);weight(top,search,40);TextView btn=button("Buscar",()->{});btn.setBackground(outline(BRAND,16));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(100),dp(40));bp.setMargins(dp(16),0,0,0);top.addView(btn,bp);add(body,top,-1,-2);LinearLayout results=scroll();btn.setOnClickListener(v->{results.removeAllViews();String query=search.getText().toString().toLowerCase(Locale.ROOT).trim();if(query.isEmpty()){search.setError("Introduce un título o autor");return;}JSONArray a=store.arr("libros");boolean any=false;for(int i=0;i<a.length();i++){JSONObject book=a.optJSONObject(i);if(book!=null&&(book.optString("titulo")+book.optString("autor")).toLowerCase(Locale.ROOT).contains(query)){any=true;LinearLayout p=panel(results,8,12);add(p,text(book.optString("titulo"),16,PINK,true),-1,-2);gap(p,8);add(p,txt(book.optString("autor")+"\n"+book.optString("ubicacion")),-1,-2);}}if(!any)empty(results);hideKeyboard();});icon(bar,"search",WHITE,26,()->{search.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT);});}
    void discounts(){TextView t=text("Lista de rubros",16,fg,false);t.setGravity(Gravity.CENTER);pad(t,0,8);add(body,t,-1,-2);selector(body,"Ciudad:","",new String[]{"COCHABAMBA"},s->{LinearLayout results=(LinearLayout)((ScrollView)body.getChildAt(2)).getChildAt(0);results.removeAllViews();JSONArray a=store.arr("descuentos");if(a.length()==0)empty(results);for(int i=0;i<a.length();i++){JSONObject d=a.optJSONObject(i);if(d==null)continue;LinearLayout p=panel(results,10,12);if(!d.optString("imagen").isEmpty())add(p,picture(d.optString("imagen"),null,10),-1,130);add(p,text(d.optString("nombre"),17,PINK,true),-1,-2);gap(p,7);add(p,txt(d.optString("detalle")),-1,-2);}});scroll();}
    void mail(){LinearLayout l=scroll();pad(l,8,8);JSONObject c=store.obj("correo");add(l,text("Cuenta:  "+store.obj("perfil").optString("correo"),14,PINK,true),-1,-2);gap(l,20);TextView password=text("Contraseña:  "+c.optString("claveLocal"),14,PINK,true);add(l,password,-1,-2);gap(l,18);LinearLayout r=row();r.setGravity(Gravity.CENTER);add(r,button("Restablecer Contraseña",()->new AlertDialog.Builder(this).setTitle("Restablecer clave local").setMessage("Se cambiará solo el dato de demostración guardado en esta aplicación.").setNegativeButton("Cancelar",null).setPositiveButton("Restablecer",(d,w)->{LocalStore.put(c,"claveLocal","Local"+new java.security.SecureRandom().nextInt(100000));store.save();render();}).show()),-2,40);add(l,r,-1,-2);gap(l,20);add(l,text("Para acceder al correo, ingrese a:",14,PINK,false),-1,-2);gap(l,20);TextView link=text(c.optString("enlace"),17,Color.rgb(184,139,213),false);link.setGravity(Gravity.CENTER);link.setOnClickListener(v->open(c.optString("enlace")));add(l,link,-1,-2);gap(l,20);add(l,txt(c.optString("nota")),-1,-2);gap(l,20);add(l,txt("En caso de olvido, no disponibilidad de contraseña presione en el botón 'Restablecer Contraseña'."),-1,-2);gap(l,18);add(l,text("Datos de demostración locales. Esta opción no cambia tu cuenta institucional.",12,GRAY,false),-1,-2);}
    void projects(){LinearLayout l=scroll();JSONObject p=store.obj("proyectos");LinearLayout intro=panel(l,8,14);add(intro,text(p.optString("saludo"),14,fg,true),-1,-2);gap(intro,14);add(intro,txt(p.optString("mensaje")),-1,-2);gap(l,16);TextView question=txt("¿Porque deseas participar de nuestros proyectos?");pad(question,8,0);add(l,question,-1,-2);gap(l,14);LinearLayout form=panel(l,8,10);EditText e=input(form,"Explica tu motivo",p.optString("motivo"),true,180);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(450)});TextView count=text(e.length()+"/450",12,GRAY,false);count.setGravity(Gravity.RIGHT);add(form,count,-1,-2);e.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable s){LocalStore.put(p,"motivo",s.toString());store.save();count.setText(s.length()+"/450");}});gap(l,12);TextView register=button(p.optBoolean("registrado")?"PARTICIPACIÓN GUARDADA":"REGISTRAR MI PARTICIPACIÓN",()->{if(e.getText().toString().trim().isEmpty()){e.setError("Escribe tu motivo");return;}confirmProject();});register.setBackground(outline(BRAND,25));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(42));rp.setMargins(dp(45),0,dp(45),0);l.addView(register,rp);}
    void confirmProject(){JSONObject p=store.obj("perfil");LinearLayout l=col();pad(l,15,12);add(l,txt("Número de celular"),-1,-2);EditText phone=input(l,"Número de celular",p.optString("celular"),false,45);phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);gap(l,12);add(l,txt("Correo electrónico"),-1,-2);EditText email=input(l,"Correo electrónico",p.optString("correoPersonal"),false,45);email.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("PRIMERO DEBES CONFIRMAR TUS DATOS").setView(l).setNegativeButton("CANCELAR",null).setPositiveButton("CONFIRMAR",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{if(phone.length()<5||!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()){toast("Introduce un teléfono y correo válidos");return;}LocalStore.put(p,"celular",phone.getText().toString());LocalStore.put(p,"correoPersonal",email.getText().toString());LocalStore.put(store.obj("proyectos"),"registrado",true);store.save();dialog.dismiss();message("Participación guardada","Se guardó en este teléfono.");render();}));dialog.show();}
    void banks(int tab){tabs(new String[]{"Bancos","Cajas"},tab,i->{route="bancos:"+i;render();});JSONArray a=store.arr(tab==0?"bancos":"cajas");String[] labels=new String[a.length()];for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);labels[i]=o==null?a.optString(i):o.optString("nombre");}LinearLayout results=col();selector(body,tab==0?"Bancos:":"Cajas:","",labels,s->{results.removeAllViews();for(int i=0;i<a.length();i++)if(labels[i].equals(s)){JSONObject o=a.optJSONObject(i);if(o!=null){LinearLayout p=panel(results,10,14);add(p,text(s,17,PINK,true),-1,-2);gap(p,12);add(p,txt(o.optString("detalle")),-1,-2);}}});ScrollView sv=new ScrollView(this);sv.addView(results);body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));}
    void helpList(){LinearLayout l=scroll();JSONArray a=store.arr("ayuda");for(int i=0;i<a.length();i++){int index=i;JSONObject h=a.optJSONObject(i);if(h==null)continue;LinearLayout p=panel(l,15,0);p.setBackground(shape(Color.rgb(63,63,63),14));LinearLayout r=row();View stripe=new View(this);stripe.setBackgroundColor(PINK);add(r,stripe,8,80);TextView t=text(h.optString("titulo"),14,WHITE,true);t.setPadding(dp(8),0,0,0);add(r,t,-1,-2);add(p,r,-1,-2);p.setClipToOutline(true);p.setOnClickListener(v->go("ayudaDetalle:"+index));}}
    void helpDetail(int index){JSONObject o=store.arr("ayuda").optJSONObject(index);if(o==null)return;LinearLayout l=scroll();JSONArray a=o.optJSONArray("textos");if(a!=null)for(int i=0;i<a.length();i++){LinearLayout p=panel(l,10,18);add(p,txt(a.optString(i)),-1,-2);}}
    void empty(LinearLayout l){empty(l,"No se encontraron resultados.");}
    void empty(LinearLayout l,String s){TextView t=txt(s);t.setGravity(Gravity.CENTER);pad(t,8,7);add(l,t,-1,-2);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    void message(String t,String m){new AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("Aceptar",null).show();}
    void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){message("Abrir enlace","No hay una aplicación disponible para este enlace.");}}
    abstract static class SimpleWatcher implements TextWatcher {public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){} }

    // The editor is separate from the reference screens. Every displayed data field is editable.
    String escapeKey(String key){return key.replace("~","~0").replace("/","~1");}
    String unescapeKey(String key){return key.replace("~1","/").replace("~0","~");}
    Object resolve(String path){Object o=store.data;if(path.isEmpty())return o;for(String encoded:path.split("/")){String part=unescapeKey(encoded);if(o instanceof JSONObject j)o=j.opt(part);else if(o instanceof JSONArray a){try{o=a.opt(Integer.parseInt(part));}catch(Exception e){return null;}}else return null;}return o;}
    void setPath(String path,Object value){int slash=path.lastIndexOf('/');String parent=slash<0?"":path.substring(0,slash),key=unescapeKey(path.substring(slash+1));Object o=resolve(parent);if(o instanceof JSONObject j)LocalStore.put(j,key,value);else if(o instanceof JSONArray a)try{a.put(Integer.parseInt(key),value);}catch(Exception e){throw new IllegalArgumentException(e);}store.save();}
    String label(String key){return switch(key){case "perfil"->"Perfil, foto y QR";case "qrTexto"->"Contenido del QR generado";case "qrImagen"->"Imagen QR (sustituye al QR generado)";case "foto"->"Foto de perfil";case "notasAnteriores"->"Notas de gestiones anteriores";case "claveLocal"->"Contraseña de demostración";case "imagenes"->"Imágenes de la aplicación";case "marcaAgua"->"Marca de agua";case "lector"->"Autenticación con lector";case "montoPagar"->"Monto a pagar";case "aCuenta"->"A cuenta";case "correoPersonal"->"Correo personal";case "tiposTramites"->"Tipos de trámites";case "misTramites"->"Mis trámites";case "categoriasSugerencias"->"Categorías de sugerencias";default->key.isEmpty()?"Contenido":Character.toUpperCase(key.charAt(0))+key.substring(1).replaceAll("([a-z])([A-Z])","$1 $2");};}
    boolean imageKey(String key){return key.equals("foto")||key.equals("imagen")||key.equals("qrImagen")||key.equals("sello")||key.equals("marcaAgua");}
    String summary(Object value){if(value instanceof JSONObject o){for(String k:new String[]{"nombre","titulo","asunto","dia","categoria"})if(!o.optString(k).isEmpty())return o.optString(k);return "Registro";}return String.valueOf(value);}
    void editor(String path){
        if(path.equals("notasAnteriores")){JSONArray gestiones=store.arr("gestiones");for(int i=1;i<gestiones.length();i++){String gestion=gestiones.optString(i);if(!store.obj("notasAnteriores").has(gestion))LocalStore.put(store.obj("notasAnteriores"),gestion,new JSONArray());}store.save();}
        Object o=resolve(path);LinearLayout l=scroll();pad(l,8,6);TextView info=text(path.isEmpty()?"Los cambios se guardan al editar. Abre una sección para modificar su contenido.":label(path.substring(path.lastIndexOf('/')+1))+" . Guardado automático",13,GRAY,false);pad(info,5,8);add(l,info,-1,-2);
        if(o instanceof JSONObject j){Iterator<String> keys=j.keys();while(keys.hasNext()){String k=keys.next();editorField(l,path.isEmpty()?escapeKey(k):path+"/"+escapeKey(k),k,j.opt(k));}}
        else if(o instanceof JSONArray a){for(int i=0;i<a.length();i++){int index=i;String child=path+"/"+i;Object value=a.opt(i);if(value instanceof JSONObject||value instanceof JSONArray){LinearLayout p=panel(l,0,10);LinearLayout r=row();TextView t=text((i+1)+". "+summary(value),15,PINK,true);weight(r,t,-2);TextView remove=button("*",()->new AlertDialog.Builder(this).setTitle("Eliminar registro").setMessage(summary(value)).setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{a.remove(index);store.save();render();}).show());add(r,remove,42,40);add(p,r,-1,-2);t.setOnClickListener(v->go("editor:"+child));}
                else {LinearLayout p=panel(l,0,8);editorField(p,child,"Elemento "+(i+1),value);add(p,button("Eliminar",()->{a.remove(index);store.save();render();}),-1,-2);}}
            gap(l,10);add(l,button("+ Añadir registro",()->{a.put(template(path,a));store.save();render();}),-1,-2);
        }else empty(l,"Este campo no está disponible.");
        gap(l,20);add(l,button("Volver",this::back),-1,-2);
    }
    void editorField(LinearLayout parent,String path,String key,Object value){
        if(value instanceof JSONObject || value instanceof JSONArray){LinearLayout p=panel(parent,0,12);LinearLayout r=row();TextView t=text(label(key),16,PINK,true);weight(r,t,-2);icon(r,"chevron",GRAY,18,null);add(p,r,-1,-2);gap(p,5);add(p,text(value instanceof JSONArray a?a.length()+" registros":"Editar campos",12,GRAY,false),-1,-2);p.setOnClickListener(v->go("editor:"+path));}
        else if(value instanceof Boolean b){LinearLayout p=panel(parent,0,8);LinearLayout r=row();weight(r,txt(label(key)),-2);Switch s=new Switch(this);s.setChecked(b);s.setOnCheckedChangeListener((v,c)->setPath(path,c));add(r,s,-2,-2);add(p,r,-1,-2);}
        else if(imageKey(key)){
            LinearLayout p=panel(parent,0,10);add(p,text(label(key),15,PINK,true),-1,-2);gap(p,8);String current=String.valueOf(value);ImageView preview=picture(current,key.equals("foto")?"portrait.jpg":key.equals("sello")?"seal.jpg":key.equals("marcaAgua")?"watermark.jpg":null,10);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);add(p,preview,-1,110);LinearLayout r=row();weight(r,button("Cambiar imagen",()->pickImage(path)),42);weight(r,button("Quitar",()->{setPath(path,"");render();}),42);add(p,r,-1,-2);
            if(key.equals("qrImagen")){gap(p,6);add(p,text("Sin imagen se genera un QR con el texto del campo siguiente.",12,GRAY,false),-1,-2);add(p,button("Ver QR",this::showQr),-1,-2);}
        }else{LinearLayout p=panel(parent,0,8);add(p,text(label(key),13,PINK,true),-1,-2);EditText e=new EditText(this);e.setTextColor(fg);e.setHintTextColor(GRAY);e.setTextSize(15);e.setText(value==null?"":String.valueOf(value));e.setSelectAllOnFocus(false);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setMaxLines(8);e.setMinHeight(dp(43));add(p,e,-1,-2);e.addTextChangedListener(new SimpleWatcher(){@Override public void afterTextChanged(Editable s){setPath(path,s.toString());}});}
    }
    Object template(String path,JSONArray a){
        try{
            String key=path.substring(path.lastIndexOf('/')+1);JSONObject defaults=new JSONObject(new String(readAsset("defaults.json"),java.nio.charset.StandardCharsets.UTF_8));JSONArray original=defaults.optJSONArray(key);
            if(path.startsWith("notasAnteriores/"))return clearTemplate(new JSONObject(defaults.getJSONArray("materias").getJSONObject(0).toString()));
            if(original!=null&&original.length()>0){Object first=original.get(0);if(first instanceof JSONObject j)return clearTemplate(new JSONObject(j.toString()));if(first instanceof String)return "Nuevo elemento";}
            if(a.length()>0&&a.opt(0) instanceof JSONObject j)return clearTemplate(new JSONObject(j.toString()));
            return switch(key){
                case "libros"->new JSONObject("{\"titulo\":\"Nuevo libro\",\"autor\":\"\",\"ubicacion\":\"\"}");
                case "descuentos"->new JSONObject("{\"nombre\":\"Nuevo descuento\",\"detalle\":\"\",\"imagen\":\"\"}");
                case "bancos","cajas"->new JSONObject("{\"nombre\":\"Nuevo lugar\",\"detalle\":\"\"}");
                case "misTramites"->new JSONObject("{\"nombre\":\"Nuevo trámite\",\"estado\":\"Pendiente\"}");
                case "consultas"->new JSONObject("{\"departamento\":\"Bienestar Estudiantil\",\"asunto\":\"Nueva consulta\",\"mensaje\":\"\"}");
                case "sugerencias"->new JSONObject("{\"categoria\":\"ACADEMICO\",\"texto\":\"\"}");
                case "Gestión 1/2026","Gestión 2/2025"->clearTemplate(new JSONObject(defaults.getJSONArray("materias").getJSONObject(0).toString()));
                default->"Nuevo elemento";
            };
        }catch(Exception e){return "Nuevo elemento";}
    }
    JSONObject clearTemplate(JSONObject j){Iterator<String> keys=j.keys();while(keys.hasNext()){String k=keys.next();Object v=j.opt(k);if(v instanceof String)LocalStore.put(j,k,k.equals("nombre")||k.equals("titulo")?"Nuevo registro":"");else if(v instanceof Boolean)LocalStore.put(j,k,false);else if(v instanceof JSONArray)LocalStore.put(j,k,new JSONArray());}return j;}
    byte[] readAsset(String path)throws java.io.IOException{try(java.io.InputStream in=getAssets().open(path)){return LocalStore.bytes(in);}}
    void pickImage(String path){pendingImagePath=path;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,71);}
    void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"SIU-local-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT).format(new Date())+".zip");startActivityForResult(i,72);}
    void importBackup(){new AlertDialog.Builder(this).setTitle("Importar copia").setMessage("La copia reemplazará los datos actuales. Puedes exportarlos antes desde Ajustes.").setNegativeButton("Cancelar",null).setPositiveButton("Elegir archivo",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,73);}).show();}
    @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null)return;Uri uri=intent.getData();if(request!=71&&request!=72&&request!=73)return;String imagePath=pendingImagePath;busy=true;ProgressDialog progress=ProgressDialog.show(this,"SIU Local",request==71?"Guardando imagen...":"Procesando copia...",true,false);java.util.concurrent.ExecutorService executor=Executors.newSingleThreadExecutor();executor.execute(()->{String error=null;try{if(request==71){String name=store.importImage(uri);setPath(imagePath,name);}else if(request==72)store.exportZip(uri);else store.importZip(uri);}catch(Exception e){error=e.getMessage()==null?"No se pudo completar la operación":e.getMessage();}String failure=error;runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed()){progress.dismiss();if(failure!=null)message("No se pudo completar",failure);else{if(request==73){history.clear();route="ajustes";selectedGestion="";}render();toast(request==71?"Imagen guardada":request==72?"Copia exportada":"Copia restaurada");}}});executor.shutdown();});}
}
