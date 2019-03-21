package com.woodoo.testkiev.utils;

import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class MyException implements Thread.UncaughtExceptionHandler {
	

    Thread.UncaughtExceptionHandler oldHandler;
    private final Context myContext;
    
    public MyException(Context context) {
        myContext = context;
        oldHandler = Thread.getDefaultUncaughtExceptionHandler(); // save android ExceptionHandler
    }

    

    
 // save android ExceptionHandler
    //public MyException() {        oldHandler = Thread.getDefaultUncaughtExceptionHandler();     }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
    	
    	//Application.AnalyticsUtils.getInstance(MainApplication.this).dispatch();
    	
    	 Writer result = new StringWriter();
         PrintWriter printWriter = new PrintWriter(result);
         throwable.printStackTrace(printWriter);
         final String stacktrace = result.toString();
         printWriter.close();
         //Log.d("mylog","stacktrace-"+stacktrace);
    	
    	
        //Log.d("mylog", "Something wrong happened! - "+thread.toString()+" - "+throwable.toString());
    	
        new Thread(new Runnable() {
    		public void run() {
    			//Log.d("mylog", "start send start");
				URL url;
				HttpURLConnection connection = null;
				try {
					//Create connection
					String urlParameters =
							"thread=" + URLEncoder.encode(thread.toString(), "UTF-8") +
							"&os=" + URLEncoder.encode("andorid "+ Build.VERSION.RELEASE, "UTF-8")+
							"&pakage=" + URLEncoder.encode(myContext.getPackageName(), "UTF-8")+
							"&version=" + URLEncoder.encode(myContext.getPackageManager().getPackageInfo(myContext.getPackageName(), 0).versionName, "UTF-8")+
							"&stacktrace=" + URLEncoder.encode(stacktrace, "UTF-8")+
							"&devicename=" + URLEncoder.encode(getDeviceName(), "UTF-8");

					url = new URL("http://www.gbsoft.pw/android/crashlog/index.php");
					connection = (HttpURLConnection)url.openConnection();
					connection.setRequestMethod("POST");
					connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

					connection.setRequestProperty("Content-Length", "" +Integer.toString(urlParameters.getBytes().length));
					connection.setRequestProperty("Content-Language", "en-US");

					connection.setUseCaches (false);
					connection.setDoInput(true);
					connection.setDoOutput(true);

					//Send request
					DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
					wr.writeBytes (urlParameters);
					wr.flush ();
					wr.close ();

					//Get Response
					InputStream is = connection.getInputStream();
					BufferedReader rd = new BufferedReader(new InputStreamReader(is));
					String line;
					StringBuffer response = new StringBuffer();
					while((line = rd.readLine()) != null) {
						response.append(line);
						//response.append('\r');
					}
					rd.close();

				} catch (Exception e) {
					e.printStackTrace();

				} finally {
					if(connection != null) {
						connection.disconnect();
					}
				}

    		}

    	}).start();
        
        
        if(oldHandler != null) 
            oldHandler.uncaughtException(thread, throwable); 
    }
    
    public String getDeviceName() {
		  String manufacturer = Build.MANUFACTURER;
		  String model = Build.MODEL;
		  if (model.startsWith(manufacturer)) {
		    return model;
		  } else {
		    return manufacturer + " " + model;
		  }
		}

}
