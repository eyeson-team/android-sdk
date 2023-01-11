package com.eyeson.android.ui.view.scanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.eyeson.android.BR
import com.eyeson.android.R
import com.eyeson.android.databinding.ScannerFragmentBinding

class ScannerFragment : Fragment() {
    private lateinit var binding: ScannerFragmentBinding
    private val viewModel: ScannerViewModel by viewModels()

    private lateinit var codeScanner: CodeScanner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.scanner_fragment, container, false)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        codeScanner = CodeScanner(requireContext(), binding.scannerView)
        codeScanner.decodeCallback = DecodeCallback {
            setFragmentResult(
                SCAN_RESULT,
                bundleOf(SCAN_ULR to viewModel.extractGuestToken(it.text))
            )
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.scannerView.setOnClickListener {
            codeScanner.startPreview()
        }

    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    companion object {
        const val SCAN_RESULT = "scan_result"
        const val SCAN_ULR = "scan_result"
        fun newInstance() = ScannerFragment()

    }
}