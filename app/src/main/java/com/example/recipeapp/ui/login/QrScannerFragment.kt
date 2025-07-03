package com.example.recipeapp.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.recipeapp.PrefsHelper
import com.example.recipeapp.R
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScannerFragment : Fragment(), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null
    private val CAMERA_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_qr_scanner, container, false)

        scannerView = ZXingScannerView(requireContext())
        val containerLayout = rootView.findViewById<FrameLayout>(R.id.qrScannerContainer)
        val exitQrButton = rootView.findViewById<Button>(R.id.btnClose2)
        containerLayout.addView(scannerView)
        exitQrButton.setOnClickListener{
            findNavController().navigate(R.id.action_qrScannerFragment_to_loginFragment)
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            startScanning()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        scannerView?.stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scannerView = null
    }

    private fun startScanning() {
        scannerView?.setResultHandler(this)
        scannerView?.startCamera()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    override fun handleResult(rawResult: com.google.zxing.Result?) {
        val qrContent = rawResult?.text
        if (qrContent.isNullOrBlank()) {
            Toast.makeText(context, "QR scan failed or is empty", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Save to SharedPreferences
        PrefsHelper.activateKey = rawResult.toString()
        // Log the saved value for debugging
        Log.d("activateKey", PrefsHelper.activateKey ?: "No value found")
        Toast.makeText(context, "Scanned & saved!", Toast.LENGTH_SHORT).show()


        parentFragmentManager.popBackStack()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

//    override fun handleResult(rawResult: com.google.zxing.Result?) {
//        Toast.makeText(context, "Scanned: ${rawResult?.text}", Toast.LENGTH_SHORT).show()
//        parentFragmentManager.popBackStack()
//    }
}
