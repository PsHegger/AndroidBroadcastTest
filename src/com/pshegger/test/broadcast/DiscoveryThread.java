package com.pshegger.test.broadcast;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class DiscoveryThread implements Runnable {
	DatagramSocket socket;
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
			socket.setBroadcast(true);
			
			while (true) {
				System.out.println(getClass().getName() + ">>>Ready to receive broadcast packages!");
				
				byte[] recvBuff = new byte[15000];
				DatagramPacket packet = new DatagramPacket(recvBuff, recvBuff.length);
				socket.receive(packet);
				
				System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
				System.out.println(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()));
				
				String message = new String(packet.getData()).trim();
				if (message.equals("DISCOVER_FUIFSERVER_REQUEST")) {
					byte[] sendData = "DISCOVER_FUIFSERVER_RESPONSE".getBytes();
					
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
					socket.send(sendPacket);
					
					System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static DiscoveryThread getInstance() {
		return DiscoveryThreadHolder.INSTANCE;
	}
	
	private static class DiscoveryThreadHolder {
		private static DiscoveryThread INSTANCE = new DiscoveryThread();
	}
}
