package com.lansosdk.videoeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.lansosdk.box.AudioInsertManager;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.BoxMediaInfo;
import com.lansosdk.box.BoxVideoEditor;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.DrawPadVideoRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.LayerShader;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;

/**
 * 此类是对老版本的 DrawPadVideoExecute的一个封装, 并为每个方法增加了详细的使用说明, 以方便您调用具体的方法.
 * 
 * 2017年5月4日12:35:51:
 * 把DrawPadVideoExecute更改名字为DrawPadVideoRunnable
 *  DrawPadVideoRunnable 我们封装成一个异步的线程. 用来处理视频.
 *  实际使用中, 不建议你另外再开线程处理,不建议同时创建多个DrawPadVideoExecute来处理,
 *  因为手机硬件编码器,当前大部分同一时刻只支持一路编码.即使您开多个,处理速度也不会很快.
 *
 */
public class DrawPadVideoExecute {
	
	private DrawPadVideoRunnable  mDrawPad=null;
	/**
	 * 
	 * @param ctx 语境,android的Context
	 * @param srcPath  主视频的路径
	 * @param padwidth DrawPad的的宽度
	 * @param padheight  DrawPad的的高度
	 * @param bitrate 编码视频所希望的码率,比特率.
	 * @param filter  为视频图层增加一个滤镜 如果您要增加多个滤镜,则用{@link #switchFilterList(Layer, ArrayList)}
	 * @param dstPath  视频处理后保存的路径.
	 */
   public DrawPadVideoExecute(Context ctx,String srcPath,int padwidth,int padheight,int bitrate,GPUImageFilter filter,String dstPath) 
   {
	   if(mDrawPad==null){
		   mDrawPad=new DrawPadVideoRunnable(ctx, srcPath, padwidth, padheight, bitrate, filter, dstPath);   
	   }   
   }
   
   public DrawPadVideoExecute(Context ctx,String srcPath,int padwidth,int padheight,GPUImageFilter filter,String dstPath) 
   {
	   if(mDrawPad==null){
		   mDrawPad=new DrawPadVideoRunnable(ctx, srcPath, padwidth, padheight, 0, filter, dstPath);   
	   }   
   }
   /**
    *  Drawpad后台执行, 可以指定开始时间.
    *  因视频编码原理, 会定位到 [指定时间]前面最近的一个IDR刷新帧, 然后解码到[指定时间],画板才开始渲染视频,中间或许有一些延迟后.
    * @param ctx
    * @param srcPath  主视频的完整路径.
    * @param startTimeMs  开始时间. 单位毫秒
    * @param padwidth  画板宽度.
    * @param padheight 画板高度.
    * @param bitrate 画板编码的码率
    * @param filter  为这视频设置一个滤镜, 如果您要增加多个滤镜,则用{@link #switchFilterList(Layer, ArrayList)}
    * @param dstPath  处理后保存的目标文件.
    */
   public DrawPadVideoExecute(Context ctx,String srcPath,long startTimeMs,int padwidth,int padheight,int bitrate,GPUImageFilter filter,String dstPath) 
   {
	   if(mDrawPad==null){
		   mDrawPad=new DrawPadVideoRunnable(ctx, srcPath, padwidth, padheight, bitrate, filter, dstPath);
	   }
   }
   
   public DrawPadVideoExecute(Context ctx,String srcPath,long startTimeMs,int padwidth,int padheight,GPUImageFilter filter,String dstPath) 
   {
	   if(mDrawPad==null){
		   mDrawPad=new DrawPadVideoRunnable(ctx, srcPath, padwidth, padheight,0, filter, dstPath);
	   }
   }
  /**
   * 启动DrawPad,开始执行.
   * 开启成功,返回true, 失败返回false
   */
   public boolean startDrawPad() {
	   if(mDrawPad!=null && mDrawPad.isRunning()==false	){
		   return mDrawPad.startDrawPad();
	   }else{
		   return false;
	   }
   }
   /**
    * 启动DrawPad,开始执行. 可以在开始的时候,暂停录制.从而可以增加一些图层.
   * 开启成功,返回true, 失败返回false
   * 
    * @param pause 是否在开启drawPad后, 暂停录制.
    * @return
    */
   public boolean startDrawPad(boolean pause) {
	   if(mDrawPad!=null && mDrawPad.isRunning()==false	){
		   return mDrawPad.startDrawPad();
	   }else{
		   return false;
	   }
   }
   public void stopDrawPad() {
   	// TODO Auto-generated method stub
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.stopDrawPad();
	   }
   }
  /**
   * 
   *设置是否使用主视频的时间戳为录制视频的时间戳;
   *
   *如果您传递过来的是一个完整的视频, 只是需要在此视频上做一些操作, 
   *操作完成后,时长等于源视频的时长, 则建议使用主视频的时间戳, 如果视频是从中间截取一般开始的
   *
   *此方法,在DrawPad开始前调用.
   */
   public void setUseMainVideoPts(boolean use)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()==false){
		   mDrawPad.setUseMainVideoPts(use);
	   }
   }
   /**
    * 设置画面刷新模式, 当前有两种模式, 视频刷新/自动刷新. 
    * 
    * 如果你处理一个完整的视频,只是处理视频的滤镜/缩放/增加其他图层等等, 建议用 视频刷新模式.
    * 默认不设置,则为视频刷新模式;
    * 
    * 视频刷新 是指: 当这一帧视频解码完成后,才刷新DrawPad;
    * 自动刷新 是指: DrawPad自动根据您设置的帧率来刷新, 不判断视频有没有解码完成;
    * 
    * @param mode
    * @param fps  一秒钟的帧率, 在自动刷新时有效.
    */
   public void setUpdateMode(DrawPadUpdateMode mode, int fps) {
	   if(mDrawPad!=null && mDrawPad.isRunning()==false){
		   mDrawPad.setUpdateMode(mode, fps);
	   }
   }
   /**
	 * DrawPad每执行完一帧画面,会调用这个Listener,返回的timeUs是当前画面的时间戳(微妙),
	 *  可以利用这个时间戳来做一些变化,比如在几秒处缩放, 在几秒处平移等等.从而实现一些动画效果.
	 * @param currentTimeUs  当前DrawPad处理画面的时间戳.,单位微秒.
	 */
	
	public void setDrawPadProgressListener(onDrawPadProgressListener listener){
		if(mDrawPad!=null){
			mDrawPad.setDrawPadProgressListener(listener);
		}
	}
	/**
	 * 方法与   onDrawPadProgressListener不同的地方在于:
	 * 此回调是在DrawPad渲染完一帧后,立即执行这个回调中的代码,不通过Handler传递出去,你可以精确的执行一些下一帧的如何操作.
	 * 故不能在回调 内增加各种UI相关的代码.
	 */
	
	public void setDrawPadThreadProgressListener(onDrawPadThreadProgressListener listener)
	{
		if(mDrawPad!=null){
			mDrawPad.setDrawPadThreadProgressListener(listener);
		}
	}
	/**
	 * DrawPad执行完成后的回调.
	 * @param listener
	 */
	public void setDrawPadCompletedListener(onDrawPadCompletedListener listener){
		if(mDrawPad!=null){
			mDrawPad.setDrawPadCompletedListener(listener);
		}
	}
  /**
   * 把当前图层放到DrawPad的最底部.
   * DrawPad运行后,有效.
   * @param pen
   */
   public void bringToBack(Layer layer)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.bringToBack(layer);
	   }
   }
   /**
    * 
    * @param layer
    */
   public void bringToFront(Layer layer)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.bringToFront(layer);
	   }
   }
   /**
    * 
    * @return
    */
   public VideoLayer getMainVideoLayer()
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		  return  mDrawPad.getMainVideoLayer();
	   }else{
		   return null;
	   }
   }
   /**
    * 在处理中插入一段其他音频,比如笑声,雷声, 各种搞怪声音等. 
    * 注意,这里插入的声音是和视频原有的声音混合后, 形成新的音频,而不是替换原来的声音, 如果您要替换原理的声音,则建议用{@link VideoEditor}中的相关方法来做.
    * 如果原视频没有音频部分,则默认创建一段无声的音频和别的音频混合.
    * 
    * 成功增加后, 会在DrawPad开始运行时,开启一个线程去编解码音频文件, 如果您音频过大,则可能需要一定的时间来处理完成.
    * 此方法在DrawPad开始前调用.
    * 
    * 此方法可以被多次调用, 从而增加多段其他的音频.
    * 
    * @param srcPath  音频的完整路径,当前支持mp3格式和aac格式.
    * 
    * @param startTimeMs 设置从主音频的哪个时间点开始插入.单位毫秒.
    * @param durationMs   把这段声音多长插入进去.
    * @param mainvolume 插入时,主音频音量多大  默认是1.0f, 大于1,0则是放大, 小于则是降低
    * @param volume  插入时,当前音频音量多大  默认是1.0f, 大于1,0则是放大, 小于则是降低
    * @return  插入成功, 返回true, 失败返回false
    */
	 public boolean addSubAudio(String srcPath,long startTimeMs,long durationMs,float mainvolume,float volume) 
	 {
		 if(mDrawPad!=null && mDrawPad.isRunning()==false){
			return mDrawPad.addSubAudio(srcPath, startTimeMs, durationMs, mainvolume, volume);
		 }else{
			 return false;
		 }
	 }
	 /**
	  * 增加图片图层.
	  * @param bmp
	  * @return
	  */
   public BitmapLayer  addBitmapLayer(Bitmap bmp)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addBitmapLayer(bmp);
	   }else{
		   return null;
	   }
   }
   /**
    * 增加数据图层,  DataLayer有一个{@link DataLayer#pushFrameToTexture(java.nio.IntBuffer)}可以把数据或图片传递到DrawPad中. 
    * 
    * @param dataWidth
    * @param dataHeight
    * @return
    */
   public DataLayer  addDataLayer(int dataWidth,int dataHeight)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		 return  mDrawPad.addDataLayer(dataWidth, dataHeight);
	   }else{
		   return null;
	   }
   }
   
   /**
    *  获取一个VideoLayer并返回, 在DrawPadVideoExecute中获取,则等于是在主视频中另外叠加上一个视频,并可以设置其滤镜效果.
    * @param videoPath  叠加视频的完整路径
    * @param vWidth 叠加视频的完整路径
    * @param vHeight 该视频的高度.
    * @param filter 对即将获取到的VideoLayer设置的滤镜对象.
    * @return  返回获取到的VideoLayer对象.
    */
   public VideoLayer addVideoLayer(String videoPath,int vWidth,int vHeight,GPUImageFilter filter)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		  return mDrawPad.addVideoLayer(videoPath, vWidth, vHeight, filter);
	   }else{
		   return null;
	   }
   }
   /**
    * 当mv在解码的时候, 是否异步执行; 
    * 如果异步执行,则MV解码可能没有那么快,从而MV画面会有慢动作的现象.
    * 如果同步执行,则视频处理会等待MV解码完成, 从而处理速度会慢一些,但MV在播放时,是正常的. 
    *  
    * @param srcPath  MV的彩色视频
    * @param maskPath  MV的黑白视频.
    * @param isAsync   是否异步执行.
    * @return
    */
   public MVLayer addMVLayer(String srcPath,String maskPath,boolean isAsync)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addMVLayer(srcPath, maskPath);
	   }else{
		   return null;
	   }
   }
   /**
    * 增加一个MV图层.
    * @param srcPath
    * @param maskPath
    * @return
    */
   public MVLayer addMVLayer(String srcPath,String maskPath)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addMVLayer(srcPath, maskPath);
	   }else{
		   return null;
	   }
   }
   /**
    * 增加gif图层
    * @param gifPath
    * @return
    */
   public GifLayer addGifLayer(String gifPath)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addGifLayer(gifPath);
	   }else{
		   return null;
	   }
   }
   /**
    * 增加gif图层
    * resId 来自apk中drawable文件夹下的各种资源文件, 我们会在GifLayer中拷贝这个资源到默认文件夹下面, 然后作为一个普通的gif文件来做处理,使用完后, 会在Giflayer
	 * 图层释放的时候, 删除.
    * @param resId
    * @return
    */
   public GifLayer addGifLayer(int resId)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addGifLayer(resId);
	   }else{
		   return null;
	   }
   }
   /**
    * 增加一个Canvas图层, 可以用Android系统的Canvas来绘制一些文字线条,颜色等. 可参考我们的的 "花心形"的举例
    * 因为Android的View机制是无法在非UI线程中使用View的. 但可以使用Canvas这个类工作在其他线程.
    * 因此我们设计了CanvasLayer,从而可以用Canvas来做各种Draw文字, 线条,图案等.
    * @return
    */
   public CanvasLayer addCanvasLayer() 
	{
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.addCanvasLayer();
	   }else{
		   return null;
	   }
	}
   /**
    * 删除一个图层.
    * @param layer
    */
   public void removeLayer(Layer layer)
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.removeLayer(layer);
	   }
   }
   /**
    * 已废弃.请用pauseRecord();
    */
   @Deprecated
   public void pauseRecordDrawPad()
   {
	   pauseRecord();
   }
   /**
    * 已废弃,请用resumeRecord();
    */
   @Deprecated
   public void resumeRecordDrawPad()
   {
	   resumeRecord();
   }
   /**
    * 暂停录制,
    * 使用在 : 开始DrawPad后, 需要暂停录制, 来增加一些图层, 然后恢复录制的场合.
    */
   public void pauseRecord()
   {
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.pauseRecordDrawPad();
	   }
   }
   /**
    * 恢复录制.
    */
   public void resumeRecord(){
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   mDrawPad.resumeRecordDrawPad();
	   }
   }
   /**
    * 是否在录制.
    * @return
    */
   public boolean isRecording(){
	   if(mDrawPad!=null && mDrawPad.isRunning()){
		   return mDrawPad.isRecording();
	   }else{
		   return false;
	   }
   }
     /**
      * DrawPad是否在运行
      * @return
      */
	 public boolean isRunning()
	 {
		 if(mDrawPad!=null){
			 return mDrawPad.isRunning();
		 }else{
			 return false;
		 }
	 }
	 /**
	  * 切换滤镜
	  * 为一个图层切换多个滤镜. 即一个滤镜处理完后的输出, 作为下一个滤镜的输入.
	  * 
	  * filter的列表, 是先add进去,最新渲染, 把第一个渲染的结果传递给第二个,第二个传递给第三个,以此类推.
	  * 
	  * 注意: 这里内部会在切换的时候, 会销毁 之前的列表中的所有滤镜对象, 然后重新增加, 故您不可以把同一个滤镜对象再次放到进来,
	  * 您如果还想使用之前的滤镜,则应该重新创建一个对象.
	  * 
	  * @param layer
	  * @param filters
	  */
	 public void switchFilterList(Layer layer, ArrayList<GPUImageFilter> filters)
	 {
		 if(mDrawPad!=null && mDrawPad.isRunning()){
			 mDrawPad.switchFilterList(layer, filters);
		 }
	 }
	 /**
	  * 释放DrawPad,方法等同于 {@link #stopDrawPad()}
	  * 只是为了代码标准化而做.
	  */
	 public void releaseDrawPad() {
		   	// TODO Auto-generated method stub
		 if(mDrawPad!=null && mDrawPad.isRunning()){
			 mDrawPad.releaseDrawPad();
		 }
		 mDrawPad=null;
	 }
	   /**
	    * 停止DrawPad, 并释放资源.如果想再次开始,需要重新new, 然后start.
	    * 
	    * 注意:这里阻塞执行, 只有等待opengl线程执行退出完成后,方返回.
	    * 方法等同于 {@link #stopDrawPad()}
	  * 只是为了代码标准化而做.
	    */
	   public void release()
	   {
		   if(mDrawPad!=null && mDrawPad.isRunning()){
			   mDrawPad.release();
		   }
		   mDrawPad=null;
	   }
	   
	   /**
	    * 是否在开始运行DrawPad的时候,检查您设置的码率和分辨率是否正常.
	    * 
	    * 默认是检查, 如果您清楚码率大小的设置,请调用此方法,不再检查.
	    *  
	    */
	   public void setNotCheckBitRate()
	   {
		   if(mDrawPad!=null && mDrawPad.isRunning()==false){
			   mDrawPad.setNotCheckBitRate();
		   }
	   }
	   /**
	    * 是否在开始运行DrawPad的时候, 检查您设置的DrawPad宽高是否是16的倍数.
	    * 默认是检查.
	    */
	   public void setNotCheckDrawPadSize()
	   {
		   if(mDrawPad!=null && mDrawPad.isRunning()==false){
			   mDrawPad.setNotCheckDrawPadSize();
		   }
	   }
}
