/*
 *  Peter Lin: rootlocus@yahoo.com
 *  
 */

package com.example.fairfax;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.widget.ListView;
import org.json.JSONObject;
import android.widget.ArrayAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.graphics.drawable.Drawable;
import java.util.Map;
import java.util.HashMap;
import java.net.MalformedURLException;
import android.os.Handler;
import android.os.Message;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import android.util.Log;
import android.app.ProgressDialog;
import java.util.Iterator;


public class MainActivity extends Activity {

	private String mListTitle;
	private listItem[] mListItems;
	private ListView mListView;
	private ViewHolderAdapter mAdapter;
	private Context mContext = this;
	private LazyLoadDrawableManager mDrawableManager;
	
	public class listItem {
		String title;
		String description;
		String imageHref;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("a22183", "onCreate()");

		mListTitle = null;		
		mListView = (ListView) findViewById(R.id.list);
		mDrawableManager = new LazyLoadDrawableManager();
		
		new DownloadJsonFeed().execute("https://dl.dropboxusercontent.com/u/10168342/facts.json");
	}
    
    private class DownloadJsonFeed extends AsyncTask<String, Void, String> {
    	
    	private ProgressDialog progressDialog;
    	
        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(mContext, "", "Loading...");
        }
        
        @Override
        protected String doInBackground(String... params) {
              
            try {
                return downloadUrl(params[0]);
            } catch (IOException e) {
                return "Unable to retrieve json feed. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
        	progressDialog.dismiss();
        	
        	try {
        	    parseJSON(result);
        	    mAdapter = new ViewHolderAdapter(mContext, R.layout.listview_item_row, mListItems);
        	    mListView.setAdapter(mAdapter);
        	    Activity activity = (Activity) mContext;
        	    activity.setTitle(mListTitle);
        	} catch (JSONException e) {
        	}
        }
    }
    
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
            
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line = null;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            return sb.toString();
        } finally {
            if (is != null) {
                is.close();
            } 
        }
    }
    
	private void parseJSON(String feed) throws JSONException {
			JSONObject json_obj = new JSONObject(feed);
			mListTitle = json_obj.getString("title");
			JSONArray jArray = json_obj.getJSONArray("rows");
			Log.d("a22183", "number of rows is: " + jArray.length());
			mListItems = new listItem[jArray.length()];
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject jo = jArray.getJSONObject(i);
				listItem item = new listItem();
				item.title = jo.getString("title");
				item.description = jo.getString("description");
				item.imageHref = jo.getString("imageHref");
				mListItems[i] = item;
				Log.d("a22183", "rows " + i + " : " + "\ntitle=" + item.title + "\ndescription=" + item.description + "\nimageHref=" + item.imageHref);
			}
	}

	public class ViewHolderAdapter extends ArrayAdapter<listItem> {
		
		private int layoutResourceId;
		private listItem items[] = null;

		public ViewHolderAdapter(Context context, int layoutResourceId, listItem[] items) {
			super(context, layoutResourceId, items);
			this.layoutResourceId = layoutResourceId;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder viewHolder = null;
			View v = convertView;  //!!!!!!!! otherwise image sometimes misplaced

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(layoutResourceId, parent, false);

				viewHolder = new ViewHolder();
				viewHolder.title = (TextView) v.findViewById(R.id.title);
				viewHolder.description = (TextView) v.findViewById(R.id.description);
				viewHolder.image = (ImageView) v.findViewById(R.id.image);

				v.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) v.getTag();
			}

			listItem item = items[position];
			if (item != null) {
				if (!(item.title).equalsIgnoreCase("null")) {
					viewHolder.title.setText(item.title);
				} else {
					viewHolder.title.setText("");
				}
					
                if (!(item.description).equalsIgnoreCase("null")) {
                	viewHolder.description.setText(item.description);
                } else {
                	viewHolder.description.setText("");
                }
				if (!(item.imageHref).equalsIgnoreCase("null")) {
					viewHolder.image.setImageDrawable(null);
					mDrawableManager.fetchDrawableOnThread(item.imageHref, viewHolder.image);
				} else {
					viewHolder.image.setImageDrawable(null);
				}
			}
			return v;
		}
	}
	
	static class ViewHolder {
		TextView title;
		TextView description;
		ImageView image;
	}
	
	public class LazyLoadDrawableManager {
	    private final Map<String, Drawable> drawableMap;
	    private final Object mLock = new Object();
	    
	    public LazyLoadDrawableManager() {
	        drawableMap = new HashMap<String, Drawable>();
	    }

	    public Drawable fetchDrawable(String urlString) {
	        try {
	            InputStream is = fetch(urlString);
	            Drawable drawable = Drawable.createFromStream(is, "src");

	            synchronized (mLock) {
		            if (drawable != null) {
		                drawableMap.put(urlString, drawable);
		                Log.d("a22183", "store in cache drawable url = " + urlString);          
		            } else {
		            }
	            }

	            return drawable;
	        } catch (MalformedURLException e) {
	            return null;
	        } catch (IOException e) {
	            return null;
	        }
	    }

	    public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
	    	Log.d("a22183", "get drawable url = " + urlString);
	    	
	    	synchronized (mLock) {
		        if (drawableMap.containsKey(urlString)) {
		        	Log.d("a22183", "get from cache");
		            imageView.setImageDrawable(drawableMap.get(urlString));
		            return;
		        }
	    	}

	        final Handler handler = new Handler() {
	            @Override
	            public void handleMessage(Message message) {
	                imageView.setImageDrawable((Drawable) message.obj);
	            }
	        };

	        Log.d("a22183", "get from remote");
	        Thread thread = new Thread() {
	            @Override
	            public void run() {
	                Drawable drawable = fetchDrawable(urlString);
	                Message message = handler.obtainMessage(1, drawable);
	                handler.sendMessage(message);
	            }
	        };
	        thread.start();
	    }

	    private InputStream fetch(String urlString) throws MalformedURLException, IOException {
	        DefaultHttpClient httpClient = new DefaultHttpClient();
	        HttpGet request = new HttpGet(urlString);
	        HttpResponse response = httpClient.execute(request);
	        return response.getEntity().getContent();
	    }
	}
}
