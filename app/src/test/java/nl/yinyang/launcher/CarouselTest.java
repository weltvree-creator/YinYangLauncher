package nl.yinyang.launcher;
import android.view.*;import android.os.*;import android.graphics.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;
import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class CarouselTest {
 ActivityController<MainActivity> controller; MainActivity a;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();a=controller.get();a.prefs.edit().clear().commit();a.home.layout(0,0,1080,1920);}
 @After public void finish(){controller.pause().stop().destroy();}
 void touch(int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,x,y,0);a.home.onTouchEvent(e);e.recycle();}
 @Test public void centerTogglesToolbarWithoutSwitchingApps(){assertEquals(View.GONE,a.toolbar.getVisibility());touch(0,540,960);touch(1,540,960);assertEquals(View.VISIBLE,a.toolbar.getVisibility());assertEquals(0,a.page);touch(0,540,960);touch(1,540,960);assertEquals(View.GONE,a.toolbar.getVisibility());}
 @Test public void circleDragShowsNextBankAndRetainsBindings(){a.prefs.edit().putString(a.key(0,2),"camera/one").putString(a.key(1,2),"camera/two").commit();float r=a.home.ringRadius();touch(0,540,960-r);touch(2,540+r,960);assertTrue(a.home.rotation>.32f);touch(1,540+r,960);a.home.settling.end();assertEquals(1,a.page);assertEquals("camera/one",a.prefs.getString(a.key(0,2),""));assertEquals("camera/two",a.prefs.getString(a.key(1,2),""));touch(0,540+r,960);touch(2,540,960-r);touch(1,540,960-r);a.home.settling.end();assertEquals(0,a.page);}
 @Test public void canceledGestureDoesNotSwitchBank(){touch(0,540,550);touch(2,950,960);touch(3,950,960);assertEquals(0,a.page);assertEquals(0,a.home.rotation,0);}
 @Test public void photoMovementIsIndependentOfAppBank(){a.photoPane=1;a.page=0;a.adjusting=true;touch(0,300,1700);touch(1,350,1740);assertEquals(50,a.prefs.getFloat("x_1",0),0);assertEquals(40,a.prefs.getFloat("y_1",0),0);assertEquals(0,a.prefs.getFloat("x_0",0),0);assertEquals(0,a.page);}
 @Test public void drawingBothPhotosDoesNotChangeSelectedBank(){a.photos[0]=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);a.photos[0].eraseColor(Color.RED);a.photos[1]=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);a.photos[1].eraseColor(Color.GREEN);Bitmap out=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);a.home.draw(new Canvas(out));assertEquals(Color.RED,out.getPixel(20,20));assertEquals(Color.GREEN,out.getPixel(20,1900));assertEquals(0,a.page);}
}