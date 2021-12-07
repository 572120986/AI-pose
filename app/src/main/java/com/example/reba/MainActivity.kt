package com.example.reba

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.reba.classification.PoseClassifierProcessor
import com.example.reba.databinding.ActivityMainBinding

import com.google.mlkit.vision.pose.Pose

import kotlinx.android.synthetic.main.activity_main.*


import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.example.reba.utils.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import github.hongbeomi.macgyver.mlkit.vision.barcode_scan.PoseGraphic


class MainActivity : BaseActivity(){
    private var textToSpeech: TextToSpeech? = null //创建自带语音对象
    private lateinit var onSuccessMedia: MediaPlayer
    private lateinit var onFailMedia: MediaPlayer
    var i = 0
    var i2 = 0
    var xy= false
    private var viewMap = HashMap<Int, LottieAnimationView>()
    var flag = false
    var pause = false
    var score = 0
    var x = 0.0f
    var y = 0.0f
    private lateinit var startCountDownTimer: CountDownTimer
    private fun startStatus() = ::startCountDownTimer.isInitialized
    private lateinit var ballCountDownTimer: CountDownTimer
    private fun ballStatus() = ::ballCountDownTimer.isInitialized
    private lateinit var projectCountDownTimer: CountDownTimer
    private fun projectStatus() = ::ballCountDownTimer.isInitialized
    private var projectTime = 60000L
    private var projectTimeStr = (projectTime / 60 % 60).toString()
    private var animation: Animation? = null
    private val binding by binding<ActivityMainBinding>(R.layout.activity_main)
    private lateinit var task: ThrottleTask

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textView : TextView;

    //图像分析镜像/缩放处理图
    private lateinit var graphicOverlay: GraphicOverlay
    //姿势分类
    private var poseClassifierProcessor: PoseClassifierProcessor? = null
    var classificationResult: List<String> = ArrayList()

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(options);






    /**
     * 启动
     *
     */
    @SuppressLint("WrongThread", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graphicOverlay=binding.graphicOverlayFinder
        //横 SCREEN_ORIENTATION_LANDSCAPE
        //        //竖 SCREEN_ORIENTATION_PORTRAIT
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        textView = findViewById(R.id.text_view_id)

        //姿势分类
        poseClassifierProcessor = PoseClassifierProcessor(this, true)


        //安卓端特效/声音
        animation = AnimationUtils.loadAnimation(this, R.animator.count_down_timer_animator)
        initTTS()
        onFailMedia = MediaPlayUtils.initMediaPlay(assets.openFd("fail.mp3"), false)
        onSuccessMedia = MediaPlayUtils.initMediaPlay(assets.openFd("success.mp3"), false)
        task = ThrottleTask.build({
            runOnUiThread {
                if (flag) {
                    binding.position.visibility = View.VISIBLE
                }
            }
        }, 1000L)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }






    /**
     * 手部地标是否进入矩形区域
     *       msdsZHONG
     */
    private fun handPointInRect(x: Int, y: Int, ballx: PointF, bally: PointF, ballw: PointF, ballh: PointF): Boolean {
        val A: PointF = ballx
        val B: PointF =bally
        val C: PointF = ballw
        val D: PointF = ballh
        val a = (B.x - A.x) * (y - A.y) - (B.y - A.y) * (x - A.x)
        val b = (C.x - B.x) * (y - B.y) - (C.y - B.y) * (x - B.x)
        val c = (D.x - C.x) * (y - C.y) - (D.y - C.y) * (x - C.x)
        val d = (A.x - D.x) * (y - D.y) - (A.y - D.y) * (x - D.x)
        return if (a > 0 && b > 0 && c > 0 && d > 0 || a < 0 && b < 0 && c < 0 && d < 0) {
            true
        } else
            false

    }

    /**
     * 关节点半径圆形检测
     *       msdsZHONG
     */
    fun yuan(lastX:Float,lastY:Float,vCenterX:Float,vCenterY:Float): Boolean {
        var r = 150
        var distanceX = Math.abs(vCenterX-lastX);
        var distanceY = Math.abs(vCenterY-lastY);
        var distanceZ =  Math.sqrt(Math.pow(distanceX.toDouble(), 2.0)+Math.pow(distanceY.toDouble(),2.0));
        if(distanceZ.toInt() > r){
            return false;
        }
        return  true
    }

    /**
     * 相机处理类
     *  msdsZHONG
     */
    @SuppressLint("UnsafeExperimentalUsageError", "WrongConstant", "RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

            val  analysis = ImageAnalysis.Builder()
                .setCameraSelector(cameraSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                poseDetector.process(InputImage.fromMediaImage(imageProxy.image, imageProxy.imageInfo.rotationDegrees)).addOnSuccessListener { pose ->
                    imagePoseAnalyzer(pose,imageProxy.cropRect)
                    imageProxy.close()
                }
                    .addOnFailureListener { error ->
                        Log.d(ContentValues.TAG, "Failed to process the image")
                        error.printStackTrace()
                        imageProxy.close()
                    }

            })
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(90)
                .build()
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, analysis, imageCapture, preview)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    override fun onDestroy() { super.onDestroy()
        cameraExecutor.shutdown()
        poseDetector.close()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }




    /**
     * MLKIT-pose地标解析
     *       msdsZHONG
     */
    private fun imagePoseAnalyzer(pose: Pose,rect: Rect) {
        graphicOverlay.clear()
        if (pose.allPoseLandmarks.isNotEmpty()) {
            //地标镜像/缩放/绘制等
            val barcodeGraphic = PoseGraphic(graphicOverlay, pose,rect)
            graphicOverlay.add(barcodeGraphic)
            graphicOverlay.postInvalidate()

            binding.position.visibility = View.GONE
              //全身地标置信度大于等于0.95
//            if (pose.allPoseLandmarks[0].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[1].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[2].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[3].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[4].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[5].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[6].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[7].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[8].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[9].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[10].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[11].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[12].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[13].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[14].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[15].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[16].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[17].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[18].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[19].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[20].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[21].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[22].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[23].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[24].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[25].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[26].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[27].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[28].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[29].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[30].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[31].inFrameLikelihood in 0.95f..1.0f && pose.allPoseLandmarks[32].inFrameLikelihood in 0.95f..1.0f)
//            {
//                onSuccess(pose)
//            }
            //左右手腕地标置信度大于等于0.95
            if (pose.allPoseLandmarks[15].inFrameLikelihood in 0.95f..1f||pose.allPoseLandmarks[16].inFrameLikelihood in 0.95f..1f){
                onSuccess(pose)
            }
        } else {
            onFail()
        }
    }

    /**
     * 镜像缩放-X映射
     *       msdsZHONG
     */
    fun translateX(horizontal: Float): Float {
        return if (graphicOverlay.mScale != null && graphicOverlay.mOffsetX != null && !graphicOverlay.isFrontMode()) {
            (horizontal * graphicOverlay.mScale!!) + graphicOverlay.mOffsetX!!
        } else if (graphicOverlay.mScale != null && graphicOverlay.mOffsetX != null && graphicOverlay.isFrontMode()) {
            val centerX = graphicOverlay.width.toFloat() / 2
            centerX - ((horizontal * graphicOverlay.mScale!!) + graphicOverlay.mOffsetX!! - centerX)
        } else {
            horizontal
        }
    }
    /**
     * 镜像缩放-Y映射
     *       msdsZHONG
     */
    fun translateY(vertical: Float): Float {
        return if (graphicOverlay.mScale != null && graphicOverlay.mOffsetY != null) {
            (vertical * graphicOverlay.mScale!!) + graphicOverlay.mOffsetY!!
        } else {
            vertical
        }
    }


    /**
     * MLKIT-pose地标与浮点特效解析识别
     * msdsZHONG
     */
    private fun onSuccess(pose: Pose) {
        if (flag) {
            //俯卧撑识别计数（竖屏） start
//            classificationResult = poseClassifierProcessor!!.getPoseResult(pose)
//            for ((num,index) in classificationResult.withIndex()){
//                println("num=$num"+"index=$index")
//                if(index.length>0){
//                    if(index.contains("pushups_down",ignoreCase = true)){
//                        var result = index.filter { it.isDigit() }
//                        score = result.toInt()
//                        binding.score.text = score.toString()
//                        onSuccessMedia.start()
//                    }
//                }
//            }
            //俯卧撑识别计数（竖屏） end

            //全身入框  start
            var handleft0 =handPointInRect(translateX(pose.allPoseLandmarks[0].position.x).toInt()-50,translateY(pose.allPoseLandmarks[0].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft12 =handPointInRect(translateX(pose.allPoseLandmarks[12].position.x).toInt()-50,translateY(pose.allPoseLandmarks[12].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft11 =handPointInRect(translateX(pose.allPoseLandmarks[11].position.x).toInt()-50,translateY(pose.allPoseLandmarks[11].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft23 =handPointInRect(translateX(pose.allPoseLandmarks[23].position.x).toInt()-50,translateY(pose.allPoseLandmarks[23].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft24 =handPointInRect(translateX(pose.allPoseLandmarks[24].position.x).toInt()-50,translateY(pose.allPoseLandmarks[24].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft25 =handPointInRect(translateX(pose.allPoseLandmarks[25].position.x).toInt()-50,translateY(pose.allPoseLandmarks[25].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft26 =handPointInRect(translateX(pose.allPoseLandmarks[26].position.x).toInt()-50,translateY(pose.allPoseLandmarks[26].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft27 =handPointInRect(translateX(pose.allPoseLandmarks[27].position.x).toInt()-50,translateY(pose.allPoseLandmarks[27].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft28 =handPointInRect(translateX(pose.allPoseLandmarks[28].position.x).toInt()-50,translateY(pose.allPoseLandmarks[28].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft13 =handPointInRect(translateX(pose.allPoseLandmarks[13].position.x).toInt()-50,translateY(pose.allPoseLandmarks[13].position.y*3.3f).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
            var handleft14 =handPointInRect(translateX(pose.allPoseLandmarks[14].position.x).toInt()-50,translateY(pose.allPoseLandmarks[14].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))

//            if(handleft14==true && handleft13==true && handleft27==true &&handleft28==true&&handleft25==true&&handleft26==true&&handleft23==true&&handleft24==true&&handleft11==true&&handleft12==true
//                && handleft0==true){
//                score += 3
//                binding.score.text = score.toString()
//                binding.scoreAnimation.x = x+160
//                binding.scoreAnimation.y = y+160
//                binding.scoreAnimation.playAnimation()
//                x = 0f
//                y = 0f
//                onSuccessMedia.start()
//                binding.zhuang.cancelAnimation()
////                binding.table.removeView(viewMap[i])
////                ballCountDownTimer.cancel()
////                ballCountDownTimer.start()
//                pileleft()
//            }
            //全身入框  end

            //左右手入框  start
            if(!booleanbody){
                var handleft16 =handPointInRect(translateX(pose.allPoseLandmarks[16].position.x).toInt()-50,translateY(pose.allPoseLandmarks[16].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft18 =handPointInRect(translateX(pose.allPoseLandmarks[18].position.x).toInt()-50,translateY(pose.allPoseLandmarks[18].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft20 =handPointInRect(translateX(pose.allPoseLandmarks[20].position.x).toInt()-50,translateY(pose.allPoseLandmarks[20].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft22 =handPointInRect(translateX(pose.allPoseLandmarks[22].position.x).toInt()-50,translateY(pose.allPoseLandmarks[22].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))

                var handleft15 =handPointInRect(translateX(pose.allPoseLandmarks[15].position.x).toInt()-50,translateY(pose.allPoseLandmarks[15].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft17 =handPointInRect(translateX(pose.allPoseLandmarks[17].position.x).toInt()-50,translateY(pose.allPoseLandmarks[17].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft19 =handPointInRect(translateX(pose.allPoseLandmarks[19].position.x).toInt()-50,translateY(pose.allPoseLandmarks[19].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))
                var handleft21 =handPointInRect(translateX(pose.allPoseLandmarks[21].position.x).toInt()-50,translateY(pose.allPoseLandmarks[21].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+550,y+80),PointF(x+550,y+80+385))

                if (handleft16==true &&handleft18==true &&handleft20==true &&handleft22==true || handleft15==true && handleft17==true && handleft19==true&& handleft21==true){
                    score += 3
                    binding.score.text = score.toString()
                    binding.scoreAnimation.x = x+160
                    binding.scoreAnimation.y = y+160
                    binding.scoreAnimation.playAnimation()
                    x = 0f
                    y = 0f
                    onSuccessMedia.start()
                    binding.table.removeView(viewMap[i])
                    bodybox()
                }
            }
            if(booleanbody){
                //左右手入框
                var handleft16 =handPointInRect(translateX(pose.allPoseLandmarks[16].position.x).toInt()-50,translateY(pose.allPoseLandmarks[16].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft18 =handPointInRect(translateX(pose.allPoseLandmarks[18].position.x).toInt()-50,translateY(pose.allPoseLandmarks[18].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft20 =handPointInRect(translateX(pose.allPoseLandmarks[20].position.x).toInt()-50,translateY(pose.allPoseLandmarks[20].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft22 =handPointInRect(translateX(pose.allPoseLandmarks[22].position.x).toInt()-50,translateY(pose.allPoseLandmarks[22].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))

                var handleft15 =handPointInRect(translateX(pose.allPoseLandmarks[15].position.x).toInt()-50,translateY(pose.allPoseLandmarks[15].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft17 =handPointInRect(translateX(pose.allPoseLandmarks[17].position.x).toInt()-50,translateY(pose.allPoseLandmarks[17].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft19 =handPointInRect(translateX(pose.allPoseLandmarks[19].position.x).toInt()-50,translateY(pose.allPoseLandmarks[19].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))
                var handleft21 =handPointInRect(translateX(pose.allPoseLandmarks[21].position.x).toInt()-50,translateY(pose.allPoseLandmarks[21].position.y).toInt()+50,PointF(x,y+80+550),PointF(x,y+80),PointF(x+660,y+80),PointF(x+660,y+80+550))

                if (handleft16==true && handleft18==true && handleft20==true && handleft22==true || handleft15==true && handleft17==true && handleft19==true&& handleft21==true){
                    score += 3
                    binding.score.text = score.toString()
                    binding.scoreAnimation.x = x+160
                    binding.scoreAnimation.y = y+160
                    binding.scoreAnimation.playAnimation()
                    x = 0f
                    y = 0f
                    onSuccessMedia.start()
                    binding.table.removeView(viewMap[i])
                    bodybox()
                }
            }
            //左右手入框  end


            //左右手顶点半径内侧滑摸桩OR摸球   start
            var handright=yuan(x+247,y+192,translateX(pose.allPoseLandmarks[19].position.x),translateY(pose.allPoseLandmarks[19].position.y))
            var handleft=yuan(x+247,y+192,translateX(pose.allPoseLandmarks[20].position.x),translateY(pose.allPoseLandmarks[20].position.y))
//            if (handleft16==true &&handleft18==true &&handleft20==true &&handleft22==true || handleft15==true && handleft17==true && handleft19==true&& handleft21==true){
////            if (handright==true||handleft==true){
//                score += 3
//                binding.score.text = score.toString()
//                binding.scoreAnimation.x = x+160
//                binding.scoreAnimation.y = y+160
//                binding.scoreAnimation.playAnimation()
//                x = 0f
//                y = 0f
//                onSuccessMedia.start()
//                binding.zhuang.cancelAnimation()
//                binding.table.removeView(viewMap[i])
////                ballCountDownTimer.cancel()
////                ballCountDownTimer.start()
//                bodybox()
//            }else{
////                x = 0f
////                y = 0f
//            }
            //左右手顶点半径内侧滑摸桩OR摸球   end

        } else {
            flag = true
            binding.tips.visibility = View.GONE
            binding.person.visibility = View.GONE
            binding.success.playAnimation()
            binding.success.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                }
                override fun onAnimationEnd(animation: Animator?) {
                    binding.success.visibility = View.GONE
                    startGame()
                }
                override fun onAnimationCancel(animation: Animator?) {
                }
                override fun onAnimationRepeat(animation: Animator?) {
                }
            })
        }
    }

    private fun onFail() {
        if (flag) {
            binding.position.visibility = View.VISIBLE
            task.run()
        }
    }

    var randomsY = 0f


    /**
     * MLKIT检测到骨骼时启动进入游戏
     */
    private fun startGame() {
        val projectName = "游戏开始"
        val text1 = projectName.substring(0, projectName.length / 2)
        val text2 = projectName.substring(projectName.length / 2)
        binding.project.text = text1 + "\n" + text2
        start()
    }


    /**
     * 特效准备
     */
    private fun start() {
        startCountDownTimer = object : CountDownTimer(6000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished / 1000 <= 3 && millisUntilFinished / 1000 != 0L) {
                    binding.project.text = "" + millisUntilFinished / 1000
                    binding.project.startAnimation(animation)
                } else if (millisUntilFinished / 1000 == 0L) {
                    binding.project.text = "Go"
                    binding.project.startAnimation(animation)
//                    randomsBall()
//                    pileleft()
                    bodybox()
                    projectCountDownTimer()
                }
            }

            override fun onFinish() {
                binding.project.visibility = View.GONE
            }
        }.start()
    }


  var booleanbody=false

    /**
     * 随机框 1.bot-big 2.top-big 3.bot-sm
     * msdsZHONG
     */
    var bodyboxrandom ="1"


    /**
     * 全身入框
     * msdsZHONG
     */
    var jsonpath=""
    fun bodybox(){
        i+=1
        if(bodyboxrandom.toInt()==1){
            randomsY =1000f
            bodyboxrandom =="2"
            x = 100f
            y = randomsY
            jsonpath="bodyBox/bot_big.json"
            booleanbody=false
        }
        if(bodyboxrandom.toInt() ==2) {
            randomsY = 500f
            bodyboxrandom =="3"
            x = 500f
            y = randomsY
            jsonpath="bodyBox/top_big.json"
            booleanbody=false
        }
        if(bodyboxrandom.toInt() ==3) {
            randomsY = 1600f
            bodyboxrandom =="1"
            x = 100f
            y = randomsY
            jsonpath="bodyBox/bot_sm.json"
            booleanbody=true
        }
        var lottieAnimationView=LottieAnimationView(applicationContext)
        binding.table.addView(lottieAnimationView)
        viewMap[i] = lottieAnimationView
        lottieAnimationView.setAnimation(jsonpath)
        lottieAnimationView.translationX = x
        lottieAnimationView.translationY = randomsY
        lottieAnimationView.rotation=90f
        lottieAnimationView.playAnimation()
    }

    /**
     * 侧滑摸桩
     * msdsZHONG
     */
    fun pileleft(){
                i+=1
                if(xy){
                    randomsY =150f
                    xy=false
                } else{
                    randomsY =1500f
                    xy=true
                }
                x = 100f
                y = randomsY
                viewMap[i] = binding.zhuang
                binding.zhuang.translationX = 100f
                binding.zhuang.translationY = randomsY
                binding.zhuang.playAnimation()
    }

    /**
     * 随机生成浮点特效（X,Y）
     */
    @SuppressLint("WrongConstant")
     private fun randomsBall() {
        ballCountDownTimer = object : CountDownTimer(projectTime, 3000) {
            override fun onTick(millisUntilFinished: Long) {
                i += 1
                if(xy){
                     randomsY =350f
                     xy=false
                } else{
                    randomsY =1700f
                    xy=true
                }
                val randomsX = 500f
                x = randomsX
                y = randomsY
                val lottieAnimationView = LottieAnimationView(applicationContext)
                binding.table.addView(lottieAnimationView)
                viewMap[i] = lottieAnimationView
                lottieAnimationView.setAnimation("green.json")
                lottieAnimationView.translationX = randomsX
                lottieAnimationView.translationY = randomsY
                lottieAnimationView.playAnimation()
                val animator = ValueAnimator.ofFloat(0f, 1f)
                    .setDuration(3000)
                animator.addUpdateListener { animation: ValueAnimator ->
                    lottieAnimationView.progress = animation.animatedValue as Float
                }
                lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {
                    }
                    override fun onAnimationEnd(animator: Animator) {
                        if (score != 0) {
                            score - 3
                            binding.score.text = score.toString()
                        }
                        binding.table.removeView(viewMap[i])
                        onFailMedia.start()
                    }
                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
            }

            override fun onFinish() {
            }
        }.start()
    }

    /**
     * ????
     */
    private fun projectCountDownTimer() {
        projectCountDownTimer = object : CountDownTimer(projectTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                projectTime = millisUntilFinished
                if ((millisUntilFinished / 1000) <= 60) {
                    binding.time.text = "0:" + (millisUntilFinished / 1000).toString()
                }
            }
            override fun onFinish() {
//                ballCountDownTimer.cancel()
                binding.scoreContent.visibility = View.VISIBLE
                binding.tv1.text = score.toString()
                binding.totalTime.text = projectTimeStr
                binding.reset.setOnClickListener {
                    score = 0
                    projectTime = 60000L
                    cancelCountDownTime()
                    flag = false
                    pause = false
                    binding.person.visibility = View.VISIBLE
                    binding.time.text = "0:00"
                    binding.success.visibility = View.VISIBLE
                    binding.tips.visibility = View.VISIBLE
                    binding.position.visibility = View.GONE
                    binding.zhuang.cancelAnimation()


                }
            }
        }.start()
    }

    /**
     * 声音信息
     */
    private fun initTTS() {
        //实例化自带语音对象
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.setPitch(1.5f) //方法用来控制音调
                textToSpeech!!.setSpeechRate(1.0f) //用来控制语速
            } else {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelCountDownTime() {
        if (startStatus()) {
            startCountDownTimer.cancel()
        }
        if (ballStatus()) {
            ballCountDownTimer.cancel()
        }
        if (projectStatus()) {
            projectCountDownTimer.cancel()
        }
    }
}