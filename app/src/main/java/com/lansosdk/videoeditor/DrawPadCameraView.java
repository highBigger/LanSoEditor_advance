package com.lansosdk.videoeditor;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.TableLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.example.advanceDemo.view.TextureRenderView;
import com.lansosdk.box.AudioLine;
import com.lansosdk.box.AudioInsertManager;
import com.lansosdk.box.BitmapLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.CameraLayer;
import com.lansosdk.box.CanvasLayer;
import com.lansosdk.box.DrawPadCameraRunnable;
import com.lansosdk.box.GifLayer;
import com.lansosdk.box.DataLayer;
import com.lansosdk.box.MVLayer;
import com.lansosdk.box.Layer;
import com.lansosdk.box.DrawPadUpdateMode;
import com.lansosdk.box.DrawPadViewRender;
import com.lansosdk.box.TwoVideoLayer;
import com.lansosdk.box.VideoLayer;
import com.lansosdk.box.ViewLayer;
import com.lansosdk.box.YUVLayer;
import com.lansosdk.box.onDrawPadCompletedListener;
import com.lansosdk.box.onDrawPadProgressListener;
import com.lansosdk.box.onDrawPadSizeChangedListener;
import com.lansosdk.box.onDrawPadSnapShotListener;
import com.lansosdk.box.onDrawPadThreadProgressListener;



/**
 *  处理预览和实时保存的View, 继承自FrameLayout.
 *  
 *   适用在增加到UI界面中, 一边预览,一边实时保存的场合.
 *
 * 此代码由我们来维护, 但您可以放到您项目的任意地方. 以方便您的使用.
 * 
 */
public class DrawPadCameraView extends FrameLayout {
	
	private static final String TAG="DrawPadView";
	private static final boolean VERBOSE = false;  
  
    private int mVideoRotationDegree;

    private TextureRenderView mTextureRenderView;
 	
    private DrawPadCameraRunnable  renderer;
 	
 	private SurfaceTexture mSurfaceTexture=null;
 	
 	
 	private int encWidth,encHeight,encFrameRate;
 	private int encBitRate=0;
 	/**
 	 *  经过宽度对齐到手机的边缘后, 缩放后的宽高,作为drawpad(画板)的宽高. 
 	 */
 	private int drawPadWidth,drawPadHeight; 
 	/**
 	 * 摄像头是否是前置摄像头
 	 */
 	private boolean isFrontCam=false;
 	/**
 	 * 是否在相机录制过程中,使用到美颜滤镜.
 	 */
 	private boolean maybeBeauful=true;
 	/**
 	 * 初始化CameraLayer的时候, 是否需要设置滤镜. 当然您也可以在后面实时切换为别的滤镜.
 	 */
 	private ArrayList<GPUImageFilter>  initFilters=null;
 	
 	  
    public DrawPadCameraView(Context context) {
        super(context);
        initVideoView(context);
    }

    public DrawPadCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public DrawPadCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawPadCameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    
    private void initVideoView(Context context) {
        setTextureView();

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }
    /**
     * 画面显示模式：不裁剪直接和父view匹配， 这样如果画面超出父view的尺寸，将会只显示画面的
     * 一部分，您可以使用这个来平铺画面，通过手动拖拽的形式来截取画面的一部分。类似画面区域裁剪的功能。
     */
    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    /**
     * 画面显示模式:裁剪和父view匹配, 当画面超过父view大小时,不会缩放,会只显示画面的一部分.
     * 超出部分不予显示.
     */
    static final int AR_ASPECT_FILL_PARENT = 1; // may clip
    /**
     * 画面显示模式: 自适应大小.当小于画面尺寸时,自动显示.当大于尺寸时,缩放显示.
     */
    static final int AR_ASPECT_WRAP_CONTENT = 2;
    /**
     * 画面显示模式:和父view的尺寸对其.完全填充满父view的尺寸
     */
    static final int AR_MATCH_PARENT = 3;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照16:9的形式显示. 大部分的网络推荐用这种方式显示.
     */
    static final int AR_16_9_FIT_PARENT = 4;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照4:3的形式显示.
     */
    static final int AR_4_3_FIT_PARENT = 5;

    
    private void setTextureView() {
    	mTextureRenderView = new TextureRenderView(getContext());
    	mTextureRenderView.setSurfaceTextureListener(new SurfaceCallback());
    
    	mTextureRenderView.setDispalyRatio(AR_ASPECT_FIT_PARENT);
        
    	View renderUIView = mTextureRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mTextureRenderView.setVideoRotation(mVideoRotationDegree);
    }
    private String encodeOutput=null; //编码输出路径
    /**
     * 获取当前View的 宽度
     * @return
     */
    public int getViewWidth(){
    	return drawPadWidth;
    }
    /**
     * 获得当前View的高度.
     * @return
     */
    public int getViewHeight(){
    	return drawPadHeight;
    }
    
    public int getDrawPadWidth(){
    	return drawPadWidth;
    }
    /**
     * 获得当前View的高度.
     * @return
     */
    public int getDrawPadHeight(){
    	return drawPadHeight;
    }
  
    public interface onViewAvailable {	    
        void viewAvailable(DrawPadCameraView v);
    }
	private onViewAvailable mViewAvailable=null;
	  /**
     * 此回调仅仅是作为演示: 当跳入到别的Activity后的返回时,会再次预览当前画面的功能.
     * 你完全可以重新按照你的界面需求来修改这个DrawPadView类.
     *
     */
	public void setOnViewAvailable(onViewAvailable listener)
	{
		mViewAvailable=listener;
	}
	
	
	private class SurfaceCallback implements SurfaceTextureListener {
    			
				/**
				 *  Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
				 *   当画面呈现出来的时候, 会调用这个回调.
				 *   
				 *  当Activity跳入到别的界面后,这时会调用{@link #onSurfaceTextureDestroyed(SurfaceTexture)} 销毁这个Texture,
				 *  如果您想在再次返回到当前Activity时,再次显示预览画面, 可以在这个方法里重新设置一遍DrawPad,并再次startDrawPad 
				 */
				
    	        @Override
    	        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    	            mSurfaceTexture = surface;
    	            drawPadHeight=height;
    	            drawPadWidth=width;
    	            if(mViewAvailable!=null){
    	            	mViewAvailable.viewAvailable(null);
    	            }	
    	        }
    	        
    	        /**
    	         * Invoked when the {@link SurfaceTexture}'s buffers size changed.
    	         * 当创建的TextureView的大小改变后, 会调用回调.
    	         * 
    	         * 当您本来设置的大小是480x480,而DrawPad会自动的缩放到父view的宽度时,会调用这个回调,提示大小已经改变, 这时您可以开始startDrawPad
    	         * 如果你设置的大小更好等于当前Texture的大小,则不会调用这个, 详细的注释见 {@link DrawPadCameraView#startDrawPad(onDrawPadProgressListener, onDrawPadCompletedListener)}
    	         */
    	        @Override
    	        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    	            mSurfaceTexture = surface;
    	            drawPadHeight=height;
    	            drawPadWidth=width;
    	            
    	            
	        			if(mSizeChangedCB!=null)
	        				mSizeChangedCB.onSizeChanged(width, height);
    	        }
    	
    	        /**
    	         *  Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
    	         *  
    	         *  当您跳入到别的Activity的时候, 会调用这个,销毁当前Texture;
    	         *  
    	         */
    	        @Override
    	        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    	            mSurfaceTexture = null;
    	            drawPadHeight=0;
    	            drawPadWidth=0;
    	            
    	            stopDrawPad();  //可以在这里增加以下. 这样当Texture销毁的时候, 停止DrawPad
    	            
    	            return false;
    	        }
    	
    	        /**
    	         * 
    	         * Invoked when the specified {@link SurfaceTexture} is updated through
    	         * {@link SurfaceTexture#updateTexImage()}.
    	         * 
    	         *每帧如果更新了, 则会调用这个!!!! 
    	         */
    	        @Override
    	        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    	        }
    }
	/**
	 * 
	 * 
	 * @param front  
	 * @param filters  
	 */
	/**
	 * 设置CameraLayer是否使用前置相机, 是否在刚开始增加滤镜.
	 * @param front 是否是前置
	 * @param filters  是否增加滤镜.
	 * @param beauful   是否在录制的过程中,有没有可能用到美颜滤镜, 如果有的话, 则这里设置为true, 完全不可能用到,则设置为false;
	 */
	public void setCameraParam(boolean front,ArrayList<GPUImageFilter> filters, boolean beauful)
	{
		isFrontCam=front;
		initFilters=filters;
		maybeBeauful=beauful;
	}
		/**
		 * 
		 * 
		 *  如果实时保存的宽高和原的宽高不成比例,则会先等比例缩放原,然后在多出的部分出增加黑边的形式呈现,比如原是16:9,设置的宽高是480x480,则会先把原按照宽度进行16:9的比例缩放.
		 *  在缩放后,在的上下增加黑边的形式来实现480x480, 从而不会让变形.
		 *  
		 *  如果在拍摄时有角度, 比如手机相机拍照,会有90度或270, 则会自动的识别拍摄的角度,并在缩放时,自动判断应该左右加黑边还是上下加黑边.
		 *  
		 *  因有缩放的特性, 您可以直接向DrawPad中投入一个,然后把宽高比设置一致,这样可实现一个压缩的功能;您也可以把宽高比设置一下,这样可实现加黑边的压缩功能.
		 *  或者您完全不进行缩放, 仅仅想把码率减低一下, 也可以把其他参数设置为和源一致, 仅仅调试encBr这个参数,来实现压缩的功能.
		 *  
		 * @param encW  录制宽度
		 * @param encH  录制高度
		 * @param encBr 录制bitrate,
		 * @param encFr 录制帧率
		 * @param outPath  录制 的保存路径. 注意:这个路径在分段录制功能时,无效.即调用 {@link #segmentStart(String)}时.
		 */
	public void setRealEncodeEnable(int encW,int encH,int encBr,int encFr,String outPath)
	    {
	    	if(encW>0 && encH>0 && encBr>0 && encFr>0){
	    			encWidth=encW;
			        encHeight=encH;
			        encBitRate=encBr;
			        encFrameRate=encFr;
			        encodeOutput=outPath;
	    	}else{
	    		Log.w(TAG,"enable real encode is error");
	    	}
	    }
	public void setRealEncodeEnable(int encW,int encH,int encFr,String outPath)
    {
    	if(encW>0 && encH>0 && encFr>0){
    			encWidth=encW;
		        encHeight=encH;
		        encBitRate=0;
		        encFrameRate=encFr;
		        encodeOutput=outPath;
    	}else{
    		Log.w(TAG,"enable real encode is error");
    	}
    }
	
	   private onDrawPadSizeChangedListener mSizeChangedCB=null; 
	   /**
	    * 设置当前DrawPad的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
	    * 
	    * 如果在父view中已经预设好了希望的宽高,则可以不调用这个方法,直接 {@link #startDrawPad(onDrawPadProgressListener, onDrawPadCompletedListener)}
	    * 可以通过 {@link #getViewHeight()} 和 {@link #getViewWidth()}来得到当前view的宽度和高度.
	    * 
	    * 
	    * 注意: 这里的宽度和高度,会根据手机屏幕的宽度来做调整,默认是宽度对齐到手机的左右两边, 然后调整高度, 把调整后的高度作为DrawPad渲染线程的真正宽度和高度.
	    * 注意: 此方法需要在 {@link #startDrawPad(onDrawPadProgressListener, onDrawPadCompletedListener)} 前调用.
	    * 比如设置的宽度和高度是480,480, 而父view的宽度是等于手机分辨率是1080x1920,则DrawPad默认对齐到手机宽度1080,然后把高度也按照比例缩放到1080.
	    * 
	    * @param width  DrawPad宽度
	    * @param height DrawPad高度 
	    * @param cb   设置好后的回调, 注意:如果预设值的宽度和高度经过调整后 已经和父view的宽度和高度一致,则不会触发此回调(当然如果已经是希望的宽高,您也不需要调用此方法).
	    */
	public void setDrawPadSize(int width,int height,onDrawPadSizeChangedListener cb){
		
		if (width != 0 && height != 0 && cb!=null) {
			float setAcpect=(float)width/(float)height;
			
			float setViewacpect=(float)drawPadWidth/(float)drawPadHeight;
			
			Log.i(TAG,"setAcpect="+setAcpect+" setViewacpect:"+setViewacpect+
					"set width:"+width+"x"+height+" view width:"+drawPadWidth+"x"+drawPadHeight);
			
			
		    if(setAcpect==setViewacpect){  //如果比例已经相等,不需要再调整,则直接显示.
		    	if(cb!=null)
		    		cb.onSizeChanged(width, height);
		    	
		    }else if (mTextureRenderView != null) {
            	
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1,1);
                mSizeChangedCB=cb;
            }
            requestLayout();
        }
	}
	/**
	 * DrawPad每执行完一帧画面,会调用这个Listener,返回的timeUs是当前画面的时间戳(微妙),
	 *  可以利用这个时间戳来做一些变化,比如在几秒处缩放, 在几秒处平移等等.从而实现一些动画效果.
	 * @param currentTimeUs  当前DrawPad处理画面的时间戳.,单位微秒.
	 */
	private onDrawPadProgressListener drawpadProgressListener=null;
	
	public void setOnDrawPadProgressListener(onDrawPadProgressListener listener){
		if(renderer!=null){
			renderer.setDrawPadProgressListener(listener);
		}
		drawpadProgressListener=listener;
	}
	/**
	 * 方法与   onDrawPadProgressListener不同的地方在于:
	 * 此回调是在DrawPad渲染完一帧后,立即执行这个回调中的代码,不通过Handler传递出去,你可以精确的执行一些下一帧的如何操作.
	 * 故不能在回调 内增加各种UI相关的代码.
	 */
	private onDrawPadThreadProgressListener drawPadThreadProgressListener=null;
	
	public void setOnDrawPadThreadProgressListener(onDrawPadThreadProgressListener listener)
	{
		if(renderer!=null){
			renderer.setDrawPadThreadProgressListener(listener);
		}
		drawPadThreadProgressListener=listener;
	}
	
	private onDrawPadSnapShotListener drawpadSnapShotListener=null;
	/**
	 * 设置 获取当前DrawPad这一帧的画面的监听, 
	 *  设置截图监听,当截图完成后, 返回当前图片的btimap格式的图片.
	 *  此方法工作在主线程. 请不要在此方法里做图片的处理,以免造成拥堵;  建议获取到bitmap后,放入到一个链表中,在外面或另开一个线程处理. 
	 */
	public void setOnDrawPadSnapShotListener(onDrawPadSnapShotListener listener)
	{
		if(renderer!=null){
			renderer.setDrawpadSnapShotListener(listener);
		}
		drawpadSnapShotListener=listener;
	}
	
	/**
	 * 触发一下截取当前DrawPad中的内容.
	 * 触发后, 会在DrawPad内部设置一个标志位,DrawPad线程会检测到这标志位后, 截取DrawPad, 并通过onDrawPadSnapShotListener监听反馈给您.
	 * 请不要多次或每一帧都截取DrawPad, 以免操作DrawPad处理过慢.
	 * 
	 * 此方法,仅在前台工作时有效.
	 * (注意:截取的仅仅是各种图层的内容, 不会截取DrawPad的黑色背景)
	 */
	public void toggleSnatShot()
	{
		if(drawpadSnapShotListener!=null && renderer!=null && renderer.isRunning()){
			renderer.toggleSnapShot();
		}else{
			Log.e(TAG,"toggle snap shot failed!!!");
		}
	}
	private onDrawPadCompletedListener drawpadCompletedListener=null;
	public void setOnDrawPadCompletedListener(onDrawPadCompletedListener listener){
		if(renderer!=null){
			renderer.setDrawPadCompletedListener(listener);
		}
		drawpadCompletedListener=listener;
	}

	/**
	 * 此方法仅仅使用在录制的同时,您也设置了录制音频
	 * 
	 * @return  在录制结束后, 返回录制mic的音频文件路径, 
	 */
	public String getMicPath()
	{
		if(renderer!=null){
			return renderer.getAudioRecordPath();
		}else{
			return null;
		}
	}
	/**
	 * 开始DrawPad的渲染线程, 阻塞执行, 直到DrawPad真正开始执行后才退出当前方法.
	 * 
	 * 可以先通过 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}来设置宽高,然后在回调中执行此方法.
	 * 如果您已经在xml中固定了view的宽高,则可以直接调用这里, 无需再设置DrawPadSize
	 * @return
	 */
	public boolean startDrawPad()
	{
		return startDrawPad(isPauseRecordDrawPad);
	}
	/**
	 * 开始DrawPad的渲染线程, 阻塞执行, 直到DrawPad真正开始执行后才退出当前方法.
	 * 如果DrawPad设置了录制功能, 这里可以在开启后暂停录制. 适用在当您开启录制后, 需要先增加一个图层的场合后,在让它开始录制的场合, 可用resume
	 * 	 * {@link #resumeDrawPadRecord()} 来回复录制.
	 * 
	 * @param pauseRecord  如果DrawPad设置了录制功能, 这里可以在开启后暂停录制. 适用在当您开启录制后, 需要先增加一个图层的场合后,在让它开始录制的场合, 可用resume
	 * 	  {@link #resumeDrawPadRecord()} 来回复录制.
	 * @return
	 */
	public boolean startDrawPad(boolean pauseRecord)
	{
		boolean ret=false;
	 
		 if( mSurfaceTexture!=null && renderer==null && drawPadWidth>0 &&drawPadHeight>0)
         {
			 	renderer=new DrawPadCameraRunnable(getContext(), drawPadWidth, drawPadHeight);  //<----从这里去建立DrawPad线程.
				renderer.setCameraParam(isFrontCam,initFilters,maybeBeauful);
	 			if(renderer!=null){
	 				renderer.setDisplaySurface(mTextureRenderView,new Surface(mSurfaceTexture));
	 				
	 				if(isCheckPadSize){
	 					encWidth=makeQuad(encWidth);
	 					encHeight=makeQuad(encHeight);
	 				}
	 				if(isCheckBitRate || encBitRate==0){
	 					encBitRate=checkSuggestBitRate(encHeight * encWidth, encBitRate);
	 				}
	 				renderer.setEncoderEnable(encWidth,encHeight,encBitRate,encFrameRate,encodeOutput);
	 				
	 				 //设置DrawPad处理的进度监听, 回传的currentTimeUs单位是微秒.
	 				renderer.setDrawpadSnapShotListener(drawpadSnapShotListener);
	 				renderer.setDrawPadProgressListener(drawpadProgressListener);
	 				renderer.setDrawPadCompletedListener(drawpadCompletedListener);
	 				renderer.setDrawPadThreadProgressListener(drawPadThreadProgressListener);
	 				
	 				if(isRecordMic){
	 					renderer.setRecordMic(isRecordMic);	
	 				}else if(isRecordExtPcm){
	 					renderer.setRecordExtraPcm(isRecordExtPcm, pcmChannels,pcmSampleRate,pcmBitRate);
	 				}
	 				
	 				if(pauseRecord || isPauseRecordDrawPad){
	 					renderer.pauseRecordDrawPad();	
	 				}
	 				if(isPauseRefreshDrawPad){
	 					renderer.pauseRefreshDrawPad();
	 				}
	 				ret=renderer.startDrawPad();
	 				
	 				
	 				if(ret==false){  //用中文注释.
	 					Log.e(TAG,"开启 DrawPad 失败, 或许是您之前的DrawPad没有Stop, 或者传递进去的surface对象已经被系统Destory!!," +
	 							"请检测您 的代码或参考本文件中的SurfaceCallback 这个类中的注释;\n" +
	 							"如果您是从一个Activity返回到当前Activity,希望再次预览,可以看下我们setOnViewAvailable, 在PictureSetRealtimeActivity.java代码里有说明.");
	 				}
	 			}
         }else{
        	 Log.e(TAG,"开启 DrawPad 失败, 您设置的宽度和高度是:"+drawPadWidth+" x "+drawPadHeight+" 如果您是从一个Activity返回到当前Activity,希望再次预览, 可以看下我们setOnViewAvailable, 在PictureSetRealtimeActivity.java代码里有说明.");
         }
		 return ret;
	}
	/**
	 * 暂停DrawPad的画面刷新
	 * 在一些场景里,您需要开启DrawPad后,暂停下, 然后增加各种Layer后,安排好各种事宜后,再让其画面更新,
	 * 则用到这个方法.
	 */
	public void pauseDrawPad()
	{
		if(renderer!=null){
			renderer.pauseRefreshDrawPad();
		}
		isPauseRefreshDrawPad=true;
	}
	/**
	 * 恢复之前暂停的DrawPad,让其继续画面刷新. 与{@link #pauseDrawPad()}配对使用.
	 */
	public void resumeDrawPad()
	{
		if(renderer!=null){
			renderer.resumeRefreshDrawPad();
		}
		isPauseRefreshDrawPad=false;
	}
	/**
	 * 暂停drawpad的录制,这个方法使用在暂停录制后, 在当前画面做其他的一些操作,
	 * 不可用来暂停后跳入到别的Activity中.
	 */
	public void pauseDrawPadRecord()
	{
		if(renderer!=null){
			renderer.pauseRecordDrawPad();
		}
		isPauseRecordDrawPad=true;
	}
	public void resumeDrawPadRecord()
	{
		if(renderer!=null){
			renderer.resumeRecordDrawPad();
		}
		isPauseRecordDrawPad=false;
	}
	/**
	 * 是否也录制mic的声音.
	 */
	private boolean isRecordMic=false;
	private boolean isPauseRefreshDrawPad=false;
	private boolean isPauseRecordDrawPad=false;

	 private boolean  isRecordExtPcm=false;  //是否使用外面的pcm输入.
	 private int pcmSampleRate=44100;
	 private int pcmBitRate=64000;
	 private int pcmChannels=2; //音频格式. 音频默认是双通道.
	 

	/**
	 * 是否在CameraLayer录制的同时, 录制mic的声音.  在drawpad开始前调用. 
	 * 
	 * 此方法仅仅使用在录像的时候, 同时录制Mic的场合.录制的采样默认是44100, 码率64000, 编码为aac格式.
	 * 录制的同时, 编码以音频的时间戳为参考.
	 * 可通过 {@link #stopDrawPad2()}停止,停止后返回mic录制的音频文件 m4a格式的文件, 
	 * @param record
	 */
	public void setRecordMic(boolean record)
	{
		if(renderer!=null && renderer.isRecording()){
			Log.e(TAG,"DrawPad is running. set Mic Error!");
		}else{
			isRecordMic=record;
		}
	}
	/**
	 * 是否在录制画面的同时,录制外面的push进去的音频数据 .
	 * 
	 * 适用在需要实时录制的, 如果您仅仅是对增加背景音乐等, 可以使用 {@link VideoEditor#executeVideoMergeAudio(String, String, String)}
	 * 来做处理.
	 * 
	 * 注意:当设置了录制外部的pcm数据后, 当前画板上录制的帧,就以音频的帧率为参考时间戳,从而保证音同步进行. 
	 * 故您在投递音频的时候, 需要严格按照音频播放的速度投递. 
	 * 
	 * 如采用外面的pcm数据,则在录制过程中,会参考音频时间戳,来计算得出的时间戳,
	 * 如外界音频播放完毕,无数据push,应及时stopDrawPad2
	 * 
	 * 可以通过 AudioLine 的getFrameSize()方法获取到每次应该投递多少个字节,此大小为固定大小, 每次投递必须push这么大小字节的数据,
	 * 
	 * 可通过 {@link #stopDrawPad2()}停止,停止后返回mic录制的音频文件 m4a格式的文件,
	 * 
	 * @param isrecord  是否录制
	 * @param channels  声音通道个数, 如是mp3或aac文件,可根据MediaInfo获得
	 * @param samplerate 采样率 如是mp3或aac文件,可根据MediaInfo获得
	 * @param bitrate  码率 如是mp3或aac文件,可根据MediaInfo获得
	 */
	public void setRecordExtraPcm(boolean isrecord,int channels,int samplerate,int bitrate)
	{
		if(renderer!=null){
			renderer.setRecordExtraPcm(isrecord, channels,samplerate, bitrate);
		}else{
			isRecordExtPcm=isrecord;
			pcmSampleRate=samplerate;
			pcmBitRate=bitrate;
			pcmChannels=channels;
		}
	}
	/**
	 * 获取一个音频输入对象, 向内部投递数据, 
	 * 只有当开启画板录制,并设置了录制外面数据的情况下,才有效.
	 * @return
	 */
	public AudioLine getAudioLine()
	{
		if(renderer!=null){
			return renderer.getAudioLine();
		}else{
			return null;
		}
	}
	/**
	 * 此代码只是用在分段录制的Camera的过程中, 其他地方不建议使用.
	 */
	public void segmentStart()
	{
		if(renderer!=null){
			renderer.segmentStart();
		}
	}
	/**
	 * 此代码只是用在分段录制的Camera的过程中, 其他地方不建议使用.
	 * 录制完成后, 返回当前录制这一段的文件完整路径名,
	 * 因为这里会等待 编码和音频采集模块处理完毕释放后才返回, 故有一定的阻塞时间(在低端手机上大概100ms),
	 * 建议用Handler的 HandlerMessage的形式来处理
	 */
	public String segmentStop()
	{
		if(renderer!=null){
			return renderer.segmentStop();
		}else{
			return null;	
		}
	}
	public boolean isRecording()
	{
		if(renderer!=null)
			return renderer.isRecording();
		else
			return false;
	}
	/**
	 * 当前DrawPad是否在工作.
	 * @return
	 */
	public boolean isRunning()
	{
		if(renderer!=null)
			return renderer.isRunning();
		else
			return false;
	}
	/**
	 * 停止DrawPad的渲染线程.
	 * 此方法执行后, DrawPad会释放内部所有Layer对象,您外界拿到的各种图层对象将无法再使用.
	 */
	public void stopDrawPad()
	{	
			if (renderer != null){
	        	renderer.release();
	        	renderer=null;
	        }
	}
	/**
	 * 停止DrawPad的渲染线程 
	 * 如果设置了在录制的时候,设置了录制mic或extPcm, 则返回录制音频的文件路径. 
	 * 此方法执行后, DrawPad会释放内部所有Layer对象,您外界拿到的各种图层对象将无法再使用.
	 * @return
	 */
	public String stopDrawPad2()
	{	
			String ret=null;
			if (renderer != null){
	        	renderer.release();
	        	ret=renderer.getAudioRecordPath();
	        	renderer=null;
	        }
			return ret;
	}
	/**
	 * 作用同 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}, 只是有采样宽高比, 如用我们的VideoPlayer则使用此方法,
	 * 建议用 {@link #setDrawPadSize(int, int, onDrawPadSizeChangedListener)}
	 * @param width
	 * @param height
	 * @param sarnum  如mediaplayer设置后,可以为1,
	 * @param sarden  如mediaplayer设置后,可以为1,
	 * @param cb
	 */
	public void setDrawPadSize(int width,int height,int sarnum,int sarden,onDrawPadSizeChangedListener cb)
	{
		if (width != 0 && height != 0) {
            if (mTextureRenderView != null) {
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(sarnum,sarden);
            }
            mSizeChangedCB=cb;
            requestLayout();
        }
	}
	/**
	 * 直接设置画板的宽高, 不让他自动缩放.
	 * 
	 * 要在画板开始前调用.
	 * @param width
	 * @param height
	 */
	public void setDrawpadSizeDirect(int width,int height)
	{
		drawPadWidth=width;
		drawPadHeight=height;
		if(renderer!=null){
			Log.w(TAG,"renderer maybe is running. your setting is not available!!");
		}
	}
	 /**
     * 把当前图层放到最里层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     * @param layer
     */
	public void bringLayerToBack(Layer layer)
	{
			if(renderer!=null){
				renderer.bringToBack(layer);
			}
	}
	  /**
     * 把当前图层放到最外层, 里面有 一个handler-loop机制, 将会在下一次刷新后执行.
     * @param layer
     */
	public void bringLayerToFront(Layer layer)
	{
			if(renderer!=null){
				renderer.bringToFront(layer);
			}
	}
	/**
	 * 设置当前图层对象layer 在DrawPad中所有图层队列中的位置, 您可以认为内部是一个ArrayList的列表, 先add进去的 的position是0, 后面增加的依次是1,2,3等等
	 * 可以通过Layer的getIndexLayerInDrawPad属性来获取当前图层的位置.
	 * 
	 * @param layer
	 * @param position
	 */
	public void changeLayerLayPosition(Layer layer,int position)
    {
		if(renderer!=null){
			renderer.changeLayerLayPosition(layer, position);
		}
    }
	/**
	 * 交换两个图层的位置.
	 * @param first
	 * @param second
	 */
    public void swapTwoLayerPosition(Layer first,Layer second)
    {
    	if(renderer!=null){
			renderer.swapTwoLayerPosition(first, second);
		}
    }
    public CameraLayer getCameraLayer()
    {
		if(renderer!=null)
			return renderer.getCameraLayer();
		else{
			Log.e(TAG,"addTwoVideoLayer error render is not avalid");
			return null;
		}
    }
	/**
	 * 获取一个VideoLayer,从中获取surface {@link VideoLayer#getSurface()}来设置到播放器中,
	 * 用播放器提供的画面,来作为DrawPad的画面输入源.
	 * 
	 * 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
	 * 
	 * @param width  的宽度
	 * @param height 的高度
	 * @return  VideoLayer对象
	 */
	public VideoLayer addVideoLayer(int width, int height,GPUImageFilter filter)
	{
		if(renderer!=null)
			return renderer.addVideoLayer(width,  height,filter);
		else{
			Log.e(TAG,"addVideoLayer error render is not avalid");
			return null;
		}
	}
	/**
	 * 因之前有客户自定义一个Camera图层, 我们的Drawpad可以接受外部客户自定义图层.这里填入.
	 */
//	public Layer addCustemLayer()
//	{
//			CameraLayer ret=null;
//			if(renderer!=null){
//				ret =new CameraLayer(getContext(),false,viewWidth,viewHeight,null,mUpdateMode);
//				renderer.addCustemLayer(ret);
//			}else{
//				Log.e(TAG,"CameraMaskLayer error render is not avalid");
//			}
//			return ret;
//	}
	
	/**
	 * 获取一个BitmapLayer
	 * 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
	 * @param bmp  图片的bitmap对象,可以来自png或jpg等类型,这里是通过BitmapFactory.decodeXXX的方法转换后的bitmap对象.
	 * @return 一个BitmapLayer对象
	 */
	public BitmapLayer addBitmapLayer(Bitmap bmp)
	{
		if(bmp!=null)
		{
			if(renderer!=null)
				return renderer.addBitmapLayer(bmp);
			else{
				Log.e(TAG,"addBitmapLayer error render is not avalid");
				return null;
			}
		}else{
			Log.e(TAG,"addBitmapLayer error, bitmap is null");
			return null;
		}
	}
	
	/**
	 * 获取一个DataLayer的图层, 
	 * 数据图层, 是一个RGBA格式的数据, 内部是一个RGBA格式的图像.
	 * 
	 * @param dataWidth 图像的宽度
	 * @param dataHeight 图像的高度.
	 * @return
	 */
	public DataLayer  addDataLayer(int dataWidth,int dataHeight)
	{
		if(dataWidth>0 && dataHeight>0)
		{
			if(renderer!=null)
				return renderer.addDataLayer(dataWidth, dataHeight);
			else{
				Log.e(TAG,"addDataLayer error render is not avalid");
				return null;
			}
		}else{
			Log.e(TAG,"addDataLayer error, data size is error");
			return null;
		}
	}
	
	/**
	 * 增加一个gif图层.
	 * @param gifPath  gif的绝对地址,
	 * @return
	 */
	public GifLayer  addGifLayer(String gifPath)
	{
			if(renderer!=null)
				return renderer.addGifLayer(gifPath);
			else{
				Log.e(TAG,"addYUVLayer error! render is not avalid");
				return null;
			}
	}
	
	/**
	 * 增加一个gif图层, 
	 * 
	 * resId 来自apk中drawable文件夹下的各种资源文件, 我们会在GifLayer中拷贝这个资源到默认文件夹下面, 然后作为一个普通的gif文件来做处理,使用完后, 会在Giflayer
	 * 图层释放的时候, 删除.
	 * 
	 * @param resId 来自apk中drawable文件夹下的各种资源文件.
	 * @return
	 */
	public GifLayer  addGifLayer(int resId)
	{
			if(renderer!=null)
				return renderer.addGifLayer(resId);
			else{
				Log.e(TAG,"addGifLayer error! render is not avalid");
				return null;
			}
	}
	/**
	 * 增加一个mv图层, mv图层分为两个文件, 一个是彩色的, 一个黑白
	 * @param srcPath
	 * @param maskPath
	 * @return
	 */
	public MVLayer addMVLayer(String srcPath,String maskPath)
	{
			if(renderer!=null)
				return renderer.addMVLayer(srcPath,maskPath);
			else{
				Log.e(TAG,"addMVLayer error render is not avalid");
				return null;
			}
	}
	/**
	 * 获得一个 ViewLayer,您可以在获取后,仿照我们的例子,来为增加各种UI空间.
	 * 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
	 * @return 返回ViewLayer对象.
	 */
	 public ViewLayer addViewLayer()
	 {
			if(renderer!=null)
				return renderer.addViewLayer();
			else{
				Log.e(TAG,"addViewLayer error render is not avalid");
				return null;
			}
	 }
	 /**
	  *  获得一个 CanvasLayer
	  * 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
	  * @return
	  */
	 public CanvasLayer addCanvasLayer()
	 {
			if(renderer!=null)
				return renderer.addCanvasLayer();
			else{
				Log.e(TAG,"addCanvasLayer error render is not avalid");
				return null;
			}
	 }
	 public YUVLayer addYUVLayer()
	 {
			if(renderer!=null)
				return renderer.addYUVLayer();
			else{
				Log.e(TAG,"addCanvasLayer error render is not avalid");
				return null;
			}
	 }
	 /**
	  * 从渲染线程列表中移除并销毁这个Layer;
	  * 注意:此方法一定在 startDrawPad之后,在stopDrawPad之前调用.
	  * @param Layer
	  */
	public void removeLayer(Layer layer)
	{
		if(layer!=null)
		{
			if(renderer!=null)
				renderer.removeLayer(layer);
			else{
				Log.w(TAG,"removeLayer error render is not avalid");
			}
		}
	}
	   /**
	    * 为一个图层切换多个滤镜. 即一个滤镜处理完后的输出, 作为下一个滤镜的输入.
	    * 
	    * filter的列表, 是先add进去,最新渲染, 把第一个渲染的结果传递给第二个,第二个传递给第三个,以此类推.
	    * 
	    * 注意: 这里内部会在切换的时候, 会销毁 之前的列表中的所有滤镜对象, 然后重新增加, 故您不可以把同一个滤镜对象再次放到进来,
	    * 您如果还想使用之前的滤镜,则应该重新创建一个对象.
	    * 
	    * @param layer  图层对象,
	    * @param filters  滤镜数组; 如果设置为null,则不增加滤镜.
	    * @return
	    */
	   public void  switchFilterList(Layer layer, ArrayList<GPUImageFilter> filters) {
	    	if(renderer!=null && renderer.isRunning()){
	    		renderer.switchFilterList(layer, filters);
	    	}
	    }
	   /**
	    * 请使用上面的这个,
	    * @param layer
	    * @param filter
	    */
	    @Deprecated
	     public boolean switchFilterTo(Layer layer,GPUImageFilter filter) {
	    	 if(renderer!=null && renderer.isRunning()){
		    		return renderer.switchFilterTo(layer, filter);
		    	}else{
		    		return false;
		    	}
	     }
	   
	   //----------------------------------------------
	   private boolean isCheckBitRate=true;
	   
	   private boolean isCheckPadSize=true;
	   /**
	    * 是否在开始运行DrawPad的时候,检查您设置的码率和分辨率是否正常.
	    * 
	    * 默认是检查, 如果您清楚码率大小的设置,请调用此方法,不再检查.
	    *  
	    */
	   public void setNotCheckBitRate()
	   {
		   isCheckBitRate=false;
	   }
	   /**
	    * 是否在开始运行DrawPad的时候, 检查您设置的DrawPad宽高是否是16的倍数.
	    * 默认是检查.
	    */
	   public void setNotCheckDrawPadSize()
	   {
		   isCheckPadSize=false;
	   }
	   /**
		 * 把采样点处理成4的倍数.
		 * 小于等于4的, 处理成4;  大于4小于8的处理成8;
		 * 1234 处理成4, 5678处理成8;
		 * @param value
		 * @return
		 */
		public static int makeQuad(int value)
		{
			int  val2= value/4;
			
			if(value%4!=0)
			{
				val2+=1;
				
				val2*=4;
				return val2;
			}else{
				return value;
			}
			/**
			 * 	for(int i=0;i<100;i++)
			{
				int  val= i/4;
				
				if(i%4!=0)
					val+=1;
				
				val*=4;
				System.out.println("i="+i+" val:"+val);
			}
			 */
		}
		/**
		 *  获取lansosdk的建议码率; 
		 *  这个码率不是唯一的, 仅仅是我们建议这样设置, 如果您对码率理解很清楚或有一定的压缩要求,则完全可以不用我们的建议,自行设置.
		 *  
		 * @param wxh  宽度和高度的乘积;
		 * @return
		 */
		public static int getSuggestBitRate(int wxh)
		{
			if(wxh <= 480 * 480){
				return 1000*1024;
			}else if(wxh<=640 * 480){
				return 1500*1024;
			}else if(wxh <=800 *480){
				return 1800*1024;
			}else if(wxh <=960 * 544){
				return 2000*1024;
			}else if(wxh <=1280 * 720){
				return 2500*1024;
			}else if(wxh<=1920 * 1088){
				return 3000*1024;
			}else{
				return 3500*1024;
			}
		}
		public static int checkSuggestBitRate(int wxh, int bitrate)
		{
			int sugg=getSuggestBitRate(wxh);
			return bitrate < sugg ?  sugg: bitrate;   //如果设置过来的码率小于建议码率,则返回建议码率,不然返回设置码率
		}
}
