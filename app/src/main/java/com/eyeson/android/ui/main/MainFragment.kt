package com.eyeson.android.ui.main

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eyeson.android.BR
import com.eyeson.android.R
import com.eyeson.android.databinding.MainFragmentBinding
import com.eyeson.android.ui.events.EventListDialogFragment
import com.eyeson.android.ui.events.EventsViewModel
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon
import timber.log.Timber

class MainFragment : Fragment() {

    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels()
    private val eventViewModel: EventsViewModel by activityViewModels()

    private var accessKey: String? = null
    private var guestToken: String? = null
    private var guestName: String? = null

    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Timber.d("Permission request granted")
            } else {
                Timber.d("Permission request was denied.")
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // NOOP. Permission should be granted at this point
        }
    private val requestScreenCapturePermission =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode != AppCompatActivity.RESULT_OK) {
                Toast.makeText(
                    requireContext(),
                    "Permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            viewModel.startScreenShare(
                it.data ?: return@registerForActivityResult,
                false,
                7,
                generateScreenShareNotification()
            )

//            bindVideoViews()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
        setHasOptionsMenu(true)

        arguments?.let {
            accessKey = it.getString(ACCESS_KEY)
            guestToken = it.getString(GUEST_TOKEN)
            guestName = it.getString(GUEST_NAME)
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

        binding.connect.setOnClickListener {
            viewModel.disconnect()
            clearTargets()

            when {
                !accessKey.isNullOrBlank() -> {
                    viewModel.connect(
                        accessKey ?: return@setOnClickListener,
                        binding.localVideo,
                        binding.remoteVideo
                    )
                }
                !guestToken.isNullOrBlank() -> {
                    viewModel.connectAsGuest(
                        guestToken ?: return@setOnClickListener,
                        guestName ?: "I'm a guest name!",
                        binding.localVideo,
                        binding.remoteVideo
                    )
                }
            }
            bindVideoViews()

            val imm: InputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        binding.disconnect.setOnClickListener {
            disconnect()
        }

        binding.muteLocal.setOnClickListener {
            viewModel.muteAudio()
        }

        binding.sendChat.setOnClickListener {
            viewModel.sendMessage(binding.chatMessage.text.toString())

            val imm: InputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        binding.muteAll.setOnClickListener {
            viewModel.muteAll()
        }

        binding.muteVideo.setOnClickListener {
            viewModel.muteVideoLocal()
            return@setOnClickListener
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.events.collect {
                        eventViewModel.newEvent(it)
                    }
                }

                launch {
                    viewModel.callTerminated.collect {
                        if (it) {
                            clearTargets()
                        }
                    }
                }
            }
        }

        binding.localVideo.setOnClickListener {
            viewModel.switchCamera()
        }

        if (viewModel.inCall) {
            bindVideoViews()

            viewModel.setTargets(binding.localVideo, binding.remoteVideo)
        }

        binding.startScreenShare.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

            }
            val manager =
                requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            requestScreenCapturePermission.launch(manager.createScreenCaptureIntent())
        }

        binding.fromShareToLocal.setOnClickListener {
            viewModel.stopScreenShare()
        }

        val menuHost: MenuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.showEvents -> {
                        EventListDialogFragment.newInstance()
                            .show(requireActivity().supportFragmentManager, "dialog")
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    private fun disconnect() {
        viewModel.disconnect()
        binding.localVideo.release()
        binding.remoteVideo.release()
    }

    private fun bindVideoViews() {
        binding.localVideo.init(viewModel.getEglContext(), null)
        binding.localVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        binding.localVideo.setEnableHardwareScaler(true)

        binding.remoteVideo.init(viewModel.getEglContext(), null)
        binding.remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        binding.remoteVideo.setEnableHardwareScaler(true)
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

    private fun generateScreenShareNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chan = NotificationChannel(
                "7", "CHANNEL_NAME", NotificationManager.IMPORTANCE_NONE
            )
            val manager =
                (requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            manager.createNotificationChannel(chan)

            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(requireContext(), "7")
            notificationBuilder
                .setOngoing(true)
                .setContentTitle("ScreenCapturerService is running in the foreground")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        } else {
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(requireContext(), "7")
            notificationBuilder
                .setOngoing(true)
                .setContentText("HALLOOOOOOO")
                .setContentTitle("ScreenCapturerService is running in the foreground")
                .setPriority(2)
                .setSmallIcon(R.drawable.menu)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        }
    }

    companion object {
        private const val ACCESS_KEY = "access_key"
        private const val GUEST_TOKEN = "guest_token"
        private const val GUEST_NAME = "guest_name"

        fun newInstance(accessKey: String, guestToken: String, guestName: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ACCESS_KEY, accessKey)
                    putString(GUEST_TOKEN, guestToken)
                    putString(GUEST_NAME, guestName)
                }
            }
    }
}