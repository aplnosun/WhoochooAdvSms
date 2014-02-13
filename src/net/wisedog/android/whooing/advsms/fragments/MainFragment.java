/*
 * Copyright (C) 2013 Jongha Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wisedog.android.whooing.advsms.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import net.wisedog.android.whooing.advsms.AppDefine;
import net.wisedog.android.whooing.advsms.CardInfo;
import net.wisedog.android.whooing.advsms.R;
import net.wisedog.android.whooing.advsms.db.DatabaseHandler;
import net.wisedog.android.whooing.network.ThreadRestAPI;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

@SuppressLint("HandlerLeak")
public class MainFragment extends Fragment {
	
	private ArrayList<MessageEntity> mDataArray;
	private SmsListAdapter mListAdapter;
	private int mSelectIndex = -1;
	
	private long mFromTimeStamp;
	private long mToTimeStamp;
	
	DatabaseHandler mDb = null;
	
	public void setArgument(DatabaseHandler db){
		mDb = db;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.sms_main_fragment, container, false);
		if(view != null){
			mDataArray = new ArrayList<MessageEntity>();
			mListAdapter = new SmsListAdapter(getActivity(), mDataArray);
			ListView listView = (ListView)view.findViewById(R.id.smsListView); 
			if(listView != null){
				listView.setAdapter(mListAdapter);
			}
			String[] str = {"3일 전","특정 날짜"};
			final Spinner spinnerDate = (Spinner) view.findViewById(R.id.smsMainDateSpinner);
			final Spinner spinnerCard = (Spinner) view.findViewById(R.id.smsMainCardSpinner);
			TextView messageBoard = (TextView) view.findViewById(R.id.smsMessageBoard);
			Button searchBtn = (Button) view.findViewById(R.id.smsBtnSearch);
			final Button sendBtn = (Button) view.findViewById(R.id.smsBtnUpload);
			
			SharedPreferences prefs = getActivity().getSharedPreferences(AppDefine.SHARED_PREFERENCE, 0);
			
			if(sendBtn != null){
				sendBtn.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Bundle bundle = new Bundle();
						Boolean[] selected = mListAdapter.getSelectedArray();
						String rows = "";
						for(int i = 0; i < selected.length; i++){
							if(selected[i] == true){
								rows = rows + mDataArray.get(i).getBody() + "\n";
							}
						}
						
						bundle.putString("rows", rows);
						showProgress(true);
						sendBtn.setEnabled(false);
						ThreadRestAPI thread = new ThreadRestAPI(mHandler, getActivity(), AppDefine.API_POST_SMS, bundle);
						thread.start();
					}
				});				
			}
			
			
			String holdingCards = prefs.getString(AppDefine.KEY_SHARED_HOLDING_CARD, null);
			if(holdingCards == null){
				if(spinnerDate != null && spinnerCard != null
						&& searchBtn != null){
					spinnerDate.setEnabled(false);
					spinnerCard.setEnabled(false);
					searchBtn.setEnabled(false);
				}
				if(messageBoard != null){
					messageBoard.setText(getString(R.string.main_msg_need_setting));
				}
			}
			else{
				
				if(searchBtn != null){
					searchBtn.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							readSmsMessage(mFromTimeStamp, mToTimeStamp);							
						}
					});
				}
				//setting holding cards array
				if(spinnerCard != null){
					ArrayList<Integer> array = CardInfo.convertStringToIntArray(holdingCards);
					String[] cards = new String[array.size()];
					for(int i = 0; i < array.size(); i++){
						cards[i] = CardInfo.cardAddressList[(array.get(i)*2) +1];
					}
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
			                android.R.layout.select_dialog_item, cards) {
						
			            @Override
			            public View getView(int position, View convertView, ViewGroup parent) {
			                View v = super.getView(position, convertView, parent);
			                ((TextView) v).setTextColor(Color.rgb(0x33, 0x33, 0x33));
			                return v;
			            }

			        };
			        spinnerCard.setAdapter(adapter);
			        spinnerCard.setOnItemSelectedListener(new OnItemSelectedListener() {

						@Override
						public void onItemSelected(AdapterView<?> parent,
								View view, int pos, long id) {
							mSelectIndex = pos;
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {
							// Do nothing
							
						}
					});
				}
				
				if(spinnerDate != null){
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
			                android.R.layout.select_dialog_item, str) {
						
			            @Override
			            public View getView(int position, View convertView, ViewGroup parent) {
			                View v = super.getView(position, convertView, parent);
			                ((TextView) v).setTextColor(Color.rgb(0x33, 0x33, 0x33));
			                return v;
			            }

			        };
			        spinnerDate.setAdapter(adapter);
			        
			        spinnerDate.setOnItemSelectedListener(new OnItemSelectedListener() {
			        	final Calendar c = Calendar.getInstance();

						@Override
						public void onItemSelected(AdapterView<?> parent, View view, 
					            int pos, long id) {
							if(pos == 0){ 
						        /*c.add(Calendar.DAY_OF_MONTH, -3);
								readSMSMessage(MODE_LAST_THREE_DAYS, c.get(Calendar.YEAR), 
										c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));*/
								Calendar rightNow = Calendar.getInstance(TimeZone.getDefault());
								Date now = rightNow.getTime();
								mToTimeStamp = now.getTime();	
								
						        rightNow.add(Calendar.DAY_OF_MONTH, -3);
						        Date date = rightNow.getTime();
						        
						        mFromTimeStamp = date.getTime();
							}
							else if(pos == 1){
								
						        
						        int year = c.get(Calendar.YEAR);
						        int month = c.get(Calendar.MONTH)+1;
						        int day = c.get(Calendar.DAY_OF_MONTH); 
								
								DatePickerDialog dlg = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
									
									@Override
									public void onDateSet(DatePicker view, int year, int monthOfYear,
											int dayOfMonth) {
										/*readSMSMessage(MainActivity.MODE_SPECIFIED_DATE, year, monthOfYear, dayOfMonth);*/
										
									}
								}, year, month, day);
								dlg.show();
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {
							//Do nothing
						}
					});
			        
				}
				spinnerDate.setSelection(0);
				
			}
		}
		return view;
	}
	
	/**
	 * Show/Hide progressbar
	 * @param	onoff	true makes it show, false makes it gone 
	 * */
	public void showProgress(boolean onoff){
		ProgressBar progress = (ProgressBar)getView().findViewById(R.id.smsActivityProgress);
		if(progress != null){
			if(onoff == true){
				progress.setVisibility(View.VISIBLE);
			}else{
				progress.setVisibility(View.GONE);
			}
		}
	}
	

	/**
	 * Read SMS messages in the phone
	 * @param	from	time stamp of received time from
	 * @param	to		time stamp of received time to 
	 * */
	public int readSmsMessage(long from, long to) {
		Uri allMessage = Uri.parse("content://sms");
		ContentResolver cr = getActivity().getContentResolver();
		
		if(mSelectIndex == -1){
			return 0;
		}
		
		showProgress(true);
		
		SharedPreferences prefs = getActivity().getSharedPreferences(AppDefine.SHARED_PREFERENCE, 0);
		String holdingCards = prefs.getString(AppDefine.KEY_SHARED_HOLDING_CARD, null);
		
		if(holdingCards == null){
			return 0;
		}
		ArrayList<Integer> array = CardInfo.convertStringToIntArray(holdingCards);
		String addr = CardInfo.cardAddressList[array.get(mSelectIndex)*2];
		
		
		Cursor c = null;
		String[] PROJECTION = { "_id", "thread_id",
			"address", "person", "date", "body" }; 
		String WHERE1 = "address = " + addr;
		String WHERE = "(date BETWEEN " + from + " AND " 
				+ to + ") AND (" + WHERE1 + ")";
		
		c = cr.query(allMessage, PROJECTION , WHERE, null, "date DESC");

		String string = "";
		int count = 0;
		mDataArray.clear();
		while (c.moveToNext()) {
			MessageEntity entity = new MessageEntity(c.getLong(0), c.getLong(1), c.getString(2), 
					c.getLong(3), c.getLong(4), c.getString(5));
			if(mDb != null){
				if(mDb.isSent(c.getLong(0), c.getString(2), from, to) == true){
					;	// do not insert to array
				}
				else{
					mDataArray.add(entity);
				}
			}
			
			
			if(AppDefine.IS_DEBUG){
				//For debugging
				long messageId = c.getLong(0);
				long threadId = c.getLong(1);
				String address = c.getString(2);
				long contactId = c.getLong(3);
				String contactId_string = String.valueOf(contactId);
				long timestamp = c.getLong(4);
				String body = c.getString(5);

				string = String.format("msgid:%d, threadid:%d, address:%s, "
						+ "contactid:%d, contackstring:%s, timestamp:%d, body:%s",
						messageId, threadId, address, contactId, contactId_string,
						timestamp, body);
				Log.d("wisedog", ++count + "st, Message: " + string);
			}
		}
		mListAdapter.notifyDataSetChanged();
		showProgress(false);

		return 0;
	}
	
	 Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			if(msg.what == AppDefine.MSG_API_OK){
				showProgress(false);
				Button btn = (Button) getView().findViewById(R.id.smsBtnUpload);
				if(btn != null){
					btn.setEnabled(true);
				}
				if(msg.arg1 == AppDefine.API_POST_SMS){
					JSONObject obj = (JSONObject)msg.obj;
					try{
						int code = obj.getInt("code");
						if(code != 200){
							Toast.makeText(getActivity(), "Failed", Toast.LENGTH_LONG).show();
						}
						else{
							Boolean[] selected = mListAdapter.getSelectedArray();
							ArrayList<MessageEntity> arr = new ArrayList<MessageEntity>();
							for(int i = 0; i < selected.length; i++){
								if(selected[i] == true){
									arr.add(mDataArray.get(i));
								}
							}
							if(mDb.addSentSms(arr) == true){
								readSmsMessage(mFromTimeStamp, mToTimeStamp);
							}
							else{
								Toast.makeText(getActivity(), "DB Insert fail", Toast.LENGTH_LONG).show();
							}
						}
					}catch(JSONException e){
						
					}
				}				
			}
			super.handleMessage(msg);
		}
	 };
}
