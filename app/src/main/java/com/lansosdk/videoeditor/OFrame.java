/**
 * 杭州蓝松科技, 专业的视频开发团队
 * 
 * www.lansongtech.com
 * 
 * 此代码为开源给客户使用，请勿传递给第三方。 谢谢。
 */
package com.lansosdk.videoeditor;

public class OFrame {
	long ts;  //当前录制的时间戳, 视频:等于当前时间和开始时间的差值,单位MS.
	byte[] data;

	public OFrame(long ts, byte[] data) {
		super();
		this.ts = ts;
		this.data = data;
	}
}
