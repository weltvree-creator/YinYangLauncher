package nl.yinyang.launcher;
import android.view.*;import android.os.*;import android.graphics.*;
import android.content.*;import android.content.pm.*;import android.graphics.drawable.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;
import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk={28,35})
public class CarouselTest {
 ActivityController<MainActivity> controller;MainActivity a;
 @Before public void setup(){controller=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();a=controller.get();a.prefs.edit().clear().commit();a.home.wheelPosition=0;a.home.layout(0,0,1080,1920);}
 @After public void finish(){controller.pause().stop().destroy();}
 void touch(int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,SystemClock.uptimeMillis(),action,x,y,0);a.home.onTouchEvent(e);e.recycle();}
 void turn(){float r=a.home.ringRadius();touch(0,540,960-r);touch(2,540-r,960);touch(2,540,960+r);touch(2,540+r,960);touch(2,540,960-r);touch(1,540,960-r);a.home.settling.end();}
 @Test public void centerTogglesToolbarWithoutAdvancing(){assertEquals(View.GONE,a.toolbar.getVisibility());touch(0,540,960);touch(1,540,960);assertEquals(View.VISIBLE,a.toolbar.getVisibility());assertEquals(0,a.home.wheelPosition,0);touch(0,540,960);touch(1,540,960);assertEquals(View.GONE,a.toolbar.getVisibility());}
 @Test public void eachFullTurnRevealsNineNewSlotsAndEventuallyWraps(){assertEquals(0,a.home.visibleSlot(0));turn();assertEquals(9,a.home.visibleSlot(0));assertEquals(17,a.home.visibleSlot(8));turn();assertEquals(18,a.home.visibleSlot(0));assertEquals(26,a.home.visibleSlot(8));turn();assertEquals(0,a.home.visibleSlot(0));}
 @Test public void partialDragIntroducesNewSlotInsteadOfRepeatingSameNine(){float r=a.home.ringRadius();touch(0,540,960-r);touch(2,540-r,960);assertTrue(a.home.wheelPosition>2);touch(1,540-r,960);a.home.settling.end();assertEquals(2,a.home.visibleSlot(0));assertEquals(10,a.home.visibleSlot(8));}
 @Test public void bothOldBanksKeepTheirBindingsWhileScrolling(){a.prefs.edit().putString(a.key(0,2),"camera/one").putString(a.key(1,2),"camera/two").commit();turn();turn();assertEquals("camera/one",a.prefs.getString(a.key(0,2),""));assertEquals("camera/two",a.prefs.getString(a.key(1,2),""));}
 @Test public void canceledGestureRestoresStartingPosition(){a.home.wheelPosition=5;touch(0,540,550);touch(2,950,960);touch(3,950,960);assertEquals(5,a.home.wheelPosition,0);}
 static class App extends ResolveInfo{App(){activityInfo=new ActivityInfo();activityInfo.packageName="example.camera";activityInfo.name="CameraActivity";} @Override public CharSequence loadLabel(PackageManager pm){return "Camera";} @Override public Drawable loadIcon(PackageManager pm){return new ColorDrawable(Color.RED);}}
 @Test public void tappingLaterSlotOpensItsOwnApp(){App camera=new App();a.installed.clear();a.installed.add(camera);a.prefs.edit().putString(a.key(2,0),a.appId(camera)).commit();a.home.wheelPosition=18;float r=a.home.ringRadius();touch(0,540,960-r);touch(1,540,960-r);Intent opened=shadowOf(a).getNextStartedActivity();assertNotNull(opened);assertEquals("CameraActivity",opened.getComponent().getClassName());}
 @Test public void addingMoreSlotsMakesThemReachableWithoutAlteringExistingSlots(){a.prefs.edit().putString(a.key(0,2),"keep/this").commit();a.wheelSettings();android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();dialog.getListView().performItemClick(null,0,0);assertEquals(36,a.wheelSlots());assertEquals(27,a.home.visibleSlot(0));assertEquals(35,a.home.visibleSlot(8));assertEquals("keep/this",a.prefs.getString(a.key(0,2),""));}
 @Test public void positionSurvivesActivityRecreation(){turn();controller.recreate();a=controller.get();assertEquals(9,a.home.visibleSlot(0));}
 @Test public void negativePositionWrapsAndHitTestingUsesVisibleSlot(){a.home.wheelPosition=-1;assertEquals(26,a.home.visibleSlot(0));assertEquals(7,a.home.visibleSlot(8));assertEquals(26,a.home.wheelSlotAt(540,960-a.home.ringRadius()));}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE) @Test public void photoAreasStillDrawWhileWheelScrolls(){a.photos[0]=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);a.photos[0].eraseColor(Color.RED);a.photos[1]=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);a.photos[1].eraseColor(Color.GREEN);a.home.wheelPosition=18.3f;Bitmap out=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);a.home.draw(new Canvas(out));assertEquals(Color.RED,out.getPixel(540,20));assertEquals(Color.GREEN,out.getPixel(540,1900));assertEquals(18.3f,a.home.wheelPosition,0);}
}