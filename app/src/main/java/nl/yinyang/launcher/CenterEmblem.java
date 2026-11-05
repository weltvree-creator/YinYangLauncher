package nl.yinyang.launcher;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Selectable center artwork, independent of wheel slots and middle background. */
class CenterEmblem {
 final MainActivity a;
 Bitmap tattoo;
 static final String[] ZODIAC_NAMES={"Ram","Stier","Tweelingen","Kreeft","Leeuw","Maagd","Weegschaal","Schorpioen","Boogschutter","Steenbok","Waterman","Vissen"};
 static final String[] ZODIAC_GLYPHS={"♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓"};
 static final String[] BASE_IDS={"yin","tattoo","heart","flower","mandala","sun","moon","star"};
 static final String[] BASE_NAMES={"Yin-Yang","Drie J’s · tatoeage","Hart","Bloem","Mandala","Zon","Maan","Ster"};
 CenterEmblem(MainActivity activity){a=activity;}
 String selected(){return a.prefs.getString("center_template","yin");}
 int ink(){return a.prefs.getInt("center_ink",0xffffb26b);}
 int background(){return a.prefs.getInt("color_3",0xff102743);}
 void select(String id){a.prefs.edit().putString("center_template",id).apply();a.home.invalidate();}
 void menu(){new AlertDialog.Builder(a).setTitle("Midden van de appcirkel").setItems(new String[]{"Sjabloon kiezen","Eigen foto / avatar kiezen","Foto / avatar aanpassen","Kleur van symbool","Achtergrond van symbool","Achtergrond van middenvlak"},(d,n)->{
  if(n==0)picker();if(n==1){a.photoPane=3;a.pickPhoto();}if(n==2){a.photoPane=3;a.adjustPhoto();}if(n==3)colors();if(n==4)a.chooseColor(3);if(n==5)a.photoMenu(2);
 }).setNegativeButton("Sluiten",null).show();}
 void colors(){String[] labels={"Warm oranje","Goud","Wit","Lichtblauw","Roze","Groen","Paars","Zwart"};int[] colors={0xffffb26b,0xffffd479,0xfff5f7fb,0xff83d6ff,0xffffa6c9,0xff9ce3be,0xffc2a5ff,0xff111111};new AlertDialog.Builder(a).setTitle("Kleur van symbool").setItems(labels,(d,n)->{a.prefs.edit().putInt("center_ink",colors[n]).apply();a.home.invalidate();}).setNegativeButton("Annuleren",null).show();}
 void picker(){
  Dialog dialog=new Dialog(a,android.R.style.Theme_Material_NoActionBar);LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);panel.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));panel.setBackgroundColor(0xff101a2b);
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);TextView heading=a.folderText("Kies je middensymbool",21,Color.WHITE);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));TextView close=a.folderAction("Sluiten");close.setOnClickListener(v->dialog.dismiss());bar.addView(close);panel.addView(bar);
  ScrollView scroll=new ScrollView(a);LinearLayout items=new LinearLayout(a);items.setOrientation(1);scroll.addView(items);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  section(items,"Symbolen",BASE_IDS,BASE_NAMES,dialog);
  String[] ids=new String[12];for(int i=0;i<12;i++)ids[i]="zodiac_"+i;section(items,"Sterrenbeelden",ids,ZODIAC_NAMES,dialog);
  TextView own=a.folderAction("＋ Eigen foto of avatar kiezen");own.setTag("center-own-photo");own.setOnClickListener(v->{dialog.dismiss();a.photoPane=3;a.pickPhoto();});items.addView(own,new LinearLayout.LayoutParams(-1,a.dp(56)));
  if(Build.VERSION.SDK_INT>=30)panel.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(a.dp(16)+bars.left,a.dp(12)+bars.top,a.dp(16)+bars.right,a.dp(12)+bars.bottom);return insets;});dialog.setContentView(panel);dialog.show();dialog.getWindow().setLayout(-1,-1);
 }
 void section(LinearLayout target,String title,String[] ids,String[] names,Dialog dialog){TextView label=a.folderText(title,14,0xffa9bfd9);label.setPadding(0,a.dp(20),0,a.dp(8));target.addView(label);int columns=a.getResources().getConfiguration().screenWidthDp<320?2:3;
  for(int start=0;start<ids.length;start+=columns){LinearLayout row=new LinearLayout(a);for(int col=0;col<columns;col++){int index=start+col;if(index>=ids.length){row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));continue;}String id=ids[index];LinearLayout tile=new LinearLayout(a);tile.setTag("center-template-"+id);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(4),a.dp(10),a.dp(4),a.dp(10));tile.setBackground(a.rounded(selected().equals(id)?0xff304566:0xff17263d,16));tile.setContentDescription(names[index]);tile.setFocusable(true);View preview=new View(a){@Override protected void onDraw(Canvas c){CenterEmblem.this.draw(c,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*.45f,id);}};tile.addView(preview,new LinearLayout.LayoutParams(a.dp(74),a.dp(74)));TextView name=a.folderText(names[index],12,Color.WHITE);name.setGravity(Gravity.CENTER);name.setMaxLines(2);tile.addView(name,new LinearLayout.LayoutParams(-1,a.dp(38)));tile.setOnClickListener(v->{select(id);dialog.dismiss();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(a.dp(3),a.dp(3),a.dp(3),a.dp(3));row.addView(tile,lp);}target.addView(row);}
 }
 Bitmap tattoo(){if(tattoo==null)tattoo=BitmapFactory.decodeResource(a.getResources(),R.drawable.three_j);return tattoo;}
 void draw(Canvas c,float cx,float cy,float radius,String id){
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);c.save();c.translate(cx,cy);c.scale(radius,radius);
  p.setColor(background());c.drawCircle(0,0,1,p);
  if("photo".equals(id)){
   c.restore();if(a.photos[3]!=null)a.paintPhoto(c,3,new RectF(cx-radius,cy-radius,cx+radius,cy+radius),a.photoState(3));return;
  }
  p.setColor(ink());
  if("yin".equals(id)){p.setColor(0xccff9c40);c.drawArc(-1,-1,1,1,-90,180,true,p);c.drawCircle(0,-.5f,.5f,p);p.setColor(0xcc0e2d66);c.drawCircle(0,.5f,.5f,p);c.drawCircle(0,-.5f,.125f,p);p.setColor(0xccff9c40);c.drawCircle(0,.5f,.125f,p);}
  else if("tattoo".equals(id)){Bitmap b=tattoo();if(b!=null){p.setColorFilter(new PorterDuffColorFilter(ink(),PorterDuff.Mode.SRC_IN));c.drawBitmap(b,null,new RectF(-.98f,-.98f,.98f,.98f),p);}}
  else if(id.startsWith("zodiac_")){int index;try{index=Integer.parseInt(id.substring(7));}catch(NumberFormatException e){index=0;}index=Math.max(0,Math.min(11,index));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(1.3f);p.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(ZODIAC_GLYPHS[index]+"\uFE0E",0,-(fm.ascent+fm.descent)/2,p);}
  else if("heart".equals(id)){Path path=new Path();path.moveTo(0,.65f);path.cubicTo(-1,-.05f,-.8f,-.85f,-.25f,-.56f);path.quadTo(-.06f,-.48f,0,-.3f);path.cubicTo(.15f,-.95f,1,-.65f,.68f,0);path.quadTo(.45f,.35f,0,.65f);c.drawPath(path,p);}
  else if("flower".equals(id)||"mandala".equals(id)){int count="flower".equals(id)?8:12;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.045f);for(int i=0;i<count;i++){c.save();c.rotate(360f*i/count);c.drawOval(-.2f,-.8f,.2f,.05f,p);if(count==12)c.drawOval(-.1f,-.6f,.1f,.1f,p);c.restore();}p.setStyle(Paint.Style.FILL);c.drawCircle(0,0,.16f,p);}
  else if("sun".equals(id)){c.drawCircle(0,0,.38f,p);p.setStrokeWidth(.055f);p.setStrokeCap(Paint.Cap.ROUND);for(int i=0;i<12;i++){c.save();c.rotate(30*i);c.drawLine(0,-.55f,0,-.82f,p);c.restore();}}
  else if("moon".equals(id)){Path moon=new Path();moon.addCircle(0,0,.73f,Path.Direction.CW);Path cut=new Path();cut.addCircle(.35f,-.2f,.68f,Path.Direction.CW);moon.op(cut,Path.Op.DIFFERENCE);c.drawPath(moon,p);}
  else {Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?.8f:.36f;float x=(float)Math.cos(angle)*r,y=(float)Math.sin(angle)*r;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();c.drawPath(star,p);}
  c.restore();
 }
}
