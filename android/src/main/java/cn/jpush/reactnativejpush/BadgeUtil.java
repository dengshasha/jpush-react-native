package cn.jpush.reactnativejpush;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.jpush.android.data.JPushLocalNotification;

/**
 * Created by Melody.Deng on 2017/8/28.
 */

public class BadgeUtil {
    /**
     * Set badge count<br/>
     * 针对 Samsung / xiaomi / sony 手机有效
     * @param context The context of the application package.
     * @param count Badge count to be set
     */
    public static void setBadgeCount(Context context, int count) {
        if (count <= 0) {
            count = 0;
        } else {
            count = Math.max(0, Math.min(count, 99));
        }

        if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            sendToXiaoMi(context, count);
        } else if (Build.MANUFACTURER.equalsIgnoreCase("sony")) {
            sendToSony(context, count);
        } else if (Build.MANUFACTURER.toLowerCase().contains("samsung")) {
            sendToSamsumg(context, count);
        } else {
            Toast.makeText(context, "Not Support", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 向小米手机发送未读消息数广播
     * 在小米5上测试通过
     * 但有一个很奇怪的地方，小米手机应用图标上显示的数量是跟通知栏有关的，通知栏有几条，图标上就显示几条
     * 而且如果清除掉通知栏，应用图标上的数字也会相应的减少，并且在打开app以后，数字会自动清空，这里我传入的count貌似并没有作用
     * 这一点我查过是小米自己默认的操作，应该是无法更改的，我在小米手机上测试了今日头条，发现也是这样的情况。
     * @param count
     */
    public static void sendToXiaoMi(Context context, int count) {
        try {

//            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//            Notification.Builder builder = new Notification.Builder(context)
//                    .setContentTitle("title")
//                    .setContentText("hello")
//                    .setSmallIcon(R.drawable.ic_launcher);
//            Notification notification = builder.build();
            Notification notification = new Notification();
            Field field = notification.getClass().getDeclaredField("extraNotification");
            Object extraNotification = field.get(notification);
            Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
            method.invoke(extraNotification, count);

//            manager.notify(124, notification);
        } catch (Exception e) {
            e.printStackTrace();

            // miui 6之前的版本

            Intent localIntent = new Intent(
                    "android.intent.action.APPLICATION_MESSAGE_UPDATE");
            localIntent.putExtra(
                    "android.intent.extra.update_application_component_name",
                    context.getPackageName() + "/" + getLauncherClassName(context));
            localIntent.putExtra(
                    "android.intent.extra.update_application_message_text"
                    , String.valueOf(count == 0 ? "" : count));
            context.sendBroadcast(localIntent);
        }

    }

    /**
     * 向索尼手机发送未读消息数广播<br/>
     * 据说：需添加权限：
     *<uses-permission
     *android:name="com.sonyericsson.home.permission.BROADCAST_BADGE" /> [未验证]
     * @param count
     */
    private static void sendToSony(Context context, int count){
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }

        boolean isShow = true;
        if (count == 0) {
            isShow = false;
        }
        Intent localIntent = new Intent();

        localIntent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");

        localIntent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE"
                ,isShow);//是否显示

        localIntent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME"
                ,launcherClassName );//启动页

        localIntent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE"
                ,String.valueOf(count));//数字

        localIntent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME"
                , context.getPackageName());//包名

        context.sendBroadcast(localIntent);
    }

    /**
     * 向三星手机发送未读消息数广播
     * @param count
     */
    private static void sendToSamsumg(Context context, int count){
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        intent.putExtra("badge_count", count);
        intent.putExtra("badge_count_package_name", context.getPackageName());
        intent.putExtra("badge_count_class_name", launcherClassName);
        context.sendBroadcast(intent);
    }

    /**
     *向华为手机发送未读消息广播
     * */
    boolean isSupportedBade = false;
    public void checkIsSupportedByVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager(); //获取android项目的versionname
            PackageInfo info = manager.getPackageInfo("com.huawei.android.launcher", 0);
            if (info.versionCode >= 63029) {
                isSupportedBade = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setToHuawei(Context context, int count) {
        try {
            Bundle extra = new Bundle();
            extra.putString("package", "xxxxxx");
            extra.putString("class", "yyyyyy");
            extra.putInt("badgenumber", 2);
            context.getContentResolver().call(Uri.parse(
                    "content://com.huawei.android.launcher.settings/badge/"
            ), "change_launcher_badge", null, extra);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 重置、清除Badge未读显示数<br/>
     * @param context
     */
    public static void resetBadgeCount(Context context) {
        setBadgeCount(context, 0);
    }

    /**
     * Retrieve launcher activity name of the application from the context
     *
     * @param context The context of the application package.
     * @return launcher activity name of this application. From the
     *         "android:name" attribute.
     */
    private static String getLauncherClassName(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        // To limit the components this Intent will resolve to, by setting an
        // explicit package name.
        intent.setPackage(context.getPackageName());
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // All Application must have 1 Activity at least.
        // Launcher activity must be found!
        ResolveInfo info = packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        // get a ResolveInfo containing ACTION_MAIN, CATEGORY_LAUNCHER
        // if there is no Activity which has filtered by CATEGORY_DEFAULT
        if (info == null) {
            info = packageManager.resolveActivity(intent, 0);
        }

        return info.activityInfo.name;
    }
}

