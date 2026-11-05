package nl.yinyang.launcher;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Two persistent widget pages around the existing launcher wheel. */
class WidgetWorkspace extends FrameLayout {
 static final int BIND=210, CONFIGURE=211, PHOTO=212, HOST_ID=1707;
 final MainActivity a;
 final AppWidgetManager manager;
 final AppWidgetHost host;
 final View[] screens=new View[3];
 final LinearLayout[] lists=new LinearLayout[3];
 int current=1;
 float downX,downY;
 boolean swiping, wheelGesture, vertical;
 WidgetWorkspace(MainActivity activity,View home){
  super(activity);a=activity;manager=AppWidgetManager.getInstance(a);host=new AppWidgetHost(a,HOST_ID);
  setBackgroundColor(0xff101a2b);setTag("widget-workspace");
  screens[1]=home;addView(home,new FrameLayout.LayoutParams(-1,-1));
  for(int side:new int[]{0,2}){screens[side]=makePage(side);addView(screens[side],new FrameLayout.LayoutParams(-1,-1));}
  showPage(1,false);
 }
 int dp(float n){return a.dp(n);}
 void start(){try{host.startListening();}catch(RuntimeException ignored){}}
 void stop(){try{host.stopListening();}catch(RuntimeException ignored){}}
 JSONArray entries(int side){try{return new JSONArray(a.prefs.getString("widgets_"+side,"[]"));}catch(JSONException e){return new JSONArray();}}
 void save(int side,JSONArray values){a.prefs.edit().putString("widgets_"+side,values.toString()).apply();render(side);}
 LinearLayout makePage(int side){
  LinearLayout page=new LinearLayout(a);page.setOrientation(1);page.setPadding(dp(12),dp(4),dp(12),dp(8));
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);
  TextView back=a.folderAction("☯");back.setContentDescription("Terug naar hoofdscherm");back.setOnClickListener(v->showPage(1,true));bar.addView(back,new LinearLayout.LayoutParams(dp(52),dp(52)));
  TextView title=a.folderText(side==0?"Widgets links":"Widgets rechts",20,Color.WHITE);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  TextView add=a.folderAction("＋");add.setTextSize(28);add.setContentDescription("Widget of foto toevoegen");add.setTag("widget-add-"+side);add.setOnClickListener(v->addMenu(side));bar.addView(add,new LinearLayout.LayoutParams(dp(52),dp(52)));page.addView(bar);
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);LinearLayout list=new LinearLayout(a);list.setOrientation(1);lists[side]=list;scroll.addView(list,new ScrollView.LayoutParams(-1,-2));page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  render(side);return page;
 }
 void addMenu(int side){new AlertDialog.Builder(a).setTitle("Toevoegen").setItems(new String[]{"Android-widget","Foto of rooster"},(d,n)->{if(n==0)chooseWidget(side);else pickPhoto(side);}).setNegativeButton("Sluiten",null).show();}
 void render(int side){
  LinearLayout list=lists[side];if(list==null)return;list.removeAllViews();JSONArray values=entries(side);
  if(values.length()==0){TextView empty=a.folderText("Ruimte voor je widgets\n\nTik op ＋ voor een widget of roosterfoto",18,0xffadbed5);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(72),dp(20),dp(72));empty.setOnClickListener(v->addMenu(side));list.addView(empty,new LinearLayout.LayoutParams(-1,-2));return;}
  for(int index=0;index<values.length();index++){
   final int item=index;JSONObject entry=values.optJSONObject(index);if(entry==null)continue;
   LinearLayout card=new LinearLayout(a);card.setOrientation(1);card.setPadding(dp(6),dp(4),dp(6),dp(8));card.setBackground(a.rounded(0xff17263d,18));
   LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);
   TextView title=a.folderText(entry.optString("label","Widget"),13,0xffc0d0e4);title.setPadding(dp(8),0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
   TextView edit=a.folderAction("⋮");edit.setTextSize(22);edit.setContentDescription("Widget aanpassen");edit.setTag("widget-edit-"+side+"-"+index);edit.setOnClickListener(v->editMenu(side,item));bar.addView(edit,new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(bar);
   int height=entry.optInt("height",240);
   if(entry.has("uri")){
    ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setAdjustViewBounds(false);image.setContentDescription("Foto of rooster");
    Bitmap bitmap=readPhoto(entry.optString("uri"));if(bitmap!=null)image.setImageBitmap(bitmap);else image.setImageResource(android.R.drawable.ic_menu_report_image);
    image.setOnClickListener(v->openPhoto(entry.optString("uri")));card.addView(image,new LinearLayout.LayoutParams(-1,dp(height)));
   }else{
    int id=entry.optInt("id",AppWidgetManager.INVALID_APPWIDGET_ID);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
    if(info==null){TextView missing=a.folderText("Widget niet beschikbaar. Voeg hem opnieuw toe via ＋.",14,Color.WHITE);missing.setPadding(dp(16),dp(20),dp(16),dp(20));card.addView(missing);}
    else try{
     AppWidgetHostView view=host.createView(a,id,info);view.setTag("host-widget-"+id);card.addView(view,new LinearLayout.LayoutParams(-1,dp(height)));
     view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot){float density=getResources().getDisplayMetrics().density;int w=Math.max(1,Math.round((r-l)/density)),h=Math.max(1,Math.round((b-t)/density));view.updateAppWidgetSize(null,w,h,w,h);}});
    }catch(RuntimeException e){TextView error=a.folderText("Deze widget kon niet worden geladen.",14,Color.WHITE);card.addView(error);}
   }
   LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);margin.setMargins(0,dp(8),0,dp(4));list.addView(card,margin);
  }
 }
 void editMenu(int side,int index){
  JSONObject entry=entries(side).optJSONObject(index);if(entry==null)return;
  new AlertDialog.Builder(a).setTitle(entry.optString("label","Widget")).setItems(new String[]{"Hoogte aanpassen","Omhoog","Omlaag","Naar andere widgetpagina","Opnieuw instellen","Verwijderen"},(d,n)->{
   if(n==0)resize(side,index);
   if(n==1)move(side,index,-1);if(n==2)move(side,index,1);
   if(n==3){JSONArray from=entries(side),to=entries(2-side);JSONObject value=from.optJSONObject(index);if(value!=null){from.remove(index);to.put(value);a.prefs.edit().putString("widgets_"+side,from.toString()).putString("widgets_"+(2-side),to.toString()).apply();render(side);render(2-side);showPage(2-side,true);}}
   if(n==4){if(entry.has("uri"))replacePhoto(side,index);else reconfigure(entry.optInt("id",-1));}
   if(n==5)new AlertDialog.Builder(a).setMessage("Deze widget verwijderen?").setPositiveButton("Verwijderen",(q,w)->remove(side,index)).setNegativeButton("Annuleren",null).show();
  }).setNegativeButton("Sluiten",null).show();
 }
 void resize(int side,int index){String[] names={"Klein","Middel","Groot","Extra groot"};int[] heights={120,240,400,600};new AlertDialog.Builder(a).setTitle("Hoogte").setItems(names,(d,n)->{JSONArray values=entries(side);try{values.getJSONObject(index).put("height",heights[n]);save(side,values);}catch(JSONException ignored){}}).show();}
 void move(int side,int index,int direction){JSONArray values=entries(side);int next=index+direction;if(next<0||next>=values.length())return;try{Object value=values.get(index);values.put(index,values.get(next));values.put(next,value);save(side,values);}catch(JSONException ignored){}}
 void remove(int side,int index){JSONArray values=entries(side);JSONObject entry=values.optJSONObject(index);if(entry==null)return;if(entry.has("id"))try{host.deleteAppWidgetId(entry.optInt("id"));}catch(RuntimeException ignored){}values.remove(index);save(side,values);}
 Bundle options(AppWidgetProviderInfo info){Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);int width=Math.max(100,a.getResources().getConfiguration().screenWidthDp-36);int height=Math.max(120,Math.min(600,Math.round(info.minHeight/getResources().getDisplayMetrics().density)));options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,width);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,height);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height);return options;}
 void chooseWidget(int side){
  ArrayList<AppWidgetProviderInfo> providers=new ArrayList<>(manager.getInstalledProviders());providers.sort((x,y)->String.CASE_INSENSITIVE_ORDER.compare(x.loadLabel(a.getPackageManager()),y.loadLabel(a.getPackageManager())));
  if(providers.isEmpty()){Toast.makeText(a,"Er zijn geen widgets beschikbaar",Toast.LENGTH_LONG).show();return;}
  String[] labels=new String[providers.size()];for(int i=0;i<labels.length;i++){AppWidgetProviderInfo info=providers.get(i);String app=info.provider.getPackageName();try{app=a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(app,0)).toString();}catch(Exception ignored){}labels[i]=app+" · "+info.loadLabel(a.getPackageManager());}
  new AlertDialog.Builder(a).setTitle("Widget kiezen").setItems(labels,(d,n)->beginWidget(side,providers.get(n))).setNegativeButton("Annuleren",null).show();
 }
 void beginWidget(int side,AppWidgetProviderInfo info){
  cancelPending();int id=host.allocateAppWidgetId();a.prefs.edit().putInt("pending_widget",id).putInt("pending_widget_side",side).putBoolean("pending_widget_existing",false).apply();
  try{if(manager.bindAppWidgetIdIfAllowed(id,info.provider,options(info)))configurePending();else{Intent intent=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider);intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options(info));a.startActivityForResult(intent,BIND);}}
  catch(RuntimeException e){cancelPending();Toast.makeText(a,"Deze widget kon niet worden toegevoegd",Toast.LENGTH_LONG).show();}
 }
 void configurePending(){int id=a.prefs.getInt("pending_widget",-1);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null){cancelPending();return;}if(info.configure==null){completePending();return;}try{host.startAppWidgetConfigureActivityForResult(a,id,0,CONFIGURE,null);}catch(RuntimeException e){cancelPending();Toast.makeText(a,"Widgetinstellingen konden niet worden geopend",Toast.LENGTH_LONG).show();}}
 void reconfigure(int id){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null||info.configure==null){Toast.makeText(a,"Deze widget heeft geen instellingen",Toast.LENGTH_SHORT).show();return;}cancelPending();a.prefs.edit().putInt("pending_widget",id).putInt("pending_widget_side",current).putBoolean("pending_widget_existing",true).apply();configurePending();}
 void clearPending(){a.prefs.edit().remove("pending_widget").remove("pending_widget_side").remove("pending_widget_existing").apply();}
 void cancelPending(){int id=a.prefs.getInt("pending_widget",-1);if(id!=-1&&!a.prefs.getBoolean("pending_widget_existing",false))try{host.deleteAppWidgetId(id);}catch(RuntimeException ignored){}clearPending();}
 void completePending(){
  int id=a.prefs.getInt("pending_widget",-1),side=a.prefs.getInt("pending_widget_side",0);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null){cancelPending();return;}
  if(!a.prefs.getBoolean("pending_widget_existing",false)){JSONArray values=entries(side);try{JSONObject entry=new JSONObject();entry.put("id",id);entry.put("label",info.loadLabel(a.getPackageManager()));entry.put("height",options(info).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));values.put(entry);save(side,values);}catch(JSONException ignored){cancelPending();return;}}
  clearPending();render(side);showPage(side,false);
 }
 void pickPhoto(int side){replacePhoto(side,-1);}
 void replacePhoto(int side,int index){a.prefs.edit().putInt("pending_photo_side",side).putInt("pending_photo_index",index).apply();Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);try{a.startActivityForResult(intent,PHOTO);}catch(RuntimeException e){Toast.makeText(a,"Geen fotokiezer beschikbaar",Toast.LENGTH_SHORT).show();}}
 Bitmap readPhoto(String uri){try{Uri u=Uri.parse(uri);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(java.io.InputStream in=a.getContentResolver().openInputStream(u)){BitmapFactory.decodeStream(in,null,options);}int sample=1;while(Math.max(options.outWidth,options.outHeight)/sample>2048)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;Bitmap bitmap;try(java.io.InputStream in=a.getContentResolver().openInputStream(u)){bitmap=BitmapFactory.decodeStream(in,null,options);}int orientation=1;try(java.io.InputStream in=a.getContentResolver().openInputStream(u)){orientation=new android.media.ExifInterface(in).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);}catch(Exception ignored){}return bitmap==null?null:a.applyExif(bitmap,orientation);}catch(Exception e){return null;}}
 void openPhoto(String uri){Bitmap bitmap=readPhoto(uri);if(bitmap==null){Toast.makeText(a,"Kies de foto opnieuw via ⋮",Toast.LENGTH_SHORT).show();return;}Dialog dialog=new Dialog(a,android.R.style.Theme_Material_NoActionBar);LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);panel.setBackgroundColor(Color.BLACK);TextView close=a.folderAction("Sluiten");close.setOnClickListener(v->dialog.dismiss());panel.addView(close,new LinearLayout.LayoutParams(-1,dp(56)));ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(bitmap);panel.addView(image,new LinearLayout.LayoutParams(-1,0,1));if(Build.VERSION.SDK_INT>=30)panel.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});dialog.setContentView(panel);dialog.show();dialog.getWindow().setLayout(-1,-1);}
 boolean result(int request,int result,Intent data){
  if(request==BIND||request==CONFIGURE){if(a.prefs.getInt("pending_widget",-1)==-1)return true;if(result!=Activity.RESULT_OK)cancelPending();else if(request==BIND)configurePending();else completePending();return true;}
  if(request!=PHOTO)return false;
  if(result==Activity.RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();try{a.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
   if(readPhoto(uri.toString())==null){Toast.makeText(a,"Deze foto kon niet worden geopend",Toast.LENGTH_LONG).show();return true;}
   int side=a.prefs.getInt("pending_photo_side",0),index=a.prefs.getInt("pending_photo_index",-1);JSONArray values=entries(side);try{JSONObject entry=index>=0?values.optJSONObject(index):null;if(entry==null){entry=new JSONObject();entry.put("height",400);values.put(entry);}entry.put("uri",uri.toString());entry.put("label","Foto / rooster");save(side,values);showPage(side,false);}catch(JSONException ignored){}
  }a.prefs.edit().remove("pending_photo_side").remove("pending_photo_index").apply();return true;
 }
 void showPage(int target,boolean animate){
  target=Math.max(0,Math.min(2,target));int previous=current;current=target;if(a.toolbar!=null)a.toolbar.setVisibility(View.GONE);
  for(int i=0;i<3;i++){View screen=screens[i];if(screen==null)continue;screen.animate().cancel();screen.setVisibility(i==current?View.VISIBLE:View.GONE);screen.setTranslationX(0);}
  if(animate&&previous!=current&&getWidth()>0){screens[current].setTranslationX((current>previous?1:-1)*getWidth()*.2f);screens[current].animate().translationX(0).setDuration(160).start();}
 }
 @Override public boolean onInterceptTouchEvent(MotionEvent event){
  int action=event.getActionMasked();float x=event.getX(),y=event.getY();
  if(action==MotionEvent.ACTION_DOWN){downX=x;downY=y;swiping=false;vertical=false;wheelGesture=current==1&&y>=a.home.paneBounds(2).top&&y<=a.home.paneBounds(2).bottom;return false;}
  if(action==MotionEvent.ACTION_MOVE&&!wheelGesture&&!vertical){float dx=x-downX,dy=y-downY;int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;return false;}if(Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.5f){swiping=true;return true;}}
  return swiping;
 }
 @Override public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_UP){float dx=event.getX()-downX;if(swiping&&Math.abs(dx)>dp(48))showPage(current+(dx<0?1:-1),true);swiping=false;return true;}if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){swiping=false;return true;}return true;}
}
