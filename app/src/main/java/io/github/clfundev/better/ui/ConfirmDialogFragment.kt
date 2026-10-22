package io.github.clfundev.better.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import io.github.clfundev.better.R
import io.github.clfundev.better.databinding.DialogConfirmBinding

class ConfirmDialogFragment : DialogFragment() {
    private lateinit var requestKey: String
    private lateinit var title: String
    private lateinit var content: String
    private lateinit var positiveText: String
    private lateinit var negativeText: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogConfirmBinding.inflate(requireActivity().layoutInflater)

        val titleTv = binding.tvConfirmTitle
        val contentTv = binding.tvConfirmContent
        val negativeTv = binding.btnbarConfirm.tvBtnbarNegative
        val positiveTv = binding.btnbarConfirm.tvBtnbarPositive

        val bundle = arguments
        if (bundle != null) {
            requestKey = bundle.getString(REQUEST_KEY) ?: ""
            title = bundle.getString(KEY_TITLE) ?: ""
            content = bundle.getString(KEY_CONTENT) ?: ""
            positiveText = bundle.getString(KEY_POSITIVE_TEXT) ?: ""
            negativeText = bundle.getString(KEY_NEGATIVE_TEXT) ?: ""
        }
        if (savedInstanceState != null) {
            requestKey = savedInstanceState.getString(REQUEST_KEY) ?: ""
            title = savedInstanceState.getString(KEY_TITLE) ?: ""
            content = savedInstanceState.getString(KEY_CONTENT) ?: ""
            positiveText = savedInstanceState.getString(KEY_POSITIVE_TEXT) ?: ""
            negativeText = savedInstanceState.getString(KEY_NEGATIVE_TEXT) ?: ""
        }

        titleTv.text = title
        contentTv.text = content
        positiveTv.text = positiveText
        positiveTv.setOnClickListener {
            val result = Bundle()
            result.putInt(KEY_WHICH, RESULT_POSITIVE)
            parentFragmentManager.setFragmentResult(requestKey, result)
            dismiss()
        }
        negativeTv.text = negativeText
        negativeTv.setOnClickListener {
            val result = Bundle()
            result.putInt(KEY_WHICH, RESULT_NEGATIVE)
            parentFragmentManager.setFragmentResult(requestKey, result)
            dismiss()
        }

        val dialog = Dialog(requireContext(), R.style.FadeAnimDialog)
        dialog.setContentView(binding.root)
        val window = dialog.window
        if (window != null) {
            val lp = window.attributes
            lp.width = resources.displayMetrics.widthPixels / 8 * 7 // 宽度
            window.setAttributes(lp)
        }
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(REQUEST_KEY, requestKey)
        outState.putString(KEY_TITLE, title)
        outState.putString(KEY_CONTENT, content)
        outState.putString(KEY_POSITIVE_TEXT, positiveText)
        outState.putString(KEY_NEGATIVE_TEXT, negativeText)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val TAG = "ConfirmDialogFragment"

        const val RESULT_POSITIVE = 1
        const val RESULT_NEGATIVE = 2
        const val KEY_WHICH = "KEY_WHICH"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_CONTENT = "KEY_CONTENT"
        private const val KEY_POSITIVE_TEXT = "KEY_POSITIVE_TEXT"
        private const val KEY_NEGATIVE_TEXT = "KEY_NEGATIVE_TEXT"
        private const val REQUEST_KEY = "REQUEST_KEY"

        @JvmStatic
        fun newInstance(
            requestKey: String,
            title: String,
            content: String,
            positiveText: String,
            negativeText: String
        ): ConfirmDialogFragment {
            val fragment = ConfirmDialogFragment()
            val bundle = Bundle()
            bundle.putString(REQUEST_KEY, requestKey)
            bundle.putString(KEY_TITLE, title)
            bundle.putString(KEY_CONTENT, content)
            bundle.putString(KEY_POSITIVE_TEXT, positiveText)
            bundle.putString(KEY_NEGATIVE_TEXT, negativeText)
            fragment.setArguments(bundle)
            return fragment
        }
    }
}