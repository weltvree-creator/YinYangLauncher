package nl.yinyang.launcher;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class MainActivityTest {
 ActivityController<MainActivity> controller; MainActivity activity; IconApp camera;
 static class IconApp extends ResolveInfo {
  int iconLoads;
  IconApp(String name){activityInfo=new ActivityInfo();activityInfo.packageName="example.camera";activityInfo.name=name;}
  @Override public CharSequence loadLabel(PackageManager pm){return "Camera";}
  @Override public Drawable loadIcon(PackageManager pm){iconLoads++;return new ColorDrawable(Color.RED);}
 }
 @Before public void setUp(){
  controller=Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();
  activity=controller.get();activity.prefs.edit().clear().commit();
  camera=new IconApp("CameraActivity");activity.installed.clear();activity.installed.add(camera);
 }
 @After public void tearDown(){controller.pause().stop().destroy();}
 void saveCamera(){activity.prefs.edit().putString(activity.key(0,2),activity.appId(camera)).commit();}
 @Test public void singleAppOpensDirectly(){
  saveCamera();activity.slot(2);
  Intent launched=shadowOf(activity).getNextStartedActivity();
  assertNotNull(launched);assertEquals("example.camera",launched.getComponent().getPackageName());
  assertEquals("CameraActivity",launched.getComponent().getClassName());
 }
 @Test public void savedDuplicatesStillOpenSingleApp(){
  String id=activity.appId(camera);activity.prefs.edit().putString(activity.key(0,2),id+"|"+id).commit();
  assertEquals(1,activity.slotApps(0,2).size());activity.slot(2);assertNotNull(shadowOf(activity).getNextStartedActivity());
 }
 @Test public void circleDrawsActualAppIcon(){
  saveCamera();Bitmap bitmap=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888);
  activity.home.drawSlotIcons(new Canvas(bitmap),2,100,100);
  assertTrue(camera.iconLoads>0);assertEquals(Color.RED,bitmap.getPixel(100,100));
 }
 @Test public void groupKeepsBothAppsAndShowsChooser(){
  IconApp second=new IconApp("SecondCamera");activity.installed.add(second);
  activity.prefs.edit().putString(activity.key(0,2),activity.appId(camera)+"|"+activity.appId(second)).commit();
  activity.slot(2);assertNull(shadowOf(activity).getNextStartedActivity());
  assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
  android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
  android.view.View tile=dialog.getWindow().getDecorView().findViewWithTag(activity.appId(second));
  assertNotNull(tile);assertTrue(camera.iconLoads>0);tile.performClick();
  assertFalse(dialog.isShowing());assertEquals("SecondCamera",shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
 }
 @Test public void savedBindingSurvivesRecreation(){
  saveCamera();controller.recreate();activity=controller.get();
  activity.installed.clear();activity.installed.add(camera);
  assertEquals(1,activity.slotApps(0,2).size());
 }
 @Test public void editMenuRemainsAvailableForSingleApp(){
  saveCamera();activity.slotMenu(2);
  assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
  assertNotNull(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView().findViewWithTag(activity.appId(camera)));
 }
}
