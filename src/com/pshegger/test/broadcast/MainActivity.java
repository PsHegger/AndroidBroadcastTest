package com.pshegger.test.broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	TextView tvMyIP, tvServerIP;
	Button btnClient, btnServer;
	
	Thread serverThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tvMyIP = (TextView) findViewById(R.id.tvMyIP);
		tvServerIP = (TextView) findViewById(R.id.tvServerIP);
		btnServer = (Button) findViewById(R.id.btnServer);
		btnClient = (Button) findViewById(R.id.btnClient);
		
		WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		
		final String myIP = Formatter.formatIpAddress(ip);
		
		tvMyIP.setText(myIP);
		
		btnServer.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				serverThread = new Thread(DiscoveryThread.getInstance());
				serverThread.start();
				btnServer.setEnabled(false);
			}
		});
		
		btnClient.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new DiscoveryTask().execute(myIP);
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		if (serverThread != null) {
			serverThread.interrupt();
			serverThread = null;
		}
	}
	
	private class DiscoveryTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String myIP = params[0];
			String broadcastIP = myIP.substring(0, myIP.lastIndexOf(".")+1)+"255";
			
			DatagramSocket c;
			try {
				c = new DatagramSocket();
				c.setBroadcast(true);
				
				byte[] sendData = "DISCOVER_FUIFSERVER_REQUEST".getBytes();
				
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(broadcastIP), 8888);
					c.send(sendPacket);
				}catch (Exception e) {}
				
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();
					
					if (networkInterface.isLoopback() || !networkInterface.isUp()) {
						continue;
					}
					
					for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
						InetAddress broadcast = interfaceAddress.getBroadcast();
						
						if (broadcast == null) {
							continue;
						}
						
						try {
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
							c.send(sendPacket);
						}catch (Exception e) {}
					}
				}
				
				
				byte[] recvBuff = new byte[15000];
				DatagramPacket receivePacket = new DatagramPacket(recvBuff, recvBuff.length);
				c.receive(receivePacket);
				
				String message = new String(receivePacket.getData()).trim();
				if (message.equals("DISCOVER_FUIFSERVER_RESPONSE")) {
					return receivePacket.getAddress().getHostAddress();
				}
				
				c.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			

			tvServerIP.setText(result==null?"Unknown":result);
		}
		
	}
}
