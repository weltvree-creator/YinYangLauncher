package nl.yinyang.launcher;
import android.graphics.*;
import android.graphics.drawable.*;
import android.app.Dialog;
import android.net.Uri;
import android.media.ExifInterface;
import android.view.*;
import org.junit.*;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35},qualifiers="w360dp-h640dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PhotoTest {
 ActivityController<MainActivity> controller;MainActivity a;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();a=controller.get();a.prefs.edit().clear().commit();a.home.layout(0,0,1080,1920);}
 @After public void teardown(){controller.pause().stop().destroy();}
 Bitmap image(int w,int h,int color){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(color);return b;}
 @Test public void landscapePhotoFitsCompletelyWithoutDistortion(){Bitmap b=image(1600,900,Color.RED);MainActivity.PhotoState state=new MainActivity.PhotoState();RectF frame=a.home.paneBounds(0);RectF dst=a.photoBounds(b,state,frame);assertEquals(1600f/900,dst.width()/dst.height(),.001f);assertTrue(dst.left>=frame.left-.1f&&dst.right<=frame.right+.1f);assertTrue(dst.top>=frame.top-.1f&&dst.bottom<=frame.bottom+.1f);a.photos[0]=b;Bitmap out=image(1080,1920,Color.BLACK);a.home.draw(new Canvas(out));assertEquals(Color.RED,out.getPixel((int)dst.centerX(),(int)dst.centerY()));assertEquals(a.prefs.getInt("color_0",0xff18243a),out.getPixel(10,(int)frame.centerY()));}
 @Test public void fillingAreaCoversFrameAndRetainsAspect(){Bitmap b=image(1600,900,Color.RED);MainActivity.PhotoState state=new MainActivity.PhotoState();state.fit=false;RectF frame=a.home.paneBounds(0),dst=a.photoBounds(b,state,frame);assertTrue(dst.width()>=frame.width()-.1f&&dst.height()>=frame.height()-.1f);assertEquals(1600f/900,dst.width()/dst.height(),.001f);}
 @Test public void threeAreasKeepSeparateSettings(){for(int i=0;i<3;i++){MainActivity.PhotoState state=new MainActivity.PhotoState();state.turns=i;state.fit=i!=1;state.zoom=1+i*.25f;state.x=i*20;a.savePhotoState(i,state);}for(int i=0;i<3;i++){MainActivity.PhotoState state=a.photoState(i);assertEquals(i,state.turns);assertEquals(i!=1,state.fit);assertEquals(1+i*.25f,state.zoom,.001f);assertEquals(i*20,state.x,.001f);}}
 @Test public void quarterTurnFitsPortraitCorrectly(){Bitmap b=image(1600,900,Color.RED);MainActivity.PhotoState state=new MainActivity.PhotoState();state.turns=1;RectF dst=a.photoBounds(b,state,a.home.paneBounds(0));assertEquals(900f/1600,dst.width()/dst.height(),.001f);}
 @Test public void panningCannotLosePhotoOutsideFrame(){Bitmap b=image(1600,900,Color.RED);MainActivity.PhotoState state=new MainActivity.PhotoState();state.x=100000;state.y=-100000;RectF frame=a.home.paneBounds(0),dst=a.photoBounds(b,state,frame);assertTrue(dst.left>=frame.left-.1f&&dst.right<=frame.right+.1f);assertTrue(dst.top>=frame.top-.1f&&dst.bottom<=frame.bottom+.1f);}
 @Test public void exifRotationIsReadWhenLoadingJpeg()throws Exception{File file=File.createTempFile("orientation-",".jpg",a.getCacheDir());try(FileOutputStream stream=new FileOutputStream(file)){image(600,300,Color.RED).compress(Bitmap.CompressFormat.JPEG,95,stream);}ExifInterface exif=new ExifInterface(file.getPath());exif.setAttribute(ExifInterface.TAG_ORIENTATION,"6");exif.saveAttributes();a.prefs.edit().putString("photo_0",Uri.fromFile(file).toString()).commit();a.loadPhoto(0);assertNotNull(a.photos[0]);assertEquals(300,a.photos[0].getWidth());assertEquals(600,a.photos[0].getHeight());file.delete();}
 @Test public void editorSavesRotationAndCanCancel(){a.photos[2]=image(800,450,Color.BLUE);a.photoPane=2;a.adjustPhoto();Dialog dialog=ShadowDialog.getLatestDialog();View decor=dialog.getWindow().getDecorView();decor.findViewWithTag("photo-rotate").performClick();assertEquals(0,a.photoState(2).turns);decor.findViewWithTag("photo-cancel").performClick();assertEquals(0,a.photoState(2).turns);a.adjustPhoto();dialog=ShadowDialog.getLatestDialog();decor=dialog.getWindow().getDecorView();decor.findViewWithTag("photo-rotate").performClick();decor.findViewWithTag("photo-save").performClick();assertEquals(1,a.photoState(2).turns);assertEquals(0,a.photoState(0).turns);}
 @Test public void upgradeKeepsUrisAndAppsWhileRepairingOldCrop(){a.prefs.edit().putString("photo_0","content://test/top").putString(a.key(1,2),"camera/one").putFloat("zoom_0",3).putFloat("x_0",5000).commit();a.migratePhotos();assertEquals("content://test/top",a.prefs.getString("photo_0",""));assertEquals("camera/one",a.prefs.getString(a.key(1,2),""));assertEquals(1,a.photoState(0).zoom,0);assertEquals(0,a.photoState(0).x,0);assertTrue(a.photoState(0).fit);MainActivity.PhotoState state=a.photoState(0);state.zoom=2;a.savePhotoState(0,state);a.migratePhotos();assertEquals(2,a.photoState(0).zoom,0);}
 @Test public void centerBackgroundAndColorDrawBehindApps(){a.prefs.edit().putInt("color_2",0xff778899).commit();Bitmap out=image(1080,1920,Color.BLACK);a.home.draw(new Canvas(out));assertEquals(0xff778899,out.getPixel(10,960));a.photos[2]=image(200,200,Color.RED);MainActivity.PhotoState state=new MainActivity.PhotoState();state.fit=false;a.savePhotoState(2,state);a.home.draw(new Canvas(out));int pixel=out.getPixel(10,960);assertTrue(Color.red(pixel)>100);assertEquals(0,Color.green(pixel));assertEquals(0,Color.blue(pixel));}
 @Test public void renderHomeAndPhotoEditorPreview()throws Exception{
  Bitmap landscape=image(800,450,0xff93bfdd);Canvas scene=new Canvas(landscape);Paint brush=new Paint(3);brush.setColor(0xffe9c46a);scene.drawCircle(610,105,50,brush);brush.setColor(0xff507b67);scene.drawRect(0,240,800,450,brush);brush.setColor(0xffcc694e);scene.drawRect(55,150,190,320,brush);brush.setColor(0xffe6e0cd);scene.drawRect(610,170,745,340,brush);
  a.photos[0]=landscape;a.photos[1]=landscape;a.prefs.edit().putInt("color_2",0xff263747).commit();Bitmap out=image(1080,1920,Color.BLACK);a.home.draw(new Canvas(out));File dir=new File("build/photo-previews");dir.mkdirs();try(FileOutputStream stream=new FileOutputStream(new File(dir,"home-"+android.os.Build.VERSION.SDK_INT+".png"))){out.compress(Bitmap.CompressFormat.PNG,100,stream);}
  a.photoPane=0;a.adjustPhoto();Dialog dialog=ShadowDialog.getLatestDialog();View decor=dialog.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,1920);Bitmap editor=image(1080,1920,Color.BLACK);decor.draw(new Canvas(editor));try(FileOutputStream stream=new FileOutputStream(new File(dir,"editor-"+android.os.Build.VERSION.SDK_INT+".png"))){editor.compress(Bitmap.CompressFormat.PNG,100,stream);}
 }
}
