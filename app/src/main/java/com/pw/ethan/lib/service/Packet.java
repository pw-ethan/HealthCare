package com.pw.ethan.lib.service;


import com.pw.ethan.lib.util.AtomicIntegerUtil;

/**
 * 
 * @author Administrator
 *
 */
public class Packet {
	
	private int id= AtomicIntegerUtil.getIncrementID();
	private byte[] data;
	
	public int getId() {
		return id;
	}

	public void pack(String txt)
	{
		data=txt.getBytes();
	}
	
	public byte[] getPacket()
	{
		return data;
	}
}
