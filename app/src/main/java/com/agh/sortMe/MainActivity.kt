package com.agh.sortMe

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import com.mindorks.paracamera.Camera
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log

class MainActivity : AppCompatActivity() {

  private lateinit var camera: Camera
  private val PERMISSION_REQUEST_CODE = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    Log.i("r", "init")
    // Init Firebase
    FirebaseApp.initializeApp(this)

    // Configure Camera
    camera = Camera.Builder()
            .resetToCorrectOrientation(true)//1
            .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO)//2
            .setDirectory("pics")//3
            .setName("waste_${System.currentTimeMillis()}")//3
            .setImageFormat(Camera.IMAGE_JPEG)//4
            .setCompression(75)//5
            .build(this)
  }

  fun takePicture(view: View) {
    if (!hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            !hasPermission(android.Manifest.permission.CAMERA)) {
      // If do not have permissions then request it
      requestPermissions()
    } else {
      // else all permissions granted, go ahead and take a picture using camera
      try {
        camera.takePicture()
      } catch (e: Exception) {
        // Show a toast for exception
        Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun requestPermissions() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

      mainLayout.snack(getString(R.string.permission_message), Snackbar.LENGTH_INDEFINITE) {
        action(getString(R.string.OK)) {
          ActivityCompat.requestPermissions(this@MainActivity,
                  arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                          android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
      }
    } else {
      ActivityCompat.requestPermissions(this,
              arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                      android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
      return
    }
  }

  private fun hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this,
            permission) == PackageManager.PERMISSION_GRANTED

  }

  override fun onRequestPermissionsResult(requestCode: Int,
                                          permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      PERMISSION_REQUEST_CODE -> {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
          try {
            camera.takePicture()
          } catch (e: Exception) {
            Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                    Toast.LENGTH_SHORT).show()
          }
        }
        return
      }
    }
  }


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
        val bitmap = camera.cameraBitmap
        if (bitmap != null) {
          imageView.setImageBitmap(bitmap)
          detectWasteOnCloud(bitmap)
        } else {
          Toast.makeText(this.applicationContext, getString(R.string.picture_not_taken),
                  Toast.LENGTH_SHORT).show()
        }
      }
    }
  }


  private fun displayResultMessage(getObjectFromPicture: String) {
    responseCardView.visibility = View.VISIBLE
    val result: String = getObjectFromPicture
    if(isPaper(result)){
      responseCardView.setCardBackgroundColor(Color.BLUE)
      responseTextView.text = "PAPIER";
    }
    else if (isGlass(result)){
      responseCardView.setCardBackgroundColor(Color.GREEN)
      responseTextView.text = "SZKŁO";

    }
    else if (isPlastic(result)){
      responseCardView.setCardBackgroundColor(Color.rgb(255,165,0))
      responseTextView.text = "PLASTIC";
    }
    else if (isBio(result)){
      responseCardView.setCardBackgroundColor(Color.rgb(160,82,45))
      responseTextView.text = "BIO";
    }
    else
    {
      responseCardView.setCardBackgroundColor(Color.GRAY)
      responseTextView.text = "ODPADY ZMIESZANE"
    }
  }
  private fun isPaper(rubbish: String):Boolean{
    var array = arrayOf<String>("Paper","Box","Book","Newspaper")
    if(array.contains(rubbish)){
      return true;
    }
    return false;
  }
  private fun isGlass(rubbish: String):Boolean{
    var array = arrayOf<String>("Glass","Jar")
    if(array.contains(rubbish)){
      return true;
    }
    return false;
  }

  private fun isPlastic(rubbish: String):Boolean{
    var array = arrayOf<String>("Water","Plastic")
    if(array.contains(rubbish)){
      return true;
    }
    return false;
  }
  private fun isBio(rubbish: String):Boolean{
    var array = arrayOf<String>("Food")
    if(array.contains(rubbish)){
      return true;
    }
    return false;
  }

  private fun getObjectFromPicture(items: List<String>): String {
    for (result in items) {

      Log.i("result",result)
      return result
    }
    return "false"
  }

  private fun detectDeliciousWasteOnDevice(bitmap: Bitmap) {
    //1
    progressBar.visibility = View.VISIBLE
    val image = FirebaseVisionImage.fromBitmap(bitmap)
    val options = FirebaseVisionLabelDetectorOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()
    val detector = FirebaseVision.getInstance().getVisionLabelDetector(options)

    //2
    detector.detectInImage(image)
            //3
            .addOnSuccessListener {
              progressBar.visibility = View.INVISIBLE

//          getObjectFromPicture(it.map { it.label.toString() }))

              displayResultMessage(getObjectFromPicture(it.map { it.label.toString() }))
//          } else {
//            displayResultMessage(false)
//          }
            }//4
            .addOnFailureListener {
              progressBar.visibility = View.INVISIBLE
              Toast.makeText(this.applicationContext, getString(R.string.error),
                      Toast.LENGTH_SHORT).show()
            }
  }

//
  private fun detectWasteOnCloud(bitmap: Bitmap) {
    progressBar.visibility = View.VISIBLE
    val image = FirebaseVisionImage.fromBitmap(bitmap)
    val options = FirebaseVisionCloudDetectorOptions.Builder()
        .setMaxResults(10)
        .build()
    val detector = FirebaseVision.getInstance()
        //1
        .getVisionCloudLabelDetector(options)

    detector.detectInImage(image)
        .addOnSuccessListener {

          progressBar.visibility = View.INVISIBLE
          displayResultMessage(getObjectFromPicture(it.map { it.label.toString() }))
        }
        .addOnFailureListener {
          progressBar.visibility = View.INVISIBLE
          Toast.makeText(this.applicationContext, getString(R.string.error),
              Toast.LENGTH_SHORT).show()

        }
  }
}






 
