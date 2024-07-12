package com.example.sesliasistan

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sesliasistan.ml.Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.Timer
import java.util.TimerTask


class Camera : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private lateinit var textView: TextView
    private lateinit var handler: Handler
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageView: ImageView
    private lateinit var model: Metadata1
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var paint: Paint

    private lateinit var labels: List<String>

    private lateinit var closeButton: Button

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var timer: Timer
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        closeButton = findViewById(R.id.closeButton)

        closeButton.setOnClickListener {
            model.close() // Modeli kapat
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = Metadata1.newInstance(this)
        labels = FileUtil.loadLabels(this, "label.txt")

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        textView = findViewById(R.id.textView)

        paint = Paint()

        // Timer başlat
        timer = Timer()


        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val bitmap = textureView.bitmap!!

                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray

                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)

                val h = mutableBitmap.height.toFloat()
                val w = mutableBitmap.width.toFloat()

                paint.textSize = h / 30f
                paint.strokeWidth = h / 180f

                // Mavi alanın koordinatlarını belirleme
                val rectLeft = w * 0.33f
                val rectTop = h * 0.23f
                val rectRight = w * 0.69f
                val rectBottom = h * 0.78f

                /* // Mavi alanın koordinatlarını belirleme
                 val rectLeft = w * 0.25f // Sol kenarın yüzde 25'i
                 val rectTop = h * 0.25f // Üst kenarın yüzde 25'i
                 val rectRight = w * 0.75f // Sağ kenarın yüzde 75'i
                 val rectBottom = h * 0.75f // Alt kenarın yüzde 75'i*/


                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                   if ((fl > 0.5) && (labels[classes[index].toInt()]=="bottle" || labels[classes[index].toInt()]=="chair" || labels[classes[index].toInt()]=="person" || labels[classes[index].toInt()]=="mouse" || labels[classes[index].toInt()]=="laptop"  || labels[classes[index].toInt()]=="cell phone"
                                || labels[classes[index].toInt()]=="car" || labels[classes[index].toInt()]=="bicycle" || labels[classes[index].toInt()]=="motorcycle" || labels[classes[index].toInt()]=="traffic light"  || labels[classes[index].toInt()]=="stop sign" || labels[classes[index].toInt()]=="bench" || labels[classes[index].toInt()]=="couch" || labels[classes[index].toInt()]=="keyboard" || labels[classes[index].toInt()]=="vase" || labels[classes[index].toInt()]=="toilet" || labels[classes[index].toInt()]=="sink" || labels[classes[index].toInt()]=="dog"
                                || labels[classes[index].toInt()]=="bus"
                                || labels[classes[index].toInt()]=="handbag" || labels[classes[index].toInt()]=="backpack" || labels[classes[index].toInt()]=="suitcase"
                                || labels[classes[index].toInt()]=="wine glass" || labels[classes[index].toInt()]=="cup"

                            )
                        ) {
                        // Bounding box'ın tam ortasına kırmızı bir çizgi ekleme
                        val centerX = (locations[x + 1] * w + locations[x + 3] * w) / 2
                        val centerY = (locations[x] * h + locations[x + 2] * h) / 2

                        // Nesne merkezi mavi alanın içinde mi kontrol etme
                        if (centerX >= rectLeft && centerX <= rectRight && centerY >= rectTop && centerY <= rectBottom) {
                            paint.color = Color.RED
                            canvas.drawLine(centerX - 10f, centerY, centerX + 10f, centerY, paint)
                            canvas.drawLine(centerX, centerY - 10f, centerX, centerY + 10f, paint)

                            // Nesne mavi alanın içinde, bounding box'u çizme
                            paint.color = Color.GREEN // Örneğin yeşil renk
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locations[x + 1] * w,
                                    locations[x] * h,
                                    locations[x + 3] * w,
                                    locations[x + 2] * h
                                ), paint
                            )
                            // Nesne sınıfını ve doğruluk oranını bounding box'un altına ekleme
                            paint.style = Paint.Style.FILL
                            canvas.drawText(
                                labels[classes[index].toInt()] + " " + fl.toString(),
                                locations[x + 1] * w,
                                locations[x] * h - 10f, // -10f, metnin bounding box'un üstündeki olan uzaklığını ayarlar
                                paint
                            )

                            seslendir(labels[classes[index].toInt()])
                        }
                    }
                }

                // Mavi alanı çizme
                paint.color = Color.BLUE
                paint.style = Paint.Style.STROKE
                canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)

                runOnUiThread {
                    imageView.setImageBitmap(mutableBitmap)
                }
            }

        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }



    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val surfaceTexture = textureView.surfaceTexture
                    val surface = Surface(surfaceTexture)

                    val characteristics = cameraManager.getCameraCharacteristics(camera.id)
                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull { size ->
                        size.width == textureView.width && size.height == textureView.height
                    }

                    previewSize?.let { size ->
                        surfaceTexture!!.setDefaultBufferSize(size.width, size.height)
                    }

                    val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        previewSize?.let {
                            set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, it.width, it.height))
                        }
                        addTarget(surface)
                    }

                    cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            session.setRepeatingRequest(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                    super.onCaptureCompleted(session, request, result)
                                    // Bu kısımda işlem yapmaya gerek yok çünkü işlem TextureView'deki
                                    // onSurfaceTextureUpdated içinde yapılıyor.
                                }
                            }, handler)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            TODO("Not yet implemented")
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    TODO("Not yet implemented")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    TODO("Not yet implemented")
                }
            }, handler)
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }



    private fun seslendir(sinif: String){

        textView.text=sinif

        if (!isTimerRunning) {
            // Timer başlat
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    isTimerRunning = false
                }
            }, 1500)
            isTimerRunning = true

            mediaPlayer?.release()
            when (sinif) {
                "bottle" -> mediaPlayer = MediaPlayer.create(this, R.raw.sise)
                "chair" -> mediaPlayer = MediaPlayer.create(this, R.raw.sandalye)
                "person" -> mediaPlayer = MediaPlayer.create(this, R.raw.insan)
                "mouse" -> mediaPlayer = MediaPlayer.create(this, R.raw.fare)
                "laptop" -> mediaPlayer = MediaPlayer.create(this, R.raw.laptop)
                "cell phone" -> mediaPlayer = MediaPlayer.create(this, R.raw.telefon)

                "car" -> mediaPlayer = MediaPlayer.create(this, R.raw.araba)
                "bicycle" -> mediaPlayer = MediaPlayer.create(this, R.raw.bisiklet)
                "motorcycle" -> mediaPlayer = MediaPlayer.create(this, R.raw.motorsiklet)
                "traffic light" -> mediaPlayer = MediaPlayer.create(this, R.raw.trafikisigi)
                "stop sign" -> mediaPlayer = MediaPlayer.create(this, R.raw.durisareti)
                "bench" -> mediaPlayer = MediaPlayer.create(this, R.raw.bank)
                "couch" -> mediaPlayer = MediaPlayer.create(this, R.raw.koltuk)
                "keyboard" -> mediaPlayer = MediaPlayer.create(this, R.raw.klavye)
                "vase" -> mediaPlayer = MediaPlayer.create(this, R.raw.vazo)
                "sink" -> mediaPlayer = MediaPlayer.create(this, R.raw.lavabo)
                "toilet" -> mediaPlayer = MediaPlayer.create(this, R.raw.tuvalet)
                "dog" -> mediaPlayer = MediaPlayer.create(this, R.raw.kopek)

                "bus" -> mediaPlayer = MediaPlayer.create(this, R.raw.otobus)


                "handback" -> mediaPlayer = MediaPlayer.create(this, R.raw.canta)
                "backpack" -> mediaPlayer = MediaPlayer.create(this, R.raw.canta)
                "suitcase" -> mediaPlayer = MediaPlayer.create(this, R.raw.canta)

                "wine glass" -> mediaPlayer = MediaPlayer.create(this, R.raw.bardak)
                "cup" -> mediaPlayer = MediaPlayer.create(this, R.raw.bardak)

            }
            mediaPlayer?.start()
        }


        

        println(sinif)
    }




}