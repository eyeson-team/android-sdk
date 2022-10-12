package com.eyeson.android.ui.connection

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.eyeson.android.BR
import com.eyeson.android.R
import com.eyeson.android.databinding.ConnectionFragmentBinding
import com.eyeson.android.ui.main.MainFragment
import com.eyeson.android.ui.scanner.ScannerFragment
import timber.log.Timber

class ConnectionFragment : Fragment() {

    private lateinit var binding: ConnectionFragmentBinding
    private val viewModel: ConnectionViewModel by viewModels()

    private val requestMultiplePermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                if (it.value) {
                    Timber.d("Permission request \"${it.key}\" granted")
                } else {
                    Timber.d("Permission request \"${it.key}\" was denied.")
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(ScannerFragment.SCAN_RESULT) { _, bundle ->
            bundle.getString(ScannerFragment.SCAN_ULR)?.let {
                binding.guestToken.setText(it, TextView.BufferType.EDITABLE)
            }
        }

        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        requestMultiplePermissions.launch(
            permissions.toTypedArray()
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.connection_fragment, container, false)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startScanner.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                addToBackStack(null)
                replace(
                    R.id.container,
                    ScannerFragment.newInstance(),
                    ScannerFragment::class.java.canonicalName
                )
            }
        }

        binding.connect.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                addToBackStack(null)
                replace(
                    R.id.container,
                    MainFragment.newInstance(binding.accessKey.text.toString(), "", ""),
                    MainFragment::class.java.canonicalName
                )
            }
        }

        binding.guestConnect.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                addToBackStack(null)
                replace(
                    R.id.container,
                    MainFragment.newInstance(
                        "",
                        binding.guestToken.text.toString(),
                        binding.guestName.text.toString()
                    ),
                    MainFragment::class.java.canonicalName
                )
            }
        }
    }

    companion object {
        fun newInstance() = ConnectionFragment()
    }
}