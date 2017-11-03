package doext.module.do_MultiAudio.implement;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import doext.module.do_MultiAudio.define.do_MultiAudio_MAbstract;
import doext.module.do_MultiAudio.define.do_MultiAudio_IMethod;

/**
 * 自定义扩展MM组件Model实现，继承do_MultiAudio_MAbstract抽象类，并实现do_MultiAudio_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象；
 * 获取DoInvokeResult对象方式new DoInvokeResult(this.getUniqueKey());
 */
public class do_MultiAudio_Model extends do_MultiAudio_MAbstract implements do_MultiAudio_IMethod {

    private MediaPlayer mediaPlayer;
    private boolean isStop;
    private Timer timer;

    public do_MultiAudio_Model() throws Exception {
        super();
        iniMediaPlayer();
    }

    private void iniMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    getEventCenter().fireEvent("error", new DoInvokeResult(getUniqueKey()));
                    stopPlayer();
                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    getEventCenter().fireEvent("playFinished", new DoInvokeResult(getUniqueKey()));
                    stopPlayer();
                }
            });
        }
    }

    /**
     * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
     *
     * @_methodName 方法名称
     * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas,
                                    DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult)
            throws Exception {
        if ("play".equals(_methodName)) {
            play(_dictParas, _scriptEngine, _invokeResult);
            return true;
        }
        if ("stop".equals(_methodName)) {
            stop(_dictParas, _scriptEngine, _invokeResult);
            return true;
        }
        if ("pause".equals(_methodName)) {
            pause(_dictParas, _scriptEngine, _invokeResult);
            return true;
        }
        if ("resume".equals(_methodName)) {
            resume(_dictParas, _scriptEngine, _invokeResult);
            return true;
        }
        return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
    }

    /**
     * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用，
     * 可以根据_methodName调用相应的接口实现方法；
     *
     * @_methodName 方法名称
     * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
     * @_scriptEngine 当前page JS上下文环境
     * @_callbackFuncName 回调函数名
     * #如何执行异步方法回调？可以通过如下方法：
     * _scriptEngine.callback(_callbackFuncName, _invokeResult);
     * 参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
     * 获取DoInvokeResult对象方式new DoInvokeResult(this.getUniqueKey());
     */
    @Override
    public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas,
                                     DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
        //...do something
        return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
    }


    /**
     * 暂停播放；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public void pause(JSONObject _dictParas, DoIScriptEngine _scriptEngine,
                      DoInvokeResult _invokeResult) throws Exception {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            _invokeResult.setResultInteger(mediaPlayer.getCurrentPosition());
        }
    }

    /**
     * 开始播放；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public void play(JSONObject _dictParas, DoIScriptEngine _scriptEngine,
                     DoInvokeResult _invokeResult) throws Exception {
        String path = DoJsonHelper.getString(_dictParas, "path", "");
        int position = DoJsonHelper.getInt(_dictParas, "point", 0);
        if (null == DoIOHelper.getHttpUrlPath(path)) {
            path = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), path);
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        isStop = false;
        mediaPlayer.reset();//把各项参数恢复到初始状态
        if (DoIOHelper.isAssets(path)) {
            Context mContext = DoServiceContainer.getPageViewFactory().getAppContext();
            AssetFileDescriptor _mFileDescriptor = mContext.getAssets().openFd(DoIOHelper.getAssetsRelPath(path));
            mediaPlayer.setDataSource(_mFileDescriptor.getFileDescriptor(), _mFileDescriptor.getStartOffset(), _mFileDescriptor.getLength());
        } else {
            mediaPlayer.setDataSource(path);
        }
        mediaPlayer.prepareAsync();//进行缓冲
        mediaPlayer.setOnPreparedListener(new PreparedListener(position));//注册一个监听器
    }

    private final class PreparedListener implements MediaPlayer.OnPreparedListener {
        private int positon;

        public PreparedListener(int positon) {
            this.positon = positon;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start();//开始播放
            if (positon > 0) {//如果音乐不是从头播放
                mediaPlayer.seekTo(positon);
            }
            if (null == timer) {
                timer = new Timer();
            }
            onPlayPositionChange();
        }
    }

    private void onPlayPositionChange() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        DoInvokeResult jsonResult = new DoInvokeResult(getUniqueKey());
                        JSONObject json = new JSONObject();
                        json.put("currentTime", mediaPlayer.getCurrentPosition());
                        json.put("totalTime", mediaPlayer.getDuration());
                        jsonResult.setResultNode(json);
                        getEventCenter().fireEvent("playProgress", jsonResult);
                    }
                } catch (JSONException e) {
                    DoServiceContainer.getLogEngine().writeError("do_Audio->playProgress", e);
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, 0, 500);
    }

    /**
     * 继续播放；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public void resume(JSONObject _dictParas, DoIScriptEngine _scriptEngine,
                       DoInvokeResult _invokeResult) throws Exception {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                if (!isStop) {
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 停止播放；
     *
     * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
     * @_scriptEngine 当前Page JS上下文环境对象
     * @_invokeResult 用于返回方法结果对象
     */
    @Override
    public void stop(JSONObject _dictParas, DoIScriptEngine _scriptEngine,
                     DoInvokeResult _invokeResult) throws Exception {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            isStop = true;
            mediaPlayer.stop();
            stopPlayer();
        }
    }

    //释放
    private void stopPlayer() {
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer = null;
        }
        stopPlayer();
    }
}