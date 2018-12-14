package tv.caffeine.app.profile


import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentMyProfileBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.navigateToLanding
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val REQUEST_GET_PHOTO = 1
private const val CAMERA_IMAGE_PATH = "CAMERA_IMAGE_PATH"

class MyProfileFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentMyProfileBinding

    private val viewModel by lazy { viewModelProvider.get(MyProfileViewModel::class.java) }
    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }

    private var cameraImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraImagePath = savedInstanceState?.getString(CAMERA_IMAGE_PATH)
        if (MyProfileFragmentArgs.fromBundle(arguments).launchAvatarSelection) {
            chooseNewAvatarImage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CAMERA_IMAGE_PATH, cameraImagePath)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.signOutButton.setOnClickListener { confirmSignOut() }
        binding.infoButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToEditBioFragment()
            findNavController().navigate(action)
        }
        binding.nameEditText.setOnAction(EditorInfo.IME_ACTION_SEND) {
            val updatedName = binding.nameEditText.text.toString()
            viewModel.updateName(updatedName)
        }
        binding.settingsButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
        binding.goldAndCreditsButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToGoldAndCreditsFragment()
            findNavController().navigate(action)
        }
        walletViewModel.wallet.observe(viewLifecycleOwner, androidx.lifecycle.Observer { wallet ->
            if (wallet == null) return@Observer
            val goldBalance = NumberFormat.getInstance().format(wallet.gold)
            val creditsBalance = NumberFormat.getInstance().format(wallet.credits)
            binding.goldAndCreditsButton.htmlText = getString(R.string.gold_and_credits_button_with_balance, goldBalance, creditsBalance)
        })
        binding.numberFollowingTextView.setOnClickListener { showFollowingList() }
        binding.numberOfFollowersTextView.setOnClickListener { showFollowersList() }
        binding.avatarImageView.setOnClickListener { chooseNewAvatarImage() }
    }

    private fun chooseNewAvatarImage() {
        val galleryIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        val chooserTitle = getString(R.string.pick_profile_photo)
        val chooser = Intent.createChooser(galleryIntent, chooserTitle)
        val cameraIntent = createCameraIntent()
        if (cameraIntent != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }
        startActivityForResult(chooser, REQUEST_GET_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_GET_PHOTO) {
            return super.onActivityResult(requestCode, resultCode, data)
        }
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data
        val cameraFile = File(cameraImagePath)
        val fileExists = cameraImagePath != null && cameraFile.exists() && cameraFile.length() > 1000
        when {
            uri != null && !fileExists -> uploadPhotoFromUri(uri)
            uri == null && fileExists -> uploadPhotoFromCamera()
        }
    }

    private fun uploadPhotoFromUri(uri: Uri) {
        launch(dispatchConfig.io) {
            val inputStream = context?.contentResolver?.openInputStream(uri) ?: return@launch
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            withContext(dispatchConfig.main) {
                viewModel.uploadAvatar(bitmap)
            }
        }
    }

    private fun uploadPhotoFromCamera() {
        val imageFile = File(cameraImagePath)
        cameraImagePath = null
        if (!imageFile.exists()) return
        val uri = imageFile.toUri()
        uploadPhotoFromUri(uri)
    }

    private fun createCameraIntent(): Intent? {
        val context = this.context ?: return null
        val packageManager = context.packageManager ?: return null
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) == null) return null
        val imageFile = createImageFile() ?: return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraImagePath = imageFile.absolutePath
        return cameraIntent
    }

    private fun createImageFile(): File? {
        // Create an image file name
        val storageDir: File = context?.cacheDir?.let { File(it, "camera") } ?: return null
        storageDir.mkdirs()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File.createTempFile("JPEG_${timeStamp}_",  ".jpg", storageDir)
    }

    private fun showFollowingList() {
        val caid = tokenStore.caid ?: return
        val action = MyProfileFragmentDirections.actionMyProfileFragmentToFollowingFragment(caid)
        findNavController().navigate(action)
    }

    private fun showFollowersList() {
        val caid = tokenStore.caid ?: return
        val action = MyProfileFragmentDirections.actionMyProfileFragmentToFollowersFragment(caid)
        findNavController().navigate(action)
    }

    private fun confirmSignOut() {
        SignOutDialogFragment().let {
            it.positiveClickListener =  DialogInterface.OnClickListener { _, _ -> signOut() }
            it.show(fragmentManager, "signOut")
        }
    }

    private fun signOut() {
        tokenStore.clear()
        findNavController().navigateToLanding()
        accountsService.signOut().enqueue(object : Callback<Unit?> {
            override fun onFailure(call: Call<Unit?>?, t: Throwable?) {
                Timber.e(t, "Failed to sign out")
            }

            override fun onResponse(call: Call<Unit?>?, response: Response<Unit?>?) {
                Timber.d("Signed out successfully $response")
            }
        })
    }

}
