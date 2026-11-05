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
 android.content.SharedPreferences prefs; FrameLayout root; HomeView home; int page=0; Bitmap[] photos=new Bitmap[4]; boolean adjusting=false; int photoPane=0; LinearLayout toolbar; float downX,downY;
 WidgetWorkspace workspace; CenterEmblem emblem;
 ArrayList<ResolveInfo> installed=new ArrayList<>();
 final HashMap<String,Drawable> appIcons=new HashMap<>(); long touchStarted;
 String appId(ResolveInfo r){return r.activityInfo.packageName+"/"+r.activityInfo.name;}
 ArrayList<ResolveInfo> slotApps(int p,int i){LinkedHashSet<String> ids=new LinkedHashSet<>(Arrays.asList(prefs.getString(key(p,i),"").split("\\|")));ArrayList<ResolveInfo> found=new ArrayList<>();for(String id:ids)for(ResolveInfo r:installed)if(appId(r).equals(id)){found.add(r);break;}return found;}
 @Override protected void onResume(){super.onResume();if(prefs!=null){loadApps();if(home!=null)home.invalidate();}}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 String key(int p,int i){return "slot_"+p+"_"+i;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);prefs=getSharedPreferences("launcher",0);migratePhotos();loadApps();for(int i=0;i<4;i++)loadPhoto(i);emblem=new CenterEmblem(this);showHome();if(b!=null)workspace.showPage(b.getInt("workspace_page",1),false);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(workspace!=null)out.putInt("workspace_page",workspace.current);}
 @Override protected void onStart(){super.onStart();if(workspace!=null)workspace.start();}
 @Override protected void onStop(){if(workspace!=null)workspace.stop();super.onStop();}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);if(workspace!=null)workspace.showPage(1,false);}
 void loadApps(){Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);installed.clear();appIcons.clear();HashSet<String> seen=new HashSet<>();for(ResolveInfo r:getPackageManager().queryIntentActivities(q,0))if(!r.activityInfo.packageName.equals(getPackageName())&&seen.add(appId(r)))installed.add(r);Collections.sort(installed,(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(getPackageManager()).toString(),b.loadLabel(getPackageManager()).toString()));}
 void migratePhotos(){if(!prefs.getBoolean("photo_layout_15",false)){android.content.SharedPreferences.Editor e=prefs.edit();for(int p=0;p<3;p++)e.putBoolean("fit_"+p,true).putFloat("zoom_"+p,1).putFloat("x_"+p,0).putFloat("y_"+p,0);e.putBoolean("photo_layout_15",true).apply();}}
 String paneName(int pane){return pane==3?"Foto / avatar in de appcirkel":pane==0?"Foto boven":pane==1?"Foto onder":"Achtergrond midden";}
 Bitmap applyExif(Bitmap bitmap,int orientation){Matrix matrix=new Matrix();switch(orientation){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(-90);matrix.postScale(-1,1);break;case 8:matrix.setRotate(-90);break;default:return bitmap;}Bitmap result=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(result!=bitmap)bitmap.recycle();return result;}
 void loadPhoto(int p){photos[p]=null;String value=prefs.getString("photo_"+p,"");if(value.isEmpty())return;try{Uri uri=Uri.parse(value);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>2048)opts.inSampleSize*=2;
 Bitmap bitmap;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,opts);}if(bitmap==null)throw new java.io.IOException("Geen afbeelding");int orientation=1;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){orientation=new android.media.ExifInterface(in).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);}catch(Exception ignored){}photos[p]=applyExif(bitmap,orientation);
 }catch(Exception e){Toast.makeText(this,"Deze foto kon niet worden geopend. Kies hem opnieuw.",Toast.LENGTH_LONG).show();}}
 static class PhotoState {boolean fit=true;float zoom=1,x=0,y=0;int turns=0;}
 PhotoState photoState(int p){PhotoState state=new PhotoState();state.fit=prefs.getBoolean("fit_"+p,true);state.zoom=prefs.getFloat("zoom_"+p,1);state.x=prefs.getFloat("x_"+p,0);state.y=prefs.getFloat("y_"+p,0);state.turns=prefs.getInt("turns_"+p,0);return state;}
 void savePhotoState(int p,PhotoState state){prefs.edit().putBoolean("fit_"+p,state.fit).putFloat("zoom_"+p,state.zoom).putFloat("x_"+p,state.x).putFloat("y_"+p,state.y).putInt("turns_"+p,state.turns).apply();home.invalidate();}
 RectF photoBounds(Bitmap bitmap,PhotoState state,RectF box){boolean quarter=(state.turns%2)!=0;float width=quarter?bitmap.getHeight():bitmap.getWidth(),height=quarter?bitmap.getWidth():bitmap.getHeight();float scale=(state.fit?Math.min(box.width()/width,box.height()/height):Math.max(box.width()/width,box.height()/height))*state.zoom;float bw=width*scale,bh=height*scale;float maxX=Math.abs(box.width()-bw)/2,maxY=Math.abs(box.height()-bh)/2;float x=box.centerX()+Math.max(-maxX,Math.min(maxX,state.x)),y=box.centerY()+Math.max(-maxY,Math.min(maxY,state.y));return new RectF(x-bw/2,y-bh/2,x+bw/2,y+bh/2);}
 void paintPhoto(Canvas c,int pane,RectF box,PhotoState state){Paint brush=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);brush.setColor(prefs.getInt("color_"+pane,pane==2?0xff101a2b:0xff18243a));c.save();c.clipRect(box);if(pane==3){Path mask=new Path();mask.addCircle(box.centerX(),box.centerY(),Math.min(box.width(),box.height())/2,Path.Direction.CW);c.clipPath(mask);}c.drawRect(box,brush);Bitmap b=photos[pane];if(b!=null){RectF dst=photoBounds(b,state,box);c.translate(dst.centerX(),dst.centerY());c.rotate(state.turns*90);float bw=(state.turns%2==0)?dst.width():dst.height(),bh=(state.turns%2==0)?dst.height():dst.width();brush.setColor(Color.WHITE);c.drawBitmap(b,null,new RectF(-bw/2,-bh/2,bw/2,bh/2),brush);}c.restore();}
 void showHome(){root=new FrameLayout(this);
 if(Build.VERSION.SDK_INT>=30){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});}
 home=new HomeView(this);workspace=new WidgetWorkspace(this,home);root.addView(workspace,new FrameLayout.LayoutParams(-1,-1));
 toolbar=new LinearLayout(this);toolbar.setTag("home-toolbar");toolbar.setGravity(Gravity.CENTER);toolbar.setPadding(dp(8),dp(8),dp(8),dp(8));toolbar.setBackground(rounded(0xf0142137,22));
 String[] names={"Boven","Midden","Onder","Apps","Instellen"};for(int i=0;i<names.length;i++){final int which=i;TextView t=folderAction(names[i]);toolbar.addView(t,new LinearLayout.LayoutParams(0,-2,1));t.setOnClickListener(v->{toolbar.setVisibility(View.GONE);if(which==1)emblem.menu();else if(which<3)photoMenu(which==0?0:1);if(which==3)showAllApps();if(which==4)wheelSettings();});}
 FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);lp.setMargins(dp(12),0,dp(12),dp(12));root.addView(toolbar,lp);toolbar.setVisibility(View.GONE);setContentView(root);}
 void toggleToolbar(){toolbar.setVisibility(toolbar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}
 @Override public void onBackPressed(){if(workspace!=null&&workspace.current!=1){workspace.showPage(1,true);}else if(adjusting){adjusting=false;home.invalidate();}else if(toolbar.getVisibility()==View.VISIBLE)toolbar.setVisibility(View.GONE);}
 void photoMenu(int pane){photoPane=pane;new AlertDialog.Builder(this).setTitle(paneName(pane)).setItems(new String[]{"Foto kiezen","Foto aanpassen","Achtergrondkleur","Foto verwijderen"},(d,n)->{photoPane=pane;if(n==0)pickPhoto();if(n==1)adjustPhoto();if(n==2)chooseColor(pane);if(n==3){prefs.edit().remove("photo_"+pane).remove("zoom_"+pane).remove("x_"+pane).remove("y_"+pane).remove("turns_"+pane).apply();photos[pane]=null;home.invalidate();}}).setNegativeButton("Sluiten",null).show();}
 void chooseColor(int pane){String[] names={"Donkerblauw","Zwart","Antraciet","Lichtgrijs","Zand","Groen","Bruin","Wit"};int[] colors={0xff101a2b,0xff000000,0xff30343b,0xffc7ccd1,0xffded1b8,0xff354e42,0xff665044,0xffffffff};new AlertDialog.Builder(this).setTitle("Achtergrondkleur · "+paneName(pane)).setItems(names,(d,n)->{prefs.edit().putInt("color_"+pane,colors[n]).apply();home.invalidate();}).setNegativeButton("Annuleren",null).show();}
 void pickPhoto(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,100+photoPane);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(workspace!=null&&workspace.result(req,result,data))return;if(result==RESULT_OK&&data!=null&&data.getData()!=null&&req>=100&&req<=103){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}int pane=req-100;prefs.edit().putString("photo_"+pane,u.toString()).apply();savePhotoState(pane,new PhotoState());loadPhoto(pane);home.invalidate();photoPane=pane;if(photos[pane]!=null){if(pane==3)emblem.select("photo");adjustPhoto();}}}
 void adjustPhoto(){final int pane=photoPane;if(photos[pane]==null){pickPhoto();return;}final PhotoState state=photoState(pane);
 Dialog dialog=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(16),dp(16),dp(16),dp(12));panel.setBackgroundColor(0xff101a2b);
 TextView heading=folderText(paneName(pane)+" aanpassen",22,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);panel.addView(heading);TextView hint=folderText("Sleep om te verplaatsen. Knijp of gebruik de schuif om te zoomen.",13,0xffb8c8dd);hint.setPadding(0,dp(8),0,dp(12));panel.addView(hint);
 PhotoPreview preview=new PhotoPreview(pane,state);preview.setTag("photo-preview");panel.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout modes=new LinearLayout(this);TextView fit=folderAction("Hele foto"),fill=folderAction("Vlak vullen"),rotate=folderAction("Draai ↻");fit.setTag("photo-fit");fill.setTag("photo-fill");rotate.setTag("photo-rotate");modes.addView(fit,new LinearLayout.LayoutParams(0,-2,1));modes.addView(fill,new LinearLayout.LayoutParams(0,-2,1));modes.addView(rotate,new LinearLayout.LayoutParams(0,-2,1));panel.addView(modes);
 TextView zoomLabel=folderText("Zoom",13,Color.WHITE);zoomLabel.setPadding(0,dp(8),0,0);panel.addView(zoomLabel);SeekBar zoom=new SeekBar(this);zoom.setTag("photo-zoom");zoom.setMax(375);zoom.setProgress(Math.round(state.zoom*100)-25);panel.addView(zoom);
 Runnable refresh=()->{zoomLabel.setText("Zoom "+Math.round(state.zoom*100)+"%");zoom.setProgress(Math.round(state.zoom*100)-25);fit.setTextColor(state.fit?0xffffbb77:0xff9aadc6);fill.setTextColor(state.fit?0xff9aadc6:0xffffbb77);preview.invalidate();};
 zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar seek,int value,boolean user){if(user){state.zoom=(25+value)/100f;refresh.run();}}public void onStartTrackingTouch(SeekBar seek){}public void onStopTrackingTouch(SeekBar seek){}});preview.onChanged=refresh;
 fit.setOnClickListener(v->{state.fit=true;state.zoom=1;state.x=state.y=0;refresh.run();});fill.setOnClickListener(v->{state.fit=false;state.zoom=1;state.x=state.y=0;refresh.run();});rotate.setOnClickListener(v->{state.turns=(state.turns+1)%4;state.x=state.y=0;refresh.run();});refresh.run();
 LinearLayout actions=new LinearLayout(this);TextView cancel=folderAction("Annuleren"),save=folderAction("Opslaan");save.setTag("photo-save");cancel.setTag("photo-cancel");actions.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));panel.addView(actions);cancel.setOnClickListener(v->dialog.dismiss());save.setOnClickListener(v->{savePhotoState(pane,state);if(pane==3)emblem.select("photo");dialog.dismiss();});
 if(Build.VERSION.SDK_INT>=30){panel.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(16)+bars.left,dp(16)+bars.top,dp(16)+bars.right,dp(12)+bars.bottom);return insets;});}dialog.setContentView(panel);Window window=dialog.getWindow();if(window!=null){window.setStatusBarColor(Color.BLACK);window.setNavigationBarColor(Color.BLACK);}dialog.show();if(window!=null)window.setLayout(-1,-1);
 }
 class PhotoPreview extends View {
 final int pane;final PhotoState state;final ScaleGestureDetector scale;Runnable onChanged=()->{};float lastX,lastY;boolean pinching=false;
 PhotoPreview(int p,PhotoState s){super(MainActivity.this);pane=p;state=s;setLayerType(View.LAYER_TYPE_SOFTWARE,null);scale=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){state.zoom=Math.max(.25f,Math.min(4,state.zoom*d.getScaleFactor()));onChanged.run();return true;}});}
 RectF previewBox(){RectF actual=home.paneBounds(pane);float ratio=Math.min(Math.max(1,getWidth()-dp(8))/actual.width(),Math.max(1,getHeight()-dp(12))/actual.height());float w=actual.width()*ratio,h=actual.height()*ratio;return new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);}
 @Override protected void onDraw(Canvas c){c.drawColor(0xff080d16);RectF box=previewBox();float ratio=box.width()/home.paneBounds(pane).width();PhotoState display=new PhotoState();display.fit=state.fit;display.zoom=state.zoom;display.turns=state.turns;display.x=state.x*ratio;display.y=state.y*ratio;paintPhoto(c,pane,box,display);Paint line=new Paint(3);line.setColor(0xff8ca5c5);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(dp(1));c.drawRect(box,line);}
 @Override public boolean onTouchEvent(MotionEvent e){scale.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();pinching=false;return true;}if(e.getPointerCount()>1){pinching=true;return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(!pinching&&!scale.isInProgress()){float ratio=home.paneBounds(pane).width()/previewBox().width();state.x+=(e.getX()-lastX)*ratio;state.y+=(e.getY()-lastY)*ratio;RectF frame=home.paneBounds(pane),bounds=photoBounds(photos[pane],state,frame);state.x=bounds.centerX()-frame.centerX();state.y=bounds.centerY()-frame.centerY();onChanged.run();}lastX=e.getX();lastY=e.getY();return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){performClick();return true;}return true;}
 @Override public boolean performClick(){super.performClick();return true;}
 }
 void chooseHome(){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){new AlertDialog.Builder(this).setMessage("Ga naar Instellingen → Apps → Standaard-apps → Startscherm en kies YinYangLauncher.").setPositiveButton("OK",null).show();}}
 void showAllApps(){String[] labels=new String[installed.size()];for(int i=0;i<labels.length;i++)labels[i]=installed.get(i).loadLabel(getPackageManager()).toString();new AlertDialog.Builder(this).setTitle("Alle apps").setItems(labels,(d,i)->launch(installed.get(i))).setNegativeButton("Sluiten",null).show();}
 void launch(ResolveInfo info){try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(info.activityInfo.packageName,info.activityInfo.name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){Toast.makeText(this,"App kan niet openen",Toast.LENGTH_SHORT).show();}}
 int wheelSlots(){return Math.max(27,prefs.getInt("wheel_slots",27));}
 int wrapSlot(int id){int size=wheelSlots();return ((id%size)+size)%size;}
 void openWheelSlot(int id,boolean manage){int slot=wrapSlot(id);page=slot/9;if(manage)slotMenu(slot%9);else slot(slot%9);}
 void wheelSettings(){new AlertDialog.Builder(this).setTitle("Instellen").setItems(new String[]{"9 approndjes toevoegen","Standaardstartscherm instellen"},(d,n)->{if(n==0){int previous=wheelSlots();prefs.edit().putInt("wheel_slots",previous+9).putFloat("wheel_position",previous).apply();home.wheelPosition=previous;home.invalidate();Toast.makeText(this,"9 nieuwe approndjes toegevoegd",Toast.LENGTH_SHORT).show();}else chooseHome();}).setNegativeButton("Sluiten",null).show();}
 String title(int p,int i){return prefs.getString("name_"+p+"_"+i,p==0?defaults[i]:"App "+(p*9+i+1));}
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
 final float wheelStep=(float)(2*Math.PI/9);
 float wheelPosition=prefs.getFloat("wheel_position",0),gestureStartPosition,lastAngle,lastTouchX,lastTouchY; boolean dragging=false; android.animation.ValueAnimator settling;
 float centerX(){return getWidth()/2f;} float centerY(){return getHeight()/2f;}
 float ringRadius(){return Math.min(getWidth()*.38f,getHeight()*.245f);}
 float centerRadius(){return ringRadius()*.46f;}
 float angle(float x,float y){return (float)Math.atan2(y-centerY(),x-centerX());}
 float angleDelta(float a,float b){float d=a-b;while(d>Math.PI)d-=2*Math.PI;while(d<-Math.PI)d+=2*Math.PI;return d;}
 RectF paneBounds(int pane){if(pane==3){float r=centerRadius();return new RectF(centerX()-r,centerY()-r,centerX()+r,centerY()+r);}float top=Math.max(dp(60),centerY()-ringRadius()-dp(56)),bottom=Math.min(getHeight()-dp(60),centerY()+ringRadius()+dp(60));return pane==0?new RectF(0,0,getWidth(),top):pane==1?new RectF(0,bottom,getWidth(),getHeight()):new RectF(0,top,getWidth(),bottom);}
 void photo(Canvas c,int pane,RectF box){paintPhoto(c,pane,box,photoState(pane));if(photos[pane]==null&&pane!=2)text(c,pane==0?"Jouw foto boven":"Jouw foto onder",box.centerX(),box.centerY(),14);}
 int visibleSlot(int place){return wrapSlot((int)Math.floor(wheelPosition)+place);}
 float fraction(){return wheelPosition-(float)Math.floor(wheelPosition);}
 void drawWheelSlot(Canvas c,int slot,float angle,float opacity){
  if(opacity<=0)return;float sx=centerX()+(float)Math.cos(angle)*ringRadius(),sy=centerY()+(float)Math.sin(angle)*ringRadius();float rad=Math.min(dp(25),getWidth()*.067f);
  int layer=c.saveLayerAlpha(0,0,getWidth(),getHeight(),Math.round(opacity*255));
  color(0xee15253e);paint.setShadowLayer(dp(5),0,0,0xff007aff);c.drawCircle(sx,sy,rad,paint);color(0xaaff9234);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));c.drawCircle(sx,sy,rad,paint);paint.setStyle(Paint.Style.FILL);
  int previous=page;page=slot/9;drawSlotIcons(c,slot%9,sx,sy);String label=title(page,slot%9);page=previous;
  if(label.length()>13)label=label.substring(0,12)+"…";text(c,label,sx,sy+rad+dp(15),10);c.restoreToCount(layer);
 }
 void drawWheel(Canvas c){float part=fraction();for(int i=0;i<=9;i++){float alpha=i==0?1-part:i==9?part:1;drawWheelSlot(c,visibleSlot(i),-(float)Math.PI/2+wheelStep*(i-part),alpha);}}
 int wheelSlotAt(float x,float y){float part=fraction();int closest=-1;double distance=dp(34);for(int i=0;i<=9;i++){if((i==0&&part>=.5f)||(i==9&&part<.5f))continue;float angle=-(float)Math.PI/2+wheelStep*(i-part);float sx=centerX()+(float)Math.cos(angle)*ringRadius(),sy=centerY()+(float)Math.sin(angle)*ringRadius();double d=Math.hypot(x-sx,y-sy);if(d<distance){closest=visibleSlot(i);distance=d;}}return closest;}
 @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();c.drawColor(0xff101a2b);float cx=centerX(),cy=centerY(),rr=ringRadius(),r=centerRadius();
 float top=Math.max(dp(60),cy-rr-dp(56)),bottom=Math.min(h-dp(60),cy+rr+dp(60));
 photo(c,2,paneBounds(2));if(photos[2]!=null){color(0x44000000);c.drawRect(paneBounds(2),paint);}photo(c,0,paneBounds(0));photo(c,1,paneBounds(1));
 color(0xff305074);c.drawRect(0,top,w,top+dp(1),paint);c.drawRect(0,bottom,w,bottom+dp(1),paint);
 color(0x99007aff);paint.setShadowLayer(dp(12),0,0,0xff007aff);c.drawCircle(cx,cy,r+dp(3),paint);paint.clearShadowLayer();emblem.draw(c,cx,cy,r,emblem.selected());
 drawWheel(c);

 }
 void settle(){final float target=Math.round(wheelPosition);
 settling=android.animation.ValueAnimator.ofFloat(wheelPosition,target);settling.setDuration(180);settling.addUpdateListener(a->{wheelPosition=(Float)a.getAnimatedValue();invalidate();});
 settling.addListener(new android.animation.AnimatorListenerAdapter(){boolean canceled=false;@Override public void onAnimationCancel(android.animation.Animator a){canceled=true;}@Override public void onAnimationEnd(android.animation.Animator a){if(!canceled){wheelPosition=((target%wheelSlots())+wheelSlots())%wheelSlots();prefs.edit().putFloat("wheel_position",wheelPosition).apply();invalidate();}}});settling.start();}
 @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();int action=e.getActionMasked();
 if(action==MotionEvent.ACTION_DOWN){if(settling!=null&&settling.isRunning())settling.end();downX=x;downY=y;lastTouchX=x;lastTouchY=y;lastAngle=angle(x,y);gestureStartPosition=wheelPosition;dragging=false;touchStarted=SystemClock.uptimeMillis();return true;}
 if(action==MotionEvent.ACTION_MOVE){float dx=x-downX,dy=y-downY;if(adjusting)return true;
  if(Math.hypot(dx,dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dragging=true;
  if(dragging){float current=angle(x,y);if(Math.hypot(downX-centerX(),downY-centerY())>centerRadius()&&Math.hypot(x-centerX(),y-centerY())>centerRadius()*.6f)wheelPosition-=angleDelta(current,lastAngle)/wheelStep;else wheelPosition-=((x-lastTouchX)+(y-lastTouchY))/(Math.max(1,ringRadius())*wheelStep);
   toolbar.setVisibility(View.GONE);invalidate();}lastAngle=angle(x,y);lastTouchX=x;lastTouchY=y;return true;}
 if(action==MotionEvent.ACTION_CANCEL){wheelPosition=gestureStartPosition;dragging=false;invalidate();return true;}
 if(action==MotionEvent.ACTION_UP){float dx=x-downX,dy=y-downY;
  if(adjusting){if(Math.hypot(x-centerX(),y-centerY())<centerRadius()+dp(8)){adjusting=false;Toast.makeText(MainActivity.this,"Positie opgeslagen",Toast.LENGTH_SHORT).show();}else{prefs.edit().putFloat("x_"+photoPane,prefs.getFloat("x_"+photoPane,0)+dx).putFloat("y_"+photoPane,prefs.getFloat("y_"+photoPane,0)+dy).apply();}invalidate();return true;}
  if(dragging||Math.hypot(dx,dy)>dp(20)){if(Math.abs(wheelPosition-gestureStartPosition)<.1f&&Math.hypot(dx,dy)>dp(45))wheelPosition=gestureStartPosition+(dx+dy>=0?-1:1);settle();return true;}
  if(Math.hypot(x-centerX(),y-centerY())<centerRadius()+dp(8)){toggleToolbar();performClick();return true;}
  toolbar.setVisibility(View.GONE);int hit=wheelSlotAt(x,y);if(hit>=0){openWheelSlot(hit,SystemClock.uptimeMillis()-touchStarted>=ViewConfiguration.getLongPressTimeout());performClick();}return true;
 }return true;}
 @Override public boolean performClick(){super.performClick();return true;}
 }
}

