package tv.caffeine.app.profile


import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.facebook.login.LoginManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentMyProfileBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.ui.setOnAction
import tv.caffeine.app.util.decodeToBitmap
import tv.caffeine.app.util.getBitmapInSampleSize
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToLanding
import tv.caffeine.app.util.rotate
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.wallet.WalletViewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val REQUEST_GET_PHOTO = 1
private const val CAMERA_IMAGE_PATH = "CAMERA_IMAGE_PATH"

class MyProfileFragment @Inject constructor(
        private val accountsService: AccountsService,
        private val tokenStore: TokenStore,
        private val authWatcher: AuthWatcher,
        private val picasso: Picasso,
        private val facebookLoginManager: LoginManager
): CaffeineFragment(R.layout.fragment_my_profile) {

    private lateinit var binding: FragmentMyProfileBinding

    private val viewModel: MyProfileViewModel by viewModels { viewModelFactory }
    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }
    private val args by navArgs<MyProfileFragmentArgs>()

    private var cameraImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraImagePath = savedInstanceState?.getString(CAMERA_IMAGE_PATH)
        if (args.launchAvatarSelection) {
            chooseNewAvatarImage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CAMERA_IMAGE_PATH, cameraImagePath)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentMyProfileBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            binding.userProfile = userProfile
        })
        binding.signOutButton.setOnClickListener { confirmSignOut() }
        binding.infoButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToEditBioFragment()
            findNavController().safeNavigate(action)
        }
        binding.nameEditText.setOnAction(EditorInfo.IME_ACTION_SEND) {
            val updatedName = binding.nameEditText.text.toString()
            viewModel.updateName(updatedName)
        }
        binding.settingsButton.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToSettingsFragment()
            findNavController().safeNavigate(action)
        }
        binding.goldAndCreditsButtonContainer.setOnClickListener {
            val action = MyProfileFragmentDirections.actionMyProfileFragmentToGoldAndCreditsFragment()
            findNavController().safeNavigate(action)
        }
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer { wallet ->
            if (wallet == null) return@Observer
            val goldBalance = NumberFormat.getInstance().format(wallet.gold)
            val creditsBalance = NumberFormat.getInstance().format(wallet.credits)
            binding.goldAndCreditsBalanceButton.formatUsernameAsHtml(picasso, getString(R.string.gold_and_credits_button_balance, goldBalance, creditsBalance))
        })
        binding.followingContainer.setOnClickListener { showFollowingList() }
        binding.followerContainer.setOnClickListener { showFollowersList() }
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
        val fileExists = cameraImagePath?.let {
            File(it).run { exists() && length() > 1000 }
        } ?: false
        when {
            uri != null && !fileExists -> uploadPhotoFromUri(uri)
            uri == null && fileExists -> uploadPhotoFromCamera()
        }
    }

    private fun uploadPhotoFromUri(uri: Uri) {
        launch(dispatchConfig.io) {
            val contentResolver = context?.contentResolver ?: return@launch
            // Sample the bitmap
            var inputStream = contentResolver.openInputStream(uri) ?: return@launch
            val inSampleSize = inputStream.getBitmapInSampleSize(1024)
            inputStream.close()

            // Open the stream again to create the bitmap since it doesn't support reset()
            inputStream = contentResolver.openInputStream(uri) ?: return@launch
            val bitmap = inputStream.decodeToBitmap(inSampleSize)
            inputStream.close()
            if (bitmap == null) return@launch

            // Rotate if needed
            inputStream = contentResolver.openInputStream(uri) ?: return@launch
            val rotationDegrees = ExifInterface(inputStream).rotationDegrees.toFloat()
            inputStream.close()
            val editedBitmap = bitmap.rotate(rotationDegrees)

            withContext(dispatchConfig.main) {
                viewModel.uploadAvatar(editedBitmap)
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
        findNavController().safeNavigate(action)
    }

    private fun showFollowersList() {
        val caid = tokenStore.caid ?: return
        val action = MyProfileFragmentDirections.actionMyProfileFragmentToFollowersFragment(caid)
        findNavController().safeNavigate(action)
    }

    private fun confirmSignOut() {
        SignOutDialogFragment().let {
            it.positiveClickListener =  DialogInterface.OnClickListener { _, _ -> signOut() }
            it.maybeShow(fragmentManager, "signOut")
        }
    }

    private fun signOut() {
        tokenStore.clear()
        authWatcher.onSignOut()
        findNavController().navigateToLanding()
        facebookLoginManager.logOut()
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
