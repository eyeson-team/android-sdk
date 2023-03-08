package com.eyeson.android.ui.view.main

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eyeson.android.BR
import com.eyeson.android.R
import com.eyeson.android.databinding.MainFragmentBinding
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon

class MainFragment : Fragment() {

    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels()

    private var accessKey: String? = null

    private val requestMultiplePermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // NOOP. Permission should be granted at this point
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        arguments?.let {
            accessKey = it.getString(ACCESS_KEY)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            disconnect()
            isEnabled = false
            requireActivity().onBackPressed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        val imm: InputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.callTerminated.collect {
                        if (it) {
                            clearTargets()
                            Toast.makeText(requireContext(), "Call terminated", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }

        if (!viewModel.inCall) {
            if (!accessKey.isNullOrBlank()) {
                viewModel.connect(
                    accessKey ?: return,
                    binding.localVideo,
                    binding.remoteVideo
                )
            }
        } else {
            viewModel.setTargets(binding.localVideo, binding.remoteVideo)
        }

        bindVideoViews()
    }

    private fun disconnect() {
        viewModel.disconnect()
        binding.localVideo.release()
        binding.remoteVideo.release()
    }

    private fun bindVideoViews() {
        binding.localVideo.init(viewModel.getEglContext())
        binding.localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        binding.remoteVideo.init(viewModel.getEglContext())
        binding.remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearTargets()
    }

    private fun clearTargets() {
        binding.localVideo.release()
        binding.remoteVideo.release()

        viewModel.clearTarget()
    }

    override fun onDestroy() {
        if (requireActivity().isFinishing) {
            viewModel.disconnect()
        }
        super.onDestroy()
    }

    companion object {
        private const val ACCESS_KEY = "access_key"

        fun newInstance(accessKey: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ACCESS_KEY, accessKey)
                }
            }
    }
}