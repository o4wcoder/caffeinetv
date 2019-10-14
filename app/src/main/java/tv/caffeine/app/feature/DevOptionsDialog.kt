package tv.caffeine.app.feature

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Switch
import androidx.annotation.StyleRes
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jakewharton.processphoenix.ProcessPhoenix
import tv.caffeine.app.R
import tv.caffeine.app.databinding.DialogDevOptionsBinding

class DevOptionsDialog : BottomSheetDialog {

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)
    constructor(context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener) :
        super(context, cancelable, cancelListener)

    lateinit var binding: DialogDevOptionsBinding
    lateinit var featureConfig: FeatureConfig
    private val switchMap = HashMap<Switch, Boolean>()

    override fun show() {
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_dev_options, null, false)
        setContentView(binding.root)
        configure()
        super.show()
    }

    private fun configure() {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        binding.versionTextView.text = context.getString(R.string.dev_options_version, versionName)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        load(sharedPreferences)
        binding.okButton.setOnClickListener {
            var hasChange = false
            val editor = sharedPreferences.edit()
            for ((switch, value) in switchMap) {
                if (switch.isChecked != value) {
                    hasChange = true
                    editor.putBoolean(switch.tag as String, switch.isChecked)
                }
            }
            if (hasChange) {
                editor.commit() // synchronously commit before the app restarts
                ProcessPhoenix.triggerRebirth(context)
            }
            dismiss()
        }
    }

    private fun load(sharedPreferences: SharedPreferences) {
        setSwitchValue(binding.releaseUiSwitch, "release_design", sharedPreferences)
        setSwitchValue(binding.autoplayPreviewAllSwitch, "play_all", sharedPreferences)
    }

    private fun setSwitchValue(switch: Switch, featureName: String, sharedPreferences: SharedPreferences) {
        val value = sharedPreferences.getBoolean(featureName, false)
        switchMap[switch] = value
        switch.isChecked = value
        switch.tag = featureName
    }
}