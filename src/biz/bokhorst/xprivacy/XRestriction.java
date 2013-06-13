package biz.bokhorst.xprivacy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.util.Log;

public class XRestriction {

	// This should correspond with restrict_<name> in strings.xml
	public static final String cAccounts = "accounts";
	public static final String cBoot = "boot";
	public static final String cBrowser = "browser";
	public static final String cCalendar = "calendar";
	public static final String cContacts = "contacts";
	public static final String cIdentification = "identification";
	public static final String cLocation = "location";
	public static final String cMedia = "media";
	public static final String cMessages = "messages";
	public static final String cPhone = "phone";
	public static final String cStorage = "storage";
	public static final String cSystem = "system";

	public static final String cDefaceString = "DEFACE";
	public static final long cDefaceHex = 0xDEFACEL;
	public static final String cDefacedMac = "de:fa:ce:de:fa:ce";

	public final static int cUidAndroid = 1000;

	public final static String cExpertMode = "ExpertMode";

	private static Map<String, List<String>> mRestrictions = new LinkedHashMap<String, List<String>>();

	static {
		mRestrictions.put(cAccounts, new ArrayList<String>());
		mRestrictions.put(cBoot, new ArrayList<String>());
		mRestrictions.put(cBrowser, new ArrayList<String>());
		mRestrictions.put(cCalendar, new ArrayList<String>());
		mRestrictions.put(cContacts, new ArrayList<String>());
		mRestrictions.put(cIdentification, new ArrayList<String>());
		mRestrictions.put(cLocation, new ArrayList<String>());
		mRestrictions.put(cMedia, new ArrayList<String>());
		mRestrictions.put(cMessages, new ArrayList<String>());
		mRestrictions.put(cPhone, new ArrayList<String>());
		mRestrictions.put(cStorage, new ArrayList<String>());
		mRestrictions.put(cSystem, new ArrayList<String>());

		// Temporary solution
		mRestrictions.get(cAccounts).add("GET_ACCOUNTS");
		mRestrictions.get(cAccounts).add("USE_CREDENTIALS");
		mRestrictions.get(cAccounts).add("MANAGE_ACCOUNTS");
		mRestrictions.get(cBoot).add("RECEIVE_BOOT_COMPLETED");
		mRestrictions.get(cBrowser).add("READ_HISTORY_BOOKMARKS");
		mRestrictions.get(cBrowser).add("GLOBAL_SEARCH");
		mRestrictions.get(cCalendar).add("READ_CALENDAR");
		mRestrictions.get(cContacts).add("READ_CONTACTS");
		mRestrictions.get(cIdentification).add("ACCESS_WIFI_STATE");
		mRestrictions.get(cLocation).add("ACCESS_COARSE_LOCATION");
		mRestrictions.get(cLocation).add("ACCESS_FINE_LOCATION");
		mRestrictions.get(cLocation).add("ACCESS_COARSE_UPDATES");
		mRestrictions.get(cLocation).add("CONTROL_LOCATION_UPDATES");
		mRestrictions.get(cMedia).add("CAMERA");
		mRestrictions.get(cMedia).add("RECORD_AUDIO");
		mRestrictions.get(cMedia).add("RECORD_VIDEO");
		mRestrictions.get(cMessages).add("READ_WRITE_ALL_VOICEMAIL");
		mRestrictions.get(cMessages).add("READ_SMS");
		mRestrictions.get(cMessages).add("RECEIVE_SMS");
		mRestrictions.get(cPhone).add("READ_PHONE_STATE");
		mRestrictions.get(cPhone).add("PROCESS_OUTGOING_CALLS");
		mRestrictions.get(cPhone).add("READ_CALL_LOG");
		mRestrictions.get(cPhone).add("WRITE_APN_SETTINGS");
		mRestrictions.get(cStorage).add("READ_EXTERNAL_STORAGE");
		mRestrictions.get(cStorage).add("WRITE_EXTERNAL_STORAGE");
	}

	public static void registerMethod(String methodName, String restrictionName, String[] permissions) {
		// TODO: register method name for more granularity
		if (restrictionName != null && !mRestrictions.containsKey(restrictionName))
			XUtil.log(null, Log.WARN, "Missing restriction " + restrictionName);
		for (String permission : permissions)
			if (!mRestrictions.get(restrictionName).contains(permission))
				XUtil.log(null, Log.WARN, "Missing permission " + permission);
	}

	public static List<String> getRestrictions(Context context) {
		List<String> listRestriction = new ArrayList<String>(mRestrictions.keySet());
		if (!XRestriction.getSetting(null, context, XRestriction.cExpertMode))
			listRestriction.remove(cBoot);
		return listRestriction;
	}

	public static List<String> getPermissions(String restrictionName) {
		return mRestrictions.get(restrictionName);
	}

	public static boolean hasInternet(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		return (pm.checkPermission("android.permission.INTERNET", packageName) == PackageManager.PERMISSION_GRANTED);
	}

	@SuppressLint("DefaultLocale")
	public static boolean hasPermission(Context context, String packageName, String restrictionName) {
		List<String> listPermission = mRestrictions.get(restrictionName);
		if (listPermission == null || listPermission.size() == 0)
			return true;

		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
			if (pInfo != null)
				for (String rPermission : pInfo.requestedPermissions)
					for (String permission : listPermission)
						if (rPermission.toLowerCase().contains(permission.toLowerCase()))
							return true;
		} catch (Throwable ex) {
			XUtil.bug(null, ex);
			return false;
		}
		return false;
	}

	public static boolean isUsed(Context context, int uid, String restrictionName) {
		long lastUsage = 0;
		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(XPrivacyProvider.URI_USAGE, null, restrictionName,
				new String[] { Integer.toString(uid) }, null);
		if (cursor.moveToNext())
			lastUsage = cursor.getLong(cursor.getColumnIndex(XPrivacyProvider.COL_USED));
		cursor.close();
		return (lastUsage != 0);
	}

	public static boolean getRestricted(XHook hook, Context context, int uid, String restrictionName, boolean usage) {
		try {
			if (uid == XRestriction.cUidAndroid)
				return false;

			// Check context
			if (context == null) {
				XUtil.log(hook, Log.WARN, "context is null");
				XUtil.logStack(hook);
				return false;
			}

			// Check uid
			if (uid == 0) {
				XUtil.log(hook, Log.WARN, "uid=0");
				XUtil.logStack(hook);
				return false;
			}

			// Get content resolver
			ContentResolver contentResolver = context.getContentResolver();
			if (contentResolver == null) {
				XUtil.log(hook, Log.WARN, "contentResolver is null");
				XUtil.logStack(hook);
				return false;
			}

			// Query restriction
			String methodName = (hook == null ? null : hook.getMethodName());
			Cursor cursor = contentResolver.query(XPrivacyProvider.URI_RESTRICTION, null, restrictionName,
					new String[] { Integer.toString(uid), Boolean.toString(usage), methodName }, null);
			if (cursor == null) {
				XUtil.log(hook, Log.WARN, "cursor is null");
				XUtil.logStack(null);
				return false;
			}

			// Get restriction
			boolean restricted = true;
			if (cursor.moveToNext())
				restricted = Boolean.parseBoolean(cursor.getString(cursor
						.getColumnIndex(XPrivacyProvider.COL_RESTRICTED)));
			else {
				XUtil.log(hook, Log.WARN, "cursor is empty");
				XUtil.logStack(null);
			}
			cursor.close();

			// Result
			XUtil.log(
					hook,
					Log.INFO,
					String.format("get %s/%s %s=%b", getPackageName(context, uid),
							(hook == null ? null : hook.getMethodName()), restrictionName, restricted));
			return restricted;
		} catch (Throwable ex) {
			XUtil.bug(hook, ex);
			return false;
		}
	}

	public static void setRestricted(XHook hook, Context context, int uid, String restrictionName, boolean restricted) {
		// Check context
		if (context == null) {
			XUtil.log(hook, Log.WARN, "context is null");
			return;
		}

		// Check uid
		if (uid == 0) {
			XUtil.log(hook, Log.WARN, "uid=0");
			return;
		}

		// Get content resolver
		ContentResolver contentResolver = context.getContentResolver();
		if (contentResolver == null) {
			XUtil.log(hook, Log.WARN, "contentResolver is null");
			return;
		}

		// Set restrictions
		ContentValues values = new ContentValues();
		values.put(XPrivacyProvider.COL_UID, uid);
		values.put(XPrivacyProvider.COL_RESTRICTED, Boolean.toString(restricted));
		contentResolver.update(XPrivacyProvider.URI_RESTRICTION, values, restrictionName, null);

		XUtil.log(
				hook,
				Log.INFO,
				String.format("set %s/%s %s=%b", getPackageName(context, uid),
						(hook == null ? null : hook.getMethodName()), restrictionName, restricted));
	}

	public static boolean getSetting(XHook hook, Context context, String settingName) {
		boolean enabled = false;
		if (context == null) {
			XUtil.log(hook, Log.WARN, "context is null");
			XUtil.logStack(hook);
		} else
			try {
				ContentResolver cr = context.getContentResolver();
				Cursor cursor = cr.query(XPrivacyProvider.URI_SETTING, null, settingName, null, null);
				if (cursor.moveToNext())
					enabled = Boolean
							.parseBoolean(cursor.getString(cursor.getColumnIndex(XPrivacyProvider.COL_ENABLED)));
				else {
					XUtil.log(hook, Log.WARN, "cursor is empty setting=" + settingName);
					XUtil.logStack(hook);
				}
				cursor.close();
			} catch (Throwable ex) {
				XUtil.log(hook, Log.ERROR, "get setting=" + settingName);
				XUtil.bug(hook, ex);
			}
		XUtil.log(hook, Log.INFO, String.format("get %s=%b", settingName, enabled));
		return enabled;
	}

	public static void setSetting(XHook hook, Context context, String settingName, boolean enabled) {
		ContentResolver contentResolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(XPrivacyProvider.COL_ENABLED, Boolean.toString(enabled));
		contentResolver.update(XPrivacyProvider.URI_SETTING, values, settingName, null);
		XUtil.log(hook, Log.INFO, String.format("set %s=%b", settingName, enabled));
	}

	public static void deleteAuditTrail(Context context, int uid) {
		for (String restrictionName : XRestriction.getRestrictions(context))
			context.getContentResolver().delete(XPrivacyProvider.URI_AUDIT, restrictionName,
					new String[] { Integer.toString(uid) });
	}

	public static String getLocalizedName(Context context, String restrictionName) {
		String packageName = XRestriction.class.getPackage().getName();
		int stringId = context.getResources().getIdentifier("restrict_" + restrictionName, "string", packageName);
		return (stringId == 0 ? null : context.getString(stringId));
	}

	private static String getPackageName(Context context, int uid) {
		String[] packages = context.getPackageManager().getPackagesForUid(uid);
		if (packages != null && packages.length == 1)
			return packages[0];
		return Integer.toString(uid);
	}
}
