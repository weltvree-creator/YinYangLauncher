package nl.yinyang.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
 final String[] defaults={"Telefoon","Berichten","Camera","Internet","Foto's","Muziek","Werk","Tools","Overig"};
 android.content.SharedPreferences prefs; FrameLayout root; HomeView home; int page=0; Bitmap[] photos=new Bitmap[2]; boolean adjusting=false; float downX,downY;
 ArrayList<ResolveInfo> installed=new ArrayList<>();
 final HashMap<String,Drawable> appIcons=new HashMap<>(); long touchStarted;
 String appId(ResolveInfo r){return r.activityInfo.packageName+"/"+r.activityInfo.name;}
 ArrayList<ResolveInfo> slotApps(int p,int i){LinkedHashSet<String> ids=new LinkedHashSet<>(Arrays.asList(prefs.getString(key(p,i),"").split("\\|")));ArrayList<ResolveInfo> found=new ArrayList<>();for(String id:ids)for(ResolveInfo r:installed)if(appId(r).equals(id)){found.add(r);break;}return found;}
 @Override protected void onResume(){super.onResume();if(prefs!=null){loadApps();if(home!=null)home.invalidate();}}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 String key(int p,int i){return "slot_"+p+"_"+i;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);prefs=getSharedPreferences("launcher",0);loadApps();for(int i=0;i<2;i++)loadPhoto(i);showHome();}
 void loadApps(){Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);installed.clear();appIcons.clear();HashSet<String> seen=new HashSet<>();for(ResolveInfo r:getPackageManager().queryIntentActivities(q,0))if(!r.activityInfo.packageName.equals(getPackageName())&&seen.add(appId(r)))installed.add(r);Collections.sort(installed,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(getPackageManager()).toString(),b.loadLabel(getPackageManager()).toString()));}
 void loadPhoto(int p){try{String uri=prefs.getString("photo_"+p,"");if(!uri.isEmpty()){try(java.io.InputStream in=getContentResolver().openInputStream(Uri.parse(uri))){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;photos[p]=BitmapFactory.decodeStream(in,null,o);}}}catch(Exception ignored){photos[p]=null;}}
 void showHome(){root=new FrameLayout(this);
 if(Build.VERSION.SDK_INT>=30){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});}
 home=new HomeView(this);root.addView(home,new FrameLayout.LayoutParams(-1,-1));LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER);toolbar.setBackgroundColor(0x88000000);String[] names={"Foto","Pas foto aan","Apps","Startscherm"};for(int i=0;i<names.length;i++){final int which=i;TextView t=new TextView(this);t.setText(names[i]);t.setTextColor(Color.WHITE);t.setTextSize(12);t.setGravity(Gravity.CENTER);t.setPadding(dp(5),dp(13),dp(5),dp(13));toolbar.addView(t,new LinearLayout.LayoutParams(0,-2,1));t.setOnClickListener(v->{if(which==0)pickPhoto();if(which==1)adjustPhoto();if(which==2)showAllApps();if(which==3)chooseHome();});}FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(toolbar,lp);setContentView(root);}
 void pickPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,100+page);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result==RESULT_OK&&data!=null&&req>=100&&req<=101){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("photo_"+(req-100),u.toString()).apply();loadPhoto(req-100);home.invalidate();}}
 void adjustPhoto(){if(photos[page]==null){Toast.makeText(this,"Kies eerst een foto",Toast.LENGTH_SHORT).show();return;}LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(20),dp(10),dp(20),dp(10));TextView msg=new TextView(this);msg.setText("Zoom met de schuif. Verschuif de foto met je vinger op het startscherm nadat je op Gereed drukt niet: gebruik eerst Verplaatsen.");l.addView(msg);SeekBar zoom=new SeekBar(this);zoom.setMax(300);zoom.setProgress((int)((prefs.getFloat("zoom_"+page,1f)-1)*100));l.addView(zoom);zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean u){prefs.edit().putFloat("zoom_"+page,1+v/100f).apply();home.invalidate();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});new AlertDialog.Builder(this).setTitle("Foto aanpassen – scherm "+(page+1)).setView(l).setPositiveButton("Verplaatsen",(d,w)->{adjusting=true;Toast.makeText(this,"Sleep foto; tik op yin-yang als je klaar bent",Toast.LENGTH_LONG).show();}).setNegativeButton("Gereed",null).show();}
 void chooseHome(){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){new AlertDialog.Builder(this).setMessage("Ga naar Instellingen → Apps → Standaard-apps → Startscherm en kies YinYangLauncher.").setPositiveButton("OK",null).show();}}
 void showAllApps(){String[] labels=new String[installed.size()];for(int i=0;i<labels.length;i++)labels[i]=installed.get(i).loadLabel(getPackageManager()).toString();new AlertDialog.Builder(this).setTitle("Alle apps").setItems(labels,(d,i)->launch(installed.get(i))).setNegativeButton("Sluiten",null).show();}
 void launch(ResolveInfo info){try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(info.activityInfo.packageName,info.activityInfo.name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){Toast.makeText(this,"App kan niet openen",Toast.LENGTH_SHORT).show();}}
 String title(int p,int i){return prefs.getString("name_"+p+"_"+i,defaults[i]);}
 void slot(int i){ArrayList<ResolveInfo> apps=slotApps(page,i);if(apps.size()==1){launch(apps.get(0));return;}if(apps.isEmpty()){editSlot(i);return;}slotMenu(i);}
 GradientDrawable rounded(int color,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
 TextView folderText(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);return v;}
 TextView folderAction(String label){TextView v=folderText(label,13,0xffffbb77);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setPadding(dp(6),dp(10),dp(6),dp(10));v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x33ffffff),rounded(0x441f3555,14),null));return v;}
 void slotMenu(int i){
  ArrayList<ResolveInfo> matches=slotApps(page,i);
  AlertDialog dialog=new AlertDialog.Builder(this).create();
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(20),dp(18),dp(20),dp(16));
  GradientDrawable background=rounded(0xee142137,26);background.setStroke(dp(1),0x667ba4cc);panel.setBackground(background);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  TextView heading=folderText(title(page,i),22,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  TextView close=folderText("×",28,0xffc7d4e5);close.setGravity(Gravity.CENTER);close.setContentDescription("Map sluiten");header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));close.setOnClickListener(v->dialog.dismiss());panel.addView(header);
  TextView count=folderText(matches.size()+ (matches.size()==1?" app":" apps"),12,0xffafc0d5);count.setPadding(0,0,0,dp(16));panel.addView(count);
  LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setTag("folder-apps");
  int columns=getResources().getConfiguration().screenWidthDp<320?2:3;
  for(int start=0;start<matches.size();start+=columns){
   LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.TOP);
   for(int column=0;column<columns;column++){
    int index=start+column;
    if(index>=matches.size()){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));continue;}
    ResolveInfo app=matches.get(index);String label=app.loadLabel(getPackageManager()).toString();
    LinearLayout tile=new LinearLayout(this);tile.setTag(appId(app));tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(10),dp(3),dp(10));tile.setMinimumHeight(dp(108));tile.setContentDescription(label);tile.setFocusable(true);
    tile.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x44ffffff),rounded(0x00142137,16),rounded(Color.WHITE,16)));
    ImageView icon=new ImageView(this);icon.setImageDrawable(app.loadIcon(getPackageManager()));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
    TextView name=folderText(label,12,Color.WHITE);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(0,dp(8),0,0);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(name,new LinearLayout.LayoutParams(-1,-2));
    tile.setOnClickListener(v->{dialog.dismiss();launch(app);});row.addView(tile,new LinearLayout.LayoutParams(0,-2,1));
   }
   rows.addView(row);
  }
  if(matches.isEmpty()){TextView empty=folderText("Voeg je favoriete apps toe",14,0xffafc0d5);empty.setPadding(0,dp(12),0,dp(24));rows.addView(empty);}
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(rows);
  int available=Math.max(dp(96),getResources().getDisplayMetrics().heightPixels-dp(280));int gridHeight=Math.min(dp(340),available);
  panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
  scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);if(scroll.getHeight()>gridHeight){scroll.getLayoutParams().height=gridHeight;scroll.requestLayout();}}});
  View line=new View(this);line.setBackgroundColor(0x337ba4cc);LinearLayout.LayoutParams lineParams=new LinearLayout.LayoutParams(-1,dp(1));lineParams.setMargins(0,dp(12),0,dp(14));panel.addView(line,lineParams);
  LinearLayout actions=new LinearLayout(this);TextView manage=folderAction("Apps beheren"),renameAction=folderAction("Naam wijzigen");LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(0,-2,1);actionParams.setMargins(0,0,dp(8),0);actions.addView(manage,actionParams);actions.addView(renameAction,new LinearLayout.LayoutParams(0,-2,1));panel.addView(actions);
  manage.setOnClickListener(v->{dialog.dismiss();editSlot(i);});renameAction.setOnClickListener(v->{dialog.dismiss();rename(i);});
  dialog.setView(panel,0,0,0,0);dialog.setCanceledOnTouchOutside(true);dialog.show();
  Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams attributes=window.getAttributes();attributes.dimAmount=.25f;window.setAttributes(attributes);window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(420)),-2);}
 }
 void rename(int i){EditText e=new EditText(this);e.setSingleLine(true);e.setText(title(page,i));new AlertDialog.Builder(this).setTitle("Naam van map").setView(e).setPositiveButton("Opslaan",(d,w)->{prefs.edit().putString("name_"+page+"_"+i,e.getText().toString()).apply();home.invalidate();}).setNegativeButton("Annuleren",null).show();}
 void editSlot(int i){int p=page;String current=prefs.getString(key(p,i),"");HashSet<String> set=new HashSet<>(Arrays.asList(current.split("\\|")));String[] names=new String[installed.size()];boolean[] checked=new boolean[installed.size()];for(int j=0;j<installed.size();j++){ResolveInfo r=installed.get(j);names[j]=r.loadLabel(getPackageManager()).toString();checked[j]=set.contains(r.activityInfo.packageName+"/"+r.activityInfo.name);}new AlertDialog.Builder(this).setTitle("Apps in "+title(p,i)).setMultiChoiceItems(names,checked,(d,j,v)->checked[j]=v).setPositiveButton("Opslaan",(d,w)->{StringBuilder b=new StringBuilder();for(int j=0;j<checked.length;j++)if(checked[j]){ResolveInfo r=installed.get(j);if(b.length()>0)b.append('|');b.append(r.activityInfo.packageName).append('/').append(r.activityInfo.name);}prefs.edit().putString(key(p,i),b.toString()).apply();home.invalidate();}).setNegativeButton("Annuleren",null).show();}
 class HomeView extends View{Paint paint=new Paint(3);HomeView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}void color(int c){paint.setColor(c);paint.setStyle(Paint.Style.FILL);paint.clearShadowLayer();}void text(Canvas c,String s,float x,float y,int size){color(Color.WHITE);paint.setTextSize(dp(size));paint.setTextAlign(Paint.Align.CENTER);paint.setShadowLayer(3,1,2,Color.BLACK);c.drawText(s,x,y,paint);paint.clearShadowLayer();}
 void drawIcon(Canvas c,ResolveInfo app,float x,float y,int size){try{String id=appId(app);Drawable icon=appIcons.get(id);if(icon==null){icon=app.loadIcon(getPackageManager());appIcons.put(id,icon);}int left=Math.round(x-size/2f),top=Math.round(y-size/2f);icon.setBounds(left,top,left+size,top+size);icon.draw(c);}catch(RuntimeException ignored){}}
 void drawSlotIcons(Canvas c,int i,float x,float y){ArrayList<ResolveInfo> apps=slotApps(page,i);if(apps.size()==1){drawIcon(c,apps.get(0),x,y,dp(38));}else if(apps.size()>1){for(int j=0;j<Math.min(4,apps.size());j++)drawIcon(c,apps.get(j),x+(j%2==0?-1:1)*dp(10),y+(j<2?-1:1)*dp(10),dp(18));}}
 @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();c.drawColor(0xff10141d);Bitmap b=photos[page];if(b!=null){float scale=Math.max(w/(float)b.getWidth(),h/(float)b.getHeight())*prefs.getFloat("zoom_"+page,1f);float bw=b.getWidth()*scale,bh=b.getHeight()*scale;float x=(w-bw)/2+prefs.getFloat("x_"+page,0),y=(h-bh)/2+prefs.getFloat("y_"+page,0);color(Color.WHITE);c.drawBitmap(b,null,new RectF(x,y,x+bw,y+bh),paint);}else{color(0xff18243a);c.drawRect(0,0,w,h,paint);}color(0x66000000);c.drawRect(0,0,w,h,paint);
 float cx=w/2f,cy=h*.45f,r=Math.min(w,h)*.19f;paint.setShadowLayer(dp(18),0,0,0xff007cff);color(0x99007aff);paint.setShadowLayer(dp(12),0,0,0xff007aff);c.drawCircle(cx,cy,r+dp(5),paint);color(0x99ff861f);paint.setShadowLayer(dp(12),0,0,0xffff861f);c.drawArc(cx-r,cy-r,cx+r,cy+r,90,180,true,paint);color(0xbb0e2d66);c.drawCircle(cx,cy,r,paint);color(0xaaff9c40);c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,180,true,paint);c.drawCircle(cx,cy-r/2,r/2,paint);color(0xaa0e2d66);c.drawCircle(cx,cy+r/2,r/2,paint);c.drawCircle(cx,cy-r/2,r/8,paint);color(0xaaff9c40);c.drawCircle(cx,cy+r/2,r/8,paint);
 for(int i=0;i<9;i++){double a=-Math.PI/2+2*Math.PI*i/9;float rr=Math.min(w*.39f,h*.35f),sx=cx+(float)Math.cos(a)*rr,sy=cy+(float)Math.sin(a)*rr;int rad=dp(25);color(0xbb15253e);paint.setShadowLayer(dp(5),0,0,0xff007aff);c.drawCircle(sx,sy,rad,paint);color(0xaaff9234);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawCircle(sx,sy,rad,paint);paint.setStyle(Paint.Style.FILL);drawSlotIcons(c,i,sx,sy);text(c,title(page,i),sx,sy+rad+dp(16),11);}text(c,"Scherm "+(page+1)+" / 2",cx,dp(45),16);text(c,"Houd een cirkel ingedrukt om apps te wijzigen",cx,h-dp(75),12);}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX(),y=e.getY(),cx=getWidth()/2f,cy=getHeight()*.45f;if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;touchStarted=SystemClock.uptimeMillis();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dx=x-downX,dy=y-downY;if(adjusting){if(Math.hypot(x-cx,y-cy)<dp(80)){adjusting=false;Toast.makeText(MainActivity.this,"Positie opgeslagen",Toast.LENGTH_SHORT).show();}else{prefs.edit().putFloat("x_"+page,prefs.getFloat("x_"+page,0)+dx).putFloat("y_"+page,prefs.getFloat("y_"+page,0)+dy).apply();invalidate();}return true;}if(Math.abs(dx)>dp(60)&&Math.abs(dx)>Math.abs(dy)){page=1-page;invalidate();return true;}if(Math.hypot(x-cx,y-cy)<Math.min(getWidth(),getHeight())*.2){page=1-page;invalidate();return true;}for(int i=0;i<9;i++){double a=-Math.PI/2+2*Math.PI*i/9;float rr=Math.min(getWidth()*.39f,getHeight()*.35f),sx=cx+(float)Math.cos(a)*rr,sy=cy+(float)Math.sin(a)*rr;if(Math.hypot(x-sx,y-sy)<dp(48)){if(SystemClock.uptimeMillis()-touchStarted>=ViewConfiguration.getLongPressTimeout())slotMenu(i);else slot(i);return true;}}return true;}return true;}}
}

