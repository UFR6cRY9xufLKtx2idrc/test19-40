package me.ykrank.s1next.view.dialog

import android.app.Activity
import android.app.Dialog
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.ClipboardUtil
import com.github.ykrank.androidtools.widget.AlipayDonate
import me.ykrank.s1next.R
import me.ykrank.s1next.databinding.DialogRedEnvelopesBinding

/**
 * A dialog lets the user enter blacklist remark.
 */
class RedEnvelopesDialogFragment : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity as Activity
        val binding = DataBindingUtil.inflate<DialogRedEnvelopesBinding>(activity.layoutInflater,
                R.layout.dialog_red_envelopes, null, false)

        val clickListener = {
            ClipboardUtil.copyText(activity, getString(R.string.red_envelopes_copy_label), binding.tvCode.text.toString())
            activity.toast(R.string.copied)
        }

        binding.root.setOnClickListener { clickListener.invoke() }

        val alertDialog = AlertDialog.Builder(activity)
                .setTitle(R.string.red_envelopes)
                .setView(binding.root)
                .setPositiveButton(R.string.dialog_button_text_confirm) { dialog, which ->
                    clickListener.invoke()
                    if (AlipayDonate.hasInstalledAlipayClient(activity)) {
                        AlipayDonate.startAlipay(activity)
                    } else {
                        activity.toast("需要安装支付宝")
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        return alertDialog
    }

    companion object {

        val TAG = RedEnvelopesDialogFragment::class.java.name

        fun newInstance(): RedEnvelopesDialogFragment {
            return RedEnvelopesDialogFragment()
        }
    }
}
