package nl.yinyang.launcher;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class CenterEmblemTest {
 ActivityController<MainActivity> controller;MainActivity a;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();a=controller.get();a.prefs.edit().clear().commit();a.home.layout(0,0,1080,1920);}
 @After public void finish(){controller.pause().stop().destroy();}
 @Test public void defaultIsYinYang(){assertEquals("yin",a.emblem.selected());}
 @Test public void twelveNamedZodiacTemplatesAvailable(){assertEquals(12,CenterEmblem.ZODIAC_NAMES.length);assertEquals(12,CenterEmblem.ZODIAC_GLYPHS.length);assertEquals("Ram",CenterEmblem.ZODIAC_NAMES[0]);assertEquals("Vissen",CenterEmblem.ZODIAC_NAMES[11]);}
 @Test public void choosingTemplateSurvivesRestartWithoutChangingSlotsOrWidgets(){a.prefs.edit().putString(a.key(2,1),"keep/app").putString("widgets_0","[{\"uri\":\"content://roster\",\"height\":400}]").commit();a.home.wheelPosition=18;a.prefs.edit().putFloat("wheel_position",18).commit();a.emblem.select("zodiac_4");controller.recreate();a=controller.get();assertEquals("zodiac_4",a.emblem.selected());assertEquals("keep/app",a.prefs.getString(a.key(2,1),""));assertEquals(1,a.workspace.entries(0).length());assertEquals(18,a.home.wheelPosition,0);}
 @Test public void centerPickerShowsTattooAndAllZodiacOptions(){a.emblem.picker();Dialog d=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(d.findViewById(android.R.id.content).findViewWithTag("center-template-tattoo"));for(int i=0;i<12;i++)assertNotNull(d.findViewById(android.R.id.content).findViewWithTag("center-template-zodiac_"+i));d.findViewById(android.R.id.content).findViewWithTag("center-template-zodiac_6").performClick();assertEquals("zodiac_6",a.emblem.selected());}
 @Test public void ownImageIsSeparateFromAllThreeBackgrounds(){a.prefs.edit().putString("photo_0","above").putString("photo_1","below").putString("photo_2","middle").commit();a.photoPane=3;a.pickPhoto();Intent intent=shadowOf(a).getNextStartedActivity();assertEquals(Intent.ACTION_OPEN_DOCUMENT,intent.getAction());assertEquals("image/*",intent.getType());assertEquals("above",a.prefs.getString("photo_0",""));assertEquals("middle",a.prefs.getString("photo_2",""));assertEquals(4,a.photos.length);}
 @Test public void tappingCenterStillOpensLauncherMenu(){a.emblem.select("tattoo");for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,540,960,0);a.home.onTouchEvent(e);e.recycle();}assertEquals(View.VISIBLE,a.toolbar.getVisibility());assertEquals(0,a.home.wheelPosition,0);}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void selectedPhotoDrawsInsideCircleAndLeavesOutsideUntouched(){a.photos[3]=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);a.photos[3].eraseColor(Color.MAGENTA);a.emblem.select("photo");Bitmap out=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.GREEN);a.emblem.draw(c,150,150,100,"photo");assertEquals(Color.MAGENTA,out.getPixel(150,150));assertEquals(Color.GREEN,out.getPixel(51,51));assertEquals(Color.GREEN,out.getPixel(10,150));}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test public void renderEveryTemplateAndGallery() throws Exception {
  assertNotNull(a.emblem.tattoo());boolean alpha=false;Bitmap tattoo=a.emblem.tattoo();for(int x=0;x<tattoo.getWidth();x+=20)if(Color.alpha(tattoo.getPixel(x,0))<255)alpha=true;assertTrue(alpha);
  Bitmap atlas=Bitmap.createBitmap(1000,1250,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(atlas);c.drawColor(0xff101a2b);Paint text=new Paint(3);text.setColor(Color.WHITE);text.setTextSize(18);text.setTextAlign(Paint.Align.CENTER);
  String[] ids=new String[20],names=new String[20];System.arraycopy(CenterEmblem.BASE_IDS,0,ids,0,8);System.arraycopy(CenterEmblem.BASE_NAMES,0,names,0,8);for(int i=0;i<12;i++){ids[i+8]="zodiac_"+i;names[i+8]=CenterEmblem.ZODIAC_NAMES[i];}
  for(int i=0;i<ids.length;i++){float cx=125+(i%4)*250,cy=100+(i/4)*250;a.emblem.draw(c,cx,cy,85,ids[i]);c.drawText(names[i],cx,cy+115,text);}
  java.io.File dir=new java.io.File("build/photo-previews");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,"templates-"+Build.VERSION.SDK_INT+".png"))){atlas.compress(Bitmap.CompressFormat.PNG,100,out);}
  a.emblem.select("tattoo");Bitmap home=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);a.home.draw(new Canvas(home));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,"tattoo-home-"+Build.VERSION.SDK_INT+".png"))){home.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
}
