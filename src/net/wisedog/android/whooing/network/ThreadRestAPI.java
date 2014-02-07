package net.wisedog.android.whooing.network;

import org.json.JSONObject;

import net.wisedog.android.whooing.advsms.AppDefine;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ThreadRestAPI extends Thread {
	private Handler mHandler;
	private Context mContext;
	
	public ThreadRestAPI(Handler mHandler, Context context) {
        super();
        this.mHandler = mHandler;
        this.mContext = context;
        checkLoginInfo();
    }
	
	private void checkLoginInfo() {	    
        SharedPreferences prefs = mContext.getSharedPreferences(AppDefine.SHARED_PREFERENCE, 0);    //0 is MODE_PRIVATE
        AppDefine.REAL_TOKEN = prefs.getString(AppDefine.KEY_SHARED_TOKEN, null);
        AppDefine.PIN = prefs.getString(AppDefine.KEY_SHARED_PIN, null);
        AppDefine.TOKEN_SECRET = prefs.getString(AppDefine.KEY_SHARED_TOKEN_SECRET, null);
        AppDefine.APP_SECTION = prefs.getString(AppDefine.KEY_SHARED_SECTION_ID, null);
        AppDefine.USER_ID = prefs.getInt(AppDefine.KEY_SHARED_USER_ID, 0);
        AppDefine.CURRENCY_CODE = prefs.getString(AppDefine.KEY_SHARED_CURRENCY_CODE, null);
        AppDefine.COUNTRY_CODE = prefs.getString(AppDefine.KEY_SHARED_COUNTRY_CODE, null);
        AppDefine.LOCALE_LANGUAGE = prefs.getString(AppDefine.KEY_SHARED_LOCALE_LANGUAGE, "");
        AppDefine.TIMEZONE = prefs.getString(AppDefine.KEY_SHARED_TIMEZONE, null);
		if (AppDefine.IS_DEBUG) {
			Log.i("wisedog", "user_id: " + AppDefine.USER_ID + " app_section : "
					+ AppDefine.APP_SECTION + " real_token:" + AppDefine.REAL_TOKEN
					+ " pin : " + AppDefine.PIN + " token_secret : "
					+ AppDefine.TOKEN_SECRET);
			Log.i("wisedog", "country: " + AppDefine.COUNTRY_CODE + " currency: "
					+ AppDefine.CURRENCY_CODE + " Locale : "
					+ AppDefine.LOCALE_LANGUAGE + " Timezone: " + AppDefine.TIMEZONE);
		}		
	}
	
	@Override
	public void run() {
		JSONObject result = null;
		Section section = new Section();
		result = section.getSections(AppDefine.APP_ID, AppDefine.REAL_TOKEN, 
				AppDefine.APP_SECRET, AppDefine.TOKEN_SECRET);
		sendMessage(result);
		super.run();
	}
	
	private void sendMessage(JSONObject result){
		Message msg = new Message();
		if(result != null){
			msg.what = AppDefine.MSG_API_OK;
		}
		else{
			msg.what = AppDefine.MSG_API_FAIL;
		}
		msg.obj = result;
		mHandler.sendMessage(msg);
	}
}
