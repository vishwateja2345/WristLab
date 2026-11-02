package studio.teja.airbridge;
/** Only app package names and explicit command state; no UI content or history. */
public final class ControlContext {
 public static volatile String selected="AUTO",activePackage="";public static volatile boolean comments;
 public static String mode(){if(comments&&activePackage.equals("com.instagram.android"))return "COMMENTS";if(!selected.equals("AUTO"))return selected;if(activePackage.equals("com.instagram.android")||activePackage.equals("com.google.android.youtube"))return "REELS";return "READING";}
 public static String resolve(String command,String mode){if(command.equals("PRIMARY"))command=mode.equals("MEDIA")?"MEDIA_PLAY_PAUSE":mode.equals("NAV")?"BACK":"NEXT";if(command.equals("CONTEXT"))command=(mode.equals("REELS")||mode.equals("COMMENTS"))?"COMMENTS":mode.equals("MEDIA")?"MEDIA_NEXT":mode.equals("NAV")?"RECENTS":"BACK";if((command.equals("NEXT")||command.equals("PREV"))&&mode.equals("MEDIA"))command=command.equals("NEXT")?"VOLUME_UP":"VOLUME_DOWN";if((command.equals("NEXT")||command.equals("PREV"))&&mode.equals("NAV"))command=command.equals("NEXT")?"BACK":"HOME";return command;}
 public static boolean allowed(String s){return (" NEXT PREV PRIMARY CONTEXT COMMENTS SCROLL_DOWN SCROLL_UP BACK HOME RECENTS NOTIFICATIONS QUICK_SETTINGS SCREENSHOT MEDIA_PLAY_PAUSE MEDIA_NEXT MEDIA_PREV VOLUME_UP VOLUME_DOWN LAUNCH_INSTAGRAM LAUNCH_YOUTUBE LAUNCH_BROWSER LAUNCH_MAPS PROFILE_AUTO PROFILE_REELS PROFILE_READING PROFILE_MEDIA PROFILE_NAV VIDEO_RESET ").contains(" "+s+" ");}
}
