package com.example.reba.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.camera.core.internal.utils.ImageUtil

import com.google.mlkit.vision.pose.PoseLandmark





class RectOverlay constructor(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    lateinit var paints: Paint
    lateinit var paintss: Paint
    lateinit var paint: Paint

    init{
        init()
    }

    private fun init(){
        paint = Paint()
        paint.color = Color.WHITE
        paint.strokeWidth = 10f
        paint.style = Paint.Style.STROKE

        paintss = Paint()
        paintss.strokeWidth = 10f
        paintss.color = Color.GREEN
        paints = Paint()
        paints.strokeWidth = 10f
        paints.color = Color.YELLOW


    }
    fun bit(bitmap: Bitmap){
        extraBitmap=bitmap
    }


    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val dm2 = resources.displayMetrics


        println("width-display :" + dm2.widthPixels)
        println("heigth-display :" + dm2.heightPixels)
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        var matrix =  Matrix();
//        matrix.postScale((-1).toFloat(), 1F); // 镜像水平翻转
//        var convertBmp = extraBitmap?.let { Bitmap.createBitmap(extraBitmap, 0, 0, extraBitmap.getWidth(),  extraBitmap.getHeight(), matrix, true) };
        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    fun clear() {
        extraCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    internal fun drawNecksss(x :Float,y:Float) {
        extraCanvas.drawCircle(x, y, 10f, paints);//圆
        invalidate();
    }
    internal fun drawNeckssss(x :Float,y:Float) {
        extraCanvas.drawCircle(x, y, 10f, paint);//圆
        invalidate();
    }



    fun drawtx(startx:Float,starty:Float,endx:Float,endy:Float) {
        extraCanvas.drawLine(
            startx , starty, endx , endy, paintss
        )
        invalidate();

    }
    fun drawtxx(startx:Float,starty:Float,endx:Float,endy:Float) {
        extraCanvas.drawLine(
            startx , starty, endx , endy, paints
        )
        invalidate();

    }
internal  fun drawLines(pts : FloatArray , offset:Int , count:Int ,paint: Paint ){
    System.out.println(pts)
    extraCanvas.drawLines(
        pts , offset,count , paint
    )
    invalidate()
}

fun juxing(x:Float,y:Float,radius:Float){
    extraCanvas.drawCircle(x,y,radius,paints);
    invalidate()
}

    internal fun drawLine(
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?
    ) {
        val start = startLandmark!!.position
        val end = endLandmark!!.position

        val xmul = 3.3f;
        val ymul = 3.3f;

//        System.out.println("原坐标"+start.x+"--"+start.y)

        extraCanvas.drawLine(
            start.x , start.y,end.x ,end.y, paints
        )
        extraCanvas.drawLine(
            start.x *xmul-250, start.y*ymul,end.x*xmul-250 ,end.y*ymul, paint
        )

//        extraCanvas.drawLine(
//            (pts[0]) , pts[1], ptse[0] , ptse[1], paint
//        )

        invalidate();
    }




}